
package edu.columbia.cs.psl.chroniclerj;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.chroniclerj.analysis.MutabilityAnalyzer;
import edu.columbia.cs.psl.chroniclerj.struct.AnnotatedMethod;
import edu.columbia.cs.psl.chroniclerj.visitor.CallbackDuplicatingClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingClassVisitor;

public class Instrumenter {
    public static URLClassLoader loader;

    private static Logger logger = Logger.getLogger(Instrumenter.class);

    public static HashMap<String, AnnotatedMethod> annotatedMethods = new HashMap<String, AnnotatedMethod>();

    public static HashMap<String, ClassNode> instrumentedClasses = new HashMap<String, ClassNode>();

    private static MutabilityAnalyzer ma = new MutabilityAnalyzer(annotatedMethods);

    public static HashMap<String, HashSet<MethodCall>> methodCalls = new HashMap<String, HashSet<MethodCall>>();

    private static final int NUM_PASSES = 2;

    private static final int PASS_ANALYZE = 0;

    private static final int PASS_OUTPUT = 1;

    public static final boolean IS_DACAPO = false;

    private static int pass_number = 0;

    public static final boolean FINALIZERS_ENABLED = true;

    public static final String FIELD_LOGICAL_CLOCK = "_chronicler_clock";

    private static File rootOutputDir;

    private static String lastInstrumentedClass;

    public static String getParentType(String type) {
        final ClassNode cn = instrumentedClasses.get(type);
        if (cn == null)
            return null;
        return cn.superName;
    }

    public static AnnotatedMethod getAnnotatedMethod(String owner, String name, String desc) {
        String lookupKey = owner + "." + name + ":" + desc;
        return annotatedMethods.get(lookupKey);
    }

    private static void analyzeClass(InputStream inputStream) {
        try {
            ClassNode analysisResult = ma.analyzeClass(new ClassReader(inputStream));
            inputStream.close();
            if (analysisResult == null) {
                logger.error("Null analysis result on this analysis");
            }
            instrumentedClasses.put(analysisResult.name, analysisResult);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error("Error in analysis", e);
        }
    }

