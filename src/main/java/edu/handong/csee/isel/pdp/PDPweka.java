package edu.handong.csee.isel.pdp;

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

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.MultiSearch;
import weka.classifiers.meta.multisearch.DefaultEvaluationMetrics;
import weka.classifiers.meta.multisearch.DefaultSearch;
import weka.classifiers.trees.ADTree;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.setupgenerator.MathParameter;
import weka.filters.supervised.instance.SMOTE;
import weka.core.setupgenerator.AbstractParameter;


public class PDPweka {
	
	String inputPath;
	String projectname;
	String output;
	String defaultCluster;
	String type;
	String minimumCommit;
	String totalDeveloper;
	String preprocessedDeveloper;
	String startGap;
	String resultFileName;
	HashMap<String,HashMap<Integer,Integer>> tr_bc_num  ;

	public void main() throws Exception {
		init();

		File dir = new File(inputPath);
		File []fileList = dir.listFiles();

		HashMap<String, ArrayList<ArffInformation>> algorithm_MLresult = new HashMap<>();

		for(File file : fileList) {
			String fileName = file.getName();
			System.out.println(fileName);
			int run = 0;
			if(!fileName.contains("baseline")) {
				run = Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(".")));
			}

			DataSource source = new DataSource(inputPath+File.separator+fileName);
			Instances Data = source.getDataSet();
			Data.setClassIndex(0);
			Instances Trains_smote = Data;

			AttributeStats attStats = Trains_smote.attributeStats(0);

			Pattern pattern = Pattern.compile(".+\\{(\\w+),(\\w+)\\}");
			Matcher m = pattern.matcher(Trains_smote.attribute(0).toString());
			m.find();

			HashMap<Integer,Integer> tr_a = tr_bc_num.get(m.group(1));
			HashMap<Integer,Integer> tr_b = tr_bc_num.get(m.group(2));
			HashMap<Integer,Integer> tr_c = tr_bc_num.get("total");

			int index = 10;
			if(m.group(1).compareTo("buggy") == 0) index = 0;
			else index = 1;

