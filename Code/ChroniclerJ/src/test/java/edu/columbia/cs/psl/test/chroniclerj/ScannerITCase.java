package edu.columbia.cs.psl.test.chroniclerj;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import org.junit.Test;

public class ScannerITCase {

	@Test
	public void testScannerReadInt() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf((byte)(Math.random() * 20)));
		sb.append('\n');
		Scanner in = new Scanner(new ByteArrayInputStream(sb.toString().getBytes()));
		int num = in.nextByte();
		in.close();
		System.out.println(num);
	}

}
