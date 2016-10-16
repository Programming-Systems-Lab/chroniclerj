
package edu.columbia.cs.psl.chroniclerj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.thoughtworks.xstream.XStream;

import edu.columbia.cs.psl.chroniclerj.xstream.StaticReflectionProvider;
import java.util.jar.Manifest;

public class ChroniclerJExportRunner extends Thread {

    private static String mainClass = "";

    private static String[] mainArgs = new String[0];

    private static ArrayList<String> serializableLogs = new ArrayList<>();

    private static ArrayList<String> otherLogs = new ArrayList<>();

    public static void logMain(String main, String[] args) {
        CloningUtils.init();
        mainClass = main;
        mainArgs = new String[args.length];
        System.arraycopy(args, 0, mainArgs, 0, args.length);
    }

    public static void genTestCase()
    {
    	genTestCase("chroniclerj-crash-" + System.currentTimeMillis() + ".test");
    }
    public static void genTestCase(String name) {
        export();
        exportSerializable();
        try {

            File logFile = new File(name);

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,
                    "edu.columbia.cs.psl.wallace.replay.ReplayRunner");

            JarOutputStream zos = new JarOutputStream(new FileOutputStream(logFile));

            JarEntry mainEntry = new JarEntry("main-info");
            zos.putNextEntry(mainEntry);
            zos.write(mainClass.getBytes());
            zos.write("\n".getBytes());
            zos.write(("" + mainArgs.length + "\n").getBytes());
            for (String s : mainArgs) {
                zos.write((s + "\n").getBytes());
            }
            for (String s : serializableLogs) {
                zos.write((s + "\n").getBytes());
            }
            for (String s : otherLogs) {
                zos.write((s + "\n").getBytes());
            }
            zos.closeEntry();
            for (String s : serializableLogs) {
                JarEntry e = new JarEntry(s);
                zos.putNextEntry(e);
                InputStream is = new FileInputStream(s);
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = is.read(buffer);
                    if (count == -1)
                        break;
                    zos.write(buffer, 0, count);
                }
                is.close();
                zos.closeEntry();
                File f = new File(s);
                f.delete();
            }
            for (String s : otherLogs) {
                JarEntry e = new JarEntry(s);
                zos.putNextEntry(e);
                InputStream is = new FileInputStream(s);
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = is.read(buffer);
                    if (count == -1)
                        break;
                    zos.write(buffer, 0, count);
                }
                is.close();
                zos.closeEntry();
                File f = new File(s);
                f.delete();
            }
            zos.flush();
            zos.close();
            System.out.println("Chroniclerj exported a test case");
            serializableLogs.clear();
            otherLogs.clear();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (1 == 1) {
            try {
                Thread.sleep(10000); // 10 seconds
                if (shouldExport == 1)
                    export();
                if (shouldExportSerializable == 1)
                    exportSerializable();
                if (shouldExport == 1)
                    export();

            } catch (InterruptedException e) {
                if (shouldExport == 1)
                    export();
                if (shouldExportSerializable == 1)
                    exportSerializable();
                if (shouldExport == 1)
                    export();
            }
        }
    }

    static ChroniclerJExportRunner inst = new ChroniclerJExportRunner();

    public ChroniclerJExportRunner() {
        setDaemon(true);
        setPriority(Thread.MAX_PRIORITY);
    }

    private static ExportedLog log = new ExportedLog();

    public static void export() {
        shouldExport = 0;
        try {

            XStream xstream = new XStream(new StaticReflectionProvider());
            String xml = "";
            Log.logLock.lock();
            ExportedLog.aLog = Log.aLog;
            ExportedLog.aLog_owners = Log.aLog_owners;
            ExportedLog.aLog_fill = Log.aLog_fill;
            ExportedLog.aLog_debug = Log.aLog_debug;
            Log.logsize = 0;
            Log.aLog = new Object[Constants.DEFAULT_LOG_SIZE];
            Log.aLog_fill = 0;
            Log.logLock.unlock();

            try {
                xml = xstream.toXML(log);
            } catch (Exception ex) {
                System.err.println("NPE" + ex.getMessage());
            }

            ExportedLog.clearLog();
            String name = "chroniclerj_" + System.currentTimeMillis() + ".log";
            otherLogs.add(name);
            File output = new File(name);
            FileWriter fw = new FileWriter(output);
            fw.write(xml);
            fw.close();
        } catch (Exception exi) {

        }
        shouldExport = -1;
    }

    private static ExportedSerializableLog logS = new ExportedSerializableLog();

    public static void exportSerializable() {
        shouldExportSerializable = 0;
        try {

            Log.logLock.lock();
            {
                ExportedSerializableLog.aLog = SerializableLog.aLog;
                ExportedSerializableLog.aLog_fill = SerializableLog.aLog_fill;
                ExportedSerializableLog.bLog = SerializableLog.bLog;
                ExportedSerializableLog.cLog = SerializableLog.cLog;
                ExportedSerializableLog.dLog = SerializableLog.dLog;
                ExportedSerializableLog.iLog = SerializableLog.iLog;
                ExportedSerializableLog.fLog = SerializableLog.fLog;
                ExportedSerializableLog.jLog = SerializableLog.jLog;
                ExportedSerializableLog.zLog = SerializableLog.zLog;
                ExportedSerializableLog.sLog = SerializableLog.sLog;

                ExportedSerializableLog.bLog_fill = SerializableLog.bLog_fill;
                ExportedSerializableLog.cLog_fill = SerializableLog.cLog_fill;
                ExportedSerializableLog.dLog_fill = SerializableLog.dLog_fill;
                ExportedSerializableLog.iLog_fill = SerializableLog.iLog_fill;
                ExportedSerializableLog.fLog_fill = SerializableLog.fLog_fill;
                ExportedSerializableLog.jLog_fill = SerializableLog.jLog_fill;
                ExportedSerializableLog.zLog_fill = SerializableLog.zLog_fill;
                ExportedSerializableLog.sLog_fill = SerializableLog.sLog_fill;

                ExportedSerializableLog.aLog_owners = SerializableLog.aLog_owners;
                ExportedSerializableLog.iLog_owners = SerializableLog.iLog_owners;
                ExportedSerializableLog.jLog_owners = SerializableLog.jLog_owners;
                ExportedSerializableLog.fLog_owners = SerializableLog.fLog_owners;
                ExportedSerializableLog.dLog_owners = SerializableLog.dLog_owners;
                ExportedSerializableLog.bLog_owners = SerializableLog.bLog_owners;
                ExportedSerializableLog.zLog_owners = SerializableLog.zLog_owners;
                ExportedSerializableLog.cLog_owners = SerializableLog.cLog_owners;
                ExportedSerializableLog.sLog_owners = SerializableLog.sLog_owners;

                ExportedSerializableLog.aLog_debug = SerializableLog.aLog_debug;
                ExportedSerializableLog.iLog_debug = SerializableLog.iLog_debug;
                ExportedSerializableLog.jLog_debug = SerializableLog.jLog_debug;
                ExportedSerializableLog.fLog_debug = SerializableLog.fLog_debug;
                ExportedSerializableLog.dLog_debug = SerializableLog.dLog_debug;
                ExportedSerializableLog.bLog_debug = SerializableLog.bLog_debug;
                ExportedSerializableLog.zLog_debug = SerializableLog.zLog_debug;
                ExportedSerializableLog.cLog_debug = SerializableLog.cLog_debug;
                ExportedSerializableLog.sLog_debug = SerializableLog.sLog_debug;
                
                SerializableLog.aLog = new Object[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.iLog = new int[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.jLog = new long[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.fLog = new float[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.dLog = new double[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.bLog = new byte[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.zLog = new boolean[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.cLog = new char[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.sLog = new short[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.aLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.iLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.jLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.fLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.dLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.bLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.zLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.cLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.sLog_owners = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.aLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.iLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.jLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.fLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.dLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.bLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.zLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.cLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.sLog_debug = new String[Constants.DEFAULT_LOG_SIZE];
                SerializableLog.logsize = 0;
                SerializableLog.iLog_fill = 0;
                SerializableLog.jLog_fill = 0;
                SerializableLog.fLog_fill = 0;
                SerializableLog.dLog_fill = 0;
                SerializableLog.bLog_fill = 0;
                SerializableLog.zLog_fill = 0;
                SerializableLog.cLog_fill = 0;
                SerializableLog.sLog_fill = 0;
                SerializableLog.aLog_fill = 0;
            }
            Log.logLock.unlock();
            String name = "chroniclerj_serializable_" + System.currentTimeMillis() + ".log";
            File output = new File(name);
            serializableLogs.add(name);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(output));
            oos.writeObject(logS);
            oos.flush();
            oos.close();
            ExportedLog.clearLog();
        } catch (Exception exi) {

        }
        shouldExportSerializable = -1;
    }

    private static int shouldExport = -1;

    private static int shouldExportSerializable = -1;

    public static void _exportSerializable() {
        if (shouldExportSerializable == -1) {
            Thread.yield();
            shouldExportSerializable = 1;
            inst.interrupt();
        }
    }

    public static void _export() {
        if (shouldExport == -1) {
            Thread.yield();
            shouldExport = 1;
            inst.interrupt();
        }
    }

}
