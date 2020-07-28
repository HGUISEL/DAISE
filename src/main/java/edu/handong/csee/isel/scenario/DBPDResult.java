package edu.handong.csee.isel.scenario;

public class DBPDResult {
	String commitTime;
	String authorID;
	String realLabel;
	boolean isCorrect;
	int cluster;
	
	DBPDResult(){
		this.commitTime = null;
		this.authorID = null;
		this.realLabel = null;
		this.isCorrect = false;
		this.cluster = 0;
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
	

}
