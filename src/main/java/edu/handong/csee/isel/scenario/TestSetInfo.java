package edu.handong.csee.isel.scenario;

public class TestSetInfo {
	String commitHashSource;
	String data;
	
	TestSetInfo(String commitHashSource, String data){
		this.commitHashSource = commitHashSource;
		this.data = data;
	}

	public String getCommitHashSource() {
		return commitHashSource;
	}

	public String getData() {
		return data;
	}
	
	

}
