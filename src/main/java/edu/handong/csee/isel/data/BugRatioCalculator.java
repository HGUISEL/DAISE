package edu.handong.csee.isel.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import weka.core.converters.CSVLoader;

public class BugRatioCalculator {
	static int allMetricNum = 0;

	public static void main(String[] args) throws Exception {

		TreeSet<String> commitTimes = new TreeSet<String>();
		HashMap<String, ArrayList<MetaDataInfo>> metrics = new HashMap<>();///
		
		Reader in = new FileReader(args[0]);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
		
		for (CSVRecord record : records) {
			MetaDataInfo metaDataInfo = new MetaDataInfo(record);
			String commitTime = metaDataInfo.getCommitTime();
			commitTimes.add(commitTime);
			
			if(metrics.containsKey(commitTime)) {
				ArrayList<MetaDataInfo> arr = metrics.get(commitTime);
				arr.add(metaDataInfo);
				//testDeveloperMetrics.put(commitTime, testMetaDataArr);??
			}else {
				ArrayList<MetaDataInfo> arr = new ArrayList<>();
				arr.add(metaDataInfo);
				metrics.put(commitTime, arr);
			}
			allMetricNum++;
		}
		
		
		ArrayList<BugRatioInfo> bugRatioInfos = new ArrayList<>();
		BugRatioInfo bugRatioInfo = new BugRatioInfo();
		bugRatioInfo.setStartDate(commitTimes.first());
		bugRatioInfo.setMidDate(commitTimes.first());
		int i = 0;
		
		for(String commitTime : commitTimes) {
			int calDateDays = calDateBetweenAandB(bugRatioInfo.getMidDate(),commitTime);

			bugRatioInfo.setAccumulatedDate(calDateDays);
			
			ArrayList<MetaDataInfo> arr = metrics.get(commitTime);
			bugRatioInfo.setAccumulatedNumofMetric(arr.size());
			for(MetaDataInfo a : arr) {
				if(a.getIsBuggy().compareTo("buggy") == 0) {
					bugRatioInfo.setAccumulatedNumofBug(1);
				}
				bugRatioInfo.setMetricKey(a.getKey());
			}
			
			if(bugRatioInfo.getAccumulatedDate() > 30) {
				bugRatioInfo.setEndDate(commitTime);
				bugRatioInfos.add(bugRatioInfo);
				bugRatioInfo = new BugRatioInfo();
				bugRatioInfo.setStartDate(commitTime);
				bugRatioInfo.setMidDate(commitTime);
			}
			
			bugRatioInfo.setMidDate(commitTime);
			
			
			
//			i++;
//			if(i > 100)
//			break;
		}
		
		Save2CSV(bugRatioInfos);
	}

	private static void Save2CSV(ArrayList<BugRatioInfo> bugRatioInfos) throws Exception {
		
		String resultCSVPath = "/Users/yangsujin/Desktop/sentry-result.csv";
		BufferedWriter writer = new BufferedWriter(new FileWriter( new File(resultCSVPath)));
		CSVPrinter csvPrinter = new CSVPrinter(writer, 
				CSVFormat.DEFAULT.withHeader("StartDate","EndDate","BetweenDate","NumMetrics","NumBuggy","NumClean","Ratio%"));

		
		for(BugRatioInfo bugRatioInfo : bugRatioInfos) {
			String startDate = bugRatioInfo.getStartDate();
			int accumulatedDate = bugRatioInfo.getAccumulatedDate();
			int accumulatedNumofMetric = bugRatioInfo.getAccumulatedNumofMetric();
			int accumulatedNumofBug = bugRatioInfo.getAccumulatedNumofBug();
			int accumulatedNumofClean = accumulatedNumofMetric - accumulatedNumofBug;
//			ArrayList<String> metricKey = bugRatioInfo.getMetricKey();
			String endDate = bugRatioInfo.getEndDate();
			float bugRatio = (float)accumulatedNumofBug / (float) allMetricNum;
			bugRatio = bugRatio * 100;
			
			csvPrinter.printRecord(startDate,endDate,accumulatedDate,accumulatedNumofMetric,accumulatedNumofBug,accumulatedNumofClean,bugRatio);

		}
		
		csvPrinter.close();
		
	}

	private static int calDateBetweenAandB(String preTime, String commitTime) throws Exception {

		SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        // date1, date2 두 날짜를 parse()를 통해 Date형으로 변환.
		
        Date FirstDate = format.parse(preTime);
        Date SecondDate = format.parse(commitTime);
        
        // Date로 변환된 두 날짜를 계산한 뒤 그 리턴값으로 long type 변수를 초기화 하고 있다.
        // 연산결과 -950400000. long type 으로 return 된다.
        long calDate = FirstDate.getTime() - SecondDate.getTime(); 
        
        // Date.getTime() 은 해당날짜를 기준으로1970년 00:00:00 부터 몇 초가 흘렀는지를 반환해준다. 
        // 이제 24*60*60*1000(각 시간값에 따른 차이점) 을 나눠주면 일수가 나온다.
        long calDateDays = calDate / ( 24*60*60*1000); 
 
        calDateDays = Math.abs(calDateDays);
//        System.out.println(commitTime);
//        System.out.println("두 날짜의 날짜 차이: "+calDateDays);
        
        return (int)calDateDays;
		
	}

}
