package src.simpleTests;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.google.code.externalsorting.ExternalSort;
import com.google.common.collect.MinMaxPriorityQueue;

public class ManualTester {

	public static void main(String[] args) {

		// for (int i = 0; i < 1000; i++) {
		// HashMap<Long, String> test1 = new HashMap<Long, String>();
		// HashMap<Long, String> test2 = new HashMap<Long, String>();
		//
		// test1.put(5l, "11");
		// test1.put(4l, "11");
		// test1.put(3l, "11");
		// test1.put(2l, "12");
		// test1.put(1l, "11");
		//
		// test2.put(1l, "21");
		// test2.put(2l, "22");
		// test2.put(3l, "33");
		// test2.put(4l, "44");
		// //test2.put(5l, "55");
		//
		// if (test1.keySet().equals(test2.keySet())) {
		//
		// } else {
		// System.out.println("false");
		// }
		// }

		// create two hash sets
		// HashSet <String> newset = new HashSet <String>();
		// HashSet <String> cloneset = new HashSet <String>();
		//
		// // populate hash set
		// newset.add("Learning");
		// newset.add("Easy");
		// newset.add("Simply");
		//
		// // clone the hash set
		// cloneset=(HashSet)newset.clone();
		//
		// cloneset.add("namak");
		//
		// System.out.println("Hash set values: "+ newset);
		// System.out.println("Clone Hash set values: "+ cloneset);

//		MinMaxPriorityQueue<Float> topKFrequentPatterns = MinMaxPriorityQueue.orderedBy(new MyComparator())
//				.maximumSize(5).create();
//
//		topKFrequentPatterns.add(15F);
//		topKFrequentPatterns.add(14F);
//		topKFrequentPatterns.add(16F);
//		topKFrequentPatterns.add(5F);
//		topKFrequentPatterns.add(4F);
//		topKFrequentPatterns.add(3F);
//		topKFrequentPatterns.add(2F);
//		topKFrequentPatterns.add(10F);
//		topKFrequentPatterns.add(25F);
//		topKFrequentPatterns.add(11F);
//		topKFrequentPatterns.add(12F);
//		topKFrequentPatterns.add(4F);
//		topKFrequentPatterns.add(5F);
//		topKFrequentPatterns.add(13F);
//		topKFrequentPatterns.add(145F);

//		System.out.println("peekFirst: " + topKFrequentPatterns.peekFirst());
//		System.out.println("peekLast: " + topKFrequentPatterns.peekLast());
//		
//		while (!topKFrequentPatterns.isEmpty()) {
//			System.out.print(topKFrequentPatterns.poll() + ", ");
//			
//		}
		
//		String str = "1,yun jun";
//		str = str.replaceAll("[^\\w ]", "").replaceAll("[0-9]+/*\\.*[0-9]*", "");
//		System.out.println(str);

	}

}

class MyComparator implements Comparator<Float> {
	@Override
	public int compare(Float o1, Float o2) {
		return Float.compare(o2, o1);
	}

}
