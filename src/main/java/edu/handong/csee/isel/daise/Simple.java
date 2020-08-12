package edu.handong.csee.isel.daise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
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
		
		Classifier randomForest = new RandomForest();
		randomForest.buildClassifier(Data);
		
		Evaluation evaluation = new Evaluation(Data);
		evaluation.crossValidateModel(randomForest, Data, 10, new Random(1));
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(args[0] +File.separator + projectname + "-10-fold.txt")));
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
