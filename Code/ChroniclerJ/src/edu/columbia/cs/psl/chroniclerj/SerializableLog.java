
package edu.columbia.cs.psl.chroniclerj;

import java.io.Serializable;

public class SerializableLog implements Serializable {

    private static final long serialVersionUID = 4627796984904522647L;

    public static Object[] aLog = new Object[Constants.DEFAULT_LOG_SIZE];

    public static int[] iLog = new int[Constants.DEFAULT_LOG_SIZE];

    public static long[] jLog = new long[Constants.DEFAULT_LOG_SIZE];

    public static float[] fLog = new float[Constants.DEFAULT_LOG_SIZE];

    public static double[] dLog = new double[Constants.DEFAULT_LOG_SIZE];

    public static byte[] bLog = new byte[Constants.DEFAULT_LOG_SIZE];

    public static boolean[] zLog = new boolean[Constants.DEFAULT_LOG_SIZE];

    public static char[] cLog = new char[Constants.DEFAULT_LOG_SIZE];

    public static short[] sLog = new short[Constants.DEFAULT_LOG_SIZE];

    public static String[] aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] iLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] jLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] fLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] dLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] bLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] zLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] cLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] sLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] aLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] iLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] jLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] fLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] dLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] bLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] zLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] cLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] sLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    public static int logsize = 0;

    public static int aLog_fill, iLog_fill, jLog_fill, fLog_fill, dLog_fill, bLog_fill, zLog_fill,
            cLog_fill, sLog_fill;

}
