package edu.columbia.cs.psl.chroniclerj;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

import edu.columbia.cs.psl.chroniclerj.replay.NonDeterministicReplayClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.CallbackDuplicatingClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingClassVisitor;

public class PreMain {
	public static boolean isIgnoredClass(String className) {
		return className.startsWith("java") || className.startsWith("com/sun") || className.startsWith("sun/") || className.startsWith("edu/columbia/cs/psl/chroniclerj") || className.startsWith("com/rits/cloning") || className.startsWith("jdk")
				||className.startsWith("com/thoughtworks") || className.startsWith("org/xmlpull")
				|| className.startsWith("org/kxml2");
	}

	public static void premain(String _args, Instrumentation inst) {
		final boolean replay = _args != null && _args.equals("replay");
		if(_args != null)
		{
			String[] args = _args.split(",");
			for(String arg : args)
			{
				String[] d = arg.split("=");
				if(d[0].equals("logFile"))
				{
					ChroniclerJExportRunner.nameOverride = d[1];
				}
				else if(d[0].equals("alwaysExport"))
				{
					ChroniclerJExportRunner.registerShutdownHook();
				}
			}
		}
		ClassFileTransformer transformer = new ClassFileTransformer() {

			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				if (replay) {
					ClassReader cr = new ClassReader(classfileBuffer);
					if (isIgnoredClass(cr.getClassName()))
						return null;
					System.out.println("Inst: " + cr.getClassName());
					ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
					NonDeterministicReplayClassVisitor cv = new NonDeterministicReplayClassVisitor(Opcodes.ASM5, cw);
					cr.accept(cv, ClassReader.EXPAND_FRAMES);
					return cw.toByteArray();
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
						return cw.toByteArray();
					} catch (Throwable t) {
						t.printStackTrace();
						return null;
					}
				}
			}
		};
		inst.addTransformer(transformer);

	}
}
