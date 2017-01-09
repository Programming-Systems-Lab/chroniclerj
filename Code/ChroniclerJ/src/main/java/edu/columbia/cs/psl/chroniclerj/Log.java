
package edu.columbia.cs.psl.chroniclerj;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.columbia.cs.psl.chroniclerj.replay.ReplayUtils;

public class Log {
    public static Object[] aLog = new Object[Constants.DEFAULT_LOG_SIZE];

    public static String[] aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];

    public static String[] aLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

    
    public static Lock logLock = new ReentrantLock();

    public static int logsize = 0;

    public static int aLog_fill;

    public static long _chronicler_clock = 0;

    public static void clearLog() {
        logsize = 0;
        aLog = new Object[Constants.DEFAULT_LOG_SIZE];
        aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
        aLog_debug = new String[Constants.DEFAULT_LOG_SIZE];

        aLog_fill = 0;

    }
    
	public static void log(Object toLog, String debug) {
		// toLog must be a clone already, let's make it easy...
		Log.logLock.lock();
		try {
			if (Log.aLog_fill >= Log.aLog.length) {
				// Grow
				Object[] newLog = new Object[(int) (Log.aLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(Log.aLog, 0, newLog, 0, Log.aLog.length);
				Log.aLog = newLog;
				String[] newOwners = new String[(int) (Log.aLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(Log.aLog_owners, 0, newOwners, 0, Log.aLog_owners.length);
				Log.aLog_owners = newOwners;
				String[] newDebug = new String[(int) (Log.aLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(Log.aLog_debug, 0, newDebug, 0, Log.aLog_debug.length);
				Log.aLog_debug = newDebug;
			}
			Log.aLog[Log.aLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				Log.aLog_owners[Log.aLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				Log.aLog_owners[Log.aLog_fill] = threadOwner;
			Log.aLog_debug[Log.aLog_fill] = debug;
			Log.aLog_fill++;
			if (Log.aLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}

	public static void log(boolean toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.zLog_fill >= SerializableLog.zLog.length) {
				// Grow
				boolean[] newLog = new boolean[(int) (SerializableLog.zLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.zLog, 0, newLog, 0, SerializableLog.zLog.length);
				String[] newOwners = new String[(int) (SerializableLog.zLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.zLog_owners, 0, newOwners, 0, SerializableLog.zLog.length);
				String[] newDebug = new String[(int) (SerializableLog.zLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.zLog_debug, 0, newDebug, 0, SerializableLog.zLog.length);
				SerializableLog.zLog = newLog;
				SerializableLog.zLog_owners = newOwners;
				SerializableLog.zLog_debug = newDebug;
			}
			SerializableLog.zLog[SerializableLog.zLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.zLog_owners[SerializableLog.zLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.zLog_owners[SerializableLog.zLog_fill] = threadOwner;
			SerializableLog.zLog_debug[SerializableLog.zLog_fill] = debug;
			SerializableLog.zLog_fill++;
			if (SerializableLog.zLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(byte toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.bLog_fill >= SerializableLog.bLog.length) {
				// Grow
				byte[] newLog = new byte[(int) (SerializableLog.bLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.bLog, 0, newLog, 0, SerializableLog.bLog.length);
				String[] newOwners = new String[(int) (SerializableLog.bLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.bLog_owners, 0, newOwners, 0, SerializableLog.bLog.length);
				String[] newDebug = new String[(int) (SerializableLog.bLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.bLog_debug, 0, newDebug, 0, SerializableLog.bLog.length);
				SerializableLog.bLog = newLog;
				SerializableLog.bLog_owners = newOwners;
				SerializableLog.bLog_debug = newDebug;
			}
			SerializableLog.bLog[SerializableLog.bLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.bLog_owners[SerializableLog.bLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.bLog_owners[SerializableLog.bLog_fill] = threadOwner;
			SerializableLog.bLog_debug[SerializableLog.bLog_fill] = debug;
			SerializableLog.bLog_fill++;
			if (SerializableLog.bLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(char toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.cLog_fill >= SerializableLog.cLog.length) {
				// Grow
				char[] newLog = new char[(int) (SerializableLog.cLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.cLog, 0, newLog, 0, SerializableLog.cLog.length);
				String[] newOwners = new String[(int) (SerializableLog.cLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.cLog_owners, 0, newOwners, 0, SerializableLog.cLog.length);
				String[] newDebug = new String[(int) (SerializableLog.cLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.cLog_debug, 0, newDebug, 0, SerializableLog.cLog.length);
				SerializableLog.cLog = newLog;
				SerializableLog.cLog_owners = newOwners;
				SerializableLog.cLog_debug = newDebug;
			}
			SerializableLog.cLog[SerializableLog.cLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.cLog_owners[SerializableLog.cLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.cLog_owners[SerializableLog.cLog_fill] = threadOwner;
			SerializableLog.cLog_debug[SerializableLog.cLog_fill] = debug;
			SerializableLog.cLog_fill++;
			if (SerializableLog.cLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(float toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.fLog_fill >= SerializableLog.fLog.length) {
				// Grow
				float[] newLog = new float[(int) (SerializableLog.fLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.fLog, 0, newLog, 0, SerializableLog.fLog.length);
				String[] newOwners = new String[(int) (SerializableLog.fLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.fLog_owners, 0, newOwners, 0, SerializableLog.fLog.length);
				String[] newDebug = new String[(int) (SerializableLog.fLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.fLog_debug, 0, newDebug, 0, SerializableLog.fLog.length);
				SerializableLog.fLog = newLog;
				SerializableLog.fLog_owners = newOwners;
				SerializableLog.fLog_debug = newDebug;
			}
			SerializableLog.fLog[SerializableLog.fLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.fLog_owners[SerializableLog.fLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.fLog_owners[SerializableLog.fLog_fill] = threadOwner;
			SerializableLog.fLog_debug[SerializableLog.fLog_fill] = debug;
			SerializableLog.fLog_fill++;
			if (SerializableLog.fLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(double toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.dLog_fill >= SerializableLog.dLog.length) {
				// Grow
				double[] newLog = new double[(int) (SerializableLog.dLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.dLog, 0, newLog, 0, SerializableLog.dLog.length);
				String[] newOwners = new String[(int) (SerializableLog.dLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.dLog_owners, 0, newOwners, 0, SerializableLog.dLog.length);
				String[] newDebug = new String[(int) (SerializableLog.dLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.dLog_debug, 0, newDebug, 0, SerializableLog.dLog.length);
				SerializableLog.dLog = newLog;
				SerializableLog.dLog_owners = newOwners;
				SerializableLog.dLog_debug = newDebug;
			}
			SerializableLog.dLog[SerializableLog.dLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.dLog_owners[SerializableLog.dLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.dLog_owners[SerializableLog.dLog_fill] = threadOwner;
			SerializableLog.dLog_debug[SerializableLog.dLog_fill] = debug;
			SerializableLog.dLog_fill++;
			if (SerializableLog.dLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(int toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.iLog_fill >= SerializableLog.iLog.length) {
				// Grow
				int[] newLog = new int[(int) (SerializableLog.iLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.iLog, 0, newLog, 0, SerializableLog.iLog.length);
				String[] newOwners = new String[(int) (SerializableLog.iLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.iLog_owners, 0, newOwners, 0, SerializableLog.iLog.length);
				String[] newDebug = new String[(int) (SerializableLog.iLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.iLog_debug, 0, newDebug, 0, SerializableLog.iLog.length);
				SerializableLog.iLog = newLog;
				SerializableLog.iLog_owners = newOwners;
				SerializableLog.iLog_debug = newDebug;
			}
			SerializableLog.iLog[SerializableLog.iLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.iLog_owners[SerializableLog.iLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.iLog_owners[SerializableLog.iLog_fill] = threadOwner;
			SerializableLog.iLog_debug[SerializableLog.iLog_fill] = debug;
			SerializableLog.iLog_fill++;
			if (SerializableLog.iLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(short toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.sLog_fill >= SerializableLog.sLog.length) {
				// Grow
				short[] newLog = new short[(int) (SerializableLog.sLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.sLog, 0, newLog, 0, SerializableLog.sLog.length);
				String[] newOwners = new String[(int) (SerializableLog.sLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.sLog_owners, 0, newOwners, 0, SerializableLog.sLog.length);
				String[] newDebug = new String[(int) (SerializableLog.sLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.sLog_debug, 0, newDebug, 0, SerializableLog.sLog.length);
				SerializableLog.sLog = newLog;
				SerializableLog.sLog_owners = newOwners;
				SerializableLog.sLog_debug = newDebug;
			}
			SerializableLog.sLog[SerializableLog.sLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.sLog_owners[SerializableLog.sLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.sLog_owners[SerializableLog.sLog_fill] = threadOwner;
			SerializableLog.sLog_debug[SerializableLog.sLog_fill] = debug;
			SerializableLog.sLog_fill++;
			if (SerializableLog.sLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
	public static void log(long toLog, String debug) {
		Log.logLock.lock();
		try {
			if (SerializableLog.jLog_fill >= SerializableLog.jLog.length) {
				// Grow
				long[] newLog = new long[(int) (SerializableLog.jLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.jLog, 0, newLog, 0, SerializableLog.jLog.length);
				String[] newOwners = new String[(int) (SerializableLog.jLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.jLog_owners, 0, newOwners, 0, SerializableLog.jLog.length);
				String[] newDebug = new String[(int) (SerializableLog.jLog.length * Constants.LOG_GROWTH_RATE)];
				System.arraycopy(SerializableLog.jLog_debug, 0, newDebug, 0, SerializableLog.jLog.length);
				SerializableLog.jLog = newLog;
				SerializableLog.jLog_owners = newOwners;
				SerializableLog.jLog_debug = newDebug;
			}
			SerializableLog.jLog[SerializableLog.jLog_fill] = toLog;
			final String threadOwner = Thread.currentThread().getName();
			if (threadOwner.equals("Finalizer"))
				SerializableLog.jLog_owners[SerializableLog.jLog_fill] = threadOwner + ReplayUtils.curFinalizer;
			else
				SerializableLog.jLog_owners[SerializableLog.jLog_fill] = threadOwner;
			SerializableLog.jLog_debug[SerializableLog.jLog_fill] = debug;
			SerializableLog.jLog_fill++;
			if (SerializableLog.jLog_fill >= Constants.MAX_LOG_SIZE) {
				ChroniclerJExportRunner._export();
			}
		} finally {
			Log.logLock.unlock();
		}
	}
}
