package edu.handong.csee.isel;

public class DeveloperInfo {

    public static String[] CSVHeader = {"ID","totalCommit","totalCommitPath", "meanEditedLineInCommit", "meanEditedLineInCommitPath", "varianceOfCommit", "varianceOfCommitPath", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    public DeveloperInfo(String id, double totalCommit, double totalCommitPath, double meanEditedLineInCommit, double meanEditedLineInCommitPath, double varianceOfCommit, double varianceOfCommitPath, double sun, double mon, double tue, double wed, double thu, double fri, double sat) {
        ID = id;
        this.totalCommit = totalCommit;
        this.totalCommitPath = totalCommitPath;
        this.meanEditedLineInCommit = meanEditedLineInCommit;
        this.meanEditedLineInCommitPath = meanEditedLineInCommitPath;
        this.varianceOfCommit = varianceOfCommit;
        this.varianceOfCommitPath = varianceOfCommitPath;
        Sun = sun;
        Mon = mon;
        Tue = tue;
        Wed = wed;
        Thu = thu;
        Fri = fri;
        Sat = sat;
    }



    public enum WeekDay {Sun, Mon, Tue, Wed, Thu, Fri, Sat}


    public final String ID;
    public final double totalCommit;
    public final double totalCommitPath;
    public final double meanEditedLineInCommit;
    public final double meanEditedLineInCommitPath;
    public final double varianceOfCommit;
    public final double varianceOfCommitPath;
    public final double Sun;
    public final double Mon;
    public final double Tue;
    public final double Wed;
    public final double Thu;
    public final double Fri;
    public final double Sat;

}
