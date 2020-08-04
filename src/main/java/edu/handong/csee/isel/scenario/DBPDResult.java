package edu.handong.csee.isel.scenario;

import org.apache.commons.csv.CSVRecord;

public class DBPDResult {
	String commitTime;
	String authorID;
	String realLabel;
	boolean isCorrect;
	int cluster;
	
	String predictLabel;
	String key;
	
	DBPDResult(){
		this.commitTime = null;
		this.authorID = null;
		this.realLabel = null;
		this.isCorrect = false;
		this.cluster = 0;
	}
	
	DBPDResult(CSVRecord record){
		this.cluster = Integer.parseInt(record.get("Cluster"));
		this.key = record.get("Key");
		this.commitTime = record.get("Commit Time");
		this.authorID = record.get("Author ID");
		this.predictLabel = record.get("P Label");
		this.realLabel = record.get("R Label");
	}

	public String getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(String commitTime) {
		this.commitTime = commitTime;
	}

	public String getAuthorID() {
		return authorID;
	}

	public void setAuthorID(String authorID) {
		this.authorID = authorID;
	}

	public String getRealLabel() {
		return realLabel;
	}

	public void setRealLabel(String realLabel) {
		this.realLabel = realLabel;
	}

	public boolean isCorrect() {
		return isCorrect;
	}

	public void setCorrect(boolean isCorrect) {
		this.isCorrect = isCorrect;
	}

	public int getCluster() {
		return cluster;
	}

	public void setCluster(int cluster) {
		this.cluster = cluster;
	}

	public String getPredictLabel() {
		return predictLabel;
	}

	public void setPredictLabel(String predictLabel) {
		this.predictLabel = predictLabel;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	

}
