package edu.handong.csee.isel.data;

import java.util.ArrayList;

public class BugRatioInfo {
	String startDate = "0000-00-00 00:00:00";
	String midDate;
	int accumulatedDate = 0;
	int accumulatedNumofMetric = 0;
	int accumulatedNumofBug = 0;
	ArrayList<String> metricKey;
	String endDate;
	
	BugRatioInfo(){
		this.startDate = "0000-00-00 00:00:00";
		this.midDate = "0000-00-00 00:00:00";
		this.accumulatedDate = 0;
		this.accumulatedNumofMetric = 0;
		this.accumulatedNumofBug = 0;
		this.metricKey = new ArrayList<String>();
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public int getAccumulatedDate() {
		return accumulatedDate;
	}

	public void setAccumulatedDate(int accumulatedDate) {
		this.accumulatedDate = this.accumulatedDate + accumulatedDate;
	}

	public int getAccumulatedNumofMetric() {
		return accumulatedNumofMetric;
	}

	public void setAccumulatedNumofMetric(int accumulatedNumofMetric) {
		this.accumulatedNumofMetric = this.accumulatedNumofMetric + accumulatedNumofMetric;
	}

	public int getAccumulatedNumofBug() {
		return accumulatedNumofBug;
	}

	public void setAccumulatedNumofBug(int accumulatedNumofBug) {
		this.accumulatedNumofBug = this.accumulatedNumofBug + accumulatedNumofBug;
	}

	public ArrayList<String> getMetricKey() {
		return metricKey;
	}

	public void setMetricKey(String metricKey) {
		this.metricKey.add(metricKey);
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getMidDate() {
		return midDate;
	}

	public void setMidDate(String midDate) {
		this.midDate = midDate;
	}
	
	

}
