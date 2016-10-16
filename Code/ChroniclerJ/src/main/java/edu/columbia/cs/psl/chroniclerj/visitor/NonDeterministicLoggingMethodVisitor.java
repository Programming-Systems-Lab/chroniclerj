
package edu.columbia.cs.psl.chroniclerj.visitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodInsnNode;

import edu.columbia.cs.psl.chroniclerj.Constants;
import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.MethodCall;
import edu.columbia.cs.psl.chroniclerj.replay.NonDeterministicReplayMethodVisitor;
import edu.columbia.cs.psl.chroniclerj.struct.AnnotatedMethod;

public class NonDeterministicLoggingMethodVisitor extends CloningAdviceAdapter {
    private static Logger logger = Logger.getLogger(NonDeterministicLoggingMethodVisitor.class);

    private String name;

    private String desc;

    private String classDesc;

    private int pc;

    public static HashSet<String> nonDeterministicMethods = new HashSet<String>();

    private boolean isStatic;

    private boolean constructor;

    private boolean superInitialized;

    private AnalyzerAdapter analyzer;

    private static HashSet<String> ignoredNDMethods = new HashSet<String>();

    public static boolean isND(String owner, String name, String desc) {
        return nonDeterministicMethods.contains(owner + "." + name + ":" + desc);
    }

    public static void registerNDMethod(String owner, String name, String desc) {
        nonDeterministicMethods.add(owner + "." + name + ":" + desc);
    }

