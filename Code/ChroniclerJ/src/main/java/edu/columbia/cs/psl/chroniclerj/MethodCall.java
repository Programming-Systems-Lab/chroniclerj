
package edu.columbia.cs.psl.chroniclerj;

import java.util.HashSet;

import javax.swing.JFrame;

import org.objectweb.asm.Type;

public class MethodCall {
    private String sourceMethodName;

    private String sourceMethodDesc;

    private String sourceClass;

    private int pc;

    private int lineNumber;

    private String methodOwner;

    private String methodName;

    private String methodDesc;

    private boolean isStatic;

    private static HashSet<String> serializableClasses = new HashSet<String>();
    static {
        serializableClasses.add(Type.getType(String.class).getInternalName());
        serializableClasses.add(Type.getType(JFrame.class).getInternalName());

    }

    public MethodCall(String sourceMethodName, String sourceMethodDesc, String sourceClass, int pc,
            int lineNumber, String methodOwner, String methodName, String methodDesc,
            boolean isStatic) {
        this.sourceMethodName = sourceMethodName;
        this.sourceMethodDesc = sourceMethodDesc;
        this.sourceClass = sourceClass;
        this.pc = pc;
        this.lineNumber = lineNumber;
        this.methodOwner = methodOwner;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getSourceMethodName() {
        return sourceMethodName;
    }

    public String getSourceMethodDesc() {
        return sourceMethodDesc;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public int getPc() {
        return pc;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMethodOwner() {
        return methodOwner;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + lineNumber;
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result + pc;
        result = prime * result + ((sourceClass == null) ? 0 : sourceClass.hashCode());
        result = prime * result + ((sourceMethodName == null) ? 0 : sourceMethodName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MethodCall other = (MethodCall) obj;
        if (other.getLogFieldName().equals(this.getLogFieldName())
                && other.sourceClass.equals(this.sourceClass))
            return true;
        return false;
    }

    public static String getLogClassName(Type t) {
        if ((t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) || // primitives
                (t.getSort() == Type.OBJECT && serializableClasses.contains(t.getInternalName()))
                || // serializble
                (t.getSort() == Type.ARRAY && ((t.getElementType().getSort() != Type.OBJECT && t
                        .getElementType().getSort() != Type.ARRAY) || serializableClasses
                        .contains(t.getElementType().getInternalName())))) // array
                                                                           // of
                                                                           // prims
                                                                           // or
                                                                           // array
                                                                           // of
                                                                           // serializable
            return Type.getInternalName(SerializableLog.class);
        else
            return Type.getInternalName(Log.class);
    }

    public static String getReplayClassName(Type t) {
        if (getLogClassName(t).equals(Type.getInternalName(SerializableLog.class)))
            return Type.getInternalName(ExportedSerializableLog.class);
        else
            return Type.getInternalName(ExportedLog.class);
    }

    public String getReplayClassName() {
        return getReplayClassName(Type.getReturnType(methodDesc));
    }

    public String getLogClassName() {
        return getLogClassName(Type.getReturnType(methodDesc));
    }

    public String getCapturePrefix() {
        String r = sourceMethodName.replace("<", "___").replace(">", "___") + "$$$$"
                + methodName.replace("<", "___").replace(">", "___") + "$$$$";
        r += lineNumber + "$" + pc;
        return r;
    }

    public String getLogFieldName() {
        // Type[] args = Type.getArgumentTypes(methodDesc);
        // String r = sourceMethodName.replace("<", "___").replace(">",
        // "___")+"$$$$"+methodName+"$$$$";
        // for(Type t : args)
        // {
        // r+=t.getInternalName().replace("/", "$")+"$$";
        // }
        // r += lineNumber+ "$"+pc;
        Type t = Type.getReturnType(methodDesc);
        if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY || methodName.equals("<init>"))
            return "aLog";
        else if (t.getSort() == Type.VOID)
            return "bLog";
        else
            return t.getDescriptor().toLowerCase() + "Log";
    }

    public Type getReturnType() {
        return Type.getMethodType(methodDesc).getReturnType();
    }

    public Type getLogFieldType() {
        Type t = Type.getMethodType(methodDesc).getReturnType();
        if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY)
            return Type.getType("[Ljava/lang/Object;");
        else if (t.getSort() == Type.VOID)
            return Type.getType("[" + Type.BYTE);
        else
            return Type.getType("[" + t.getDescriptor());
        // return
        // Type.getType("["+Type.getMethodType(methodDesc).getReturnType().getDescriptor());
    }

    public static String getLogFieldName(Type t) {
        if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY)
            return "aLog";
        else if (t.getSort() == Type.VOID)
            return "bLog";
        else
            return t.getDescriptor().toLowerCase() + "Log";
    }

    public static Type getLogFieldType(Type t) {
        if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY)
            return Type.getType("[Ljava/lang/Object;");
        else if (t.getSort() == Type.VOID)
            return Type.getType("[" + Type.BYTE);
        else
            return Type.getType("[" + t.getDescriptor());
    }
}
