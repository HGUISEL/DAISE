package edu.handong.csee.isel.pdp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import edu.handong.csee.isel.MainDAISE;
import edu.handong.csee.isel.data.ExtractData;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;


public class PDPmain {
	String metadataPath;
	String outputPath;
	boolean verbose;
	boolean help;
	String projectName;
	String referenceFolderPath;
	int numOfTopDeveloper;
	int numOfDeveloper;
	ArrayList<String> keyOfFinalArffFile = new ArrayList<String>();
	
	private final static String firstDeveloperIDPatternStr = ".+\\{'\\s([^,]+)',.+\\}"; 
	private final static Pattern firstDeveloperIDPattern = Pattern.compile(firstDeveloperIDPatternStr);

	private final static String firstcommitTimePatternStr = "'(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)'";
	private final static Pattern firstcommitTimePattern = Pattern.compile(firstcommitTimePatternStr);
	
	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		PDPmain main = new PDPmain();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			numOfTopDeveloper = 10;
			
			//parsing projectName and referenceFoldername
			Pattern pattern = Pattern.compile("(.+)/(.+)-data.arff");
			
			Matcher matcher = pattern.matcher(metadataPath);
			while(matcher.find()) {
				referenceFolderPath = matcher.group(1);
				projectName = matcher.group(2);
			}
			referenceFolderPath = referenceFolderPath+File.separator+projectName+"-reference";
			
			//mk result directory
			File PDPDir = new File(outputPath +File.separator+projectName+"-PDP"+File.separator);
			String directoryPath = PDPDir.getAbsolutePath();
			PDPDir.mkdir();
			
			//init
			ArrayList<String> attributeLineList = new ArrayList<>();
			ArrayList<String> dataLineList = new ArrayList<>();
			HashMap<String,ArrayList<TreeMap<String,ArrayList<String>>>> sortedData = new HashMap<>();  //developer - commitTime, data
			
			//(1) read final arff file and save only PDP metrics (include commitTime data .arff)
			ExtractData.main(extratPDPargs(metadataPath,directoryPath));
			String PDPMetricArffPath = directoryPath+File.separator+projectName+"-data-PDP.arff";
			
			String content = FileUtils.readFileToString(new File(PDPMetricArffPath), "UTF-8");
			String[] lines = content.split("\n");
			
			//use this value when parsing developerID and commitTime in data line
			String firstDeveloperID = null;
			int indexOfDeveloperID = 0;
			String firstCommitTime = null;
			int indexOfCommitTime = 0;
			String firstKey = null;
			int indexOfKey = 0;
			
			boolean dataPart = false;
			for (String line : lines) {
				if (dataPart) {
					dataLineList.add(line);
					continue;

				}else if(!dataPart){
					attributeLineList.add(line);

					if(line.startsWith("@attribute meta_data-AuthorID")) {
						Matcher m = firstDeveloperIDPattern.matcher(line);
						m.find();
						firstDeveloperID = m.group(1);
						indexOfDeveloperID = attributeLineList.size()-3;
					}
					if(line.startsWith("@attribute meta_data-commitTime")) {
						Matcher m = firstcommitTimePattern.matcher(line);
						m.find();
						firstCommitTime = m.group(1);
						indexOfCommitTime = attributeLineList.size()-3;
					}
					if(line.startsWith("@attribute Key {")) {
						Matcher m = firstKeyPattern.matcher(line);
						m.find();
						firstKey = m.group(1);
						indexOfKey = attributeLineList.size()-3;
					}
					if (line.startsWith("@data")) {
						dataPart = true;
					}
				}
			}
			
			//developer - class (for find num of developer commit)
			//number of commit
			//key - data
			//commit time - commit hash : tree map <string, arrayList>
			//save developer - commitTime, data   HashMap<String,ArrayList<TreeMap<String,ArrayList<String>>>> sortedData
			HashMap<String,DeveloperCommit> developerInformation = new HashMap<>();
			
