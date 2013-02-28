
package edu.columbia.cs.psl.chroniclerj;

public class ChroniclerJUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            System.err.println("ChroniclerJ caught an exception");
            e.printStackTrace();
            System.err.println("Writing log");
            ChroniclerJExportRunner.genTestCase();
        } catch (Exception exi) {
            exi.printStackTrace();
        }
    }

}
