//package src.optAlg.test;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.Date;
//
//import src.alg1.topkMonitoring.SnapshotTopkMonitoring;
//import src.utilities.Dummy;
//import src.utilities.Dummy.DummyFunctions;
//
//public class SnapshotTopKTester {
//
//	private static String allFocusLinesPath;
//	private static String dataGraphPath;
//	private static String dateFormat;
//	private static boolean debugMode = false;
//	private static int k = 10;
//	private static double threshold = 0.0d;
//	private static int maxAllowedHops = 3;
//	private static int maxAllowedEdges = 5;
//	public static String deltaEFileOrFiles;
//	public static int numberOfTransactionInASnapshot = 10;
//	public static int numberOfSnapshots;
//	private static int numberOfIgnoranceInitEdges = 0;
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
//				if (dataGraphPath.endsWith("/") || dataGraphPath.endsWith("\\")) {
//					dataGraphPath = dataGraphPath.substring(0, dataGraphPath.length() - 1);
//				}
//			} else if (args[i].equals("-debugMode")) {
//				debugMode = Boolean.parseBoolean(args[++i]);
//			} else if (args[i].equals("-k")) {
//				k = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-threshold")) {
//				threshold = Double.parseDouble(args[++i]);
//			} else if (args[i].equals("-deltaEFileOrFiles")) {
//				deltaEFileOrFiles = args[++i];
//			} else if (args[i].equals("-numberOfTransactionInASnapshot")) {
//				numberOfTransactionInASnapshot = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-numberOfSnapshots")) {
//				numberOfSnapshots = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-dateFormat")) {
//				dateFormat = args[++i];
//			} else if (args[i].equals("-numberOfIgnoranceInitEdges")) {
//				numberOfIgnoranceInitEdges = Integer.parseInt(args[++i]);
//			}
//		}
//
//		if (allFocusLinesPath == null || dataGraphPath == null || k == 0 || maxAllowedHops == 0 || maxAllowedEdges == 0
//				|| numberOfTransactionInASnapshot == 0 || numberOfSnapshots == 0 || deltaEFileOrFiles == null) {
//			throw new Exception(
//					"input parameters: focusSetPath, dataGraphPath, k, maxAllowedHops, maxAllowedEdges, numberOfTransactionInASnapshot, numberOfSnapshots, deltaEFileOrFiles");
//		} else {
//			System.out.println("-allFocusLinesPath  " + allFocusLinesPath + ", -dataGraphPath:" + dataGraphPath
//					+ ", -k  " + k + ", -maxAllowedHops:" + maxAllowedHops + ", -maxAllowedEdges  " + maxAllowedEdges
//					+ ", -deltaEFileOrFiles:  " + deltaEFileOrFiles + ", -numberOfTransactionInASnapshot:  "
//					+ numberOfTransactionInASnapshot + ", -numberOfSnapshots:  " + numberOfSnapshots);
//		}
//
//		// finding the root directory of dataGraph to copy it in another
//		// directory
//		// if (dataGraphPath.lastIndexOf("/") <= 0) {
//		// throw new Exception("dataGraphPath.lastIndexOf('\\')<=0");
//		// }
//
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
//
//					Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
//					if (newDGPath == null) {
//						throw new Exception("newDGPath is null!");
//					}
//
//					SnapshotTopkMonitoring snapshotTopkMonitoring = new SnapshotTopkMonitoring("focusSet.txt", h, e,
//							newDGPath.toString(), debugMode, k, threshold, deltaEFileOrFiles,
//							numberOfTransactionInASnapshot, numberOfSnapshots, dateFormat, numberOfIgnoranceInitEdges);
//
//					snapshotTopkMonitoring.snapshotTopkMonitor();
//
//					System.out.println(
//							"focus was " + line + ", h:" + h + " , e:" + e + " , k:" + k + ", threshold:" + threshold);
//
//					snapshotTopkMonitoring = null;
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
//}
