package edu.columbia.cs.psl.test.chroniclerj;

import javax.management.MBeanServerFactory;

import org.junit.Test;

public class CreateMBeanITCase {
	@Test
	public void testCallCreateMBeanDoesntCrashInstrumentation() throws Exception {
		if(1!=1)
			MBeanServerFactory.createMBeanServer().createMBean(null, null, null, null, null);
	}
}