    private static void finishedPass() {
        switch (pass_number) {
            case PASS_ANALYZE:
                ma.doneSupplyingClasses();
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
                is.close();
            }

            ClassWriter cw = new InstrumenterClassWriter(cr, ClassWriter.COMPUTE_MAXS
                    | ClassWriter.COMPUTE_FRAMES, loader);
            NonDeterministicLoggingClassVisitor cv = new NonDeterministicLoggingClassVisitor(
                    Opcodes.ASM4, cw);
            CallbackDuplicatingClassVisitor callbackDuplicator = new CallbackDuplicatingClassVisitor(
                    Opcodes.ASM4, cv);

            cr.accept(callbackDuplicator, ClassReader.EXPAND_FRAMES);
            if (methodCalls.containsKey(cv.getClassName()))
                methodCalls.get(cv.getClassName()).addAll(cv.getLoggedMethodCalls());
            else
                methodCalls.put(cv.getClassName(), cv.getLoggedMethodCalls());
            lastInstrumentedClass = cv.getClassName();
            byte[] out = cw.toByteArray();
            try {
                ClassReader cr2 = new ClassReader(out);
                cr2.accept(new CheckClassAdapter(new ClassWriter(0)), ClassReader.EXPAND_FRAMES);
            } catch (Exception ex) {
                System.err.println(lastInstrumentedClass);
                ex.printStackTrace();
            }
            return out;
        } catch (Exception ex) {
            logger.error("Exception processing class: " + lastInstrumentedClass, ex);
            ex.printStackTrace();
            return null;
        }
    }

    public static void _main(String[] args) {

        String outputFolder = args[1];
        rootOutputDir = new File(outputFolder);
        if (!rootOutputDir.exists())
            rootOutputDir.mkdir();
        String inputFolder = args[0];
        // Setup the class loader
        URL[] urls = new URL[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            File f = new File(args[i]);
            if (!f.exists()) {
                System.err.println("Unable to read path " + args[i]);
                System.exit(-1);
            }
            if (f.isDirectory() && !f.getAbsolutePath().endsWith("/"))
                f = new File(f.getAbsolutePath() + "/");
            try {
                urls[i - 2] = f.getCanonicalFile().toURI().toURL();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        loader = new URLClassLoader(urls, Instrumenter.class.getClassLoader());

        for (pass_number = 0; pass_number < NUM_PASSES; pass_number++) // Do
                                                                       // each
                                                                       // pass.
        {
            File f = new File(inputFolder);
            if (!f.exists()) {
                System.err.println("Unable to read path " + inputFolder);
                System.exit(-1);
            }
            if (f.isDirectory())
                processDirectory(f, rootOutputDir, true);
            else if (inputFolder.endsWith(".jar"))
                processJar(f, rootOutputDir);
            else if (inputFolder.endsWith(".zip"))
                processZip(f, rootOutputDir);
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
        // }

    }

    private static void processClass(String name, InputStream is, File outputDir) {
        switch (pass_number) {
            case PASS_ANALYZE:
                analyzeClass(is);
                break;
            case PASS_OUTPUT:
                try {
                    FileOutputStream fos = new FileOutputStream(outputDir.getPath()
                            + File.separator + name);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    lastInstrumentedClass = outputDir.getPath() + File.separator + name;
                    bos.write(instrumentClass(is));
                    bos.writeTo(fos);
                    fos.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
        }
    }

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
            else if (fi.getName().endsWith(".jar")) {
                if (!thisOutputDir.exists())
                    thisOutputDir.mkdir();
                processJar(fi, thisOutputDir);
            } else if (fi.getName().endsWith(".zip")) {
                processZip(fi, thisOutputDir);
            } else if (pass_number == PASS_OUTPUT) {
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

    // @Deprecated
    // private static void processJar(InputStream inputStream, OutputStream os)
    // {
    // try {
    //
    // File f = new File("/tmp/classfile");
    // if(f.exists())
    // f.delete();
    // FileOutputStream fos = new FileOutputStream(f);
    // byte buf[] = new byte[1024];
    // int len;
    // while((len=inputStream.read(buf)) > 0)
    // {
    // fos.write(buf,0,len);
    // }
    // fos.close();
    // inputStream.close();
    //
    // @SuppressWarnings("resource")
    // JarFile jar = new JarFile(f);
    // JarOutputStream jos = null;
    // if (pass_number == PASS_OUTPUT)
    // jos = new JarOutputStream(os);
    // // jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() +
    // File.separator + f.getName()));
    // Enumeration<JarEntry> entries = jar.entries();
    // while (entries.hasMoreElements()) {
    // JarEntry e = entries.nextElement();
    // switch (pass_number) {
    // case PASS_ANALYZE:
    // if (e.getName().endsWith(".class")) {
    // analyzeClass(jar.getInputStream(e));
    // }
    // break;
    // case PASS_OUTPUT:
    // if (e.getName().endsWith(".class") && !e.getName().startsWith("java") &&
    // !e.getName().startsWith("org/objenesis")
    // && !e.getName().startsWith("com/thoughtworks/xstream/") &&
    // !e.getName().startsWith("com/rits/cloning")
    // && !e.getName().startsWith("com/apple/java/Application")) {
    // {
    // JarEntry outEntry = new JarEntry(e.getName());
    // jos.putNextEntry(outEntry);
    // byte[] clazz = instrumentClass(jar.getInputStream(e));
    // if(clazz == null)
    // {
    // System.out.println("Unable to transform " + e.getName());
    // InputStream is = jar.getInputStream(e);
    // byte[] buffer = new byte[1024];
    // while (true) {
    // int count = is.read(buffer);
    // if (count == -1)
    // break;
    // jos.write(buffer, 0, count);
    // }
    // }
    // else
    // jos.write(clazz);
    // jos.closeEntry();
    // }
    //
    //
    // } else {
    // JarEntry outEntry = new JarEntry(e.getName());
    // if (e.isDirectory()) {
    // jos.putNextEntry(outEntry);
    // jos.closeEntry();
    // } else if (e.getName().startsWith("META-INF") &&
    // (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
    // // don't copy this
    // } else if (e.getName().equals("META-INF/MANIFEST.MF")) {
    // Scanner s = new Scanner(jar.getInputStream(e));
    // jos.putNextEntry(outEntry);
    //
    // String curPair = "";
    // while (s.hasNextLine()) {
    // String line = s.nextLine();
    // if (line.equals("")) {
    // curPair += "\n";
    // if (!curPair.contains("SHA1-Digest:"))
    // jos.write(curPair.getBytes());
    // curPair = "";
    // } else {
    // curPair += line + "\n";
    // }
    // }
    // s.close();
    // jos.write("\n".getBytes());
    // jos.closeEntry();
    // } else {
    // jos.putNextEntry(outEntry);
    // InputStream is = jar.getInputStream(e);
    // byte[] buffer = new byte[1024];
    // while (true) {
    // int count = is.read(buffer);
    // if (count == -1)
    // break;
    // jos.write(buffer, 0, count);
    // }
    // jos.closeEntry();
    // }
    // }
    // }
    //
    // }
    // if(jos != null)
    // {
    // jos.flush();
    // // jos.finish();
    // }
    // } catch (Exception e) {
    // // TODO Auto-generated catch block
    // logger.error("Unable to process jar" , e);
    // System.exit(-1);
    // }
    //
    // }
    private static void processJar(File f, File outputDir) {
        try {

            // @SuppressWarnings("resource")
            JarFile jar = new JarFile(f);
            JarOutputStream jos = null;
            if (pass_number == PASS_OUTPUT)
                // jos = new JarOutputStream(os);
                jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + File.separator
                        + f.getName()));
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                switch (pass_number) {
                    case PASS_ANALYZE:
                        if (e.getName().endsWith(".class")) {
                            analyzeClass(jar.getInputStream(e));
                        }
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
                                    System.out.println("Failed to instrument " + e.getName());
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
            if (jos != null) {
                jos.close();

            }
            jar.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Unable to process jar", e);
            System.exit(-1);
        }

    }

    private static void processZip(File f, File outputDir) {
        try {
            // @SuppressWarnings("resource")
            ZipFile zip = new ZipFile(f);
            ZipOutputStream zos = null;
            if (pass_number == PASS_OUTPUT)
                zos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + File.separator
                        + f.getName()));
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                switch (pass_number) {
                    case PASS_ANALYZE:
                        if (e.getName().endsWith(".class")) {
                            analyzeClass((zip.getInputStream(e)));
                        } else if (e.getName().endsWith(".jar")) {
                            File tmp = new File("/tmp/classfile");
                            if (tmp.exists())
                                tmp.delete();
                            FileOutputStream fos = new FileOutputStream(tmp);
                            byte buf[] = new byte[1024];
                            int len;
                            InputStream is = zip.getInputStream(e);
                            while ((len = is.read(buf)) > 0) {
                                fos.write(buf, 0, len);
                            }
                            is.close();
                            fos.close();

                            processJar(tmp, new File("/tmp"));
                            // processJar(jar.getInputStream(e), jos);
                        }
                        break;
                    case PASS_OUTPUT:
                        if (e.getName().endsWith(".class") && !e.getName().startsWith("java")
                                && !e.getName().startsWith("org/objenesis")
                                && !e.getName().startsWith("com/thoughtworks/xstream/")
                                && !e.getName().startsWith("com/rits/cloning")
                                && !e.getName().startsWith("com/apple/java/Application")) {
                            {
                                ZipEntry outEntry = new ZipEntry(e.getName());
                                zos.putNextEntry(outEntry);
                                byte[] clazz = instrumentClass(zip.getInputStream(e));
                                if (clazz == null) {
                                    InputStream is = zip.getInputStream(e);
                                    byte[] buffer = new byte[1024];
                                    while (true) {
                                        int count = is.read(buffer);
                                        if (count == -1)
                                            break;
                                        zos.write(buffer, 0, count);
                                    }
                                } else
                                    zos.write(clazz);
                                zos.closeEntry();
                            }

                        } else if (e.getName().endsWith(".jar")) {
                            ZipEntry outEntry = new ZipEntry(e.getName());
                            // jos.putNextEntry(outEntry);
                            // try {
                            // processJar(jar.getInputStream(e), jos);
                            // jos.closeEntry();
                            // } catch (FileNotFoundException e1) {
                            // // TODO Auto-generated catch block
                            // e1.printStackTrace();
                            // }

                            File tmp = new File("/tmp/classfile");
                            if (tmp.exists())
                                tmp.delete();
                            FileOutputStream fos = new FileOutputStream(tmp);
                            byte buf[] = new byte[1024];
                            int len;
                            InputStream is = zip.getInputStream(e);
                            while ((len = is.read(buf)) > 0) {
                                fos.write(buf, 0, len);
                            }
                            is.close();
                            fos.close();
                            // System.out.println("Done reading");
                            processJar(tmp, new File("tmp2"));

                            zos.putNextEntry(outEntry);
                            is = new FileInputStream("tmp2/classfile");
                            byte[] buffer = new byte[1024];
                            while (true) {
                                int count = is.read(buffer);
                                if (count == -1)
                                    break;
                                zos.write(buffer, 0, count);
                            }
                            is.close();
                            zos.closeEntry();
                            // jos.closeEntry();
                        } else {
                            ZipEntry outEntry = new ZipEntry(e.getName());
                            if (e.isDirectory()) {
                                zos.putNextEntry(outEntry);
                                zos.closeEntry();
                            } else if (e.getName().startsWith("META-INF")
                                    && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
                                // don't copy this
                            } else if (e.getName().equals("META-INF/MANIFEST.MF")) {
                                Scanner s = new Scanner(zip.getInputStream(e));
                                zos.putNextEntry(outEntry);

                                String curPair = "";
                                while (s.hasNextLine()) {
                                    String line = s.nextLine();
                                    if (line.equals("")) {
                                        curPair += "\n";
                                        if (!curPair.contains("SHA1-Digest:"))
                                            zos.write(curPair.getBytes());
                                        curPair = "";
                                    } else {
                                        curPair += line + "\n";
                                    }
                                }
                                s.close();
                                zos.write("\n".getBytes());
                                zos.closeEntry();
                            } else {
                                zos.putNextEntry(outEntry);
                                InputStream is = zip.getInputStream(e);
                                byte[] buffer = new byte[1024];
                                while (true) {
                                    int count = is.read(buffer);
                                    if (count == -1)
                                        break;
                                    zos.write(buffer, 0, count);
                                }
                                zos.closeEntry();
                            }
                        }
                }

            }
            if (pass_number == PASS_OUTPUT) {
                zos.close();
                zip.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Unable to process zip" + f, e);
            System.exit(-1);
        }

    }
}
