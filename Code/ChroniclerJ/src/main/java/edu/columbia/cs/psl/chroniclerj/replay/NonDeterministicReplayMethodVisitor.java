
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
import org.objectweb.asm.tree.MethodInsnNode;

import edu.columbia.cs.psl.chroniclerj.CallbackRegistry;
import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;
import edu.columbia.cs.psl.chroniclerj.ExportedLog;
import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.Log;
import edu.columbia.cs.psl.chroniclerj.MethodCall;
import edu.columbia.cs.psl.chroniclerj.struct.AnnotatedMethod;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingMethodVisitor;

public class NonDeterministicReplayMethodVisitor extends AdviceAdapter implements Opcodes {
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
            AnnotatedMethod am = Instrumenter.getAnnotatedMethod(this.classDesc, "finalize", "()V");
            if (am != null && am.isCallsNDMethods()) {
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
            }
        }
        if (!constructor)
            superInitialized = true;
    }

    private boolean isFirstConstructor;

    AnalyzerAdapter analyzer;

    protected NonDeterministicReplayMethodVisitor(int api, MethodVisitor mv, int access,
            String name, String desc, String classDesc, boolean isFirstConstructor,
            AnalyzerAdapter analyzer, boolean isCallbackInit) {
        super(api, mv, access, name, desc);
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

    private void loadReplayIndex(String className, String fieldName) {
        // super.visitFieldInsn(GETSTATIC, className, fieldName +
        // "_replayIndex", "Ljava/util/HashMap;");
        // super.visitMethodInsn(INVOKESTATIC,
        // Type.getInternalName(Thread.class), "currentThread",
        // "()Ljava/lang/Thread;");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(Thread.class), "getName",
        // "()Ljava/lang/String;");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(HashMap.class), "containsKey",
        // "(Ljava/lang/Object;)Z");
        // Label exists = new Label();
        // super.visitJumpInsn(Opcodes.IFNE, exists);
        // super.visitFieldInsn(GETSTATIC, className, fieldName +
        // "_replayIndex", "Ljava/util/HashMap;");
        // super.visitMethodInsn(INVOKESTATIC,
        // Type.getInternalName(Thread.class), "currentThread",
        // "()Ljava/lang/Thread;");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(Thread.class), "getName",
        // "()Ljava/lang/String;");
        // super.visitInsn(ICONST_0);
        // super.visitMethodInsn(INVOKESTATIC,
        // Type.getInternalName(Integer.class), "valueOf",
        // "(I)Ljava/lang/Integer;");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(HashMap.class), "put",
        // "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        // super.visitInsn(POP);
        // super.visitLabel(exists);
        // super.visitFieldInsn(GETSTATIC, className, fieldName +
        // "_replayIndex", "Ljava/util/HashMap;");
        // super.visitMethodInsn(INVOKESTATIC,
        // Type.getInternalName(Thread.class), "currentThread",
        // "()Ljava/lang/Thread;");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(Thread.class), "getName",
        // "()Ljava/lang/String;");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(HashMap.class), "get",
        // "(Ljava/lang/Object;)Ljava/lang/Object;");
        // super.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        // super.visitMethodInsn(INVOKEVIRTUAL,
        // Type.getInternalName(Integer.class), "intValue", "()I");

        Label load = new Label();
        visitLabel(load);
        super.visitFieldInsn(GETSTATIC, className, fieldName + "_replayIndex",
                "Ljava/util/HashMap;");
        super.visitFieldInsn(GETSTATIC, className, fieldName + "_owners", "[Ljava/lang/String;");
        super.visitFieldInsn(GETSTATIC, className, fieldName + "_fill", "I");
        super.visitLdcInsn(className);
        if (className.contains("Serializable"))
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class),
                    "getNextIndex", "(Ljava/util/HashMap;[Ljava/lang/String;ILjava/lang/String;)I", false);
        else {
            super.visitFieldInsn(GETSTATIC, className, fieldName, "[Ljava/lang/Object;");

            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class),
                    "getNextIndexO",
                    "(Ljava/util/HashMap;[Ljava/lang/String;ILjava/lang/String;[Ljava/lang/Object;)I", false);
        }
        super.visitInsn(DUP);
        Label cont = new Label();
        super.visitJumpInsn(IFGE, cont);
        super.visitInsn(POP);
        super.visitLdcInsn(className);
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayRunner.class),
                "loadNextLog", "(Ljava/lang/String;)V", false);
        super.visitJumpInsn(GOTO, load);
        visitLabel(cont);
        // super.visitInsn(ICONST_0);
    }

    private void incrementReplayIndex(String className, String fieldName) {
        super.visitFieldInsn(GETSTATIC, className, fieldName + "_replayIndex",
                "Ljava/util/HashMap;");
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Thread.class), "currentThread",
                "()Ljava/lang/Thread;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Thread.class), "getName",
                "()Ljava/lang/String;", false);
        loadReplayIndex(className, fieldName);
        super.visitInsn(ICONST_1);
        super.visitInsn(IADD);
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
                "(I)Ljava/lang/Integer;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(HashMap.class), "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
        super.visitInsn(POP);

        super.visitFieldInsn(GETSTATIC, Type.getInternalName(ExportedLog.class),
                "globalReplayIndex", "I");
        super.visitInsn(ICONST_1);
        super.visitInsn(IADD);
        super.visitFieldInsn(PUTSTATIC, Type.getInternalName(ExportedLog.class),
                "globalReplayIndex", "I");
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

                if (!(owner.equals(Instrumenter.instrumentedClasses.get(classDesc).superName) && this.name
                        .equals("<init>"))) {
                    if (analyzer.stack == null) {
                        super.visitMethodInsn(opcode, owner, name, desc, itfc);
                    } else {
                        Type[] args = Type.getArgumentTypes(desc);
                        for (int i = args.length - 1; i >= 0; i--) {
                            Type t = args[i];
                            if (t.getSize() == 2)
                                mv.visitInsn(POP2);
                            else
                                mv.visitInsn(POP);
                        }

                        if (analyzer.stack != null
                                && analyzer.stack.size() > 0
                                && analyzer.uninitializedTypes.containsKey(analyzer.stack
                                        .get(analyzer.stack.size() - 1))
                                && analyzer.uninitializedTypes.get(
                                        analyzer.stack.get(analyzer.stack.size() - 1))
                                        .equals(owner)) {
                            mv.visitInsn(POP);
                            if (analyzer.stack.size() > 0
                                    && analyzer.uninitializedTypes.containsKey(analyzer.stack
                                            .get(analyzer.stack.size() - 1))
                                    && analyzer.uninitializedTypes.get(
                                            analyzer.stack.get(analyzer.stack.size() - 1)).equals(
                                            owner))
                                mv.visitInsn(POP);

                            String replayClassName = MethodCall.getReplayClassName(Type.getType("L"
                                    + m.getMethodOwner() + ";"));
                            mv.visitFieldInsn(GETSTATIC, replayClassName, m.getLogFieldName(),
                                    "[Ljava/lang/Object;");

                            loadReplayIndex(replayClassName, m.getLogFieldName());

                            mv.visitInsn(AALOAD);
                            mv.visitTypeInsn(CHECKCAST, m.getMethodOwner());
                            incrementReplayIndex(replayClassName, m.getLogFieldName());
                        }
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itfc);
                }

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

                super.visitFieldInsn(GETSTATIC, Type.getInternalName(Log.class), "logLock",
                        Type.getDescriptor(Lock.class));
                super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Lock.class), "lock",
                        "()V", true);

                logger.debug("Adding field in MV to list " + m.getLogFieldName());
                methodCallsToClear.add(m);
                Type[] args = Type.getArgumentTypes(desc);
                boolean hasArray = false;
                for (Type t : args)
                    if (t.getSort() == Type.ARRAY)
                        hasArray = true;

                if (hasArray) {

                    Type[] targs = Type.getArgumentTypes(desc);
                    for (int i = targs.length - 1; i >= 0; i--) {
                        Type t = targs[i];
                        if (t.getSort() == Type.ARRAY) {
                            /*
                             * stack (grows down): dest (fill not incremented
                             * yet)
                             */
                            String replayClassName = MethodCall.getReplayClassName(t);
                            String replayFieldName = MethodCall.getLogFieldName(t);
                            mv.visitFieldInsn(GETSTATIC, replayClassName, MethodCall
                                    .getLogFieldName(t), MethodCall.getLogFieldType(t)
                                    .getDescriptor());
                            // mv.visitFieldInsn(GETSTATIC,replayClassName,
                            // MethodCall.getLogFieldName(t)+"_replayIndex",
                            // "I");
                            loadReplayIndex(replayClassName, replayFieldName);
                            // mv.visitInsn(DUP);
                            // mv.visitFieldInsn(GETSTATIC, replayClassName,
                            // MethodCall.getLogFieldName(t) + "_fill", "I");
                            // Label fallThrough = new Label();
                            //
                            // mv.visitJumpInsn(Opcodes.IF_ICMPNE, fallThrough);
                            // mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            // Type.getInternalName(ReplayRunner.class),
                            // "loadNextLog", "()V");
                            // pop();
                            // loadReplayIndex(replayClassName,
                            // replayFieldName);
                            // visitLabel(fallThrough);

                            arrayLoad(t);

                            /*
                             * stack (grows down): dest src
                             */
                            swap();
                            /*
                             * stack (grows down): src dest
                             */
                            push(0);
                            /*
                             * stack (grows down): src dest 0
                             */
                            swap();
                            /*
                             * stack (grows down): src 0 dest
                             */
                            push(0);
                            /*
                             * stack (grows down): src 0 dest 0
                             */

                            mv.visitFieldInsn(GETSTATIC, replayClassName, MethodCall
                                    .getLogFieldName(t), MethodCall.getLogFieldType(t)
                                    .getDescriptor());
                            loadReplayIndex(replayClassName, replayFieldName);
                            arrayLoad(t);
                            mv.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                            mv.visitInsn(ARRAYLENGTH);
                            incrementReplayIndex(replayClassName, replayFieldName);
                            /*
                             * stack: src (fill incremented) 0 dest 0 length
                             */
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy",
                                    "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
                            /*
                             * stack: dest popped
                             */
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

                } else {
                    // Type[] targs = Type.getArgumentTypes(desc);
                    // for (Type t : targs) {
                    // switch (t.getSize()) {
                    // case 2:
                    // visitInsn(POP2);
                    // break;
                    // case 1:
                    // default:
                    // visitInsn(POP);
                    // break;
                    // }
                    // }
                    for (int i = args.length - 1; i >= 0; i--) {
                        Type t = args[i];
                        if (t.getSize() == 2)
                            mv.visitInsn(POP2);
                        else
                            mv.visitInsn(POP);
                    }
                }

                if (opcode != INVOKESTATIC)
                    mv.visitInsn(POP);

                if (returnType.getSort() == Type.VOID)
                    mv.visitInsn(NOP);
                else {
                    mv.visitFieldInsn(GETSTATIC, m.getReplayClassName(), m.getLogFieldName(), m
                            .getLogFieldType().getDescriptor());

                    loadReplayIndex(m.getReplayClassName(), m.getLogFieldName());
                    arrayLoad(m.getReturnType());
                    if(m.getReturnType().getSort() == Type.OBJECT || m.getReturnType().getSort() == Type.ARRAY)
                    	super.visitTypeInsn(CHECKCAST, m.getReturnType().getInternalName());
                    incrementReplayIndex(m.getReplayClassName(), m.getLogFieldName());
                }
                // Unlock
                super.visitFieldInsn(GETSTATIC, Type.getInternalName(Log.class), "logLock",
                        Type.getDescriptor(Lock.class));
                super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Lock.class), "unlock",
                        "()V", true);

            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itfc);
            }
        } catch (Exception ex) {
            logger.error("Unable to instrument method call", ex);
        }
    }

    @Override
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
