package src.utilities;

import java.nio.file.Path;
import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;

import src.base.IPrefixTreeNodeData;
import src.finalAlgorithm.topkMonitoring.SnapshotTopkMonitoringOptBiSim;
import src.finalAlgorithm.topkMonitoring.StaticGraphTopKBiSimOpt;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class AccuracyChecker {

	public static String dataGraphPath1;
	public static String dataGraphPath2;
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

	private static String deltaEFileOrFiles;

	private static int edgeStreamCacheCapacity;

	private static String focusSetPath;

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-focusSetPath")) {
				focusSetPath = args[++i];
			} else if (args[i].equals("-maxAllowedHops")) {
				maxAllowedHops = getArrOutOfCSV(maxAllowedHops, args[++i]);
			} else if (args[i].equals("-maxAllowedEdges")) {
				maxAllowedEdges = getArrOutOfCSV(maxAllowedEdges, args[++i]);
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
			} else if (args[i].equals("-deltaEFileOrFiles")) {
				deltaEFileOrFiles = args[++i];
			} else if (args[i].equals("-dataGraphPathStat")) {
				dataGraphPath1 = args[++i];
			} else if (args[i].equals("-dataGraphPathSnap")) {
				dataGraphPath2 = args[++i];
			} else if (args[i].equals("-edgeStreamCacheCapacity")) {
				edgeStreamCacheCapacity = Integer.parseInt(args[++i]);
			}

		}

		Dummy.DummyProperties.debugMode = debugMode;

		for (int h : maxAllowedHops) {
			for (int e : maxAllowedEdges) {
				for (double t : thresholds) {
					for (int k : ks) {

						if (e < h)
							continue;

						StaticGraphTopKBiSimOpt staticGraphTopKBiSimOpt = new StaticGraphTopKBiSimOpt(focusSetPath, h,
								e, dataGraphPath1, debugMode, k, t, DummyProperties.windowMode,
								DummyProperties.WINDOW_SIZE);
						staticGraphTopKBiSimOpt.findStaticTopK();

						Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath2);
						dataGraphPath2 = newDGPath.toString();

						SnapshotTopkMonitoringOptBiSim snapshotTopkMonitoringOptBiSim = new SnapshotTopkMonitoringOptBiSim(
								focusSetPath, h, e, dataGraphPath2, debugMode, k, t, deltaEFileOrFiles,
								numberOfTransactionInASnapshot, numberOfSnapshots, dateFormat,
								numberOfIgnoranceInitEdges, edgeStreamCacheCapacity, DummyProperties.windowMode,
								DummyProperties.WINDOW_SIZE);

						snapshotTopkMonitoringOptBiSim.snapshotTopkMonitor();

						// comparison of answers:
						HashMap<Integer, ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>> patternsInALevelStatic = new HashMap<Integer, ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>>();
						HashMap<Integer, ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>> patternsInALevelSnap = new HashMap<Integer, ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>>();

						for (int i = 1; i < e + 2; i++) {
							patternsInALevelStatic.putIfAbsent(i, new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>());
							patternsInALevelSnap.putIfAbsent(i, new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>());
						}

						// static
						for (Integer patternId : staticGraphTopKBiSimOpt.prefixTree.getPrefixTreeNodeIndex().keySet()) {
							patternsInALevelStatic
									.get(staticGraphTopKBiSimOpt.prefixTree.getPrefixTreeNodeIndex().get(patternId)
											.getLevel())
									.add(staticGraphTopKBiSimOpt.prefixTree.getPrefixTreeNodeIndex().get(patternId));
						}

						// snap
						for (Integer patternId : snapshotTopkMonitoringOptBiSim.prefixTree.getPrefixTreeNodeIndex()
								.keySet()) {
							patternsInALevelSnap
									.get(snapshotTopkMonitoringOptBiSim.prefixTree.getPrefixTreeNodeIndex()
											.get(patternId).getLevel())
									.add(snapshotTopkMonitoringOptBiSim.prefixTree.getPrefixTreeNodeIndex()
											.get(patternId));
						}

						ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> statNotInSnap = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();
						ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> snapNotInStat = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();
						for (int i = 1; i < e + 2; i++) {
							if (patternsInALevelStatic.get(i).size() != patternsInALevelSnap.get(i).size()) {
								System.out.println("inconsistencies in level " + i + " static size:"
										+ patternsInALevelStatic.get(i).size() + " snap size:"
										+ patternsInALevelSnap.size());
							}

							for (PrefixTreeNode<IPrefixTreeNodeData> staticPTNode : patternsInALevelStatic.get(i)) {
								boolean foundIsomorphic = false;
								for (PrefixTreeNode<IPrefixTreeNodeData> snapPTNode : patternsInALevelSnap.get(i)) {
									VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = staticGraphTopKBiSimOpt.prefixTree
											.getIsomorphism(staticPTNode.getData().getPatternGraph(),
													snapPTNode.getData().getPatternGraph());
									if (iso.isomorphismExists()) {
										foundIsomorphic = true;
										break;
									}
								}

								if (!foundIsomorphic) {
									statNotInSnap.add(staticPTNode);
								}
							}

							for (PrefixTreeNode<IPrefixTreeNodeData> snapPTNode : patternsInALevelSnap.get(i)) {
								boolean foundIsomorphic = false;
								for (PrefixTreeNode<IPrefixTreeNodeData> staticPTNode : patternsInALevelStatic.get(i)) {
									VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = staticGraphTopKBiSimOpt.prefixTree
											.getIsomorphism(staticPTNode.getData().getPatternGraph(),
													snapPTNode.getData().getPatternGraph());
									if (iso.isomorphismExists()) {
										foundIsomorphic = true;
										break;
									}
								}

								if (!foundIsomorphic) {
									snapNotInStat.add(snapPTNode);
								}
							}
						}

						Visualizer.visualizeXnotInY("are in inc not in batch", snapNotInStat);
						Visualizer.visualizeXnotInY("are in batch not in inc", statNotInSnap);

					}

				}

			}
		}
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
