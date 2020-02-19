package edu.handong.csee.isel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class MainDAISE {
	String gitRepositoryPath;
	String metadataPath;
	String outputPath;
	private String startDate = "0000-00-00 00:00:00";
	private String endDate = "9999-99-99 99:99:99";
	boolean verbose;
	boolean help;

	public static void main(String[] args) {
		MainDAISE main = new MainDAISE();
		main.run(args);
	}
	
	private void run(String[] args){
		Options options = createOptions();

		if (parseOptions(options, args)) {
			if (help) {
				printHelp(options);
				return; 
			}
		}
		
		///
		
		if (verbose) {
			System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
		}
	}
	
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			gitRepositoryPath = cmd.getOptionValue("i");
			outputPath = cmd.getOptionValue("o");
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
				.argName("Start date")
				.required()
				.build());

		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes. Format: \"yyyy-MM-dd HH:mm:ss\"")
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
