package src.utilities;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.github.jamm.MemoryMeter;

public class DeepMemory22 {
	MemoryMeter meter = new MemoryMeter();

	HashMap hm1 = new HashMap<Integer, Integer>();
	HashMap hm2 = new HashMap<Integer, Integer>();
	HashMap hm3 = new HashMap<Integer, Integer>();
	HashMap hm4 = new HashMap<Integer, Integer>();
	ArrayList<TestiClass> testiClass = new ArrayList<TestiClass>();;
	 TestiClass tc1;
 TestiClass tc2;

	public void measure(Object anObject) {

		System.out.println("-----------------------------------");
		System.out.printf("size: %d bytes\n", meter.measure(anObject));
		System.out.printf("retained size: %d bytes\n", meter.measureDeep(anObject));
		System.out.printf("inner object count: %d\n", meter.countChildren(anObject));
	}

	public static void main(String args[]) {

		DeepMemory22 dm = new DeepMemory22();
		dm.run();
		// System.out.println("first");
		// dm.measure(dm);
		//
		// for (int i = 1; i <= 100; i++) {
		// dm.hm1.put(i, i);
		// }
		// // dm.measure(dm.hm1);
		//
		// for (int i = 1; i <= 1000; i++) {
		// dm.hm2.put(i, i);
		// }
		// // dm.measure(dm.hm2);
		//
		// for (int i = 1; i <= 10000; i++) {
		// dm.hm3.put(i, i);
		// }
		// // dm.measure(dm.hm3);
		//
		// for (int i = 1; i <= 100000; i++) {
		// dm.hm4.put(i, i);
		// }

		// dm.measure(dm.hm4);

	}

	private void run() {
		System.out.println("second");
		this.measure(this);

		 tc1 = new TestiClass(10);

		System.out.println("third");
		this.measure(this);

		 tc2 = new TestiClass(100);
		System.out.println("forth");
		this.measure(this);

	}
}

class TestiClass {
	public InnerClass ic;

	public TestiClass(int maxI) {
		ic = new InnerClass(maxI);
	}
}

class InnerClass {
	public InnerInnerClass iic;

	public InnerClass(int maxI) {
		iic = new InnerInnerClass(maxI);
	}
}

class InnerInnerClass {
	ArrayList<Integer> dd = new ArrayList<Integer>();

	public InnerInnerClass(int maxI) {
		for (int i = 0; i < maxI; i++) {
			dd.add(i);
		}
	}
}
