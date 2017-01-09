package edu.columbia.cs.psl.test.chroniclerj;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;

import org.junit.Test;

public class ArraysParamITCase {

	@Test
	public void testReadFromInput() throws Exception {
		byte[] ar = new byte[100];
		Arrays.fill(ar, (byte) 5);
		ByteArrayInputStream bis = new ByteArrayInputStream(ar);
		DataInputStream dis = new DataInputStream(bis);
		byte b[] = new byte[100];
		dis.read(b, 0, 100);
		bis.close();
		assertEquals(5, b[0]);
	}

}
