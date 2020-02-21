package edu.handong.csee.isel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Utils {
    public static MetaData readMetadataCSV(String metadataPath) throws IOException {
        ArrayList<HashMap<String,String>> metricToValueMapList = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(metadataPath));

        String line = br.readLine();

        String[] metrics = line.split(",");

        while((line = br.readLine()) != null) {

            HashMap<String,String> metricToValueMap = new HashMap<>();

            String[] values = line.split(",");
            for(int i = 0; i < metrics.length; i++) {
                metricToValueMap.put(metrics[i],values[i]);
            }

            metricToValueMapList.add(metricToValueMap);
        }

        MetaData metaData = new MetaData(Arrays.asList(metrics),metricToValueMapList);
        return metaData;
    }
}
