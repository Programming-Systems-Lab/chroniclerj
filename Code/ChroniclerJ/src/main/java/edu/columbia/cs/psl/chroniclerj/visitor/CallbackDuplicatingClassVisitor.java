
package edu.columbia.cs.psl.chroniclerj.visitor;

import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.chroniclerj.Instrumenter;


/**
 * If we identify a method as a callback method: Rename it. the renamed one will
 * then not be logged. Create a new method with the original name, and the only
 * instructions are to call the _chronicler_ version
 * 
 * @author jon
 */
public class CallbackDuplicatingClassVisitor extends ClassVisitor {

    private String className;

    private String superName;
    private String[] interfaces;

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public CallbackDuplicatingClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    private HashSet<MethodNode> methodsToGenerateLogging = new HashSet<MethodNode>();

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        if (Instrumenter.methodIsCallback(className, name, desc, superName, interfaces)) {
            methodsToGenerateLogging.add(new MethodNode(access, name, desc, signature, exceptions));
            return super.visitMethod(access, "_chronicler_" + name, desc, signature, exceptions);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        for (MethodNode mn : methodsToGenerateLogging) {
            // mn.name = "BBB"+mn.name;
            MethodVisitor mv = super.visitMethod(mn.access, mn.name, mn.desc, mn.signature,
                    (String[]) mn.exceptions.toArray(new String[0]));
            CloningAdviceAdapter caa = new CloningAdviceAdapter(mv, mn.access,
                    mn.name, mn.desc, className);
            LocalVariablesSorter lvsorter = new LocalVariablesSorter(mn.access, mn.desc, mv);
            CallbackLoggingMethodVisitor clmv = new CallbackLoggingMethodVisitor(mv,
                    mn.access, mn.name, mn.desc, className, lvsorter, caa, superName, interfaces);
            caa.setLocalVariableSorter(lvsorter);

            if ((mn.access & Opcodes.ACC_STATIC) == 0) // not static
                clmv.visitVarInsn(Opcodes.ALOAD, 0);

            // load all of the arguments onto the stack again
            Type[] args = Type.getArgumentTypes(mn.desc);

            int j = 0;
            for (int i = 0; i < args.length; i++) {
                clmv.load(j, args[i]);
                j+= args[i].getSize();
            }
            clmv.visitMethodInsn((mn.access & Opcodes.ACC_STATIC) == 0 ? Opcodes.INVOKESPECIAL
                    : Opcodes.INVOKESTATIC, className, "_chronicler_" + mn.name, mn.desc, false);
            clmv.visitInsn(Type.getReturnType(mn.desc).getOpcode(Opcodes.IRETURN));
            clmv.visitMaxs(0, 0);

            clmv.visitEnd();
        }
        super.visitEnd();

    }
}
