
package edu.columbia.cs.psl.chroniclerj.visitor;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;
import edu.columbia.cs.psl.chroniclerj.CloningUtils;
import edu.columbia.cs.psl.chroniclerj.Constants;
import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.Log;
import edu.columbia.cs.psl.chroniclerj.SerializableLog;

public class CloningAdviceAdapter extends GeneratorAdapter implements Opcodes {

    private static final HashSet<String> immutableClasses = new HashSet<String>();

    private static final HashSet<String> nullInsteads = new HashSet<>();
    static {
        immutableClasses.add("Ljava/lang/Integer;");
        immutableClasses.add("Ljava/lang/Long;");
        immutableClasses.add("Ljava/lang/Short;");
        immutableClasses.add("Ljava/lang/Float;");
        immutableClasses.add("Ljava/lang/String;");
        immutableClasses.add("Ljava/lang/Char;");
        immutableClasses.add("Ljava/lang/Byte;");
        immutableClasses.add("Ljava/lang/Integer;");
        immutableClasses.add("Ljava/lang/Long;");
        immutableClasses.add("Ljava/lang/Short;");
        immutableClasses.add("Ljava/lang/Float;");
        immutableClasses.add("Ljava/lang/String;");
        immutableClasses.add("Ljava/lang/Char;");
        immutableClasses.add("Ljava/lang/Byte;");
        immutableClasses.add("Ljava/sql/ResultSet;");
        immutableClasses.add("Ljava/lang/Class;");
        immutableClasses.add("Ljava/net/InetAddress;");
        immutableClasses.add("Ljava/util/TimeZone;");
        immutableClasses.add("Ljava/util/zip/ZipEntry;");
        immutableClasses.add("Z");
        immutableClasses.add("B");
        immutableClasses.add("C");
        immutableClasses.add("S");
        immutableClasses.add("I");
        immutableClasses.add("J");
        immutableClasses.add("F");
        immutableClasses.add("L");
        for (Class<?> cz : CloningUtils.moreIgnoredImmutables) {
            immutableClasses.add(Type.getDescriptor(cz));
        }

        for (Class<?> cz : CloningUtils.nullInsteads) {
            nullInsteads.add(Type.getDescriptor(cz));
        }
    }

    private LocalVariablesSorter lvsorter;

    public CloningAdviceAdapter(int api, MethodVisitor mv, int access, String name, String desc,
            String classname) {
        super(api, mv, access, name, desc);
    }

    /**
     * Precondition: Current element at the top of the stack is the element we
     * need cloned Post condition: Current element at the top of the stack is
     * the cloned element (and non-cloned is removed)
     */

    protected void cloneValAtTopOfStack(String typeOfField) {
        _generateClone(typeOfField, Constants.OUTER_COPY_METHOD_NAME, null, false);
    }

    protected void cloneValAtTopOfStack(String typeOfField, String debug,
            boolean secondElHasArrayLen) {
        _generateClone(typeOfField, Constants.OUTER_COPY_METHOD_NAME, debug, secondElHasArrayLen);
    }

    protected void generateCloneInner(String typeOfField) {
        _generateClone(typeOfField, Constants.INNER_COPY_METHOD_NAME, null, false);
    }

    public void println(String toPrint) {
        visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        visitLdcInsn(toPrint + " : ");
        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print",
                "(Ljava/lang/String;)V");

        visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()Ljava/lang/Thread;");
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;");
        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V");
    }

    private void _generateClone(String typeOfField, String copyMethodToCall, String debug,
            boolean secondElHasArrayLen) {
        Type fieldType = Type.getType(typeOfField);

        if (
        // fieldType.getSort() == Type.ARRAY &&
        // fieldType.getElementType().getSort()
        // ||
        fieldType.getSort() == Type.VOID
                || (fieldType.getSort() != Type.ARRAY && (fieldType.getSort() != Type.OBJECT || isImmutable(typeOfField)))) {
            // println("reference> " + debug);
            return;
        } else if (isSetNull(typeOfField)) {
            pop();
            visitInsn(ACONST_NULL);
        }
        if (fieldType.getSort() == Type.ARRAY) {
            if (fieldType.getElementType().getSort() != Type.OBJECT
                    || immutableClasses.contains(fieldType.getElementType().getDescriptor())) {
                // println("array> " + debug);

                // Just need to duplicate the array
                dup();
                Label nullContinue = new Label();
                ifNull(nullContinue);
                if (secondElHasArrayLen) {
                    swap();
                } else {
                    dup();
                    visitInsn(ARRAYLENGTH);
                }
                dup();
                newArray(Type.getType(fieldType.getDescriptor().substring(1)));
                dupX2();
                swap();
                push(0);
                dupX2();
                swap();
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy",
                        "(Ljava/lang/Object;ILjava/lang/Object;II)V");
                Label noNeedToPop = new Label();
                if (secondElHasArrayLen) {
                    visitJumpInsn(GOTO, noNeedToPop);
                    visitLabel(nullContinue);
                    swap();
                    pop();
                } else {
                    visitLabel(nullContinue);
                }

                visitLabel(noNeedToPop);

            } else {
                // println("heavy> " + debug);
                // Just use the reflective cloner
                visitLdcInsn(debug);
                invokeStatic(Type.getType(CloningUtils.class),
                        Method.getMethod("Object clone(Object, String)"));
                checkCast(fieldType);
            }
        } else if (fieldType.getClassName().contains("InputStream")
                || fieldType.getClassName().contains("OutputStream")
                || fieldType.getClassName().contains("Socket")) {
            // Do nothing
        } else {
            // println("heavy> " + debug);
            visitLdcInsn(debug);
            invokeStatic(Type.getType(CloningUtils.class),
                    Method.getMethod("Object clone(Object, String)"));
            checkCast(fieldType);

        }
    }

    private boolean isImmutable(String desc) {
        if (immutableClasses.contains(desc))
            return true;
        final String parent = Instrumenter.getParentType(desc);
        if (parent == null)
            return false;
        return isImmutable(parent);
    }

    private boolean isSetNull(String desc) {
        if (nullInsteads.contains(desc))
            return true;
        final String parent = Instrumenter.getParentType(desc);
        if (parent == null)
            return false;
        return isSetNull(parent);
    }

    protected void logValueAtTopOfStackToArray(String logFieldOwner, String logFieldName,
            String logFieldTypeDesc, Type elementType, boolean isStaticLoggingField, String debug,
            boolean secondElHasArrayLen, boolean doLocking) {
        if (secondElHasArrayLen) {
            dupX1();
            // swap(); //data, size
            // dupX1(); //size, data, size
            // swap(); //size, size, data
            // dupX2(); //size, data ,size, data
        } else {
            if (elementType.getSize() == 1)
                dup(); // size?, data,size?, data
            else
                dup2();
        }
        cloneValAtTopOfStack(elementType.getDescriptor(), debug, secondElHasArrayLen);
        logValueAtTopOfStackToArrayNoDup(logFieldOwner, logFieldName, logFieldTypeDesc,
                elementType, isStaticLoggingField, debug, secondElHasArrayLen, doLocking);
    }

    protected void logValueAtTopOfStackToArrayNoDup(String logFieldOwner, String logFieldName,
            String logFieldTypeDesc, Type elementType, boolean isStaticLoggingField, String debug,
            boolean secondElHasArrayLen, boolean doLocking) {
        int getOpcode = (isStaticLoggingField ? Opcodes.GETSTATIC : Opcodes.GETFIELD);
        int putOpcode = (isStaticLoggingField ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD);

        // Lock
        if (doLocking) {
            super.visitFieldInsn(GETSTATIC, Type.getInternalName(Log.class), "logLock",
                    Type.getDescriptor(Lock.class));
            super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Lock.class), "lock", "()V");
        }
        // Grow the array if necessary

        super.visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
                Type.INT_TYPE.getDescriptor());
        super.visitFieldInsn(getOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);
        super.arrayLength();
        Label labelForNoNeedToGrow = new Label();
        super.ifCmp(Type.INT_TYPE, Opcodes.IFNE, labelForNoNeedToGrow);
        // In this case, it's necessary to grow it
        // Create the new array and initialize its size

        int newArray = lvsorter.newLocal(Type.getType(logFieldTypeDesc));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);
        arrayLength();
        visitInsn(Opcodes.I2D);
        visitLdcInsn(Constants.LOG_GROWTH_RATE);
        visitInsn(Opcodes.DMUL);
        visitInsn(Opcodes.D2I);

        newArray(Type.getType(logFieldTypeDesc.substring(1))); // Bug in
                                                               // ASM
                                                               // prevents
                                                               // us
                                                               // from
                                                               // doing
                                                               // type.getElementType
        storeLocal(newArray, Type.getType(logFieldTypeDesc));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);
        visitInsn(Opcodes.ICONST_0);
        loadLocal(newArray);
        visitInsn(Opcodes.ICONST_0);
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);
        arrayLength();
        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy",
                "(Ljava/lang/Object;ILjava/lang/Object;II)V");

        // array = newarray

        loadLocal(newArray);
        visitFieldInsn(putOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);

        int newArray2 = lvsorter.newLocal(Type.getType("[Ljava/lang/String;"));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_owners", "[Ljava/lang/String;");
        arrayLength();
        visitInsn(Opcodes.I2D);
        visitLdcInsn(Constants.LOG_GROWTH_RATE);
        visitInsn(Opcodes.DMUL);
        visitInsn(Opcodes.D2I);

        newArray(Type.getType("Ljava/lang/String;"));

        storeLocal(newArray2, Type.getType("[Ljava/lang/String;"));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_owners", "[Ljava/lang/String;");
        visitInsn(Opcodes.ICONST_0);
        loadLocal(newArray2);
        visitInsn(Opcodes.ICONST_0);
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_owners", "[Ljava/lang/String;");
        arrayLength();
        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy",
                "(Ljava/lang/Object;ILjava/lang/Object;II)V");

        // array = newarray

        loadLocal(newArray2);
        visitFieldInsn(putOpcode, logFieldOwner, logFieldName + "_owners", "[Ljava/lang/String;");
        
        
        int newArray3 = lvsorter.newLocal(Type.getType("[Ljava/lang/String;"));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_debug", "[Ljava/lang/String;");
        arrayLength();
        visitInsn(Opcodes.I2D);
        visitLdcInsn(Constants.LOG_GROWTH_RATE);
        visitInsn(Opcodes.DMUL);
        visitInsn(Opcodes.D2I);

        newArray(Type.getType("Ljava/lang/String;"));

        storeLocal(newArray3, Type.getType("[Ljava/lang/String;"));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_debug", "[Ljava/lang/String;");
        visitInsn(Opcodes.ICONST_0);
        loadLocal(newArray3);
        visitInsn(Opcodes.ICONST_0);
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_debug", "[Ljava/lang/String;");
        arrayLength();
        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy",
                "(Ljava/lang/Object;ILjava/lang/Object;II)V");

        // array = newarray

        loadLocal(newArray3);
        visitFieldInsn(putOpcode, logFieldOwner, logFieldName + "_debug", "[Ljava/lang/String;");

        visitLabel(labelForNoNeedToGrow);
        // Load this into the end piece of the array
        if (elementType.getSize() == 1) {
            // if (secondElHasArrayLen) {
            // /*
            // * size buf
            // */
            // dupX1();
            // /*
            // * buf size buf
            // */
            // visitFieldInsn(getOpcode, logFieldOwner, logFieldName,
            // logFieldTypeDesc);
            // dupX2();
            // pop();
            // /*
            // * buf logfield size buf
            // */
            // visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
            // Type.INT_TYPE.getDescriptor());
            // dupX2();
            // pop();
            // /*
            // * buf logfield logsize size buf
            // */
            // } else {
            // dup();
            visitFieldInsn(getOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);
            swap();
            visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
                    Type.INT_TYPE.getDescriptor());
            swap();
            // }
        } else if (elementType.getSize() == 2) {
            // dup2();
            if (!isStaticLoggingField)
                super.loadThis();
            super.visitFieldInsn(getOpcode, logFieldOwner, logFieldName, logFieldTypeDesc);
            dupX2();
            pop();
            if (!isStaticLoggingField)
                super.loadThis();
            super.visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
                    Type.INT_TYPE.getDescriptor());
            dupX2();
            pop();
        }

        arrayStore(elementType);

        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_debug", "[Ljava/lang/String;");
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
                Type.INT_TYPE.getDescriptor());
        visitLdcInsn(debug);
        arrayStore(Type.getType(String.class));

        
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_owners", "[Ljava/lang/String;");
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
                Type.INT_TYPE.getDescriptor());

        if (debug.startsWith("callback"))
            visitLdcInsn("callback-handler");
        else {
            visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                    "()Ljava/lang/Thread;");
            visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;");

            visitInsn(DUP);
            visitLdcInsn("Finalizer");
            visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
            Label contin = new Label();
            visitJumpInsn(IFEQ, contin);
            // we are in finalize thread
            visitInsn(POP);
            visitTypeInsn(NEW, "java/lang/StringBuilder");
            visitInsn(DUP);
            visitLdcInsn("Finalizer");
            visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>",
                    "(Ljava/lang/String;)V");
            visitFieldInsn(Opcodes.GETSTATIC, "edu/columbia/cs/psl/chroniclerj/replay/ReplayUtils",
                    "curFinalizer", "J");
            visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(J)Ljava/lang/StringBuilder;");
            visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;");
            visitLabel(contin);

        }
        arrayStore(Type.getType(String.class));
        visitFieldInsn(getOpcode, logFieldOwner, logFieldName + "_fill",
                Type.INT_TYPE.getDescriptor());

        super.visitInsn(Opcodes.ICONST_1);
        super.visitInsn(Opcodes.IADD);
        super.visitFieldInsn(putOpcode, logFieldOwner, logFieldName + "_fill",
                Type.INT_TYPE.getDescriptor());
        // println("Incremented fill for " + logFieldOwner+"."+logFieldName);
        // Release the export lock
        // super.visitFieldInsn(GETSTATIC,
        // Type.getInternalName(CloningUtils.class), "exportLock",
        // Type.getDescriptor(ReadWriteLock.class));
        // super.visitMethodInsn(INVOKEINTERFACE,
        // Type.getInternalName(ReadWriteLock.class), "readLock",
        // "()Ljava/util/concurrent/locks/Lock;");
        // super.visitMethodInsn(INVOKEINTERFACE,
        // Type.getInternalName(Lock.class), "unlock", "()V");

        // if (threadSafe) {
        // Unlock
        // super.visitVarInsn(ALOAD, monitorIndx);
        // super.monitorExit();
        // visitLabel(monitorEndLabel);
        Label endLbl = new Label();

        // if (elementType.getSort() == Type.ARRAY) {
        // super.visitInsn(DUP);
        // super.visitInsn(ARRAYLENGTH);
        // } else
        super.visitInsn(ICONST_1);
        // super.visitVarInsn(ALOAD, monitorIndx);
        // super.monitorEnter();
        super.visitFieldInsn(getOpcode, logFieldOwner, "logsize", Type.INT_TYPE.getDescriptor());
        super.visitInsn(IADD);
        super.visitInsn(DUP);
        super.visitFieldInsn(PUTSTATIC, logFieldOwner, "logsize", Type.INT_TYPE.getDescriptor());

        super.visitLdcInsn(Constants.MAX_LOG_SIZE);
        // super.visitInsn(ISUB);
        super.visitJumpInsn(IF_ICMPLE, endLbl);
        // super.ifCmp(Type.INT_TYPE, Opcodes.IFGE, endLbl);
        // super.visitVarInsn(ALOAD, monitorIndx);
        // super.monitorExit();
        if (logFieldOwner.equals(Type.getInternalName(SerializableLog.class)))
            super.visitMethodInsn(INVOKESTATIC,
                    Type.getInternalName(ChroniclerJExportRunner.class), "_exportSerializable",
                    "()V");
        else
            super.visitMethodInsn(INVOKESTATIC,
                    Type.getInternalName(ChroniclerJExportRunner.class), "_export", "()V");
        // super.visitVarInsn(ALOAD, monitorIndx);
        // super.monitorEnter();
        // super.visitFieldInsn(getOpcode, logFieldOwner, "logsize",
        // Type.INT_TYPE.getDescriptor());
        // super.visitLdcInsn(Constants.VERY_MAX_LOG_SIZE);
        // super.visitJumpInsn(IF_ICMPLE, endLbl);

        // println("GOing to wait for " + logFieldOwner);
        // super.visitLabel(tryStart);

        // super.visitFieldInsn(Opcodes.GETSTATIC,
        // Type.getInternalName(Log.class), "lock", "Ljava/lang/Object;");
        // super.visitLdcInsn(500L);
        // super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "wait",
        // "(J)V");

        // super.visitLabel(tryEnd);

        // super.visitJumpInsn(GOTO, endLbl);
        // super.visitLabel(handlerStart);
        // int n = newLocal(Type.getType(InterruptedException.class));
        // super.visitVarInsn(ASTORE, n);
        // super.visitInsn(POP);
        visitLabel(endLbl);
        // super.visitVarInsn(ALOAD, monitorIndx);
        // super.monitorExit();
        if (doLocking) {
            super.visitFieldInsn(GETSTATIC, Type.getInternalName(Log.class), "logLock",
                    Type.getDescriptor(Lock.class));
            super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Lock.class), "unlock",
                    "()V");
        }
        // super.visitLocalVariable(logFieldName + "_monitor",
        // "Ljava/lang/Object;", null, monitorStart, monitorEndLabel,
        // monitorIndx);
        // }

    }

    public void setLocalVariableSorter(LocalVariablesSorter smv) {
        this.lvsorter = smv;
    }

}
