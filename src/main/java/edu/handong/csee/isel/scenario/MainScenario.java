package edu.handong.csee.isel.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
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

import edu.handong.csee.isel.DeveloperInfo;
import edu.handong.csee.isel.MainDAISE;
import edu.handong.csee.isel.MetaData;
import edu.handong.csee.isel.Utils;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class MainScenario {
	String metadataPath;
	String outputPath;
	boolean verbose;
	boolean help;
	String projectName;
	
	private final static String developerIDPatternStr = "' (.+)',(.+)";
	private final static Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);
	
	private final static String firstDeveloperIDPatternStr = ".+\\{'\\s([^,]+)',.+\\}"; 
	private final static Pattern firstDeveloperIDPattern = Pattern.compile(firstDeveloperIDPatternStr);
	
	private final static String commitTimePatternStr = ",\\d+\\s'(.+)'";
	private final static Pattern commitTimePattern = Pattern.compile(commitTimePatternStr);
	
	private final static String commitHashPatternStr = ",\\d+\\s(.+)";
	private final static Pattern commitHashPattern = Pattern.compile(commitHashPatternStr);
	
	
	public static void main(String[] args) throws Exception {
		MainScenario main = new MainScenario();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();
		
		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			Pattern pattern = Pattern.compile(".+/(.+)-reference");
			Matcher matcher = pattern.matcher(metadataPath);
			while(matcher.find()) {
				projectName = matcher.group(1);
			}
			
			File trainMetrics = new File(metadataPath +File.separator+ projectName + "-train-data.arff");
			File testMetrics = new File(metadataPath +File.separator+ projectName + "-test-data.arff");//later
			File testMetricsDeveloperHistory = new File(metadataPath +File.separator+ projectName + "-test-developer-data.arff");
			
			File trainDeveloperProfiling = new File(collectingDeveloperProfilingMetrics(metadataPath +File.separator + projectName + "_train_developer.csv"));
			
			//(1) standard : make prediction model using training data set
			
			//(1) - 1 : test (1)'s prediction model using test data set
			
			
			//(2) make developer cluster(incubator-hivemall_train_developer.csv)
			CSVLoader loader = new CSVLoader();
			loader.setSource(trainDeveloperProfiling);
			
			Instances data = loader.getDataSet();
			
			ArrayList<String> developerNameCSV = new ArrayList<String>(); //developer ID
			ArrayList<String> developerInstanceCSV = new ArrayList<String>(); //All of developer instance 
			
			for(int i = 0; i < data.numInstances(); i++) {
				Matcher m = developerIDPattern.matcher(data.instance(i).toString());
				while(m.find()) {
					developerNameCSV.add(m.group(1));
					developerInstanceCSV.add(m.group(2));
				}
			}
			
//			for(int i = 0; i < developerInstanceCSV.size(); i++) {
//				System.out.println(developerNameCSV.get(i));
//				System.out.println(developerInstanceCSV.get(i));
//			}
//			
			
			//delete developer ID column of CSV file
			int[] toSelect = new int[data.numAttributes()-1];
			
			for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) {
				toSelect[i] = j;
			}
			
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
			System.out.println(eval.clusterResultsToString()); //weka처럼 clustering 결과 볼 수 있
			System.out.println();
