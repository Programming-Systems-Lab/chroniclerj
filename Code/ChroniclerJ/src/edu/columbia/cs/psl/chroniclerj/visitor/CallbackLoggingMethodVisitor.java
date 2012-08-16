package edu.columbia.cs.psl.chroniclerj.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.chroniclerj.CallbackInvocation;
import edu.columbia.cs.psl.chroniclerj.CallbackRegistry;
import edu.columbia.cs.psl.chroniclerj.Log;

public class CallbackLoggingMethodVisitor extends GeneratorAdapter implements Opcodes {

	private String className;
	private String methodName;
	private String methodDesc;
	private boolean isCallback;
	private boolean isInit;
	private CloningAdviceAdapter caa;
	private boolean initialized;
	public CallbackLoggingMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String classname, LocalVariablesSorter lvsorter, CloningAdviceAdapter caa) {
		super(api, mv, access, name, desc);
		this.className = classname;
		this.methodName = name;
		this.methodDesc = desc;
		this.isInit = name.equals("<init>");
		this.isCallback = NonDeterministicLoggingClassVisitor.methodIsCallback(classname, name, desc);
		this.caa = caa; 
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		super.visitMethodInsn(opcode, owner, name, desc);
		if(opcode == INVOKESPECIAL && !initialized && isInit)
		{
			loadThis();
			visitMethodInsn(INVOKESTATIC, Type.getInternalName(CallbackRegistry.class), "register", "(Ljava/lang/Object;)V");
			initialized = true;
		}
	}
	
	@Override
	public void visitCode() {
		super.visitCode();
		if (this.isCallback) {
			visitTypeInsn(NEW, Type.getInternalName(CallbackInvocation.class));
			visitInsn(DUP);
			visitLdcInsn(className);
			visitLdcInsn(methodName);
			visitLdcInsn(methodDesc);
			loadArgArray();
			loadThis();
			visitMethodInsn(INVOKESPECIAL, Type.getInternalName(CallbackInvocation.class), "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;)V");
			caa.logValueAtTopOfStackToArray(Type.getInternalName(Log.class), "aLog", "[Ljava/lang/Object;", Type.getType(Object.class), true, "callback\t" + className + "."+methodName+methodDesc+"\t", false);
			visitInsn(POP);
		}
	}


}
