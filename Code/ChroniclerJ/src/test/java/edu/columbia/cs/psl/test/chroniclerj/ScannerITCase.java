package edu.columbia.cs.psl.test.chroniclerj;

import java.util.List;
import java.util.Scanner;

public class ScannerITCase {
	static List<Integer> list;

	
	public static void main(String[] args) {

		Scanner in = new Scanner(System.in);
		int num = in.nextInt();

		System.out.println(num);
		list.add(0);

	}
}