////////////////////////////////////////////////////////
			//read test developer metric set
			HashMap<String, ArrayList<TestMetaData>> testDeveloperMetrics = new HashMap<>();///
			HashMap<String, ArrayList<TestMetaData>> accumulatedTestDeveloperMetrics = new HashMap<>();
			TreeSet<String> commitTimes = new TreeSet<String>();///
			
			Reader in = new FileReader(metadataPath+File.separator+projectName+"_test_developer.csv");
	        Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

	        for (CSVRecord record : records) {
	        	String commitTime = record.get("commitTime");
	        	commitTimes.add(commitTime);
	        	TestMetaData testMetaData = new TestMetaData(record);
	        	
	        	if(testDeveloperMetrics.containsKey(commitTime)) {
	        		ArrayList<TestMetaData> testMetaDataArr = testDeveloperMetrics.get(commitTime);
	        		testMetaDataArr.add(testMetaData);
//	        		testDeveloperMetrics.put(commitTime, testMetaDataArr);??
	        	}else {
	        		ArrayList<TestMetaData> testMetaDataArr = new ArrayList<>();
	        		testMetaDataArr.add(testMetaData);
	        		testDeveloperMetrics.put(commitTime, testMetaDataArr);
	        	}
	        }
	        
	        //make test developer metrics for developer profiling
	        HashMap<String, Integer> commitTime_cluster = new HashMap<>();////////////
	        
	        File classifyDir = new File(metadataPath +File.separator+projectName+"-classify"+File.separator);
	        classifyDir.mkdir();
	        String classifyDirPath = classifyDir.getAbsolutePath();

	        for(String commitTime : commitTimes) {
				File newFileD = new File(classifyDirPath + File.separator + commitTime + "-developer-metric.csv");
				
				String developerMetricPath = newFileD.getAbsolutePath();
				
				//make developer metrics csv file
				ArrayList<TestMetaData> testMetaDataArr;
				ArrayList<TestMetaData> contents = testDeveloperMetrics.get(commitTime);
				String authorID = contents.get(0).getAuthorID();
				
				if(! (accumulatedTestDeveloperMetrics.containsKey(authorID))) {
					testMetaDataArr = contents;
					accumulatedTestDeveloperMetrics.put(authorID, testMetaDataArr);
				}else {
					testMetaDataArr = accumulatedTestDeveloperMetrics.get(authorID);
					testMetaDataArr.addAll(contents);
				}
				
				makedeveloperMetricCSV(testMetaDataArr,developerMetricPath);
				File testDeveloperProfiling = new File(collectingDeveloperProfilingMetrics(developerMetricPath));
				
				Instances test = makeInstances(testDeveloperProfiling);
				//evealuate cluster test set
				eval.evaluateClusterer(test);//test set을 cluster로 분류한 것 !!!!
//				System.out.println(eval.clusterResultsToString());
				Instance test1 = test.get(0);
				commitTime_cluster.put(commitTime, em.clusterInstance(test1));
				testDeveloperProfiling.delete();
	        }
	        
	        
			////////////////////////////////////////////////////////
			//(2) - 1 : make each developer cluster model
