
package edu.columbia.cs.psl.chroniclerj.struct;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;

public class SimpleExpression extends Expression {
    private AbstractInsnNode insn;

    @Override
    public int getOpcode() {
        return insn.getOpcode();
    }

    public SimpleExpression(AbstractInsnNode insn) {
        this.insn = insn;
    }

    public AbstractInsnNode getInsn() {
        return insn;
    }

    @Override
    public Expression getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setParent(Expression ir) {

    }

    @Override
    public int getType() {
        return CONSTANT_TYPE;
    }

    @Override
    public int getStackElementsToSkip() {
        return 0;
    }

    public String getDesc() {
        switch (insn.getType()) {
            case AbstractInsnNode.TYPE_INSN:
                return ((TypeInsnNode) insn).desc;
            case AbstractInsnNode.VAR_INSN:
                return "" + ((VarInsnNode) insn).var;
            case AbstractInsnNode.LDC_INSN:
                return ((LdcInsnNode) insn).cst.toString();
            default:
                return "";
        }

    }

    @Override
    public String toString() {
        return "[" + Printer.OPCODES[getOpcode()] + " " + getDesc() + "]";
    }

}
