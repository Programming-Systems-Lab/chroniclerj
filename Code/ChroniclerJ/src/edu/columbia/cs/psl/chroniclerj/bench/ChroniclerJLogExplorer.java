
package edu.columbia.cs.psl.chroniclerj.bench;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

import edu.columbia.cs.psl.chroniclerj.ExportedSerializableLog;

public class ChroniclerJLogExplorer {
    @SuppressWarnings({
            "resource", "unused"
    })
    public static void main(String[] args) throws Exception {
        File f = new File("../chroniclerj-test/inst-bin/chroniclerj_serializable_1362003992533.log");
        if (!f.exists())
            System.err.println("No such fiel");
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
        double[] dlog = ExportedSerializableLog.dLog;
        String[] ownersD = ExportedSerializableLog.dLog_owners;
        System.out.println(Arrays.toString(dlog));
        System.out.println(Arrays.toString(ownersD));
    }
}
