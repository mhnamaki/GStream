package src.experimenters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import org.github.jamm.MemoryMeter;

import src.alg1.topkMonitoring.SnapshotTopkMonitoring;
import src.alg1.topkMonitoring.StaticGraphTopK;
import src.alg1BiSim.topkMonitoring.SnapshotTopkMonitoringBiSim;
import src.finalAlgorithm.topkMonitoring.SnapshotTopkMonitoringOptBiSim;
import src.finalAlgorithm.topkMonitoring.StaticGraphTopKBiSimOpt;
import src.optAlg.topkMonitoring.SnapshotTopkMonitoringOpt;
import src.opt‌BiSimAlg.topkMonitoringSGI.StaticGraphTopKBiSimOptSGI;
import src.utilities.Dummy;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class RegularExperimenterForBaseline {
	private static String allFocusLinesPath;
	private static String dataGraphPath;
	private static String dateFormat;
	private static boolean debugMode = false;
	private static int k = 10;
	private static double[] thresholds;
	private static int[] maxAllowedHops;
	private static int[] maxAllowedEdges;
	public static int numberOfTransactionInASnapshot = 10;
	public static int numberOfSnapshots;
	private static int numberOfIgnoranceInitEdges = 0;
	private static int interval;
	private static int numberOfIntervals;
	private static String deltaEFileOrFiles;

	private static String graphDBPostfix = ".graphdb";
	private static String g0GraphName = "g0" + graphDBPostfix;

	private enum StreamMode {
		INSERT, DELETE, MIX
	};

	static StreamMode streamMode = StreamMode.INSERT;
	private static int numberOfSameExperiments;
	private static int edgeStreamCacheCapacity;
	private static int startingInterval = 0;

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-allFocusLinesPath")) {
				allFocusLinesPath = args[++i];
			} else if (args[i].equals("-maxAllowedHops")) {
				maxAllowedHops = getArrOutOfCSV(maxAllowedHops, args[++i]);
			} else if (args[i].equals("-maxAllowedEdges")) {
				maxAllowedEdges = getArrOutOfCSV(maxAllowedEdges, args[++i]);
			} else if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
				if (dataGraphPath.contains("/") && !dataGraphPath.endsWith("/")) {
					dataGraphPath += "/";
				} else if (dataGraphPath.contains("\\") && !dataGraphPath.endsWith("\\")) {
					dataGraphPath += "\\";
				}
			} else if (args[i].equals("-debugMode")) {
				debugMode = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-k")) {
				k = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-thresholds")) {
				thresholds = getArrOutOfCSV(thresholds, args[++i]);
			} else if (args[i].equals("-numberOfTransactionInASnapshot")) {
				numberOfTransactionInASnapshot = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfSnapshots")) {
				numberOfSnapshots = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			} else if (args[i].equals("-numberOfIgnoranceInitEdges")) {
				numberOfIgnoranceInitEdges = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-interval")) {
				interval = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfIntervals")) {
				numberOfIntervals = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-streamMode")) {
				switch (args[++i].toLowerCase()) {
				case "insert":
					streamMode = StreamMode.INSERT;
					break;
				case "delete":
					streamMode = StreamMode.DELETE;
					break;
				case "mixed":
					streamMode = StreamMode.MIX;
					break;

				}
			} else if (args[i].equals("-numberOfSameExperiments")) {
				numberOfSameExperiments = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-deltaEFileOrFiles")) {
				deltaEFileOrFiles = args[++i];
			} else if (args[i].equals("-edgeStreamCacheCapacity")) {
				edgeStreamCacheCapacity = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-startingInterval")) {
				startingInterval = Integer.parseInt(args[++i]);
			}

		}

		if (allFocusLinesPath == null || dataGraphPath == null || k == 0 || thresholds == null || thresholds.length == 0
				|| maxAllowedEdges == null || maxAllowedEdges.length == 0 || maxAllowedHops == null
				|| maxAllowedHops.length == 0) {
			throw new Exception("input parameters: focusSetPath, dataGraphPath, k, maxAllowedHops, maxAllowedEdges,");
		} else {
			System.out.println("-allFocusLinesPath  " + allFocusLinesPath + "\n -allDataGraphPath:" + dataGraphPath
					+ "\n -k  " + k + "\n -maxAllowedHops:" + Arrays.toString(maxAllowedHops) + "\n -maxAllowedEdges  "
					+ Arrays.toString(maxAllowedEdges) + "\n -numberOfTransactionInASnapshot:  "
					+ numberOfTransactionInASnapshot + "\n -numberOfSnapshots:  " + numberOfSnapshots
					+ "\n -thresholds:  " + Arrays.toString(thresholds) + "\n -streamMode:  " + streamMode
					+ "\n -edgeStreamCacheCapacity:  " + edgeStreamCacheCapacity + "\n -deltaEFileOrFiles:  "
					+ deltaEFileOrFiles + "\n -numberOfSameExperiments:  " + numberOfSameExperiments
					+ "\n -dateFormat:  " + dateFormat);
		}

		// finding the root directory of dataGraph to copy it in another
		// directory
		// if (dataGraphPath.lastIndexOf("/") <= 0) {
		// throw new Exception("dataGraphPath.lastIndexOf('\\')<=0");
		// }

		// MemoryMeter meter = new MemoryMeter();

		Dummy.DummyProperties.debugMode = debugMode;

		// read from each line of all focus lines path and create a
		// focusSetFile....
		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			File fout = new File("focusSet.txt");
			FileOutputStream fos = new FileOutputStream(fout);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(line.split(" = ")[0]);
			bw.close();

			for (int h : maxAllowedHops) {
				for (int e : maxAllowedEdges) {
					for (double t : thresholds) {

						if (e < h)
							continue;

						//String dataGraphPath = null;

						// when n=0 dataGraph is null so g0
						// then n=1 => 5k, 10k, ...

						if (DummyProperties.windowMode)
							throw new Exception("shouldn't be used for windowing.");

						// Batch SGI
						for (int exp = 0; exp < numberOfSameExperiments; exp++) {
							StaticGraphTopK staticGraphTopK = new StaticGraphTopK("focusSet.txt", h, e, dataGraphPath,
									debugMode, k, t, DummyProperties.windowMode, DummyProperties.WINDOW_SIZE);
							// double startTime = System.nanoTime();
							staticGraphTopK.findStaticTopK();

							System.out.println("StaticGraphTopK: exp " + exp + " focus was " + line + ", h:" + h
									+ " , e:" + e + " , k:" + k + ", threshold:" + t);

							staticGraphTopK = null;
							sleepAndWakeUp();
						}

					}

				}
			}
		}

		br.close();
		System.out.println("program is finished successfully!");

	}

	private static void sleepAndWakeUp() throws Exception {
		System.out.println("sleeping..." + new Date());
		System.gc();
		System.runFinalization();
		Thread.sleep(2500);
		System.gc();
		System.runFinalization();
		Thread.sleep(2500);
		System.out.println("waking up..." + new Date());
	}

	private static double[] getArrOutOfCSV(double[] doubleArr, String string) {
		String[] strArray = string.split(",");
		doubleArr = new double[strArray.length];
		for (int i = 0; i < strArray.length; i++) {
			doubleArr[i] = Double.parseDouble(strArray[i]);
		}
		return doubleArr;
	}

	private static int[] getArrOutOfCSV(int[] intArr, String string) {
		String[] strArray = string.split(",");
		intArr = new int[strArray.length];
		for (int i = 0; i < strArray.length; i++) {
			intArr[i] = Integer.parseInt(strArray[i]);
		}
		return intArr;
	}
}
