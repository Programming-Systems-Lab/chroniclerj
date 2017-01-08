
package edu.columbia.cs.psl.chroniclerj.replay;

import java.util.HashMap;

import edu.columbia.cs.psl.chroniclerj.CallbackInvocation;
import edu.columbia.cs.psl.chroniclerj.ExportedLog;
import edu.columbia.cs.psl.chroniclerj.ExportedSerializableLog;
import edu.columbia.cs.psl.chroniclerj.Log;

public class ReplayUtils {
    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    public static int getNextIndex(HashMap replayIndexMap, String[] threadEntries, int fill) {
        String threadName = Thread.currentThread().getName();
        if (threadName.equals("Finalizer"))
            threadName = threadName + curFinalizer;
        if (!replayIndexMap.containsKey(threadName))
            replayIndexMap.put(threadName, 0);
        int r = (int) replayIndexMap.get(threadName);
        while (r <= fill && threadEntries[r] != null && !threadEntries[r].equals(threadName)) {
            r++;
        }
        checkForDispatch();
        if (threadEntries[r] == null) {
            // System.out.println(Arrays.deepToString(threadEntries));

            // System.out.println(Arrays.deepToString(ExportedSerializableLog.iLog_owners));
            return r;
        }
        if (threadEntries[r] != null && threadEntries[r].equals(threadName)) {
            replayIndexMap.put(threadName, r + 1);
            return r;
        }

        // System.out.println("Skipping " + threadEntries[r] + " vs " +
        // threadName);
        return -1;
    }

    public static HashMap<Integer, CallbackInvocation> dispatchesToRun;

    public static void checkForDispatch() {
        int curClock = ExportedLog.globalReplayIndex;
        // System.out.println("Looking for dispatches at " + curClock);
        if (dispatchesToRun != null && dispatchesToRun.get(curClock) != null) {
            // System.out.println("Invoke " + dispatchesToRun.get(curClock));
            if (dispatchesToRun.get(curClock).invoke()) {
                // System.out.println("Success");
                ExportedLog.globalReplayIndex++;
                checkForDispatch();
            }
        }
        curClock++;
        if (dispatchesToRun != null && dispatchesToRun.get(curClock) != null) {
            // System.out.println("Invoke " + dispatchesToRun.get(curClock));
            if (dispatchesToRun.get(curClock).invoke()) {
                // System.out.println("Success");
                ExportedLog.globalReplayIndex += 2;
                checkForDispatch();
            }
        }
    }

    public static long curFinalizer;

    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    public static int getNextIndexO(HashMap replayIndexMap, String[] threadEntries, int fill,
            Object[] log) {

        String threadName = Thread.currentThread().getName();
        if (threadName.equals("Finalizer"))
            threadName = threadName + curFinalizer;
        if (!replayIndexMap.containsKey(threadName))
            replayIndexMap.put(threadName, 0);
        int r = (int) replayIndexMap.get(threadName);
        while (r <= fill && threadEntries[r] != null && !threadEntries[r].equals(threadName)) {
            r++;
        }

        checkForDispatch();
        if (threadEntries[r] == null)
        {
        	System.err.println("Replay log ended in thread " + threadName);
            System.exit(-1);
        }
        if (threadEntries[r].equals(threadName)) {
            replayIndexMap.put(threadName, r + 1);
            return r;
        }

        return -1;
    }
    
	public static Object getNextObject() {
		Log.logLock.lock();
		try {
			int idx = -1;
			while (idx < 0) {
				idx = ReplayUtils.getNextIndexO(ExportedLog.aLog_replayIndex, ExportedLog.aLog_owners, ExportedLog.aLog_fill, ExportedLog.aLog);
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedLog");
			}
			ExportedLog.globalReplayIndex++;
			return ExportedLog.aLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}

	public static int getNextI() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.iLog_replayIndex, ExportedSerializableLog.iLog_owners, ExportedSerializableLog.iLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.iLog_replayIndex, ExportedSerializableLog.iLog_owners, ExportedSerializableLog.iLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.iLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static float getNextF() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.fLog_replayIndex, ExportedSerializableLog.fLog_owners, ExportedSerializableLog.fLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.fLog_replayIndex, ExportedSerializableLog.fLog_owners, ExportedSerializableLog.fLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.fLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static short getNextS() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.sLog_replayIndex, ExportedSerializableLog.sLog_owners, ExportedSerializableLog.sLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.sLog_replayIndex, ExportedSerializableLog.sLog_owners, ExportedSerializableLog.sLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.sLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static long getNextJ() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.jLog_replayIndex, ExportedSerializableLog.jLog_owners, ExportedSerializableLog.jLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.jLog_replayIndex, ExportedSerializableLog.jLog_owners, ExportedSerializableLog.jLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.jLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static boolean getNextZ() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.zLog_replayIndex, ExportedSerializableLog.zLog_owners, ExportedSerializableLog.zLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.zLog_replayIndex, ExportedSerializableLog.zLog_owners, ExportedSerializableLog.zLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.zLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static byte getNextB() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.bLog_replayIndex, ExportedSerializableLog.bLog_owners, ExportedSerializableLog.bLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.bLog_replayIndex, ExportedSerializableLog.bLog_owners, ExportedSerializableLog.bLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.bLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static char getNextC() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.cLog_replayIndex, ExportedSerializableLog.cLog_owners, ExportedSerializableLog.cLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.cLog_replayIndex, ExportedSerializableLog.cLog_owners, ExportedSerializableLog.cLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.cLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	public static double getNextD() {
		Log.logLock.lock();
		try {
			int idx = ReplayUtils.getNextIndex(ExportedSerializableLog.dLog_replayIndex, ExportedSerializableLog.dLog_owners, ExportedSerializableLog.dLog_fill);
			while (idx < 0) {
				ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedSerializableLog");
				idx = ReplayUtils.getNextIndex(ExportedSerializableLog.dLog_replayIndex, ExportedSerializableLog.dLog_owners, ExportedSerializableLog.dLog_fill);
			}
			ExportedLog.globalReplayIndex++;
			return ExportedSerializableLog.dLog[idx];
		} finally {
			Log.logLock.unlock();
		}
	}
	
	
	public static void copyInto(Object dest, Object src, int len)
	{
		System.arraycopy(src, 0, dest, 0, len);
	}
}
