
package edu.columbia.cs.psl.chroniclerj.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectionInterceptingMethodVisitor extends MethodVisitor {

    public ReflectionInterceptingMethodVisitor(int api, MethodVisitor mv) {
        super(api, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
            opcode = Opcodes.INVOKESTATIC;
            owner = "edu/columbia/cs/psl/chroniclerj/MethodInterceptor";
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "edu/columbia/cs/psl/chroniclerj/MethodInterceptor", "invoke",
                    "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        } else
            super.visitMethodInsn(opcode, owner, name, desc, itfc);
    }
}
