
package edu.columbia.cs.psl.chroniclerj.visitor;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;
import edu.columbia.cs.psl.chroniclerj.CloningUtils;
import edu.columbia.cs.psl.chroniclerj.Constants;
import edu.columbia.cs.psl.chroniclerj.Instrumenter;
import edu.columbia.cs.psl.chroniclerj.Log;
import edu.columbia.cs.psl.chroniclerj.SerializableLog;

public class CloningAdviceAdapter extends InstructionAdapter implements Opcodes {

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

    public CloningAdviceAdapter(MethodVisitor mv, int access, String name, String desc,
            String classname) {
        super(Opcodes.ASM5, mv);
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
                "(Ljava/lang/String;)V", false);

        visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()Ljava/lang/Thread;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);
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
                super.visitJumpInsn(IFNULL, nullContinue);
                if (secondElHasArrayLen) {
                    swap();
                } else {
                    dup();
                    visitInsn(ARRAYLENGTH);
                }
                dup();
                newarray(Type.getType(fieldType.getDescriptor().substring(1)));
                dupX2();
                swap();
                iconst(0);
                dupX2();
                swap();
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy",
                        "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
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
				invokestatic(Type.getType(CloningUtils.class).getInternalName(), "clone", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
                checkcast(fieldType);
            }
        } else if (fieldType.getClassName().contains("InputStream")
                || fieldType.getClassName().contains("OutputStream")
                || fieldType.getClassName().contains("Socket")) {
            // Do nothing
        } else {
            // println("heavy> " + debug);
            visitLdcInsn(debug);
			invokestatic(Type.getType(CloningUtils.class).getInternalName(), "clone", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
            checkcast(fieldType);
        }
    }

    private boolean isImmutable(String desc) {
        if (immutableClasses.contains(desc))
            return true;
        return false;
    }

    private boolean isSetNull(String desc) {
        if (nullInsteads.contains(desc))
            return true;
        return false;
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
    	String t;
    	switch(elementType.getSort())
    	{
	    	case Type.ARRAY:
	    	case Type.OBJECT:
	    		t = "Ljava/lang/Object;";
	    		break;
	    		default:
	    			t = elementType.getDescriptor();
    	}
    	super.visitLdcInsn(debug);
    	super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Log.class), "log", "("+t+"Ljava/lang/String;)V", false);
    }

    public void setLocalVariableSorter(LocalVariablesSorter smv) {
        this.lvsorter = smv;
    }

}
