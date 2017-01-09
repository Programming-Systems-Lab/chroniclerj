
package edu.columbia.cs.psl.chroniclerj.struct;

import java.util.Stack;

public class MethodExpression extends Expression {
    @Override
    public String toString() {
        String r = "";
        if (method.getMethod().getName().equals("<init>"))
            r += "new " + method.getClazz() + "(";
        else
            r += method.getMethod().getName() + "(";

        for (int j = 0; j < params.size() - (method.getMethod().getName().equals("<init>") ? 2 : 0); j++) {
            Expression i = params.get(j);
            Expression parent = i.getParent();
            String paramParent = "";
            while (parent != null) {
                paramParent = parent.toString() + "." + paramParent;
                parent = parent.getParent();
            }
            r += paramParent;
            r += i.toString();
            if (j != params.size() - 1 - (method.getMethod().getName().equals("<init>") ? 2 : 0))
                r += ",";
        }
        r += ")";
        return r;

    }

    private Stack<Expression> params = new Stack<Expression>();

    private AnnotatedMethod method;

    private int opcode;

    public MethodExpression(AnnotatedMethod method, int opcode) {

        this.method = method;
        this.opcode = opcode;
    }

    @Override
    public int getOpcode() {
        return opcode;
    }

    @Override
    public int getType() {
        return METHOD_TYPE;
    }

    public Stack<Expression> getParams() {
        return params;
    }

    public AnnotatedMethod getMethod() {
        return method;
    }

    public int getNumParamsNeeded() {
        return (method.getMethod().getName().equals("<init>") ? 2 : 0)
                + method.getMethod().getArgumentTypes().length;
    }

    public boolean hasAllParameters() {
        // TODO Auto-generated method stub
        return getNumParamsNeeded() == params.size();
    }

    @Override
    public int getStackElementsToSkip() {
        return (method.getMethod().getName().equals("<init>") ? 1 : 0)
                + method.getMethod().getArgumentTypes().length;
    }
}
