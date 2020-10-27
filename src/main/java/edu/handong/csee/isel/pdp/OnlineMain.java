package edu.handong.csee.isel.pdp;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	String metadataPath;
	String BICpath;
	String outputPath;
	boolean verbose;
	boolean help;
	
	private final static String firstcommitTimePatternStr = "'(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)'";
	private final static Pattern firstcommitTimePattern = Pattern.compile(firstcommitTimePatternStr);
	
	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);

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
			
			BaseSetting baseSet = new BaseSetting();
//			
//			baseSet.setProjectName(parseProjectName(metadataPath));
//			baseSet.setReferenceFolderPath(parseReferenceFolderPath(metadataPath));
			System.out.println(baseSet.getProjectName());
			
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
			baseSet.setAverageBugFixingTime(calDateDays/numOfBIC);
			
			//test set 크기 설정 : 프로젝트의 총 실험 change 횟수의 1/4 → 최대 크기 
			//(1) read arff file
			ArrayList<String> attributeLineList = new ArrayList<>();
			ArrayList<String> dataLineList = new ArrayList<>();
			
			String content = FileUtils.readFileToString(new File(args[0]), "UTF-8");
			String[] lines = content.split("\n");
			
			String firstCommitTime = null;
			int indexOfCommitTime = 0;
			String firstKey = null;
			int indexOfKey = 0;
			
			boolean dataPart = false;
			for (String line : lines) {
				if (dataPart) {
					dataLineList.add(line);
					continue;

				}else if(!dataPart){
					attributeLineList.add(line);
					if(line.startsWith("@attribute meta_data-commitTime")) {
						Matcher m = firstcommitTimePattern.matcher(line);
						m.find();
						firstCommitTime = m.group(1);
						indexOfCommitTime = attributeLineList.size()-3;
					}
					if(line.startsWith("@attribute Key {")) {
						Matcher m = firstKeyPattern.matcher(line);
						m.find();
						firstKey = m.group(1);
						indexOfKey = attributeLineList.size()-3;
					}
					if (line.startsWith("@data")) {
						dataPart = true;
					}
				}
			}
			
			baseSet.setSizeOfTestSet(dataLineList.size()/4);
			

			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
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
	
//	private String parseReferenceFolderPath(String metadataPath2) {
//		Pattern pattern = Pattern.compile("(.+)/(.+).arff");
//		String referenceFolderPath = null;
//		
//		Matcher matcher = pattern.matcher(metadataPath);
//		while(matcher.find()) {
//			referenceFolderPath = matcher.group(1);
//		}
//		referenceFolderPath = referenceFolderPath+File.separator+projectName+"-reference";
//		
//		return referenceFolderPath;
//	}
//
//	private String parseProjectName(String metadataPath2) {
//		Pattern pattern = Pattern.compile("(.+)/(.+).arff");
//		String projectName = null;
//		
//		Matcher matcher = pattern.matcher(metadataPath);
//		while(matcher.find()) {
//			projectName = matcher.group(2);
//		}
//		
//		return projectName;
//	}

	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			metadataPath = cmd.getOptionValue("i");
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


class BaseSetting {
	String projectName;
	String referenceFolderPath;
	int averageBugFixingTime;
	int sizeOfTestSet;
	int gapTime;
	
	BaseSetting(){
		projectName = null;
		referenceFolderPath = null;
		averageBugFixingTime = 0;
		sizeOfTestSet = 0;
		gapTime = 0;
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
	public int getAverageBugFixingTime() {
		return averageBugFixingTime;
	}
	public void setAverageBugFixingTime(int averageBugFixingTime) {
		this.averageBugFixingTime = averageBugFixingTime;
	}
	public int getSizeOfTestSet() {
		return sizeOfTestSet;
	}
	public void setSizeOfTestSet(int sizeOfTestSet) {
		this.sizeOfTestSet = sizeOfTestSet;
	}

	public int getGapTime() {
		return gapTime;
	}

	public void setGapTime(int gapTime) {
		this.gapTime = gapTime;
	}
	
	
}
