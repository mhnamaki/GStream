package src.simpleTests;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class BitmapMemoryTest {

	public static void main(String[] args) throws Exception {

		// get one hashMap<Integer, HashSet<Integer>>
		// HashMap<Integer, HashSet<Integer>> hashMapTest = new HashMap<Integer,
		// HashSet<Integer>>();
		//
		// for (int i = 0; i < 1000000; i++) {
		// hashMapTest.put(i, new HashSet<Integer>());
		// if (i % 50000 == 0) {
		// System.out.println(i);
		// }
		// for (int j = 0; j < 5000; j++) {
		// hashMapTest.get(i).add(j);
		// }
		// }
		//
		// System.out.println("let's see");
		//
		// Thread.sleep(30000);
		// System.out.println("finisnhed!");

		// ArrayList<HashSet<Integer>> hashMapTest = new
		// ArrayList<HashSet<Integer>>();
		//
		// for (int i = 0; i < 1000000; i++) {
		// hashMapTest.add(new HashSet<Integer>());
		// if (i % 50000 == 0) {
		// System.out.println(i);
		// }
		// for (int j = 0; j < 5000; j++) {
		// hashMapTest.get(i).add(j);
		// }
		// }
		//
		// System.out.println("let's see");
		//
		// Thread.sleep(30000);
		// System.out.println("finisnhed!");

		// get bitmap
		//
		// RoaringBitmap[] rBitmapsOfNodes = new RoaringBitmap[1000000];
		// for (int i = 0; i < rBitmapsOfNodes.length; i++) {
		// rBitmapsOfNodes[i] = new RoaringBitmap();
		// }
		//
		// for (int i = 0; i < rBitmapsOfNodes.length; i++) {
		// if (i % 50000 == 0) {
		// System.out.println(i);
		// }
		// for (int j = 0; j < 70000; j++) {
		// rBitmapsOfNodes[i].add(j);
		// }
		// rBitmapsOfNodes[i].runOptimize();
		//
		//
		// }
		//
		// System.out.println("let's see");
		//
		// Thread.sleep(30000);
		// System.out.println("finisnhed!");

//		double start = System.nanoTime();
//		BiMap<Integer, Integer> integerOfInteger = HashBiMap.create();
//		for (int i = 0; i < 10000000; i++) {
//			// if (i % 50000 == 0) {
//			// System.out.println(i);
//			// }
//			int j = i + 10000000;
//			integerOfInteger.put(i, j);
//
//		}
//		System.out.println((System.nanoTime() - start) / 1e6);
//		System.out.println("let's see");
//
//		Thread.sleep(30000);
//		System.out.println("finisnhed!");

		 double start = System.nanoTime();
		 HashMap<Integer, Integer> integerOfInteger1 = new HashMap<Integer,
		 Integer>();
		 HashMap<Integer, Integer> integerOfInteger2 = new HashMap<Integer,
		 Integer>();
		 for (int i = 0; i < 10000000; i++) {
		 //if (i % 50000 == 0) {
		 //System.out.println(i);
		 //}
		 int j = i + 10000000;
		 integerOfInteger1.put(i, j);
		 integerOfInteger2.put(j, i);
		
		 }
		 System.out.println((System.nanoTime() - start) / 1e6);
		 System.out.println("let's see");
		
		 Thread.sleep(30000);
		 System.out.println("finisnhed!");

	}

}
