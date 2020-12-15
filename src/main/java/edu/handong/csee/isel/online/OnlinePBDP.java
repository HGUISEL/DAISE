package edu.handong.csee.isel.online;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class OnlinePBDP {
	String outputPath;
	String projectName;
	
	RunDate firstRunDate;
	
	ArrayList<String> attributeLineList;
	TreeMap<String,String> key_fixTime = new TreeMap<>();
	TreeMap<String,TreeSet<String>> commitTime_commitHash;
	HashMap<String,HashMap<String,String>> commitHash_key_data;
	HashMap<String,HashMap<String,Boolean>> commitHash_key_isBuggy;
	
	String trS;//training start time
	String trE_gapS;//training end time == gap start time
	String gapE_teS;//gap start time == test start time 
	String teE;//test end
	
	public void profilingBasedDefectPrediction() {
		//make PBDP directory
		File PBDPdir = new File(outputPath);
		String directoryPath = PBDPdir.getAbsolutePath();
		PBDPdir.mkdir();
		
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setFirstRunDate(RunDate firstRunDate) {
		this.firstRunDate = firstRunDate;
	}

	public void setAttributeLineList(ArrayList<String> attributeLineList) {
		this.attributeLineList = attributeLineList;
	}

	public void setKey_fixTime(TreeMap<String, String> key_fixTime) {
		this.key_fixTime = key_fixTime;
	}

	public void setCommitTime_commitHash(TreeMap<String, TreeSet<String>> commitTime_commitHash) {
		this.commitTime_commitHash = commitTime_commitHash;
	}

	public void setCommitHash_key_data(HashMap<String, HashMap<String, String>> commitHash_key_data) {
		this.commitHash_key_data = commitHash_key_data;
	}

	public void setCommitHash_key_isBuggy(HashMap<String, HashMap<String, Boolean>> commitHash_key_isBuggy) {
		this.commitHash_key_isBuggy = commitHash_key_isBuggy;
	}
	

}