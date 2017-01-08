package edu.columbia.cs.psl.test.chroniclerj;

import org.junit.Test;

public class RandomITCase {

	@Test
	public void testTwo() throws Exception {
		System.err.println("Test two ran??");
		System.out.println("Two");
	}

	@Test
	public void testRandom() throws Exception {
		System.out.println(Math.random());
	}
}
