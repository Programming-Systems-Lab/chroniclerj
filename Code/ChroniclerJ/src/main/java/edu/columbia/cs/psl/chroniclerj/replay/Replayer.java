
package edu.columbia.cs.psl.chroniclerj.replay;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.tree.ClassNode;

import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.InstrumenterClassWriter;
import edu.columbia.cs.psl.chroniclerj.MethodCall;
import edu.columbia.cs.psl.chroniclerj.analysis.MutabilityAnalyzer;
import edu.columbia.cs.psl.chroniclerj.struct.AnnotatedMethod;

public class Replayer {

    private static Logger logger = Logger.getLogger(Replayer.class);

    private static final int NUM_PASSES = 2;

    private static final int PASS_ANALYZE = 0;

    private static final int PASS_OUTPUT = 1;

    private static int pass_number = 0;

    private static File rootOutputDir;

    private static String lastInstrumentedClass;

    public static AnnotatedMethod getAnnotatedMethod(String owner, String name, String desc) {
        String lookupKey = owner + "." + name + ":" + desc;
        return Instrumenter.annotatedMethods.get(lookupKey);
    }

    private static void finishedPass() {
        switch (pass_number) {
            case PASS_ANALYZE:
                break;
            case PASS_OUTPUT:
                break;
        }
    }

    private static byte[] instrumentClass(InputStream is) {
        try {
            ClassReader cr = new ClassReader(is);
            {
                ClassWriter cw = new ClassWriter(cr, 0);
                SerialVersionUIDAdder uidAdder = new SerialVersionUIDAdder(cw);
                cr.accept(uidAdder, 0);
                byte[] b = cw.toByteArray();
                cr = new ClassReader(b);
            }
            ClassWriter cw = new InstrumenterClassWriter(cr, ClassWriter.COMPUTE_MAXS
                    | ClassWriter.COMPUTE_FRAMES, Instrumenter.loader);
            NonDeterministicReplayClassVisitor cv = new NonDeterministicReplayClassVisitor(
                    Opcodes.ASM4, cw);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            Instrumenter.methodCalls.put(cv.getClassName(), cv.getLoggedMethodCalls());
            lastInstrumentedClass = cv.getClassName();
            byte[] out = cw.toByteArray();
            try {
                // ClassReader cr2 = new ClassReader(out);
                // cr2.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
            } catch (Exception ex) {
                System.err.println(lastInstrumentedClass);
                ex.printStackTrace();
            }

            return out;
        } catch (Exception ex) {
            logger.error("Exception processing class:", ex);
            return null;
        }
    }

