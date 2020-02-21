package edu.handong.csee.isel;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetaData {

    public final List<String> metrics;
    public final ArrayList<HashMap<String,String>> metricToValueMapList;

    public MetaData(List<String> metrics, ArrayList <HashMap<String,String>> metricToValueMapList) {

        this.metrics =  metrics;
        this.metricToValueMapList = metricToValueMapList;
    }
}
