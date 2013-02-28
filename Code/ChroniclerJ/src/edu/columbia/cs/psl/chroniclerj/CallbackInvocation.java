
package edu.columbia.cs.psl.chroniclerj;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;

public class CallbackInvocation {
    @Override
    public String toString() {
        return "CallbackInvocation [clazz=" + clazz + ", methodName=" + methodName
                + ", methodDesc=" + methodDesc + ", ownerID=" + ownerID + ", executed=" + executed
                + ", clock=" + clock + "]";
    }

    private String clazz;

    private String methodName;

    private String methodDesc;

    private Object[] args;

    private String ownerID;

    private boolean executed;

    private int clock;

    private String threadName;

    public CallbackInvocation(String clazz, String methodName, String methodDesc, Object[] args,
            Object owner) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.args = args;
        this.ownerID = CallbackRegistry.getId(owner);
        this.clock = SerializableLog.aLog_fill + SerializableLog.bLog_fill
                + SerializableLog.cLog_fill + SerializableLog.dLog_fill + SerializableLog.fLog_fill
                + SerializableLog.jLog_fill + SerializableLog.sLog_fill + SerializableLog.zLog_fill
                + Log.aLog_fill;
        this.threadName = Thread.currentThread().getName();
    }

    public String getThreadName() {
        return threadName;
    }

    public int getClock() {
        return clock;
    }

    public void resetExecuted() {
        executed = false;
    }

    public boolean invoke() {
        if (executed)
            return false;
        executed = true;
        try {
            if (CallbackRegistry.get(ownerID) == null) {
                // System.out.println("Queued");
                CallbackRegistry.queueInvocation(ownerID, this);
            } else {
                if (!this.threadName.startsWith("AWT-EventQueue-")) {
                    try {
                        final Object owner = CallbackRegistry.get(ownerID);
                        final Method method = getMethod();
                        EventQueue.invokeAndWait(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    // System.out.println("Dispatching to AWT");
                                    method.invoke(owner, args);
                                    // System.out.println("Executed");
                                } catch (IllegalAccessException | IllegalArgumentException
                                        | InvocationTargetException e) {
                                    // TODO Auto-generated catch block
                                    e.getCause().printStackTrace();
                                    System.exit(-1);
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    try {
                        getMethod().invoke(CallbackRegistry.get(ownerID), args);
                    } catch (IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        // TODO Auto-generated catch block
                        e.getCause().printStackTrace();
                        System.exit(-1);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    public Method getMethod() {
        Type[] argDesc = Type.getMethodType(methodDesc).getArgumentTypes();
        return getMethod(methodName, argDesc, CallbackRegistry.get(ownerID).getClass());
    }

    protected Method getMethod(String methodName, Type[] types, Class<?> clazz) {
        try {
            for (Method m : clazz.getDeclaredMethods()) {
                boolean ok = true;
                if (m.getName().equals(methodName)) {
                    Class<?>[] mArgs = m.getParameterTypes();
                    if (mArgs.length != types.length)
                        break;
                    for (int i = 0; i < mArgs.length; i++)
                        if (!mArgs[i].getName().equals(types[i].getClassName()))
                            ok = false;
                    if (ok) {
                        if (!m.isAccessible())
                            m.setAccessible(true);
                        return m;
                    }
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        if (clazz.getSuperclass() != null)
            return getMethod(methodName, types, clazz.getSuperclass());
        return null;
    }
}