    static {
        Scanner s;
        try {
            s = new Scanner(NonDeterministicReplayMethodVisitor.class.getClassLoader()
                    .getResourceAsStream("nondeterministic-methods.txt"));
            while (s.hasNextLine())
                nonDeterministicMethods.add(s.nextLine());
            if (Instrumenter.IS_DACAPO) {
                s = new Scanner(NonDeterministicLoggingMethodVisitor.class.getClassLoader()
                        .getResourceAsStream("nd-geronimo-ignored"));
                while (s.hasNextLine())
                    nonDeterministicMethods.remove(s.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ignoredNDMethods
                .add("java/util/Properties.keys()Ljava/util/Enumeration;org/apache/geronimo/axis/client/GenericServiceEndpoint.createCall");
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (!constructor)
            superInitialized = true;
    }

    private boolean isFirstConstructor;

    protected NonDeterministicLoggingMethodVisitor(int api, MethodVisitor mv, int access,
            String name, String desc, String classDesc, boolean isFirstConstructor,
            AnalyzerAdapter analyzer) {
        super(api, mv, access, name, desc, classDesc);
        this.name = name;
        this.desc = desc;
        this.classDesc = classDesc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.constructor = "<init>".equals(name);
        this.isFirstConstructor = isFirstConstructor;
        this.analyzer = analyzer;
    }

    private NonDeterministicLoggingClassVisitor parent;

    public void setClassVisitor(NonDeterministicLoggingClassVisitor coaClassVisitor) {
        this.parent = coaClassVisitor;
    }

    @Override
    public void visitEnd() {
        // System.out.println(classDesc + " " + name);
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

    private HashMap<MethodCall, MethodInsnNode> captureMethodsToGenerate = new HashMap<MethodCall, MethodInsnNode>();

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        try {
            if (Instrumenter.instrumentedClasses.get(classDesc) == null) {
                System.err.println("No class info for " + classDesc);
            }
            MethodCall m = new MethodCall(this.name, this.desc, this.classDesc, pc, lineNumber,
                    owner, name, desc, isStatic);
            Type returnType = Type.getMethodType(desc).getReturnType();
            if ((!constructor || isFirstConstructor || superInitialized)
                    && !returnType.equals(Type.VOID_TYPE)
                    && nonDeterministicMethods.contains(owner + "." + name + ":" + desc)
                    && !ignoredNDMethods.contains(owner + "." + name + desc + this.classDesc + "."
                            + this.name)) {
                logger.debug("Adding field in MV to list " + m.getLogFieldName());
                methodCallsToClear.add(m);
                Type[] args = Type.getArgumentTypes(desc);
                boolean hasArray = false;
                for (Type t : args)
                    if (t.getSort() == Type.ARRAY && !name.contains("write")
                            && !name.contains("invoke"))
                        hasArray = true;

                if (hasArray) { // TODO uncomment this block
                    captureMethodsToGenerate.put(m, new MethodInsnNode(opcode, owner, name, desc));
                    String captureDesc = desc;

                    int invokeOpcode = Opcodes.INVOKESTATIC;
                    if (opcode == Opcodes.INVOKESPECIAL && !name.equals("<init>")) {
                        invokeOpcode = Opcodes.INVOKESPECIAL;
                    } else if (opcode != Opcodes.INVOKESTATIC) {
                        // Need to put owner of the method on the top of the
                        // args list
                        captureDesc = "(L" + owner + ";";
                        for (Type t : args)
                            captureDesc += t.getDescriptor();
                        captureDesc += ")" + Type.getReturnType(desc).getDescriptor();
                    }
                    mv.visitMethodInsn(invokeOpcode, classDesc, m.getCapturePrefix() + "_capture",
                            captureDesc);
                    logValueAtTopOfStackToArray(m.getLogClassName(), m.getLogFieldName(), m
                            .getLogFieldType().getDescriptor(), returnType, true, owner + "."
                            + name + "\t" + desc + "\t\t" + classDesc + "." + this.name, false,
                            true);
                } else {
                    mv.visitMethodInsn(opcode, owner, name, desc);
                    logValueAtTopOfStackToArray(m.getLogClassName(), m.getLogFieldName(), m
                            .getLogFieldType().getDescriptor(), returnType, true, owner + "."
                            + name + "\t" + desc + "\t\t" + classDesc + "." + this.name, false,
                            true);
                }
            } else if (opcode == INVOKESPECIAL
                    && name.equals("<init>")
                    && nonDeterministicMethods.contains(owner + "." + name + ":" + desc)
                    && !(owner.equals(Instrumenter.instrumentedClasses.get(classDesc).superName) && this.name
                            .equals("<init>"))) {
                super.visitMethodInsn(opcode, owner, name, desc);
                if (analyzer.stack != null && analyzer.stack.size() > 0
                        && analyzer.stack.get(analyzer.stack.size() - 1).equals(owner))
                    logValueAtTopOfStackToArray(
                            MethodCall.getLogClassName(Type.getType("L" + owner + ";")), "aLog",
                            "[Ljava/lang/Object;", Type.getType("L" + owner + ";"), true, owner
                                    + "." + name + "\t" + desc + "\t\t" + classDesc + "."
                                    + this.name, false, true);

            } else
                mv.visitMethodInsn(opcode, owner, name, desc);

            if (constructor && !init) {
                init = true;
                AnnotatedMethod am = Instrumenter.getAnnotatedMethod(this.classDesc, "finalize",
                        "()V");
                if (am != null && am.isCallsNDMethods()) {
                    visitVarInsn(ALOAD, 0);
                    visitFieldInsn(Opcodes.GETSTATIC, "edu/columbia/cs/psl/chroniclerj/Log",
                            Instrumenter.FIELD_LOGICAL_CLOCK, "J");
                    visitInsn(DUP2_X1);
                    visitFieldInsn(Opcodes.PUTFIELD, this.classDesc,
                            Instrumenter.FIELD_LOGICAL_CLOCK, "J");
                    visitInsn(LCONST_1);
                    visitInsn(LADD);
                    visitFieldInsn(Opcodes.PUTSTATIC, "edu/columbia/cs/psl/chroniclerj/Log",
                            Instrumenter.FIELD_LOGICAL_CLOCK, "J");
                }
            }
            pc++;
        } catch (Exception ex) {
            logger.error("Unable to instrument method call", ex);
        }
    }

    private boolean init = false;

    private HashSet<MethodCall> methodCallsToClear = new HashSet<MethodCall>();

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        pc++;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        pc++;
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        pc++;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        pc++;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        pc++;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        pc++;
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        pc++;
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        pc++;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        pc++;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        pc++;
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        pc++;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        pc++;
    }
}
