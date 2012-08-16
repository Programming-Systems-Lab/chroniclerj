package edu.columbia.cs.psl.nddetector;

import java.util.HashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class NDClassVisitor extends ClassVisitor {

	private String className;
	private HashMap<String, MethodInstance> methodLookupCache;
	private HashMap<String, ClassInstance> classLookupCache;

	public NDClassVisitor(int api, ClassVisitor cv, HashMap<String, MethodInstance> lookupCache, HashMap<String, ClassInstance> classMap) {
		super(api, cv);
		this.methodLookupCache = lookupCache;
		this.classLookupCache = classMap;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		classLookupCache.put(name, new ClassInstance(name, superName, interfaces));
	}

	/**
	 * We are seeing method A.x for the first time. Add it to methodMap.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodInstance mi = new MethodInstance(name, desc, this.className, access);
		if (methodLookupCache.containsKey(mi.getFullName()))
			methodLookupCache.get(mi.getFullName()).setAccess(access);
		else
			methodLookupCache.put(mi.getFullName(), mi);
		mi = methodLookupCache.get(mi.getFullName());
		if(className.contains("Exception") || className.contains("Throwable"))
		{
			mi.setNonDeterministic(false);
			mi.setAccess(0);
		}
//		else if (( className.startsWith("java/io") || className.startsWith("java/nio") || className.startsWith("java/lang/Readable.")) 
//				&& !className.startsWith("java/io/String"))
//			mi.forceNative();
		else if (NativeDetector.deterministicNativeMethods.contains(mi.getFullName()))
			mi.setAccess(0);
		else if (mi.isNative())
			mi.setNonDeterministic(true);
		return new NDMethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions), mi.getFullName(), mi.getMethod().getName(), mi.getMethod().getDescriptor(), access, methodLookupCache);
	}

}
