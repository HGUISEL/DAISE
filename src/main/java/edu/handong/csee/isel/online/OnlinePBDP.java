package edu.handong.csee.isel.online;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class OnlinePBDP {
	String dataPath;
	String BICpath;
	boolean verbose;
	boolean help;
	static BaseSetting baseSet;
	
	private final static String firstcommitTimePatternStr = "'(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)'";
	private final static Pattern firstcommitTimePattern = Pattern.compile(firstcommitTimePatternStr);

	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);

	private final static String defaultLabelPatternStr = "@attribute @@class@@ \\{\\w+,(\\w+)\\}";
	private final static Pattern defaultLabelPattern = Pattern.compile(defaultLabelPatternStr);


	public static void main(String[] args) throws Exception {
		OnlinePBDP main = new OnlinePBDP();
		baseSet = new BaseSetting();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
	}
	
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			dataPath = cmd.getOptionValue("i");
			BICpath = cmd.getOptionValue("b");
			String outputPath = cmd.getOptionValue("o");
			if(outputPath.endsWith(File.separator)) {
				outputPath = outputPath.substring(0, outputPath.lastIndexOf(File.separator));
				baseSet.setOutputPath(outputPath);
			}else {
				baseSet.setOutputPath(outputPath);
			}

			if(cmd.hasOption("s") && cmd.hasOption("e")) {
				baseSet.setStartDate(cmd.getOptionValue("s"));
				baseSet.setEndDate(cmd.getOptionValue("e"));
			}else if(cmd.hasOption("s") || cmd.hasOption("e")) {
				System.out.println("Please enter Start date and end date");
			}else if(!(cmd.hasOption("s") || cmd.hasOption("e"))){
				baseSet.setStartDate(null);
			}


			if(cmd.hasOption("u") && cmd.hasOption("g")) {
				baseSet.setUpdateDays(Integer.parseInt(cmd.getOptionValue("u")));
				baseSet.setGapDays(Integer.parseInt(cmd.getOptionValue("g")));
			}
			else if(cmd.hasOption("u") && !cmd.hasOption("g")) {
				baseSet.setUpdateDays(Integer.parseInt(cmd.getOptionValue("u")));
				baseSet.setGapDays(0);
			}
			else if(!cmd.hasOption("u") && cmd.hasOption("g")) {
				baseSet.setGapDays(Integer.parseInt(cmd.getOptionValue("g")));
				baseSet.setUpdateDays(0);
			}
			else if(!(cmd.hasOption("u") && cmd.hasOption("g"))){
				baseSet.setGapDays(0);
				baseSet.setUpdateDays(0);
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
		//options
		options.addOption(Option.builder("s").longOpt("startdate")
				.desc("Start date for collecting training data. Format: \"yyyy-MM-dd HH:mm:ss\"")
				.hasArg()
				.argName("Start date")
				.build());

		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting test data. Format: \"yyyy-MM-dd HH:mm:ss\"")
				.hasArg()
				.argName("End date")
				.build());

		options.addOption(Option.builder("u").longOpt("updatedays")
				.desc("update date for collecting training data. Format: days")
				.hasArg()
				.argName("Update date")
				.build());

		options.addOption(Option.builder("g").longOpt("gapdays")
				.desc("gap date for collecting training data. Format: days")
				.hasArg()
				.argName("Gap date")
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