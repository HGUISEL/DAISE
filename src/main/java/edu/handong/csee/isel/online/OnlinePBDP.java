package edu.handong.csee.isel.online;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import edu.handong.csee.isel.MainDAISE;
import edu.handong.csee.isel.pdp.Metrics;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class OnlinePBDP {
	String wekaOutputPath;
	String outputPath;
	static String projectName;
	String referencePath;
	boolean accumulate;
	int minimumCommitForProfilingParameter;
	int run = 0;

	ArrayList<RunDate> runDates;

	int minCommit;//mincommit 30 to 100 - option
	int numOfCluster; //option
	ClusterEvaluation eval;
	String defaultLabel;
	int defaultCluster;

	private String firstcommitTimePatternStr ;
	private Pattern firstcommitTimePattern ;
	private String developerIDPatternStr ;
	private Pattern developerIDPattern ;

	HashMap<String, ArrayList<String>> accum_developerID_commitHash; 

	OnlinePBDP(){
		this.firstcommitTimePatternStr = ".+/Developer_(.+)_Online.csv";
		this.firstcommitTimePattern = Pattern.compile(firstcommitTimePatternStr);
		this.developerIDPatternStr = "((\\?)|' (.+)'),(.+)";
		this.developerIDPattern = Pattern.compile(developerIDPatternStr);

		minCommit = 0;
		numOfCluster = 0;
		accum_developerID_commitHash = new HashMap<>();
		accumulate = false;
		defaultCluster = 0;
	}


	public void profilingBasedDefectPrediction(ArrayList<String> attributeLineList,
			TreeMap<String,String> key_fixTime,
			TreeMap<String,TreeSet<String>> commitTime_commitHash,
			HashMap<String,HashMap<String,String>> commitHash_key_data,
			HashMap<String,HashMap<String,Boolean>> commitHash_key_isBuggy,
			HashMap<String,String> commitHash_developer) throws Exception {
		System.out.println();
		System.out.println("######################################################################################");
		System.out.println("###        Start Online PBDP2   |   DefaultCluster = "+defaultCluster+"    |   isMinCommit = "+minimumCommitForProfilingParameter+"   ###");
		System.out.println("######################################################################################");
		System.out.println();

		//make PBDP directory
		File PBDPdir = new File(outputPath);
		String directoryPath = PBDPdir.getAbsolutePath();
		if(PBDPdir.isDirectory()) {
			deleteFile(outputPath);
		}
		PBDPdir.mkdir();

		HashMap<Integer,RunningData> run_runningData = new HashMap<>();
		int BeforeNumOfDeveloper = 0;
		int trNumOfCommitHash;
		int trDeveloper;

		for(RunDate runDate : runDates) {
			RunningData runningData = new RunningData();
			
			numOfCluster = 0;
			if(minimumCommitForProfilingParameter != 0) {
				minCommit = minimumCommitForProfilingParameter;
			}else {
				minCommit = 0;
			}
			BeforeNumOfDeveloper = 0;
			trNumOfCommitHash = 0;
			trDeveloper = 0;
			
			System.out.println("------------------Run = "+ run + " ------------------");

			//cal the number of developer commit in training set
			String trS = runDate.getTrS();//training start time
			String trE_gapS = runDate.getTrE_gapS();//training end time == gap start time
			String gapE_teS = runDate.getGapE_teS();//gap start time == test start time 
			String teE = runDate.getTeE();//test end
			System.out.println("trS = "+ trS);
			System.out.println("trE_gapS = "+ trE_gapS);
			System.out.println("gapE_teS = "+ gapE_teS);
			System.out.println("teE = "+ teE);
			System.out.println();
			
			runningData.setTrS(trS);
			runningData.setTrE_gapS(trE_gapS);
			runningData.setGapE_teS(gapE_teS);
			runningData.setTeE(teE);

			//make developerID_commitHashs in training period
			HashMap<String,ArrayList<String>> tr_developerID_commitHashs; //in tr period

			tr_developerID_commitHashs = countTheNumOfdeveloperAndCommit(trS, trE_gapS, commitTime_commitHash, commitHash_developer,"tr");
			System.out.println("numOfDev in tr peroid : "+tr_developerID_commitHashs.size());
			
			//count the number of dveloper commit tr peroid- descending order in
			TreeMap<Integer,ArrayList<String>> numOfCommit_developer;
			numOfCommit_developer = countTheNumOfDeveloperCommit(tr_developerID_commitHashs);

			//pick dev id : dev commit > "minCommit"
			HashMap<Integer,ArrayList<String>> tr_cluster_developerID = null;
			while(true) {
				//pick dev id : condition : commit > "minCommit"
				ArrayList<String> trClusteringDeveloperID = new ArrayList<String>();

				for(int numOfCommit : numOfCommit_developer.keySet()) {
					if(numOfCommit < minCommit) break;
					ArrayList<String> devID = numOfCommit_developer.get(numOfCommit);
					trClusteringDeveloperID.addAll(devID);
				}
				
				if(minimumCommitForProfilingParameter != 0) {
					if(BeforeNumOfDeveloper == trClusteringDeveloperID.size()) {
						if(minCommit < 30){
							minCommit++;
							tr_cluster_developerID.clear();
							continue;
						}
					}
				}
				//				System.out.println("Top developer id in tr set: "+trClusteringDeveloperID.size());

				//count numOfCluster
				//0. save only tr commitHash (top Devloper)
				ArrayList<String> trClusteringCommitHash = saveDeveloperCommitHash(trClusteringDeveloperID,tr_developerID_commitHashs);
				trNumOfCommitHash = trClusteringCommitHash.size();
				trDeveloper = trClusteringDeveloperID.size();
				
				//1. make developer profiling data set
				String profilingMetadatacsvPath = makeCsvFileFromTopTenDeveloperInTraining(trClusteringCommitHash,"tr");
				if(profilingMetadatacsvPath == null) {
					System.out.println("profilingMetadatacsvPath is null");
					System.exit(0);
				}
				File developerProfiling = new File(collectingDeveloperProfilingMetrics(profilingMetadatacsvPath));
				//2. read developer profiling csv and apply cluster
				tr_cluster_developerID = clusteringProfilingDeveloper(developerProfiling);
				//3. parsing cluster result
				numOfCluster = tr_cluster_developerID.size();
				
				if(defaultCluster != 0) break;
				
				if(minimumCommitForProfilingParameter != 0) {
					if((numOfCluster != 1)) {
						break;
					}else if(minCommit == 30){
						break;
					}else {
						minCommit++;
						BeforeNumOfDeveloper = trClusteringDeveloperID.size();
						tr_cluster_developerID.clear();
					}
				}else {
					break;
				}
			}
			System.out.println("Final minCommit of tr : "+minCommit);
			System.out.println("Final numOfCluster of tr : "+numOfCluster);
			runningData.setAllTrDeveloper(tr_developerID_commitHashs.size());
			runningData.setNumOfCluster(numOfCluster);
			runningData.setTrCommit(trNumOfCommitHash);
			runningData.setTrDeveloper(trDeveloper);
			
			//make tr arff file in each clustering
			TreeMap<Integer,String> trcluster_clusterName = makeArffFileInEachTrClustering(tr_cluster_developerID, tr_developerID_commitHashs, attributeLineList, commitHash_key_data, outputPath, run, key_fixTime, commitHash_key_isBuggy, teE);
			
			System.out.println();
			System.out.println("test start");
			//////////////////////
			//	  test set      //
			//////////////////////
			HashMap<String,ArrayList<String>>te_developerID_commitHashs = countTheNumOfdeveloperAndCommit(gapE_teS, teE, commitTime_commitHash, commitHash_developer,"te");
			System.out.println("numOfDev in te peroid : "+te_developerID_commitHashs.size());

			//find the cluster of test developer
			//save the profiling csv file of each developer 
			ArrayList<String> teProfilingMetadatacsvPath = makeCsvFileFromTestDeveloper(te_developerID_commitHashs);
			ArrayList<File> teDeveloperProfiling = collectingTestDeveloperProfilingMetrics(teProfilingMetadatacsvPath);

			//read each test developer csv and find cluster
			HashMap<String,ArrayList<String>> teCluster_developerID = clusteringTestProfilingDeveloper(teDeveloperProfiling, trcluster_clusterName);

			//count number of test set commit 
			int total = 0;
			for (ArrayList<String> l : te_developerID_commitHashs.values()) {
			    total += l.size();
			}
			runningData.setTeDeveloper(te_developerID_commitHashs.size());
			runningData.setTeCommit(total);
			
			//make te arff file in each clustering
			makeArffFileInEachTestClustering(teCluster_developerID, te_developerID_commitHashs, attributeLineList, commitHash_key_data, outputPath, run);

			//save running data
			run_runningData.put(run,runningData);
			run++;
		}	
//		System.exit(0);
		Save2CSV(directoryPath,run_runningData);
		System.out.println(directoryPath);
		//call PBDP weka directoryPath
		wekaClassify(directoryPath,wekaOutputPath,defaultCluster, minimumCommitForProfilingParameter);

	}

	private void Save2CSV(String directoryPath, HashMap<Integer, RunningData> run_runningData) throws Exception {
		String resultCSVPath = directoryPath + File.separator + "Run_PBDP_Information.csv";
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultCSVPath));
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("TrS","TrEn_GapS","GapEn_TeSt","TeE","numOfCluster","trCommit","AlltrDeveloper","trClusteringDev","teCommit","teDeveloper"));

		for(int i = 0; i < run; i++) {
			RunningData runningData = run_runningData.get(i);
			csvPrinter.printRecord(runningData.getTrS(),runningData.getTrE_gapS(),runningData.getGapE_teS(),runningData.getTeE(),runningData.getNumOfCluster(),runningData.getTrCommit(),runningData.getAllTrDeveloper(),runningData.getTrDeveloper(),runningData.getTeCommit(),runningData.getTeDeveloper());
		}
		csvPrinter.close();
		writer.close();
		
	}


	public void wekaClassify(String path, String wekaOutputPath, int defaultCluster, int minimumCommitForProfilingParameter) throws Exception {
		String[] WekaArgs = new String[4];

		WekaArgs[0] = path;
		WekaArgs[1] = wekaOutputPath;
		WekaArgs[2] = Integer.toString(defaultCluster);
		WekaArgs[3] = Integer.toString(minimumCommitForProfilingParameter);

		OnlineWeka onlineWeka2 = new OnlineWeka();
		onlineWeka2.main(WekaArgs);
	}

	private HashMap<String, ArrayList<String>> clusteringTestProfilingDeveloper(ArrayList<File> teDeveloperProfiling, TreeMap<Integer, String> trcluster_clusterName) throws Exception {
		HashMap<String,ArrayList<String>> teCluster_developerID = new HashMap<>();
		ClusterEvaluation eval = getEval();

		for(File developerProfiling : teDeveloperProfiling) {

			Matcher m = firstcommitTimePattern.matcher(developerProfiling.toString());
			m.find();
			String developerID = m.group(1);

			CSVLoader loader = new CSVLoader();
			loader.setSource(developerProfiling);

			Instances data = loader.getDataSet();

			int[] toSelect = new int[data.numAttributes()-1];

			for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) {
				toSelect[i] = j;
			}
			//delete developer ID column of CSV file
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndicesArray(toSelect);
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(data);
			Instances newData = Filter.useFilter(data, removeFilter);

			eval.evaluateClusterer(newData);
			double[] assignments = eval.getClusterAssignments();

			if(assignments.length > 1) System.out.println("_______________________Emergency_________________");
			int wekaCluster = (int)assignments[0];
			
			String cluster = trcluster_clusterName.get(wekaCluster);

			ArrayList<String> teDeveloperList;
			if(teCluster_developerID.containsKey(cluster)) {
				teDeveloperList = teCluster_developerID.get(cluster);
				teDeveloperList.add(developerID);
			}else {
				teDeveloperList = new ArrayList<>();
				teDeveloperList.add(developerID);
				teCluster_developerID.put(cluster, teDeveloperList);
			}
		}

		return teCluster_developerID;
	}

	private ArrayList<File> collectingTestDeveloperProfilingMetrics(ArrayList<String> teProfilingMetadatacsvPath) throws Exception{
		ArrayList<File> developerProfiling = new ArrayList<>();

		for(String path : teProfilingMetadatacsvPath) {
			developerProfiling.add(new File(collectingDeveloperProfilingMetrics(path)));
		}

		return developerProfiling;
	}

	private ArrayList<String> makeCsvFileFromTestDeveloper(
			HashMap<String, ArrayList<String>> te_developerID_commitHashs) {
		ArrayList<String> teProfilingMetadatacsvPath = new ArrayList<>();

		try {
			for(String devID : te_developerID_commitHashs.keySet()) {
				ArrayList<String> commitHashs = te_developerID_commitHashs.get(devID);

				String profilingMetadatacsvPath = makeCsvFileFromTopTenDeveloperInTraining(commitHashs,devID);
				teProfilingMetadatacsvPath.add(profilingMetadatacsvPath);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return teProfilingMetadatacsvPath;
	}
	
	private TreeMap<Integer, String> makeArffFileInEachTrClustering(HashMap<Integer, ArrayList<String>> cluster_developerID,
			HashMap<String, ArrayList<String>> tr_developerID_commitHashs, ArrayList<String> attributeLineList, HashMap<String, HashMap<String, String>> commitHash_key_data, String outputPath, int run, TreeMap<String, String> key_fixTime, HashMap<String, HashMap<String, Boolean>> commitHash_key_isBuggy, String teE) throws Exception {
		TreeMap<Integer,String> trcluster_clusterName = new TreeMap<>();
		TreeMap<Integer,ArrayList<Integer>> numOfKey_trcluster = new TreeMap<>(Collections.reverseOrder());
		File newDevelopeBaseLineArff = new File(outputPath +File.separator+"run_"+run+"_cluster_PBDPbaseline_tr.arff");
		StringBuffer newBaseLineContentBuf = new StringBuffer();
		
		for (String line : attributeLineList) {
			if(line.startsWith("@attribute meta_data-commitTime")) continue;
			if(line.startsWith("@attribute Key {")) continue;
			newBaseLineContentBuf.append(line + "\n");
		}
		
		for(int cluster : cluster_developerID.keySet()) {
			File newDeveloperArff = new File(outputPath +File.separator+"run_"+run+"_cluster_"+cluster+"_tr.arff");
			int numOfKey = 0;
			
			// ./run_1_cluster_2_tr.arff
			StringBuffer newContentBuf = new StringBuffer();
			ArrayList<String> developerIDs = cluster_developerID.get(cluster);

			//write attribute
			for (String line : attributeLineList) {
				if(line.startsWith("@attribute meta_data-commitTime")) continue;
				if(line.startsWith("@attribute Key {")) continue;
				newContentBuf.append(line + "\n");
			}
			
			//write data
			for(String developerID : tr_developerID_commitHashs.keySet()) {
				if(developerIDs.contains(developerID)) {
					ArrayList<String> commitHashs = tr_developerID_commitHashs.get(developerID);
					for(String commitHash : commitHashs) {
						for(String key : commitHash_key_data.get(commitHash).keySet()) {
							boolean isBuggy = commitHash_key_isBuggy.get(commitHash).get(key);
							String fixTime = key_fixTime.get(key);
							String data = commitHash_key_data.get(commitHash).get(key);
							
							if(isBuggy == true) {
								if(teE.compareTo(fixTime)<=0) { // fixTime > teE // run의 기간 보다 후에 결함이 수정될경우 label = clean

									//make data to clean 
									if(defaultLabel.compareTo("buggy") == 0){
										if(data.startsWith("{0 buggy,")) {
											data = "{"+data.substring(data.indexOf(",")+1, data.length());
										}else {
											System.out.println("큰일!!!");
										}
									}else if(defaultLabel.compareTo("clean") == 0) {
										if(!data.startsWith("{0 clean,")) {
											data = data.substring(data.indexOf("{")+1, data.length());
											data = "{0 clean," + data;
										}else {
											System.out.println("큰일!!!");
										}
									}
								}
							}
							newContentBuf.append(data + "\n");
							newBaseLineContentBuf.append(data + "\n");
							numOfKey++;
						}
					}
				}
			}
			FileUtils.write(newDeveloperArff, newContentBuf.toString(), "UTF-8");
			ArrayList<Integer> trclusters;
			if(numOfKey_trcluster.containsKey(numOfKey)) {
				trclusters = numOfKey_trcluster.get(numOfKey);
				trclusters.add(cluster);
			}else {
				trclusters = new ArrayList<Integer>();
				trclusters.add(cluster);
				numOfKey_trcluster.put(numOfKey, trclusters);
			}
		}
		FileUtils.write(newDevelopeBaseLineArff, newBaseLineContentBuf.toString(), "UTF-8");
		
		float largestKey = numOfKey_trcluster.firstKey();
		int twentyPercentOfLargestKey = (int)(largestKey * 0.2);
		
		for(int numOfKey : numOfKey_trcluster.keySet()) {
			ArrayList<Integer> trclusters = numOfKey_trcluster.get(numOfKey);
			for(int trcluster : trclusters) {
				String clusterName = Integer.toString(trcluster);
				if(numOfKey < twentyPercentOfLargestKey) {
					clusterName = "PBDPbaseline";
				}
				trcluster_clusterName.put(trcluster,clusterName);
			}
		}
		
		return trcluster_clusterName;
	}

	private void makeArffFileInEachTestClustering(HashMap<String, ArrayList<String>> cluster_developerID,
			HashMap<String, ArrayList<String>> tr_developerID_commitHashs, ArrayList<String> attributeLineList, HashMap<String, HashMap<String, String>> commitHash_key_data, String outputPath, int run) throws Exception {


		for(String cluster : cluster_developerID.keySet()) {
			File newDeveloperArff = new File(outputPath +File.separator+"run_"+run+"_cluster_"+cluster+"_te.arff");
			// ./run_1_cluster_2_tr.arff
			StringBuffer newContentBuf = new StringBuffer();
			ArrayList<String> developerIDs = cluster_developerID.get(cluster);

			//write attribute
			for (String line : attributeLineList) {
				if(line.startsWith("@attribute meta_data-commitTime")) continue;
				if(line.startsWith("@attribute Key {")) continue;
				newContentBuf.append(line + "\n");
			}

			//write data
			for(String developerID : tr_developerID_commitHashs.keySet()) {
				if(developerIDs.contains(developerID)) {
					ArrayList<String> commitHashs = tr_developerID_commitHashs.get(developerID);
					for(String commitHash : commitHashs) {
						for(String key : commitHash_key_data.get(commitHash).keySet()) {
							String data = commitHash_key_data.get(commitHash).get(key);
							newContentBuf.append(data + "\n");
						}
					}
				}
			}

			FileUtils.write(newDeveloperArff, newContentBuf.toString(), "UTF-8");

		}

	}

	private HashMap<Integer, ArrayList<String>> clusteringProfilingDeveloper(File developerProfiling) throws Exception {
		HashMap<Integer,ArrayList<String>> cluster_developer = new HashMap<>();

		CSVLoader loader = new CSVLoader();
		loader.setSource(developerProfiling);

		Instances data = loader.getDataSet();

		int[] toSelect = new int[data.numAttributes()-1];

		for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) {
			toSelect[i] = j;
		}

		//delete developer ID column of CSV file
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndicesArray(toSelect);
		removeFilter.setInvertSelection(true);
		removeFilter.setInputFormat(data);
		Instances newData = Filter.useFilter(data, removeFilter);

		//apply EM clustering algorithm
		EM em = new EM(); //option
		if(defaultCluster != 0) {
			em.setNumClusters(defaultCluster); //option
		}
		em.buildClusterer(newData);

		//		SimpleKMeans sk = new SimpleKMeans();
		//		sk.setSeed(10);
		//		sk.setPreserveInstancesOrder(true);
		//		sk.setNumClusters(3);
		//		sk.buildClusterer(newData);

		///save developer cluster!
		ArrayList<String> developerNameCSV = new ArrayList<String>(); //developer ID
		ArrayList<String> developerInstanceCSV = new ArrayList<String>(); //All of developer instance 

		for(int i = 0; i < data.numInstances(); i++) {
			Matcher m = developerIDPattern.matcher(data.instance(i).toString());
			while(m.find()) {
				if(m.group(3) == null) developerNameCSV.add(m.group(1));
				else developerNameCSV.add(m.group(3));
				developerInstanceCSV.add(m.group(4));
			}
		}

		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(em);
		eval.evaluateClusterer(newData);
		setEval(eval);
		System.out.println("------------------------------NUM cluster --- "+eval.getNumClusters());

		double[] assignments = eval.getClusterAssignments();

		for(int i = 0; i < newData.size(); i++) {
			int index = findIndex(newData.instance(i).toString(), developerInstanceCSV);
			int cluster = (int)assignments[i];
			if(index == 1000) {
				System.out.println(data.instance(i));
			}
			String developerID = developerNameCSV.get(index);
			ArrayList<String> developerList;

			if(cluster_developer.containsKey(cluster)) {
				developerList = cluster_developer.get(cluster);
				developerList.add(developerID);
			}else {
				developerList = new ArrayList<>();
				developerList.add(developerID);
				cluster_developer.put(cluster, developerList);
			}

			developerInstanceCSV.remove(index);
			developerNameCSV.remove(index);
		}

		return cluster_developer;
	}

	private int findIndex(String instance, ArrayList<String> developerInstanceCSV) {
		for(int i = 0; i < developerInstanceCSV.size(); i++) {
			if(developerInstanceCSV.get(i).equals(instance)) {
				return i;
			}
		}
		return 1000;
	}


	private ArrayList<String> saveDeveloperCommitHash(ArrayList<String> clusteringDeveloperID,
			HashMap<String, ArrayList<String>> tr_developerID_commitHashs) {
		ArrayList<String> trClusteringCommitHash = new ArrayList<>();

		for(String devID : tr_developerID_commitHashs.keySet()) {

			if(clusteringDeveloperID.contains(devID)) {
				ArrayList<String> commitHashs = tr_developerID_commitHashs.get(devID);
				trClusteringCommitHash.addAll(commitHashs);
			}

		}

		return trClusteringCommitHash;
	}

	private TreeMap<Integer, ArrayList<String>> countTheNumOfDeveloperCommit(
			HashMap<String, ArrayList<String>> tr_developerID_commitHashs) {
		TreeMap<Integer,ArrayList<String>> numOfCommit_developer = new TreeMap<>(Collections.reverseOrder());

		for(String developerID : tr_developerID_commitHashs.keySet()) {
			int numOfCommit = tr_developerID_commitHashs.get(developerID).size();

			ArrayList<String> developers;
			if(numOfCommit_developer.containsKey(numOfCommit)) {
				developers = numOfCommit_developer.get(numOfCommit);
				developers.add(developerID);
			}else {
				developers  = new ArrayList<String>();
				developers.add(developerID);
				numOfCommit_developer.put(numOfCommit, developers);
			}
		}

		return numOfCommit_developer;
	}

	private String collectingDeveloperProfilingMetrics(String path) throws Exception {
		String[] DAISEargs = new String[4];

		DAISEargs[0] = "-m";
		DAISEargs[1] = path;
		DAISEargs[2] = "-o";
		DAISEargs[3] = referencePath.toString();

		MainDAISE DAISEmain = new MainDAISE();
		DAISEmain.run(DAISEargs);
		return DAISEmain.getOutpuCSV();
	}

	private HashMap<String, ArrayList<String>> countTheNumOfdeveloperAndCommit(String trS, String trE_gapS,
			TreeMap<String, TreeSet<String>> commitTime_commitHash, HashMap<String, String> commitHash_developer, String string) {

		HashMap<String,ArrayList<String>> tr_developerID_commitHashs = new HashMap<>();

		for(String commitTime : commitTime_commitHash.keySet()) {
			if(!(trS.compareTo(commitTime)<=0 && commitTime.compareTo(trE_gapS)<=0))
				continue;

			TreeSet<String> commitHashs = commitTime_commitHash.get(commitTime);
			for(String commitHash : commitHashs) {
				String developerID = commitHash_developer.get(commitHash);

				ArrayList<String> dev_commitHash;

				if(accumulate == false || (string.compareTo("tr")==0) ) {
					if(tr_developerID_commitHashs.containsKey(developerID)) {
						dev_commitHash = tr_developerID_commitHashs.get(developerID);
						dev_commitHash.add(commitHash);
					}else {
						dev_commitHash = new ArrayList<String>();
						dev_commitHash.add(commitHash);
						tr_developerID_commitHashs.put(developerID, dev_commitHash);
					}
				}else {
					if(accum_developerID_commitHash.containsKey(developerID)) {
						dev_commitHash = accum_developerID_commitHash.get(developerID);
						dev_commitHash.add(commitHash);
						int numOfCommit = dev_commitHash.size();
						if(numOfCommit > 500) {
							dev_commitHash.remove(0);
						}
						//						System.out.println("dev_commitHash = "+developerID+" : "+dev_commitHash.size());
						accum_developerID_commitHash.put(developerID, dev_commitHash);
					}else {
						dev_commitHash = new ArrayList<String>();
						dev_commitHash.add(commitHash);
						accum_developerID_commitHash.put(developerID, dev_commitHash);
					}
				}

			}
		}
		if(accumulate == false || string.compareTo("tr")==0)
			return tr_developerID_commitHashs;
		else {
			return accum_developerID_commitHash;
		}
	}

	public static void deleteFile(String path) {
		File deleteFolder = new File(path);

		if(deleteFolder.exists()){
			File[] deleteFolderList = deleteFolder.listFiles();
			
			for (int i = 0; i < deleteFolderList.length; i++) {
				if(deleteFolderList[i].isFile()) {
					deleteFolderList[i].delete();
				}else {
					deleteFile(deleteFolderList[i].getPath());
				}
				deleteFolderList[i].delete(); 
			}
			deleteFolder.delete();
		}
	}

	private String makeCsvFileFromTopTenDeveloperInTraining(ArrayList<String> trCommitHash, String devID) {

		try {
			//read csv
			Reader in = new FileReader(referencePath+File.separator+projectName+"_Label.csv");
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

			//save csv
			String resultCSVPath = referencePath+File.separator+devID+"_Online.csv";
			BufferedWriter writer = new BufferedWriter(new FileWriter( new File(resultCSVPath)));
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("isBuggy","Modify Lines","Add Lines","Delete Lines","Distribution modified Lines","numOfBIC","AuthorID","fileAge","SumOfSourceRevision","SumOfDeveloper","CommitHour","CommitDate","AGE","numOfSubsystems","numOfDirectories","numOfFiles","NUC","developerExperience","REXP","SEXP","LT","commitTime","Key"));

			for (CSVRecord record : records) {
				Metrics metrics = new Metrics(record);
				String key = metrics.getKey();

				for(String commitHash : trCommitHash) {
					if(key.startsWith(commitHash)) {
						csvPrinter.printRecord(metrics.getIsBuggy(),metrics.getModify_Lines(),metrics.getAdd_Lines(),metrics.getDelete_Lines(),metrics.getDistribution_modified_Lines(),metrics.getNumOfBIC(),metrics.getAuthorID(),metrics.getFileAge(),metrics.getSumOfSourceRevision(),metrics.getSumOfDeveloper(),metrics.getCommitHour(),metrics.getCommitDate(),metrics.getAGE(),metrics.getNumOfSubsystems(),metrics.getNumOfDirectories(),metrics.getNumOfFiles(),metrics.getNUC(),metrics.getDeveloperExperience(),metrics.getREXP(),metrics.getSEXP(),metrics.getLT(),metrics.getCommitTime(),key);
						break;
					}
				}
			}
			writer.close();
			csvPrinter.close();
			return resultCSVPath;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setReferencePath(String referencePath) {
		this.referencePath = referencePath;
	}

	public void setRunDates(ArrayList<RunDate> runDates) {
		this.runDates = runDates;
	}

	public ClusterEvaluation getEval() {
		return eval;
	}

	public void setEval(ClusterEvaluation eval) {
		this.eval = eval;
	}


	public void setAccumulate(boolean accumulate) {
		this.accumulate = accumulate;
	}


	public void setWekaOutputPath(String wekaOutputPath) {
		this.wekaOutputPath = wekaOutputPath;
	}


	public void setDefaultLabel(String defaultLabel) {
		this.defaultLabel = defaultLabel;
	}


	public void setDefaultCluster(int defaultCluster) {
		this.defaultCluster = defaultCluster;
	}


	public void setMinCommit(int isMinCommit) {
		this.minimumCommitForProfilingParameter = isMinCommit;
	}
	
	
	
	
}

class RunningData{
	String trS;
	String trE_gapS;
	String gapE_teS;
	String teE;
	
	int numOfCluster;
	
	int trCommit;
	int trDeveloper;
	int allTrDeveloper;
	int teCommit;
	int teDeveloper;


	RunningData(){
		this.trS = null;
		this.trE_gapS = null;
		this.gapE_teS = null;
		this.teE = null;
		this.numOfCluster = 0;
		this.trCommit = 0;
		this.trDeveloper = 0;
		this.teCommit = 0;
		this.teDeveloper = 0;
		this.allTrDeveloper = 0;
	}


	public String getTrS() {
		return trS;
	}


	public void setTrS(String trS) {
		this.trS = trS;
	}


	public String getTrE_gapS() {
		return trE_gapS;
	}


	public void setTrE_gapS(String trE_gapS) {
		this.trE_gapS = trE_gapS;
	}


	public String getGapE_teS() {
		return gapE_teS;
	}


	public void setGapE_teS(String gapE_teS) {
		this.gapE_teS = gapE_teS;
	}


	public String getTeE() {
		return teE;
	}


	public void setTeE(String teE) {
		this.teE = teE;
	}


	public int getNumOfCluster() {
		return numOfCluster;
	}


	public void setNumOfCluster(int numOfCluster) {
		this.numOfCluster = numOfCluster;
	}


	public int getTrCommit() {
		return trCommit;
	}


	public void setTrCommit(int trCommit) {
		this.trCommit = trCommit;
	}


	public int getTrDeveloper() {
		return trDeveloper;
	}


	public void setTrDeveloper(int trDeveloper) {
		this.trDeveloper = trDeveloper;
	}


	public int getTeCommit() {
		return teCommit;
	}


	public void setTeCommit(int teCommit) {
		this.teCommit = teCommit;
	}


	public int getTeDeveloper() {
		return teDeveloper;
	}


	public void setTeDeveloper(int teDeveloper) {
		this.teDeveloper = teDeveloper;
	}


	public int getAllTrDeveloper() {
		return allTrDeveloper;
	}


	public void setAllTrDeveloper(int allTrDeveloper) {
		this.allTrDeveloper = allTrDeveloper;
	}
	

}
