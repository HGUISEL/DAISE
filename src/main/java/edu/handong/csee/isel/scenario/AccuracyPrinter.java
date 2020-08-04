package edu.handong.csee.isel.scenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class AccuracyPrinter {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		calAccuracy(args[0],args[3]);
		JITdefectPrediction(args[1],args[2],args[3]);
	}
	
	private static void JITdefectPrediction(String train, String test, String output) throws Exception {
		// TODO Auto-generated method stub
		
		
		DataSource trainSource = new DataSource(train);
		Instances trainData = trainSource.getDataSet();
		trainData.setClassIndex(0);
		
		AttributeStats attStats = trainData.attributeStats(0);
		
		DataSource testSource = new DataSource(test);
		Instances testData = testSource.getDataSet();
		testData.setClassIndex(0);
		
		//make machine learning model
		System.out.println("Start classify");
		Classifier randomForest = new RandomForest();
		randomForest.buildClassifier(trainData);
		System.out.println("End classify");
		
		Evaluation evalClassify = new Evaluation(trainData);
		evalClassify.evaluateModel(randomForest, testData);
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(output + "/result-main.txt")));
		String strSummary = evalClassify.toSummaryString();
		String detail = evalClassify.toClassDetailsString();
		bufferedWriter.write(trainData.attribute(0).toString());
		bufferedWriter.write("\n");
		bufferedWriter.write(attStats.toString());
		bufferedWriter.write(strSummary);
		bufferedWriter.write(detail);
		bufferedWriter.close();
	}

	static void calAccuracy(String path, String output) throws Exception {
		int bTruePositive = 0;
		int bFalsePositive = 0;
		int bFalseNegative = 0;
		int bTrueNegative = 0;
		
		int cTruePositive = 0;
		int cFalsePositive = 0;
		int cFalseNegative = 0;
		int cTrueNegative = 0;
		
		//read file and save value
		Reader in = new FileReader(path);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

		for (CSVRecord record : records) {
			DBPDResult data = new DBPDResult(record);
			//buggy value calculate
			saveValue("buggy",bTruePositive, bFalsePositive, bFalseNegative, bTrueNegative);
		}
		
	}

	private static void saveValue(String string, int bTruePositive, int bFalsePositive, int bFalseNegative,
			int bTrueNegative) {
		// TODO Auto-generated method stub
		String label = string;
	}

}
