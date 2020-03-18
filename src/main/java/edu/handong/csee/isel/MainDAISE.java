package edu.handong.csee.isel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MainDAISE {
	String gitRepositoryPath;
	String metadataPath;
	String outputPath;
	private String startDate = "0000-00-00 00:00:00";
	private String endDate = "9999-99-99 99:99:99";
	boolean verbose;
	boolean help;

	public static void main(String[] args) throws Exception {
		MainDAISE main = new MainDAISE();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();

		if (parseOptions(options, args)) {

			MetaData metaData = Utils.readMetadataCSV(metadataPath); //testPrint(metaData);

			HashMap<String,DeveloperInfo> developerInfoMap = new HashMap<String,DeveloperInfo>();
			Set<String> developerNameSet = getDeveloperNameSet(metaData); // System.out.println(developerSet);

			HashMap<String, HashSet<String>> developerToCommitSetMap = new HashMap<>();
			for(String developerName : developerNameSet) {

				HashSet<String> commitSet = new HashSet<>();

				for (HashMap<String, String> metricToValueMap : metaData.metricToValueMapList) {
					String dev = metricToValueMap.get("AuthorID");
					if (!developerName.equals(dev)) {
						continue;
					}

					int endIndexOfCommit = metricToValueMap.get("Key").indexOf("-");
					String commit = metricToValueMap.get("Key").substring(0, endIndexOfCommit);

					commitSet.add(commit);

				}
				developerToCommitSetMap.put(developerName,commitSet);
			}

			HashMap<String, List<HashMap<String,String>>> commitToMetricToValueMapListMap = new HashMap<>();
			for (HashMap<String, String> metricToValueMap : metaData.metricToValueMapList) {
				int endIndexOfCommit = metricToValueMap.get("Key").indexOf("-");
				String commit = metricToValueMap.get("Key").substring(0, endIndexOfCommit);

				if(commitToMetricToValueMapListMap.containsKey(commit)) {
					List<HashMap<String,String>> list = commitToMetricToValueMapListMap.get(commit);
					list.add(metricToValueMap);
				} else {
					List<HashMap<String, String>> list = new ArrayList<>();
					list.add(metricToValueMap);
					commitToMetricToValueMapListMap.put(commit, list);
				}
			}

			for(String developer : developerNameSet) {

				int bugCount = 0;

				HashSet<String> commitSet = developerToCommitSetMap.get(developer);

				HashMap<DeveloperInfo.WeekDay, Double> dayOfWeekToRatioMap = new HashMap<>(); // Mon: 0.1, Two: 0.2, ..., Sat: 0.3 -> total: 1.0
				double meanOfEditedLineOfCommit;
				double meanOfEditedLineOfCommitPath = 0;
				double varianceOfCommit = 0;
				double varianceOfCommitPath = 0;

				// variance of commit, commit Path
				double totalCommit = commitSet.size();
				double totalCommitPath = 0;
				double totalEditedLineForEachCommit = 0;
				double totalEditedLineForEachCommitPath = 0;

				for(String commit : commitSet) {

					List<HashMap<String,String>> metricToValueMapList = commitToMetricToValueMapListMap.get(commit);

					totalCommitPath += metricToValueMapList.size();

					for(HashMap<String,String> metricToValueMap : metricToValueMapList) {

						double editedLine = Double.parseDouble(metricToValueMap.get("Modify Lines"));
						totalEditedLineForEachCommit += editedLine;
						totalEditedLineForEachCommitPath += editedLine;
					}
				}
				meanOfEditedLineOfCommit = totalEditedLineForEachCommit / totalCommit;
				meanOfEditedLineOfCommitPath = totalEditedLineForEachCommitPath / totalCommitPath;

				for(String commit : commitSet) {

					List<HashMap<String,String>> metricToValueMapList = commitToMetricToValueMapListMap.get(commit);

					for(HashMap<String,String> metricToValueMap : metricToValueMapList) {

						double editedLine = Double.parseDouble(metricToValueMap.get("Modify Lines"));



						varianceOfCommitPath += Math.pow(editedLine - meanOfEditedLineOfCommitPath, 2);
						varianceOfCommit += Math.pow(editedLine - meanOfEditedLineOfCommit, 2);
					}
				}
				varianceOfCommit /= totalCommit;
				varianceOfCommitPath /= totalCommitPath;

				// dayOfWeekToRatioMap

				for(String commit : commitSet) {
					List<HashMap<String,String>> metricToValueMapList = commitToMetricToValueMapListMap.get(commit);

					DeveloperInfo.WeekDay weekDay = toEnum(metricToValueMapList.get(0).get("CommitDate"));

					boolean isBug = metricToValueMapList.get(0).get("isBuggy").equals("buggy");
					if(isBug) {
						bugCount ++;
					}

					dayOfWeekToRatioMap.putIfAbsent(weekDay, 1.0);
					dayOfWeekToRatioMap.computeIfPresent(weekDay, (key,val) -> val++);
				}

				for(DeveloperInfo.WeekDay weekDay : dayOfWeekToRatioMap.keySet()) {

					double val = dayOfWeekToRatioMap.get(weekDay);
					dayOfWeekToRatioMap.put(weekDay, val / totalCommit);
				}

//				HashMap<String,DeveloperInfo> developerInfoMap = new HashMap<String,DeveloperInfo>();
				//{"ID","totalCommit","totalCommitPath", "meanEditedLineInCommit", "meanEditedLineInCommitPath", "varianceOfCommit", "varianceOfCommitPath", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
				double sun,mon,tue,wed,thu,fri,sat;
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Sun) ){
					sun = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Sun);
				} else {
					sun = 0;
				}
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Mon) ){
					mon = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Mon);
				} else {
					mon = 0;
				}
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Tue) ){
					tue = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Tue);
				} else {
					tue = 0;
				}
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Wed) ){
					wed = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Wed);
				} else {
					wed = 0;
				}
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Thu) ){
					thu = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Thu);
				} else {
					thu = 0;
				}
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Fri) ){
					fri = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Fri);
				} else {
					fri = 0;
				}
				if( dayOfWeekToRatioMap.containsKey(DeveloperInfo.WeekDay.Sat) ){
					sat = dayOfWeekToRatioMap.get(DeveloperInfo.WeekDay.Sat);
				} else {
					sat = 0;
				}

				DeveloperInfo developerInfo = new DeveloperInfo(developer,totalCommit,totalCommitPath,meanOfEditedLineOfCommit,meanOfEditedLineOfCommitPath,varianceOfCommit,varianceOfCommitPath,sun,mon,tue,wed,thu,fri,sat);
				developerInfoMap.put(developer,developerInfo);
			}



			FileWriter out = new FileWriter(outputPath + File.separator + "Developer_" + metadataPath.substring(metadataPath.lastIndexOf(File.separator)+1));
			try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
					.withHeader(DeveloperInfo.CSVHeader))) {
				developerInfoMap.forEach((developerName, developerInfo) -> {
					try {
						//{"ID","totalCommit","totalCommitPath", "meanEditedLineInCommit", "meanEditedLineInCommitPath", "varianceOfCommit", "varianceOfCommitPath", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
						printer.printRecord(developerName, String.valueOf(developerInfo.totalCommit),String.valueOf(developerInfo.totalCommitPath), String.valueOf(developerInfo.meanEditedLineInCommit), String.valueOf(developerInfo.meanEditedLineInCommitPath),String.valueOf(developerInfo.varianceOfCommit),String.valueOf(developerInfo.varianceOfCommitPath),String.valueOf(developerInfo.Sun),String.valueOf(developerInfo.Mon),String.valueOf(developerInfo.Tue),String.valueOf(developerInfo.Wed),String.valueOf(developerInfo.Thu),String.valueOf(developerInfo.Fri),String.valueOf(developerInfo.Sat));
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
				printer.flush();
			}

			if (help) {
				printHelp(options);
				return; 
			}
		}
		
		if (verbose) {
			System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
		}
	}

	private DeveloperInfo.WeekDay maxDay(HashMap<DeveloperInfo.WeekDay, Long> dayToCountMap) throws Exception {
		long max = 0;
		DeveloperInfo.WeekDay maxDay = null;

		for(DeveloperInfo.WeekDay day : dayToCountMap.keySet()) {

			long cnt = dayToCountMap.get(day);

			if(max < cnt) {
				maxDay = day;
				max = cnt;
			}
		}

		if(maxDay == null) {
			throw new Exception("Sum of Edited Day of the week (Sun, Mon, ..., Sat) is Zero");
		}

		return maxDay;
	}

	private DeveloperInfo.WeekDay toEnum(String weekDay) throws Exception {

		switch (weekDay.charAt(0)) {
			case 'M':
				return DeveloperInfo.WeekDay.Mon;

			case 'T': //thu, tue
				return weekDay.toUpperCase().equals("TUESDAY") ? DeveloperInfo.WeekDay.Tue : DeveloperInfo.WeekDay.Thu;

			case 'W':
				return DeveloperInfo.WeekDay.Wed;

			case 'F':
				return DeveloperInfo.WeekDay.Fri;

			case 'S': //sat, sun
				return weekDay.toUpperCase().equals("SATURDAY") ? DeveloperInfo.WeekDay.Sat : DeveloperInfo.WeekDay.Sun;
		}

		throw new Exception("The column CommitDate: '" + weekDay + "' cannot be parsed ");
	}

	private Set<String> getDeveloperNameSet(MetaData metaData) {

		HashSet<String> developerSet = new HashSet<>();

		for(HashMap<String,String> metricToValueMap : metaData.metricToValueMapList) {
			String name = metricToValueMap.get("AuthorID");
			developerSet.add(name);
		}

		return developerSet;
	}

	private void testPrint(MetaData metaData) {
		for(HashMap<String,String> metricToValueMap : metaData.metricToValueMapList) {


			int cnt = 1;
			for(String key : metaData.metrics) { //equal to metricToValueMap.keySet()

				String val = metricToValueMap.get(key);

				System.out.println("metric["+cnt+"]"+" key: " + key + ", value: " + val);
				cnt ++;
			}
		}
	}

	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			gitRepositoryPath = cmd.getOptionValue("i");
			outputPath = cmd.getOptionValue("o");
			if(outputPath.endsWith(File.separator)) {
				outputPath = outputPath.substring(0, outputPath.lastIndexOf(File.separator));
			}
			metadataPath = cmd.getOptionValue("m");
			startDate = cmd.getOptionValue("s");
			endDate = cmd.getOptionValue("e");
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
		options.addOption(Option.builder("i").longOpt("git")
				.desc("Git URI. Don't use double quotation marks")
				.hasArg()
				.argName("URI")
				.required()
				.build());// 필수
		
		options.addOption(Option.builder("m").longOpt("metadata")
				.desc("Address of meta data csv file. Don't use double quotation marks")
				.hasArg()
				.argName("URI")
				.build());
		
		options.addOption(Option.builder("o").longOpt("output")
				.desc("output path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.required()
				.build());
		
		options.addOption(Option.builder("s").longOpt("startdate")
				.desc("Start date for collecting bug-introducing changes. Format: \"yyyy-MM-dd HH:mm:ss\"")
				.hasArg()
				.argName("Start date")
				.required()
				.build());

		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes. Format: \"yyyy-MM-dd HH:mm:ss\"")
				.hasArg()
				.argName("End date")
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