//			ArrayList<String> developer = new ArrayList<String>();
//			ArrayList<Integer> cluster = new ArrayList<Integer>();
//			TreeSet<Integer> numOfCluster = new TreeSet<Integer>();
//			int lengthOfInstance = 0;
//			
//			for (Instance inst : newData) {
//				 int index = developerInstanceCSV.indexOf(inst.toString()); //cluster number 
//				 
//				 developer.add(developerNameCSV.get(index));//save developer
//				 cluster.add(em.clusterInstance(inst));// save cluster of developer
//				 numOfCluster.add(em.clusterInstance(inst)); //number of developer each cluster
//				 lengthOfInstance++;
////		         System.out.println("Instance " + inst + " is assignned to cluster " + (em.clusterInstance(inst)));
//			}
//			 
//			//read training arff file
//			ArrayList<String> attributeLineList = new ArrayList<String>(); //use again
//			ArrayList<String> dataLineList = new ArrayList<String>();
//			String firstDeveloperID = null;
//			int indexOfDeveloperID = 0;
//			
//			String content = FileUtils.readFileToString(trainMetrics, "UTF-8");
//			String[] lines = content.split("\n");
//
//			boolean dataPart = false;
//			for (String line : lines) {
//				if (dataPart) {
//					dataLineList.add(line);
//					continue;
//					
//				}else if(!dataPart){
//					attributeLineList.add(line);
//					
//					if(line.startsWith("@attribute meta_data-AuthorID")) {
//						Matcher m = firstDeveloperIDPattern.matcher(line);
//						m.find();
//						firstDeveloperID = m.group(1);
//						indexOfDeveloperID = attributeLineList.size() - 3;
//					}
//					if (line.startsWith("@data")) {
//						dataPart = true;
//					}
//				}
//			}
//			
//			//divide training data to each cluster group
//			HashMap<Integer,ArrayList<String>> clusterInformation = new HashMap<Integer,ArrayList<String>>(); //cluster number, instance
//			
//			//init hashmap
//			for(int i = 0; i < numOfCluster.size(); i++) {
//				ArrayList<String> contents = new ArrayList<String>();
//				clusterInformation.put(i, contents);
//			}
//			
//			for(String line : dataLineList) {
//				int key;
//				if(!(line.contains(","+indexOfDeveloperID+" "))) {
//					key = cluster.get(developer.indexOf(firstDeveloperID));
//				}else {
//					key = cluster.get(findDeveloperCluster(line,developer));
//				}
//				ArrayList<String> contents = clusterInformation.get(key);
//				contents.add(line);
//				clusterInformation.put(key, contents);
//			}
//			
//			//make each cluster*.arff file
//			File clusterDir = new File(metadataPath +File.separator+projectName+"-clusters"+File.separator);
//			String directoryPath = clusterDir.getAbsolutePath();
//			clusterDir.mkdir();
//			
//			ArrayList<String> clusterPath = new ArrayList<String>();
//			
//			for(int key : clusterInformation.keySet()) {
//				File newCluster = new File(directoryPath +File.separator+ "cluster"+key+".arff");
//				clusterPath.add(newCluster.getAbsolutePath());
//			
//				StringBuffer newContentBuf = new StringBuffer();
//				ArrayList<String> contents = clusterInformation.get(key);
//				
//				for (String line : attributeLineList) {
//					newContentBuf.append(line + "\n");
//				}
//				for (String line : contents) {
//					newContentBuf.append(line + "\n");
//				}
//				
//				FileUtils.write(newCluster, newContentBuf.toString(), "UTF-8");
//			}
//			
//			
//			//read test set (defect prediction metric arff)
//			String content1 = FileUtils.readFileToString(testMetricsDeveloperHistory, "UTF-8");
//			
//			HashMap<String, HashMap<String,String>> testSet = getTestSetFrom(content1);///
//	        
//			//apply classify algorithm each cluster
//			for(int i = 0; i < clusterPath.size(); i++){
////				String path = clusterPath.get(i);
////				DataSource source = new DataSource(path);
////				Instances clusterData = source.getDataSet();
////				clusterData.setClassIndex(0);
////				
////				AttributeStats attStats = clusterData.attributeStats(0);
////				
////				//make machine learning model
////				System.out.println("Start classify");
////				Classifier randomForest = new RandomForest();
////				randomForest.buildClassifier(clusterData);
//				
//				break;
//				//test set evaluating
////				ClusterEvaluation eval = new ClusterEvaluation();
////				eval.setClusterer(em);
////				eval.evaluateClusterer(newData);
////				System.out.println(eval.clusterResultsToString());
//				
//				
//			}
			
			//(3) test
			
			//(3) - 1 test : all is new developer
			
			// (4) find developer cluster
			// (4) - 1 put a commit information (using commitTime) in developer hashmap
			// (4) - 2 make developer metrics using developer hashmap
			// (4) - 3 put developer metrics to developer cluster model as test case -> can know developer's cluster group
			
			
			//(5) prediction
			//(5) find developers commit ! one commit ! - if using commit time, can find commit using test commit set
			//preprocessing...(remove committime)
			//(5) apply metrics to each cluster model
			//save result commitHash-source / commitTime / developer / prediction label / real label (incubator-hivemall_test_developer.csv)
			
			
			
			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
				
			}
		}
	}

	private Instances makeInstances(File testDeveloperProfiling) throws Exception {
		// TODO Auto-generated method stub
		CSVLoader loader = new CSVLoader();
		loader.setSource(testDeveloperProfiling);
		
		Instances data = loader.getDataSet();
		
		int[] toSelect = new int[data.numAttributes()-1];
		
		for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) {
			toSelect[i] = j;
		}
		
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndicesArray(toSelect);
		removeFilter.setInvertSelection(true);
		removeFilter.setInputFormat(data);
		Instances newData = Filter.useFilter(data, removeFilter);
		
		return newData;
	}

	int findDeveloperCluster(String line, ArrayList<String> developer) {
		int cluster = 100;
		
		for(int i = 0; i < developer.size(); i++) {
			if(line.contains(developer.get(i))) cluster = i;
		}
		return cluster;
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
	
	private String collectingDeveloperProfilingMetrics(String path) throws Exception {
		String[] DAISEargs = new String[4];
		
		DAISEargs[0] = "-m";
		DAISEargs[1] = path;
		DAISEargs[2] = "-o";
		DAISEargs[3] = metadataPath.toString();
		
		MainDAISE DAISEmain = new MainDAISE();
		DAISEmain.run(DAISEargs);
		return DAISEmain.getOutpuCSV();
	}
	
	HashMap<String, HashMap<String,String>> getTestSetFrom(String content){
		HashMap<String, HashMap<String,String>> testSet = new HashMap<String, HashMap<String,String>>();
		
		String[] lines = content.split("\n");
		
		HashMap<String,String> testSetContents = new HashMap<String,String>();
		String commitHashSource;
		String commitTime;
		Matcher m;
		boolean dataPart = false;
		
		for (String line : lines) {
			if(!dataPart) {
				if (line.startsWith("@data")) {
					dataPart = true;
				}
				continue;
			}
			
			commitHashSource = line.substring(line.lastIndexOf(','),line.lastIndexOf('}'));
			m = commitHashPattern.matcher(commitHashSource);
			m.find();
			commitHashSource = m.group(1);
			
			line = line.substring(0, line.lastIndexOf(','));
			
			commitTime = line.substring(line.lastIndexOf(','),line.length());
			m = commitTimePattern.matcher(commitTime);
			m.find();
			commitTime = m.group(1);
			
			line = line.substring(0, line.lastIndexOf(',')) + "}";
			
			testSetContents.put(commitHashSource, line);
			testSet.put(commitTime, testSetContents);
		}
		
		return testSet;
	}
	
    void makedeveloperMetricCSV(ArrayList<TestMetaData> testMetaDataArr,String developerMetricPath) throws Exception {
    	BufferedWriter writer = new BufferedWriter(new FileWriter(developerMetricPath));
    	CSVPrinter csvPrinter = new CSVPrinter(writer, 
    			CSVFormat.DEFAULT.withHeader("isBuggy","Modify Lines","Add Lines","Delete Lines","Distribution modified Lines","numOfBIC","AuthorID","fileAge","SumOfSourceRevision","SumOfDeveloper","CommitHour","CommitDate","AGE","numOfSubsystems","numOfDirectories","numOfFiles","NUC","developerExperience","REXP","LT","Key"));
    	
    	for(TestMetaData content : testMetaDataArr) {
    		String isBuggy = content.getIsBuggy();
    		String Modify_Lines = content.getModify_Lines();
    		String Add_Lines = content.getAdd_Lines();
    		String Delete_Lines = content.getDelete_Lines();
    		String Distribution_modified_Lines = content.getDistribution_modified_Lines();
    		String numOfBIC = content.getNumOfBIC();
    		String AuthorID = content.getAuthorID();
    		String fileAge = content.getFileAge();
    		String SumOfSourceRevision = content.getSumOfSourceRevision();
    		String SumOfDeveloper = content.getSumOfDeveloper();
    		String CommitHour = content.getCommitHour();
    		String CommitDate = content.getCommitDate();
    		String AGE = content.getAGE();
    		String numOfSubsystems = content.getNumOfSubsystems();
    		String numOfDirectories = content.getNumOfDirectories();
    		String numOfFiles = content.getNumOfFiles();
    		String NUC = content.getNUC();
    		String developerExperience = content.getDeveloperExperience();
    		String REXP = content.getREXP();
    		String LT = content.getLT();
    		String Key = content.getKey();
    		
			csvPrinter.printRecord(isBuggy, Modify_Lines,Add_Lines,Delete_Lines,Distribution_modified_Lines,numOfBIC,AuthorID,fileAge,SumOfSourceRevision,SumOfDeveloper,CommitHour,CommitDate,AGE,numOfSubsystems,numOfDirectories,numOfFiles,NUC,developerExperience,REXP,LT,Key);
    	}
    	
    	csvPrinter.close();
    	writer.close();
	}
	
	private Options createOptions() {
		Options options = new Options();

		// add options by using OptionBuilder
		options.addOption(Option.builder("i").longOpt("metadata")
				.desc("Address of meta data csv file. Don't use double quotation marks")
				.hasArg()
				.argName("URI")
				.required()
				.build());// 필수
		
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
