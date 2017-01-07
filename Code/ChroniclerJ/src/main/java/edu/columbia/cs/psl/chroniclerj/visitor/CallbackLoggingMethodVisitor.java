
package edu.columbia.cs.psl.chroniclerj.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.chroniclerj.CallbackInvocation;
import edu.columbia.cs.psl.chroniclerj.CallbackRegistry;
import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.Log;

public class CallbackLoggingMethodVisitor extends InstructionAdapter implements Opcodes {

    private String className;

    private String methodName;

    private String methodDesc;

    private boolean isCallback;

    private boolean isInit;

    private CloningAdviceAdapter caa;

    public CallbackLoggingMethodVisitor(MethodVisitor mv, int access, String name,
            String desc, String classname, LocalVariablesSorter lvsorter, CloningAdviceAdapter caa, String superName, String[] interfaces) {
        super(Opcodes.ASM5, mv);
        this.className = classname;
        this.methodName = name;
        this.methodDesc = desc;
        this.isInit = name.equals("<init>");
        this.isCallback = Instrumenter.methodIsCallback(classname, name,
                desc, superName, interfaces);
        this.caa = caa;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
     	super.visitMethodInsn(opcode, owner, name, desc, itf);
     	if(isInit && opcode == INVOKESPECIAL && name.equals("<init>"))
     	{
     		onMethodEnter();
     		isInit = false;
     	}
    }
    protected void onMethodEnter() {
        if (this.isInit) {
            super.visitVarInsn(ALOAD, 0);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CallbackRegistry.class),
                    "register", "(Ljava/lang/Object;)V", false);
        }
    }
    private static Type getBoxedType(final Type type) {
        switch (type.getSort()) {
        case Type.BYTE:
            return Type.BYTE_TYPE;
        case Type.BOOLEAN:
            return Type.BOOLEAN_TYPE;
        case Type.SHORT:
            return Type.SHORT_TYPE;
        case Type.CHAR:
            return Type.CHAR_TYPE;
        case Type.INT:
            return Type.INT_TYPE;
        case Type.FLOAT:
            return Type.FLOAT_TYPE;
        case Type.LONG:
            return Type.LONG_TYPE;
        case Type.DOUBLE:
            return Type.DOUBLE_TYPE;
        }
        return type;
    }
    public void box(final Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return;
        }
        if (type == Type.VOID_TYPE) {
            aconst(NULL);
        } else {
            Type boxed = getBoxedType(type);
            anew(boxed);
            if (type.getSize() == 2) {
                // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
                dupX2();
                dupX2();
                pop();
            } else {
                // p -> po -> opo -> oop -> o
                dupX1();
                swap();
            }
            invokespecial(boxed.getInternalName(), "<init>", "("+type.getDescriptor()+")V", false);
        }
    }
    @Override
    public void visitCode() {
        super.visitCode();
        if (this.isCallback) {
            super.visitTypeInsn(NEW, Type.getInternalName(CallbackInvocation.class));
            super.visitInsn(DUP);
            super.visitLdcInsn(className);
            super.visitLdcInsn(methodName);
            super.visitLdcInsn(methodDesc);

            Type[] args = Type.getArgumentTypes(methodDesc);
            iconst(args.length);
            newarray(OBJECT_TYPE);
            int  j = 0;
            for (int i = 0; i < args.length; i++) {
                dup();
                iconst(i);
                load(j,args[i]);

                box(args[i]);
                j += args[i].getSize();
                visitInsn(Opcodes.AASTORE);
            }
            
            super.visitVarInsn(Opcodes.ALOAD, 0);
            super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(CallbackInvocation.class),
                    "<init>",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;)V", false);
            caa.logValueAtTopOfStackToArrayNoDup(Type.getInternalName(Log.class), "aLog",
                    "[Ljava/lang/Object;", Type.getType(Object.class), true, "callback\t"
                            + className + "." + methodName + methodDesc + "\t", false, true);
            // super.visitInsn(POP);
        }
    }

}
