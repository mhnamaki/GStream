package src.simpleTests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.github.jamm.MemoryMeter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import src.base.IPrefixTreeNodeData;
import src.utilities.PrefixTreeNode;
import src.utilities.SupportComparator;

import com.google.caliper.*;
import com.google.caliper.memory.ObjectGraphMeasurer;

import java.lang.instrument.Instrumentation;

public class DameDasti2 {
	public static void main(String[] args) {

		DameDasti2 d = new DameDasti2();
		// d.test();
		// d.test2();
		// d.test3();
		// d.test5();
		// d.test6();
		// d.test7();
		// d.test8();
		
		d.test9();
		//d.test10();
		//d.test11();

		// for(double i = 0; i < 1.0; i += 0.1)
		// System.out.println(i);

		// boolean printing = true;
		// print(getValue(), printing);

	}

//	private void test11() {
//		MinMaxPriorityQueue<DoubleClass> topKFrequentPatterns = MinMaxPriorityQueue.orderedBy(new SupportComparator()).maximumSize(k).create();
//		
//	}

	private void test10() {
		MemoryMeter meter = new MemoryMeter();
	    
		Set<Integer> hashset = new HashSet<Integer>();
		for (int i = 1; i <= 100; i++) {
			hashset.add(i);
		}
		
		meter.measure(hashset);
	    meter.measureDeep(hashset);
	    meter.countChildren(hashset);
		
	}

	private void test9() {
		Random rng = new Random();

		Set<Integer> hashset = new HashSet<Integer>();
		for (int i = 1; i <= 100; i++) {
			hashset.add(i);
		}
		
		
		System.out.println(ObjectGraphMeasurer.measure(hashset));
		
		Set<Integer> hashset2 = new HashSet<Integer>();
		for (int i = 1; i <= 1000; i++) {
			hashset2.add(i);
		}
		System.out.println(ObjectGraphMeasurer.measure(hashset2));
		
		Set<Integer> hashset3 = new HashSet<Integer>();
		for (int i = 1; i <= 10000; i++) {
			hashset3.add(i);
		}
		System.out.println(ObjectGraphMeasurer.measure(hashset3));

	}

	private void test8() {
		HashSet<Integer> set1 = new HashSet<Integer>();
		set1.add(1);
		set1.add(2);
		set1.add(3);
		set1.add(4);
		HashSet<Integer> set2 = new HashSet<Integer>();
		set2.add(1);
		set2.add(2);
		SetView<Integer> result = Sets.symmetricDifference(set1, set2);

		System.out.println(result);

	}

	private void test7() {
		BiMap<String, Integer> dialectConverterForWisconsinites = HashBiMap.create();

		dialectConverterForWisconsinites.put("bratwurst", 1);
		dialectConverterForWisconsinites.put("drinking fountain", 1);
		dialectConverterForWisconsinites.put("that", 2);
		dialectConverterForWisconsinites.put("alright", 3);
		dialectConverterForWisconsinites.put("soda", 4);

		BiMap<Integer, String> dialectConverterForEveryoneElse = dialectConverterForWisconsinites.inverse();

		System.out.println(dialectConverterForWisconsinites);
		System.out.println(dialectConverterForEveryoneElse);

	}

	private void test6() {
		HashSet<Integer> newTest = new HashSet<Integer>();
		newTest.add(2);
		newTest.add(0);
		newTest.add(4);
		newTest.add(1);

		for (Integer testi : newTest) {
			newTest.add(testi + 1);
			System.out.println(newTest);
		}

	}

	private void test5() {
		ArrayList<Double> twoDOfArrayLists = new ArrayList<Double>();
		twoDOfArrayLists.add(2.0);
		twoDOfArrayLists.add(2.0);

		twoDOfArrayLists.remove(2.1);
		twoDOfArrayLists.remove(2.0);

		System.out.println(twoDOfArrayLists.size());

	}

	private void test3() {

		int i = 0;

		if (i == 0 || (i = test4()) > 5) {
			System.out.println(i);
		}

	}

	private int test4() {
		return 6;

	}

	private void test2() {
		PairVal pairVal = new PairVal(0, 0);

		method1(pairVal);
		System.out.println("pairVal.key:" + pairVal.key);

		method2(pairVal);
		System.out.println("pairVal.value:" + pairVal.value);
	}

	private void method2(PairVal pairVal) {
		pairVal.value = 2;
	}

	private void method1(PairVal pairVal) {

		pairVal.key = 1;
	}

	private void test() {
		Queue<PairVal> testQ = new LinkedList<>();

		testQ.add(null);

		testQ.add(new PairVal(0, 0));

		testQ.add(new PairVal(0, 2));

		testQ.add(new PairVal(3, 2));

		testQ.add(null);

		System.out.println(testQ.size());
		while (testQ.contains(null)) {
			testQ.remove(null);
		}

		System.out.println(testQ.size());

		System.out.println("finish!");

	}

	private static int getValue() {
		return 2 + 3 * 8;
	}

	private static void print(int i, boolean printing) {
		if (printing) {
			System.out.println(i);
		}

	}

	class PairVal {
		public int key;
		public int value;

		public PairVal(int key, int value) {
			this.key = key;
			this.value = value;
		}
	}

	

}
