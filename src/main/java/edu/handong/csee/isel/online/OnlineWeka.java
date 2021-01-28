package edu.handong.csee.isel.online;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	 * args[2] : defaultCluster number (0 : auto)
	 * args[3] : minimumCommitForProfilingParameter (0 : no mincommit)
	 */

	static HashMap<String,HashMap<String,RunCluster>> algo_RunCluster;

	static ArrayList<String> runs ;
	static ArrayList<String> clusters ;
	static HashMap<String,ArrayList<Integer>> tr_bc_num  ;
	static HashMap<String,ArrayList<Integer>> te_bc_num  ;
	
	static double averCluster;
	static double varCluster;

	public void main(String[] args) throws Exception {
		init();

		String inputPath = args[0];
		String projectname = setProjectName(inputPath); 
		String output = args[1] +File.separator + projectname;
		String defaultCluster = args[2];
		String minimumCommitForProfilingParameter = args[3];

		File dir = new File(inputPath);
		File []fileList = dir.listFiles();

		ArrayList<String> fileName = new ArrayList<>();
		ArrayList<String> trainingArffFileName = new ArrayList<>();

		for(File file : fileList) {
			fileName.add(file.getName());
			if(file.getName().endsWith("tr.arff")) {
				trainingArffFileName.add(file.getName());
			}
		}
		
		calClusterInfo(trainingArffFileName);
		
		if(projectname.endsWith("PBDP-C"+defaultCluster) || projectname.endsWith("PBDP-M"+minimumCommitForProfilingParameter+"-C"+defaultCluster)) {
			onlinePBDP(fileName,inputPath);
		}else {
			averCluster = 0;
			varCluster = 0;
			onlineBaseLine(fileName,inputPath);
		}
		
		makeCSVFile(output,defaultCluster,minimumCommitForProfilingParameter);

		System.out.println("Finish "+projectname);
	}

	private void calClusterInfo(ArrayList<String> trainingArffFileName) {
		HashMap<String,Integer> run_numOfCluster = new HashMap<>();
		
		for(String file : trainingArffFileName) {
			if(file.contains("PBDPbaseline")) continue;
			
			String[] str = file.split("_");
			String run = null;
			String cluster = null;

			run = str[1];
			
			int numOfCluster;
			if(run_numOfCluster.containsKey(run)) {
				numOfCluster = run_numOfCluster.get(run);
				numOfCluster++;
				run_numOfCluster.put(run, numOfCluster);
			}else {
				numOfCluster = 1;
				run_numOfCluster.put(run, numOfCluster);
			}
			
		}
		
		ArrayList list = new ArrayList(run_numOfCluster.values());
		
		averCluster = average(list);
		varCluster = var(list);
		
	}
	
	private static double var(ArrayList<Integer> allClusters) {
		double mean = average(allClusters);
        double temp = 0;
        
        for(double a :allClusters)
            temp += (a-mean)*(a-mean);
        
        return temp/(double)(allClusters.size()-1);
	}
	
	private static double average(ArrayList<Integer> allClusters) {

        return sum(allClusters)/(double)allClusters.size();
	}
	
	private static double sum(ArrayList<Integer> arrayList) {
		int sum = 0;
		 
		for(int i : arrayList) {
			sum += i;
		}

		return (double)sum;
	}

	private static void makeCSVFile(String output, String defaultCluster, String minimumCommitForProfilingParameter) {

		BufferedWriter confusionMatrixWriter;

		try {
			for(String algorithm : algo_RunCluster.keySet()) {

				confusionMatrixWriter = new BufferedWriter(new FileWriter(output+"_"+algorithm+"_CM.csv"));
				CSVPrinter confusionMatrixcsvPrinter = new CSVPrinter(confusionMatrixWriter, CSVFormat.DEFAULT.withHeader("algorithm","run","cluster","TP","FN","FP","TN","precision","recall","fMeasure","MCC","AUC","tr_total","tr_buggy","tr_clean","tr_Ratio(%)","te_total","te_buggy","te_clean","te_Ratio(%)"));


				HashMap<String,RunCluster> runClusters = algo_RunCluster.get(algorithm);
				
				double AUCs = 0;
				double MCCs = 0;
				int total_trs = 0;
				int buggy_trs = 0;
				int total_tes = 0;
				int buggy_tes = 0;
				double TPs = 0;
				double FNs = 0;
				double FPs = 0;
				double TNs = 0;
				int numOfRun = runs.size();
				
				for(int i = 0; i < numOfRun; i++) {
					String key = runs.get(i)+"-"+clusters.get(i);
					RunCluster runCluster = runClusters.get(key);

					String run = runs.get(i);
					String cluster = clusters.get(i);
					int total_tr = tr_bc_num.get("total").get(i);
					int buggy_tr = tr_bc_num.get("buggy").get(i);
					int clean_tr = tr_bc_num.get("clean").get(i);
					float ratio_tr = ((float)buggy_tr/(float)total_tr) * 100;

					int total_te = te_bc_num.get("total").get(i);
					int buggy_te = te_bc_num.get("buggy").get(i);
					int clean_te = te_bc_num.get("clean").get(i);
					float ratio_te = ((float)buggy_te/(float)total_te) * 100;

					double TP = runCluster.getTP();
					double FN = runCluster.getFN();
					double FP = runCluster.getFP();
					double TN = runCluster.getTN();
					double precision = TP/(TP + FP);
					double recall = TP/(TP + FN);
					double fMeasure = ((precision * recall)/(precision + recall))*2;
					double AUC = runCluster.getAUC();
					double up = (TP*TN)-(FP*FN);
					double under = (TP + FP) * (TP + FN) * (TN +FP) * (TN+FN);
					double MCC = up/Math.sqrt(under);
					
					System.out.println();

					confusionMatrixcsvPrinter.printRecord(algorithm,run,cluster,(int)TP,(int)FN,(int)FP,(int)TN,precision,recall,fMeasure,MCC,AUC,total_tr,buggy_tr,clean_tr,ratio_tr,total_te,buggy_te,clean_te,ratio_te);
					
					total_trs += total_tr;
					buggy_trs += buggy_tr;
					total_tes += total_te;
					buggy_tes += buggy_te;
					TPs += TP;
					FNs += FN;
					FPs += FP;
					TNs += TN;
				}
				double aver_total_trs = ((double)total_trs/(double)numOfRun);
				double aver_buggy_trs = ((double)buggy_trs/(double)numOfRun);
				
				double aver_total_tes = ((double)total_tes/(double)numOfRun);
				double aver_buggy_tes = ((double)buggy_tes/(double)numOfRun);
				
				double ratio_tr = (aver_buggy_trs/aver_total_trs) * 100;
				double ratio_te = (aver_buggy_tes/aver_total_tes) * 100;
				
				double precisions = TPs/(TPs + FPs);
				double recalls =  TPs/(TPs + FNs);
				double fMeasures = ((precisions * recalls)/(precisions + recalls))*2;
				double up = (TPs*TNs)-(FPs*FNs);
				double under = (TPs + FPs) * (TPs + FNs) * (TNs +FPs) * (TNs+FNs);
				double MCC = up/Math.sqrt(under);

				confusionMatrixcsvPrinter.printRecord("algorithm","defaultCluster","Mincommit","TP","FN","FP","TN","P","R","F","MCC","tr_ratio","te_ratio","tr-te ratio","averClu","varClu");
				confusionMatrixcsvPrinter.printRecord(algorithm,defaultCluster,minimumCommitForProfilingParameter,(int)TPs,(int)FNs,(int)FPs,(int)TNs,precisions,recalls,fMeasures,MCC,ratio_tr,ratio_te,ratio_tr-ratio_te,averCluster,varCluster);

				confusionMatrixcsvPrinter.close();
				confusionMatrixWriter.close();
			}
			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void init() {
		averCluster = 0;
		varCluster = 0;
		
		algo_RunCluster = new HashMap<>();

		runs = new ArrayList<>();
		clusters = new ArrayList<>();

		tr_bc_num = new HashMap<>();
		tr_bc_num.put("buggy", new ArrayList<Integer>());
		tr_bc_num.put("clean", new ArrayList<Integer>());
		tr_bc_num.put("total", new ArrayList<Integer>());

		te_bc_num = new HashMap<>();
		te_bc_num.put("buggy", new ArrayList<Integer>());
		te_bc_num.put("clean", new ArrayList<Integer>());
		te_bc_num.put("total", new ArrayList<Integer>());
	}

	private static void onlinePBDP(ArrayList<String> fileName, String inputPath) {
		ArrayList<String> finishClassifyFileNameList = new ArrayList<String>();

		for(String name : fileName) {
			if(!name.endsWith(".arff")) continue;
			String[] str = name.split("_");
			String run = null;
			String cluster = null;

			run = str[1];
			cluster = str[3];

			String tr_arff = "run_"+run+"_cluster_"+cluster+"_tr.arff";
			String te_arff = "run_"+run+"_cluster_"+cluster+"_te.arff";
			System.out.println("run_"+run+"_cluster_"+cluster);
			System.out.println();

			//if there is no te or tr file, pass classify
			if(!(fileName.contains(tr_arff) && fileName.contains(te_arff))) {
				System.out.println("pass");
				continue;
			}

			//if the te and tr files are already classify, pass
			if(finishClassifyFileNameList.contains(tr_arff) && finishClassifyFileNameList.contains(te_arff)) {
				continue;
			}

			runs.add(run);
			clusters.add(cluster);

			//classify
			classify(tr_arff,te_arff,inputPath,run,cluster);

			finishClassifyFileNameList.add(tr_arff);
			finishClassifyFileNameList.add(te_arff);
			System.out.println();
		}
	}

	private static void classify(String tr_arff, String te_arff, String inputPath, String run, String cluster) {
		try {
			// read training set
			DataSource source = new DataSource(inputPath+File.separator+tr_arff);
			Instances Data = source.getDataSet();
			Data.setClassIndex(0);

			//read test set
			DataSource testSource = new DataSource(inputPath+File.separator+te_arff);
			Instances testData = testSource.getDataSet();
			testData.setClassIndex(0);

			//save num of buggy and clean instance in training data set
			AttributeStats attStats = Data.attributeStats(0);

			Pattern pattern = Pattern.compile(".+\\{(\\w+),(\\w+)\\}");
			Matcher m = pattern.matcher(Data.attribute(0).toString());
			m.find();

			ArrayList<Integer> tr_a = tr_bc_num.get(m.group(1));
			ArrayList<Integer> tr_b = tr_bc_num.get(m.group(2));
			ArrayList<Integer> tr_c = tr_bc_num.get("total");

			int index = 10;
			if(m.group(1).compareTo("buggy") == 0) index = 0;
			else index = 1;

			tr_a.add(attStats.nominalCounts[0]);
			tr_b.add(attStats.nominalCounts[1]);
			tr_c.add(attStats.totalCount);

			//save num of buggy and clean instance in test data set

			AttributeStats attStats_te = testData.attributeStats(0);

			Pattern pattern_te = Pattern.compile(".+\\{(\\w+),(\\w+)\\}");
			Matcher m_te = pattern_te.matcher(testData.attribute(0).toString());
			m_te.find();

			ArrayList<Integer> te_a = te_bc_num.get(m_te.group(1));
			ArrayList<Integer> te_b = te_bc_num.get(m_te.group(2));
			ArrayList<Integer> te_c = te_bc_num.get("total");

			te_a.add(attStats_te.nominalCounts[0]);
			te_b.add(attStats_te.nominalCounts[1]);
			te_c.add(attStats_te.totalCount);

			ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("ibk"));
			String key = run+"-"+cluster;

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

				//algo_RunCluster
				RunCluster runCluster = new RunCluster();
				runCluster.setRun(Integer.parseInt(run));
				runCluster.setCluster(cluster);
				runCluster.setTP(evaluation.numTruePositives(index));
				runCluster.setFN(evaluation.numFalseNegatives(index));
				runCluster.setFP(evaluation.numFalsePositives(index));
				runCluster.setTN(evaluation.numTrueNegatives(index));
				runCluster.setAUC(evaluation.areaUnderROC(index));

				HashMap<String,RunCluster> runClusters;
				if(algo_RunCluster.containsKey(algorithm)) {
					runClusters = algo_RunCluster.get(algorithm);
					runClusters.put(key, runCluster);
				}else {
					runClusters = new HashMap<>();
					runClusters.put(key, runCluster);
					algo_RunCluster.put(algorithm, runClusters);
				}
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	//	private static void onlineBaseLine(ArrayList<String> fileName, String arffFolder) {
	//
	//		if(!fileName.contains("Project_Information.txt")) System.exit(0);
	//
	//		for(int run = 0; ; run++) {
	//			if(!(fileName.contains(run+"_tr.arff"))) {
	//				break;
	//			}
	//			System.out.println("run : "+run);
	//			String tr_arff = run+"_tr.arff";
	//			String te_arff = run+"_te.arff";
	//
	//			runs.add(Integer.toString(run));
	//			clusters.add("0");
	//
	//			classify(tr_arff,te_arff,arffFolder);
	//
	//		}
	//	}

	private String setProjectName(String inputPath) {
		Pattern projectNamePattern = Pattern.compile(".+/(.+)");
		Matcher m = projectNamePattern.matcher(inputPath);
		m.find();
		return m.group(1);
	}
	
	private static void onlineBaseLine(ArrayList<String> fileName, String inputPath) {

		if(!fileName.contains("Project_Information.txt")) System.exit(0);

		for(int run = 0; ; run++) {
			if(!(fileName.contains(run+"_tr.arff"))) {
				break;
			}
			System.out.println("run : "+run);
			String tr_arff = run+"_tr.arff";
			String te_arff = run+"_te.arff";

			runs.add(Integer.toString(run));
			clusters.add("Nan");

			classify(tr_arff,te_arff,inputPath,Integer.toString(run),"Nan");

		}
	}

}

class RunCluster{
	double run;
	String cluster;
	double TP;
	double FN;
	double FP;
	double TN;
	
	double AUC;

	double TRnumOfBuggy;
	double TRnumOfClean;
	double TEnumOfBuggy;
	double TEnumOfClean;

	public RunCluster() {
		this.run = 0;
		this.cluster = null;
		this.TP = 0;
		this.FN = 0;
		this.FP = 0;
		this.TN = 0;
		this.TRnumOfBuggy = 0;
		this.TRnumOfClean = 0;
		this.TEnumOfBuggy = 0;
		this.TEnumOfClean = 0;
		this.AUC = 0;
				
	}

	public double getRun() {
		return run;
	}

	public void setRun(double run) {
		this.run = run;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public double getTP() {
		return TP;
	}

	public void setTP(double tP) {
		TP = tP;
	}

	public double getFN() {
		return FN;
	}

	public void setFN(double fN) {
		FN = fN;
	}

	public double getFP() {
		return FP;
	}

	public void setFP(double fP) {
		FP = fP;
	}

	public double getTN() {
		return TN;
	}

	public void setTN(double tN) {
		TN = tN;
	}

	public double getTRnumOfBuggy() {
		return TRnumOfBuggy;
	}

	public void setTRnumOfBuggy(double tRnumOfBuggy) {
		TRnumOfBuggy = tRnumOfBuggy;
	}

	public double getTRnumOfClean() {
		return TRnumOfClean;
	}

	public void setTRnumOfClean(double tRnumOfClean) {
		TRnumOfClean = tRnumOfClean;
	}

	public double getTEnumOfBuggy() {
		return TEnumOfBuggy;
	}

	public void setTEnumOfBuggy(double tEnumOfBuggy) {
		TEnumOfBuggy = tEnumOfBuggy;
	}

	public double getTEnumOfClean() {
		return TEnumOfClean;
	}

	public void setTEnumOfClean(double tEnumOfClean) {
		TEnumOfClean = tEnumOfClean;
	}

	public double getAUC() {
		return AUC;
	}

	public void setAUC(double aUC) {
		AUC = aUC;
	}

}
