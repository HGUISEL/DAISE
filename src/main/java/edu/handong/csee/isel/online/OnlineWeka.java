package edu.handong.csee.isel.online;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
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

public class OnlineWeka {
	/*
	 * args[0] : arff folder path
	 * args[1] : result path
	 */
	public static void main(String[] args) throws Exception {
		
		String projectname = null;
		
		String projectNamePatternStr = ".+/(\\w+)-data-online";
		Pattern projectNamePattern = Pattern.compile(projectNamePatternStr);
		Matcher m = projectNamePattern.matcher(args[0]);
		
		while(m.find()) {
			projectname = m.group(1);
		}
		
		File resultDir = new File(args[1] +File.separator + projectname+"_result_online");
		resultDir.mkdir();
		String output = resultDir.getAbsolutePath();
		
		String inputPath = args[0];
		File dir = new File(inputPath);
		File []fileList = dir.listFiles();
		ArrayList<String> fileName = new ArrayList<>();
		
		for(File file : fileList) {
			fileName.add(file.getName());
		}
		
		if(!fileName.contains("Project_Information.txt")) System.exit(0);
		
		for(int run = 0; ; run++) {
			if(!(fileName.contains(run+"_tr.arff"))) {
				break;
			}
			System.out.println("run : "+run);
			String tr_arff = run+"_tr.arff";
			String te_arff = run+"_te.arff";
			
			try {
				DataSource source = new DataSource(args[0]+File.separator+tr_arff);
				Instances Data = source.getDataSet();
				Data.setClassIndex(0);
				System.out.println(Data.classAttribute());
				
				AttributeStats attStats = Data.attributeStats(0);
				
				DataSource testSource = new DataSource(args[0]+File.separator+te_arff);
				Instances testData = testSource.getDataSet();
				testData.setClassIndex(0);
//				testData.setClassIndex(testData.numAttributes() - 1);
				System.out.println(testData.classAttribute());
				
				ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("naive"));
						
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
					
					BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(output +File.separator + projectname + "-" + algorithm + "-" +run+".txt")));
					
					String strSummary = evaluation.toSummaryString();
					String detail = evaluation.toClassDetailsString();
					
					bufferedWriter.write(Data.attribute(0).toString());
					bufferedWriter.write("\n");
					bufferedWriter.write(attStats.toString());
					bufferedWriter.write(strSummary);
					bufferedWriter.write(detail);
					bufferedWriter.close();
				
				
//					for(int i = 1; i < Integer.parseInt(args[2])+1; i++) {
//						evaluation.crossValidateModel(classifyModel, Data, 10, new Random(i));
//						
//				//		evaluation.evaluateModel(classifyModel, testData);
//						
//						BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(output +File.separator + projectname + "-" + algorithm + "-" +i+"-10-fold.txt")));
//						
//						String strSummary = evaluation.toSummaryString();
//						String detail = evaluation.toClassDetailsString();
//						
//						bufferedWriter.write(Data.attribute(0).toString());
//						bufferedWriter.write("\n");
//						bufferedWriter.write(attStats.toString());
//						bufferedWriter.write(strSummary);
//						bufferedWriter.write(detail);
//						bufferedWriter.close();
//					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		System.out.println("Finish "+projectname);
	}
	
}
