package src.alg1.test;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.util.Date;
//
//import src.alg1.topkMonitoring.StaticGraphTopK;
//import src.utilities.Dummy;
//import src.utilities.Dummy.DummyProperties;
//
public class StaticTopKTester {
//
//	private static String allFocusLinesPath;
//	private static String dataGraphPath;
//	private static boolean debugMode = false;
//	private static int k = 10;
//	private static double threshold = 0.0d;
//	private static int maxAllowedHops = 3;
//	private static int maxAllowedEdges = 5;
//	private static boolean windowMode = false;
//	private static int windowSizeL = 2;
//	private static int startingWindow = 0;
//	private static int endingWindow = 1; // 0, 1, 2
//
//	public static void main(String[] args) throws Exception {
//
//		for (int i = 0; i < args.length; i++) {
//			if (args[i].equals("-allFocusLinesPath")) {
//				allFocusLinesPath = args[++i];
//			} else if (args[i].equals("-maxAllowedHops")) {
//				maxAllowedHops = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-maxAllowedEdges")) {
//				maxAllowedEdges = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-dataGraphPath")) {
//				dataGraphPath = args[++i];
//			} else if (args[i].equals("-debugMode")) {
//				debugMode = Boolean.parseBoolean(args[++i]);
//			} else if (args[i].equals("-k")) {
//				k = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-threshold")) {
//				threshold = Double.parseDouble(args[++i]);
//			} else if (args[i].equals("-windowMode")) {
//				windowMode = Boolean.parseBoolean(args[++i]);
//				DummyProperties.windowMode = windowMode;
//			} else if (args[i].equals("-windowSize")) {
//				windowSizeL = Integer.parseInt(args[++i]);
//				DummyProperties.WINDOW_SIZE = windowSizeL;
//				endingWindow = windowSizeL - 1;
//			}
//		}
//
//		if (allFocusLinesPath == null || dataGraphPath == null || k == 0 || maxAllowedHops == 0
//				|| maxAllowedEdges == 0) {
//			throw new Exception("input parameters: focusSetPath, dataGraphPath, k, maxAllowedHops, maxAllowedEdges");
//		} else {
//			System.out.println("-allFocusLinesPath  " + allFocusLinesPath + ", -dataGraphPath:" + dataGraphPath
//					+ ", -k  " + k + ", -maxAllowedHops:" + maxAllowedHops + ", -maxAllowedEdges  " + maxAllowedEdges);
//		}
//		Dummy.DummyProperties.debugMode = debugMode;
//
//		// read from each line of all focus lines path and create a
//		// focusSetFile....
//		FileInputStream fis = new FileInputStream(allFocusLinesPath);
//
//		// Construct BufferedReader from InputStreamReader
//		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//
//		String line = null;
//		// a focus set line
//		while ((line = br.readLine()) != null) {
//			if (line.trim().equals(""))
//				continue;
//
//			File fout = new File("focusSet.txt");
//			FileOutputStream fos = new FileOutputStream(fout);
//
//			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//			bw.write(line.split(" = ")[0]);
//			bw.close();
//
//			for (int h = 1; h <= maxAllowedHops; h++) {
//				for (int e = 1; e <= maxAllowedEdges; e++) {
//					if (e < h) {
//						continue;
//					}
//					StaticGraphTopK staticGraphTopK = new StaticGraphTopK("focusSet.txt", h, e, dataGraphPath,
//							debugMode, k, threshold);
//					staticGraphTopK.findStaticTopK();
//					System.out.println(
//							"focus was " + line + ", h:" + h + " , e:" + e + " , k:" + k + ", threshold:" + threshold);
//
//					staticGraphTopK = null;
//					System.out.println("sleeping..." + new Date());
//					System.gc();
//					System.runFinalization();
//					Thread.sleep(5000);
//					System.gc();
//					System.runFinalization();
//					Thread.sleep(5000);
//					System.out.println("waking up..." + new Date());
//				}
//			}
//		}
//
//		br.close();
//		System.out.println("program is finished successfully!");
//	}
//
}
