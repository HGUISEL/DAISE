package edu.handong.csee.isel;

import java.util.Map;

public class DeveloperInfo {

    public static String[] CSVHeader = {"ID","totalCommit","totalCommitPath", "meanEditedLineInCommit", "meanEditedLineInCommitPath", "varianceOfCommit", "varianceOfCommitPath", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat","0h","1h","2h","3h","4h","5h","6h","7h","8h","9h","10h","11h","12h","13h","14h","15h","16h","17h","18h","19h","20h","21h","22h","23h"};

    public DeveloperInfo(String id, double totalCommit, double totalCommitPath, double meanEditedLineInCommit, double meanEditedLineInCommitPath, double varianceOfCommit, double varianceOfCommitPath, Map<WeekDay,Double> weekRatioMap, Map<Integer,Integer> hourMap) {
        ID = id;
        this.totalCommit = totalCommit;
        this.totalCommitPath = totalCommitPath;
        this.meanEditedLineInCommit = meanEditedLineInCommit;
        this.meanEditedLineInCommitPath = meanEditedLineInCommitPath;
        this.varianceOfCommit = varianceOfCommit;
        this.varianceOfCommitPath = varianceOfCommitPath;
        this.weekRatioMap = weekRatioMap;
        this.hourMap =hourMap;
    }



    static public enum WeekDay {Sun, Mon, Tue, Wed, Thu, Fri, Sat}


    public final String ID;
    public final double totalCommit;
    public final double totalCommitPath;
    public final double meanEditedLineInCommit;
    public final double meanEditedLineInCommitPath;
    public final double varianceOfCommit;
    public final double varianceOfCommitPath;
//    public final double Sun;
//    public final double Mon;
//    public final double Tue;
//    public final double Wed;
//    public final double Thu;
//    public final double Fri;
//    public final double Sat;

    public final Map<WeekDay,Double> weekRatioMap;
    public final Map<Integer,Integer> hourMap;

}
