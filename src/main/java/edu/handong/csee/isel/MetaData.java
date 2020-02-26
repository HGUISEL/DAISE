package edu.handong.csee.isel;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetaData {

    public static final String[] headers = {"isBuggy", "Modify Lines", "Add Lines", "Delete Lines", "Distribution modified Lines", "numOfBIC", "AuthorID", "fileAge", "SumOfSourceRevision", "SumOfDeveloper", "CommitHour", "CommitDate", "AGE", "numOfSubsystems", "numOfDirectories", "numOfFiles", "NUC", "developerExperience", "REXP", "LT", "Key"};

    public final List<String> metrics;
    public final ArrayList<HashMap<String,String>> metricToValueMapList;

    public MetaData(List<String> metrics, ArrayList <HashMap<String,String>> metricToValueMapList) {

        this.metrics =  metrics;
        this.metricToValueMapList = metricToValueMapList;
    }
}
