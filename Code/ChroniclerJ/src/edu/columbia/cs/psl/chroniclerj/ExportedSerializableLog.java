
package edu.columbia.cs.psl.chroniclerj;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class ExportedSerializableLog implements Serializable {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1166783255069514273L;

    public static Object[] aLog = new Object[Constants.DEFAULT_LOG_SIZE];

    public static int[] iLog = new int[Constants.DEFAULT_LOG_SIZE];

    public static long[] jLog = new long[Constants.DEFAULT_LOG_SIZE];

    public static float[] fLog = new float[Constants.DEFAULT_LOG_SIZE];

    public static double[] dLog = new double[Constants.DEFAULT_LOG_SIZE];

    public static byte[] bLog = new byte[Constants.DEFAULT_LOG_SIZE];

    public static boolean[] zLog = new boolean[Constants.DEFAULT_LOG_SIZE];

    public static char[] cLog = new char[Constants.DEFAULT_LOG_SIZE];

    public static short[] sLog = new short[Constants.DEFAULT_LOG_SIZE];

    public static Object lock = new Object();

    public static int aLog_fill, iLog_fill, jLog_fill, fLog_fill, dLog_fill, bLog_fill, zLog_fill,
            cLog_fill, sLog_fill;

    public static HashMap<String, Integer> aLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> iLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> jLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> fLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> dLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> bLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> zLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> cLog_replayIndex = new HashMap<String, Integer>();

    public static HashMap<String, Integer> sLog_replayIndex = new HashMap<String, Integer>();

    public static String[] aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] iLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] jLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] fLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] dLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] bLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] zLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] cLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] sLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static void clearLog() {
        aLog = new Serializable[Constants.DEFAULT_LOG_SIZE];
        aLog_fill = 0;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeInt(aLog_fill);
        oos.writeObject(aLog);
        oos.writeInt(iLog_fill);
        oos.writeObject(iLog);
        oos.writeInt(jLog_fill);
        oos.writeObject(jLog);
        oos.writeInt(fLog_fill);
        oos.writeObject(fLog);
        oos.writeInt(dLog_fill);
        oos.writeObject(dLog);
        oos.writeInt(bLog_fill);
        oos.writeObject(bLog);
        oos.writeInt(zLog_fill);
        oos.writeObject(zLog);
        oos.writeInt(cLog_fill);
        oos.writeObject(cLog);
        oos.writeInt(sLog_fill);
        oos.writeObject(sLog);

        oos.writeObject(aLog_owners);
        oos.writeObject(iLog_owners);
        oos.writeObject(jLog_owners);
        oos.writeObject(fLog_owners);
        oos.writeObject(dLog_owners);
        oos.writeObject(bLog_owners);
        oos.writeObject(zLog_owners);
        oos.writeObject(cLog_owners);
        oos.writeObject(sLog_owners);

    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        aLog_fill = ois.readInt();
        aLog = (Object[]) ois.readObject();
        iLog_fill = ois.readInt();
        iLog = (int[]) ois.readObject();
        jLog_fill = ois.readInt();
        jLog = (long[]) ois.readObject();
        fLog_fill = ois.readInt();
        fLog = (float[]) ois.readObject();
        dLog_fill = ois.readInt();
        dLog = (double[]) ois.readObject();
        bLog_fill = ois.readInt();
        bLog = (byte[]) ois.readObject();
        zLog_fill = ois.readInt();
        zLog = (boolean[]) ois.readObject();
        cLog_fill = ois.readInt();
        cLog = (char[]) ois.readObject();
        sLog_fill = ois.readInt();
        sLog = (short[]) ois.readObject();

        aLog_owners = (String[]) ois.readObject();
        iLog_owners = (String[]) ois.readObject();
        jLog_owners = (String[]) ois.readObject();
        fLog_owners = (String[]) ois.readObject();
        dLog_owners = (String[]) ois.readObject();
        bLog_owners = (String[]) ois.readObject();
        zLog_owners = (String[]) ois.readObject();
        cLog_owners = (String[]) ois.readObject();
        sLog_owners = (String[]) ois.readObject();

        ExportedSerializableLog.aLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.iLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.jLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.fLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.dLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.bLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.zLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.cLog_replayIndex = new HashMap<String, Integer>();
        ExportedSerializableLog.sLog_replayIndex = new HashMap<String, Integer>();
    }
}
