
package edu.columbia.cs.psl.chroniclerj.replay;

import java.util.HashMap;

import edu.columbia.cs.psl.chroniclerj.CallbackInvocation;
import edu.columbia.cs.psl.chroniclerj.ExportedLog;

public class ReplayUtils {
    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    public static int getNextIndex(HashMap replayIndexMap, String[] threadEntries, int fill,
            String logClass) {
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
            replayIndexMap.put(threadName, r);
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
            String logClass, Object[] log) {

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
            System.exit(-1);
        if (threadEntries[r].equals(threadName)) {
            replayIndexMap.put(threadName, r);
            return r;
        }

        return -1;
    }
}