			tr_a.put(run,attStats.nominalCounts[0]);
			tr_b.put(run,attStats.nominalCounts[1]);
			tr_c.put(run,attStats.totalCount);

			ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("adt"));

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
				}else if (algorithm.compareTo("adt") == 0) {
					classifyModel = new ADTree();
				}
				
				//multisearch
				ArrayList<String> multisearchEvaluationNames = new ArrayList<String>(Arrays.asList("Fmeasure"));
				MultiSearch multi_search = new MultiSearch();
				FilteredClassifier f = new FilteredClassifier();
				SMOTE smote = new SMOTE();
				
				MathParameter param = new MathParameter();
				param.setProperty("numOfBoostingIterations");
				param.setMin(2);
				param.setMax(3);
				param.setStep(1);
				param.setExpression("I");
				
				for(String multisearchEvaluationName : multisearchEvaluationNames) {
					SelectedTag tag = null;
					if(multisearchEvaluationName.equals("AUC")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_AUC, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Fmeasure")) {//!
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_FMEASURE, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("MCC")) {//!
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_MATTHEWS_CC, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Precision")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_PRECISION, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Recall")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_RECALL, new DefaultEvaluationMetrics().getTags());
					}
					
					multi_search.setSearchParameters(new AbstractParameter[]{param});
					multi_search.setEvaluation(tag);
					multi_search.setAlgorithm(new DefaultSearch());
					multi_search.setClassifier(classifyModel);
					
					
					multi_search.buildClassifier(Trains_smote);
					
					classifyModel.buildClassifier(Trains_smote);

					Evaluation evaluation = new Evaluation(Trains_smote);

					evaluation.crossValidateModel(classifyModel, Trains_smote, 10, new Random(1));

					ArffInformation arffInformation = new ArffInformation();
					arffInformation.setRun(run);
					arffInformation.setTP(evaluation.numTruePositives(index));
					arffInformation.setFN(evaluation.numFalseNegatives(index));
					arffInformation.setFP(evaluation.numFalsePositives(index));
					arffInformation.setTN(evaluation.numTrueNegatives(index));
					arffInformation.setAUC(evaluation.areaUnderROC(index));

					ArrayList<ArffInformation> MLresult;

					if(algorithm_MLresult.containsKey(algorithm)) {
						MLresult = algorithm_MLresult.get(algorithm);
						MLresult.add(arffInformation);
					}else {
						MLresult = new ArrayList<>();
						MLresult.add(arffInformation);
						algorithm_MLresult.put(algorithm, MLresult);
					}
				}
			}
		}

		save2CSV(algorithm_MLresult);
	}

	private void save2CSV(HashMap<String, ArrayList<ArffInformation>> algorithm_MLresult) {
		try {
			BufferedWriter confusionMatrixWriter;
			File temp = new File(output+File.separator + "PDP_result_"+resultFileName+".csv");
			boolean isFile = temp.isFile();
			BufferedWriter AllconfusionMatrixWriter = new BufferedWriter(new FileWriter(output+File.separator + "PDP_result_"+resultFileName+".csv", true));
			CSVPrinter AllconfusionMatrixcsvPrinter = null;

			if(!isFile) {
				AllconfusionMatrixcsvPrinter = new CSVPrinter(AllconfusionMatrixWriter, CSVFormat.DEFAULT.withHeader("Project","algorithm","type","TP","FN","FP","TN","P","R","F","MCC","bugRatio","NumBuggy","NumClean","minimumCommit","defaultCluster","totalNumDev","NumDev","startGap"));
			}else {
				AllconfusionMatrixcsvPrinter = new CSVPrinter(AllconfusionMatrixWriter, CSVFormat.DEFAULT);
			}

			File outputFoler = new File(output+File.separator + projectname);
			outputFoler.mkdir();
			String outputPath = outputFoler.getAbsolutePath();

			for(String algorithm : algorithm_MLresult.keySet()) {

				confusionMatrixWriter = new BufferedWriter(new FileWriter(outputPath + File.separator + projectname +"_"+algorithm+"_"+type+"_CM.csv"));
				CSVPrinter confusionMatrixcsvPrinter = new CSVPrinter(confusionMatrixWriter, CSVFormat.DEFAULT.withHeader("algorithm","run","TP","FN","FP","TN","precision","recall","fMeasure","MCC","AUC","total","buggy","clean","Ratio(%)"));

				ArrayList<ArffInformation> MLresults = algorithm_MLresult.get(algorithm);

				int total_trs = 0;
				int buggy_trs = 0;
				int clean_trs = 0;
				double TPs = 0;
				double FNs = 0;
				double FPs = 0;
				double TNs = 0;
				int numOfRun = 0;

				for(ArffInformation MLresult : MLresults) {

					int run = MLresult.getRun();

					int total_tr = tr_bc_num.get("total").get(run);
					int buggy_tr = tr_bc_num.get("buggy").get(run);
					int clean_tr = tr_bc_num.get("clean").get(run);
					float ratio_tr = ((float)buggy_tr/(float)total_tr) * 100;

					double TP = MLresult.getTP();
					double FN = MLresult.getFN();
					double FP = MLresult.getFP();
					double TN = MLresult.getTN();
					double precision = TP/(TP + FP);
					double recall = TP/(TP + FN);
					double fMeasure = ((precision * recall)/(precision + recall))*2;
					double AUC = MLresult.getAUC();

					double up = (TP*TN)-(FP*FN);
					double under = (TP + FP) * (TP + FN) * (TN +FP) * (TN+FN);
					double MCC = up/Math.sqrt(under);

					confusionMatrixcsvPrinter.printRecord(algorithm,run,(int)TP,(int)FN,(int)FP,(int)TN,precision,recall,fMeasure,MCC,AUC,total_tr,buggy_tr,clean_tr,ratio_tr);

					total_trs += total_tr;
					buggy_trs += buggy_tr;
					clean_trs += clean_tr;
					TPs += TP;
					FNs += FN;
					FPs += FP;
					TNs += TN;
					numOfRun++;
				}
				double aver_total_trs = ((double)total_trs/(double)numOfRun);
				double aver_buggy_trs = ((double)buggy_trs/(double)numOfRun);

				double ratio_tr = (aver_buggy_trs/aver_total_trs) * 100;

				double precisions = TPs/(TPs + FPs);
				double recalls =  TPs/(TPs + FNs);
				double fMeasures = ((precisions * recalls)/(precisions + recalls))*2;
				double up = (TPs*TNs)-(FPs*FNs);
				double under = (TPs + FPs) * (TPs + FNs) * (TNs +FPs) * (TNs+FNs);
				double MCC = up/Math.sqrt(under);

				AllconfusionMatrixcsvPrinter.printRecord(PDPmain.projectName,algorithm,type,(int)TPs,(int)FNs,(int)FPs,(int)TNs,precisions,recalls,fMeasures,MCC,ratio_tr,buggy_trs,clean_trs,minimumCommit,defaultCluster,totalDeveloper,preprocessedDeveloper,startGap);

				confusionMatrixcsvPrinter.close();
				confusionMatrixWriter.close();
			}

			AllconfusionMatrixcsvPrinter.close();
			AllconfusionMatrixWriter.close();



		}catch(IOException e) {
			e.printStackTrace();
		}

	}

	private void init() {
		tr_bc_num = new HashMap<>();
		tr_bc_num.put("buggy", new HashMap<Integer,Integer>());
		tr_bc_num.put("clean", new HashMap<Integer,Integer>());
		tr_bc_num.put("total", new HashMap<Integer,Integer>());

	}

	protected String getInputPath() {
		return inputPath;
	}

	protected void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	protected String getProjectname() {
		return projectname;
	}

	protected void setProjectname(String projectname) {
		this.projectname = projectname;
	}

	protected String getOutput() {
		return output;
	}

	protected void setOutput(String output) {
		this.output = output;
	}

	protected String getDefaultCluster() {
		return defaultCluster;
	}

	protected void setDefaultCluster(String defaultCluster) {
		this.defaultCluster = defaultCluster;
	}

	protected String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	protected String getMinimumCommit() {
		return minimumCommit;
	}

	protected void setMinimumCommit(String minimumCommit) {
		this.minimumCommit = minimumCommit;
	}

	protected String getTotalDeveloper() {
		return totalDeveloper;
	}

	protected void setTotalDeveloper(String totalDeveloper) {
		this.totalDeveloper = totalDeveloper;
	}

	protected String getPreprocessedDeveloper() {
		return preprocessedDeveloper;
	}

	protected void setPreprocessedDeveloper(String preprocessedDeveloper) {
		this.preprocessedDeveloper = preprocessedDeveloper;
	}

	protected String getStartGap() {
		return startGap;
	}

	protected void setStartGap(String startGap) {
		this.startGap = startGap;
	}

	protected String getResultFileName() {
		return resultFileName;
	}

	protected void setResultFileName(String resultFileName) {
		this.resultFileName = resultFileName;
	}


}

class ArffInformation{
	int run;
	double TP;
	double FN;
	double FP;
	double TN;
	double AUC;
	double numOfBuggy;
	double numOfClean;

	public ArffInformation() {
		this.run = 0;
		this.TP = 0;
		this.FN = 0;
		this.FP = 0;
		this.TN = 0;
		this.numOfBuggy = 0;
		this.numOfClean = 0;
		this.AUC = 0;
	}

	public int getRun() {
		return run;
	}



	public void setRun(int run) {
		this.run = run;
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

	public double getAUC() {
		return AUC;
	}

	public void setAUC(double aUC) {
		AUC = aUC;
	}

	public double getNumOfBuggy() {
		return numOfBuggy;
	}

	public void setNumOfBuggy(double numOfBuggy) {
		this.numOfBuggy = numOfBuggy;
	}

	public double getNumOfClean() {
		return numOfClean;
	}

	public void setNumOfClean(double numOfClean) {
		this.numOfClean = numOfClean;
	}
}
