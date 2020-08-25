package edu.handong.csee.isel.daise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Simple {

	public static void main(String[] args) throws Exception {
		String projectname = null;
		
		String projectNamePatternStr = ".+/(.+)\\.arff";
		Pattern projectNamePattern = Pattern.compile(projectNamePatternStr);
		Matcher m = projectNamePattern.matcher(args[0]);
		while(m.find()) {
			projectname = m.group(1);
		}
		
		DataSource source = new DataSource(args[0]);
		Instances Data = source.getDataSet();
		Data.setClassIndex(0);
		
		AttributeStats attStats = Data.attributeStats(0);
		
		DataSource testSource = new DataSource(args[3]);
		Instances testData = testSource.getDataSet();
		testData.setClassIndex(0);
		
		Classifier classifyModel = null;
		
		if(args[2].toString().compareTo("r") == 0) {
			classifyModel = new RandomForest();
		}else if(args[2].toString().compareTo("n") == 0){
			classifyModel = new NaiveBayes();
		}else if(args[2].toString().compareTo("l") == 0){
			classifyModel = new Logistic();
		}else if(args[2].toString().compareTo("i")==0){
			classifyModel = new IBk();
		}
		
		classifyModel.buildClassifier(Data);
		
		Evaluation evaluation = new Evaluation(Data);
//		evaluation.crossValidateModel(classifyModel, Data, 10, new Random(1));
		evaluation.evaluateModel(classifyModel, testData);
		
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(args[1] +File.separator + projectname + "-" + args[2] + "-10-fold.txt")));
		String strSummary = evaluation.toSummaryString();
		String detail = evaluation.toClassDetailsString();
		bufferedWriter.write(Data.attribute(0).toString());
		bufferedWriter.write("\n");
		bufferedWriter.write(attStats.toString());
		bufferedWriter.write(strSummary);
		bufferedWriter.write(detail);
		bufferedWriter.close();
	}

}
