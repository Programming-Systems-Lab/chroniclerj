
package edu.columbia.cs.psl.chroniclerj.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;

public class MainLoggingMethodVisitor extends InstructionAdapter {

    private String className;

    protected MainLoggingMethodVisitor(MethodVisitor mv, int access, String name,
            String desc, String className) {
        super(Opcodes.ASM5, mv);
        this.className = className;
    }

    @Override
    public void visitCode() {
    	super.visitCode();
        visitLdcInsn(this.className);
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ChroniclerJExportRunner.class), "logMain", "(Ljava/lang/String;[Ljava/lang/String;)V", false);
    }
}
