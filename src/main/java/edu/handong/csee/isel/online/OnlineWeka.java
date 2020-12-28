package edu.handong.csee.isel.online;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

//precistion, recall method call 해서 그 값만 가져오도록!
//resampling... weka 
//op resampling : online 가장 잘나오녀 결과 확인 
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class OnlineWeka {
	/*
	 * args[0] : arff folder path
	 * args[1] : result path
	 */
	static ArrayList<String> runs ;
	static ArrayList<String> clusters ;
	static ArrayList<String> classes ;
	static HashMap<String,ArrayList<Integer>> bc_num  ;
	static HashMap<String,ArrayList<Double>> precision ;
	static HashMap<String,ArrayList<Double>> recall ;
	static HashMap<String,ArrayList<Double>> fMeasure ;
	static HashMap<String,ArrayList<Double>> mcc ;
	
	public static void main(String[] args) throws Exception {
		init();
		
		String inputPath = args[0];
		File dir = new File(inputPath);
		File []fileList = dir.listFiles();
		
		Pattern projectNamePattern = Pattern.compile(".+/(.+)");
		Matcher m = projectNamePattern.matcher(inputPath);
		m.find();
		String projectname = m.group(1);
		
		String output = args[1] +File.separator + projectname+"_result_online.csv";
		
		ArrayList<String> fileName = new ArrayList<>();
		for(File file : fileList) {
			fileName.add(file.getName());
		}
				
		if(!projectname.endsWith("PBDP")) {
			onlineBaseLine(fileName,args[0]);
		}else {
			onlinePBDP(fileName,args[0]);
		}
		
		makeCSVFile(output);
		
		System.out.println("Finish "+projectname);
	}

	private static void makeCSVFile(String output) {
		
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(output));
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("algorithm","run","cluster","total","buggy","clean","precision","recall","fMeasure","mcc","class"));
			
			
			for(int i = 0; i < runs.size(); i++) {
				String run = runs.get(i);
				String cluster = clusters.get(i);
				String Class = classes.get(i);
				int total = bc_num.get("total").get(i);
				int buggy = bc_num.get("buggy").get(i);
				int clean = bc_num.get("clean").get(i);
				
				for(String algorithm : precision.keySet()) {
					double p = precision.get(algorithm).get(i);
					double r = recall.get(algorithm).get(i);
					double f = fMeasure.get(algorithm).get(i);
					double m = mcc.get(algorithm).get(i);
					csvPrinter.printRecord(algorithm,run,cluster,total,buggy,clean,p,r,f,m,Class);
				}
			}
			csvPrinter.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static void init() {
		bc_num = new HashMap<>();
		bc_num.put("buggy", new ArrayList<Integer>());
		bc_num.put("clean", new ArrayList<Integer>());
		bc_num.put("total", new ArrayList<Integer>());
		
		precision = new HashMap<>();
		recall = new HashMap<>();
		mcc = new HashMap<>();
		fMeasure = new HashMap<>();
		
		runs = new ArrayList<>();
		clusters = new ArrayList<>();
		classes = new ArrayList<>();
	}

	private static void onlinePBDP(ArrayList<String> fileName, String arffFolder) {
		ArrayList<String> finishFileName = new ArrayList<String>();
		
		for(String name : fileName) {
			String[] str = name.split("_");
			String run = null;
			String cluster = null;

			for(int i = 0; i < str.length; i++) {
				run = str[1];
				cluster = str[3];
			}
			
			String tr_arff = "run_"+run+"_cluster_"+cluster+"_tr.arff";
			String te_arff = "run_"+run+"_cluster_"+cluster+"_te.arff";
			System.out.println(tr_arff);
			System.out.println(te_arff);
			System.out.println();
			
			if(!(fileName.contains(tr_arff) && fileName.contains(te_arff))) {
				continue;
			}
			
			if(finishFileName.contains(tr_arff) && finishFileName.contains(te_arff)) {
				continue;
			}
			
			runs.add(run);
			clusters.add(cluster);
			
			classify(tr_arff,te_arff,arffFolder);
			
			finishFileName.add(tr_arff);
			finishFileName.add(te_arff);
//			break;
		}
		
	}

	private static void classify(String tr_arff, String te_arff, String arffFolder) {
		try {
			DataSource source = new DataSource(arffFolder+File.separator+tr_arff);
			Instances Data = source.getDataSet();
			Data.setClassIndex(0);
			System.out.println(Data.classAttribute());
			classes.add(Data.classAttribute().toString());
			
			AttributeStats attStats = Data.attributeStats(0);
			
			DataSource testSource = new DataSource(arffFolder+File.separator+te_arff);
			Instances testData = testSource.getDataSet();
			testData.setClassIndex(0);
//			testData.setClassIndex(testData.numAttributes() - 1);
			System.out.println(testData.classAttribute());
			
			ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("naive","ibk"));
					
			for(String algorithm : algorithms) {
				Classifier classifyModel = null;
				
				if(algorithm.compareTo("random") == 0) {
					classifyModel = new RandomForest();
				}else if(algorithm.compareTo("naive") == 0){
					classifyModel = new NaiveBayes();
				}else if(algorithm.compareTo("j48") == 0){
					classifyModel = new J48();
				}else if(algorithm.compareTo("bayesNet") == 0){
					classifyModel = new BayesNet();
				}else if(algorithm.compareTo("lmt") == 0){
					classifyModel = new LMT();
				}else if (algorithm.compareTo("ibk") == 0) {
					classifyModel = new IBk();
				}else if (algorithm.compareTo("logi") == 0) {
					classifyModel = new Logistic();
				}
				
				classifyModel.buildClassifier(Data);
				
				Evaluation evaluation = new Evaluation(Data);
				
				evaluation.evaluateModel(classifyModel, testData);

				
				//save num of buggy and clean instance
				Pattern pattern = Pattern.compile(".+\\{(\\w+),(\\w+)\\}");
				Matcher m = pattern.matcher(Data.attribute(0).toString());
				m.find();
				
				ArrayList<Integer> a = bc_num.get(m.group(1));
				ArrayList<Integer> b = bc_num.get(m.group(2));
				ArrayList<Integer> c = bc_num.get("total");
				
				int index = 10;
				if(m.group(1).compareTo("buggy") == 0) index = 0;
				else index = 1;
				
				a.add(attStats.nominalCounts[0]);
				b.add(attStats.nominalCounts[1]);
				c.add(attStats.totalCount);
				
				//save recall fscore mcc etc...
				System.out.println(index);
				saveValue(precision, algorithm, evaluation.precision(index));
				saveValue(recall, algorithm, evaluation.recall(index));
				saveValue(fMeasure, algorithm, evaluation.fMeasure(index));
				saveValue(mcc, algorithm, evaluation.matthewsCorrelationCoefficient(index));
				
//				
//				String detail = evaluation.toClassDetailsString();
//				System.out.println("=================================");
//				System.out.println(detail);
//				System.out.println( );
				
//				for(int i = 1; i < Integer.parseInt(args[2])+1; i++) {
//					evaluation.crossValidateModel(classifyModel, Data, 10, new Random(i));
//					
//			//		evaluation.evaluateModel(classifyModel, testData);
//					
//					BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(output +File.separator + projectname + "-" + algorithm + "-" +i+"-10-fold.txt")));
//					
//					String strSummary = evaluation.toSummaryString();
//					String detail = evaluation.toClassDetailsString();
//					
//					bufferedWriter.write(Data.attribute(0).toString());
//					bufferedWriter.write("\n");
//					bufferedWriter.write(attStats.toString());
//					bufferedWriter.write(strSummary);
//					bufferedWriter.write(detail);
//					bufferedWriter.close();
//				}
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void saveValue(HashMap<String, ArrayList<Double>> measure, String algorithm, double evaluationValue) {
		ArrayList<Double> evaluationValues;
		
		if(measure.containsKey(algorithm)) {
			evaluationValues = measure.get(algorithm);
			evaluationValues.add(evaluationValue);
		}else {
			evaluationValues = new ArrayList<>();
			evaluationValues.add(evaluationValue);
			measure.put(algorithm, evaluationValues);
		}
		
	}

	private static void onlineBaseLine(ArrayList<String> fileName, String arffFolder) {

		
		if(!fileName.contains("Project_Information.txt")) System.exit(0);
		
		for(int run = 0; ; run++) {
			if(!(fileName.contains(run+"_tr.arff"))) {
				break;
			}
			System.out.println("run : "+run);
			String tr_arff = run+"_tr.arff";
			String te_arff = run+"_te.arff";
			
			runs.add(Integer.toString(run));
			clusters.add("no");
			
			classify(tr_arff,te_arff,arffFolder);
			
		}
	}
	
}
