package edu.handong.csee.isel.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class ExtractData {
	static String projectName;
	static String output;
	static ArrayList<String> kameiAttr;
	static ArrayList<String> OnlineAttr;
	static ArrayList<String> PDPAttr;
	static ArrayList<String> noBOWonlineAttr;
	static ArrayList<String> noBOWpdpAttr;
	
	private final static String attribetePatternStr = "@attribute\\s(.+)\\s[\\{|[a-zA-Z]+]";
	private final static Pattern attribetePattern = Pattern.compile(attribetePatternStr);
	
	private final static String dataPatternStr = "(\\d+)\\s(.+)";
	private final static Pattern dataPattern = Pattern.compile(dataPatternStr);
	
	/*
	 * args[0] : input projectName-data.arff path
	 * args[1] : output path
	 * args[2] : kamei(k)or pdp(p) or online()o or noBOW online(bow)?
	 */

	public static void main(String[] args) throws Exception {
		TreeMap<String, String>  kameiAttrIndex = new TreeMap<>();
		TreeMap<String, String>  PDPAttrIndex = new TreeMap<>();
		TreeMap<String, String>  onlineAttriIndex = new TreeMap<>();
		TreeMap<String, String>  noBOWonlineAttriIndex = new TreeMap<>();
		TreeMap<String, String>  noBOWpdpAttriIndex = new TreeMap<>();
		ArrayList<String> attributeLineList = new ArrayList<String>(); //use again
		ArrayList<String> dataLineList = new ArrayList<String>();
		
		File originArff = new File(args[0]);
		String mode = args[2];
		
		output = args[1];
		Pattern projectNamePattern = Pattern.compile(".+/(.+)\\.arff");
		Matcher ma = projectNamePattern.matcher(args[0]);
		while(ma.find()) {
			projectName = ma.group(1);
		}
		System.out.println(args[2]);
		
		if(mode.compareTo("k") == 0)
			initKameiMetric();		
		else if (mode.compareTo("p") == 0)
			initPDPMetric();
		else if (mode.compareTo("o") == 0)
			initOnlineMetric();
		else if (mode.compareTo("bow") == 0)
			initNoBOWonlineMetric();
		
		String content = FileUtils.readFileToString(originArff, "UTF-8");
		String[] lines = content.split("\n");
		
		boolean dataPart = false;
		int attrIndex = 0;
		for (String line : lines) {
			if (dataPart) {
				dataLineList.add(line);
				continue;

			}else if(!dataPart){
				
				if(line.startsWith("@attribute")) {
					Matcher m = attribetePattern.matcher(line);
					while(m.find()) {
//						System.out.println(m.group(1));
						if((mode.compareTo("k") == 0)&&(kameiAttr.contains(m.group(1)))) {
							kameiAttrIndex.put(Integer.toString(attrIndex),line);
						}
						if((mode.compareTo("o") == 0)&&!(OnlineAttr.contains(m.group(1)))) {
							onlineAttriIndex.put(Integer.toString(attrIndex),line);
						}
						if((mode.compareTo("p") == 0)&&!(PDPAttr.contains(m.group(1)))){
							PDPAttrIndex.put(Integer.toString(attrIndex),line);
						}
						
						if(mode.compareTo("bow") == 0){
							for(String attr : noBOWonlineAttr) {
								if(m.group(1).startsWith(attr)) {
									noBOWonlineAttriIndex.put(Integer.toString(attrIndex),line);
								}
							}
						}
						
						if(mode.compareTo("bowP") == 0){
							for(String attr : noBOWpdpAttr) {
								if(m.group(1).startsWith(attr)) {
									noBOWpdpAttriIndex.put(Integer.toString(attrIndex),line);
								}
							}
						}
						
					}
					
					if(line.startsWith("@attribute Key ")) {
						kameiAttrIndex.put(Integer.toString(attrIndex),line);
						onlineAttriIndex.put(Integer.toString(attrIndex),line);
						PDPAttrIndex.put(Integer.toString(attrIndex),line);
						noBOWonlineAttriIndex.put(Integer.toString(attrIndex),line);
						noBOWpdpAttriIndex.put(Integer.toString(attrIndex),line);
					}
					
					
					attributeLineList.add(line);
					attrIndex++;
				}
				
				if (line.startsWith("@data")) {
					dataPart = true;
				}
			}
		}
//		check kamei attr
//		for(String key : kameiAttrIndex.keySet()) {
//			String index = kameiAttrIndex.get(key);
//			System.out.println("Arr : " + key + " Index : " + index);
//		}
		
//		for(Integer key : PDPAttrIndex.keySet()) {
//			String index = PDPAttrIndex.get(key);
//			System.out.println("Arr : " + key + " Index : " + index);
//		}
		
		if(args[2].compareTo("k") == 0)
			ExtractKameiMetricFrom(attributeLineList, dataLineList, kameiAttrIndex);
		else if (args[2].compareTo("p") == 0)
			ExtractPDPmetricFrom(attributeLineList, dataLineList, PDPAttrIndex,"p");
		else if (args[2].compareTo("o") == 0)
			ExtractPDPmetricFrom(attributeLineList, dataLineList, onlineAttriIndex,"o");
		else if (args[2].compareTo("bow") == 0)
			ExtractPDPmetricFrom(attributeLineList, dataLineList, noBOWonlineAttriIndex,"o");
		else if (args[2].compareTo("bowP") == 0)
			ExtractPDPmetricFrom(attributeLineList, dataLineList, noBOWpdpAttriIndex,"p");

	}
	
	private static void ExtractPDPmetricFrom(ArrayList<String> attributeLineList, ArrayList<String> dataLineList,
			TreeMap<String, String> PDPAttrIndex, String string) throws IOException {
		
		HashMap<String, Integer> PDPNumIndex = new HashMap<>();
		ArrayList<String> PDPAttributeLineList = new ArrayList<>();
		
		TreeMap<Integer, String>  PDPAttrIndexSort = new TreeMap<>();
		for(String index : PDPAttrIndex.keySet()) {
			PDPAttrIndexSort.put(Integer.parseInt(index), PDPAttrIndex.get(index));
		}
		
		int num = 0;
		for(int index : PDPAttrIndexSort.keySet()) {
			String arr = PDPAttrIndex.get(Integer.toString(index));
			PDPAttributeLineList.add(arr);
			PDPNumIndex.put(Integer.toString(index), num);
			num++;
		}
		
		//make data
		ArrayList<String> PDPData = new ArrayList<String>();
		
		for(String dataLine : dataLineList) {
			String data = parsingIndex(PDPAttrIndex,PDPNumIndex, dataLine);
			PDPData.add(data);
//			System.out.println(data);
//			break;
		}
		
		Save2Arff(PDPAttributeLineList, PDPData, string);
		
	}
	
	private static void ExtractKameiMetricFrom(ArrayList<String> attributeLineList, ArrayList<String> dataLineList,
			TreeMap<String, String> kameiAttrIndex) throws IOException {

		//make atrr
		HashMap<String, Integer>  kameiNumIndex = new HashMap<>(); // attribute num 77884 : 12
		ArrayList<String> kameiAttributeLineList = new ArrayList<>(); //use csv print : @attribute meta_data-LT numeric
		kameiAttributeLineList.add(attributeLineList.get(0));
		
		int num = 1;
		for(String index : kameiAttrIndex.keySet()) {
			String arr = kameiAttrIndex.get(index);
			kameiAttributeLineList.add(arr);
			kameiNumIndex.put(index, num);
			num++;
		}
		
		//make data
		ArrayList<String> kemaiData = new ArrayList<String>();
		
		for(String dataLine : dataLineList) {
			String data = parsingIndexNData(kameiAttrIndex,kameiNumIndex, dataLine);
			kemaiData.add(data);
		}
		
		Save2Arff(kameiAttributeLineList, kemaiData, "k");
		
	}
	
	private static String parsingIndex(TreeMap<String, String> kameiAttrIndex, HashMap<String, Integer>  kameiNumIndex, String dataLine) {
		
		String[] lines = dataLine.split(",");
		String data = "{";
		
		for (String line : lines) {
			Matcher m = dataPattern.matcher(line);
			while(m.find()) {
				if(kameiAttrIndex.containsKey(m.group(1))) {
					int reIndex = kameiNumIndex.get(m.group(1));
					data = data + reIndex + " " + m.group(2) + ",";
				}else {
					continue;
				}
			}
		}
		
		if(data.endsWith("},")) {
			data = data.substring(0,data.length()-1);
		}else {
			data = data.substring(0,data.length()-1);
			data = data + "}";
		}
		
		return data;
	}
	
	private static String parsingIndexNData(TreeMap<String, String> kameiAttrIndex, HashMap<String, Integer>  kameiNumIndex, String dataLine) {
		
		String[] lines = dataLine.split(",");
		String data = "";
		if(lines[0].startsWith("{0 ")) {
			data = lines[0] + ",";
		}else {
			data = "{";
		}
		
		for (String line : lines) {
			Matcher m = dataPattern.matcher(line);
			while(m.find()) {
				if(kameiAttrIndex.containsKey(m.group(1))) {
					int reIndex = kameiNumIndex.get(m.group(1));
					data = data + reIndex + " " + m.group(2) + ",";
				}else {
					continue;
				}
			}
		}
		
		if(data.endsWith("},")) {
			data = data.substring(0,data.length()-1);
		}else {
			data = data.substring(0,data.length()-1);
			data = data + "}";
		}
		
		return data;
	}
	
	private static void Save2Arff(ArrayList<String> attributeLineList, ArrayList<String> data, String string) throws IOException {
		File arff = null;
		if(string.compareTo("k") == 0) {
			arff = new File(output + File.separator + projectName + "-kamei.arff");
		}else if(string.compareTo("p") == 0){
			arff = new File(output + File.separator + projectName + "-PDP.arff");
		}else if(string.compareTo("o") == 0) {
			arff = new File(output + File.separator + projectName + "-online.arff");
		}
		
		StringBuffer newContentBuf = new StringBuffer();
		
		newContentBuf.append("@relation weka.filters.unsupervised.instance.NonSparseToSparse\n\n");

		for (String line : attributeLineList) {
			newContentBuf.append(line + "\n");
		}

		newContentBuf.append("\n@data\n");

		for (String line : data) {
			newContentBuf.append(line + "\n");
		}

		FileUtils.write(arff, newContentBuf.toString(), "UTF-8");
		

	}
	
	static void initKameiMetric() {
		kameiAttr = new ArrayList<String>(Arrays.asList(
				"meta_data-numOfSubsystems",//NS
				"meta_data-numOfDirectories",//ND
				"meta_data-numOfFiles",//NF
				"'meta_data-Distribution modified Lines'",//Entropy
				"'meta_data-Add Lines'",//LA
				"'meta_data-Delete Lines'",//LD
				"meta_data-LT",//LT
				"meta_data-SumOfDeveloper",//NDEV
				"meta_data-AGE",//AGE
				"meta_data-NUC",//NUC
				"meta_data-developerExperience",//EXP
				"meta_data-REXP",//REXP
				"meta_data-SEXP"//SEXP
				));
	}
	
	static void initOnlineMetric() {  //not online metrics
		OnlineAttr = new ArrayList<String>(Arrays.asList(
				"'meta_data-Distribution modified Lines'",
				"meta_data-SumOfDeveloper",
				"meta_data-AGE",
				"meta_data-numOfSubsystems",
				"meta_data-numOfDirectories",
				"meta_data-numOfFiles",
				"meta_data-NUC",
				"meta_data-developerExperience",
				"meta_data-REXP",
				"meta_data-SEXP",
				"meta_data-LT"
				));
	}
	
	static void initPDPMetric() { //not PDP metrics
		PDPAttr = new ArrayList<String>(Arrays.asList(
				"'meta_data-Modify Lines'",
				"'meta_data-Add Lines'",
				"'meta_data-Delete Lines'",
				" meta_data-numOfModifyChunk",
				"meta_data-numOfAddChunk",
				"meta_data-numOfDeleteChunk",
				"'meta_data-Distribution modified Lines'",
				"meta_data-SumOfDeveloper",
				"meta_data-AGE",
				"meta_data-numOfSubsystems",
				"meta_data-numOfDirectories",
				"meta_data-numOfFiles",
				"meta_data-NUC",
				"meta_data-developerExperience",
				"meta_data-REXP",
				"meta_data-SEXP",
				"meta_data-LT"
				));
	}
	
	static void initNoBOWonlineMetric() {  //not online metrics
		noBOWonlineAttr = new ArrayList<String>(Arrays.asList(
				"'meta_data-Modify Lines'",
				"'meta_data-Add Lines'",
				"'meta_data-Delete Lines'",
				"meta_data-numOfBIC",
				"meta_data-AuthorID",
				"meta_data-fileAge",
				"meta_data-SumOfSourceRevision",
				"meta_data-CommitHour",
				"meta_data-CommitDate",
				"MOV",
				"INS",
				"UPD",
				"DEL",
				"meta_data-commitTime",
				"Key",
				"@@class@@"
				));
	}
	
	static void initNoBOWpdpMetric() {  //not PDP metrics
		noBOWpdpAttr = new ArrayList<String>(Arrays.asList(
				"meta_data-numOfBIC",
				"meta_data-AuthorID",
				"meta_data-fileAge",
				"meta_data-SumOfSourceRevision",
				"meta_data-CommitHour",
				"meta_data-CommitDate",
				"MOV",
				"INS",
				"UPD",
				"DEL",
				"meta_data-commitTime",
				"Key",
				"@@class@@"
				));
	}
	
}
