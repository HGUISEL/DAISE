package edu.handong.csee.isel;

public class DeveloperInfo {

    public static String[] CSVHeader = {"ID","totalEditedFile","numOfBug", "meanEditedLine", "mostCommitDay"};

    public DeveloperInfo(long totalEditedLine, long numOfBug, double meanEditedLine, WeekDay mostCommitDay) {
        this.totalEditedFile = totalEditedLine;
        this.numOfBug = numOfBug;
        this.meanEditedLine = meanEditedLine;
        this.mostCommitDay = mostCommitDay;
    }

    public enum WeekDay {Sun, Mon, Tue, Wed, Thu, Fri, Sat}

    public final long totalEditedFile;
    public final long numOfBug;
    public final double meanEditedLine;
    public final WeekDay mostCommitDay;

}
