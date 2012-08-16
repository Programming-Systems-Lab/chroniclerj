package edu.columbia.cs.psl.chroniclerj.bench;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import edu.columbia.cs.psl.chroniclerj.ExportedSerializableLog;

public class ChroniclerJLogExplorer {
	@SuppressWarnings({ "resource", "unused" })
	public static void main(String[] args) throws Exception {
		File f = new File("instrumented-test/wallace_serializable_1345072712853.log");
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
		ExportedSerializableLog log = (ExportedSerializableLog) ois.readObject();
		Object[] alog = ExportedSerializableLog.aLog;
		
		char[] clog = ExportedSerializableLog.cLog;
		byte[] blog = ExportedSerializableLog.bLog;
		String[] ownersA = ExportedSerializableLog.aLog_owners;
		String[] ownersI = ExportedSerializableLog.iLog_owners;
		System.out.println(ExportedSerializableLog.aLog_fill);
		System.out.println(ExportedSerializableLog.cLog_fill);
		System.out.println(ExportedSerializableLog.dLog_fill);
	}
}
