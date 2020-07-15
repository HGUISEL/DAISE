package edu.handong.csee.isel.daise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;



public class MainD {
	String gitRepositoryPath;
	String metadataPath;
	String outputPath;
	private String startDate = "0000-00-00 00:00:00";
	private String endDate = "9999-99-99 99:99:99";
	boolean verbose;
	boolean help;
	ArrayList<RevCommit> commits = new ArrayList<RevCommit>();
	public static HashMap<String,DeveloperInformation> developerInfo = new HashMap<String,DeveloperInformation>();//////이놈!!!
	String projectName;
	
	public static void main(String[] args) throws Exception {
		MainD main = new MainD();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();
		DeveloperInformation developerInformation;
		
		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			Pattern pattern = Pattern.compile(".+/(.+)");
			Matcher matcher = pattern.matcher(gitRepositoryPath);
			while(matcher.find()) {
				projectName = matcher.group(1);
			}
			
			Git git = Git.open(new File(gitRepositoryPath));
			int count = 0;
			
			Iterable<RevCommit> initialCommits = git.log().call();
			Repository repo = git.getRepository();
			
			for (RevCommit initialCommit : initialCommits) {
				commits.add(count,initialCommit);
				count++;
			}
			
			for (int commitIndex = commits.size()-1; commitIndex > -1; commitIndex--) {
				RevCommit commit = commits.get(commitIndex);
				
				String commitTime = Utils.getStringDateTimeFromCommit(commit);//커밋 날짜 yyyy-MM-dd HH:mm:ss
				if(!(startDate.compareTo(commitTime)<=0 && endDate.compareTo(commitTime)>=0))
					continue;
				
				if (commit.getParentCount() == 0) continue;
				RevCommit parent = commit.getParent(0);
				if (parent == null)
					continue;

				//source
				List<DiffEntry> diff = Utils.diff(parent, commit, repo);
				boolean istherejavafile = false;
				
				for (DiffEntry entry : diff) {
					String sourcePath = entry.getNewPath().toString();
					String oldPath = entry.getOldPath();
					
					if (oldPath.equals("/dev/null") || sourcePath.indexOf("Test") >= 0 || !sourcePath.endsWith(".java"))
						continue;
					istherejavafile = true;
				}
				
				if(istherejavafile == false) continue;
				
				String authorId = Utils.parseAuthorID(commit.getAuthorIdent().toString());
				
				if(!developerInfo.containsKey(authorId)) {
					developerInformation = new DeveloperInformation(commitTime);
					developerInfo.put(authorId, developerInformation);
				}else {
					developerInformation = developerInfo.get(authorId);
				}
				
				developerInformation.setEndDate(commitTime);
				developerInformation.setNumofCommit();
			}
			
			countActiveDeveloper();
			
			Save2CSV();
			
			//print Hashmap
//			Set<Map.Entry<String, DeveloperInformation>> entries = developerInfo.entrySet();
//			for (Map.Entry<String,DeveloperInformation> entry : entries) {
//				String key = entry.getKey();
//
//				System.out.println(key);
//				
//				String start = entry.getValue().getStartDate();
//				String end = entry.getValue().endDate;
//				int num = entry.getValue().getNumofCommit();
//
//				System.out.println(start + "  " + end + "  " + num);
//
//				System.out.println("-----------------------------");
//			}
			
			if(verbose) {
				
				// TODO list all files in the path
				
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
				
			}
		}
	}
	
	private void countActiveDeveloper() {
		Set<Map.Entry<String, DeveloperInformation>> entries = developerInfo.entrySet();
		
		for (Map.Entry<String,DeveloperInformation> entry : entries) {
			String key = entry.getKey();
			
			int num = entry.getValue().getNumofCommit();
			if(num < 2) continue;//the developer have at least 10 commits
			
			String developerFirst = entry.getValue().getStartDate();
			
			for (Map.Entry<String,DeveloperInformation> developer : entries) {
				if(key.equals(developer.getKey())) continue;
				String first = developer.getValue().getStartDate();
				String last = developer.getValue().getEndDate();
				
				if(developerFirst.compareTo(first) >= 0 && developerFirst.compareTo(last) <= 0) {
					entry.getValue().setNumOfActiveDeveloper();
				}
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
	
	private void Save2CSV() throws Exception {
		BufferedWriter writer;
		writer = new BufferedWriter(new FileWriter(outputPath + File.separator + projectName + ".csv"));//.xlsx

		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Developer E-mail","First Commit","Last Commit","Number of Commit","Number of active Dev","Ratio of active Dev"));
		
		Set<Map.Entry<String, DeveloperInformation>> entries = developerInfo.entrySet();
		for (Map.Entry<String,DeveloperInformation> entry : entries) {
			String key = entry.getKey();
			
			String first = entry.getValue().getStartDate();
			String last = entry.getValue().getEndDate();
			int num = entry.getValue().getNumofCommit();
			int avtiveDeveloper = entry.getValue().getNumOfActiveDeveloper();
			float ratio = (float)avtiveDeveloper/(float)entries.size();

			csvPrinter.printRecord(key,first,last,num,avtiveDeveloper,ratio);
		}
		
		csvPrinter.close();
		writer.close();

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
//				.required()
				.build());

		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes. Format: \"yyyy-MM-dd HH:mm:ss\"")
				.hasArg()
				.argName("End date")
//				.required()
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