			for(String line : dataLineList) {
				String developerID = parsingDevloperID(line,firstDeveloperID,indexOfDeveloperID);
				String commitTime = parsingCommitTime(line,firstCommitTime,indexOfCommitTime);
				String key = parsingKey(line,firstKey,indexOfKey);
				String commitHash = ParsingCommitHash(key);
				String data = parsingDataLine(line,indexOfCommitTime,indexOfKey);
				DeveloperCommit developerCommit;
				
				if(developerInformation.containsKey(developerID)) {
					developerCommit = developerInformation.get(developerID);
					developerCommit.setCommitHashs(commitHash);
					developerCommit.setCommitTime_key(commitTime, commitHash);
					developerCommit.setKey_data(key, data);
				}else {
					developerCommit = new DeveloperCommit();
					developerCommit.setCommitHashs(commitHash);
					developerCommit.setCommitTime_key(commitTime, commitHash);
					developerCommit.setKey_data(key, data);
					developerInformation.put(developerID, developerCommit);
				}
			}
			
			//save total number of developer
			numOfDeveloper = developerInformation.size();
			
			TreeMap<Integer,ArrayList<String>> numOfCommit_developer = new TreeMap<>(Collections.reverseOrder());
			Set<Map.Entry<String, DeveloperCommit>> entries = developerInformation.entrySet();
			
			for (Map.Entry<String,DeveloperCommit> entry : entries) {
				String developerID = entry.getKey();
				int NumOfcommit = entry.getValue().getContCommit();
				ArrayList<String> developerIDs;
				
				if(numOfCommit_developer.containsKey(NumOfcommit)) {
					developerIDs = numOfCommit_developer.get(NumOfcommit);
					developerIDs.add(developerID);
				}else {
					developerIDs = new ArrayList<String>();
					developerIDs.add(developerID);
					numOfCommit_developer.put(NumOfcommit, developerIDs);
				}
			}
			
			ArrayList<String> topTenDeveloper = selectTopTenDeveloper(numOfCommit_developer);
			
			HashMap<String,ArrayList<String>> topTendeveloper_100commitHashInstances = selectData(developerInformation,topTenDeveloper); 
			
			
			
			//Save top ten developer arff file
			File developerArff = new File(directoryPath+File.separator+"developerArff");
			String arffDirectoryPath = developerArff.getAbsolutePath();
			developerArff.mkdir();
			
			for(String developerID : topTendeveloper_100commitHashInstances.keySet()) {
				File newDeveloperArff = new File(arffDirectoryPath +File.separator+ developerID+".arff");
				StringBuffer newContentBuf = new StringBuffer();
				
				//write attribute
				for (String line : attributeLineList) {
					if(line.startsWith("@attribute meta_data-commitTime")) continue;
					if(line.startsWith("@attribute Key {")) continue;
					newContentBuf.append(line + "\n");
				}
				
				for(String data : topTendeveloper_100commitHashInstances.get(developerID)) {
					newContentBuf.append(data + "\n");
				}
				
				FileUtils.write(newDeveloperArff, newContentBuf.toString(), "UTF-8");
			}
			
			System.out.println("Success saveing arff file of Top 10 developer features");
			
			//PBDP
			System.out.println("All Developer : " + numOfDeveloper);
			HashMap<Integer,ArrayList<String>> cluster_developer;
			
			while(true) {
				System.out.println("The number of Top Developer : " + numOfTopDeveloper);
				keyOfFinalArffFile.clear();
				
				ArrayList<String> temp1 = selectTopTenDeveloper(numOfCommit_developer);
				
				HashMap<String,ArrayList<String>> temp2 = selectData(developerInformation,temp1); 
				//verify the number of developer cluster, if there are less than one, plus 5 developers 10->15->20->...-> NumOfDeveloper
				cluster_developer = clusteringDeveloper(keyOfFinalArffFile);
				
				//만약 cluster 수가 1이하면, +5를 해서 반복한다. 
				if(cluster_developer == null) {
					numOfTopDeveloper += 5; //1?
					if(numOfTopDeveloper >= numOfDeveloper) {
						numOfTopDeveloper = numOfDeveloper;
						numOfDeveloper = 0;
						continue;
					}
					if(numOfDeveloper == 0) {
						System.out.println("There is no more one cluster...");
						System.exit(0);
					}
				}else {
					break;
				}
			}
			
