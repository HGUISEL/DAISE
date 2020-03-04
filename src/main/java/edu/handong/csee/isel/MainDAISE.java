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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
			for(String developerName : developerNameSet) {

				long numBuggy = 0;
				double totalEditedLine = 0;
				int editedFileCount = 0;
				HashMap<DeveloperInfo.WeekDay, Long> dayToCountMap = new HashMap<>();

				for (HashMap<String, String> metricToValueMap : metaData.metricToValueMapList) {
					String dev = metricToValueMap.get("AuthorID");
					if (!developerName.equals(dev)) {
						continue;
					}

					boolean isBug = metricToValueMap.get("isBuggy").equals("buggy");
					long editedLine = Long.parseLong(metricToValueMap.get("Modify Lines"));
					editedFileCount++;
					String weekDay = metricToValueMap.get("CommitDate");
					DeveloperInfo.WeekDay editedDay = toEnum(weekDay);

					if (isBug) {
						numBuggy++;
					}
					totalEditedLine += editedLine;
					dayToCountMap.putIfAbsent(editedDay, 1L);
					dayToCountMap.computeIfPresent(editedDay, (day, cnt) -> cnt++);
				}

				DeveloperInfo developerInfo = new DeveloperInfo(editedFileCount, numBuggy, totalEditedLine / editedFileCount, maxDay(dayToCountMap));
				developerInfoMap.put(developerName, developerInfo);
			}


			FileWriter out = new FileWriter(outputPath + File.separator + "Developer_" + metadataPath.substring(metadataPath.lastIndexOf(File.separator)+1));
			try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
					.withHeader(DeveloperInfo.CSVHeader))) {
				developerInfoMap.forEach((developerName, developerInfo) -> {
					try {
						printer.printRecord(developerName, String.valueOf(developerInfo.totalEditedFile),String.valueOf(developerInfo.numOfBug), String.valueOf(developerInfo.meanEditedLine), String.valueOf(developerInfo.mostCommitDay));
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
