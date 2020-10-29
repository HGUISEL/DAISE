package edu.handong.csee.isel.pdp;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

public class OnlineMain {
	String dataPath;
	String BICpath;
	String outputPath;
	boolean verbose;
	boolean help;
	
	private final static String firstcommitTimePatternStr = "'(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)'";
	private final static Pattern firstcommitTimePattern = Pattern.compile(firstcommitTimePatternStr);
	
	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);
	
	private final static String defaultLabelPatternStr = "@attribute @@class@@ \\{\\w+,(\\w+)\\}";
	private final static Pattern defaultLabelPattern = Pattern.compile(defaultLabelPatternStr);
	
	private static HashMap<Integer,ArrayList<String>> run_trainingSetCommithash = new HashMap<>();
	private static HashMap<Integer,ArrayList<String>> run_testSetCommithash = new HashMap<>();

	public static void main(String[] args) throws Exception {
		OnlineMain main = new OnlineMain();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			BaseSetting baseSet = new BaseSetting(dataPath);
			
			System.out.println(baseSet.getProjectName());
			System.out.println(baseSet.getReferenceFolderPath());
			
			//mk result directory
			File PDPDir = new File(outputPath +File.separator+baseSet.getProjectName()+"-online"+File.separator);
			String directoryPath = PDPDir.getAbsolutePath();
			PDPDir.mkdir();
			
			//read BIC file and calculate Average Bug fix time
			Reader in = new FileReader(BICpath);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
			int numOfBIC = 0;
			int calDateDays = 0;
			for (CSVRecord record : records) {
				String BICtime = record.get("BIDate");
				String BFCtime = record.get("FixDate");
				calDateDays = calDateDays + calDateBetweenAandB(BICtime,BFCtime);
				numOfBIC++;
			}
			baseSet.setAverageBugFixingTimeDays(calDateDays/numOfBIC);
			baseSet.setEndGapDays(baseSet.getAverageBugFixingTimeDays()); //set end gap
			
			//(1) read arff file
			ArrayList<String> attributeLineList = new ArrayList<>();
			ArrayList<String> dataLineList = new ArrayList<>();
			
			String content = FileUtils.readFileToString(new File(dataPath), "UTF-8");
			String[] lines = content.split("\n");
			
			String firstAttrCommitTime = null;
			int indexOfCommitTime = 0;
			String firstAttrKey = null;
			int indexOfKey = 0;
			String firstAttrLabel = null;
			String defaultLabel = null;
			
			boolean dataPart = false;
			for (String line : lines) {
				if (dataPart) {
					dataLineList.add(line);
					continue;

				}else if(!dataPart){
					attributeLineList.add(line);
					if(line.startsWith("@attribute @@class@@")) {
						Matcher m = defaultLabelPattern.matcher(line);
						m.find();
						defaultLabel = m.group(1);
						firstAttrLabel = "{0 "+ m.group(1)+",";
					}
					if(line.startsWith("@attribute meta_data-commitTime")) {
						Matcher m = firstcommitTimePattern.matcher(line);
						m.find();
						firstAttrCommitTime = m.group(1);
						indexOfCommitTime = attributeLineList.size()-3;
					}
					if(line.startsWith("@attribute Key {")) {
						Matcher m = firstKeyPattern.matcher(line);
						m.find();
						firstAttrKey = m.group(1);
						indexOfKey = attributeLineList.size()-3;
					}
					if (line.startsWith("@data")) {
						dataPart = true;
					}
				}
			}
			
			TreeMap<String,ArrayList<String>> commitTime_commitHash = new TreeMap<>();
			HashMap<String,ArrayList<String>> commitHash_data = new HashMap<>();
			HashMap<String,Boolean> commitHash_isBuggy = new HashMap<>();

			
			for(String line : dataLineList) {
				String commitTime = parsingCommitTime(line,firstAttrCommitTime,indexOfCommitTime);
				String commitHash = parsingCommitHash(line,firstAttrKey,indexOfKey);
				String aData = parsingDataLine(line,indexOfCommitTime,indexOfKey);
				boolean isBuggy  = parsingBugCleanLabel(line,firstAttrLabel,defaultLabel);
				
				//put2commitTime_commitHash
				ArrayList<String> commitHashs;
				if(commitTime_commitHash.containsKey(commitTime)) {
					commitHashs = commitTime_commitHash.get(commitTime);
					commitHashs.add(commitHash);
				}else {
					commitHashs = new ArrayList<>();
					commitHashs.add(commitHash);
					commitTime_commitHash.put(commitTime, commitHashs);
				}
				
				//put2commitHash_data
				ArrayList<String> data;
				if(commitHash_data.containsKey(commitHash)) {
					data = commitHash_data.get(commitHash);
					data.add(aData);
				}else {
					data = new ArrayList<>();
					data.add(aData);
					commitHash_data.put(commitHash, data);
				}
				
				//put2commitHash_isBuggy
				if(commitHash_isBuggy.containsKey(commitHash)) {
					boolean temp = commitHash_isBuggy.get(commitHash);
					if((temp != true) && (isBuggy == true)) commitHash_isBuggy.put(commitHash, isBuggy);
				}else {
					commitHash_isBuggy.put(commitHash, isBuggy);
				}
			}
			
			//set total first, last commit time
			baseSet.setFirstCommitTimeStr(commitTime_commitHash.firstKey());
			baseSet.setLastCommitTimeStr(commitTime_commitHash.lastKey());
			System.out.println(baseSet.getFirstCommitTimeStr());
			System.out.println(baseSet.getLastCommitTimeStr());
			System.out.println();
			
			//total experiment change
			TreeMap<String,ArrayList<String>> commitTime_commitHash_experimental;
			int defaultStartGap = 365 * 3; // startGap default : 3 years 
			
			while(true) {
				System.out.println(defaultStartGap);
				commitTime_commitHash_experimental = new TreeMap<>();
				
				//set startGapDate str
				String startGapDate = addDate(baseSet.getFirstCommitTimeStr(),defaultStartGap);
				baseSet.setStartGapStr(findNearDate(startGapDate,commitTime_commitHash,"r"));
				
				//set endGapDate str
				String endGapDate = addDate(baseSet.getLastCommitTimeStr(),-baseSet.getEndGapDays());
				//조건문 추가, 만약 last commit time str에서 average bug fixing time을 뺀 값이 음수일 경우? - 개발 전체 기간이 평균fixing time보다 작아야 일어나는 현상이라 상관 안해도 될듯 
				//조건문 추가, 만약 start date가 end date보다 클 경우 start date를 낮춘다 3year -> 2year
				baseSet.setEndGapStr(findNearDate(endGapDate,commitTime_commitHash,"l"));
			
//				System.out.println("real str date : "+baseSet.getStartGapStr());
//				System.out.println("real end date : "+baseSet.getEndGapStr());
//				System.out.println();
				
				//count total experiment change
				for(String commitTime : commitTime_commitHash.keySet()) {
					if(!(baseSet.getStartGapStr().compareTo(commitTime)<=0 && commitTime.compareTo(baseSet.getEndGapStr())<=0))
						continue;
					ArrayList<String> commitHash = commitTime_commitHash.get(commitTime);
					baseSet.setTotalExperimentalCommit(commitHash.size());
					commitTime_commitHash_experimental.put(commitTime, commitHash);
				}
				
				System.out.println("ExpCh : "+baseSet.getTotalExperimentalCommit());
				if(baseSet.getTotalExperimentalCommit() > 10000) 
					break;
				
				defaultStartGap -= 30;
				if(defaultStartGap < 0) defaultStartGap = 0;
				baseSet.resetTotalExperimentalCommit();
			}
			System.out.println();
			System.out.println("real str date : "+baseSet.getStartGapStr());
			System.out.println("real end date : "+baseSet.getEndGapStr());
			System.out.println();
			
			//set average test size 
			baseSet.setAverageTestSetSize(baseSet.getTotalExperimentalCommit()/4);
			System.out.println("average test size : "+baseSet.getAverageTestSetSize());
			
			//cal update time (test set Period)
			int averageTestPeriodDays = 0;
			int numOfCalTestPeriod = 0; //number of count : sumOfCommit > AverageTestSetSize
			
			int sumOfCommit = 0; //temp
			String fCommitTime = baseSet.getStartGapStr(); //temp
			String lCommitTime = null; //temp
			
			for(String commitTime : commitTime_commitHash_experimental.keySet()) {
				
				ArrayList<String> commitHash = commitTime_commitHash_experimental.get(commitTime);
				
				sumOfCommit += commitHash.size();
				
				if(sumOfCommit > baseSet.getAverageTestSetSize()) {
					lCommitTime = commitTime;
					averageTestPeriodDays += calDateBetweenAandB(fCommitTime,lCommitTime);
					numOfCalTestPeriod++;
					
					fCommitTime = commitTime;
					lCommitTime = null;
					sumOfCommit = 0;
				}
			}
			//set update time ==  test set period
			baseSet.setUpdateTimeDays(averageTestPeriodDays/numOfCalTestPeriod);
			System.out.println("UT : "+baseSet.getUpdateTimeDays());
			//set gap : bug-fixing time - test set time period (== update time)
			baseSet.setGapDays(baseSet.getAverageBugFixingTimeDays()-baseSet.getUpdateTimeDays());
			System.out.println("Gap : " + baseSet.getGapDays());
			
			//cal run
			int numOfTrainingSet = baseSet.getAverageTestSetSize(); //default training set size : my think : fist training set size (no in paper) -> test set size
			int numOfTestSet = baseSet.getAverageTestSetSize(); 
			int updateTimeDays = baseSet.getUpdateTimeDays(); //day
			int GapDays = baseSet.getGapDays(); //day
			int run = 0;
			String firstCommitDate = baseSet.getFirstCommitTimeStr();
			String lastCommitDate = baseSet.getLastCommitTimeStr();
			
			HashMap<Integer,RunDate> run_RunDate = new HashMap<>();

				//init T time
			String T1 = firstCommitDate; //start training set date
			String T2 = null; //end tarining set date : start gap date
			String T3 = null; //end gap date : start test set date
			String T4 = firstCommitDate; //end test set date
			
			int realTriniingSet = 0;
			
			while(true) {
				RunDate runDate = new RunDate();
				
				if(!(T4.compareTo(lastCommitDate)<0)) {
					T4 = lastCommitDate;
					runDate.setT4(T4);
					run_RunDate.put(run,runDate);
					break;
				}
				T2 = setStartGapDate(T1,T2,GapDays,numOfTrainingSet,commitTime_commitHash_experimental);
				T3 = addDate(T2,GapDays);
				T3 = findNearDate(T3,commitTime_commitHash_experimental,"l");
				T4 = setStartTestSetDate(T3,run,numOfTestSet,commitTime_commitHash_experimental);
				
				runDate.setT2(T1);
				runDate.setT2(T2);
				runDate.setT3(T3);
				runDate.setT4(T4);
				
				run_RunDate.put(run,runDate);
				run++;
			}
			
			
			//BIC re label!!

			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
	}
	

	
	private boolean parsingBugCleanLabel(String line, String firstLabel ,String defaultLabel) {
		if(line.startsWith(firstLabel)) {
			if(defaultLabel.compareTo("clean") == 0) 
				return false;
			else {
				return true;
			}
		}else {
			if(defaultLabel.compareTo("clean") == 0) 
				return true;
			else {
				return false;
			}
		}
	}

	private String setStartTestSetDate(String t3, int run, int numOfTestSet, TreeMap<String, ArrayList<String>> commitTime_commitHash_experimental) {
		ArrayList<String> testSetCommithash = new ArrayList<String>();
		String T4 = null;
		String startDate = t3;
		
		for(String commitTime : commitTime_commitHash_experimental.keySet()) {
			if(!(startDate.compareTo(commitTime) < 0)) 
				continue;
			ArrayList<String> commitHash = commitTime_commitHash_experimental.get(commitTime);
			testSetCommithash.addAll(commitHash);
			if(testSetCommithash.size() > numOfTestSet) {
				T4 = commitTime;
				break;
			}
		}
		
		run_testSetCommithash.put(run, testSetCommithash);
		return T4;
	}

	private String setStartGapDate(String t1, String t2, int Gap, int numOfTrainingSet, TreeMap<String, ArrayList<String>> commitTime_commitHash_experimental) throws Exception {
		ArrayList<String> trainingSetCommithash = new ArrayList<String>();
		String T2 = null;
		
//		if(t2 == null) {
//			T2 = t2;
//		}else{
//			T2 = addDate(t2,Gap);
//		}
//		
//		T2 = findNearDate(T2,commitTime_commitHash_experimental,"r");
		
		for(String commitTime : commitTime_commitHash_experimental.keySet()) {
			if(!(t1.compareTo(commitTime)<=0)) 
				continue;
			
			ArrayList<String> commitHash = commitTime_commitHash_experimental.get(commitTime);
			trainingSetCommithash.addAll(commitHash);
			if(trainingSetCommithash.size() > numOfTrainingSet) {
				T2 = commitTime;
				break;
			}
		}
		
		return T2;
	}

	private String findNearDate(String time, TreeMap<String, ArrayList<String>> commitTime_commitHash_experimental,
			String string) {
		if(string.compareTo("r") == 0) {
			for(String commitTime : commitTime_commitHash_experimental.keySet()) {
				if(!(time.compareTo(commitTime) < 0)) continue;
				return commitTime;
			}
		}else if(string.compareTo("l") == 0) {
			String beforeCommitTime = null;
			for(String commitTime : commitTime_commitHash_experimental.keySet()) {
				if(!(time.compareTo(commitTime) < 0)) {
					beforeCommitTime = commitTime;
					continue;
				}
				return beforeCommitTime;
			}
		}
		return null;
	}


	private String parsingCommitHash(String line, String firstKey, int indexOfKey) {
		String key = null;
		if((line.contains(","+indexOfKey+" "))) {
			key = line.substring(line.lastIndexOf(Integer.toString(indexOfKey)),line.lastIndexOf("}"));
			key = key.substring(key.lastIndexOf(" ")+1,key.length());
		}else {
			key = firstKey;
		}
		
		return key.substring(0,key.indexOf("-"));
	}
	
	private String parsingDataLine(String line, int indexOfCommitTime,int indexOfKey) {
		if((line.contains(","+indexOfKey+" "))) {
			if((line.contains(","+indexOfCommitTime+" "))) { //index previous,index commitTime, index key} 
				line = line.substring(0,line.lastIndexOf(","+indexOfCommitTime));
				line = line + "}";
				return line;
			}else {											//index previous,index key}
				line = line.substring(0,line.lastIndexOf(","+indexOfKey));
				line = line + "}";
				return line;
			}
		}else {
			if((line.contains(","+indexOfCommitTime+" "))) {//index previous,index commitTime} 
				line = line.substring(0,line.lastIndexOf(","+indexOfCommitTime));
				line = line + "}";
				return line;
			}else {											//index previous,index commitTime} 
				return line;
			}
		}
	}

	private String parsingCommitTime(String line, String firstCommitTime, int indexOfCommitTime) {
		if((line.contains(","+indexOfCommitTime+" "))) {
			String commitTime = line.substring(line.lastIndexOf(indexOfCommitTime+" '"),line.lastIndexOf("'"));
			commitTime = commitTime.substring(commitTime.lastIndexOf("'")+1,commitTime.length());
			return commitTime;
		}else {
			return firstCommitTime;
		}
	}
	
	private String addDate(String dt, int d) throws Exception  {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Calendar cal = Calendar.getInstance();
		Date date = format.parse(dt);
		cal.setTime(date);
        cal.add(Calendar.DATE, d);		//년 더하기

		return format.format(cal.getTime());

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

	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			dataPath = cmd.getOptionValue("i");
			BICpath = cmd.getOptionValue("b");
			outputPath = cmd.getOptionValue("o");
			if(outputPath.endsWith(File.separator)) {
				outputPath = outputPath.substring(0, outputPath.lastIndexOf(File.separator));
			}

			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}
	
	private Options createOptions() {
		Options options = new Options();

		// add options by using OptionBuilder
		options.addOption(Option.builder("i").longOpt("metadata.arff")
				.desc("Address of meta data arff file. Don't use double quotation marks")
				.hasArg()
				.argName("URI")
				.required()
				.build());// 필수
		
		options.addOption(Option.builder("b").longOpt("BIC.csv")
				.desc("BIC file path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.required()
				.build());

		options.addOption(Option.builder("o").longOpt("output")
				.desc("output path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.required()
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());

		return options;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Collecting developer Meta-data program";
		String footer = "\nPlease report issues at https://github.com/HGUISEL/DAISE/issues";
		formatter.printHelp("DAISE", header, options, footer, true);
	}

}

class RunDate {
	String T1;
	String T2;
	String T3;
	String T4;
	int numOfTrainingSet;
	int numOfTestSet;
	
	RunDate(){
		T1 = null;
		T2 = null;
		T3 = null;
		T4 = null;
		numOfTrainingSet = 0;
		numOfTestSet = 0;
	}

	public String getT1() {
		return T1;
	}

	public void setT1(String t1) {
		T1 = t1;
	}

	public String getT2() {
		return T2;
	}

	public void setT2(String t2) {
		T2 = t2;
	}

	public String getT3() {
		return T3;
	}

	public void setT3(String t3) {
		T3 = t3;
	}

	public String getT4() {
		return T4;
	}

	public void setT4(String t4) {
		T4 = t4;
	}
	
}


class BaseSetting {
	String projectName;
	String referenceFolderPath;
	
	String firstCommitTimeStr;
	String lastCommitTimeStr;
	int averageBugFixingTimeDays;
	int averageTestSetSize; //numCommit
	String startGapStr;
	String endGapStr;
	int gapDays; //days
	int endGapDays;
	
	int maxTrainingSetSize; //numCommit
	int numOfRun; //number of run
	int updateTimeDays; //days  test set duration days
	int developementPeriod;
	int sumOfGapAndTestPeriod;
	int totalExperimentalCommit; //commit
	
	
	BaseSetting(String dataPath){
		Pattern pattern = Pattern.compile("(.+)/(.+).arff");
		
		Matcher matcher = pattern.matcher(dataPath);
		while(matcher.find()) {
			referenceFolderPath = matcher.group(1);
			projectName = matcher.group(2);
		}
		
		firstCommitTimeStr = null;
		lastCommitTimeStr = null;
		
		averageBugFixingTimeDays = 0;
		averageTestSetSize = 0;
		gapDays = 0;
		numOfRun = 0;
		maxTrainingSetSize = 0;
		updateTimeDays = 0;
		developementPeriod = 0;
		sumOfGapAndTestPeriod = 0;
		startGapStr = null;
		endGapStr = null;
		totalExperimentalCommit = 0;
		endGapDays = 0;
	}
	
	
	
	public String getFirstCommitTimeStr() {
		return firstCommitTimeStr;
	}
	public void setFirstCommitTimeStr(String firstCommitTime) {
		this.firstCommitTimeStr = firstCommitTime;
	}
	public String getLastCommitTimeStr() {
		return lastCommitTimeStr;
	}
	public void setLastCommitTimeStr(String lastCommitTime) {
		this.lastCommitTimeStr = lastCommitTime;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getReferenceFolderPath() {
		return referenceFolderPath;
	}
	public void setReferenceFolderPath(String referenceFolderPath) {
		this.referenceFolderPath = referenceFolderPath;
	}
	public int getAverageBugFixingTimeDays() {
		return averageBugFixingTimeDays;
	}
	public void setAverageBugFixingTimeDays(int averageBugFixingTimeDays) {
		this.averageBugFixingTimeDays = averageBugFixingTimeDays;
	}

	public int getAverageTestSetSize() {
		return averageTestSetSize;
	}

	public void setAverageTestSetSize(int averageTestSetSize) {
		this.averageTestSetSize = averageTestSetSize;
	}

	public int getGapDays() {
		return gapDays;
	}

	public void setGapDays(int gapDays) {
		this.gapDays = gapDays;
	}

	public int getMaxTrainingSetSize() {
		return maxTrainingSetSize;
	}

	public void setMaxTrainingSetSize(int maxTrainingSetSize) {
		this.maxTrainingSetSize = maxTrainingSetSize;
	}

	public int getNumOfRun() {
		return numOfRun;
	}

	public void setNumOfRun(int numOfRun) {
		this.numOfRun = numOfRun;
	}

	public int getUpdateTimeDays() {
		return updateTimeDays;
	}

	public void setUpdateTimeDays(int updateTimeDays) {
		this.updateTimeDays = updateTimeDays;
	}

	public int getDevelopementPeriod() {
		return developementPeriod;
	}

	public void setDevelopementPeriod(int developementPeriod) {
		this.developementPeriod = developementPeriod;
	}

	public int getSumOfGapAndTestPeriod() {
		return sumOfGapAndTestPeriod;
	}

	public void setSumOfGapAndTestPeriod(int sumOfGapAndTestPeriod) {
		this.sumOfGapAndTestPeriod = sumOfGapAndTestPeriod;
	}

	public String getStartGapStr() {
		return startGapStr;
	}

	public void setStartGapStr(String startGapStr) {
		this.startGapStr = startGapStr;
	}

	public String getEndGapStr() {
		return endGapStr;
	}

	public void setEndGapStr(String endGapStr) {
		this.endGapStr = endGapStr;
	}

	public int getTotalExperimentalCommit() {
		return totalExperimentalCommit;
	}

	public void setTotalExperimentalCommit(int plusExperimentalCommit) {
		this.totalExperimentalCommit += plusExperimentalCommit;
	}
	
	public void resetTotalExperimentalCommit() {
		this.totalExperimentalCommit = 0;
	}
	public int getEndGapDays() {
		return endGapDays;
	}
	public void setEndGapDays(int endGapDays) {
		this.endGapDays = endGapDays;
	}
	
	
}
