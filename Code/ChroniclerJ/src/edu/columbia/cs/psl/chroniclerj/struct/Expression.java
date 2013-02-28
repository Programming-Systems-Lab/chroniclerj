
package edu.columbia.cs.psl.chroniclerj.struct;

public abstract class Expression {
    public abstract int getType();

    public static int FIELD_TYPE = 1;

    public static int METHOD_TYPE = 2;

    public static int CONSTANT_TYPE = 3;

    public Expression getParent() {
        return parent;
    }

    public void setParent(Expression ir) {
        this.parent = ir;
    }

    public Expression getRootParent() {
        if (getParent() == null)
            return this;
        else
            return getParent().getRootParent();
    }

    private Expression parent;

    public abstract int getStackElementsToSkip();

    public abstract int getOpcode();

    public String printParents() {
        String r = "";
        if (getParent() != null)
            r += getParent().printParents() + ".";
        r += toString();
        return r;
    }
}
