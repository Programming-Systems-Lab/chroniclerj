
package edu.columbia.cs.psl.chroniclerj.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.chroniclerj.Instrumenter;

public class FinalizerLoggingMethodVisitor extends MethodVisitor {

    private boolean isFinalize;

    private String className;

    public FinalizerLoggingMethodVisitor(int api, MethodVisitor mv, String name, String desc,
            String className) {
        super(api, mv);
        this.isFinalize = (name.equals("finalize")) && desc.equals("()V");
        this.className = className;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (this.isFinalize) {
            // Log to the log our finalizer #
            visitVarInsn(Opcodes.ALOAD, 0);
            visitFieldInsn(Opcodes.GETFIELD, this.className, Instrumenter.FIELD_LOGICAL_CLOCK, "J");
            visitFieldInsn(Opcodes.PUTSTATIC, "edu/columbia/cs/psl/chroniclerj/replay/ReplayUtils",
                    "curFinalizer", "J");
        }
    }
}
