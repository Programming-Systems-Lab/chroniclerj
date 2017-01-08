package edu.columbia.cs.psl.test.chroniclerj;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ArraysParamITCase {

	@Test
	public void testShuffle() throws Exception {
		Integer[] array = { 7, 5, 9, 3, 6, 0, 2, 4 };

		byte[] ar = new byte[100];
		Arrays.fill(ar, (byte) 5);
		ByteArrayInputStream bis = new ByteArrayInputStream(ar);
		byte b[] = new byte[100];
		bis.read(b, 0, 100);
		bis.close();
		assertEquals(5, b[0]);

	}
	public static void main(String[] args) throws Throwable{
		new ArraysParamITCase().testShuffle();
	}
}