    public static void _main(String[] args) {

        String outputFolder = args[1];
        rootOutputDir = new File(outputFolder);
        if (!rootOutputDir.exists())
            rootOutputDir.mkdir();
        String inputFolder = args[0];

        pass_number = PASS_OUTPUT;
        File f = new File(inputFolder);
        if (!f.exists()) {
            System.err.println("Unable to read path " + inputFolder);
            System.exit(-1);
        }
        if (f.isDirectory())
            processDirectory(f, rootOutputDir, true);
        else if (inputFolder.endsWith(".jar"))
            processJar(f, rootOutputDir);
        else if (inputFolder.endsWith(".class"))
            try {
                processClass(f.getName(), new FileInputStream(f), rootOutputDir);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        else {
            System.err.println("Unknown type for path " + inputFolder);
            System.exit(-1);
        }
        finishedPass();

    }

    private static void processClass(String name, InputStream is, File outputDir) {
        ByteArrayOutputStream bos = null;
        FileOutputStream fos = null;

        switch (pass_number) {
            case PASS_ANALYZE:
                break;
            case PASS_OUTPUT:
                try {
                    File f = new File(outputDir.getPath() + File.separator + name);
                    if (f.exists())
                        f.delete();
                    fos = new FileOutputStream(f);
                    bos = new ByteArrayOutputStream();
                    bos.write(instrumentClass(is));

                    // if (name.contains("Reader")) {
                    // ReplayRunner.run(bos.toByteArray(), "ReaderUser");
                    // }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                } finally {
                    try {
                        bos.writeTo(fos);
                        fos.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
        }
    }

    /*
     * private static byte[] generateReplayClass() { return
     * generateReplayClass(lastInstrumentedClass); }
     */

    private static void processDirectory(File f, File parentOutputDir, boolean isFirstLevel) {
        File thisOutputDir;
        if (isFirstLevel) {
            thisOutputDir = parentOutputDir;
        } else {
            thisOutputDir = new File(parentOutputDir.getAbsolutePath() + File.separator
                    + f.getName());
            if (pass_number == PASS_OUTPUT)
                thisOutputDir.mkdir();
        }
        for (File fi : f.listFiles()) {
            if (fi.isDirectory())
                processDirectory(fi, thisOutputDir, false);
            else if (fi.getName().endsWith(".class"))
                try {
                    processClass(fi.getName(), new FileInputStream(fi), thisOutputDir);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            else if (fi.getName().endsWith(".jar"))
                processJar(fi, thisOutputDir);
            else if (pass_number == PASS_OUTPUT) {
                File dest = new File(thisOutputDir.getPath() + File.separator + fi.getName());
                FileChannel source = null;
                FileChannel destination = null;

                try {
                    source = new FileInputStream(fi).getChannel();
                    destination = new FileOutputStream(dest).getChannel();
                    destination.transferFrom(source, 0, source.size());
                } catch (Exception ex) {
                    logger.error("Unable to copy file " + fi, ex);
                    System.exit(-1);
                } finally {
                    if (source != null) {
                        try {
                            source.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (destination != null) {
                        try {
                            destination.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }

            }
        }

    }

    private static void processJar(File f, File outputDir) {
        try {
            @SuppressWarnings("resource")
            JarFile jar = new JarFile(f);
            JarOutputStream jos = null;
            if (pass_number == PASS_OUTPUT)
                jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + File.separator
                        + f.getName()));
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                switch (pass_number) {
                    case PASS_ANALYZE:
                        break;
                    case PASS_OUTPUT:
                        if (e.getName().endsWith(".class") && !e.getName().startsWith("java")
                                && !e.getName().startsWith("org/objenesis")
                                && !e.getName().startsWith("com/thoughtworks/xstream/")
                                && !e.getName().startsWith("com/rits/cloning")
                                && !e.getName().startsWith("com/apple/java/Application")
                                && !e.getName().startsWith("net/sf/cglib/")) {
                            {
                                JarEntry outEntry = new JarEntry(e.getName());
                                jos.putNextEntry(outEntry);
                                byte[] clazz = instrumentClass(jar.getInputStream(e));
                                if (clazz == null) {
                                    InputStream is = jar.getInputStream(e);
                                    byte[] buffer = new byte[1024];
                                    while (true) {
                                        int count = is.read(buffer);
                                        if (count == -1)
                                            break;
                                        jos.write(buffer, 0, count);
                                    }
                                } else
                                    jos.write(clazz);
                                jos.closeEntry();
                            }
                            {
                                /*
                                 * JarEntry outEntry = new
                                 * JarEntry(e.getName().replace(".class",
                                 * Constants.LOG_CLASS_SUFFIX +".class"));
                                 * jos.putNextEntry(outEntry); byte[] clazz =
                                 * generateReplayClass(); jos.write(clazz);
                                 * jos.closeEntry();
                                 */
                            }

                        } else {
                            JarEntry outEntry = new JarEntry(e.getName());
                            if (e.isDirectory()) {
                                jos.putNextEntry(outEntry);
                                jos.closeEntry();
                            } else if (e.getName().startsWith("META-INF")
                                    && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
                                // don't copy this
                            } else if (e.getName().equals("META-INF/MANIFEST.MF")) {
                                Scanner s = new Scanner(jar.getInputStream(e));
                                jos.putNextEntry(outEntry);

                                String curPair = "";
                                while (s.hasNextLine()) {
                                    String line = s.nextLine();
                                    if (line.equals("")) {
                                        curPair += "\n";
                                        if (!curPair.contains("SHA1-Digest:"))
                                            jos.write(curPair.getBytes());
                                        curPair = "";
                                    } else {
                                        curPair += line + "\n";
                                    }
                                }
                                s.close();
                                jos.write("\n".getBytes());
                                jos.closeEntry();
                            } else {
                                jos.putNextEntry(outEntry);
                                InputStream is = jar.getInputStream(e);
                                byte[] buffer = new byte[1024];
                                while (true) {
                                    int count = is.read(buffer);
                                    if (count == -1)
                                        break;
                                    jos.write(buffer, 0, count);
                                }
                                jos.closeEntry();
                            }
                        }
                }

            }
            if (pass_number == PASS_OUTPUT) {
                jos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Unable to process jar" + f, e);
            System.exit(-1);
        }

    }
}
