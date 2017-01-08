package edu.columbia.cs.psl.test.chroniclerj;

import org.junit.Test;

public class RandomITCase {

	public static void main(String[] args) {
		System.out.println(Math.random());
	}
	
	@Test
	public void testRandom() throws Exception {
		System.out.println(Math.random());
		throw new NullPointerException();

	}
}
