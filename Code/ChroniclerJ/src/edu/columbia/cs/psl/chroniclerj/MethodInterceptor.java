
package edu.columbia.cs.psl.chroniclerj;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.chroniclerj.replay.ReplayRunner;
import edu.columbia.cs.psl.chroniclerj.replay.ReplayUtils;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingMethodVisitor;

public class MethodInterceptor {
    public static Object invoke(Method method, Object obj, Object... args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (NonDeterministicLoggingMethodVisitor.isND(
                method.getDeclaringClass().getName().replace(".", "/"), method.getName(),
                Type.getMethodDescriptor(method))) {
            Object ret = method.invoke(obj, args);
            if (args != null)
                for (Object arg : args) {
                    if (arg != null && arg.getClass().isArray())
                        log(arg);
                }
            log(ret);
            return ret;
        } else
            return method.invoke(obj, args);
    }

    public static Object invokeReplay(Method method, Object obj, Object... args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (NonDeterministicLoggingMethodVisitor.isND(
                method.getDeclaringClass().getName().replace(".", "/"), method.getName(),
                Type.getMethodDescriptor(method))) {
            if (args != null)
                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null && args[i].getClass().isArray())
                        args[i] = replay();
                }
            Object ret = replay();
            return ret;
        } else
            return method.invoke(obj, args);
    }

    private static Object replay() {
        Log.logLock.lock();

        int i = ReplayUtils.getNextIndexO(ExportedLog.aLog_replayIndex, ExportedLog.aLog_owners,
                ExportedLog.aLog_fill, "edu/columbia/cs/psl/chroniclerj/ExportedLog",
                ExportedLog.aLog);
        if (i < 0) {
            ReplayRunner.loadNextLog("edu/columbia/cs/psl/chroniclerj/ExportedLog");
            return replay();
        }
        Object ret = ExportedLog.aLog[i];

        ReplayUtils.getNextIndexO(ExportedLog.aLog_replayIndex, ExportedLog.aLog_owners,
                ExportedLog.aLog_fill, "edu/columbia/cs/psl/chroniclerj/ExportedLog",
                ExportedLog.aLog);
        ExportedLog.aLog_replayIndex.put(Thread.currentThread().getName(), i + 1);
        ExportedLog.globalReplayIndex++;
        Log.logLock.unlock();
        return ret;
    }

    private static void log(Object obj) {
        Log.logLock.lock();

        if (Log.aLog_fill == Log.aLog.length) {
            Object[] temp = new Object[(int) (Log.aLog.length * 2.5)];
            System.arraycopy(Log.aLog, 0, temp, 0, Log.aLog.length);
            Log.aLog = temp;

            String[] tempOwners = new String[(int) (Log.aLog.length * 2.5)];
            System.arraycopy(Log.aLog_owners, 0, tempOwners, 0, Log.aLog_owners.length);
            Log.aLog_owners = tempOwners;
        }
        Log.aLog[Log.aLog_fill] = CloningUtils.clone(obj, "reflection");
        Log.aLog_owners[Log.aLog_fill] = Thread.currentThread().getName();
        Log.aLog_fill++;
        Log.logsize++;
        Log.logLock.unlock();
    }
}
