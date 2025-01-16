// DO NOT MODIFY this file.
// You can make modifications by overriding the methods in Analysis.java

import java.util.*;

public class PAVBase {

    public static class ResultTuple {
        public final String m;
        public final String p;
        public final String v;
        public final List<String> pV;

        public ResultTuple(String method, String prgpoint, String stmt,
                           List<String> intervalValues) {
            this.m = method;
            this.p = prgpoint;
            this.v = stmt;
            this.pV = intervalValues;
        }

        @Override
        public String toString() {
            return this.m + " " + this.p + " " + this.v;
        }
    }


    //  Redundant Code Already in Analysis.java File

//    protected static String fmtOutputLine(ResultTuple tup, String prefix) {
//        String line = tup.m + ": " + tup.p + ": " + tup.v + ": ";
//        List<String> intervalValues = tup.pV;
//        for(String interval: intervalValues){
//            line += interval+", ";
//        }
//        return (prefix + line);
//    }

//    protected static String fmtOutputLine(ResultTuple tup) {
//        return fmtOutputLine(tup, "");
//    }

//    protected static String[] fmtOutputData(Set<ResultTuple> data, String prefix) {
//
//        String[] outputlines = new String[ data.size() ];
//
//        int i = 0;
//        for (ResultTuple tup : data) {
//            outputlines[i] = fmtOutputLine(tup, prefix);
//            i++;
//        }
//
//        Arrays.sort(outputlines);
//        return outputlines;
//    }

//    protected static String[] fmtOutputData(Set<ResultTuple> data) {
//        return fmtOutputData(data, "");
//    }

    //   In Analysis.java
//    protected static String fmtOutputLine(ResultTuple tup) {
//        return fmtOutputLine(tup, "");
//    }
}