			//save the result
			File PDPdeveloperArff = new File(directoryPath+File.separator+"PDPdeveloperArff");
			String PDParffDirectoryPath = PDPdeveloperArff.getAbsolutePath();
			PDPdeveloperArff.mkdir();
			
			
			for(int cluster : cluster_developer.keySet()) {
				ArrayList<String> devloperList = cluster_developer.get(cluster);
				HashMap<String,ArrayList<String>> developerList_100commitHashInstances = selectData(developerInformation,devloperList); 
				//developer who include in this cluser
				File newDeveloperArff = new File(PDParffDirectoryPath +File.separator+"PDP_" + cluster + ".arff");
				StringBuffer newContentBuf = new StringBuffer();
				
				//write attribute
				for (String line : attributeLineList) {
					if(line.startsWith("@attribute meta_data-commitTime")) continue;
					if(line.startsWith("@attribute Key {")) continue;
					newContentBuf.append(line + "\n");
				}
				
				for(String developerID : developerList_100commitHashInstances.keySet()) {
					for(String data : developerList_100commitHashInstances.get(developerID)) {
						newContentBuf.append(data + "\n");
					}
				}
				
				FileUtils.write(newDeveloperArff, newContentBuf.toString(), "UTF-8");
			}
			
			
			//
			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
	}
	
	private int countNumberOfCluster(String clustereddata) {
		String[] lines = clustereddata.split("\n");
		String numOfClusterStr = lines[4];
		int numOfCluster = Integer.parseInt(numOfClusterStr.substring(numOfClusterStr.lastIndexOf(":") + 2, numOfClusterStr.length()));
		
		return numOfCluster;
	}

	private HashMap<Integer,ArrayList<String>> clusteringDeveloper(ArrayList<String> keyOfFinalArffFile) throws Exception {
		
		//clustering top developer :
		//read meta data csv file and save only 100commits
		String profilingMetadatacsvPath = makeCsvFile_HundredCommitFromTopTenDeveloper(keyOfFinalArffFile);
		File developerProfiling = new File(collectingDeveloperProfilingMetrics(profilingMetadatacsvPath));
		
			//clustering developer using weka EM algorithm
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
//		System.out.println(eval.clusterResultsToString()); //weka처럼 clustering 결과 볼 수 있
//		System.out.println();
		
		int numOfCluster = countNumberOfCluster(eval.clusterResultsToString());
		
		if(numOfCluster <= 1) {
			return null;
		}
		
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
		
		HashMap<Integer,ArrayList<String>> cluster_developer = new HashMap<>();

		for (Instance inst : newData) {
			int developerNameIndex = developerInstanceCSV.indexOf(inst.toString());
			String developerID = developerNameCSV.get(developerNameIndex);
			int cluster = em.clusterInstance(inst);
			ArrayList<String> developerList;
			System.out.println(developerID);
			System.out.println(cluster);
			
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

	private String collectingDeveloperProfilingMetrics(String path) throws Exception {
		String[] DAISEargs = new String[4];

		DAISEargs[0] = "-m";
		DAISEargs[1] = path;
		DAISEargs[2] = "-o";
		DAISEargs[3] = referenceFolderPath.toString();

		MainDAISE DAISEmain = new MainDAISE();
		DAISEmain.run(DAISEargs);
		return DAISEmain.getOutpuCSV();
	}

	private String makeCsvFile_HundredCommitFromTopTenDeveloper(ArrayList<String> keyOfFinalArffFile) {
			
		try {
			//read csv
			Reader in = new FileReader(referenceFolderPath+File.separator+projectName+"_Label.csv");
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
			
			//save csv
			String resultCSVPath = referenceFolderPath+File.separator+projectName+"_PDP.csv";
			BufferedWriter writer = new BufferedWriter(new FileWriter( new File(resultCSVPath)));
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("isBuggy","Modify Lines","Add Lines","Delete Lines","Distribution modified Lines","numOfBIC","AuthorID","fileAge","SumOfSourceRevision","SumOfDeveloper","CommitHour","CommitDate","AGE","numOfSubsystems","numOfDirectories","numOfFiles","NUC","developerExperience","REXP","SEXP","LT","commitTime","Key"));

			for (CSVRecord record : records) {
				Metrics metrics = new Metrics(record);
				String key = metrics.getKey();
				if(keyOfFinalArffFile.contains(key)) {
					csvPrinter.printRecord(metrics.getIsBuggy(),metrics.getModify_Lines(),metrics.getAdd_Lines(),metrics.getDelete_Lines(),metrics.getDistribution_modified_Lines(),metrics.getNumOfBIC(),metrics.getAuthorID(),metrics.getFileAge(),metrics.getSumOfSourceRevision(),metrics.getSumOfDeveloper(),metrics.getCommitHour(),metrics.getCommitDate(),metrics.getAGE(),metrics.getNumOfSubsystems(),metrics.getNumOfDirectories(),metrics.getNumOfFiles(),metrics.getNUC(),metrics.getDeveloperExperience(),metrics.getREXP(),metrics.getSEXP(),metrics.getLT(),metrics.getCommitTime(),key);
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

	private HashMap<String, ArrayList<String>> selectData(
			HashMap<String, DeveloperCommit> developerInformation, ArrayList<String> topTenDeveloper) {
		HashMap<String,ArrayList<String>> topTendeveloper_100commitHashData = new HashMap<>();
		
		for(String developerID : topTenDeveloper) {
			DeveloperCommit developerCommit = developerInformation.get(developerID);
			
			TreeMap<String,ArrayList<String>> commitTime_commitHash = developerCommit.getCommitTime_key();
			HashMap<String,String> key_data = developerCommit.getKey_data();
			
			ArrayList<String> hundredCommitHash = selecthundredCommitHash(commitTime_commitHash);
			
			ArrayList<String> data = new ArrayList<String>();
			for(String commitHash : hundredCommitHash) {
				Set<String> keys = key_data.keySet();
				for(String key : keys) {
					if(key.startsWith(commitHash)) {
						data.add(key_data.get(key));
						keyOfFinalArffFile.add(key);
					}
				}
			}
			topTendeveloper_100commitHashData.put(developerID, data);
		}
		return topTendeveloper_100commitHashData;
	}

	private ArrayList<String> selecthundredCommitHash(TreeMap<String, ArrayList<String>> commitTime_commitHash) {
		ArrayList<String> hundredCommitHash = new ArrayList<String>();
		Set<Map.Entry<String, ArrayList<String>>> entries = commitTime_commitHash.entrySet();
		
		int num = 0;
		for (Map.Entry<String,ArrayList<String>> entry : entries) {
			ArrayList<String> commitHash = entry.getValue();
			hundredCommitHash.addAll(commitHash);
			num += commitHash.size();
			if(num > 99) {
				if(num != 100) {
					for(; num != 100; num--) {
						hundredCommitHash.remove(hundredCommitHash.size()-1);
					}
				}
				break;
			}
		}
		return hundredCommitHash;
	}

	private ArrayList<String> selectTopTenDeveloper(TreeMap<Integer, ArrayList<String>> numOfCommit_developer) {
		ArrayList<String> topTen = new ArrayList<String>();
		Set<Map.Entry<Integer, ArrayList<String>>> entries = numOfCommit_developer.entrySet();
		
		int num = 0;
		for (Map.Entry<Integer,ArrayList<String>> entry : entries) {
			ArrayList<String> developer = entry.getValue();
			topTen.addAll(developer);
			num += developer.size();
			if(num > numOfTopDeveloper-1) {
				if(num != numOfTopDeveloper) {
					for(; num != numOfTopDeveloper; num--) {
						topTen.remove(topTen.size()-1);
					}
				}
				break;
			}
		}
		return topTen;
	}

	private String ParsingCommitHash(String key) {
		return key.substring(0,key.indexOf("-"));
	}

	private String parsingKey(String line, String firstKey, int indexOfKey) {
		if((line.contains(","+indexOfKey+" "))) {
			String key = line.substring(line.lastIndexOf(Integer.toString(indexOfKey)),line.lastIndexOf("}"));
			key = key.substring(key.lastIndexOf(" ")+1,key.length());
			return key;
		}else {
			return firstKey;
		}
	}

	private String[] extratPDPargs(String metadataPath, String directoryPath) {
		
		String[] extratPDPargs = new String[3];
		extratPDPargs[0] = metadataPath;
		extratPDPargs[1] = directoryPath;
		extratPDPargs[2] = "p";
		
		return extratPDPargs;
	}

	private String rename(String adeveloper) {
		if(adeveloper.startsWith("' ")) {
			adeveloper = adeveloper.substring(2,adeveloper.lastIndexOf("'"));
		}
		return adeveloper;
	}

	private String parsingDataLine(String line, int indexOfCommitTime,int indexOfKey) {
		if((line.contains(","+indexOfKey+" "))) {
			if((line.contains(","+indexOfCommitTime+" "))) { //index previous,index commitTime, index key} 
				line = line.substring(0,line.lastIndexOf(","+indexOfCommitTime));
				line = line + "}";
				return line;
			}else {											//index previous,index key}
				line = line.substring(0,line.lastIndexOf(","+indexOfKey));
				line = line + "}";
				return line;
			}
		}else {
			if((line.contains(","+indexOfCommitTime+" "))) {//index previous,index commitTime} 
				line = line.substring(0,line.lastIndexOf(","+indexOfCommitTime));
				line = line + "}";
				return line;
			}else {											//index previous,index commitTime} 
				return line;
			}
		}
	}

	private String parsingCommitTime(String line, String firstCommitTime, int indexOfCommitTime) {
		if((line.contains(","+indexOfCommitTime+" "))) {
			String commitTime = line.substring(line.lastIndexOf(indexOfCommitTime+" '"),line.lastIndexOf("'"));
			commitTime = commitTime.substring(commitTime.lastIndexOf("'")+1,commitTime.length());
			return commitTime;
		}else {
			return firstCommitTime;
		}
	}

	private String parsingDevloperID(String line, String firstDeveloperID, int indexOfDeveloperID) {

		if((line.contains(","+indexOfDeveloperID+" "))) {
			String developerIDPatternStr = ".+"+indexOfDeveloperID+"\\s([^,]+)";
			Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);
			
			Matcher m = developerIDPattern.matcher(line);
			if(m.find()) {
				return rename(m.group(1));
			}else {
				System.out.println(line);
			}
		}else {
			return rename(firstDeveloperID);
		}
		
		return null;
	}

	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			metadataPath = cmd.getOptionValue("i");
			outputPath = cmd.getOptionValue("o");
			if(outputPath.endsWith(File.separator)) {
				outputPath = outputPath.substring(0, outputPath.lastIndexOf(File.separator));
			}

			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}
	
	private Options createOptions() {
		Options options = new Options();

		// add options by using OptionBuilder
		options.addOption(Option.builder("i").longOpt("metadata.arff")
				.desc("Address of meta data arff file. Don't use double quotation marks")
				.hasArg()
				.argName("URI")
				.required()
				.build());// 필수
//		
//		options.addOption(Option.builder("r").longOpt("referenceFolder")
//				.desc("referenceFolder path. Don't use double quotation marks")
//				.hasArg()
//				.argName("path")
//				.required()
//				.build());

		options.addOption(Option.builder("o").longOpt("output")
				.desc("output path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.required()
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());

		return options;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Collecting developer Meta-data program";
		String footer = "\nPlease report issues at https://github.com/HGUISEL/DAISE/issues";
		formatter.printHelp("DAISE", header, options, footer, true);
	}

}
