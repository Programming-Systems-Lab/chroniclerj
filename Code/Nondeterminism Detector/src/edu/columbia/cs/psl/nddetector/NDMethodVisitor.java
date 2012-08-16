package edu.columbia.cs.psl.nddetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;



public class NDMethodVisitor extends MethodVisitor implements Opcodes {
	String methodFullName;
	int access;
	private HashMap<String, MethodInstance> lookupCache;
	private List<Integer> stack;

	private Integer popV() {
		return stack.remove(stack.size() - 1);
//		return 0;
	}

	private Integer peek() {
		return stack.get(stack.size() - 1);
//		return 0;
	}

	private void pushV(final Integer o) {
		stack.add(o);
	}
	@Override
	public void visitEnd() {
		super.visitEnd();
		lookupCache.get(methodFullName).tainted = new ArrayList<Integer>();
		lookupCache.get(methodFullName).tainted.addAll(tainted);
	}
	private int getArgIndex(final int localVar) {
		int index = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
        for (int i = 0; i < argumentTypes.length; i++) {
            index += argumentTypes[i].getSize();
            if(index == localVar || index+ argumentTypes[i].getSize()-1 == localVar)
            	return i;
        }
        return -1;
    }
	private boolean isAnArgIndex(final int localVar)
	{
		if(localVar == 0 && (Opcodes.ACC_STATIC & access) == 0)
			return false;
		return localVar <= getArgIndex(argumentTypes.length);
	}
    private Type[] argumentTypes;
	private HashSet<Integer> tainted = new HashSet<Integer>();

	public NDMethodVisitor(int api, MethodVisitor mv, String methodFullName, String name, String desc, int access, HashMap<String, MethodInstance> lookupCache) {
		super(api, mv);

		this.methodFullName = methodFullName;
		this.access = access;
		this.lookupCache = lookupCache;
		this.stack = new ArrayList<Integer>();
        this.argumentTypes = Type.getArgumentTypes(desc);

        
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		super.visitMethodInsn(opcode, owner, name, desc);
		String fName = owner + "." + name + ":" + desc;

		lookupCache.get(methodFullName).functionsThatICall.add(fName);

		if (lookupCache.containsKey(fName)) {
			lookupCache.get(fName).functionsThatCallMe.add(methodFullName);
		} else {
			MethodInstance mi = new MethodInstance(fName);
			mi.functionsThatCallMe.add(methodFullName);
			lookupCache.put(fName, mi);
		}

		Type[] types = Type.getArgumentTypes(desc);
		int v = 0;
		for (int i = 0; i < types.length; i++) {
			int r = popV();
			if(r >= 0)
			{
				tainted.add(r);
				v = r;
			}
			if (types[i].getSize() == 2) {
				popV();
			}
		}
		switch (opcode) {
		// case INVOKESTATIC:
		// break;

		case INVOKEINTERFACE:
		case INVOKEVIRTUAL:
			popV(); // objectref
			break;

		case INVOKESPECIAL:
			Object type = popV(); // objectref

			break;
		}

		Type returnType = Type.getReturnType(desc);
		if (returnType != Type.VOID_TYPE) {
			pushV(v);
			if (returnType.getSize() == 2) {
				pushV(v);
			}
		}

	}

