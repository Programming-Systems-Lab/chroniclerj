
package edu.columbia.cs.psl.chroniclerj.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;

public class MainLoggingMethodVisitor extends AdviceAdapter {

    private String className;

    protected MainLoggingMethodVisitor(int api, MethodVisitor mv, int access, String name,
            String desc, String className) {
        super(api, mv, access, name, desc);
        this.className = className;
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        visitLdcInsn(this.className);
        loadArg(0);
        super.invokeStatic(Type.getType(ChroniclerJExportRunner.class),
                Method.getMethod("void logMain(String, String[])"));
    }
}
