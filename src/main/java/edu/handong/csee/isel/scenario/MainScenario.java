package edu.handong.csee.isel.scenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

import edu.handong.csee.isel.MainDAISE;
import edu.handong.csee.isel.data.AccuracyPrinter;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class MainScenario {
	String metadataPath;
	String outputPath;
	boolean accuracy;
	boolean jit;
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
			File testMetricsDeveloperHistory = new File(metadataPath +File.separator+ projectName + "-test-developer-data.arff");

			File trainDeveloperProfiling = new File(collectingDeveloperProfilingMetrics(metadataPath +File.separator + projectName + "_train_developer.csv"));


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

			//(2) - 1 : make each developer cluster model
			ArrayList<String> developer = new ArrayList<String>();
			ArrayList<Integer> cluster = new ArrayList<Integer>();
			TreeSet<Integer> numOfCluster = new TreeSet<Integer>();

			for (Instance inst : newData) {
				int index = developerInstanceCSV.indexOf(inst.toString()); //cluster number 

				developer.add(developerNameCSV.get(index));//save developer
				cluster.add(em.clusterInstance(inst));// save cluster of developer
				numOfCluster.add(em.clusterInstance(inst)); //number of developer each cluster
				//System.out.println("Instance " + inst + " is assignned to cluster " + (em.clusterInstance(inst)));
			}

			if(numOfCluster.size() == 1) {
				File warning = new File(metadataPath +File.separator+projectName+"-OnlyOneCluster");
				System.exit(0);
			}

			//read training arff file
			ArrayList<String> attributeLineList = new ArrayList<String>(); //use again
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
						dataPart = true;
					}
				}
			}

			//divide training data to each cluster group
			HashMap<Integer,ArrayList<String>> clusterInformation = initClusterInformationHashMap(numOfCluster.size()); //cluster number, instance

			for(String line : dataLineList) {
				int key;
				if(!(line.contains(","+indexOfDeveloperID+" "))) {
					key = cluster.get(developer.indexOf(firstDeveloperID));
				}else {
					key = cluster.get(findDeveloperCluster(line,developer));
				}
				ArrayList<String> contents = clusterInformation.get(key);
				contents.add(line);
				clusterInformation.put(key, contents);
			}

			//make each cluster*.arff file
			File clusterDir = new File(metadataPath +File.separator+projectName+"-clusters"+File.separator);
			String directoryPath = clusterDir.getAbsolutePath();
			clusterDir.mkdir();

			ArrayList<String> clusterPath = new ArrayList<String>();

			for(int key : clusterInformation.keySet()) {
				File newCluster = new File(directoryPath +File.separator+ "cluster"+key+".arff");
				clusterPath.add(newCluster.getAbsolutePath());

				StringBuffer newContentBuf = new StringBuffer();
				ArrayList<String> contents = clusterInformation.get(key);

				for (String line : attributeLineList) {
					newContentBuf.append(line + "\n");
				}
				for (String line : contents) {
					newContentBuf.append(line + "\n");
				}

				FileUtils.write(newCluster, newContentBuf.toString(), "UTF-8");
			}


			//read test set (defect prediction metric arff)
			String content1 = FileUtils.readFileToString(testMetricsDeveloperHistory, "UTF-8");

			HashMap<String, ArrayList<TestSetInfo>> testSet = getTestSetFrom(content1);///commitTime <commithash, dataLine>

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
					//testDeveloperMetrics.put(commitTime, testMetaDataArr);??
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

			for(String commitTime : commitTimes) {//read commitTime last -> recent order

				//make developer metrics csv file
				ArrayList<TestMetaData> contents = testDeveloperMetrics.get(commitTime);
				TreeSet<String> authorID = new TreeSet<>();

				for(int i = 0; i < contents.size(); i++) {
					authorID.add(contents.get(i).getAuthorID());
				}

				for(String aAutorID : authorID) {
					File newFileD = new File(classifyDirPath + File.separator + commitTime + aAutorID + "-developer-metric.csv");
					String developerMetricPath = newFileD.getAbsolutePath();
					ArrayList<TestMetaData> testMetaDataArr;

					if(! (accumulatedTestDeveloperMetrics.containsKey(aAutorID))) {
						testMetaDataArr = new ArrayList<>();
						accumulatedTestDeveloperMetrics.put(aAutorID, testMetaDataArr);
					}

					testMetaDataArr = accumulatedTestDeveloperMetrics.get(aAutorID);
					for(TestMetaData aData : contents) {
						if(aData.getAuthorID().equals(aAutorID))
							testMetaDataArr.add(aData);
					}

					if(testMetaDataArr == null) {
						System.out.println(aAutorID + " commitTime : "+commitTime);
					}

					makedeveloperMetricCSV(testMetaDataArr,developerMetricPath);
					File testDeveloperProfiling = new File(collectingDeveloperProfilingMetrics(developerMetricPath));

					Instances test = makeInstances(testDeveloperProfiling);

					//evealuate cluster test set
					eval.evaluateClusterer(test);//test set을 cluster로 분류한 것 !!!!
					//System.out.println(eval.clusterResultsToString());
					Instance test1 = test.get(0);
					
					commitTime_cluster.put(commitTime, em.clusterInstance(test1));
					testDeveloperProfiling.delete();
				}
			}


			////////////////////////////////////////////////////////
			HashMap<String, DBPDResult> reuslts = new HashMap<>(); 


			//apply classify algorithm each cluster
			for(int i = 0; i < clusterPath.size(); i++){
				String path = clusterPath.get(i);
				DataSource source = new DataSource(path);
				Instances clusterData = source.getDataSet();
				clusterData.setClassIndex(0);

				//make machine learning model
				System.out.println("Start classify");
				Classifier randomForest = new RandomForest();
				randomForest.buildClassifier(clusterData);
				System.out.println("End classify");

				//make test arff file
				ArrayList<String> clusterCommitTime = getKey(commitTime_cluster,i);

				for(String eachCommitTime : clusterCommitTime) {

					ArrayList<TestSetInfo> commitTimeTestSets = testSet.get(eachCommitTime);
					if(commitTimeTestSets == null) continue;
					for(TestSetInfo commitTimeTestSet : commitTimeTestSets) { //commitHash,arff
						String aTestData = commitTimeTestSet.getData();
						String key = commitTimeTestSet.getCommitHashSource();

						System.out.println(key);

						File newFileT = new File(classifyDirPath + File.separator + eachCommitTime + "-test-metric.arff");
						String testMetricPath = newFileT.getAbsolutePath();
						StringBuffer newContentBuf = new StringBuffer();
						//print attribute
						for (String line : attributeLineList) {
							newContentBuf.append(line + "\n");
						}
						//print data
						newContentBuf.append(aTestData + "\n");

						FileUtils.write(newFileT, newContentBuf.toString(), "UTF-8");

						//weka test eval
						DataSource testsource = new DataSource(testMetricPath);
						Instances testdata = testsource.getDataSet();
						testdata.setClassIndex(0);
						//						System.out.println("attribute "+testdata.get(0).stringValue(testdata.attribute("@@class@@")));
						//						System.out.println("attribute "+testdata.get(0).stringValue(testdata.attribute("meta_data-AuthorID")));

						Evaluation evalClassify = new Evaluation(clusterData);
						evalClassify.evaluateModel(randomForest, testdata);
						//						System.out.println(evalClassify.toSummaryString("\nResults\n======\n", false));
						//						System.out.println("attStats");
						//						System.out.println(attStats.toString());
						//						System.out.println("toClassDetailsString");
						//						System.out.println(evalClassify.toClassDetailsString());
						//						System.out.println();

						DBPDResult DBPD = new DBPDResult();
						DBPD.setCommitTime(eachCommitTime);
						DBPD.setRealLabel(testdata.get(0).stringValue(testdata.attribute("@@class@@")));
						DBPD.setAuthorID(testdata.get(0).stringValue(testdata.attribute("meta_data-AuthorID")));
						DBPD.setCorrect(findRealLabel(evalClassify.toSummaryString()));
						DBPD.setCluster(i);
						reuslts.put(key,DBPD);

						newFileT.delete();
					}
				}
			}

			//save result to CSV
			String resultCSVPath = Save2CSV(reuslts);

			if(accuracy == true || jit == true) {
				AccuracyPrinter accuracyPrinter = new AccuracyPrinter();

				//Accuracy
				if(accuracy == true) {
					System.out.println("Start Developer Profiling based Defect Prediction");
					accuracyPrinter.setResultCSVPath(resultCSVPath);
					accuracyPrinter.setOutputPath(outputPath);
					accuracyPrinter.setProjectName(projectName);
					accuracyPrinter.calAccuracy();
				}

				//JIT
				if(jit == true) {
					System.out.println("Start Just In Time Defect Prediction");
					String train = metadataPath +File.separator+ projectName + "-train-data.arff";
					String test = metadataPath +File.separator+ projectName + "-test-data.arff";
					accuracyPrinter.setTrain(train);
					accuracyPrinter.setTest(test);
					accuracyPrinter.setOutputPath(outputPath);
					accuracyPrinter.setProjectName(projectName);
					accuracyPrinter.JITdefectPrediction();
				}
			}

			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");

			}

		}
	}

	private HashMap<Integer, ArrayList<String>> initClusterInformationHashMap(int numberOfCluster) {
		HashMap<Integer,ArrayList<String>> clusterInformation = new HashMap<Integer,ArrayList<String>>(); //cluster number, instance

		//init hashmap : num of cluster
		for(int i = 0; i < numberOfCluster; i++) {
			ArrayList<String> contents = new ArrayList<String>();
			clusterInformation.put(i, contents);
		}
		return clusterInformation;
	}

	private String Save2CSV(HashMap<String, DBPDResult> reuslts) throws Exception {
		String resultCSVPath = outputPath+ File.separator + projectName + "-result.csv";
		BufferedWriter writer = new BufferedWriter(new FileWriter( new File(resultCSVPath)));
		CSVPrinter csvPrinter = new CSVPrinter(writer, 
				CSVFormat.DEFAULT.withHeader("Cluster","Key","Commit Time","Author ID","P Label","R Label"));

		Set<Map.Entry<String, DBPDResult>> entries = reuslts.entrySet();

		for (Map.Entry<String,DBPDResult> entry : entries) {
			String key = entry.getKey();
			int cluster = entry.getValue().getCluster();
			String commitTime = entry.getValue().getCommitTime();
			String authorID = entry.getValue().getAuthorID();
			String realLabel = entry.getValue().getRealLabel();
			boolean isCorrect = entry.getValue().isCorrect;
			String predictionLabel = setPredictionLable(realLabel,isCorrect);

			csvPrinter.printRecord(cluster,key,commitTime,authorID,predictionLabel,realLabel);
		}

		csvPrinter.close();

		return resultCSVPath;
	}

	private String setPredictionLable(String realLabel, boolean isCorrect) {
		//		System.out.println("realLabel : " +realLabel + "  isCorrect : "+ isCorrect);
		if(realLabel.equals("clean") && isCorrect == true) {
			return "clean";
		}else if(realLabel.equals("buggy") && isCorrect == true) {
			return "buggy";
		}else if(realLabel.equals("clean") && isCorrect == false) {
			return "buggy";
		}else if(realLabel.equals("buggy") && isCorrect == false) {
			return "clean";
		}
		return "null";
	}

	private boolean findRealLabel(String summaryString) {
		String[] lines = summaryString.split("\n");

		if(lines[1].startsWith("Correctly") && lines[1].contains("100")) {
			return true;
		}else if(lines[1].startsWith("Correctly") && lines[1].contains("0")) {
			return false;
		}else {
			System.out.println("There are no line?");
			return false;
		}
	}

	private ArrayList<String> getKey(HashMap<String, Integer> commitTime_cluster, int i) {
		ArrayList<String> clusterCommitTime = new ArrayList<>();
		Object value = i;

		for (Object key : commitTime_cluster.keySet()) {
			if (commitTime_cluster.get(key).equals(value)) {
				clusterCommitTime.add(key.toString());
			}
		}
		return clusterCommitTime;
	}

	private Instances makeInstances(File testDeveloperProfiling) throws Exception {
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

			accuracy = cmd.hasOption("a");
			jit = cmd.hasOption("j");
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

	HashMap<String, ArrayList<TestSetInfo>> getTestSetFrom(String content){
		HashMap<String, ArrayList<TestSetInfo>> testSet = new HashMap<>();

		String[] lines = content.split("\n");
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

			TestSetInfo testSetInfo = new TestSetInfo(commitHashSource,line);

			if(testSet.containsKey(commitTime)) {
				ArrayList<TestSetInfo> testSetInfos = testSet.get(commitTime);
				testSetInfos.add(testSetInfo);
			}else {
				ArrayList<TestSetInfo> testSetInfos = new ArrayList<>();
				testSetInfos.add(testSetInfo);
				testSet.put(commitTime, testSetInfos);
			}
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

		options.addOption(Option.builder("a").longOpt("accuracy")
				.desc("Accuracy of developer based defect prediction")
				.argName("accuracy")
				.build());

		options.addOption(Option.builder("j").longOpt("JustInTime")
				.desc("Accuracy of just in time defect prediction")
				.argName("JustInTime")
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