	@Override
	public void visitInsn(final int opcode) {
		int s;
		switch (opcode) {
		case RETURN: // empty stack
			break;

		case IRETURN: // 1 before n/a after
		case FRETURN: // 1 before n/a after
		case ARETURN: // 1 before n/a after
		case ATHROW: // 1 before n/a after
			popV();
			break;

		case LRETURN: // 2 before n/a after
		case DRETURN: // 2 before n/a after
			popV();
			popV();
			break;

		case NOP:
		case LALOAD: // remove 2 add 2
		case DALOAD: // remove 2 add 2
		case LNEG:
		case DNEG:
		case FNEG:
		case INEG:
		case L2D:
		case D2L:
		case F2I:
		case I2B:
		case I2C:
		case I2S:
		case I2F:
		case ARRAYLENGTH:
			break;

		case ACONST_NULL:
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			pushV(-1);
			break;
		case F2L: // 1 before 2 after
		case F2D:
		case I2L:
		case I2D:
			pushV(peek());
			break;

		case LCONST_0:
		case LCONST_1:
		case DCONST_0:
		case DCONST_1:
			pushV(-1);
			pushV(-1);
			break;

		case IALOAD: // remove 2 add 1
		case FALOAD: // remove 2 add 1
		case AALOAD: // remove 2 add 1
		case BALOAD: // remove 2 add 1
		case CALOAD: // remove 2 add 1
		case SALOAD: // remove 2 add 1
		case POP:
		case IADD:
		case FADD:
		case ISUB:
		case LSHL: // 3 before 2 after
		case LSHR: // 3 before 2 after
		case LUSHR: // 3 before 2 after
		case L2I: // 2 before 1 after
		case L2F: // 2 before 1 after
		case D2I: // 2 before 1 after
		case D2F: // 2 before 1 after
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
		case FCMPL: // 2 before 1 after
		case FCMPG: // 2 before 1 after
		case IMUL:
		case IDIV:
		case IREM:
		case ISHL:
		case ISHR:
		case IUSHR:
		case IAND:
		case IOR:
		case IXOR:
		case MONITORENTER:
		case MONITOREXIT:
			popV();
			break;

		case POP2:
		case LSUB:
		case LMUL:
		case LDIV:
		case LREM:
		case LADD:
		case LAND:
		case LOR:
		case LXOR:
		case DADD:
		case DMUL:
		case DSUB:
		case DDIV:
		case DREM:
			popV();
			popV();
			break;

		case IASTORE:
		case FASTORE:
		case AASTORE:
		case BASTORE:
		case CASTORE:
		case SASTORE:
		case LCMP: // 4 before 1 after
		case DCMPL:
		case DCMPG:
			popV();
			popV();
			int r = popV();
			if(r >= 0)
				tainted .add(r);
			break;

		case LASTORE:
		case DASTORE:
			popV();
			popV();
			popV();
			r = popV();
			if(r >= 0)
				tainted .add(r);
			break;
			

		case DUP:
			pushV(peek());
			break;

		case DUP_X1:
			s = stack.size();
			stack.add(s - 2, stack.get(s - 1));
			break;

		case DUP_X2:
			s = stack.size();
			stack.add(s - 3, stack.get(s - 1));
			break;

		case DUP2:
			s = stack.size();
			stack.add(s - 2, stack.get(s - 1));
			stack.add(s - 2, stack.get(s - 1));
			break;

		case DUP2_X1:
			s = stack.size();
			stack.add(s - 3, stack.get(s - 1));
			stack.add(s - 3, stack.get(s - 1));
			break;

		case DUP2_X2:
			s = stack.size();
			stack.add(s - 4, stack.get(s - 1));
			stack.add(s - 4, stack.get(s - 1));
			break;

		case SWAP:
			s = stack.size();
			stack.add(s - 2, stack.get(s - 1));
			stack.remove(s);
			break;
		}

		mv.visitInsn(opcode);
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		super.visitVarInsn(opcode, var);

		switch (opcode) {
		case ILOAD:
		case FLOAD:
			pushV(getArgIndex(var));
			break;
		case LLOAD:
		case DLOAD:
			pushV(getArgIndex(var));
			pushV(getArgIndex(var));
			break;
		case ALOAD:
			pushV(getArgIndex(var));
			break;
		case ASTORE:
		case ISTORE:
		case FSTORE:
			popV();
			break;
		case LSTORE:
		case DSTORE:
			popV();
			popV();
			break;

		}
	}
    @Override
    public void visitTryCatchBlock(
        Label start,
        Label end,
        Label handler,
        String type)
    {
        super.visitTryCatchBlock(start, end, handler, type);
        pushV(-1);

    }

	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
		MethodInstance mi = lookupCache.get(methodFullName);
		if(opcode == GETSTATIC || opcode == GETFIELD)
			mi.fieldsGet.add(owner+"."+name);
		else if(opcode == PUTSTATIC|| opcode == PUTFIELD)
			mi.fieldsPut.add(owner+"."+name);
		
		mv.visitFieldInsn(opcode, owner, name, desc);
		char c = desc.charAt(0);
		boolean longOrDouble = c == 'J' || c == 'D';
		switch (opcode) {
		case GETSTATIC:
			pushV(-1);
			if (longOrDouble) {
				pushV(-1);
			}
			break;
		case PUTSTATIC:
			popV();
			if (longOrDouble) {
				popV();
			}
			break;
		case PUTFIELD:
			popV();
			if (longOrDouble) {
				popV();
				popV();
			}
			break;
		// case GETFIELD:
		default:
			if (longOrDouble) {
				pushV(peek());
			}
		}

	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		mv.visitIntInsn(opcode, operand);
		if (opcode != NEWARRAY) {
			pushV(-1);
		}
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		mv.visitLdcInsn(cst);
		pushV(-1);
		if (cst instanceof Double || cst instanceof Long) {
			pushV(-1);
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		mv.visitMultiANewArrayInsn(desc, dims);
		for (int i = 0; i < dims; i++) {
			popV();
		}
		pushV(-1);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		mv.visitTypeInsn(opcode, type);
		// ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack
		if (opcode == NEW) {
			pushV(-1);
		}
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
		Type[] types = Type.getArgumentTypes(desc);
		int v = 0;
		for (int i = 0; i < types.length; i++) {
			int t = popV();
			if(t > -1)
				v = t;
			if (types[i].getSize() == 2) {
				popV();
			}
		}

		Type returnType = Type.getReturnType(desc);
		if (returnType != Type.VOID_TYPE) {
			pushV(v);
			if (returnType.getSize() == 2) {
				pushV(v);
			}
		}

	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		mv.visitJumpInsn(opcode, label);
		switch (opcode) {
		case IFEQ:
		case IFNE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
		case IFNULL:
		case IFNONNULL:
			popV();
			break;

		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
			popV();
			popV();
			break;

		case JSR:
			pushV(peek());
			break;
		}

	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
		mv.visitLookupSwitchInsn(dflt, keys, labels);
		popV();

	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
		mv.visitTableSwitchInsn(min, max, dflt, labels);
		popV();
	}

}
