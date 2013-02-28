
package edu.columbia.cs.psl.chroniclerj;

import edu.columbia.cs.psl.chroniclerj.replay.ReplayRunner;
import edu.columbia.cs.psl.chroniclerj.replay.Replayer;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err
                    .println("Usage: java -jar chroniclerj.jar -instrument [source] [dest-deploy] [dest-replay] {additional-classpath-entries}");
            System.err.println("or");
            System.err.println("Usage: java -jar chroniclerj.jar -replay [testcase]");
            System.exit(-1);
        }
        if (args[0].equals("-instrument")) {
            if (args.length < 4) {
                System.err
                        .println("Usage: java -jar chroniclerj.jar -instrument [source] [dest-deploy] [dest-replay] {additional-classpath-entries}");
                System.exit(-1);
            }
            String[] instrumenterArgs = new String[args.length - 1];
            instrumenterArgs[0] = args[1];
            instrumenterArgs[1] = args[2];
            instrumenterArgs[2] = args[1];
            for (int i = 4; i < args.length; i++) {
                instrumenterArgs[i] = args[i];
            }
            Instrumenter._main(instrumenterArgs);
            // TODO uncomment for release
            Replayer._main(new String[] {
                    args[1], args[3]
            });
        } else if (args[0].equals("-replay")) {
            if (args.length < 2) {
                System.err
                        .println("Usage: java -jar chroniclerj.jar -replay [testcase] {classpath}");
                System.exit(-1);
            }
            String[] classpath = new String[args.length];
            classpath[0] = System.getProperty("user.dir");
            classpath[1] = args[1];
            for (int i = 2; i < args.length; i++) {
                classpath[i] = args[i];
            }
            ReplayRunner._main(classpath);
        } else {
            System.err
                    .println("Usage: java -jar chroniclerj.jar -instrument [source] [dest-deploy] [dest-replay] {additional-classpath-entries}");
            System.err.println("or");
            System.err.println("Usage: java -jar chroniclerj.jar -replay [testcase]");
            System.exit(-1);
        }
    }
}
