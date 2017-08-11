package src.finalAlgorithm.topkMonitoring;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import com.google.common.collect.MinMaxPriorityQueue;
import src.base.IPrefixTreeNodeData;
import src.finalAlgorithm.prefixTree.PrefixTreeOptBiSim;
import src.utilities.Bitmap;
import src.utilities.DebugHelper;
import src.utilities.Dummy;
import src.utilities.InfoHolder;
import src.utilities.PrefixTreeNode;
import src.utilities.SupportComparator;
import src.utilities.TimeLogger;
import src.utilities.TopKHandler;
import src.utilities.Visualizer;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class StaticGraphTopKBiSimOpt {

	private String focusSetPath;
	private int maxAllowedHops;
	private int maxAllowedEdges;
	private String dataGraphPath;
	private boolean debugMode;
	private int k;
	private GraphDatabaseService dataGraph;
	private double threshold = 0.0d;
	private int numberOfAllFocusNodes = 0;
	MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns;
	Bitmap bitMap = new Bitmap();
	private boolean windowMode = false;
	private int windowSizeL = 2;
	private int startingWindow = 0;
	private int endingWindow = 1; // 0, 1, 2
	public PrefixTreeOptBiSim prefixTree;

	public StaticGraphTopKBiSimOpt(String[] args) throws Exception {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-focusSetPath")) {
				focusSetPath = args[++i];
			} else if (args[i].equals("-maxAllowedHops")) {
				maxAllowedHops = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-maxAllowedEdges")) {
				maxAllowedEdges = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-debugMode")) {
				debugMode = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-visualize")) {
				DummyProperties.visualize = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-k")) {
				k = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-threshold")) {
				threshold = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-windowMode")) {
				windowMode = Boolean.parseBoolean(args[++i]);
				DummyProperties.windowMode = windowMode;
			} else if (args[i].equals("-windowSize")) {
				windowSizeL = Integer.parseInt(args[++i]);
				DummyProperties.WINDOW_SIZE = windowSizeL;
				endingWindow = windowSizeL - 1;
			}
		}

		if (focusSetPath == null || dataGraphPath == null || maxAllowedHops == 0 || maxAllowedEdges == 0 || k == 0) {
			throw new Exception("input parameters: focusSetPath, maxAllowedHops, dataGraphPath, maxAllowedEdges, k");
		} else {
			System.out.println("StaticGraphTopK: -focusSetPath  " + focusSetPath + ", -maxAllowedHops:" + maxAllowedHops
					+ ", -maxAllowedEdges  " + maxAllowedEdges + ", -dataGraphPath:" + dataGraphPath + ", -k  " + k);
		}

		if (maxAllowedEdges < maxAllowedHops) {
			throw new Exception(" maxAllowedEdges < maxAllowedHops : " + maxAllowedEdges + " < " + maxAllowedHops);
		}

		Dummy.DummyProperties.debugMode = debugMode;

		// findStaticTopK();

	}

	public StaticGraphTopKBiSimOpt(String focusSetPath, int maxHops, int maxEdges, String dataGraphPath,
			boolean debugMode, int k, double threshold, boolean windowMode, int windowSizeL) {

		this.focusSetPath = focusSetPath;
		this.maxAllowedHops = maxHops;
		this.maxAllowedEdges = maxEdges;
		this.dataGraphPath = dataGraphPath;
		this.debugMode = debugMode;
		this.k = k;
		this.threshold = threshold;
		this.windowMode = windowMode;
		DummyProperties.windowMode = windowMode;
		this.windowSizeL = windowSizeL;
		DummyProperties.WINDOW_SIZE = windowSizeL;
		this.endingWindow = windowSizeL - 1;
	}

	public void findStaticTopK() {

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "2g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		topKFrequentPatterns = MinMaxPriorityQueue.orderedBy(new SupportComparator()).maximumSize(k).create();

		// in order to get the lowerbound:
		// topKFrequentPatterns.peekLast()

		try (Transaction tx1 = dataGraph.beginTx()) {

			// STAT Of the DB START
			int numberOfAllNodes = Dummy.DummyFunctions.getNumberOfAllNodes(dataGraph);
			int numberOfAllRelationships = Dummy.DummyFunctions.getNumberOfAllRels(dataGraph);
			HashSet<String> differentLabels = Dummy.DummyFunctions.getDifferentLabels(dataGraph);
			HashSet<String> differentRelTypes = Dummy.DummyFunctions.getDifferentRelType(dataGraph);

			double avgDegrees = Dummy.DummyFunctions.getAvgOutDegrees(dataGraph);
			// STAT Of the DB END

			// init prefixTree
			prefixTree = new PrefixTreeOptBiSim(focusSetPath, maxAllowedHops, maxAllowedEdges, dataGraph, debugMode,
					bitMap, topKFrequentPatterns, dataGraphPath, DummyProperties.SnapOrStat.Stat, threshold);

			double startTime = System.nanoTime();
			HashSet<Integer> allFocusNodes = new HashSet<Integer>();
			for (String focusLabel : prefixTree.allNodesOfFocusType.keySet()) {
				allFocusNodes.addAll(prefixTree.allNodesOfFocusType.get(focusLabel));
				// for (Integer nodeId :
				// prefixTree.allNodesOfFocusType.get(focusLabel)) {
				// prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(nodeId).setFocus();
				// }
				numberOfAllFocusNodes += prefixTree.allNodesOfFocusType.get(focusLabel).size();
			}

			// STAT Of the DB START
			double avgOutDegreeOfFocusNodes = DummyFunctions.getAvgOutDegreeOfFocusNodes(dataGraph, allFocusNodes,
					numberOfAllFocusNodes);
			// STAT Of the DB END

			Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;
			if (DummyProperties.debugMode) {
				System.out.println("numberOfAllFocusNodes: " + numberOfAllFocusNodes);
			}

			// generating the prefix tree for G0
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeRootNode = prefixTree.generatePrefixTreeForG0();

			// CorrectnessChecking.checkingDownwardProperty(prefixTreeRootNode);

			double findTopkStart = System.nanoTime();

			TopKHandler.findTopK(topKFrequentPatterns, prefixTree, k, prefixTreeRootNode, threshold);
			double findTopkEnd = System.nanoTime();
			double findTopkDuration = (findTopkEnd - findTopkStart) / 1e6;

			double prefixTreeGenerationTimeEnd = System.nanoTime();
			double prefixTreeGenerationDuration = (prefixTreeGenerationTimeEnd - startTime) / 1e6;

			System.out.println("prefixTreeGenerationTime: " + prefixTreeGenerationDuration + " miliseconds.");
			System.out.println("findTopkTime: " + findTopkDuration + " miliseconds.");

			int numberOfAllPatterns = prefixTree.bfsTraverse(prefixTree.emptyPTRootNode);

			if (DummyProperties.visualize) {
				for (int i = 1; i < maxAllowedEdges + 2; i++) {
					Visualizer.visualizeALevel(prefixTree, i, i);
				}
			}

			if (DummyProperties.visualize)
				Visualizer.visualizeTopK(topKFrequentPatterns);

			if (!DummyProperties.bigDataTestMode)
				TopKHandler.printTopK(topKFrequentPatterns);

			if (DummyProperties.debugMode)
				DebugHelper.printBiSimulatedPatterns(prefixTree);

			if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
				DebugHelper.printIsomorphicPatterns(prefixTree);

			if (DummyProperties.debugMode)
				DebugHelper.printGlobalCandidateSet(prefixTree);

			if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
				DebugHelper.printIfLevelIsNotConsistentWithNumberOfEdges(prefixTree);

			if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
				DebugHelper.printPatternWithDuplicateMatches(prefixTree);

			if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
				Visualizer.visualizePatternWithDuplicateMatches(prefixTree);

			ArrayList<InfoHolder> timeInfos = new ArrayList<InfoHolder>();
			timeInfos.add(new InfoHolder(0, "Focus", prefixTree.whatIsFocus));
			timeInfos.add(new InfoHolder(1, "Nodes", numberOfAllNodes));
			timeInfos.add(new InfoHolder(2, "Relationship", numberOfAllRelationships));
			timeInfos.add(new InfoHolder(3, "Distinct Labels", differentLabels.size()));
			timeInfos.add(new InfoHolder(4, "Distinct RelTypes", differentRelTypes.size()));
			timeInfos.add(new InfoHolder(5, "Average of Total Degrees", avgDegrees));
			timeInfos.add(new InfoHolder(6, "Average of Focus Out Degrees", avgOutDegreeOfFocusNodes));
			timeInfos.add(new InfoHolder(7, "Patterns", numberOfAllPatterns));
			timeInfos.add(new InfoHolder(8, "Total All Matches", prefixTree.numberOfTotalAllMatches));
			timeInfos.add(new InfoHolder(9, "Focus Nodes", numberOfAllFocusNodes));
			timeInfos.add(new InfoHolder(10, "Max Hops", maxAllowedHops));
			timeInfos.add(new InfoHolder(11, "Max Edges", maxAllowedEdges));
			timeInfos.add(new InfoHolder(12, "PrefixTree Generation Time", prefixTreeGenerationDuration));
			timeInfos.add(new InfoHolder(13, "Find Topk Time", findTopkDuration));
			timeInfos.add(new InfoHolder(14, "k", k));
			timeInfos.add(new InfoHolder(15, "Threshold", threshold));

			timeInfos.add(new InfoHolder(16, "BiSim Checking Time", prefixTree.getDurationOfBiSimChecking()));
			timeInfos.add(new InfoHolder(17, "Number Of BiSim Checking Request",
					prefixTree.getNumberOfBiSimCheckingRequest()));
			timeInfos.add(
					new InfoHolder(18, "Number Of Real BiSim Checking", prefixTree.getNumberOfRealBiSimChecking()));
			timeInfos.add(new InfoHolder(19, "Creation Of New PrefixTree Node",
					prefixTree.getDurationOfNewPrefixTreeGeneration()));
			timeInfos.add(
					new InfoHolder(20, "Creation/Checking Of Dangling Nodes", prefixTree.danglingCreationDuration));
			timeInfos.add(new InfoHolder(21, "Number of Dangling Nodes", prefixTree.numberOfDangling));
			timeInfos.add(new InfoHolder(22, "WindowSize", DummyProperties.WINDOW_SIZE));
			timeInfos.add(new InfoHolder(23, "WindowMode", DummyProperties.windowMode));
			timeInfos.add(new InfoHolder(24, "Num. of support computations", prefixTree.getNumberOfComputeSupport()));
			timeInfos.add(new InfoHolder(25, "Support computational time", prefixTree.getDurationOfComputeSupport()));
			timeInfos.add(new InfoHolder(26, "checkValidityAtLeastOneMatchForEachPatternNodeDuration",
					prefixTree.checkValidityAtLeastOneMatchForEachPatternNodeDuration));

			TimeLogger.LogTime("biSimBatch_" + (DummyProperties.windowMode ? "Win" : "Inc") + "_"
					+ DummyProperties.WINDOW_SIZE + ".txt", true, timeInfos);

			tx1.success();

		} catch (Exception e) {
			e.printStackTrace();
		}

		dataGraph.shutdown();

	}

	public static void main(String[] args) throws Exception {
		StaticGraphTopKBiSimOpt staticGraphTopK = new StaticGraphTopKBiSimOpt(args);
		staticGraphTopK.findStaticTopK();
	}
}
