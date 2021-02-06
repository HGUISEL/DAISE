package edu.handong.csee.isel.daise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
import weka.classifiers.meta.MultiSearch;
import weka.classifiers.meta.multisearch.DefaultEvaluationMetrics;
import weka.classifiers.meta.multisearch.DefaultSearch;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;

public class Simple{
	/*
	 * args[0] : arff file path
	 * args[1] : result path
	 * args[2] : evaluation repeat 
	 */

	public static void main(String[] args) throws Exception {
		String arffPath = args[0];
		String output = args[1];
		String projectname = null;

		Pattern pattern = Pattern.compile("(.+)/(.+)-data-kamei.arff");
		Matcher matcher = pattern.matcher(arffPath);
		while(matcher.find()) {
			projectname = matcher.group(2);
		}

		try {
			//make result file
			File temp = new File(output+File.separator + "KAEMI_result.csv");
			boolean isFile = temp.isFile();
			BufferedWriter AllconfusionMatrixWriter = new BufferedWriter(new FileWriter(output+File.separator + "KAEMI_result.csv", true));
			CSVPrinter AllconfusionMatrixcsvPrinter = null;

			if(!isFile) {
				AllconfusionMatrixcsvPrinter = new CSVPrinter(AllconfusionMatrixWriter, CSVFormat.DEFAULT.withHeader("Project","algorithm","MSEvaluation","precision","recall","fMeasure","MCC","AUC","TotalInstance_smote","NumBuggy_smote","NumClean_smote","bugRatio_smote","TotalInstance","NumBuggy","NumClean","bugRatio"));
			}else {
				AllconfusionMatrixcsvPrinter = new CSVPrinter(AllconfusionMatrixWriter, CSVFormat.DEFAULT);
			}

			//read arff file
			DataSource source = new DataSource(arffPath);
			Instances Data = source.getDataSet();
			Data.setClassIndex(0);
			System.out.println(Data.classAttribute());

			//save the number of buggy and clean instance 
			AttributeStats attStats = Data.attributeStats(0);
			Pattern pa = Pattern.compile(".+\\{(\\w+),(\\w+)\\}");
			Matcher m = pa.matcher(Data.attribute(0).toString());
			m.find();

			int index = 10;
			if(m.group(1).compareTo("buggy") == 0) index = 0;
			else index = 1;

			//init buggy clean variable
			int total;
			int buggy;
			int clean;
			float ratio;
			if(m.group(1).equals("buggy")) {
				buggy = attStats.nominalCounts[0];
				clean = attStats.nominalCounts[1];
			}else {
				buggy = attStats.nominalCounts[1];
				clean = attStats.nominalCounts[0];
			}
			total = attStats.totalCount;
			ratio = ((float)buggy/(float)total) * 100;

			//preprocess
			SMOTE smote=new SMOTE();
			smote.setInputFormat(Data);
			System.out.println("smote NN : " + smote.getNearestNeighbors());
			System.out.println("smote percentage of bug : " + smote.getPercentage());
			Instances Data_smote = Filter.useFilter(Data, smote);

			AttributeStats attStats_smote = Data_smote.attributeStats(0);

			//init smote buggy clean variable
			int total_smote;
			int buggy_smote;
			int clean_smote;
			float ratio_smote;
			if(m.group(1).equals("buggy")) {
				buggy_smote = attStats_smote.nominalCounts[0];
				clean_smote = attStats_smote.nominalCounts[1];
			}else {
				buggy_smote = attStats_smote.nominalCounts[1];
				clean_smote = attStats_smote.nominalCounts[0];
			}
			total_smote = attStats_smote.totalCount;
			ratio_smote = ((float)buggy_smote/(float)total_smote) * 100;

			//set algorithms
			ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("random"));

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

				//			classifyModel.buildClassifier(Data_smote);

				ArrayList<String> multisearchEvaluationNames = new ArrayList<String>(Arrays.asList("Fmeasure"));

				for(String multisearchEvaluationName : multisearchEvaluationNames) {
					MultiSearch multi_search = new MultiSearch();
					SelectedTag tag = null;
					if(multisearchEvaluationName.equals("AUC")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_AUC, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Fmeasure")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_FMEASURE, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("MCC")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_MATTHEWS_CC, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Precision")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_PRECISION, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Recall")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_RECALL, new DefaultEvaluationMetrics().getTags());
					}
					multi_search.setEvaluation(tag);
					multi_search.setAlgorithm(new DefaultSearch());
					multi_search.setClassifier(classifyModel);
					multi_search.buildClassifier(Data_smote);//여기서 error / Method not found: isRidge
					
					Evaluation evaluation = new Evaluation(Data_smote);
					evaluation.crossValidateModel(multi_search, Data_smote, 10, new Random(1));

					double precision = evaluation.precision(index);
					double recall = evaluation.recall(index);
					double fMeasure = evaluation.fMeasure(index);
					double MCC = evaluation.matthewsCorrelationCoefficient(index);
					double AUC = evaluation.areaUnderROC(index);

					AllconfusionMatrixcsvPrinter.printRecord(projectname,algorithm,multisearchEvaluationName,precision,recall,fMeasure,MCC,AUC,total_smote,buggy_smote,clean_smote,ratio_smote,total,buggy,clean,ratio);

					//			for(int i = 1; i < Integer.parseInt(args[2])+1; i++) {
					//				evaluation.crossValidateModel(classifyModel, Data_smote, 10, new Random(i));
					//				
					//		//		evaluation.evaluateModel(classifyModel, testData);
					//				
					//				BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(output +File.separator + projectname + "-" + algorithm + "-" +i+"-10-fold.txt")));
					//				
					//				String strSummary = evaluation.toSummaryString();
					//				String detail = evaluation.toClassDetailsString();
					//				
					//				bufferedWriter.write(Data_smote.attribute(0).toString());
					//				bufferedWriter.write("\n");
					//				bufferedWriter.write(attStats.toString());
					//				bufferedWriter.write(strSummary);
					//				bufferedWriter.write(detail);
					//				bufferedWriter.close();
					//				}
				}
			}
			AllconfusionMatrixcsvPrinter.close();
			AllconfusionMatrixWriter.close();

			System.out.println("Finish "+projectname);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}