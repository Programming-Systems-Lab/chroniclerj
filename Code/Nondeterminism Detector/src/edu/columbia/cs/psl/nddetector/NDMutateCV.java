package edu.columbia.cs.psl.nddetector;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class NDMutateCV extends ClassVisitor {

	public NDMutateCV(int api) {
		super(api);
		// TODO Auto-generated constructor stub
	}
	String className;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if(name.equals("<init>") || !NDMutateDetector.nonDeterministicMethods.contains(className + "." + name + ":" + desc))
			return null;
		return new NDMutateMV(Opcodes.ASM4, null, access, name, desc,className);
	}
}
