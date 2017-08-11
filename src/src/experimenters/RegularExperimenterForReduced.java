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

import src.alg1.topkMonitoring.SnapshotTopkMonitoring;
import src.alg1.topkMonitoring.StaticGraphTopK;
import src.finalAlgorithm.topkMonitoring.SnapshotTopkMonitoringOptBiSim;
import src.finalAlgorithm.topkMonitoring.StaticGraphTopKBiSimOpt;
import src.utilities.Dummy;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class RegularExperimenterForReduced {
	private static String allFocusLinesPath;
	private static String allDataGraphPath;
	private static String dateFormat;
	private static boolean debugMode = false;
	// private static int k = 10;
	private static double[] thresholds;
	private static int[] maxAllowedHops;
	private static int[] maxAllowedEdges;
	private static int[] ks;
	public static int numberOfTransactionInASnapshot = 10;
	public static int numberOfSnapshots;
	private static int numberOfIgnoranceInitEdges = 0;
	private static int interval;
	private static int numberOfIntervals;
	private static String deltaEFileOrFiles;

	private static String graphDBPostfix = ".graphdb";
	private static String g0GraphName = "0" + graphDBPostfix;

	private enum StreamMode {
		INSERT, DELETE, MIX
	};

	static StreamMode streamMode = StreamMode.INSERT;
	private static int numberOfSameExperiments;
	private static int edgeStreamCacheCapacity;
	private static int numberOfIgnoreEdgesForAll;

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-allFocusLinesPath")) {
				allFocusLinesPath = args[++i];
			} else if (args[i].equals("-maxAllowedHops")) {
				maxAllowedHops = getArrOutOfCSV(maxAllowedHops, args[++i]);
			} else if (args[i].equals("-maxAllowedEdges")) {
				maxAllowedEdges = getArrOutOfCSV(maxAllowedEdges, args[++i]);
			} else if (args[i].equals("-allDataGraphPath")) {
				allDataGraphPath = args[++i];
				if (allDataGraphPath.contains("/") && !allDataGraphPath.endsWith("/")) {
					allDataGraphPath += "/";
				} else if (allDataGraphPath.contains("\\") && !allDataGraphPath.endsWith("\\")) {
					allDataGraphPath += "\\";
				}
			} else if (args[i].equals("-debugMode")) {
				debugMode = Boolean.parseBoolean(args[++i]);
			}
			// else if (args[i].equals("-k")) {
			// k = Integer.parseInt(args[++i]);
			// }
			else if (args[i].equals("-thresholds")) {
				thresholds = getArrOutOfCSV(thresholds, args[++i]);
			} else if (args[i].equals("-ks")) {
				ks = getArrOutOfCSV(ks, args[++i]);
			} else if (args[i].equals("-numberOfTransactionInASnapshot")) {
				numberOfTransactionInASnapshot = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfSnapshots")) {
				numberOfSnapshots = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			} else if (args[i].equals("-numberOfIgnoranceInitEdges")) {
				numberOfIgnoranceInitEdges = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfIgnoreEdgesForAll")) {
				numberOfIgnoreEdgesForAll = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-interval")) {
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
			}

		}

		if (allFocusLinesPath == null || allDataGraphPath == null || ks == null || numberOfTransactionInASnapshot == 0
				|| numberOfSnapshots == 0 || numberOfIntervals == 0 || interval == 0 || edgeStreamCacheCapacity == 0
				|| thresholds == null || thresholds.length == 0 || maxAllowedEdges == null
				|| maxAllowedEdges.length == 0 || maxAllowedHops == null || maxAllowedHops.length == 0) {
			throw new Exception(
					"input parameters: focusSetPath, dataGraphPath, k, maxAllowedHops, maxAllowedEdges, numberOfTransactionInASnapshot, numberOfSnapshots, deltaEFileOrFiles");
		} else {
			System.out.println("-allFocusLinesPath  " + allFocusLinesPath + "\n -allDataGraphPath:" + allDataGraphPath
					+ "\n -k  " + Arrays.toString(ks) + "\n -maxAllowedHops:" + Arrays.toString(maxAllowedHops)
					+ "\n -maxAllowedEdges  " + Arrays.toString(maxAllowedEdges)
					+ "\n -numberOfTransactionInASnapshot:  " + numberOfTransactionInASnapshot
					+ "\n -numberOfSnapshots:  " + numberOfSnapshots + "\n -thresholds:  " + Arrays.toString(thresholds)
					+ "\n -streamMode:  " + streamMode + "\n -edgeStreamCacheCapacity:  " + edgeStreamCacheCapacity
					+ "\n -deltaEFileOrFiles:  " + deltaEFileOrFiles + "\n -numberOfSameExperiments:  "
					+ numberOfSameExperiments + "\n -dateFormat:  " + dateFormat);
		}

		// finding the root directory of dataGraph to copy it in another
		// directory
		// if (dataGraphPath.lastIndexOf("/") <= 0) {
		// throw new Exception("dataGraphPath.lastIndexOf('\\')<=0");
		// }

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
						for (int k : ks) {

							if (e < h)
								continue;

							String dataGraphPath = null;
							for (int n = 0; n < numberOfIntervals; n++) {
								// when n=0 dataGraph is null so g0
								// then n=1 => 5k, 10k, ...

								if (DummyProperties.windowMode)
									throw new Exception("shouldn't be used for windowing.");

								dataGraphPath = getNextDataGraphPath(dataGraphPath, streamMode, interval, n);
								System.out.println("dataGraphPath: " + dataGraphPath);
								numberOfIgnoranceInitEdges = n * interval;
								numberOfIgnoranceInitEdges += numberOfIgnoreEdgesForAll;

								// Batch SGI
//								for (int exp = 0; exp < numberOfSameExperiments; exp++) {
//									StaticGraphTopK staticGraphTopK = new StaticGraphTopK("focusSet.txt", h, e,
//											dataGraphPath, debugMode, k, t, DummyProperties.windowMode,
//											DummyProperties.WINDOW_SIZE);
//									staticGraphTopK.findStaticTopK();
//
//									System.out.println("Static: exp " + exp + " focus was " + line + ", h:" + h
//											+ " , e:" + e + " , k:" + k + ", threshold:" + t + " in interval: " + n);
//
//									staticGraphTopK = null;
//									sleepAndWakeUp();
//								}

								// Batch BiSim
								for (int exp = 0; exp < numberOfSameExperiments; exp++) {
									StaticGraphTopKBiSimOpt staticGraphTopKBiSimOpt = new StaticGraphTopKBiSimOpt(
											"focusSet.txt", h, e, dataGraphPath, debugMode, k, t,
											DummyProperties.windowMode, DummyProperties.WINDOW_SIZE);
									staticGraphTopKBiSimOpt.findStaticTopK();

									System.out.println("Static Bi Sim Opt: exp " + exp + " focus was " + line + ", h:"
											+ h + " , e:" + e + " , k:" + k + ", threshold:" + t + " in interval: "
											+ n);

									staticGraphTopKBiSimOpt = null;
									sleepAndWakeUp();
								}
								
								// IncDisN BiSim
								for (int exp = 0; exp < numberOfSameExperiments; exp++) {
									Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
									if (newDGPath == null) {
										throw new Exception("newDGPath is null!");
									}

									SnapshotTopkMonitoringOptBiSim snapshotTopkMonitoringOptBiSim = new SnapshotTopkMonitoringOptBiSim(
											"focusSet.txt", h, e, newDGPath.toString(), debugMode, k, t,
											deltaEFileOrFiles, numberOfTransactionInASnapshot, numberOfSnapshots,
											dateFormat, numberOfIgnoranceInitEdges, 1,
											DummyProperties.windowMode, DummyProperties.WINDOW_SIZE);

									snapshotTopkMonitoringOptBiSim.snapshotTopkMonitor();

									System.out.println("IncBiSim1: exp " + exp + " focus was " + line + ", h:" + h
											+ " , e:" + e + " , k:" + k + ", threshold:" + t + " in interval: " + n);

									snapshotTopkMonitoringOptBiSim = null;
									sleepAndWakeUp();
								}

								
								// IncDisN BiSim
								// for (int exp = 0; exp <
								// numberOfSameExperiments;
								// exp++) {
								// Path newDGPath =
								// DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
								// if (newDGPath == null) {
								// throw new Exception("newDGPath is null!");
								// }
								//
								// SnapshotTopkMonitoringBiSim
								// snapshotTopkMonitoringBiSim = new
								// SnapshotTopkMonitoringBiSim(
								// "focusSet.txt", h, e, newDGPath.toString(),
								// debugMode, k, t, deltaEFileOrFiles,
								// numberOfTransactionInASnapshot,
								// numberOfSnapshots, dateFormat,
								// numberOfIgnoranceInitEdges,
								// DummyProperties.windowMode,
								// DummyProperties.WINDOW_SIZE);
								//
								// snapshotTopkMonitoringBiSim.snapshotTopkMonitor();
								//
								// System.out.println("IncBiSim: exp " + exp + "
								// focus was " + line + ", h:" + h + " , e:"
								// + e + " , k:" + k + ", threshold:" + t + " in
								// interval: " + n);
								//
								// snapshotTopkMonitoringBiSim = null;
								// sleepAndWakeUp();
								// }

								// IncDis SGI
								// for (int exp = 0; exp <
								// numberOfSameExperiments;
								// exp++) {
								// Path newDGPath =
								// DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
								// if (newDGPath == null) {
								// throw new Exception("newDGPath is null!");
								// }
								//
								// SnapshotTopkMonitoringOpt
								// snapshotTopkMonitoringOpt = new
								// SnapshotTopkMonitoringOpt(
								// "focusSet.txt", h, e, newDGPath.toString(),
								// debugMode, k, t, deltaEFileOrFiles,
								// numberOfTransactionInASnapshot,
								// numberOfSnapshots, dateFormat,
								// numberOfIgnoranceInitEdges,
								// edgeStreamCacheCapacity,
								// DummyProperties.windowMode,
								// DummyProperties.WINDOW_SIZE);
								//
								// snapshotTopkMonitoringOpt.snapshotTopkMonitor();
								//
								// System.out.println("Inc+: exp " + exp + "
								// focus
								// was " + line + ", h:" + h + " , e:" + e
								// + " , k:" + k + ", threshold:" + t + " in
								// interval: " + n);
								//
								// snapshotTopkMonitoringOpt = null;
								// sleepAndWakeUp();
								// }

								// IncDis BiSim
								for (int exp = 0; exp < numberOfSameExperiments; exp++) {
									Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
									if (newDGPath == null) {
										throw new Exception("newDGPath is null!");
									}

									SnapshotTopkMonitoringOptBiSim snapshotTopkMonitoringOptBiSim = new SnapshotTopkMonitoringOptBiSim(
											"focusSet.txt", h, e, newDGPath.toString(), debugMode, k, t,
											deltaEFileOrFiles, numberOfTransactionInASnapshot, numberOfSnapshots,
											dateFormat, numberOfIgnoranceInitEdges, edgeStreamCacheCapacity,
											DummyProperties.windowMode, DummyProperties.WINDOW_SIZE);

									snapshotTopkMonitoringOptBiSim.snapshotTopkMonitor();

									System.out.println("IncBiSim++: exp " + exp + " focus was " + line + ", h:" + h
											+ " , e:" + e + " , k:" + k + ", threshold:" + t + " in interval: " + n);

									snapshotTopkMonitoringOptBiSim = null;
									sleepAndWakeUp();
								}

							}

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
		Thread.sleep(3000);
		System.gc();
		System.runFinalization();
		Thread.sleep(3000);
		System.out.println("waking up..." + new Date());
	}

	private static String getNextDataGraphPath(String dataGraphPath, StreamMode streamMode, int interval,
			int currentInterval) {
		if (currentInterval == 0) {
			return allDataGraphPath + g0GraphName;
		} else {
			return allDataGraphPath + (currentInterval * interval) + "_" + streamMode.toString() + graphDBPostfix;
		}
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
