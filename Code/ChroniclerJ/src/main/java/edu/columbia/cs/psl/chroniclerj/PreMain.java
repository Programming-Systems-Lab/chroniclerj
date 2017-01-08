package edu.columbia.cs.psl.chroniclerj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import edu.columbia.cs.psl.chroniclerj.replay.NonDeterministicReplayClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.CallbackDuplicatingClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingClassVisitor;

public class PreMain {
	static boolean replay;

	static class ChroniclerTransformer implements ClassFileTransformer {
		@Override
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
			// Instrumenter.loader = loader;
			if (replay) {
				try {
					ClassReader cr = new ClassReader(classfileBuffer);
					if (isIgnoredClass(cr.getClassName()))
						return null;
					System.out.println("Inst: " + cr.getClassName());
					ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
					NonDeterministicReplayClassVisitor cv = new NonDeterministicReplayClassVisitor(Opcodes.ASM5, cw);
					cr.accept(cv, ClassReader.EXPAND_FRAMES);
					if (DEBUG) {
						File f = new File("debug-replay/" + className + ".class");
						f.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(cw.toByteArray());
						fos.close();
					}
					return cw.toByteArray();
				} catch (Throwable t) {
					t.printStackTrace();
					return null;
				}
			} else {
				try {
					ClassReader cr = new ClassReader(classfileBuffer);
					if (isIgnoredClass(cr.getClassName()))
						return null;
					System.out.println("Inst: " + cr.getClassName());
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					NonDeterministicLoggingClassVisitor cv = new NonDeterministicLoggingClassVisitor(new SerialVersionUIDAdder(cw));
					CallbackDuplicatingClassVisitor callbackDuplicator = new CallbackDuplicatingClassVisitor(cv);

					cr.accept(callbackDuplicator, ClassReader.EXPAND_FRAMES);
					if (DEBUG) {
						File f = new File("debug-record/" + className + ".class");
						f.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(cw.toByteArray());
						fos.close();
					}
					cr = new ClassReader(cw.toByteArray());
					CheckClassAdapter ca = new CheckClassAdapter(new ClassWriter(0));
					cr.accept(ca, 0);
					return cw.toByteArray();
				} catch (Throwable t) {
					t.printStackTrace();
					if (DEBUG) {
						TraceClassVisitor tcv = null;
						PrintWriter fw = null;
						try {
							File f = new File("z.class");
							f.getParentFile().mkdirs();
							f.delete();
							FileOutputStream fos = new FileOutputStream(f);
							fos.write(classfileBuffer);
							fos.close();
							fw = new PrintWriter("lastClass.txt");
							ClassReader cr = new ClassReader(classfileBuffer);

							tcv = new TraceClassVisitor(fw);
							NonDeterministicLoggingClassVisitor cv = new NonDeterministicLoggingClassVisitor(new SerialVersionUIDAdder(tcv));
							CallbackDuplicatingClassVisitor callbackDuplicator = new CallbackDuplicatingClassVisitor(cv);

							cr.accept(callbackDuplicator, ClassReader.EXPAND_FRAMES);

						} catch (Throwable t2) {
							t2.printStackTrace();
						} finally {
							tcv.visitEnd();
							fw.close();
						}
					}
					return null;
				}
			}
		}
	}

	public static boolean isIgnoredClass(String className) {
		return className.startsWith("java") || className.startsWith("com/sun") || className.startsWith("sun/") || className.startsWith("edu/columbia/cs/psl/chroniclerj") || className.startsWith("com/rits/cloning") || className.startsWith("jdk")
				|| className.startsWith("com/thoughtworks") || className.startsWith("org/xmlpull") || className.startsWith("org/kxml2");
	}

	static boolean DEBUG = false;

	public static void premain(String _args, Instrumentation inst) {
		replay = _args != null && _args.equals("replay");
		if (_args != null) {
			String[] args = _args.split(",");
			for (String arg : args) {
				String[] d = arg.split("=");
				if (d[0].equals("logFile")) {
					ChroniclerJExportRunner.nameOverride = d[1];
				} else if (d[0].equals("alwaysExport")) {
					ChroniclerJExportRunner.registerShutdownHook();
				} else if (d[0].equals("debug"))
					DEBUG = true;
			}
		}
		ClassFileTransformer transformer = new ChroniclerTransformer();
		inst.addTransformer(transformer);

	}

	public static void main(String[] args) throws Throwable {
		DEBUG = true;
		FileInputStream fis = new FileInputStream("z.class");
		byte[] b = new byte[1024 * 1024 * 2];
		int l = fis.read(b);
		fis.close();
		byte[] a = new byte[l];
		System.arraycopy(b, 0, a, 0, l);
		new ChroniclerTransformer().transform(null, null, null, null, a);
	}
}
