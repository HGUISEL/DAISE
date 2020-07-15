package edu.handong.csee.isel.daise;

public class DeveloperInformation {
	String startDate;
	String endDate;
	int numofCommit;
	int numOfActiveDeveloper;
	
	DeveloperInformation(String startDate){
		this.startDate = startDate;
		this.endDate = null;
		this.numofCommit = 0;
		this.numOfActiveDeveloper = 0;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public int getNumofCommit() {
		return numofCommit;
	}

	public void setNumofCommit() {
		this.numofCommit++;
	}

	public int getNumOfActiveDeveloper() {
		return numOfActiveDeveloper;
	}

	public void setNumOfActiveDeveloper() {
		this.numOfActiveDeveloper++;
	}
	
}
