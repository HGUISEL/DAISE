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
	 String resultCSVPath;
	 String outputPath;
	 String train;
	 String test;
	 String projectName;
	
	void JITdefectPrediction() throws Exception {
		///data/DBPD/maven-reference/maven-train-data.arff /data/DBPD/maven-reference/maven-test-data.arff /data/DBPD
		
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
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(outputPath + File.separator + projectName + "-JIT-accuracy.txt")));
		String strSummary = evalClassify.toSummaryString();
		String detail = evalClassify.toClassDetailsString();
		bufferedWriter.write(trainData.attribute(0).toString());
		bufferedWriter.write("\n");
		bufferedWriter.write(attStats.toString());
		bufferedWriter.write(strSummary);
		bufferedWriter.write(detail);
		bufferedWriter.close();
	}

	void calAccuracy() throws Exception {
		int numOfdefect = 0;
		int numOfInstance = 0;
		
		int bTruePositive = 0;
		int bFalsePositive = 0;
		int bFalseNegative = 0;
		int bTrueNegative = 0;
		
		int cTruePositive = 0;
		int cFalsePositive = 0;
		int cFalseNegative = 0;
		int cTrueNegative = 0;
		
		//read file and save value
		Reader in = new FileReader(resultCSVPath);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

		for (CSVRecord record : records) {
			DBPDResult data = new DBPDResult(record);
			
			//buggy and clean value calculate
			if(data.getPredictLabel().equals("buggy")) {
				if(data.getRealLabel().equals("buggy")) {
					bTruePositive++; 						//real : buggy prediction : buggy
					cTrueNegative++;
				}else {
					bFalsePositive++;						//real : clean prediction : buggy
					cFalseNegative++;
					numOfdefect++;
				}
			}else { //data.getPredictLabel().equals clean
				if(data.getRealLabel().equals("clean")) {
					bTrueNegative++;						//real : clean prediction : clean
					cTruePositive++;
				}else {
					bFalseNegative++;						//real : buggy prediction : clean
					cFalsePositive++;
					numOfdefect++;
				}
			}
			
			numOfInstance++;
		}
		
		//cal Precision
		float denominator;
		float numerator;
		
		denominator = (float)(bTruePositive + bFalsePositive);
		float bPrecision = (float)bTruePositive / denominator;
		
		denominator = (float)(cTruePositive + cFalsePositive);
		float cPrecision = (float)cTruePositive / denominator;
		
		//cal Recall
		denominator = ((float)bTruePositive + (float)bFalseNegative);
		float bRecall = (float)bTruePositive / denominator;
		
		denominator = ((float)cTruePositive + (float)cFalseNegative);
		float cRecall = (float)cTruePositive / denominator;
		
		//cal F1 score
		denominator = bPrecision + bRecall;
		numerator = bPrecision * bRecall;
		float bF1score = (numerator/denominator) * 2;
		
		denominator = cPrecision + cRecall;
		numerator = cPrecision * cRecall;
		float cF1score = (numerator/denominator) * 2;
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(outputPath + File.separator + projectName + "-developer-accuracy.txt")));
		bufferedWriter.write("All Test Instance / All Tets Defect :\n");
		bufferedWriter.write(numOfInstance + " / " + numOfdefect + "\n");
		bufferedWriter.write("buggy\n");
		bufferedWriter.write("Recall : "+bRecall+"\n");
		bufferedWriter.write("Precision : "+ bPrecision + "\n");
		bufferedWriter.write("F1 score : " + bF1score + "\n");
		bufferedWriter.write("________________________________\n");
		bufferedWriter.write("bTruePositive : " + bTruePositive + "\n");
		bufferedWriter.write("bFalsePositive : " + bFalsePositive + "\n");
		bufferedWriter.write("bFalseNegative : " + bFalseNegative + "\n");
		bufferedWriter.write("bTrueNegative : " + bTrueNegative + "\n");
		bufferedWriter.write("________________________________\n");
		bufferedWriter.write("clean\n");
		bufferedWriter.write("Recall : "+cRecall+"\n");
		bufferedWriter.write("Precision : "+ cPrecision + "\n");
		bufferedWriter.write("F1 score : " + cF1score + "\n");
		bufferedWriter.write("________________________________\n");
		bufferedWriter.write("cTruePositive : " + cTruePositive + "\n");
		bufferedWriter.write("cFalsePositive : " + cFalsePositive + "\n");
		bufferedWriter.write("cFalseNegative : " + cFalseNegative + "\n");
		bufferedWriter.write("cTrueNegative : " + cTrueNegative + "\n");
		bufferedWriter.write("________________________________\n");
		
		bufferedWriter.close();
		
	}

	public void setResultCSVPath(String resultCSVPath) {
		this.resultCSVPath = resultCSVPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public void setTrain(String train) {
		this.train = train;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
}
