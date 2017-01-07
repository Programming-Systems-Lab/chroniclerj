package edu.columbia.cs.psl.test.chroniclerj;

import java.util.Arrays;
import java.util.List;

public class ArraysSortTestCase {

	static List<Integer> list;

	public static void main(String[] args) {
		Integer[] array={7,5,9,3,6,0,2,4};

		System.out.println("before sort: \n" + Arrays.toString(array));
		
		Arrays.sort(array);

		System.out.println("after sort: \n" + Arrays.toString(array));
		
		list.add(0);
	}
}
