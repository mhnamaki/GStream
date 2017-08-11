package src.alg1.topkMonitoring;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import com.google.common.collect.MinMaxPriorityQueue;

import src.alg1.prefixTree.PrefixTreeAlg1;

import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;
import src.utilities.Bitmap;
import src.utilities.CorrectnessChecking;
import src.utilities.DebugHelper;
import src.utilities.Dummy;
import src.utilities.InfoHolder;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;
import src.utilities.SupportComparator;
import src.utilities.SupportHelper;
import src.utilities.TimeLogger;
import src.utilities.TopKHandler;
import src.utilities.Visualizer;
import src.utilities.WinMine;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class SnapshotTopkMonitoring {

	public String focusSetPath;
	public int maxAllowedHops;
	public int maxAllowedEdges;
	public String dataGraphPath;
	public String dateFormat;
	public boolean debugMode;
	public int k;
	public GraphDatabaseService dataGraph;
	public double threshold;
	public String deltaEFileOrFiles;
	public int numberOfTransactionInASnapshot = 10;
	public int numberOfSnapshots;
	public int numberOfAllFocusNodes = 0;
	public int numberOfIgnoranceInitEdges = 0;
	public Bitmap bitMap;
	public MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns;
	private boolean windowMode = false;
	private int windowSizeL = 2;
	private int startingWindow = 0;
	private int endingWindow = 1; // 0, 1, 2
	public PrefixTreeAlg1 prefixTree;

	public void snapshotTopkMonitor() throws Exception {

		Dummy.DummyProperties.debugMode = debugMode;
		DummyProperties.incMode = false;

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		topKFrequentPatterns = MinMaxPriorityQueue.orderedBy(new SupportComparator()).maximumSize(k).create();

		// in order to get the lowerbound:
		// topKFrequentPatterns.peekLast()

		bitMap = new Bitmap();

		Dummy.DummyProperties.NUMBER_OF_SNAPSHOTS = numberOfSnapshots;

		Transaction tx1 = dataGraph.beginTx();

		// STAT Of the DB START
		int numberOfAllNodes = Dummy.DummyFunctions.getNumberOfAllNodes(dataGraph);
		int numberOfAllRelationshipsG0 = Dummy.DummyFunctions.getNumberOfAllRels(dataGraph);
		HashSet<String> differentLabels = Dummy.DummyFunctions.getDifferentLabels(dataGraph);
		HashSet<String> differentRelTypes = Dummy.DummyFunctions.getDifferentRelType(dataGraph);

		double avgDegrees = Dummy.DummyFunctions.getAvgOutDegrees(dataGraph);
		// STAT Of the DB END

		// creation of prefixtree and intializing it
		// with a root node and focus nodes
		prefixTree = new PrefixTreeAlg1(this);

		// because in future we want to check if in prev-d-hops any focus
		// nodes exist we maintain their id's here.
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
		double avgG0OutDegreeOfFocusNodes = DummyFunctions.getAvgOutDegreeOfFocusNodes(dataGraph, allFocusNodes,
				numberOfAllFocusNodes);
		// STAT Of the DB END

		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;

		// generating the prefix tree for G0
		double g0PrefixTreeGenerationStartTime = System.nanoTime();
		PrefixTreeNode<IPrefixTreeNodeData> prefixTreeRootNode = prefixTree.generatePrefixTreeForG0();
		double g0PrefixTreeGenerationDuration = ((System.nanoTime() - g0PrefixTreeGenerationStartTime) / 1e6);

		// just for the sake of test
		CorrectnessChecking.checkingDownwardProperty(prefixTreeRootNode);

		TopKHandler.findTopK(topKFrequentPatterns, prefixTree, k, prefixTreeRootNode, threshold);

		int numberOfAllG0Patterns = prefixTree.bfsTraverse(prefixTreeRootNode);
		int initNumberOfTotalAllMatches = prefixTree.numberOfTotalAllMatches;

		long numberOfIsoCheckingRequestG0 = prefixTree.getNumberOfIsoCheckingRequest();
		long numberOfRealIsoCheckingG0 = prefixTree.getNumberOfRealIsoChecking();
		double isoCheckingTimeG0 = prefixTree.getDurationOfIsoChecking();
		double newPrefixTreeGenerationG0 = prefixTree.getDurationOfNewPrefixTreeGeneration();
		int numberOfSupportComputationsG0 = prefixTree.getNumberOfComputeSupport();
		double computaionalSupportTimeG0 = prefixTree.getDurationOfComputeSupport();

		prefixTree.resetDurationOfIsoChecking();
		prefixTree.resetNumberOfIsoChecking();
		prefixTree.resetNumberOfComputeSupport();
		prefixTree.resetDurationOfComputeSupport();
		prefixTree.resetDurationOfNewPrefixTreeGeneration();

		RoaringBitmap[] patternIndexByNodeIdBitMap = bitMap.storeAllPatternIndexByNodeId(prefixTree);

		SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaEFileOrFiles, dateFormat);

		DummyProperties.incMode = true;

		double updateNeighborhoodStartTime = 0.0d;
		double updateNeighborhoodDuration = 0.0d;

		int allEdgesUpdates = -1;
		int allEdgesForIgn = -1;
		int addedEdges = 0;
		int removedEdges = 0;
		int nonAffectedInsertedEdges = 0;
		int nonAffectedDeletedEdges = 0;
		double handlingDataStreamDuration = 0;
		ArrayList<Double> insertionResponseTimes = new ArrayList<Double>();
		ArrayList<Double> deletionResponseTimes = new ArrayList<Double>();

		Double allInsertionResponseTimes = 0d;
		Double allDeletionResponseTimes = 0d;

		double updateBitMapTimeStart = 0d;
		Double updateBitMapTimeDuration = 0d;

		Node srcNode = null;
		Node destNode = null;
		String srcLabel = null;
		String destLabel = null;
		Integer srcNodeId = null;
		Integer destNodeId = null;
		Integer numberOfAffectedPatterns = 0;
		Integer numberOfAffectedPatternsWithoutRecheck = 0;

		Double allEdgesResponseTime = 0.0d;
		ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> affectedPatterns = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();
		int prevSnapshot = 0;

		double winMineStart = 0d;
		double winMineDuraion = 0d;

		double carryOverStart = 0d;
		double carryOverDuraion = 0d;

		for (int i = 0; i < numberOfTransactionInASnapshot * (numberOfSnapshots - 1); i++) {

			// because we started from G0 (so new E makes other snapshots)
			int snapshot = (i / numberOfTransactionInASnapshot) + 1;

			StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

			if (nextStreamEdge == null)
				break;

			if (windowMode) {
				winMineStart = System.nanoTime();
				if (snapshot > endingWindow) {
					WinMine.winMine(prefixTree, snapshot);
					startingWindow++;
					endingWindow++;
					if (DummyProperties.debugMode)
						System.out.println("window slid: starting:" + startingWindow + " -> ending:" + endingWindow);
				}
				winMineDuraion += ((System.nanoTime() - winMineStart) / 1e6);
			}

			if (prevSnapshot != snapshot) {
				carryOverStart = System.nanoTime();
				SupportHelper.carryOverTheSupport(prefixTree, snapshot, startingWindow);
				prevSnapshot = snapshot;
				carryOverDuraion += ((System.nanoTime() - carryOverStart) / 1e6);
			}

			// TODO: should be removed, just for the sake of insert
			// experiments
			// if (!nextStreamEdge.isAdded()) {
			// //i = Math.max(i--, 0);
			// continue;
			// } else {
			// allEdgesForIgn++;
			// }

			allEdgesUpdates++;

			if (allEdgesUpdates < numberOfIgnoranceInitEdges) {
				i = -1;
				continue;
			}

			srcNodeId = (int) nextStreamEdge.getSourceNode();
			destNodeId = (int) nextStreamEdge.getDestinationNode();
			srcNode = dataGraph.getNodeById(srcNodeId);
			destNode = dataGraph.getNodeById(destNodeId);
			srcLabel = (prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.containsKey(srcNodeId)
					? prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).nodeLabel
					: dataGraph.getNodeById(srcNodeId).getLabels().iterator().next().name().toString());

			destLabel = (prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.containsKey(destNodeId)
					? prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(destNodeId).nodeLabel
					: dataGraph.getNodeById(destNodeId).getLabels().iterator().next().name().toString());

			if (DummyProperties.debugMode) {
				System.out.println();
				System.out.println();
				System.out.println();
			}

			if ((i % 100000) == 0) {
				tx1.success();
				tx1.close();
				// System.out.println("tx is commited in " + i + "-th
				// transaction!");
				tx1 = dataGraph.beginTx();

			}

			if (DummyProperties.debugMode)
				System.out.println(allEdgesUpdates + ": " + " nextStreamEdge: " + srcLabel + "_" + srcNodeId + " -> "
						+ destLabel + "_" + destNodeId + " => isAdded? " + nextStreamEdge.isAdded());

			// handling insertion/deletion of an edge and degree indexing
			// maintenance
			double handlingDataStreamStartTime = System.nanoTime();
			handlingDataStream(nextStreamEdge, prefixTree, srcNode, destNode, srcNodeId, destNodeId);
			handlingDataStreamDuration += ((System.nanoTime() - handlingDataStreamStartTime) / 1e6);

			if (nextStreamEdge.isAdded())
				addedEdges++;
			else
				removedEdges++;

			double starttime = System.nanoTime();

			updateNeighborhoodStartTime = System.nanoTime();
			// TODO: could be faster if we used previous indexing information
			prefixTree.labelAdjacencyIndexer.updateNeighborhood(dataGraph, srcNodeId, destNodeId,
					nextStreamEdge.getRelationshipType(), maxAllowedHops, nextStreamEdge.isAdded());
			updateNeighborhoodDuration += ((System.nanoTime() - updateNeighborhoodStartTime) / 1e6);

			boolean srcIsAFocus = false;
			srcIsAFocus = allFocusNodes.contains(srcNodeId);

			boolean destIsAFocus = false;
			if (srcLabel != destLabel || !srcIsAFocus) {
				destIsAFocus = allFocusNodes.contains(destNodeId);
			}

			// if no incoming edge and this is not a focus node
			if ((prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).inDegree == 0 && !srcIsAFocus)
					&& (prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).outDegree == 0
							&& !destIsAFocus)) {
				if (nextStreamEdge.isAdded()) {
					nonAffectedInsertedEdges++;
					allInsertionResponseTimes += ((System.nanoTime() - starttime) / 1e6);
				} else {
					nonAffectedDeletedEdges++;
					allDeletionResponseTimes += ((System.nanoTime() - starttime) / 1e6);
				}

				allEdgesResponseTime += ((System.nanoTime() - starttime) / 1e6);
				// System.out.println("src has no incoming and is not focus!");
				continue;
			}

			findAffectedPatterns(affectedPatterns, prefixTree, srcNodeId, destNodeId,
					patternIndexByNodeIdBitMap[srcNodeId], patternIndexByNodeIdBitMap[destNodeId],
					nextStreamEdge.isAdded());

			// if it didn't exist before in the prefix-tree(T) and it's not
			// a
			// focus node it doesn't affect our current prefix-tree
			if (!srcIsAFocus && affectedPatterns.size() == 0) {
				if (nextStreamEdge.isAdded()) {
					nonAffectedInsertedEdges++;
					allInsertionResponseTimes += ((System.nanoTime() - starttime) / 1e6);
				} else {
					nonAffectedDeletedEdges++;
					allDeletionResponseTimes += ((System.nanoTime() - starttime) / 1e6);
				}
				// System.out.println("src is not a focus and it's not in
				// previous extendable patterns!");
				allEdgesResponseTime += ((System.nanoTime() - starttime) / 1e6);
				continue;
			}

			numberOfAffectedPatterns += affectedPatterns.size();

			sortPatternsInAscending(affectedPatterns);

			HashSet<Integer> newCreatedOrTouchedPTNodes = new HashSet<Integer>();

			if (nextStreamEdge.isAdded()) {

				for (int p = 0; p < affectedPatterns.size(); p++) {

					boolean isChecked = false;
					if (!newCreatedOrTouchedPTNodes
							.contains(affectedPatterns.get(p).getData().getPatternPrefixTreeNodeIndex())) {

						prefixTree.expandForNewInsertedEdgeOutgoing(srcNode, srcNodeId, srcLabel, destNode, destNodeId,
								destLabel, srcIsAFocus, destIsAFocus, affectedPatterns.get(p),
								newCreatedOrTouchedPTNodes, snapshot, threshold, topKFrequentPatterns,
								nextStreamEdge.getRelationshipType());
						isChecked = true;
					} 
					
					if (!newCreatedOrTouchedPTNodes
							.contains(affectedPatterns.get(p).getData().getPatternPrefixTreeNodeIndex())) {

						prefixTree.expandForNewInsertedEdgeIncoming(srcNode, srcNodeId, srcLabel, destNode, destNodeId,
								destLabel, srcIsAFocus, destIsAFocus, affectedPatterns.get(p),
								newCreatedOrTouchedPTNodes, snapshot, threshold, topKFrequentPatterns,
								nextStreamEdge.getRelationshipType());
						isChecked = true;
					} 
					
					if(!isChecked) {
						numberOfAffectedPatternsWithoutRecheck++;
					}

				}
				



				}
				updateBitMapTimeStart = System.nanoTime();
				// update bitmap:
				// here we know just we had just adding
				for (Integer patternId : newCreatedOrTouchedPTNodes) {
					// TODO: it's not very efficient(we could directly
					// change)
					bitMap.storeOnePatternIndexByNodeId(prefixTree, patternId);
				}
				updateBitMapTimeDuration += ((System.nanoTime() - updateBitMapTimeStart) / 1e6);

				insertionResponseTimes.add((System.nanoTime() - starttime) / 1e6);
				allEdgesResponseTime += ((System.nanoTime() - starttime) / 1e6);
				allInsertionResponseTimes += ((System.nanoTime() - starttime) / 1e6);

			} else {
				// System.out.println();
				for (int p = 0; p < affectedPatterns.size(); p++) {
					// System.out.println("affectedPatterns: " +
					// affectedPatterns.get(p).getData().getMappedGraphString());

					if (!newCreatedOrTouchedPTNodes
							.contains(affectedPatterns.get(p).getData().getPatternPrefixTreeNodeIndex())) {
						prefixTree.shrinkForNewDeletedEdge(srcNode, destNode, srcIsAFocus, destIsAFocus,
								affectedPatterns.get(p), newCreatedOrTouchedPTNodes, snapshot, threshold,
								topKFrequentPatterns, nextStreamEdge.getRelationshipType());
					} else {
						numberOfAffectedPatternsWithoutRecheck++;
					}
				}

				deletionResponseTimes.add((System.nanoTime() - starttime) / 1e6);
				allEdgesResponseTime += ((System.nanoTime() - starttime) / 1e6);
				allDeletionResponseTimes += ((System.nanoTime() - starttime) / 1e6);
			}

			// System.out.println("traversal after stream edge: " +
			// nextStreamEdge.getSourceNode() + " -> "
			// + nextStreamEdge.getDestinationNode() + " => isAdded? " +
			// nextStreamEdge.isAdded());
			// prefixTree.bfsTraverse(prefixTree.emptyPTRootNode);

		}

	System.out.println("final traversal");

	int numberOfAllPatterns = prefixTree.bfsTraverse(prefixTree.emptyPTRootNode);

	// the order should be like this:
	// we should first visualize then printTopk
	if(DummyProperties.debugMode)Visualizer.visualizeTopK(topKFrequentPatterns);

	if(DummyProperties.debugMode)DebugHelper.printIsomorphicPatterns(prefixTree);

	TopKHandler.printTopK(topKFrequentPatterns);

	double avgGTOutDegreeOfFocusNodes = DummyFunctions.getAvgOutDegreeOfFocusNodes(dataGraph, allFocusNodes,
			numberOfAllFocusNodes);
	int numberOfAllRelationshipsGT = Dummy.DummyFunctions.getNumberOfAllRels(dataGraph);

	ArrayList<InfoHolder> timeInfos = new ArrayList<InfoHolder>();

	timeInfos.add(new InfoHolder(0,"Nodes",numberOfAllNodes));

	timeInfos.add(new InfoHolder(1,"G0 Relationships",numberOfAllRelationshipsG0));timeInfos.add(new InfoHolder(2,"GT Relationships",numberOfAllRelationshipsGT));

	timeInfos.add(new InfoHolder(3,"Distinct Labels",differentLabels.size()));

	timeInfos.add(new InfoHolder(4,"Distinct RelTypes",differentRelTypes.size()));

	timeInfos.add(new InfoHolder(5,"Average of Total Degrees",avgDegrees));

	timeInfos.add(new InfoHolder(6,"Average of G0 Focus Out Degrees",avgG0OutDegreeOfFocusNodes));

	timeInfos.add(new InfoHolder(7,"Average of GT Focus Out Degrees",avgGTOutDegreeOfFocusNodes));

	timeInfos.add(new InfoHolder(8,"k",k));

	timeInfos.add(new InfoHolder(9,"Threshold",threshold));

	timeInfos.add(new InfoHolder(10,"Requested Transactions In A Snapshot",numberOfTransactionInASnapshot));

	timeInfos.add(new InfoHolder(11,"Requested Snapshots",numberOfSnapshots));

	timeInfos.add(new InfoHolder(12,"Diameter",maxAllowedHops));

	timeInfos.add(new InfoHolder(13,"Max Edges",maxAllowedEdges));

	timeInfos.add(new InfoHolder(14,"Focus Nodes",numberOfAllFocusNodes));

	timeInfos.add(new InfoHolder(15,"All Patterns",numberOfAllPatterns));timeInfos.add(new InfoHolder(16,"All G0 Patterns",numberOfAllG0Patterns));timeInfos.add(new InfoHolder(17,"All Inc Patterns",(numberOfAllPatterns-numberOfAllG0Patterns)));

	timeInfos.add(new InfoHolder(18,"TotalAllMatches",prefixTree.numberOfTotalAllMatches));timeInfos.add(new InfoHolder(19,"TotalAllMatches G0",initNumberOfTotalAllMatches));timeInfos.add(new InfoHolder(20,"TotalAllMatches Just Inc",(prefixTree.numberOfTotalAllMatches-initNumberOfTotalAllMatches)));

	timeInfos.add(new InfoHolder(21,"PrefixTree G0 Generation Time",g0PrefixTreeGenerationDuration));

	timeInfos.add(new InfoHolder(22,"All edges response time",allEdgesResponseTime));

	timeInfos.add(new InfoHolder(23,"Total all Insertion Response Time",allInsertionResponseTimes));

	timeInfos.add(new InfoHolder(24,"Total all Deletion Response Time",allDeletionResponseTimes));

	timeInfos.add(new InfoHolder(25,"Total Insertion AFF Response Time",DummyFunctions.getTotalSumOfDoubleArray(insertionResponseTimes)));

	timeInfos.add(new InfoHolder(26,"Total Deletion AFF Response Time",DummyFunctions.getTotalSumOfDoubleArray(deletionResponseTimes)));

	timeInfos.add(new InfoHolder(27,"Avg Insertion AFF Response Time",DummyFunctions.getAverageOfDoubleArray(insertionResponseTimes)));

	timeInfos.add(new InfoHolder(28,"Avg Deletion AFF Response Time",DummyFunctions.getAverageOfDoubleArray(deletionResponseTimes)));

	timeInfos.add(new InfoHolder(29,"Total deltaE",(addedEdges+removedEdges)));

	timeInfos.add(new InfoHolder(30,"All inserted edges",addedEdges));

	timeInfos.add(new InfoHolder(31,"All deleted edges",removedEdges));

	timeInfos.add(new InfoHolder(32,"affected inserted edges",(addedEdges-nonAffectedInsertedEdges)));

	timeInfos.add(new InfoHolder(33,"affected deleted edges",(removedEdges-nonAffectedDeletedEdges)));

	timeInfos.add(new InfoHolder(34,"Affected Patterns (Parents To be Expanded)",numberOfAffectedPatterns));

	timeInfos.add(new InfoHolder(35,"Affected Patterns Without Recheck",numberOfAffectedPatternsWithoutRecheck));

	timeInfos.add(new InfoHolder(36,"Total physical edge insertion/deletion time",handlingDataStreamDuration));

	timeInfos.add(new InfoHolder(37,"Iso Checking Time G0",isoCheckingTimeG0));timeInfos.add(new InfoHolder(38,"Number Of Iso Checking Request G0",numberOfIsoCheckingRequestG0));timeInfos.add(new InfoHolder(38,"Number Of Real Iso Checking G0",numberOfRealIsoCheckingG0));timeInfos.add(new InfoHolder(39,"Creation Of New PrefixTree Node G0",newPrefixTreeGenerationG0));timeInfos.add(new InfoHolder(40,"Num. of support computations G0",numberOfSupportComputationsG0));timeInfos.add(new InfoHolder(41,"Support computational time G0",computaionalSupportTimeG0));

	timeInfos.add(new InfoHolder(42,"Iso Checking Time Inc",prefixTree.getDurationOfIsoChecking()));timeInfos.add(new InfoHolder(43,"Number Of Iso Checking Request Inc",prefixTree.getNumberOfIsoCheckingRequest()));timeInfos.add(new InfoHolder(43,"Number Of Real Iso Checking Inc",prefixTree.getNumberOfRealIsoChecking()));timeInfos.add(new InfoHolder(44,"Creation Of New PrefixTree Node Inc",prefixTree.getDurationOfNewPrefixTreeGeneration()));timeInfos.add(new InfoHolder(45,"Num. of support computations Inc",prefixTree.getNumberOfComputeSupport()));timeInfos.add(new InfoHolder(46,"Support computational time Inc",prefixTree.getDurationOfComputeSupport()));

	timeInfos.add(new InfoHolder(47,"Update neighborhood info",updateNeighborhoodDuration));

	timeInfos.add(new InfoHolder(48,"Exp > checkValidityAtLeastOneMatchForEachPatternNode",prefixTree.checkValidityAtLeastOneMatchForEachPatternNodeDuration));

	timeInfos.add(new InfoHolder(49,"Exp > processQueueNode",prefixTree.processQueueNodeDuration));timeInfos.add(new InfoHolder(50,"WindowSize",DummyProperties.WINDOW_SIZE));timeInfos.add(new InfoHolder(51,"WindowMode",DummyProperties.windowMode));timeInfos.add(new InfoHolder(52,"carryOverDuraion",carryOverDuraion));timeInfos.add(new InfoHolder(53,"winMineDuraion",winMineDuraion));

	TimeLogger.LogTime("disn_"+(DummyProperties.windowMode?"Win":"Inc")+"_"+DummyProperties.WINDOW_SIZE+".txt",true,timeInfos);

	tx1.success();tx1.close();

	dataGraph.shutdown();
	}

	public SnapshotTopkMonitoring(String[] args) throws Exception {
		// HashMap<String, String> nodeKeyProp = new HashMap<String, String>();

		// nodeKeyProp.put("Author", "Name");
		// nodeKeyProp.put("Paper", "Index");
		// nodeKeyProp.put("Publication_Venue", "Venue_Name");

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
			} else if (args[i].equals("-k")) {
				k = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-threshold")) {
				threshold = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-deltaEFileOrFiles")) {
				deltaEFileOrFiles = args[++i];
			} else if (args[i].equals("-numberOfTransactionInASnapshot")) {
				numberOfTransactionInASnapshot = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfSnapshots")) {
				numberOfSnapshots = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			} else if (args[i].equals("-numberOfIgnoranceInitEdges")) {
				numberOfIgnoranceInitEdges = Integer.parseInt(args[++i]);
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
			System.out.println("-focusSetPath  " + focusSetPath + ", -maxAllowedHops:" + maxAllowedHops
					+ ", -maxAllowedEdges  " + maxAllowedEdges + ", -dataGraphPath:" + dataGraphPath + ", -k  " + k);
		}

		if (maxAllowedEdges < maxAllowedHops) {
			throw new Exception(" maxAllowedEdges < maxAllowedHops : " + maxAllowedEdges + " < " + maxAllowedHops);
		}

		Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
		if (newDGPath == null) {
			throw new Exception("newDGPath is null!");
		}

		dataGraphPath = newDGPath.toString();

	}

	public SnapshotTopkMonitoring(String focusSetPath, int maxHops, int maxEdges, String dataGraphPath,
			boolean debugMode, int k, double threshold, String deltaEFileOrFiles, int numberOfTransactionInASnapshot,
			int numberOfSnapshots, String dateFormat, int numberOfIgnoranceInitEdges, boolean windowMode,
			int windowSizeL) {

		this.focusSetPath = focusSetPath;
		this.maxAllowedHops = maxHops;
		this.maxAllowedEdges = maxEdges;
		this.dataGraphPath = dataGraphPath;
		this.debugMode = debugMode;
		this.k = k;
		this.threshold = threshold;
		this.deltaEFileOrFiles = deltaEFileOrFiles;
		this.numberOfTransactionInASnapshot = numberOfTransactionInASnapshot;
		this.numberOfSnapshots = numberOfSnapshots;
		this.dateFormat = dateFormat;
		this.numberOfIgnoranceInitEdges = numberOfIgnoranceInitEdges;
		this.windowMode = windowMode;
		DummyProperties.windowMode = windowMode;
		this.windowSizeL = windowSizeL;
		DummyProperties.WINDOW_SIZE = windowSizeL;
		this.endingWindow = windowSizeL - 1;

	}

	public static void main(String[] args) throws Exception {
		SnapshotTopkMonitoring snapshotTopkMonitoring = new SnapshotTopkMonitoring(args);
		snapshotTopkMonitoring.snapshotTopkMonitor();
	}

	private static void sortPatternsInAscending(ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> affectedPatterns) {
		// TODO: may be we need just parent levels because if some node
		// is in the children it should be also in the parent so we can
		// reach from it's parent to that child

		// sorting patterns based on the number of nodes/edges
		Collections.sort(affectedPatterns, new Comparator<PrefixTreeNode<IPrefixTreeNodeData>>() {
			@Override
			public int compare(PrefixTreeNode<IPrefixTreeNodeData> o1, PrefixTreeNode<IPrefixTreeNodeData> o2) {
				if (o1.getLevel() > o2.getLevel())
					return 1;
				else if (o1.getLevel() < o2.getLevel())
					return -1;
				else if (o1.getData().getPatternGraph().vertexSet().size() == o2.getData().getPatternGraph().vertexSet()
						.size())
					return 0;
				// means that they are equal to each other
				else if (o1.getData().getPatternGraph().vertexSet().size() > o2.getData().getPatternGraph().vertexSet()
						.size())
					return 1;
				else
					return -1;
			}
		});
	}

	private void findAffectedPatterns(ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> affectedPatterns,
			IPrefixTree prefixTree, int srcNodeId, int destNodeId, RoaringBitmap srcRoaringBitmap,
			RoaringBitmap destRoaringBitmap, boolean isAdded) {

		affectedPatterns.clear();

		if (isAdded) {
			srcRoaringBitmap.forEach(new IntConsumer() {
				@Override
				public void accept(int value) {
					// if (prefixTree.prefixTreeNodeIndex.get(value) == null
					// ||
					// prefixTree.prefixTreeNodeIndex.get(value).getData().matchNodes.patternNodeOfNeo4jNode
					// .get(srcNodeId) == null) {
					// System.err.println("prefixTree.prefixTreeNodeIndex.data:
					// "
					// + prefixTree.prefixTreeNodeIndex.get(value).getData() + "
					// srcNodeId: " + srcNodeId
					// + " destNodeId:" + destNodeId + " IndexValue:" + value);
					// }
					IPrefixTreeNodeData prefixTNodeData = prefixTree.getPrefixTreeNodeIndex().get(value).getData();
					if (prefixTNodeData.getPatternGraph().edgeSet().size() < maxAllowedEdges) {
						for (PatternNode patternNode : prefixTNodeData.getMatchedNodes().getPatternNodeOfNeo4jNode()
								.get(srcNodeId)) {

							if (prefixTNodeData.getStepsFromRootOfPatternNodes().get(patternNode) < maxAllowedHops) {
								// if (prefixTree.prefixTreeNodeIndex.get(value)
								// ==
								// null
								// ||
								// prefixTree.prefixTreeNodeIndex.get(value).getData()
								// == null) {
								// System.out.println();
								// }
								affectedPatterns.add(prefixTree.getPrefixTreeNodeIndex().get(value));
								break;
							}
						}
					}
				}
			});
			destRoaringBitmap.forEach(new IntConsumer() {
				@Override
				public void accept(int value) {
					IPrefixTreeNodeData prefixTNodeData = prefixTree.getPrefixTreeNodeIndex().get(value).getData();
					if (prefixTNodeData.getPatternGraph().edgeSet().size() < maxAllowedEdges) {
						for (PatternNode patternNode : prefixTNodeData.getMatchedNodes().getPatternNodeOfNeo4jNode()
								.get(destNodeId)) {
							if (prefixTNodeData.getStepsFromRootOfPatternNodes().get(patternNode) < maxAllowedHops) {
								affectedPatterns.add(prefixTree.getPrefixTreeNodeIndex().get(value));
								break;
							}
						}
					}
				}
			});
		} else {
			RoaringBitmap.and(srcRoaringBitmap, destRoaringBitmap).forEach(new IntConsumer() {

	@Override
	public void accept(int value) {

		// if (prefixTree.prefixTreeNodeIndex.get(value) == null) {
		// System.err.println("DELETED BITMAP:
		// prefixTree.prefixTreeNodeIndex.data: "
		// + prefixTree.prefixTreeNodeIndex.get(value).getData() + "
		// srcNodeId: " + srcNodeId
		// + " destNodeId:" + destNodeId + " IndexValue:" + value);
		// }

		IPrefixTreeNodeData tempData = prefixTree.getPrefixTreeNodeIndex().get(value).getData();
		boolean isFound = false;
		// for each source pattern node
		for (PatternNode srcPatternNode : tempData.getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId)) {

			// if that source can go further
			if (tempData.getStepsFromRootOfPatternNodes().get(srcPatternNode) < maxAllowedHops) {
				// if
				// (tempData.matchNodes.patternNodeOfNeo4jNode.get(destNodeId)
				// == null) {
				// System.err.println("it seems that bitmap is not
				// updated: src:" + srcNodeId + " , dest:"
				// + destNodeId);
				//
				// // because of bitmap it should always have a
				// // value otherwise bitmap is not updated
				// // becasue this is "and" of two bitmaps
				// }

				for (PatternNode destPatternNode : tempData.getMatchedNodes().getPatternNodeOfNeo4jNode()
						.get(destNodeId)) {
					// if in this pattern srcNode->destNode
					if (tempData.getPatternGraph().containsEdge(srcPatternNode, destPatternNode)) {
						// if
						// (prefixTree.prefixTreeNodeIndex.get(value)
						// == null
						// ||
						// prefixTree.prefixTreeNodeIndex.get(value).getData()
						// == null) {
						// System.out.println();
						// }
						affectedPatterns.add(prefixTree.getPrefixTreeNodeIndex().get(value));
						isFound = true;
						break;
					}
				}

				if (isFound)
					break;

			}
		}
	}

	});}}

	private StreamEdge handlingDataStream(StreamEdge nextStreamEdge, PrefixTreeAlg1 prefixTree, Node srcNode,
			Node destNode, Integer srcNodeId, Integer destNodeId) {

		// neighborhood indexing by demand for memory usage
		prefixTree.getLabelAdjacencyIndexer().checkForExistenceInNeighborhoodIndex(prefixTree, srcNodeId, destNodeId,
				srcNode, destNode);

		String relationship = nextStreamEdge.getRelationshipType();
		// then add/remove new relationship between them
		if (nextStreamEdge.isAdded()) {

			// TODO: different rels between two nodes not supported here
			if (!prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(srcNodeId).nextNodeIds.contains(destNodeId)) {
				srcNode.createRelationshipTo(destNode, RelationshipType.withName(relationship));
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(srcNodeId).incrementOutDegree();
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(destNodeId).incrementInDegree();
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(srcNodeId).addNextNode(destNodeId);
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(destNodeId).addPrevNode(srcNodeId);
				prefixTree.labelAdjacencyIndexer.relTypeOfSrcAndDest.putIfAbsent(srcNodeId,
						new HashMap<Integer, String>());
				prefixTree.labelAdjacencyIndexer.relTypeOfSrcAndDest.get(srcNodeId).put(destNodeId, relationship);
			}
		} else {
			boolean existed = false;
			// for-loop over less degree node
			if (prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos
					.get(srcNodeId).outDegree <= prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos
							.get(destNodeId).inDegree) {

				for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
						RelationshipType.withName(relationship))) {
					if (rel.getEndNode().getId() == destNode.getId()) {
						rel.delete();
						existed = true;
					}
				}

			} else {

				for (Relationship rel : destNode.getRelationships(Direction.INCOMING,
						RelationshipType.withName(relationship))) {
					if (rel.getStartNode().getId() == srcNode.getId()) {
						rel.delete();
						existed = true;
					}
				}

			}
			if (existed) {
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(srcNodeId).decrementOutDegree();
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(destNodeId).decrementInDegree();

				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(srcNodeId).removeNextNode(destNodeId);
				prefixTree.labelAdjacencyIndexer.dataGraphNodeInfos.get(destNodeId).removePrevNode(srcNodeId);

				if (prefixTree.labelAdjacencyIndexer.relTypeOfSrcAndDest.get(srcNodeId) != null)
					prefixTree.labelAdjacencyIndexer.relTypeOfSrcAndDest.get(srcNodeId).remove(destNodeId);
			}
		}

		return nextStreamEdge;

	}

	// private static Node createNode(GraphDatabaseService dataGraph, Node
	// srcNode) {
	//
	// Node newNode =
	// dataGraph.createNode(srcNode.getLabels().iterator().next());
	//
	// Map<String, Object> keyValMap = srcNode.getAllProperties();
	// for (String key : keyValMap.keySet()) {
	// newNode.setProperty(key, keyValMap.get(key));
	// }
	//
	// return newNode;
	// }

	// private static Node searchForNode(Node sourceNode, HashMap<String,
	// String> nodeKeyProp,
	// GraphDatabaseService dataGraph) {
	//
	// Label lbl = sourceNode.getLabels().iterator().next();
	// ResourceIterator<Node> foundNodesItr = dataGraph.findNodes(lbl,
	// nodeKeyProp.get(lbl),
	// sourceNode.getProperty(nodeKeyProp.get(lbl)));
	// if (!foundNodesItr.hasNext()) {
	// return null;
	// } else {
	// Node tempNode = foundNodesItr.next();
	// if (foundNodesItr.hasNext()) {
	// System.err.println("multiple copies of one node!");
	// }
	// return tempNode;
	// }
	// }

}
