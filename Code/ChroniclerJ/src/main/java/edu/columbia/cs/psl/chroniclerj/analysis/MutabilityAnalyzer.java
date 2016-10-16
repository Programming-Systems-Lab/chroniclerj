
package edu.columbia.cs.psl.chroniclerj.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.chroniclerj.struct.AnnotatedMethod;
import edu.columbia.cs.psl.chroniclerj.struct.Expression;
import edu.columbia.cs.psl.chroniclerj.struct.FieldExpression;
import edu.columbia.cs.psl.chroniclerj.struct.MethodExpression;
import edu.columbia.cs.psl.chroniclerj.struct.SimpleExpression;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingMethodVisitor;

public class MutabilityAnalyzer implements Opcodes {

    private static Logger logger = Logger.getLogger(MutabilityAnalyzer.class);

    public MutabilityAnalyzer(HashMap<String, AnnotatedMethod> lookupCache) {
        this.lookupCache = lookupCache;
    }

    private HashMap<String, AnnotatedMethod> lookupCache;

    private static boolean enabled = false;

    /**
     * Call when done calling analyzeClass
     */
    public void doneSupplyingClasses() {
        if (!enabled)
            return;
        for (String s : lookupCache.keySet()) {
            AnnotatedMethod method = lookupCache.get(s);
            if (method.isMutatesFieldsDirectly()) {
                for (AnnotatedMethod caller : method.functionsThatCallMe) {
                    method.setMutatesFields();
                    addAllRecursively(caller);
                }
            }
        }
    }

    private void addAllRecursively(AnnotatedMethod method) {
        if (method.isMutatesFields())
            return;
        method.setMutatesFields();
        for (AnnotatedMethod caller : method.functionsThatCallMe)
            addAllRecursively(caller);
    }

    /**
     * Approach: Find all methods that can change fields Recurse that out to
     * find all methods that might call methods that can change fields At the
     * start of each methods that might change fields, keep track in a field:
     * The starting count values for each storage array *recursively* this can
     * be in local variables though When a field is changed OR a "not safe"
     * local variable (not safe if it might point to a field): Store a reference
     * to the field/local variable and a copy of the value When a method is
     * invoked that might change fields, track: A reference to the callee When
     * we need to reset the system to a pre-crash state: Go through the starting
     * count for each field and compare with each current count. If count
     * changed, reset and note To find each field, may need to (recursively)
     * descend through the holder points-to fields Take 2: At the start of each
     * method, make a reference copy to a local variable of every backup pt that
     * we will subsequently read but might change If this method directly
     * changes fields, store a local variable with the original value at time of
     * change If this method indirectly changes fields, store a local variable
     * with the original value before the method is called
     * 
     * @param cr
     */
    public ClassNode analyzeClass(ClassReader cr) {
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG);

