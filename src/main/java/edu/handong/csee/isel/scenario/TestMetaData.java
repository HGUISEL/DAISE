package edu.handong.csee.isel.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestMetaData {
	
    public static final String[] headers = {"isBuggy", "Modify Lines", "Add Lines", "Delete Lines", "Distribution modified Lines", "numOfBIC", "AuthorID", "fileAge", "SumOfSourceRevision", "SumOfDeveloper", "CommitHour", "CommitDate", "AGE", "numOfSubsystems", "numOfDirectories", "numOfFiles", "NUC", "developerExperience", "REXP", "LT", "commitTime", "Key"};

    public final List<String> metrics;
    public final ArrayList<HashMap<String,String>> metricToValueMapList;

    public TestMetaData(List<String> metrics, ArrayList <HashMap<String,String>> metricToValueMapList) {

        this.metrics =  metrics;
        this.metricToValueMapList = metricToValueMapList;
    }

}
