package edu.handong.csee.isel.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import edu.handong.csee.isel.MainDAISE;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
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
			File testMetrics = new File(metadataPath +File.separator+ projectName + "-test-data.arff");
			File testMetricsDeveloperHistory = new File(metadataPath +File.separator+ projectName + "-test-developer-data.arff");
			
			File trainDeveloperProfiling = new File(collectingDeveloperProfilingMetrics("train"));
			File testDeveloperProfiling = new File(metadataPath +File.separator+ "_test_developer.csv");
		
			
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
			
			//(2) - 1 : make each developer cluster model
			ArrayList<String> developer = new ArrayList<String>();
			ArrayList<Integer> cluster = new ArrayList<Integer>();
			TreeSet<Integer> numOfCluster = new TreeSet<Integer>();
			int lengthOfInstance = 0;
			
			for (Instance inst : newData) {
				 int index = developerInstanceCSV.indexOf(inst.toString()); //cluster number 
				 
				 developer.add(developerNameCSV.get(index));//save developer
				 cluster.add(em.clusterInstance(inst));// save cluster of developer
				 numOfCluster.add(em.clusterInstance(inst)); //number of developer each cluster
				 lengthOfInstance++;
//		         System.out.println("Instance " + inst + " is assignned to cluster " + (em.clusterInstance(inst)));
			}
			 
			//read training arff file
			ArrayList<String> attributeLineList = new ArrayList<String>();
			ArrayList<String> dataLineList = new ArrayList<String>();
			String firstDeveloperID = null;
			int indexOfDeveloperID = 0;
			
			String content = FileUtils.readFileToString(trainMetrics, "UTF-8");
			String[] lines = content.split("\n");

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
						indexOfDeveloperID = attributeLineList.size() - 3;
					}
					if (line.startsWith("@data")) {
						attributeLineList.add("\n@data");
						dataPart = true;
					}
				}
			}
			
			//divide training data to each cluster group
			HashMap<Integer,String> clusterInformation = new HashMap<Integer,String>(); //cluster number, instance
			
//			for(int i = 0; i < dataLineList.size(); i++) {
//				System.out.println(dataLineList.get(i));
//				
//				String developerID = dataLineList.get(i).substring(,);
//				break;
//			}
			
			for(String line : dataLineList) {
				if(!(line.contains(","+indexOfDeveloperID+" "))) {
					int key = cluster.get(developer.indexOf(firstDeveloperID));
					clusterInformation.put(key, line);
				}else {
					int key = cluster.get(findDeveloperCluster(line,developer));
					clusterInformation.put(key, line);
				}
			}
			
			//make each cluster*.arff file
//			File clusterDir = new File(outputPath+File.separator+projectName+"-clusters"+File.separator);
//			clusterDir.mkdir();
//			
//			ArrayList<String> clusterName = new ArrayList<String>();
//			
//			 
//			File newCluster = new File(referencePath + File.separator + projectName +"-train-data.arff");
//			
			
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
	
	private String collectingDeveloperProfilingMetrics(String mode) throws Exception {
		String[] DAISEargs = new String[4];
		
		if(mode.equals("train")){
			DAISEargs[0] = "-m";
			DAISEargs[1] = metadataPath +File.separator + projectName + "_train_developer.csv";
			DAISEargs[2] = "-o";
			DAISEargs[3] = metadataPath.toString();
		}else if (mode.equals("test")){
			DAISEargs[0] = "-m";
			DAISEargs[1] = metadataPath +File.separator+ projectName + "_test_developer.csv";
			DAISEargs[2] = "-o";
			DAISEargs[3] = metadataPath.toString();
		}
		
		MainDAISE DAISEmain = new MainDAISE();
		DAISEmain.run(DAISEargs);
		return DAISEmain.getOutpuCSV();
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