        System.out.println("Analyze " + cn.name);
        for (Object o : cn.methods) {
            MethodNode thisMethodNode = (MethodNode) o;
            AnnotatedMethod thisMethod = findOrAddMethod(cn.name, thisMethodNode);
            thisMethod.setFullyDiscovered(true);
            thisMethod.setAccess(thisMethodNode.access);

            if ((thisMethodNode.access & ACC_NATIVE) != 0) // is native
                NonDeterministicLoggingMethodVisitor.registerNDMethod(cn.name, thisMethodNode.name,
                        thisMethodNode.desc);
//
//            ListIterator<?> i = thisMethodNode.instructions.iterator();
//            boolean isFirstInsn = true;
//            while (i.hasNext()) {
//                AbstractInsnNode n = (AbstractInsnNode) i.next();
//                System.out.println(n + " " + Printer.OPCODES[n.getOpcode()]);
//                if (n.getType() == AbstractInsnNode.FIELD_INSN) // Field
//                                                                // Instruction
//                {
//                    FieldInsnNode fn = (FieldInsnNode) n;
//                    if (n.getOpcode() == Opcodes.PUTSTATIC) {
//                        // This instruction is changing a static field. Previous
//                        // instruction is the value to set to
//                        FieldExpression pi = new FieldExpression(fn.name, fn.owner, fn.desc,
//                                n.getOpcode());
//                        thisMethod.getPutFieldInsns().add(pi);
//                        thisMethod.setMutatesFieldsDirectly();
//                    } else if (n.getOpcode() == Opcodes.PUTFIELD) {
//
//                        // This instruction is changing a field.
//                        // Previous instruction will have the value that we are
//                        // setting to
//                        FieldExpression pi = new FieldExpression(fn.name, fn.owner, fn.desc,
//                                n.getOpcode());
//                        pi.setParent(parentInstructionOf(thisMethodNode, pi,
//                                thisMethodNode.instructions.iterator(i.previousIndex())));
//                        thisMethod.getPutFieldInsns().add(pi);
//                        thisMethod.setMutatesFieldsDirectly();
//                    }
//                } else if (n.getType() == AbstractInsnNode.METHOD_INSN) // Method
//                                                                        // invocation
//                {
//                    MethodInsnNode whatWeCall = (MethodInsnNode) n;
//                    AnnotatedMethod otherMethod = findOrAddMethod(whatWeCall.owner,
//                            whatWeCall.name, whatWeCall.desc, 0);
//                    otherMethod.functionsThatCallMe.add(thisMethod);
//                    MethodExpression otherMethodExp = new MethodExpression(otherMethod,
//                            whatWeCall.getOpcode());
//                    otherMethodExp.getParams().addAll(
//                            paramsOf(thisMethodNode, otherMethodExp,
//                                    thisMethodNode.instructions.iterator(i.previousIndex())));
//
//                    if (whatWeCall.getOpcode() != Opcodes.INVOKESTATIC)
//                        otherMethodExp.setParent(parentInstructionOf(thisMethodNode,
//                                otherMethodExp,
//                                thisMethodNode.instructions.iterator(i.previousIndex())));
//
//                    if (NonDeterministicLoggingMethodVisitor.isND(whatWeCall.owner,
//                            whatWeCall.name, whatWeCall.desc)
//                            && whatWeCall.name.equals("<init>")
//                            && whatWeCall.owner.equals(cn.superName)
//                            && thisMethodNode.name.equals("<init>") && isFirstInsn) {
//                        NonDeterministicLoggingMethodVisitor.registerNDMethod(cn.name,
//                                thisMethodNode.name, thisMethodNode.desc);
//                    }
//
//                    thisMethod.functionsThatICall.add(otherMethodExp);
//
//                    isFirstInsn = false;
//                } else if (n.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) // Invoke
//                                                                                // dynamic
//                {
//
//                }
//            }
        }
        ClassNode ret = new ClassNode();
        ret.name = cn.name;
        ret.fields = cn.fields;
        ret.superName = cn.superName;
        ret.interfaces = cn.interfaces;
        return ret;
    }

    private List<Expression> paramsOf(MethodNode sourceMethod,
            MethodExpression methodInsnToFindParamsOf, ListIterator<?> i) {
        ArrayList<Expression> ret = new ArrayList<Expression>();
        int nParams = methodInsnToFindParamsOf.getNumParamsNeeded();
        int nToSkip = 0;
        logger.debug("Finding " + nParams + " params for " + methodInsnToFindParamsOf);
        while (i.hasPrevious() && nParams > 0) {
            AbstractInsnNode n = (AbstractInsnNode) i.previous();
            switch (n.getType()) {
                case AbstractInsnNode.METHOD_INSN:
                    MethodInsnNode min = (MethodInsnNode) n;
                    MethodExpression mi = new MethodExpression(findOrAddMethod(min.owner, min.name,
                            min.desc, min.getOpcode() == Opcodes.INVOKESTATIC ? Opcodes.ACC_STATIC
                                    : 0), min.getOpcode());
                    logger.debug("Encountered " + mi + ", skipping " + mi.getStackElementsToSkip());

                    if (nToSkip == 0) {
                        mi.setParent(parentInstructionOf(sourceMethod, mi,
                                sourceMethod.instructions.iterator(i.previousIndex() + 1)));
                        mi.getParams().addAll(
                                paramsOf(sourceMethod, mi,
                                        sourceMethod.instructions.iterator(i.previousIndex() + 1)));

                        ret.add(mi);
                        nParams--;
                    }
                    nToSkip--;
                    nToSkip += mi.getStackElementsToSkip()
                            + (mi.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1);
                    logger.debug("NTOS" + nToSkip);
                    if (nToSkip < 0)
                        nToSkip = 0;
                    break;
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                    // TODO
                    break;
                case AbstractInsnNode.TYPE_INSN:
                    switch (n.getOpcode()) {
                        case NEW:
                            if (nToSkip == 0) {
                                ret.add(new SimpleExpression(n));
                                nParams--;
                            } else
                                nToSkip--;
                            break;
                        case ANEWARRAY:
                        case CHECKCAST:
                        case INSTANCEOF:
                    }
                    break;
                case AbstractInsnNode.FIELD_INSN:
                    FieldInsnNode fn = (FieldInsnNode) n;
                    if (n.getOpcode() == Opcodes.GETFIELD) {
                        FieldExpression fi = new FieldExpression(fn.name, fn.owner, fn.desc,
                                n.getOpcode());
                        logger.debug("Encoutnered" + fi);
                        if (nToSkip == 0) {
                            fi.setParent(parentInstructionOf(sourceMethod, fi,
                                    sourceMethod.instructions.iterator(i.previousIndex() + 1)));
                            ret.add(fi);
                            nParams--;
                        }
                        nToSkip--;
                        nToSkip += 1;
                    } else if (n.getOpcode() == Opcodes.GETSTATIC) {
                        FieldExpression fi = new FieldExpression(fn.name, fn.owner, fn.desc,
                                n.getOpcode());
                        if (nToSkip == 0) {
                            ret.add(fi);
                            nParams--;
                        }
                        nToSkip--;
                    }
                    break;
                case AbstractInsnNode.INT_INSN:
                case AbstractInsnNode.LDC_INSN:
                case AbstractInsnNode.VAR_INSN:
                    switch (n.getOpcode()) {
                        case Opcodes.ILOAD:
                        case Opcodes.LLOAD:
                        case Opcodes.FLOAD:
                        case Opcodes.DLOAD:
                        case Opcodes.ALOAD:
                        case BIPUSH:
                        case SIPUSH:
                        case Opcodes.LDC:
                            if (nToSkip == 0) {
                                ret.add(new SimpleExpression(n));
                                nParams--;
                            } else
                                nToSkip--;
                            break;
                        case ISTORE:
                        case LSTORE:
                        case FSTORE:
                        case DSTORE:
                        case ASTORE:
                            nToSkip--;
                            nToSkip++;
                            break;
                        case LALOAD:
                        case FALOAD:
                        case AALOAD:
                        case BALOAD:
                        case CALOAD:
                        case SALOAD:
                            nToSkip--;
                            nToSkip += 2;
                            break;
                        case AASTORE:
                        case IASTORE:
                        case FASTORE:
                        case DASTORE:
                        case BASTORE:
                        case CASTORE:
                        case SASTORE:
                            nToSkip += 3;
                            break;
                        case NEWARRAY:
                            nToSkip--;
                            nToSkip++;
                        default:
                            logger.debug("Unknown opcode " + n.getOpcode());
                            break;
                    }
                    break;
                case AbstractInsnNode.INSN:
                    switch (n.getOpcode()) {
                    // case ATHROW: // 1 before n/a after
                    // popValue();
                    // onMethodExit(opcode);
                    // break;
                    //
                    // case LRETURN: // 2 before n/a after
                    // case DRETURN: // 2 before n/a after
                    // popValue();
                    // popValue();
                    // onMethodExit(opcode);
                    // break;

                        case NOP:
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
                        case F2L: // 1 before 2 after
                        case F2D:
                        case I2L:
                        case I2D:

                        case L2I: // 2 before 1 after
                        case L2F: // 2 before 1 after
                        case D2I: // 2 before 1 after
                        case D2F: // 2 before 1 after
                        case ARRAYLENGTH:
                        case SWAP:
                            nToSkip--;
                            nToSkip++;
                            break;

                        case IADD:
                        case FADD:
                        case ISUB:
                        case LSHL: // 3 before 2 after
                        case LSHR: // 3 before 2 after
                        case LUSHR: // 3 before 2 after
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

                        case IALOAD: // remove 2 add 1
                        case FALOAD: // remove 2 add 1
                        case AALOAD: // remove 2 add 1
                        case BALOAD: // remove 2 add 1
                        case CALOAD: // remove 2 add 1
                        case SALOAD: // remove 2 add 1
                        case LALOAD: // remove 2 add 2
                        case DALOAD: // remove 2 add 2

                        case LCMP: // 4 before 1 after
                        case DCMPL:
                        case DCMPG:
                            nToSkip--;
                            nToSkip += 2;
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
                        case LCONST_0:
                        case LCONST_1:
                        case DCONST_0:
                        case DCONST_1:

                        case DUP:
                        case DUP_X1:
                        case DUP_X2:

                        case DUP2: // is this wrong to assume that dup2 is only
                                   // used on
                                   // longs and not 2 shorts?
                        case DUP2_X1:
                        case DUP2_X2:
                            // case POP:
                            // case MONITORENTER:
                            // case MONITOREXIT:
                            // case POP2:
                            if (nToSkip == 0) {
                                ret.add(new SimpleExpression(n));
                                nParams--;
                            } else
                                nToSkip--;
                            break;

                        case LASTORE:
                        case DASTORE:
                        case IASTORE:
                        case FASTORE:
                        case AASTORE:
                        case BASTORE:
                        case CASTORE:
                        case SASTORE:
                            nToSkip--;
                            nToSkip += 3;
                            break;
                    }
                    break;
            }
        }
        return ret;
    }

    private Expression parentInstructionOf(MethodNode mn, Expression insnToFindParentOf,
            ListIterator<?> i) {
        if (insnToFindParentOf.getType() == Expression.METHOD_TYPE
                && ((MethodExpression) insnToFindParentOf).getMethod().getName().equals("<init>"))
            return null;
        int nToSkip = insnToFindParentOf.getStackElementsToSkip();
        logger.debug("Examining " + insnToFindParentOf.toString() + " for parent, skipping "
                + nToSkip);
        while (i.hasPrevious()) {
            AbstractInsnNode n = (AbstractInsnNode) i.previous();
            switch (n.getType()) {
                case AbstractInsnNode.METHOD_INSN:
                    MethodInsnNode min = (MethodInsnNode) n;
                    MethodExpression mi = new MethodExpression(findOrAddMethod(min.owner, min.name,
                            min.desc, min.getOpcode() == Opcodes.INVOKESTATIC ? Opcodes.ACC_STATIC
                                    : 0), min.getOpcode());
                    logger.debug("Encountered " + mi + ", skipping " + mi.getStackElementsToSkip());

                    if (nToSkip == 0) {
                        mi.setParent(parentInstructionOf(mn, mi,
                                mn.instructions.iterator(i.previousIndex() + 1)));
                        mi.getParams().addAll(
                                paramsOf(mn, mi, mn.instructions.iterator(i.previousIndex() + 1)));
                        return mi;
                    }
                    nToSkip--;
                    nToSkip += mi.getStackElementsToSkip()
                            + (mi.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1);
                    logger.debug("NTos" + nToSkip);
                    break;
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                    // TODO
                    break;
                case AbstractInsnNode.TYPE_INSN:
                    switch (n.getOpcode()) {
                        case NEW:
                            if (nToSkip == 0) {
                                return new SimpleExpression(n);
                            } else
                                nToSkip--;
                            break;
                        case ANEWARRAY:
                        case CHECKCAST:
                        case INSTANCEOF:
                    }
                    break;
                case AbstractInsnNode.FIELD_INSN:
                    FieldInsnNode fn = (FieldInsnNode) n;
                    if (n.getOpcode() == Opcodes.GETFIELD) {
                        FieldExpression fi = new FieldExpression(fn.name, fn.owner, fn.desc,
                                n.getOpcode());
                        logger.debug("Encoutnered field: " + fi);
                        if (nToSkip == 0) {
                            fi.setParent(parentInstructionOf(mn, fi,
                                    mn.instructions.iterator(i.previousIndex() + 1)));
                            return fi;
                        }
                        nToSkip--;
                        nToSkip += 1;
                        logger.debug("Ntos" + nToSkip);
                    } else if (n.getOpcode() == Opcodes.GETSTATIC) {
                        FieldExpression fi = new FieldExpression(fn.name, fn.owner, fn.desc,
                                n.getOpcode());
                        if (nToSkip == 0) {
                            fi.setParent(parentInstructionOf(mn, fi,
                                    mn.instructions.iterator(i.previousIndex() + 1)));
                            return fi;
                        }
                        nToSkip--;
                        // nToSkip += 1;
                    }
                    break;
                case AbstractInsnNode.INT_INSN:
                case AbstractInsnNode.LDC_INSN:
                case AbstractInsnNode.VAR_INSN:
                    switch (n.getOpcode()) {
                        case Opcodes.ILOAD:
                        case Opcodes.LLOAD:
                        case Opcodes.FLOAD:
                        case Opcodes.DLOAD:
                        case Opcodes.ALOAD:
                        case BIPUSH:
                        case SIPUSH:
                        case Opcodes.LDC:
                            if (nToSkip == 0) {
                                return new SimpleExpression(n);
                            }
                            nToSkip--;
                            break;
                        case ISTORE:
                        case LSTORE:
                        case FSTORE:
                        case DSTORE:
                        case ASTORE:
                            nToSkip--;
                            nToSkip++;
                            break;
                        case LALOAD:
                        case FALOAD:
                        case AALOAD:
                        case BALOAD:
                        case CALOAD:
                        case SALOAD:
                            nToSkip--;
                            nToSkip += 2;
                            break;
                        case AASTORE:
                        case IASTORE:
                        case FASTORE:
                        case DASTORE:
                        case BASTORE:
                        case CASTORE:
                        case SASTORE:
                            nToSkip += 3;
                            break;
                        case NEWARRAY:
                            nToSkip--;
                            nToSkip++;
                        default:
                            logger.debug("Unknown opcode " + n.getOpcode());
                            break;
                    }
                    break;
                case AbstractInsnNode.INSN:
                    switch (n.getOpcode()) {
                    // case ATHROW: // 1 before n/a after
                    // popValue();
                    // onMethodExit(opcode);
                    // break;
                    //
                    // case LRETURN: // 2 before n/a after
                    // case DRETURN: // 2 before n/a after
                    // popValue();
                    // popValue();
                    // onMethodExit(opcode);
                    // break;

                        case NOP:
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
                        case F2L: // 1 before 2 after
                        case F2D:
                        case I2L:
                        case I2D:

                        case L2I: // 2 before 1 after
                        case L2F: // 2 before 1 after
                        case D2I: // 2 before 1 after
                        case D2F: // 2 before 1 after
                        case ARRAYLENGTH:
                        case SWAP:
                            nToSkip--;
                            nToSkip++;
                            break;

                        case IADD:
                        case FADD:
                        case ISUB:
                        case LSHL: // 3 before 2 after
                        case LSHR: // 3 before 2 after
                        case LUSHR: // 3 before 2 after
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

                        case IALOAD: // remove 2 add 1
                        case FALOAD: // remove 2 add 1
                        case AALOAD: // remove 2 add 1
                        case BALOAD: // remove 2 add 1
                        case CALOAD: // remove 2 add 1
                        case SALOAD: // remove 2 add 1
                        case LALOAD: // remove 2 add 2
                        case DALOAD: // remove 2 add 2

                        case LCMP: // 4 before 1 after
                        case DCMPL:
                        case DCMPG:
                            nToSkip--;
                            nToSkip += 2;
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
                        case LCONST_0:
                        case LCONST_1:
                        case DCONST_0:
                        case DCONST_1:

                        case DUP:
                        case DUP_X1:
                        case DUP_X2:

                        case DUP2: // is this wrong to assume that dup2 is only
                                   // used on
                                   // longs and not 2 shorts?
                        case DUP2_X1:
                        case DUP2_X2:
                            // case POP:
                            // case MONITORENTER:
                            // case MONITOREXIT:
                            // case POP2:
                            if (nToSkip == 0) {
                                return new SimpleExpression(n);
                            }
                            nToSkip--;
                            break;

                        case LASTORE:
                        case DASTORE:
                        case IASTORE:
                        case FASTORE:
                        case AASTORE:
                        case BASTORE:
                        case CASTORE:
                        case SASTORE:
                            nToSkip--;
                            nToSkip += 3;
                            break;
                    }
                    break;
            }

        }
        return null;
    }

    private AnnotatedMethod findOrAddMethod(String owner, String name, String desc, int access) {
        String lookupKey = owner + "." + name + ":" + desc;
        if (!lookupCache.containsKey(lookupKey))
            lookupCache.put(lookupKey, new AnnotatedMethod(name, desc, owner, access));
        return lookupCache.get(lookupKey);
    }

    private AnnotatedMethod findOrAddMethod(String owner, MethodNode mn) {
        return findOrAddMethod(owner, mn.name, mn.desc, mn.access);
    }

    public static void main(String[] args) {
        try {
            ClassReader cr = new ClassReader("edu.columbia.cs.psl.invivo.sample.SimpleClass");
            MutabilityAnalyzer ma = new MutabilityAnalyzer(new HashMap<String, AnnotatedMethod>());
            ma.analyzeClass(cr);
            ma.doneSupplyingClasses();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
