package edu.cs.columbia.cs.psl.chronicler;
import java.io.OutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import edu.columbia.cs.psl.chroniclerj.ChroniclerJExportRunner;
public class ChroniclerJUnitListener implements JUnitResultFormatter {

	@Override
	public void addError(Test arg0, Throwable arg1) {
		
	}

	@Override
	public void addFailure(Test arg0, AssertionFailedError arg1) {
		
	}

	@Override
	public void endTest(Test arg0) {
		ChroniclerJExportRunner.genTestCase("chroniclerj_"+arg0.toString()+".test");
	}

	@Override
	public void startTest(Test arg0) {
		
	}

	@Override
	public void endTestSuite(JUnitTest arg0) throws BuildException {
		
	}

	@Override
	public void setOutput(OutputStream arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSystemError(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSystemOutput(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startTestSuite(JUnitTest arg0) throws BuildException {
		// TODO Auto-generated method stub
		
	}

}
