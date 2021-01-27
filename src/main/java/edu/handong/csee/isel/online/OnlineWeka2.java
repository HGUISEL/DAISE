package edu.handong.csee.isel.online;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class OnlineWeka2 {
	/*
	 * args[0] : arff folder path
	 * args[1] : result path
	 * args[2] : defaultCluster number (0 : auto)
	 * args[3] : minimumCommitForProfilingParameter (0 : no mincommit)
	 */
	
	static HashMap<String,RunCluster> algo_RunCluster = new HashMap<>();
	static ArrayList<String> classes ;
	static HashMap<String,ArrayList<Integer>> tr_bc_num  ;
	static HashMap<String,ArrayList<Integer>> te_bc_num  ;
	
	public void main(String[] args) throws Exception {
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
		
		if(projectname.endsWith("PBDP-C"+defaultCluster) || projectname.endsWith("PBDP-M"+minimumCommitForProfilingParameter+"-C"+defaultCluster)) {
			onlinePBDP(fileName,inputPath);
		}else {
//			onlineBaseLine(fileName,args[0]);
		}
	}
	
	private static void onlinePBDP(ArrayList<String> fileName, String inputPath) {
		ArrayList<String> finishClassifyFileNameList = new ArrayList<String>();

		for(String name : fileName) {
			if(!name.endsWith(".arff")) continue;
			
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
			
			//if there is no te or tr file, pass classify
			if(!(fileName.contains(tr_arff) && fileName.contains(te_arff))) {
				continue;
			}
			
			//if the te and tr files are already classify, pass
			if(finishClassifyFileNameList.contains(tr_arff) && finishClassifyFileNameList.contains(te_arff)) {
				continue;
			}
			
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
		classes.add(Data.classAttribute().toString());

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
		System.out.println("buggy clean index : "+index);
		
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

}

class RunCluster{
	double run;
	double cluster;
	double TP;
	double FN;
	double FP;
	double TN;
	
	double precision;
	double recall;
	double fmeasure;
	double auc;
	double mcc;
	
	double TRnumOfBuggy;
	double TRnumOfClean;
	double TEnumOfBuggy;
	double TEnumOfClean;

	public RunCluster() {
		this.run = 0;
		this.cluster = 0;
		this.TP = 0;
		this.FN = 0;
		this.FP = 0;
		this.TN = 0;
		this.precision = 0;
		this.recall = 0;
		this.fmeasure = 0;
		this.auc = 0;
		this.mcc = 0;
		this.TRnumOfBuggy = 0;
		this.TRnumOfClean = 0;
		this.TEnumOfBuggy = 0;
		this.TEnumOfClean = 0;
	}

	public double getRun() {
		return run;
	}

	public void setRun(double run) {
		this.run = run;
	}

	public double getCluster() {
		return cluster;
	}

	public void setCluster(double cluster) {
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

	public double getPrecision() {
		return precision;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public double getRecall() {
		return recall;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	public double getFmeasure() {
		return fmeasure;
	}

	public void setFmeasure(double fmeasure) {
		this.fmeasure = fmeasure;
	}

	public double getAuc() {
		return auc;
	}

	public void setAuc(double auc) {
		this.auc = auc;
	}

	public double getMcc() {
		return mcc;
	}

	public void setMcc(double mcc) {
		this.mcc = mcc;
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
	
}
