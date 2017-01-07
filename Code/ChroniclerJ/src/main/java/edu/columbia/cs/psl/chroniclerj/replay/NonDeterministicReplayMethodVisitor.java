
package edu.columbia.cs.psl.chroniclerj.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.MethodInsnNode;

import edu.columbia.cs.psl.chroniclerj.CallbackRegistry;
import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;
import edu.columbia.cs.psl.chroniclerj.ExportedLog;
import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.Log;
import edu.columbia.cs.psl.chroniclerj.MethodCall;
import edu.columbia.cs.psl.chroniclerj.struct.AnnotatedMethod;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingMethodVisitor;

public class NonDeterministicReplayMethodVisitor extends InstructionAdapter implements Opcodes {
    private static Logger logger = Logger.getLogger(NonDeterministicReplayMethodVisitor.class);

    private String name;

    private String desc;

    private String classDesc;

    private boolean isStatic;

    private boolean constructor;

    private boolean superInitialized;

    private boolean isCallbackInit;

    @Override
    public void visitCode() {
        super.visitCode();
        if (constructor) {
//            if (am != null && am.isCallsNDMethods()) {
                super.visitVarInsn(ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETSTATIC, "edu/columbia/cs/psl/chroniclerj/Log",
                        Instrumenter.FIELD_LOGICAL_CLOCK, "J");
                super.visitInsn(DUP2_X1);
                super.visitFieldInsn(Opcodes.PUTFIELD, this.classDesc,
                        Instrumenter.FIELD_LOGICAL_CLOCK, "J");
                super.visitInsn(LCONST_1);
                super.visitInsn(LADD);
                super.visitFieldInsn(Opcodes.PUTSTATIC, "edu/columbia/cs/psl/chroniclerj/Log",
                        Instrumenter.FIELD_LOGICAL_CLOCK, "J");
//            }
        }
        if (!constructor)
            superInitialized = true;
    }

    private boolean isFirstConstructor;

    AnalyzerAdapter analyzer;

    protected NonDeterministicReplayMethodVisitor(int api, MethodVisitor mv, int access,
            String name, String desc, String classDesc, boolean isFirstConstructor,
            AnalyzerAdapter analyzer, boolean isCallbackInit) {
        super(api, mv);
        this.name = name;
        this.desc = desc;
        this.classDesc = classDesc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.constructor = "<init>".equals(name);
        this.isFirstConstructor = isFirstConstructor;
        this.analyzer = analyzer;
        this.isCallbackInit = isCallbackInit;
    }

    private NonDeterministicReplayClassVisitor parent;

    public void setClassVisitor(NonDeterministicReplayClassVisitor coaClassVisitor) {
        this.parent = coaClassVisitor;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
    	super.visitMaxs(maxStack+5, maxLocals);
    }
    @Override
    public void visitEnd() {
        super.visitEnd();
        parent.addFieldMarkup(methodCallsToClear);
        parent.addCaptureMethodsToGenerate(captureMethodsToGenerate);
    }

    private int lineNumber = 0;

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        lineNumber = line;
    }


    private HashMap<String, MethodInsnNode> captureMethodsToGenerate = new HashMap<String, MethodInsnNode>();

    private boolean inited;

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        if (owner.equals(Type.getInternalName(ChroniclerJExportRunner.class))
                && name.equals("genTestCase"))
            return;
        if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
            opcode = Opcodes.INVOKESTATIC;
            owner = "edu/columbia/cs/psl/chroniclerj/MethodInterceptor";
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "edu/columbia/cs/psl/chroniclerj/MethodInterceptor", "invokeReplay",
                    "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
            return;
        }
        try {
            MethodCall m = new MethodCall(this.name, this.desc, this.classDesc, 0, lineNumber,
                    owner, name, desc, isStatic);
            Type returnType = Type.getMethodType(desc).getReturnType();
            if (opcode == INVOKESPECIAL
                    && name.equals("<init>")
                    && NonDeterministicLoggingMethodVisitor.nonDeterministicMethods.contains(owner
                            + "." + name + ":" + desc)) {
            	//I don't think that there is actually anything legal to be done here?
                super.visitMethodInsn(opcode, owner, name, desc, itfc);
            } else if ((!constructor || isFirstConstructor || superInitialized)
                    && returnType.equals(Type.VOID_TYPE)
                    && !name.equals("<init>")
                    && NonDeterministicLoggingMethodVisitor.nonDeterministicMethods.contains(owner
                            + "." + name + ":" + desc)) {
                Type[] args = Type.getArgumentTypes(desc);
                for (int i = args.length - 1; i >= 0; i--) {
                    Type t = args[i];
                    if (t.getSize() == 2)
                        mv.visitInsn(POP2);
                    else
                        mv.visitInsn(POP);
                }
                if (opcode != INVOKESTATIC)
                    mv.visitInsn(POP);

                // else
                // super.visitMethodInsn(opcode, owner, name, desc);

            } else if ((!constructor || isFirstConstructor || superInitialized)
                    && !returnType.equals(Type.VOID_TYPE)
                    && NonDeterministicLoggingMethodVisitor.nonDeterministicMethods.contains(owner
                            + "." + name + ":" + desc)) {
                logger.debug("Adding field in MV to list " + m.getLogFieldName());
                methodCallsToClear.add(m);
				Type[] targs = Type.getArgumentTypes(desc);
				for (int i = targs.length - 1; i >= 0; i--) {
					Type t = targs[i];
					if (t.getSort() == Type.ARRAY) {
						getNextReplay(t);
						super.visitInsn(DUP);
						super.visitInsn(ARRAYLENGTH);
						//Copy the contents of the replay'ed array into the one on stack.
						super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class), "copyInto", "(Ljava/lang/Object;Ljava/lang/Object;I)V", false);
					} else {
						switch (t.getSize()) {
						case 2:
							mv.visitInsn(POP2);
							break;
						case 1:
						default:
							mv.visitInsn(POP);
							break;
						}
					}
				}
				if (opcode != INVOKESTATIC)
                    mv.visitInsn(POP);

                if (returnType.getSort() != Type.VOID)
                    getNextReplay(m.getReturnType());
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itfc);
                if(constructor && !superInitialized && opcode == INVOKESPECIAL && name.equals("<init>"))
             	{
             		onMethodEnter();
             		superInitialized = true;
             	}
            }
        } catch (Exception ex) {
            logger.error("Unable to instrument method call", ex);
        }
    }

	private void getNextReplay(Type t) {
		switch (t.getSort()) {
		case Type.OBJECT:
		case Type.ARRAY:
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class), "getNextObject", "()Ljava/lang/Object;", false);
			super.visitTypeInsn(CHECKCAST, t.getInternalName());
			break;	
		default:
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class), "getNext"+t.getDescriptor(), "()"+t.getDescriptor(), false);
			break;
		}
	}

	protected void onMethodEnter() {
        if (this.name.equals("<init>") && isCallbackInit) {
            super.visitVarInsn(ALOAD, 0);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CallbackRegistry.class),
                    "register", "(Ljava/lang/Object;)V", false);
            inited = true;
        }
    }

    private ArrayList<MethodCall> methodCallsToClear = new ArrayList<MethodCall>();

}
