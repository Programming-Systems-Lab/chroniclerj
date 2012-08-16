package edu.columbia.cs.psl.nddetector;

import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

public class NDMutateMV extends MethodVisitor {

	public NDMutateMV(int api, MethodVisitor mv, int access, String name, String desc, String className) {
		super(api, mv);
		this.className = className;
		this.method = name+desc;
	}
	String method;
	String className;
	boolean taint = false;
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC)
		{
			if(!NDMutateDetector.taintedFields.containsKey(owner+name))
				NDMutateDetector.taintedFields.put(owner+name,new HashSet<String>());
			NDMutateDetector.taintedFields.get(owner+name).add(className+"."+method);
		}
	}

}
