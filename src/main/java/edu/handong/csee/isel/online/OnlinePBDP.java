package edu.handong.csee.isel.online;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class OnlinePBDP {
	String outputPath;
	String projectName;
	
	RunDate firstRunDate;
	
	String trS;//training start time
	String trE_gapS;//training end time == gap start time
	String gapE_teS;//gap start time == test start time 
	String teE;//test end
	
	public void profilingBasedDefectPrediction(ArrayList<String> attributeLineList,
			TreeMap<String,String> key_fixTime,
			TreeMap<String,TreeSet<String>> commitTime_commitHash,
			HashMap<String,HashMap<String,String>> commitHash_key_data,
			HashMap<String,HashMap<String,Boolean>> commitHash_key_isBuggy,
			HashMap<String,String> commitHash_developer) {
		
		System.out.println();
		System.out.println();
		System.out.println();
		
		//make PBDP directory
		File PBDPdir = new File(outputPath);
		String directoryPath = PBDPdir.getAbsolutePath();
		PBDPdir.mkdir();
		
		//cal the number of developer commit in training set
		HashMap<String,ArrayList<String>> developerID = new HashMap<>();
		int i = 0;
		for(String commitTime : commitTime_commitHash.keySet()) {
			if(!(trS.compareTo(commitTime)<=0 && commitTime.compareTo(trE_gapS)<=0))
				continue;
			
			TreeSet<String> commitHashs = commitTime_commitHash.get(commitTime);
			for(String commitHash : commitHashs) {
				
			}
			
			if(i == 10)break;
			i++;
		}
		
		
		TreeMap<Integer,ArrayList<String>> numOfCommit_developer = new TreeMap<>(Collections.reverseOrder());

		
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setFirstRunDate(RunDate firstRunDate) {
		this.trS = firstRunDate.getTrS();//training start time
		this.trE_gapS = firstRunDate.getTrE_gapS();//training end time == gap start time
		this.gapE_teS = firstRunDate.getGapE_teS();//gap start time == test start time 
		this.teE = firstRunDate.getTeE();//test end
	}
	

}