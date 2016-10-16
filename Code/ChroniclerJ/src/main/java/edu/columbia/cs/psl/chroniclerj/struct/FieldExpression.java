
package edu.columbia.cs.psl.chroniclerj.struct;

import org.objectweb.asm.Opcodes;

public class FieldExpression extends Expression {
    private String name;

    private String owner;

    private String desc;

    private int opcode;

    public FieldExpression(String name, String owner, String desc, int opcode) {
        this.name = name;
        this.owner = owner;
        this.desc = desc;
        this.opcode = opcode;
    }

    @Override
    public int getOpcode() {
        return opcode;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public int getType() {
        return FIELD_TYPE;
    }

    @Override
    public int getStackElementsToSkip() {
        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC)
            return 0;
        return 1;
    }

    @Override
    public String toString() {
        // return "FieldInvocation [name=" + name + ", owner=" + owner +
        // ", desc=" + desc + "]";
        return name;
    }
}
