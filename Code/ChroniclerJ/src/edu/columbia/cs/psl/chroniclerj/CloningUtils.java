
package edu.columbia.cs.psl.chroniclerj;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.security.Permissions;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import sun.nio.ch.DirectBuffer;

import com.rits.cloning.Cloner;

public class CloningUtils {
    public static boolean CATCH_ALL_ERRORS = true;

    private static Cloner cloner = new Cloner();

    public static ReadWriteLock exportLock = new ReentrantReadWriteLock();

    public static HashSet<Class<?>> moreIgnoredImmutables;

    public static HashSet<Class<?>> nullInsteads;

    private static HashSet<String> specificIgnored = new HashSet<>();

    private static boolean inited = false;

    public static void init() {
        if (inited)
            return;
        inited = true;
        ChroniclerJExportRunner.inst.start();
        if (CATCH_ALL_ERRORS) {
            Thread.setDefaultUncaughtExceptionHandler(new ChroniclerJUncaughtExceptionHandler());
        }

    }

    // private static BufferedWriter log;
    static {
        moreIgnoredImmutables = new HashSet<Class<?>>();
        moreIgnoredImmutables.add(ClassLoader.class);
        moreIgnoredImmutables.add(Thread.class);
        moreIgnoredImmutables.add(URI.class);
        moreIgnoredImmutables.add(Inflater.class);
        moreIgnoredImmutables.add(InputStream.class);
        moreIgnoredImmutables.add(OutputStream.class);
        moreIgnoredImmutables.add(Deflater.class);
        moreIgnoredImmutables.add(Class.class);
        moreIgnoredImmutables.add(CallbackInvocation.class);
        moreIgnoredImmutables.add(Method.class);
        moreIgnoredImmutables.add(Logger.class);
        moreIgnoredImmutables.add(URLConnection.class);
        moreIgnoredImmutables.add(MBeanServer.class);
        moreIgnoredImmutables.add(DirectBuffer.class);
        moreIgnoredImmutables.add(Semaphore.class);
        moreIgnoredImmutables.add(Lock.class);
        moreIgnoredImmutables.add(ReadWriteLock.class);
        moreIgnoredImmutables.add(Writer.class);
        moreIgnoredImmutables.add(Properties.class);
        moreIgnoredImmutables.add(Timer.class);
        moreIgnoredImmutables.add(ObjectName.class);
        moreIgnoredImmutables.add(ObjectInstance.class);
        moreIgnoredImmutables.add(Selector.class);
        moreIgnoredImmutables.add(URI.class);
        moreIgnoredImmutables.add(URL.class);
        moreIgnoredImmutables.add(ZipEntry.class);
        moreIgnoredImmutables.add(TimeZone.class);
        moreIgnoredImmutables.add(SelectableChannel.class);
        moreIgnoredImmutables.add(File.class);
        moreIgnoredImmutables.add(Selector.class);

        cloner.setExtraNullInsteadOfClone(moreIgnoredImmutables);
        cloner.setExtraImmutables(moreIgnoredImmutables);

        specificIgnored.add("org.apache.geronimo.security.jaas.LoginModuleControlFlag");
        specificIgnored.add("org.apache.openejb.core.ivm.naming.IvmContext");
        specificIgnored.add("org.apache.geronimo.openejb.GeronimoSecurityService");
        specificIgnored.add("org.apache.geronimo.kernel.config.ConfigurationData");
        specificIgnored.add("sun.net.www.protocol.jar.URLJarFile");
        specificIgnored.add("sun.net.www.protocol.jar.JarURLConnection");
        // cloner.dontClone("org.apache.geronimo.gbean.GBeanInfo");
        // cloner.dontClone("org.apache.geronimo.gbean.AbstractName");
        // cloner.dontClone("org.apache.geronimo.security.jaas.LoginModuleControlFlag");
        // cloner.dontClone("org.apache.geronimo.kernel.config.ConfigurationData");
        // cloner.dontClone("org.apache.geronimo.kernel.repository.Artifact");

        nullInsteads = new HashSet<Class<?>>();
        nullInsteads.add(Permissions.class);

        nullInsteads.add(File.class);
        nullInsteads.add(ZipFile.class);
        nullInsteads.add(ZipEntry.class);
        nullInsteads.add(JarFile.class);
        nullInsteads.add(Socket.class);
        nullInsteads.add(ServerSocket.class);
        nullInsteads.add(Channel.class);
        nullInsteads.add(Closeable.class);
        cloner.setExtraNullInsteadOfClone(nullInsteads);
        init();
        // cloner.setDumpClonedClasses(true);
        // cloner.setDumpCloneStatistics(true);
        // try {
        // File f = new File("cloneLog");
        // if (f.exists())
        // f.delete();
        // log = new BufferedWriter(new FileWriter("cloneLog"));
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
    }

    public static final <T> T clone(T obj, String debug) {
        if (obj != null) {
            // System.out.println(debug);
            // if(specificIgnored.contains(obj.getClass().getName()))
            // return obj;
            // System.out.println(obj.getClass().getName());
            // System.out.println("source>"+obj.getClass().getName()
            // +"..."+Thread.currentThread().getName());
            T ret = cloner.deepClone(obj);
            // T ret = obj;
            return ret;
            // return obj;
        }

        // System.out.println("Done");
        return null;
    }

    public static IdentityHashMap<Object, Object> cloneCache = new IdentityHashMap<Object, Object>();;

}
