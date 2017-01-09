
package edu.columbia.cs.psl.chroniclerj;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.WeakHashMap;

public class CallbackRegistry {
    private static HashMap<String, Object> registry = new HashMap<String, Object>();

    private static WeakHashMap<Object, String> reverseRegistry = new WeakHashMap<Object, String>();

    private static ThreadLocal<Integer> count = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        };
    };

    public static void queueInvocation(String o, CallbackInvocation i) {
        // System.out.println("QUeue" + i);
        if (!queuedInvocations.containsKey(o))
            queuedInvocations.put(o, new LinkedList<CallbackInvocation>());
        queuedInvocations.get(o).add(i);
    }

    private static HashMap<String, LinkedList<CallbackInvocation>> queuedInvocations = new HashMap<String, LinkedList<CallbackInvocation>>();

    public static synchronized void register(Object o) {
        // System.out.println("Register callback");
        String key = Thread.currentThread().getName() + count.get();
        // System.out.println(key + " registered");
        registry.put(key, o);
        reverseRegistry.put(o, key);
        count.set(count.get() + 1);

        if (queuedInvocations.containsKey(key)) {
            for (CallbackInvocation i : queuedInvocations.get(key)) {
                // System.out.println("Invoke " + i);
                i.resetExecuted();
                i.invoke();
            }
            queuedInvocations.remove(key);
        }
    }

    public static String getId(Object o) {
        return reverseRegistry.get(o);
    }

    public static synchronized Object get(String id) {
        // System.out.println("Get callback");
        return registry.get(id);
    }
}
