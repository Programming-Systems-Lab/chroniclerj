package edu.columbia.cs.psl.nddetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class NDMutateDetector {
	public static HashSet<String> nonDeterministicMethods = new HashSet<String>();
	public static HashMap<String,HashSet<String>> taintedFields =  new HashMap<String, HashSet<String>>();
	public static HashSet<String> taintedMethods = new HashSet<String>();
	public NDMutateDetector(String[] jarPath) {
		JarFile classJar;
		for (String path : jarPath) {
			try {
				classJar = new JarFile(path);

				Enumeration<JarEntry> jarContents = classJar.entries();
				int i = 0;
				while (jarContents.hasMoreElements()) {
					String name = jarContents.nextElement().getName();
					if (!name.endsWith(".class"))
						continue;
					name = name.substring(0, name.length() - 6);

					ClassReader cr = new ClassReader(name);
					NDMutateCV cv=  new NDMutateCV(Opcodes.ASM4);
					cr.accept(cv, 0);
					i++;

				}
				classJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for (String path : jarPath) {
			try {
				classJar = new JarFile(path);

				Enumeration<JarEntry> jarContents = classJar.entries();
				int i = 0;
				while (jarContents.hasMoreElements()) {
					String name = jarContents.nextElement().getName();
					if (!name.endsWith(".class"))
						continue;
					name = name.substring(0, name.length() - 6);

					ClassReader cr = new ClassReader(name);
					NDMutateCV2 cv=  new NDMutateCV2(Opcodes.ASM4);
					cr.accept(cv, 0);
					i++;

				}
				classJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ArrayList<String> bad = new ArrayList<String>(taintedMethods);
		Collections.sort(bad);
		for(String s : bad)
		{
			System.out.println(s);
		}
	}

	public static void main(String[] args) {
		NDMutateDetector detector = new NDMutateDetector(new String[] { "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/classes.jar" });
	}

	static {
		File f = new File("nondeterministic-methods.txt");
		Scanner s;
		try {
			s = new Scanner(f);
			while (s.hasNextLine())
				nonDeterministicMethods.add(s.nextLine());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
