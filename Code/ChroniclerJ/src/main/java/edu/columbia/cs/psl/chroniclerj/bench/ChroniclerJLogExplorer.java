
package edu.columbia.cs.psl.chroniclerj.bench;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

import edu.columbia.cs.psl.chroniclerj.ExportedSerializableLog;
import edu.columbia.cs.psl.chroniclerj.replay.ReplayRunner;

public class ChroniclerJLogExplorer {
    @SuppressWarnings({
            "resource", "unused"
    })
    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        if (!f.exists())
            System.err.println("No such file");
        String[] cp = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        String[] cp2 = new String[cp.length+1];
        cp2[0] = f.getAbsolutePath();
        System.arraycopy(cp, 0, cp2, 1, cp.length);
        ReplayRunner.setupLogs(cp2);
        System.out.println("Log of longs:");
        for(int i = 0; i < ExportedSerializableLog.jLog_fill; i++)
        {
        	System.out.println("Entry " + i + " value: " + ExportedSerializableLog.jLog[i] + ", debug:" + ExportedSerializableLog.jLog_debug[i]);
        }
   }
}
