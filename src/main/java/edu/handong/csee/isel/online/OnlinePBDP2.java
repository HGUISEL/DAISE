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
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class OnlinePBDP2 {
	String outputPath;
	String projectName;
	String referencePath;

	ArrayList<RunDate> runDates;
	
	int minCommit = 0;//mincommit 30 to 100 - option
	int numOfCluster = 0; //option
	
	public void profilingBasedDefectPrediction(ArrayList<String> attributeLineList,
			TreeMap<String,String> key_fixTime,
			TreeMap<String,TreeSet<String>> commitTime_commitHash,
			HashMap<String,HashMap<String,String>> commitHash_key_data,
			HashMap<String,HashMap<String,Boolean>> commitHash_key_isBuggy,
			HashMap<String,String> commitHash_developer) throws Exception {
		System.out.println();
		System.out.println("---------------Start Online PBDP2----------------------");
		System.out.println();
		
		//make PBDP directory
		File PBDPdir = new File(outputPath);
		String directoryPath = PBDPdir.getAbsolutePath();
		PBDPdir.mkdir();
		
		
		if(minCommit == 0) {
			minCommit = 30;
		}
		
		int run = 1;
		for(RunDate runDate : runDates) {
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
			
			//make developerID_commitHashs in training period
			HashMap<String,ArrayList<String>> tr_developerID_commitHashs; //in tr period
			
			tr_developerID_commitHashs = countTheNumOfdeveloperAndCommit(trS, trE_gapS, commitTime_commitHash, commitHash_developer);
			System.out.println("numOfDev in tr peroid : "+tr_developerID_commitHashs.size());
			
			//count the number of dveloper commit tr peroid- descending order in
			TreeMap<Integer,ArrayList<String>> numOfCommit_developer;
			numOfCommit_developer = countTheNumOfDeveloperCommit(tr_developerID_commitHashs);
			
			//pick dev id : dev commit > "minCommit"
			HashMap<Integer,ArrayList<String>> cluster_developerID;
			while(true) {
				//pick dev id : condition : commit > "minCommit"
				ArrayList<String> clusteringDeveloperID = new ArrayList<String>();

				for(int numOfCommit : numOfCommit_developer.keySet()) {
					if(numOfCommit < minCommit) break;
					ArrayList<String> devID = numOfCommit_developer.get(numOfCommit);
					clusteringDeveloperID.addAll(devID);
				}
				System.out.println("Top developer id in tr set: "+clusteringDeveloperID.size());
			
				//count numOfCluster
				//0. save only tr commitHash (top Devloper)
				ArrayList<String>trClusteringCommitHash = saveDeveloperCommitHash(clusteringDeveloperID,tr_developerID_commitHashs);
				//1. make developer profiling data set
				String profilingMetadatacsvPath = makeCsvFileFromTopTenDeveloperInTraining(trClusteringCommitHash);
				if(profilingMetadatacsvPath == null) {
					System.out.println("profilingMetadatacsvPath is null");
					System.exit(0);
				}
				File developerProfiling = new File(collectingDeveloperProfilingMetrics(profilingMetadatacsvPath));
				//2. read weka and apply cluster
				cluster_developerID = clusteringProfilingDeveloper(developerProfiling);
				//3. parsing cluster result
				numOfCluster = cluster_developerID.size();
				
				if(numOfCluster != 1) {
					break;
				}else if(minCommit == 100){
					break;
				}else {
					minCommit++;
				}
			}
			System.out.println("Final minCommit of tr : "+minCommit);
			System.out.println("Final numOfCluster of tr : "+numOfCluster);
			
			//make tr arff file in each clustering
			makeArffFileInEachClustering(cluster_developerID, tr_developerID_commitHashs, attributeLineList, commitHash_key_data, outputPath, run);
			
			run++;
			break;
		}	
		
		
	}

	private void makeArffFileInEachClustering(HashMap<Integer, ArrayList<String>> cluster_developerID,
			HashMap<String, ArrayList<String>> tr_developerID_commitHashs, ArrayList<String> attributeLineList, HashMap<String, HashMap<String, String>> commitHash_key_data, String outputPath, int run) throws Exception {
		
		
		for(int cluster : cluster_developerID.keySet()) {
			File newDeveloperArff = new File(outputPath +File.separator+"run_"+run+"_cluster_"+cluster+"_tr.arff");
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
		Clusterer em = new EM();
		em.buildClusterer(newData);

		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(em);
		eval.evaluateClusterer(newData);
		
		///save developer cluster!
		String developerIDPatternStr = "' (.+)',(.+)";
		Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);

		ArrayList<String> developerNameCSV = new ArrayList<String>(); //developer ID
		ArrayList<String> developerInstanceCSV = new ArrayList<String>(); //All of developer instance 

		for(int i = 0; i < data.numInstances(); i++) {
			Matcher m = developerIDPattern.matcher(data.instance(i).toString());
			while(m.find()) {
				developerNameCSV.add(m.group(1));
				developerInstanceCSV.add(m.group(2));
			}
		}
		
		//save to cluster_developer
		for (Instance inst : newData) {
			int developerNameIndex = developerInstanceCSV.indexOf(inst.toString());
			String developerID = developerNameCSV.get(developerNameIndex);
			int cluster = em.clusterInstance(inst);
			ArrayList<String> developerList;

			if(cluster_developer.containsKey(cluster)) {
				developerList = cluster_developer.get(cluster);
				developerList.add(developerID);
			}else {
				developerList = new ArrayList<>();
				developerList.add(developerID);
				cluster_developer.put(cluster, developerList);
			}
		}
		
		return cluster_developer;
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
			TreeMap<String, TreeSet<String>> commitTime_commitHash, HashMap<String, String> commitHash_developer) {
		
		HashMap<String,ArrayList<String>> tr_developerID_commitHashs = new HashMap<>();
		
		for(String commitTime : commitTime_commitHash.keySet()) {
			if(!(trS.compareTo(commitTime)<=0 && commitTime.compareTo(trE_gapS)<=0))
				continue;

			TreeSet<String> commitHashs = commitTime_commitHash.get(commitTime);
			for(String commitHash : commitHashs) {
				String developerID = commitHash_developer.get(commitHash);

				ArrayList<String> dev_commitHash;
				if(tr_developerID_commitHashs.containsKey(developerID)) {
					dev_commitHash = tr_developerID_commitHashs.get(developerID);
					dev_commitHash.add(commitHash);
				}else {
					dev_commitHash = new ArrayList<String>();
					dev_commitHash.add(commitHash);
					tr_developerID_commitHashs.put(developerID, dev_commitHash);
				}
			}
		}

		return tr_developerID_commitHashs;
	}
	
	

	
	private String makeCsvFileFromTopTenDeveloperInTraining(ArrayList<String> trCommitHash) {

		try {
			//read csv
			Reader in = new FileReader(referencePath+File.separator+"ranger_Label.csv");
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

			//save csv
			String resultCSVPath = referencePath+File.separator+projectName+"_Online.csv";
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

			csvPrinter.close();
			return resultCSVPath;
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

	public void setMinCommit(int minCommit) {
		this.minCommit = minCommit;
	}

	public void setNumOfCluster(int numOfCluster) {
		this.numOfCluster = numOfCluster;
	}
	
	
	
}
