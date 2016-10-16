
package edu.columbia.cs.psl.chroniclerj;

import java.util.HashMap;


public class ExportedLog {
    public static Object[] aLog = new Object[Constants.DEFAULT_LOG_SIZE];

    public static String[] aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] aLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    
    public static int aLog_fill;

    public static int globalReplayIndex = 0;

    public static HashMap<String, Integer> aLog_replayIndex = new HashMap<String, Integer>();

    public static void clearLog() {
        aLog = new Object[Constants.DEFAULT_LOG_SIZE];
        aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
        aLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
        aLog_fill = 0;
        globalReplayIndex = 0;
    }
}
