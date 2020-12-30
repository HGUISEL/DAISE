package edu.handong.csee.isel.online;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
	
	static HashMap<String,ArrayList<Double>> numTruePositives ;
	static HashMap<String,ArrayList<Double>> numFalseNegatives ;
	static HashMap<String,ArrayList<Double>> numFalsePositives ;
	static HashMap<String,ArrayList<Double>> numTrueNegatives ;

	public void main(String[] args) throws Exception {
		init();

		String inputPath = args[0];
		File dir = new File(inputPath);
		File []fileList = dir.listFiles();

		Pattern projectNamePattern = Pattern.compile(".+/(.+)");
		Matcher m = projectNamePattern.matcher(inputPath);
		m.find();
		String projectname = m.group(1);

		String output = args[1] +File.separator + projectname+"_result_online";

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
		HashMap<String,Run> runEvaluationValue = new HashMap<>();

		BufferedWriter confusionMatrixWriter;
		BufferedWriter evaluationValueWriter;
		
		try {
			confusionMatrixWriter = new BufferedWriter(new FileWriter(output+"_CM.csv"));
			CSVPrinter confusionMatrixcsvPrinter = new CSVPrinter(confusionMatrixWriter, CSVFormat.DEFAULT.withHeader("algorithm","run","cluster","total","buggy","clean","Ratio(%)","TP","FN","FP","TN","class"));

			for(int i = 0; i < runs.size(); i++) {
				String run = runs.get(i);
				String cluster = clusters.get(i);
				String Class = classes.get(i);
				int total = bc_num.get("total").get(i);
				int buggy = bc_num.get("buggy").get(i);
				int clean = bc_num.get("clean").get(i);
				float ratio = ((float)buggy/(float)total) * 100;
				double TP = 0;
				double FN = 0;
				double FP = 0;
				double TN = 0;
				
				Run runEV;
				if(runEvaluationValue.containsKey(run)) {
					runEV = runEvaluationValue.get(run);
				}else {
					runEV = new Run();
				}
				
				for(String algorithm : numTruePositives.keySet()) {
					 TP = numTruePositives.get(algorithm).get(i);
					 FN = numFalseNegatives.get(algorithm).get(i);
					 FP = numFalsePositives.get(algorithm).get(i);
					 TN = numTrueNegatives.get(algorithm).get(i);
					 
					confusionMatrixcsvPrinter.printRecord(algorithm,run,cluster,total,buggy,clean,ratio,TP,FN,FP,TN,Class);
					
					runEV.setTP(algorithm, TP, cluster);
					runEV.setFN(algorithm, FN, cluster);
					runEV.setFP(algorithm, FP, cluster);
					runEV.setBuggy(algorithm, buggy, cluster);
					runEV.setClean(algorithm, clean, cluster);
				}
				runEvaluationValue.put(run, runEV);
			}
			confusionMatrixcsvPrinter.close();
			confusionMatrixWriter.close();
			

			evaluationValueWriter = new BufferedWriter(new FileWriter(output+"_EV.csv"));
			CSVPrinter evaluationValuePrinter = new CSVPrinter(evaluationValueWriter, CSVFormat.DEFAULT.withHeader("algorithm","run","total","buggy","clean","Ratio(%)","precision","recall","fMeasure"));			
			double TPS = 0;
			double FNS = 0;
			double FPS = 0;
			int buggys = 0;
			int cleans = 0;
			
			for(String run : runEvaluationValue.keySet()) {
				Run runEV = runEvaluationValue.get(run);
				
				
				for(String algorithm : runEV.getTP().keySet()) {
					double TP = sum(runEV.getTP().get(algorithm));
					double FN = sum(runEV.getFN().get(algorithm));
					double FP = sum(runEV.getFP().get(algorithm));
					int buggy = (int)sum(runEV.getBuggy().get(algorithm));
					int clean = (int)sum(runEV.getClean().get(algorithm));
					int total = buggy + clean;
					float ratio = ((float)buggy/(float)total) * 100;
					
					double precision = TP/(TP + FP);
					double recall = TP/(TP + FN);
					double fMeasure = ((precision * recall)/(precision + recall))*2;
					
					evaluationValuePrinter.printRecord(algorithm,run,total,buggy,clean,ratio,precision,recall,fMeasure);
					TPS += TP;
					FNS += FN;
					FPS += FP;
					buggys += buggy;
					cleans += cleans;
				}
			}	
			
			int total = buggys + cleans;
			float ratio = ((float)buggys/(float)total) * 100;
			double precision = TPS/(TPS + FPS);
			double recall = TPS/(TPS + FNS);
			double fMeasure = ((precision * recall)/(precision + recall))*2;
			
			evaluationValuePrinter.printRecord("None","Total_Run",total,buggys,cleans,ratio,precision,recall,fMeasure);

			evaluationValuePrinter.close();
			evaluationValueWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static double sum(ArrayList<Double> arrayList) {
		double sum = 0;
		 
		for(double i : arrayList) {
			sum += i;
		}

		return sum;
	}
	private static void init() {
		bc_num = new HashMap<>();
		bc_num.put("buggy", new ArrayList<Integer>());
		bc_num.put("clean", new ArrayList<Integer>());
		bc_num.put("total", new ArrayList<Integer>());

		numTruePositives = new HashMap<>();
		numFalseNegatives = new HashMap<>();
		numFalsePositives = new HashMap<>();
		numTrueNegatives = new HashMap<>();

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
			System.out.println("run_"+run+"_cluster_"+cluster);
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
			System.out.println();
			//			break;
		}
	}

	private static void classify(String tr_arff, String te_arff, String arffFolder) {
		try {
			// read training set
			DataSource source = new DataSource(arffFolder+File.separator+tr_arff);
			Instances Data = source.getDataSet();
			Data.setClassIndex(0);
			System.out.println(Data.classAttribute());
			classes.add(Data.classAttribute().toString());

			//read test set
			DataSource testSource = new DataSource(arffFolder+File.separator+te_arff);
			Instances testData = testSource.getDataSet();
			testData.setClassIndex(0);
			System.out.println(testData.classAttribute());

			//save num of buggy and clean instance
			AttributeStats attStats = Data.attributeStats(0);

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
			System.out.println("buggy clean index : "+index);

			ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("ibk"));

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

				//save false,,.
				saveValue(numTruePositives, algorithm, evaluation.numTruePositives(index));
				saveValue(numFalseNegatives, algorithm, evaluation.numFalseNegatives(index));
				saveValue(numFalsePositives, algorithm, evaluation.numFalsePositives(index));
				saveValue(numTrueNegatives, algorithm, evaluation.numTrueNegatives(index));

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

	static void saveValue(HashMap<String, ArrayList<Double>> measure, String algorithm, double evaluationValue) {
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

class Run{
	HashMap<String,ArrayList<Double>> TP ;
	  HashMap<String,ArrayList<Double>> FN ;
	  HashMap<String,ArrayList<Double>> FP ;
	  HashMap<String,ArrayList<Double>> TN ;
	  HashMap<String,ArrayList<Double>> buggy ;
	  HashMap<String,ArrayList<Double>> clean ;
	  
	Run(){
		this.TP = new HashMap<>();
		this.FN = new HashMap<>();
		this.FP = new HashMap<>();
		this.TN = new HashMap<>();
		this.buggy = new HashMap<>();
		this.clean = new HashMap<>();
	}

	public HashMap<String, ArrayList<Double>> getTP() {
		return TP;
	}

	public void setTP(String algorithm, double tp, String cluster) {
		saveValue(this.TP, algorithm, tp, cluster);
	}

	public HashMap<String, ArrayList<Double>> getFN() {
		return FN;
	}

	public void setFN(String algorithm, double fn, String cluster) {
		saveValue(this.FN, algorithm, fn, cluster);
	}

	public HashMap<String, ArrayList<Double>> getFP() {
		return FP;
	}

	public void setFP(String algorithm, double fp, String cluster) {
		saveValue(this.FP, algorithm, fp, cluster);
	}
	
	
	public HashMap<String, ArrayList<Double>> getBuggy() {
		return buggy;
	}

	public void setBuggy(String algorithm, double buggy, String cluster) {
		saveValue(this.buggy, algorithm, buggy, cluster);
	}

	public HashMap<String, ArrayList<Double>> getClean() {
		return clean;
	}

	public void setClean(String algorithm, double clean, String cluster) {
		saveValue(this.clean, algorithm, clean, cluster);
	}

	void saveValue(HashMap<String, ArrayList<Double>> measure, String algorithm, double confusionMatrixValue, String cluster) {
		ArrayList<Double> evaluationValues;
		int index = Integer.parseInt(cluster);
		
		if(measure.containsKey(algorithm)) {
			evaluationValues = measure.get(algorithm);
			double value = evaluationValues.get(index);
			value = value + confusionMatrixValue;
			evaluationValues.add(index,value);
		}else {
			evaluationValues = new ArrayList<>();
			double value = confusionMatrixValue;
			evaluationValues.add(index,value);
			measure.put(algorithm, evaluationValues);
		}

	}

}
