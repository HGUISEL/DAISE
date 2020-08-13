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
	
	private final static String attribetePatternStr = "@attribute\\s(.+)\\s.+";
	private final static Pattern attribetePattern = Pattern.compile(attribetePatternStr);
	
	private final static String dataPatternStr = "(.+)\\s(.+)";
	private final static Pattern dataPattern = Pattern.compile(dataPatternStr);

	public static void main(String[] args) throws Exception {
		TreeMap<String, String>  kameiAttrIndex = new TreeMap<>();
		ArrayList<String> attributeLineList = new ArrayList<String>(); //use again
		ArrayList<String> dataLineList = new ArrayList<String>();
		
		File originArff = new File(args[0]);
		
		output = args[1];
		Pattern projectNamePattern = Pattern.compile(".+/(.+)\\.arff");
		Matcher ma = projectNamePattern.matcher(args[0]);
		while(ma.find()) {
			projectName = ma.group(1);
		}
				
		initKameiMetric();
		
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
						if(kameiAttr.contains(m.group(1))) {
							kameiAttrIndex.put(Integer.toString(attrIndex),line);
						}
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
		
		ExtractKameiMetricFrom(attributeLineList, dataLineList, kameiAttrIndex);

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
		
		data = data.substring(0,data.length()-1);
		
		return data;
	}
	
	private static void Save2Arff(ArrayList<String> attributeLineList, ArrayList<String> data, String string) throws IOException {
		File arff;
		if(string.compareTo("k") == 0) {
			arff = new File(output + File.separator + projectName + "-kamei.arff");
		}else {
			arff = new File(output + File.separator + projectName + "-PDP.arff");
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



	void ExtractPDPmetricFrom() {
//		TreeMap<string,string> tm = new TreeMap<string,string>(kameiAttrIndex);
//		Iterator<string> keyiterator = tm.descendingKeySet().iterator();
		
//		Set<String> keyset = kameiAttrIndex.keySet(); 
//		Iterator<String> keyiterator = kameiAttrIndex.keySet( ).iterator( );
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
				"meta_data-REXP"//REXP
				//no SEXP
				));
	}
}
