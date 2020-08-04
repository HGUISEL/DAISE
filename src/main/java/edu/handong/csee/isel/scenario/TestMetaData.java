package edu.handong.csee.isel.scenario;

import org.apache.commons.csv.CSVRecord;

public class TestMetaData {
	String isBuggy;
	String Modify_Lines;
	String Add_Lines;
	String Delete_Lines;
	String Distribution_modified_Lines;
	String numOfBIC;
	String AuthorID;
	String fileAge;
	String SumOfSourceRevision;
	String SumOfDeveloper;
	String CommitHour;
	String CommitDate;
	String AGE;
	String numOfSubsystems;
	String numOfDirectories;
	String numOfFiles;
	String NUC;
	String developerExperience;
	String REXP;
	String LT;
	String Key;
	
	TestMetaData(CSVRecord record){
		this.isBuggy = record.get("isBuggy");
		this.Modify_Lines = record.get("Modify Lines");
		this.Add_Lines = record.get("Add Lines");
		this.Delete_Lines = record.get("Delete Lines");
		this.Distribution_modified_Lines = record.get("Distribution modified Lines");
		this.numOfBIC = record.get("numOfBIC");
		this.AuthorID = record.get("AuthorID");
		this.SumOfSourceRevision = record.get("SumOfSourceRevision");
		this.SumOfDeveloper = record.get("SumOfDeveloper");
		this.CommitHour = record.get("CommitHour");
		this.CommitDate = record.get("CommitDate");
		this.AGE = record.get("AGE");
		this.numOfSubsystems = record.get("numOfSubsystems");
		this.numOfDirectories = record.get("numOfDirectories");
		this.numOfFiles = record.get("numOfFiles");
		this.NUC = record.get("NUC");
		this.developerExperience = record.get("developerExperience");
		this.REXP = record.get("REXP");
		this.LT = record.get("LT");
		this.Key = record.get("Key");
	}

	public String getIsBuggy() {
		return isBuggy;
	}

	public String getModify_Lines() {
		return Modify_Lines;
	}

	public String getAdd_Lines() {
		return Add_Lines;
	}

	public String getDelete_Lines() {
		return Delete_Lines;
	}

	public String getDistribution_modified_Lines() {
		return Distribution_modified_Lines;
	}

	public String getNumOfBIC() {
		return numOfBIC;
	}

	public String getAuthorID() {
		return AuthorID;
	}

	public String getFileAge() {
		return fileAge;
	}

	public String getSumOfSourceRevision() {
		return SumOfSourceRevision;
	}

	public String getSumOfDeveloper() {
		return SumOfDeveloper;
	}

	public String getCommitHour() {
		return CommitHour;
	}

	public String getCommitDate() {
		return CommitDate;
	}

	public String getAGE() {
		return AGE;
	}

	public String getNumOfSubsystems() {
		return numOfSubsystems;
	}

	public String getNumOfDirectories() {
		return numOfDirectories;
	}

	public String getNumOfFiles() {
		return numOfFiles;
	}

	public String getNUC() {
		return NUC;
	}

	public String getDeveloperExperience() {
		return developerExperience;
	}

	public String getREXP() {
		return REXP;
	}

	public String getLT() {
		return LT;
	}

	public String getKey() {
		return Key;
	}
	
	

}
