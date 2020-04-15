package edu.handong.csee.isel.cluster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.explorer.ClustererPanel;
import weka.gui.visualize.PlotData2D;

public class MainCluster {
	String dataPath;
	String metadataArffPath;
	String outputPath;
	boolean verbose;
	boolean help;
	
	String head = "";
	ArrayList<String> instances = new ArrayList<String>();
	ArrayList<String> developer = new ArrayList<String>();
	ArrayList<Integer> cluster = new ArrayList<Integer>();
	ArrayList<Integer> numOfInstance = new ArrayList<Integer>();
	TreeSet<Integer> numOfCluster = new TreeSet<Integer>();
	
//	HashMap<Integer,ClusterInfo> clusteringInformation = new HashMap<Integer,ClusterInfo>();
	
	
	public static void main(String[] args) throws Exception {
		MainCluster main = new MainCluster();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception{
		Options options = createOptions();
		
		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			//(1)weka clustering
			CSVLoader loader = new CSVLoader();
			loader.setSource(new File(dataPath));
			
			Instances data = loader.getDataSet();
			
			ArrayList<String> developerNameCSV = new ArrayList<String>();
			ArrayList<String> developerInstanceCSV = new ArrayList<String>();
			
			for(int i = 0; i < data.numInstances(); i++) {
				Pattern pattern = Pattern.compile("' (.+)',(.+)");
				Matcher matcher = pattern.matcher(data.instance(i).toString());
				while(matcher.find()) {
					developerNameCSV.add(matcher.group(1));
					developerInstanceCSV.add(matcher.group(2));
				}
			}
			
			int[] toSelect = new int[data.numAttributes()-1];
			
			for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) toSelect[i] = j;
			
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndicesArray(toSelect);
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(data);
			Instances newData = Filter.useFilter(data, removeFilter);
			
//			System.out.println(developerClusteringData.get);
			
			Clusterer em = new EM();
			em.buildClusterer(newData);
			
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(em);
			eval.evaluateClusterer(newData);
//			System.out.println(eval.clusterResultsToString()); //weka처럼 clustering 결과 볼 수 있
			
			//(2)parsing developer cluster
			
			 for (Instance inst : newData) {
				 int index = developerInstanceCSV.indexOf(inst.toString()); //cluster number 
				 developer.add(developerNameCSV.get(index));//save developer
				 cluster.add(em.clusterInstance(inst));
				 numOfCluster.add(em.clusterInstance(inst));
				 numOfInstance.add(0);
//		            System.out.println("Instance " + inst + " is assignned to cluster " + (em.clusterInstance(inst))); 
		      } 
//			 System.out.println(numOfCluster.size());
//			 int j = 0;
//			 for (String lien : developer) {
//				 System.out.println(j + "  "+ lien);
//				 
//				 System.out.println(cluster.get(j));
//				 j ++;
//			 }
			 
				
			//(3)read meta data final arff file
				BufferedReader buffReader = new BufferedReader(new FileReader(new File(metadataArffPath)));
				String line;
				boolean isData = false;
				while((line = buffReader.readLine()) != null){
					if(line.startsWith("@data")) {
						head = head + "@data\n";
						isData = true;
						continue;
					}
					if(isData == false) {
						head = head + line+'\n';
					}else {
						instances.add(line);
					}
		        }
				buffReader.close();

			//(4)classify .arff file each cluster
				ArrayList<Integer> removeInstances = new ArrayList<Integer>();
				HashMap<Integer,String> clusterInformation = new HashMap<Integer,String>();
				
				for(int i = 0; i < instances.size(); i++) {
					for(int j = 0; j < developer.size(); j++) {
						if(instances.get(i).contains(developer.get(j))) {
							if(!clusterInformation.containsKey(cluster.get(j))) {
								clusterInformation.put(cluster.get(j), head);
							}
							String information = clusterInformation.get(cluster.get(j));
							information = information + instances.get(i)+ "\n";
							clusterInformation.put(cluster.get(j), information);
							numOfInstance.set(j, numOfInstance.get(j) + 1);
							removeInstances.add(i);
						}
					}
				}
				
//				for(String inst : instances) {
//					System.out.println(inst);
//				}
//				System.out.println(instances.size() - removeInstances.size());
				String last = "";
				for(int i = 0; i < instances.size(); i++) {
					if(!removeInstances.contains(i)) {
						if(!developer.contains(instances.get(i)))
							last = last + instances.get(i)+"\n";
					}
				}
				
				for(int i = 0; i < numOfInstance.size(); i++) {
					if(numOfInstance.get(i)==0) {
						String infortmation = clusterInformation.get(cluster.get(i));
						infortmation = infortmation + last;
						clusterInformation.put(cluster.get(i), infortmation);
					}
				}
				
				
			//(5)save .arff file
				String projectName = null;
				ArrayList<String> clusterName = new ArrayList<String>();
				Pattern pattern = Pattern.compile(".+/(.+)-data.arff");
				Matcher matcher = pattern.matcher(metadataArffPath);
				while(matcher.find()) {
					projectName = matcher.group(1);
				}
				String clusterFolder = outputPath+File.separator+projectName+"-clusters"+File.separator;
				ArrayList<String> clusterPath = new ArrayList<String>();
				File file = new File(clusterFolder);
				file.mkdir();
				
				for(int key : clusterInformation.keySet()) {
					clusterName.add(clusterFolder +"cluster"+key+".arff");
					BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(clusterFolder +"cluster"+key+".arff")));
					String value = clusterInformation.get(key);
					bufferedWriter.write(value);
					bufferedWriter.close();
				}
				
			//(6)apply classify algorithm and save result (optional)
				DataSource source = new DataSource(metadataArffPath);
				Instances clusterData = source.getDataSet();
				clusterData.setClassIndex(0);
				
				AttributeStats attStats = clusterData.attributeStats(0);
				
				Classifier randomForest = new RandomForest();
				randomForest.buildClassifier(clusterData);
				
				Evaluation evaluation = new Evaluation(clusterData);
				evaluation.crossValidateModel(randomForest, clusterData, 10, new Random(1));
				
				BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(clusterFolder + "result-main.txt")));
				String strSummary = evaluation.toSummaryString();
				String detail = evaluation.toClassDetailsString();
				bufferedWriter.write(clusterData.attribute(0).toString());
				bufferedWriter.write("\n");
				bufferedWriter.write(attStats.toString());
				bufferedWriter.write(strSummary);
				bufferedWriter.write(detail);
				bufferedWriter.close();
				
				for(int i = 0; i < clusterName.size(); i++){
					String path = clusterName.get(i);
					source = new DataSource(path);
					clusterData = source.getDataSet();
					clusterData.setClassIndex(0);
					attStats = clusterData.attributeStats(0);
					
					randomForest = new RandomForest();
					randomForest.buildClassifier(clusterData);
					
					evaluation = new Evaluation(clusterData);
					evaluation.crossValidateModel(randomForest, clusterData, 10, new Random(1));
					
					bufferedWriter = new BufferedWriter(new FileWriter(new File(clusterFolder + "result-cluster" + i + ".txt")));
					strSummary = evaluation.toSummaryString();
					detail = evaluation.toClassDetailsString();
					bufferedWriter.write(clusterData.attribute(0).toString());
					bufferedWriter.write("\n");
					bufferedWriter.write(attStats.toString());
					bufferedWriter.write(strSummary);
					bufferedWriter.write(detail);
//					System.out.println(strSummary);
//					System.out.println(detail);
					bufferedWriter.close();
					
					
				}
			
			if(verbose) {
				
				// TODO list all files in the path
				
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
	}

	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);

			dataPath = cmd.getOptionValue("i");
			outputPath = cmd.getOptionValue("o");
			metadataArffPath = cmd.getOptionValue("m");
			verbose = cmd.hasOption("v");
			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	// Definition Stage
	private Options createOptions() {
		Options options = new Options();

		// add options by using OptionBuilder
		options.addOption(Option.builder("i").longOpt("dataPath")
				.desc("Set a path of a directory or a file to display")
				.hasArg()
				.argName("Path name to display")
				.required()
				.build());
		
		options.addOption(Option.builder("o").longOpt("output")
				.desc("Set a path of a directory or a file to display")
				.hasArg()
				.argName("Path name to display")
				.required()
				.build());
		
		options.addOption(Option.builder("m").longOpt("meta data arff")
				.desc("Set a path of a directory or a file to display")
				.hasArg()
				.argName("Path name to display")
				.required()
				.build());

		// add options by using OptionBuilder
		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Display detailed messages!")
				//.hasArg()     // this option is intended not to have an option value but just an option
				.argName("verbose option")
				//.required() // this is an optional option. So disabled required().
				.build());
		
		// add options by using OptionBuilder
		options.addOption(Option.builder("h").longOpt("help")
		        .desc("Help")
		        .build());

		return options;
	}
	
	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Clutering developer";
		String footer ="\nPlease report issues at https://github.com/HGUISEL/DAISE/issues";
		formatter.printHelp("CLIExample", header, options, footer, true);
	}

}
