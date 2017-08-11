package src.alg1.prefixTreeBiSim;

import java.io.*;
import java.util.*;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import com.google.common.collect.MinMaxPriorityQueue;

import src.alg1.topkMonitoringBiSim.SnapshotTopkMonitoringBiSimAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.dualSimulation.gtar.BatDualSimulation;
import src.utilities.Bitmap;
import src.utilities.DefaultLabeledEdge;
import src.utilities.DualSimulationHandler;
import src.utilities.Dummy;
import src.utilities.Indexer;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;
import src.utilities.SupportComparator;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy.DummyProperties.PrefixTreeMode;

import org.jgrapht.GraphMapping;

//TODO: upperbound estimation should be filled before access.
//TODO: found all focus nodes should be filled by true whenever it found all focus because from there we have downward property

//TODO: when we want to create a new pattern we don't need to copy node matches from parent for source pattern node because source and target will be checked in the expansion time
//TODO: we can decrease memory usage if we use the number of some labels in d-hops instead of using the exact node ids.

//TODO: make sure that isMaximalFrequent, isFrequent, isValid, isVerfied, isVisited, MFP queue, top-k queue are up to date at each state of the program.
//TODO: make sure that top-k list/MFP list don't have duplicated items in them 

public class PrefixTreeAlg1BiSim implements IPrefixTree {
	SnapshotTopkMonitoringBiSimAlg1 snapshotTopkMonitoring;
	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	private int maxAllowedHops;
	private int maxAllowedEdges;
	private String dataGraphPath;
	GraphDatabaseService dataGraph;
	public static final boolean The_Focus_Node = true;
	public static final boolean FRESH_SOURCE = true;

	// when we initialize a new child, we should add it here also
	public HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>> prefixTreeNodeIndex = new HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>>();
	public int numberOfPatternsInPrefixTree = 0;

	// assumption: user give different types as focus.
	// assumption: user can give property key values to just select some of the
	// node with same type
	// String: should be nodeType and then all focus node candidates.
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	private boolean debugMode = false;
	String focusSetPath = null;
	public Indexer labelAdjacencyIndexer;

	// a queue for processing all the waiting new PT nodes.
	Queue<PrefixTreeNode<IPrefixTreeNodeData>> traversalQueue;

	// maintain the same node level for graph isomorphism checking
	ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> sameLevelPrefixTreeNodes;

	public PrefixTreeNode<IPrefixTreeNodeData> emptyPTRootNode = null;
	boolean goBackToPrev = false;
	public Bitmap bitmap;
	public int numberOfAllFocusNodes = 0;
	public int numberOfTotalAllMatches = 0;

	private double isoTimeStart = 0d;
	private double isoTimeDuration = 0d;
	private long numberOfIsoCheckingRequest = 0;
	private long numberOfRealIsoChecking = 0;

	private double creationOfNewPrefixTreeNodeStart = 0d;
	private double creationOfNewPrefixTreeNodeDuration = 0d;

	private int numberOfComputeSupport = 0;
	private double computeSupportDuration = 0d;

	private double checkValidityAtLeastOneMatchForEachPatternNodeStartTime = 0d;
	public double checkValidityAtLeastOneMatchForEachPatternNodeDuration = 0d;

	private double processQueueNodeStartTime = 0d;
	public double processQueueNodeDuration = 0d;

	public double algorithmStartTimeJustForFocusFinding = 0d;

	public PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> mfpPrefixTreeNodes = new PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>>(
			100, new SupportComparator());

	public PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> mipPrefixTreeNodes = new PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>>(
			100, new SupportComparator());

	MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns;

	public PrefixTreeAlg1BiSim(String[] args) throws Exception {

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
			}
		}

		if (focusSetPath == null || dataGraphPath == null || maxAllowedHops == 0) {
			throw new Exception("input parameters: focusSetPath, maxAllowedHops, dataGraphPath");
		} else {
			if (DummyProperties.debugMode)
				System.out.println("-focusSetPath  " + focusSetPath + ", -maxAllowedHops:" + maxAllowedHops);
		}

		DummyProperties.debugMode = debugMode;

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		try (Transaction tx1 = dataGraph.beginTx()) {

			emptyPTRootNode = initializePrefixTree();
			tx1.success();

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (DummyProperties.debugMode)
			System.out.println("focusSet size: " + focusLabelPropValSet.size());

	}

	public PrefixTreeAlg1BiSim(SnapshotTopkMonitoringBiSimAlg1 snapshotTopkMonitoring) throws Exception {
		this.snapshotTopkMonitoring = snapshotTopkMonitoring;
		this.focusSetPath = snapshotTopkMonitoring.focusSetPath;
		this.maxAllowedHops = snapshotTopkMonitoring.maxAllowedHops;
		this.maxAllowedEdges = snapshotTopkMonitoring.maxAllowedEdges;
		this.dataGraph = snapshotTopkMonitoring.dataGraph;
		this.debugMode = snapshotTopkMonitoring.debugMode;
		this.bitmap = snapshotTopkMonitoring.bitMap;
		this.topKFrequentPatterns = snapshotTopkMonitoring.topKFrequentPatterns;
		emptyPTRootNode = initializePrefixTree();
	}

	/**
	 * for static top-k finder
	 * 
	 * @param focusSetPath
	 * @param maxAllowedHops
	 * @param maxAllowedEdges
	 * @param dataGraph
	 * @param debugMode
	 * @param bitMap
	 * @throws Exception
	 */
	public PrefixTreeAlg1BiSim(String focusSetPath, int maxAllowedHops, int maxAllowedEdges,
			GraphDatabaseService dataGraph, boolean debugMode, Bitmap bitMap,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) throws Exception {

		this.focusSetPath = focusSetPath;
		this.maxAllowedHops = maxAllowedHops;
		this.maxAllowedEdges = maxAllowedEdges;
		this.dataGraph = dataGraph;
		this.debugMode = debugMode;
		this.bitmap = bitMap;
		this.topKFrequentPatterns = topKFrequentPatterns;
		emptyPTRootNode = initializePrefixTree();
		// this.topKFrequentPatterns = topKFrequentPatterns;
	}

	private void fillSetFromFile(String focusSetPath) throws Exception {
		// the format should be like:
		// NodeType | key1:value1, key2:value2
		FileInputStream fis = new FileInputStream(focusSetPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] labelAndPropKeyVals = line.trim().split("\\|");
			ArrayList<PairStrings> propKeyValues = new ArrayList<PairStrings>();
			if (labelAndPropKeyVals.length == 1) {
				focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
			} else if (labelAndPropKeyVals.length > 1) {
				String[] keyValuePairs = labelAndPropKeyVals[1].split(",");
				for (int i = 0; i < keyValuePairs.length; i++) {
					String[] separatedKeyValue = keyValuePairs[i].split(":");
					propKeyValues.add(new PairStrings(separatedKeyValue[0], separatedKeyValue[1].replace("\"", "")));
				}

			}
			// Assumption: distinct labels
			focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
		}
		br.close();
	}

	public static void main(String[] args) throws Exception {

		PrefixTreeAlg1BiSim prefixTree = new PrefixTreeAlg1BiSim(args);
		try (Transaction tx1 = prefixTree.dataGraph.beginTx()) {
			PrefixTreeNode<IPrefixTreeNodeData> rootNode = prefixTree.generatePrefixTreeForG0();
			prefixTree.bfsTraverse(rootNode);
			tx1.success();

		} catch (Exception e) {
			e.printStackTrace();
		}

		// prefixTree.printTreeDualSim(rootNode, dataGraph);

		// Bitmap bitmap = new Bitmap();
		// bitmap.store(prefixTreeNodeIndex, dataGraph);

		// TODO: traversing prefix-tree to check dual-simulation of each pattern
		// over neo4j data graph

	}

	private PrefixTreeNode<IPrefixTreeNodeData> initializePrefixTree() throws Exception {

		// filling focusLabelPropValSet
		fillSetFromFile(focusSetPath);

		emptyPTRootNode = null;

		// Transaction tx1 = dataGraph.beginTx();

		// generating the root of the prefix tree
		IPrefixTreeNodeData emptyPTRootData = new PrefixTreeNodeDataAlg1BiSim(focusLabelSet);
		emptyPTRootNode = new PrefixTreeNode<IPrefixTreeNodeData>(emptyPTRootData);

		emptyPTRootNode.getData().setPatternPrefixTreeNodeIndex(-1);
		// this.prefixTreeNodeIndex.put(emptyPTRootNode.getData().getPatternPrefixTreeNodeIndex(),
		// emptyPTRootNode);

		// the first level index should be set, otherwise all the levels
		// will be null!
		emptyPTRootNode.setRootLevel();

		// getting all focus nodes of the prefix-tree
		fillFocusNodesOfRequestedTypes(dataGraph);

		// a queue for processing all the waiting new PT nodes.
		traversalQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();

		// maintain the same node level for graph isomorphism checking
		sameLevelPrefixTreeNodes = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();

		labelAdjacencyIndexer = new Indexer(dataGraph, 1, allNodesOfFocusType);

		// we don't need to worry about their existence after that.
		for (String focusLabel : allNodesOfFocusType.keySet()) {

			PatternNode focusNode = new PatternNode(focusLabel, The_Focus_Node);

			HashSet<Integer> dgGraphMatchNodes = new HashSet<Integer>();
			for (Integer nodeId : allNodesOfFocusType.get(focusLabel)) {
				dgGraphMatchNodes.add(nodeId);
			}
			IPrefixTreeNodeData firstLevelChildData = new PrefixTreeNodeDataAlg1BiSim(focusNode, dgGraphMatchNodes,
					focusLabelSet, numberOfPatternsInPrefixTree, labelAdjacencyIndexer);

			PrefixTreeNode<IPrefixTreeNodeData> firstLevelChildPTNode = new PrefixTreeNode<IPrefixTreeNodeData>(
					firstLevelChildData);

			emptyPTRootNode.addChild(firstLevelChildPTNode);

			traversalQueue.add(firstLevelChildPTNode);

			prefixTreeNodeIndex.put(numberOfPatternsInPrefixTree++, firstLevelChildPTNode);

		} // all the labels added as the children of the PT root

		if (DummyProperties.debugMode)
			System.out.println(
					"Before: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e9);
		labelAdjacencyIndexer.generateDistinctNodesAdjMatrix(maxAllowedHops);
		if (DummyProperties.debugMode)
			System.out.println(
					"After: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e9);

		return emptyPTRootNode;
	}

	/**
	 * private void estimateUpperbound(PrefixTreeNode
	 * <PrefixTreeNodeData> parentNode, PrefixTreeNode
	 * <PrefixTreeNodeData> tempProcessingNode, HashMap<String, HashSet
	 * <Integer>> allNodesOfFocusType, PatternNode lastPatternNode, String
	 * relationshipType, int snapshot) throws Exception {
	 * 
	 * // it should be minimum of parent support and ....
	 * 
	 * double parentSnapshotSupport = Double.MAX_VALUE;
	 * 
	 * if (tempProcessingNode.getParent().getData().lastSeenSnapshot < snapshot)
	 * { if (tempProcessingNode.getParent().getData().snapshotUB[snapshot] == 0)
	 * { tempProcessingNode.getData().snapshotUB[snapshot] = 1; }
	 * tempProcessingNode.getData().setTotalUpperbound(snapshot); }
	 * 
	 * if (parentNode.getData().isVerified) parentSnapshotSupport =
	 * parentNode.getData().supportFrequency[snapshot];
	 * 
	 * // PatternNode lastPatternNode = //
	 * tempProcessingNode.getData().targetPatternNode; // // if (destPatternNode
	 * != null) { // lastPatternNode = destPatternNode; // }
	 * 
	 * if (lastPatternNode == null) { throw new Exception(
	 * "lastPatternNode is null for finding shortest path!"); }
	 * 
	 * // the number of focus nodes which has the possibility of being neighbor
	 * // of this new node double cnt = 0;
	 * 
	 * for (PatternNode patternNode :
	 * tempProcessingNode.getData().patternGraph.vertexSet()) { if
	 * (patternNode.isFocus()) { // TODO: may be we can save this information
	 * also in the pattern // node like steps from root!
	 * DijkstraShortestPath<PatternNode, DefaultLabeledEdge> djShortestPath =
	 * new DijkstraShortestPath<PatternNode, DefaultLabeledEdge>(
	 * tempProcessingNode.getData().patternGraph, patternNode, lastPatternNode);
	 * 
	 * double length = djShortestPath.getPathLength();
	 * 
	 * if (length == Double.POSITIVE_INFINITY) { cnt +=
	 * tempProcessingNode.getData().getMatchedNodes().
	 * getDataGraphMatchNodeOfAbsPNode().get( patternNode).size(); } else { for
	 * (Integer nodeId : tempProcessingNode.getData().getMatchedNodes().
	 * getDataGraphMatchNodeOfAbsPNode() .get(patternNode)) { if
	 * (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId)
	 * .get(lastPatternNode.getLabel() +
	 * DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType) != null
	 * && labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId)
	 * .get(lastPatternNode.getLabel() +
	 * DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
	 * .get((int) length) != null &&
	 * labelAdjacencyIndexer.distinctNodesOfDHopsAway
	 * .get(nodeId).get(lastPatternNode.getLabel() +
	 * DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
	 * .get((int) length).size() > 0) cnt++; } }
	 * 
	 * } } double possibleSnapshotUB = cnt /
	 * DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
	 * 
	 * tempProcessingNode.getData().snapshotUB[snapshot] =
	 * Math.min(parentSnapshotSupport, possibleSnapshotUB);
	 * 
	 * tempProcessingNode.getData().setTotalUpperbound(snapshot);
	 * 
	 * }
	 **/

	public PrefixTreeNode<IPrefixTreeNodeData> generatePrefixTreeFromHere(
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns,
			HashSet<Integer> newCreatedOrTouchedPTNodes, PrefixTreeMode prefixTreeMode, int snapshot, double threshold)
			throws Exception {

		// FOR-DEBUG START
		// if (traversalQueue.isEmpty())
		// return emptyPTRootNode;

		// removeNullObjects();

		// FOR-DEBUG END

		int prefixTreeProcessingLevel = traversalQueue.peek().getLevel();

		// if(prefixTreeProcessingLevel==3){
		// System.out.println();
		// }

		while (!traversalQueue.isEmpty()) {

			if (DummyProperties.debugMode) {
				System.out.println();
				System.out.println("traversalQueue.size: " + traversalQueue.size());
			}

			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode = traversalQueue.poll();

			tempProcessingNode.getData().setVisited(true);

			// if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
			// == 4)
			// {
			// System.out.println();
			// }

			// FOR-DEBUG START
			// if (tempProcessingNode == null || tempProcessingNode.getData() ==
			// null) {
			// tempProcessingNode = null;
			// continue;
			// }
			// FOR-DEBUG END

			if (!tempProcessingNode.getData().isValid())
				// TODO: if i can i should remove checkValidty instead whenever
				// i remove a match
				// i check if it's valid yet or not
				tempProcessingNode.getData()
						.setValid(checkValidityAtLeastOneMatchForEachPatternNode(tempProcessingNode));

			if (!tempProcessingNode.getData().isValid())
				// TODO:make sure that "tempProcessingNode" doesn't take memory
				continue;

			try {
				// double lowerbound = 0;
				boolean wasFrequent = tempProcessingNode.getData().isFrequent();
				boolean wasMFP = tempProcessingNode.getData().isMaximalFrequent();

				// TODO: may be if we consider super linked nodes too we can
				// find a tighter upperbound
				// estimateUpperbound(tempProcessingNode.getParent(),
				// tempProcessingNode, allNodesOfFocusType,
				// tempProcessingNode.getData().targetPatternNode,
				// tempProcessingNode.getData().relationshipType,
				// snapshot);

				// if UB>=LB then compute support and try to add in topk

				// if (topKFrequentPatterns.size() > 0) {
				// FOR-DEBUG START
				// if (topKFrequentPatterns.peekLast().getData() == null) {
				// System.err.println("topKFrequentPatterns.peekLast().getData()
				// == null");
				// topKFrequentPatterns.removeLast();
				// } else {
				// lowerbound =
				// topKFrequentPatterns.peekLast().getData().getTotalSupportFrequency();
				// FOR-DEBUG END}
				// }

				// check for dual-simulation in an incremental way
				DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, snapshot, this);

				if (!tempProcessingNode.getData().isValid())
					continue;

				if (!checkSameTypeSameStepsFromRootHasEnoughMatches(tempProcessingNode)) {
					tempProcessingNode.getData().setCorrectness(false, tempProcessingNode, this, 0);
					continue;
				}

				// if (tempProcessingNode.getData().getTotalSupportFrequency()
				// >=
				// lowerbound) {

				// A*->A {0} , A*->A {0} is wrong!
				// if (hasSameNeighborsWithLessMatch(tempProcessingNode)) {
				// tempProcessingNode.getData().isValid() = false;
				// tempProcessingNode.getData()..isFrequent() = false;
				// tempProcessingNode.getData().isMaximalFrequent() = false;
				// mfpPrefixTreeNodes.remove(tempProcessingNode);
				// tempProcessingNode.getData().isVerified = true;
				// tempProcessingNode.getData().removeFromTopK(this,
				// tempProcessingNode);
				// removePrefixTreeNode(tempProcessingNode,
				// newCreatedOrTouchedPTNodes, sameLevelPrefixTreeNodes);
				//
				// if (DummyProperties.debugMode)
				// System.out.println("end hasSameNeighborsWithLessMatch:
				// true");
				//
				// continue;
				// }

				// if it's frequent right now
				if (tempProcessingNode.getData().getTotalSupportFrequency() >= threshold) {
					if (
					// new frequent child
					!tempProcessingNode.getData().isVisited()
							// nonFreq -> Freq
							|| !wasFrequent) {

						tempProcessingNode.getData().setAsMFP(tempProcessingNode, tempProcessingNode.getParent(),
								tempProcessingNode.getSuperNodeLinks(), this, snapshot);

						// updateNumberOfFrequentChildrenOrLinked(tempProcessingNode.getParent(),
						// tempProcessingNode.getSuperNodeLinks(), +1);
					}
				} else {
					// if it's not frequent right now
					if (wasFrequent) {

						// updateNumberOfFrequentChildrenOrLinked(tempProcessingNode.getParent(),
						// tempProcessingNode.getSuperNodeLinks(), -1);

						if (wasMFP) {
							// non-MFP's shouldn't be inside of the topK
							// list also
							tempProcessingNode.getData().maxFreqToNonFreqHandling(tempProcessingNode, this, snapshot);
						} else {
							tempProcessingNode.getData().freqToNonFreqHandling(tempProcessingNode);
						}
					}
				}

				// we just offer new MFP to the top-k list
				if (tempProcessingNode.getData().isMaximalFrequent()
						&& tempProcessingNode.getData().getFoundAllFocuses()) {
					tempProcessingNode.getData().addToTopK(this, tempProcessingNode);
				}

				if (DummyProperties.debugMode)
					System.out.println(tempProcessingNode.getData().getMappedGraphString() + " -> supp:"
							+ tempProcessingNode.getData().getSupportFrequency(snapshot));
				// }
			} catch (Exception exc) {

				System.out.println(tempProcessingNode.getData().getMappedGraphString());
				throw exc;
			}

			// if (tempProcessingNode.getData().getSupportFrequency(snapshot) ==
			// 0.0d) {
			// tempProcessingNode.getData().setPatternAsInvalid();
			// TODO: I've commented this after wrting the until edge
			// deletion
			// because we can maintain all the patterns and just make them
			// invalid
			// so we should make sure that we don't expand invalid patterns

			// bitmap.removeOnePatternIndexForAllNodesHavingIt(
			// tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());

			// TODO: i've commented this part after writing delete unit edge
			// because i want to maintain all patterns and just make them
			// invalid
			// prefixTreeNodeIndex.remove(tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());

			// if (newCreatedOrTouchedPTNodes != null) {
			// newCreatedOrTouchedPTNodes.remove(tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());
			// }
			// tempProcessingNode.removeAllReferences();
			// tempProcessingNode = null;
			// continue;
			// }

			if (!tempProcessingNode.getData().isValid()) {
				continue;
			}

			if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
				continue;
			}

			if (prefixTreeProcessingLevel < tempProcessingNode.getLevel()) {
				// new sibling will be created soon.
				// old ones should be cleared
				sameLevelPrefixTreeNodes.clear();
				// going to the next level
				prefixTreeProcessingLevel = tempProcessingNode.getLevel();
				if (DummyProperties.debugMode)
					System.out.println("prefixTreeProcessingLevel1: " + prefixTreeProcessingLevel);
			}

			// if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
			// == 6)
			// {
			// System.out.println();
			// }

			// TODO: where this isValid will be true?!

			// FOR-DEBUG STARTif (tempProcessingNode != null &&
			// tempProcessingNode.getData().isValid()) {
			if (DummyProperties.debugMode) {
				System.out.println("generator processing node:");
				System.out.println(tempProcessingNode.getData().getMappedGraphString());
			}

			processQueueNodeStartTime = System.nanoTime();
			processQueueNode(dataGraph, traversalQueue, sameLevelPrefixTreeNodes, prefixTreeProcessingLevel + 1,
					tempProcessingNode, newCreatedOrTouchedPTNodes, prefixTreeMode, snapshot);
			processQueueNodeDuration += ((System.nanoTime() - processQueueNodeStartTime) / 1e6);
			//// FOR-DEBUG END}

		}

		if (DummyProperties.debugMode)
			System.out.println("finishing queue!");

		return emptyPTRootNode;

	}

	private boolean checkSameTypeSameStepsFromRootHasEnoughMatches(
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode) {

		for (PatternNode srcPatternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
			for (String nexType : tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
					.get(srcPatternNode).keySet()) {

				int howManyOfSameType = tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
						.get(srcPatternNode).get(nexType);
				if (howManyOfSameType > 1) {

					HashSet<Integer> allMatchNodesSet = new HashSet<Integer>();
					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph()
							.outgoingEdgesOf(srcPatternNode)) {

						String tempNexType = tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e).getLabel()
								+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType();

						if (nexType.equals(tempNexType)) {
							allMatchNodesSet.addAll(
									tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
											.get(tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e)));
						}
					}
					if (allMatchNodesSet.size() < howManyOfSameType) {
						return false;
					}
				}

			}
		}
		return true;
	}

	// TODO: this method is written for a patch, we should find original problem
	private void removeNullObjects() {
		while (traversalQueue.contains(null)) {
			traversalQueue.remove(null);
			System.err.println("traversalQueue.contains(null)");
		}
		while (mfpPrefixTreeNodes.contains(null)) {
			mfpPrefixTreeNodes.remove(null);
			System.err.println("mfpPrefixTreeNodes.contains(null)");
		}
		while (topKFrequentPatterns.contains(null)) {
			topKFrequentPatterns.remove(null);
			System.err.println("topKFrequentPatterns.contains(null)");
		}
	}

	// TODO: wrong prefixTreenodes shouldn't be created from scratch
	// TODO: find where is the original bug
	private void removePrefixTreeNode(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			HashSet<Integer> newCreatedOrTouchedPTNodes,
			ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> sameLevelPrefixTreeNodes) {
		Queue<PrefixTreeNode<IPrefixTreeNodeData>> childrenQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();
		childrenQueue.add(tempProcessingNode);

		while (!childrenQueue.isEmpty()) {
			removeSinglePrefixTreeNode(childrenQueue.poll(), newCreatedOrTouchedPTNodes, sameLevelPrefixTreeNodes,
					childrenQueue);
		}

	}

	private void removeSinglePrefixTreeNode(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			HashSet<Integer> newCreatedOrTouchedPTNodes,
			ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> sameLevelPrefixTreeNodes,
			Queue<PrefixTreeNode<IPrefixTreeNodeData>> childrenQueue) {

		if (tempProcessingNode != null && tempProcessingNode.getData() != null) {
			if (tempProcessingNode.getChildren().size() > 0) {
				childrenQueue.addAll(tempProcessingNode.getChildren());
			}

			if (tempProcessingNode.getLinkedNodes() != null) {
				childrenQueue.addAll(tempProcessingNode.getLinkedNodes());
			}

			if (DummyProperties.debugMode) {
				System.out.println("removePrefixTreeNode: " + tempProcessingNode.getData());
			}

			bitmap.removeOnePatternIndexForAllNodesHavingIt(
					tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());
			prefixTreeNodeIndex.remove(tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());
			if (newCreatedOrTouchedPTNodes != null) {
				newCreatedOrTouchedPTNodes.remove(tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());
			}
			traversalQueue.remove(tempProcessingNode);

			if (tempProcessingNode.getParent() != null && tempProcessingNode.getParent().getChildren() != null) {
				tempProcessingNode.getParent().getChildren().remove(tempProcessingNode);
			}

			if (tempProcessingNode.getSuperNodeLinks() != null) {
				for (PrefixTreeNode<IPrefixTreeNodeData> superNodeLink : tempProcessingNode.getSuperNodeLinks()) {
					if (superNodeLink.getLinkedNodes() != null)
						superNodeLink.getLinkedNodes().remove(tempProcessingNode);
				}
			}

			sameLevelPrefixTreeNodes.remove(tempProcessingNode);
			if (mfpPrefixTreeNodes != null) {
				mfpPrefixTreeNodes.remove(tempProcessingNode);
			}

			tempProcessingNode.removeAllReferences();

		}
		tempProcessingNode = null;

	}

	private boolean hasSameNeighborsWithLessMatch(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode) {

		PatternNode targetPatternNode = tempProcessingNode.getData().getTargetPatternNode();
		PatternNode sourcePatternNode = tempProcessingNode.getData().getSourcePatternNode();

		if (tempProcessingNode.getData().getPatternGraph().outDegreeOf(sourcePatternNode) <= 1) {
			return false;
		}
		if (DummyProperties.debugMode) {
			System.out.println("hasSameNeighborsWithLessMatch");
			System.out.println("tempProcessingNode.getData().targetPatternNode:"
					+ tempProcessingNode.getData().getTargetPatternNode() + " _ "
					+ tempProcessingNode.getData().getTargetPatternNode().hashCode());
			System.out.println("tempProcessingNode.getData().sourcePatternNode:"
					+ tempProcessingNode.getData().getSourcePatternNode() + " _ "
					+ tempProcessingNode.getData().getSourcePatternNode().hashCode());
		}
		int cnt = 1;
		HashSet<Integer> set1 = tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.get(targetPatternNode);
		for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().outgoingEdgesOf(sourcePatternNode)) {
			PatternNode otherSimilarTarget = tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e);
			if (otherSimilarTarget != targetPatternNode
					&& tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(otherSimilarTarget)
							.equals(tempProcessingNode.getData().getStepsFromRootOfPatternNodes()
									.get(targetPatternNode))
					&& otherSimilarTarget.getType().equals(targetPatternNode.getType())
					&& e.getType().equals(tempProcessingNode.getData().getRelationshipType())) {

				HashSet<Integer> set2 = tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
						.get(otherSimilarTarget);
				if (set1.size() == set2.size() && set1.containsAll(set2)) {
					cnt++;
				}
			}
		}

		if (set1.size() < cnt) {
			if (DummyProperties.debugMode)
				System.out.println("otherSimilarTarget and targetPatternNode has less matches!");
			return true;
		}

		return false;

	}

	public PrefixTreeNode<IPrefixTreeNodeData> generatePrefixTreeForG0() throws Exception {

		// try (Transaction tx1 = dataGraph.beginTx()) {
		int prefixTreeProcessingLevel = traversalQueue.peek().getLevel();

		while (!traversalQueue.isEmpty()) {

			// if (algorithmStartTimeJustForFocusFinding > 0) {
			// double duration = ((System.nanoTime() -
			// algorithmStartTimeJustForFocusFinding) / 1e6);
			// if (duration > 120000) {
			// return null;
			// }
			// }

			if (DummyProperties.debugMode) {
				System.out.println();
				System.out.println("traversalQueue.size: " + traversalQueue.size());
			}

			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode = traversalQueue.poll();

			if (!tempProcessingNode.getData().isValid())
				tempProcessingNode.getData()
						.setValid(checkValidityAtLeastOneMatchForEachPatternNode(tempProcessingNode));

			if (!tempProcessingNode.getData().isValid())
				// TODO:make sure that "tempProcessingNode" doesn't take memory
				continue;

			try {
				// check for dual-simulation in an incremental way
				DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, 0, this);
				// tempProcessingNode.getData().getTotalSupportFrequency() =
				// tempProcessingNode.getData().supportFrequency[0];

				if (!checkSameTypeSameStepsFromRootHasEnoughMatches(tempProcessingNode)) {
					tempProcessingNode.getData().setCorrectness(false, tempProcessingNode, this, 0);
					continue;
				}

				if (DummyProperties.debugMode)
					System.out.println(tempProcessingNode.getData().getMappedGraphString() + " -> supp:"
							+ tempProcessingNode.getData().getSupportFrequency(0));
			} catch (Exception exc) {
				System.out.println(tempProcessingNode.getData().getMappedGraphString());
				throw exc;
			}

			// if (hasSameNeighborsWithLessMatch(tempProcessingNode)) {
			// tempProcessingNode.getData().isValid() = false;
			// tempProcessingNode.getData().isFrequent() = false;
			// tempProcessingNode.getData().isMaximalFrequent() = false;
			// tempProcessingNode.getData().isVerified = true;
			// removePrefixTreeNode(tempProcessingNode, null,
			// sameLevelPrefixTreeNodes);
			//
			// if (DummyProperties.debugMode)
			// System.out.println("end hasSameNeighborsWithLessMatch: true");
			//
			// continue;
			// }

			tempProcessingNode.getData().setVisited(true);

			if (!tempProcessingNode.getData().isValid()) {
				continue;
			}

			if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
				continue;
			}

			if (prefixTreeProcessingLevel < tempProcessingNode.getLevel()) {
				// new sibling will be created soon.
				// old ones should be cleared
				sameLevelPrefixTreeNodes.clear();
				// going to the next level
				prefixTreeProcessingLevel = tempProcessingNode.getLevel();
				if (DummyProperties.debugMode)
					System.out.println("prefixTreeProcessingLevel G0: " + prefixTreeProcessingLevel);
				// if (prefixTreeProcessingLevel == 5) {
				// // System.out.println();
				// }
			}

			if (tempProcessingNode != null && tempProcessingNode.getData().isValid()) {
				if (DummyProperties.debugMode) {
					System.out.println("generator processing node:");
					System.out.println(tempProcessingNode.getData().getMappedGraphString());
				}

				// if
				// (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
				// == 7) {
				// System.out.println();
				// }

				processQueueNode(dataGraph, traversalQueue, sameLevelPrefixTreeNodes, prefixTreeProcessingLevel + 1,
						tempProcessingNode, null, PrefixTreeMode.BATCH, 0);
			}
		}

		if (DummyProperties.debugMode)
			System.out.println("finishing queue!");
		return emptyPTRootNode;
	}

	private void updateNumberOfFrequentChildrenOrLinked(PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			List<PrefixTreeNode<IPrefixTreeNodeData>> superNodeLinks, int updateValue) {

		if (parentPrefixTreeNode != null) {
			parentPrefixTreeNode.getData().updateNumberOfFrequentChildrenAndLinked(updateValue);
		}
		if (superNodeLinks != null) {
			for (PrefixTreeNode<IPrefixTreeNodeData> superNodeLink : superNodeLinks) {
				superNodeLink.getData().updateNumberOfFrequentChildrenAndLinked(updateValue);
			}
		}

	}

	private void fillFocusNodesOfRequestedTypes(GraphDatabaseService dataGraph2) throws Exception {

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			ArrayList<PairStrings> propVals = focusLabelPropValSet.get(focusLabel);
			for (Node node : dataGraph.getAllNodes()) {
				// boolean isCandidate = true;
				if (!node.hasLabel(Label.label(focusLabel))) {
					continue;
				}
				if (propVals.size() > 0) {
					for (PairStrings pairString : propVals) {

						if (node.hasProperty(pairString.key)) {
							if (DummyFunctions.isContain(node.getProperty(pairString.key).toString().toLowerCase(),
									pairString.value.toLowerCase())
									|| node.getProperty(pairString.key).toString().toLowerCase()
											.equals(pairString.value.toLowerCase())) {
								allNodesOfFocusType.get(focusLabel).add((int) node.getId());
								break;
							}
						}

					}
				} else {
					allNodesOfFocusType.get(focusLabel).add((int) node.getId());
				}

			}
		}

		numberOfAllFocusNodes = 0;
		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				throw new Exception("no items for \"" + key + "\"");
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
		}

		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;
		if (DummyProperties.debugMode) {
			System.out.println("focusNodesOfSpecificType size: " + allNodesOfFocusType.size());
		}
	}

	private void processQueueNode(GraphDatabaseService dataGraph,
			Queue<PrefixTreeNode<IPrefixTreeNodeData>> traversalQueue,
			ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> sameLevelPrefixTreeNodes, int prefixTreeProcessingLevel,
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
			PrefixTreeMode prefixTreeMode, int snapshot) throws Exception {

		// while we are inside of this method we expand the same pattern to
		// generate all the possible children

		// get the pattern
		// for all nodes in the pattern
		IPrefixTreeNodeData tempProcessingNodeData = tempProcessingNode.getData();

		for (PatternNode srcPatternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {

			// if it's in the update mode, we should just expand new nodes
			// then for better performance we don't need to go further
			if (tempProcessingNodeData.getPrefixTreeMode() == PrefixTreeMode.UPDATE) {
				if (DummyProperties.debugMode) {
					System.out.println("newUnexpandedPatternsNodesOfNeo4jNodes: "
							+ tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes());
				}

				// TODO: patch:
				// tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes()
				// == null why it's null?!
				if (tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes() == null
						|| !tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes().keySet()
								.contains(srcPatternNode))
					continue;
			}

			// if it needs any new expansion based on its hops from the root
			if (tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) < maxAllowedHops) {
				if (DummyProperties.debugMode) {
					System.out.println(
							"srcAbstractPatternNode:" + srcPatternNode.getType() + "" + srcPatternNode.isFocus());
				}

				// String: the destination because source are same
				HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrc = new HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>>();
				HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc = new HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>>();

				// TODO: if we create this index inside of pattern creation then
				// it will be cheaper in terms of time
				// but more expensive in terms of memory
				HashMap<String, Integer> frequencyOfSameOutNeighborType = getFrequencyOfSameOutNeighborType(
						tempProcessingNodeData, srcPatternNode);

				int matchGraphIndex = -1;
				// for all match nodes in this prefix-tree node and for this src
				// pattern node
				for (Integer srcDataGpNodeId : tempProcessingNodeData.getMatchedNodes()
						.getDataGraphMatchNodeOfAbsPNode().get(srcPatternNode)) {

					if (tempProcessingNodeData.getPrefixTreeMode() == PrefixTreeMode.UPDATE) {
						if (!tempProcessingNodeData.getNewUnexpandedNodesOfPatternNodes().keySet()
								.contains(srcDataGpNodeId)) {
							continue;
							// TODO: maintaining update/batch mode of the PT
							// nodes
						}

						// maybe A*={a0,a1} and A={a0,a1}
						// ???
						if (!tempProcessingNodeData.getNewUnexpandedNodesOfPatternNodes().get(srcDataGpNodeId)
								.contains(srcPatternNode)) {
							continue;
						}
					}

					// if same sibling type of srcPatternNode previously
					// expanded from this srcDataNodeId and right now
					// they are alone (just one match node for the pattern node)
					// or has some outdegree
					// we shouldn't expand from this src data node id at all
					boolean expandedSrcBefore = false;
					for (DefaultLabeledEdge e1 : tempProcessingNodeData.getPatternGraph()
							.incomingEdgesOf(srcPatternNode)) {
						PatternNode parentOfSrcPattern = tempProcessingNodeData.getPatternGraph().getEdgeSource(e1);
						for (DefaultLabeledEdge e2 : tempProcessingNodeData.getPatternGraph()
								.outgoingEdgesOf(parentOfSrcPattern)) {
							if (e2.getType().equals(e1.getType())
									&& tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2) != srcPatternNode
									&& tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2).getType()
											.equals(srcPatternNode.getType())) {

								if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
										.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2))
										.contains(srcDataGpNodeId)
										&& (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
												.size() == 1
										/*
										 * || tempProcessingNodeData.
										 * getPatternGraph(). outDegreeOf(
										 * tempProcessingNodeData.
										 * getPatternGraph(). getEdgeTarget(e2))
										 * > 0
										 */)

								) {
									expandedSrcBefore = true;
									break;
								}

							}

						}
						if (expandedSrcBefore) {
							break;
						}

					}
					if (expandedSrcBefore)
						continue;

					matchGraphIndex++;

					// System.out.println("srcNodeId:" +
					// srcDataGpNode.patternGNodeId);

					// TODO: out degree indexing is needed here
					if ((labelAdjacencyIndexer.dataGraphNodeInfos
							.get(srcDataGpNodeId).outDegree > tempProcessingNodeData.getPatternGraph()
									.outDegreeOf(srcPatternNode))) {

						// of all possible labels_reltype
						for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway
								.get(srcDataGpNodeId).keySet()) {

							HashSet<Integer> sameLabelNeighborNodes = labelAdjacencyIndexer.distinctNodesOfDHopsAway
									.get(srcDataGpNodeId).get(otherNodeLabelRelType).get(Indexer.AFTER);

							if (sameLabelNeighborNodes != null && sameLabelNeighborNodes.size() > 0) {
								if (!frequencyOfSameOutNeighborType.containsKey(otherNodeLabelRelType)
										|| frequencyOfSameOutNeighborType
												.get(otherNodeLabelRelType) < sameLabelNeighborNodes.size()) {
									// we should add one same node label to this
									// TODO: difference between focus nodes and
									// non-focus nodes
									for (Integer newNodeId : sameLabelNeighborNodes) {

										// FOR-DEBUG START
										// if
										// (StringUtils.countMatches(otherNodeLabelRelType,
										// Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE)
										// > 1) {
										// throw new Exception(
										// "countMatches(otherNodeLabelRelType,
										// Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE)>1");
										// }
										// FOR-DEBUG END

										int separatorIndex = otherNodeLabelRelType
												.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
										String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
										String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);

										// FOR-DEBUG START:
										// if
										// (tempProcessingNodeData.getStepsFromRootOfPatternNodes()
										// .get(srcPatternNode) == null) {
										// System.out.println();
										// }
										// FOR-DEBUG END:

										Integer destStepsFromRoot = tempProcessingNodeData
												.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
										if (DummyProperties.debugMode) {
											System.out.println("matchGraphIndex: " + matchGraphIndex + ", srcNodeId:"
													+ srcDataGpNodeId + ", newNodeId:" + newNodeId);
										}

										// TODO: may be make some problem for us
										// our previousExpansion is not very
										// stable

										// if a src node Id wants to be
										// expanded,
										// it shouldn't have a sibling
										// containing that node Id
										// and expanded before.
										boolean expandedBefore = false;
										// if
										// (tempProcessingNodeData.prefixTreeMode
										// == PrefixTreeMode.BATCH) {
										// for (PatternNode patternNode :
										// tempProcessingNodeData.getMatchedNodes().patternNodeOfNeo4jNode
										// .get(srcDataGpNodeId)) {
										// if (srcPatternNode != patternNode) {
										// if
										// (tempProcessingNodeData.getPatternGraph()
										// .outDegreeOf(patternNode) > 0) {
										// expandedBefore = true;
										// break;
										// }
										// }
										// }
										// if (expandedBefore) {
										// System.out.println(
										// "expanded before1 from " +
										// srcDataGpNodeId + " to others");
										// continue;
										// }
										// }

										for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
												.outgoingEdgesOf(srcPatternNode)) {

											// if from this src I went to the
											// target with the newNodeId
											// and out degree of that is more
											// than 0 which means that it
											// verified before.
											// or it has just that node we
											// shouldn't expand to that node
											// again
											if (tempProcessingNode.getData().getMatchedNodes()
													.getDataGraphMatchNodeOfAbsPNode()
													.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e))
													.contains(newNodeId)
													&& (/*
														 * tempProcessingNodeData
														 * .getPatternGraph()
														 * .outDegreeOf(
														 * tempProcessingNodeData
														 * .getPatternGraph()
														 * .getEdgeTarget(e)) >
														 * 0 ||
														 */ tempProcessingNode.getData().getMatchedNodes()
															.getDataGraphMatchNodeOfAbsPNode()
															.get(tempProcessingNodeData.getPatternGraph()
																	.getEdgeTarget(e))
															.size() == 1)) {
												expandedBefore = true;
												break;
											}

										}
										//
										if (expandedBefore) {
											if (DummyProperties.debugMode)
												System.out.println(
														"expanded before from " + srcDataGpNodeId + " to " + newNodeId);
											continue;
										}

										// if (srcDataGpNodeId == 0 && newNodeId
										// == 3) {
										// System.out.println();
										// }

										// goBackToPrev = false;
										ArrayList<PatternNode> destPatternNodes = new ArrayList<PatternNode>();
										getDestPatternNodeAndCheckForGoBackToPrev(destPatternNodes,
												tempProcessingNodeData, srcPatternNode, srcDataGpNodeId, newNodeId,
												destLabel, relationshipType, destStepsFromRoot);

										for (int d = 0; d < destPatternNodes.size(); d++) {
											PatternNode destPatternNode = destPatternNodes.get(d);

											int destInDegree = 1;
											int incomingFromSameType = 1;

											if (goBackToPrev) {
												// b1 or b3 a->b->d->b and a->b
												destStepsFromRoot = Math.min(destStepsFromRoot, tempProcessingNodeData
														.getStepsFromRootOfPatternNodes().get(destPatternNode));

												destInDegree += tempProcessingNodeData.getPatternGraph()
														.inDegreeOf(destPatternNode);

												for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
														.incomingEdgesOf(destPatternNode)) {
													if (tempProcessingNodeData.getPatternGraph().getEdgeSource(e)
															.getLabel().equals(srcPatternNode.getLabel())
															&& e.getType().equals(relationshipType)) {
														incomingFromSameType++;
													}
												}
											}

											if (destInDegree > labelAdjacencyIndexer.dataGraphNodeInfos
													.get(newNodeId).inDegree) {
												if (DummyProperties.debugMode) {
													System.out.println("cont: destInDegree:" + destInDegree
															+ " >  in degree in data graph:"
															+ labelAdjacencyIndexer.dataGraphNodeInfos
																	.get(newNodeId).inDegree);
												}
												continue;
											}

											if (incomingFromSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway
													.get(newNodeId)
													.get(srcPatternNode.getLabel()
															+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
															+ relationshipType)
													.get(Indexer.BEFORE).size()) {
												if (DummyProperties.debugMode) {
													System.out.println("cont. incomingFromSameType: "
															+ incomingFromSameType + " prev index type in data graph:"
															+ labelAdjacencyIndexer.distinctNodesOfDHopsAway
																	.get(newNodeId)
																	.get(srcPatternNode.getLabel()
																			+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
																			+ relationshipType)
																	.get(Indexer.BEFORE).size());
												}
												continue;
											}

											// Integer
											// destDataGraphPatternNodeId =
											// newNodeId;

											// finding the new node type;
											String newNodeType = null;

											// if we've seen it in this pattern
											// before...

											newNodeType = destPatternNode.getType();

											PrefixTreeNode<IPrefixTreeNodeData> seenPrefixTreeNode = null;
											if (goBackToPrev) {
												if (seenPatternNodeFromPreviousNodesForThisSrc
														.containsKey(destPatternNode)) {
													seenPrefixTreeNode = seenPatternNodeFromPreviousNodesForThisSrc
															.get(destPatternNode);
												}
											} else {
												if (newlySeenPatternNodeForThisSrc.containsKey(
														newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
																+ relationshipType)) {
													// if this expansion has
													// seen
													// before
													// add it to the group of
													// that
													// prefix-tree node
													seenPrefixTreeNode = newlySeenPatternNodeForThisSrc.get(
															newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
																	+ relationshipType);
												}
											}

											if (seenPrefixTreeNode != null) {
												// double start =
												// System.nanoTime();

												IPrefixTreeNodeData prefixTreeNodeData = seenPrefixTreeNode.getData();

												PatternNode tempDestPatternNode = seenPrefixTreeNode.getData()
														.getTargetPatternNode();

												if (DummyProperties.debugMode) {
													System.out.println("prev pattern seen:");
													System.out.println(prefixTreeNodeData.getMappedGraphString());
												}

												prefixTreeNodeData.addNewMatch(tempDestPatternNode, newNodeId,
														this.labelAdjacencyIndexer);

												seenPatternNodeFromPreviousNodesForThisSrc.put(tempDestPatternNode,
														seenPrefixTreeNode);

												if (newCreatedOrTouchedPTNodes != null)
													newCreatedOrTouchedPTNodes.add(seenPrefixTreeNode.getData()
															.getPatternPrefixTreeNodeIndex());
											} else {

												// make a new pattern for SGI
												// checking
												// and add it as
												// a new child if possible
												ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
														DefaultLabeledEdge.class);

												for (PatternNode patternNode : tempProcessingNode.getData()
														.getPatternGraph().vertexSet()) {
													newPatternGraph.addVertex(patternNode);
												}

												for (DefaultLabeledEdge e : tempProcessingNode.getData()
														.getPatternGraph().edgeSet()) {
													newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e),
															newPatternGraph.getEdgeTarget(e), e);
												}

												if (!newPatternGraph.vertexSet().contains(destPatternNode)) {
													newPatternGraph.addVertex(destPatternNode);
												}

												newPatternGraph.addEdge(srcPatternNode, destPatternNode,
														new DefaultLabeledEdge(relationshipType));

												// if
												// (srcPatternNode.getLabel().equals("b")
												// &&
												// destPatternNode.getLabel().equals("d"))
												// {
												// System.out.println();
												// }
												HashSet<Integer> prevNodesOfSrcType = labelAdjacencyIndexer.distinctNodesOfDHopsAway
														.get(newNodeId)
														.get(srcPatternNode.getLabel()
																+ Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
																+ relationshipType)
														.get(Indexer.BEFORE);

												int sameTypeIncomingEdgeCnt = 0;
												for (DefaultLabeledEdge e : newPatternGraph
														.incomingEdgesOf(destPatternNode)) {
													if (newPatternGraph.getEdgeSource(e).getType()
															.equals(srcPatternNode.getType())
															&& e.getType().equals(relationshipType)) {
														sameTypeIncomingEdgeCnt++;
													}
												}
												if (sameTypeIncomingEdgeCnt > prevNodesOfSrcType.size()) {
													if (DummyProperties.debugMode)
														System.out.println(
																"newPatternGraph.inDegreeOf(destPatternNode) > prevNodesOfSrcType.size()");
													continue;
												}

												// for all other same-level
												// children
												// of the prefix-tree:
												boolean itWasBisimulated = false;
												if (tempProcessingNodeData
														.getPrefixTreeMode() == PrefixTreeMode.BATCH) {
													if (this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
															.get(prefixTreeProcessingLevel) != null) {
														for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeSibling : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
																.get(prefixTreeProcessingLevel)) {
															// if
															// (prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex()
															// == 38) {
															// System.out.println();
															// }

															if (DualSimulationHandler.isBiDualSimulated(newPatternGraph,
																	prefixTreeSibling, this)) {

																// if yes:
																// if (check sgi
																// ==
																// true)
																// link the
																// processing
																// node
																// to the SGIed
																// pattern
																// each node can
																// have
																// multiple node
																// links
																itWasBisimulated = true;

																// if
																// (!tempProcessingNode.getChildren()
																// .contains(prefixTreeSibling))
																// {
																// if
																// (tempProcessingNode.getLinkedNodes()
																// == null
																// ||
																// !tempProcessingNode.getLinkedNodes()
																// .contains(prefixTreeSibling))
																if (DummyProperties.debugMode) {
																	System.out.println(
																			"BATCH: simultanous siblings: add node link from "
																					+ tempProcessingNode.getData()
																							.getPatternPrefixTreeNodeIndex()
																					+ " to "
																					+ prefixTreeSibling.getData()
																							.getPatternPrefixTreeNodeIndex());
																}
																addIncNodeLink(tempProcessingNode, prefixTreeSibling);

																// }

																break;
																// so, this
																// child
																// doesn't
																// need any
																// from here.
															}
														}
													}
													if (!itWasBisimulated
															&& this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
																	.containsKey(tempProcessingNode.getLevel() + 1)) {

														for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
																.get(tempProcessingNode.getLevel() + 1)) {

															if (DualSimulationHandler.isBiDualSimulated(newPatternGraph,
																	prefixTreeNode, this)) {

																if (DummyProperties.debugMode) {
																	System.out.println(
																			"BATCH: sameLevels: add node link from "
																					+ tempProcessingNode.getData()
																							.getPatternPrefixTreeNodeIndex()
																					+ " to " + prefixTreeNode.getData()
																							.getPatternPrefixTreeNodeIndex());
																}

																itWasBisimulated = true;
																addIncNodeLink(tempProcessingNode, prefixTreeNode);

																// }

																break;
															}
														}
													}
												} else {
													// TODO:may be we can handle
													// it
													// without SGI
													for (PrefixTreeNode<IPrefixTreeNodeData> child : tempProcessingNode
															.getChildren()) {

														HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;

														if (DualSimulationHandler.preBiSimChecking(
																child.getData().getPatternGraph(), newPatternGraph,
																this)
																&& (dualBiSimMap = DualSimulationHandler
																		.getBiSimMapIfAny(newPatternGraph, child,
																				this)) != null) {

															if (dualBiSimMap == null || dualBiSimMap.size() == 0) {
																if (Dummy.DummyProperties.debugMode) {
																	System.err.println(
																			"no dest pattern is found after a successful SGI for children nodes!!");
																}
																continue;
															}

															itWasBisimulated = true;

															PatternNode tempSrcPatternNode = null;
															tempSrcPatternNode = dualBiSimMap.get(srcPatternNode)
																	.iterator().next();

															for (PatternNode destPtn : dualBiSimMap
																	.get(destPatternNode)) {
																// if
																// (child.getData().getPatternPrefixTreeNodeIndex()
																// == 2
																// &&
																// srcDataGpNodeId
																// == 1 &&
																// newNodeId ==
																// 3) {
																// System.out.println();
																// }
																child.getData().addNewMatchForUpdate(tempProcessingNode,
																		tempSrcPatternNode, srcDataGpNodeId, destPtn,
																		newNodeId, this.labelAdjacencyIndexer);
															}

															if (DummyProperties.debugMode) {
																System.out.println(
																		child.getData().getMappedGraphString());
																System.out.println("new match for a child node!");
															}
															traversalQueue.add(child);
															newCreatedOrTouchedPTNodes.add(
																	child.getData().getPatternPrefixTreeNodeIndex());
															break;
														}
													}

													if (!itWasBisimulated
															&& tempProcessingNode.getLinkedNodes() != null) {

														for (PrefixTreeNode<IPrefixTreeNodeData> child : tempProcessingNode
																.getLinkedNodes()) {

															HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;

															if (DualSimulationHandler.preBiSimChecking(
																	child.getData().getPatternGraph(), newPatternGraph,
																	this)
																	&& (dualBiSimMap = DualSimulationHandler
																			.getBiSimMapIfAny(newPatternGraph, child,
																					this)) != null) {

																if (dualBiSimMap == null || dualBiSimMap.size() == 0) {
																	if (Dummy.DummyProperties.debugMode) {
																		System.out.println(
																				"no dest pattern is found after a successful SGI for linked nodes!");
																	}
																	continue;
																}

																itWasBisimulated = true;

																PatternNode tempSrcPatternNode = null;
																tempSrcPatternNode = dualBiSimMap.get(srcPatternNode)
																		.iterator().next();
																// if
																// (child.getData().getPatternPrefixTreeNodeIndex()
																// == 2
																// &&
																// srcDataGpNodeId
																// == 1 &&
																// newNodeId ==
																// 3) {
																// System.out.println();
																// }
																for (PatternNode destPtn : dualBiSimMap
																		.get(destPatternNode)) {
																	child.getData().addNewMatchForUpdate(
																			tempProcessingNode, tempSrcPatternNode,
																			srcDataGpNodeId, destPtn, newNodeId,
																			this.labelAdjacencyIndexer);
																}

																if (DummyProperties.debugMode)
																	System.out.println(
																			"new match for a linked  node using SGI!");

																traversalQueue.add(child);
																newCreatedOrTouchedPTNodes.add(child.getData()
																		.getPatternPrefixTreeNodeIndex());
																break;
															}
														}

													}

													if (!itWasBisimulated
															&& this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
																	.containsKey(tempProcessingNode.getLevel() + 1)) {
														// if
														// (numberOfPatternsInPrefixTree
														// == 11) {
														// System.out.println();
														// }

														for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNodeByLevel : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
																.get(tempProcessingNode.getLevel() + 1)) {

															if (tempProcessingNode.getChildrenLinksSet()
																	.contains(prefixTreeNodeByLevel))
																// it's
																// checked
																// before
																continue;

															HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;
															if (DualSimulationHandler.preBiSimChecking(
																	prefixTreeNodeByLevel.getData().getPatternGraph(),
																	newPatternGraph, this)
																	&& (dualBiSimMap = DualSimulationHandler
																			.getBiSimMapIfAny(newPatternGraph,
																					prefixTreeNodeByLevel,
																					this)) != null) {
																itWasBisimulated = true;

																PatternNode tempSrcPatternNode = null;
																tempSrcPatternNode = dualBiSimMap.get(srcPatternNode)
																		.iterator().next();
																for (PatternNode destPtn : dualBiSimMap
																		.get(destPatternNode)) {
																	prefixTreeNodeByLevel.getData()
																			.addNewMatchForUpdate(tempProcessingNode,
																					tempSrcPatternNode, srcDataGpNodeId,
																					destPtn, newNodeId,
																					this.labelAdjacencyIndexer);
																}

																addIncNodeLink(tempProcessingNode,
																		prefixTreeNodeByLevel);

																if (DummyProperties.debugMode)
																	System.out.println(
																			"new match for a SGI node in INC mode!");

																traversalQueue.add(prefixTreeNodeByLevel);
																newCreatedOrTouchedPTNodes.add(prefixTreeNodeByLevel
																		.getData().getPatternPrefixTreeNodeIndex());

																break;
															}

															// }
														}

													}
												}

												if (!itWasBisimulated) {
													PrefixTreeNode<IPrefixTreeNodeData> newChild = createNewPrefixTreeNode(
															tempProcessingNode, newPatternGraph, srcPatternNode,
															destPatternNode, srcDataGpNodeId, newNodeId,
															newCreatedOrTouchedPTNodes, relationshipType,
															destStepsFromRoot,
															tempProcessingNode.getData()
																	.getPrefixTreeMode() == PrefixTreeMode.UPDATE
																			? !FRESH_SOURCE : FRESH_SOURCE,
															snapshot);

													if (!goBackToPrev)
														newlySeenPatternNodeForThisSrc.put(destPatternNode.getType()
																+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
																+ relationshipType, newChild);
													else
														seenPatternNodeFromPreviousNodesForThisSrc.put(destPatternNode,
																newChild);

													sameLevelPrefixTreeNodes.add(newChild);

												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		tempProcessingNodeData.renewNewUnexpandedNodesOfPatternNodes();
		tempProcessingNodeData.renewNewUnexpandedPatternsNodesOfNeo4jNodes();

	}

	private HashMap<String, Integer> getFrequencyOfSameOutNeighborType(IPrefixTreeNodeData tempProcessingNodeData,
			PatternNode srcPatternNode) {

		HashMap<String, Integer> frequencyOfSameOutNeighborType = new HashMap<String, Integer>();

		for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph().outgoingEdgesOf(srcPatternNode)) {

			frequencyOfSameOutNeighborType
					.putIfAbsent(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType(), 0);

			frequencyOfSameOutNeighborType.put(
					tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType(),
					frequencyOfSameOutNeighborType
							.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
									+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
							+ 1);
		}

		return frequencyOfSameOutNeighborType;

	}

	private PrefixTreeNode<IPrefixTreeNodeData> createNewPrefixTreeNode(
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
			PatternNode destPatternNode, Integer srcDataGpNodeId, Integer newNodeId,
			HashSet<Integer> newCreatedOrTouchedPTNodes, String relationshipType, Integer destStepsFromRoot,
			boolean freshSource, int snapshot) {

		creationOfNewPrefixTreeNodeStart = System.nanoTime();

		IPrefixTreeNodeData prefixTreeNodeData = new PrefixTreeNodeDataAlg1BiSim(newPatternGraph,
				tempProcessingNode.getData().getPatternRootNode(), tempProcessingNode.getData(),
				tempProcessingNode.getData().getMatchedNodes(), srcPatternNode, destPatternNode, srcDataGpNodeId,
				newNodeId, numberOfPatternsInPrefixTree, relationshipType, destStepsFromRoot, freshSource, snapshot,
				this.labelAdjacencyIndexer);

		PrefixTreeNode<IPrefixTreeNodeData> newChild = new PrefixTreeNode<IPrefixTreeNodeData>(prefixTreeNodeData);
		// if (numberOfPatternsInPrefixTree == 40) {
		// System.out.println();
		// }
		prefixTreeNodeIndex.put(numberOfPatternsInPrefixTree++, newChild);
		tempProcessingNode.addChild(newChild);
		traversalQueue.add(newChild);

		if (newCreatedOrTouchedPTNodes != null)
			newCreatedOrTouchedPTNodes.add(newChild.getData().getPatternPrefixTreeNodeIndex());

		if (DummyProperties.debugMode) {
			// if (newChild.getData().getPatternPrefixTreeNodeIndex() == 11) {
			// System.out.println("");
			// }

			System.out.println("newChild:" + newChild.getLevel() + " edgeSet size: "
					+ newChild.getData().getPatternGraph().edgeSet().size());
			System.out.println(newChild.getData().getMappedGraphString());
		}

		this.getLabelAdjacencyIndexer().prefixTreeNodesOfALevel.putIfAbsent(newChild.getLevel(),
				new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>());
		this.getLabelAdjacencyIndexer().prefixTreeNodesOfALevel.get(newChild.getLevel()).add(newChild);

		creationOfNewPrefixTreeNodeDuration += ((System.nanoTime() - creationOfNewPrefixTreeNodeStart) / 1e6);

		return newChild;

	}

	private void getDestPatternNodeAndCheckForGoBackToPrev(ArrayList<PatternNode> destPatternNodes,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
			Integer newNodeId, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		// PatternNode destPatternNode = null;
		goBackToPrev = false;
		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			if ((patternNode != srcPatternNode)
					&& (!tempProcessingNodeData.getPatternGraph().containsEdge(srcPatternNode, patternNode))) {
				if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						.contains(newNodeId)
				// && (tempProcessingNodeData.getPatternGraph().inDegreeOf(
				// patternNode) <
				// labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree)
				) {
					// repeated node:
					// HashSet<PatternNode>
					// destPatternNodeSet =
					// tempProcessingNodeData.getMatchedNodes().patternNodeOfNeo4jNode
					// .get(newNodeId);
					// destPatternNode =
					// destPatternNodeSet.iterator().next();

					destPatternNodes.add(patternNode);

					goBackToPrev = true;

				}
			}
		}
		if (!goBackToPrev && srcDataGpNodeId == newNodeId) {
			// handling self-loop
			destPatternNodes.add(srcPatternNode);
			goBackToPrev = true;
			// destPatternNode.addRelType(relationshipType);
			// destDataGraphPatternNodeId = newNodeId;
		}

		if (!goBackToPrev) {
			// if we can find another focus
			// node, if
			// anything
			// remaining to find.
			if (tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
				for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
					if (allNodesOfFocusType.get(type).contains(newNodeId)) {
						destPatternNodes.add(new PatternNode(otherNodeLabel, The_Focus_Node));
					}
				}
			}

			// if we already found all the focus
			// nodes, all this
			// labels is not in our focus list
			if (destPatternNodes.size() == 0) {
				destPatternNodes.add(new PatternNode(otherNodeLabel));
			}

			// destDataGraphPatternNode = new
			// DataGraphMatchNode(newNodeId,
			// destPatternNode.getType(),
			// srcDataGpNode.stepsFromRoot + 1,
			// dataGraph.getNodeById(newNodeId).getDegree(Direction.OUTGOING));
		}
	}

	private void print(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
		ArrayList<String> absGraphEdges = new ArrayList<String>();
		String returnValue = "";

		for (DefaultLabeledEdge e : newAbsPattern.edgeSet()) {
			absGraphEdges.add((newAbsPattern.getEdgeSource(e).getType())
					// + (newAbsPattern.getEdgeSource(e).isFocus() ? "*" : "")
					+ "->" + (newAbsPattern.getEdgeTarget(e).getType())
					// + (newAbsPattern.getEdgeTarget(e).isFocus() ? "*" : "")
					+ ", ");
		}
		Collections.sort(absGraphEdges);

		for (String v : absGraphEdges) {
			returnValue += v;
		}

		if (DummyProperties.debugMode)
			System.out.println(returnValue);

	}

	public boolean preIsoChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
		numberOfIsoCheckingRequest++;
		isoTimeStart = System.nanoTime();
		// SHAYAN
		// TODO: finalize it
		// if two patterns don't have same number of nodes?
		if (abstractPatternGraph.vertexSet().size() != newAbsPattern.vertexSet().size()) {
			isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
			return false;
		}

		// if two patterns don't have same number of edges?
		if (abstractPatternGraph.edgeSet().size() != newAbsPattern.edgeSet().size()) {
			isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
			return false;
		}

		// TODO: degree-distribution & label distribution checking
		// before isomorphism checking

		// if they don't have same label distribution?

		// .....
		isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
		return true;

	}

	public VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsomorphism(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {

		return getIsoChecker(abstractPatternGraph, newAbsPattern);

	}

	private VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsoChecker(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {

		isoTimeStart = System.nanoTime();

		VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = new VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge>(
				abstractPatternGraph, newAbsPattern, new Comparator<PatternNode>() {

					@Override
					public int compare(PatternNode v1, PatternNode v2) {
						if (v1.getType().equals(v2.getType()))
							return 0;

						return 1;
					}

				}, new Comparator<DefaultLabeledEdge>() {

					@Override
					public int compare(DefaultLabeledEdge e1, DefaultLabeledEdge e2) {
						if (e1.getType().equals(e2.getType()))
							return 0;

						return 1;
					}
				});

		isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
		numberOfRealIsoChecking++;

		return iso;

	}

	public void expandForNewInsertedEdge(Node srcNode, Integer srcNodeId, String srcLabel, Node destNode,
			Integer destNodeId, String destLabel, boolean srcIsAFocus, boolean destIsAFocus,
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
			int snapshot, double threshold,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns, String relationshipType)
			throws Exception {

		// TODO: thinking about if goBackToPrev affect sth here.
		// String destLabel = destNode.getLabels().iterator().next().name();
		/// String destPatternLabel_RelType = destLabel +
		/// DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType;

		// because may be one srcId existed
		// in different PatternNodes of a Pattern,
		// we should expand all of them

		HashSet<PatternNode> srcPatternNodes = new HashSet<PatternNode>(
				prefixTreeNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId));

		for (PatternNode srcPatternNode : srcPatternNodes) {
			if (DummyProperties.debugMode) {
				System.out.println("srcPatternNode: " + srcPatternNode + "srcNodeId: " + srcNodeId + ", destNodeId: "
						+ destNodeId + ", destLabel:" + destLabel);
			}

			HashMap<PrefixTreeNode<IPrefixTreeNodeData>, ArrayList<PatternNode>> thePTNodeBaseOnTheNewEdge = new HashMap<PrefixTreeNode<IPrefixTreeNodeData>, ArrayList<PatternNode>>();

			if (DummyProperties.debugMode) {
				System.out.println("incremental srcAbstractPatternNode : " + srcPatternNode.getType() + ""
						+ srcPatternNode.isFocus());
			}

			// if (srcNodeId == 5 && destNodeId == 6 &&
			// srcPatternNode.isFocus()) {
			// System.out.println("debug!");
			// }

			// because I assume we checked it before when finding affected
			// patterns
			if (prefixTreeNode.getData().getStepsFromRootOfPatternNodes().get(srcPatternNode) >= maxAllowedHops)
				// throw new Exception(srcPatternNode + " >= " +
				// maxAllowedHops);
				continue;

			Integer tempDestStepsFromRoot = prefixTreeNode.getData().getStepsFromRootOfPatternNodes()
					.get(srcPatternNode) + 1;

			// PatternNode tempDestPatternNode =
			ArrayList<PatternNode> destPatternNodes = new ArrayList<PatternNode>();
			getDestPatternNodeAndCheckForGoBackToPrev(destPatternNodes, prefixTreeNode.getData(), srcPatternNode,
					srcNodeId, destNodeId, destLabel, relationshipType, tempDestStepsFromRoot);

			for (int m = 0; m < destPatternNodes.size(); m++) {
				PatternNode tempDestPatternNode = destPatternNodes.get(m);

				// may be we can reach from other pathes in shorter steps.
				if (goBackToPrev) {
					tempDestStepsFromRoot = Math.min(tempDestStepsFromRoot,
							prefixTreeNode.getData().getStepsFromRootOfPatternNodes().get(tempDestPatternNode));
				} else {
					// we should try other ways to make sure that we don't
					// create
					// any extra patterns here:
					// only when we can create new dest pattern node that we are
					// allowed to!
					HashMap<String, Integer> frequencyOfSameOutNeighborType = getFrequencyOfSameOutNeighborType(
							prefixTreeNode.getData(), srcPatternNode);

					HashSet<Integer> sameLabelNeighborNodes = labelAdjacencyIndexer.distinctNodesOfDHopsAway
							.get(srcNodeId)
							.get(destLabel + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
							.get(Indexer.AFTER);

					// if we cannot create another dest pattern node,
					// we should use the correct previous one

					if (frequencyOfSameOutNeighborType
							.containsKey(destLabel + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
							&& frequencyOfSameOutNeighborType
									.get(destLabel + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
											+ relationshipType) >= sameLabelNeighborNodes.size()) {

						for (DefaultLabeledEdge e : prefixTreeNode.getData().getPatternGraph()
								.outgoingEdgesOf(srcPatternNode)) {
							if (e.getType().equals(relationshipType)
									&& prefixTreeNode.getData().getPatternGraph().getEdgeTarget(e).getType()
											.equals(tempDestPatternNode.getType())
									&& prefixTreeNode.getData().getStepsFromRootOfPatternNodes()
											.get(prefixTreeNode.getData().getPatternGraph().getEdgeTarget(e))
											.equals(tempDestStepsFromRoot)) {

								thePTNodeBaseOnTheNewEdge.putIfAbsent(prefixTreeNode, new ArrayList<PatternNode>());
								thePTNodeBaseOnTheNewEdge.get(prefixTreeNode)
										.add(prefixTreeNode.getData().getPatternGraph().getEdgeTarget(e));

								// for efficiency
								if (frequencyOfSameOutNeighborType
										.get(destLabel + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
												+ relationshipType) <= thePTNodeBaseOnTheNewEdge.get(prefixTreeNode)
														.size()) {
									break;
								}

							}
						}
					}

				}

				// ArrayList<PatternNode> tempDestPatternNodes = new
				// ArrayList<PatternNode>();
				// tempDestPatternNodes.add(tempDestPatternNode);

				// search in children if the new pattern is there
				// we should add the new potential matches there.
				if (thePTNodeBaseOnTheNewEdge.isEmpty()) {
					for (PrefixTreeNode<IPrefixTreeNodeData> childPTNode : prefixTreeNode.getChildren()) {
						// from same source go the same destLabel and type
						if (sameChildLinkedPattern(childPTNode, srcPatternNode, tempDestPatternNode,
								tempDestStepsFromRoot, relationshipType)) {
							thePTNodeBaseOnTheNewEdge.put(childPTNode, new ArrayList<PatternNode>());

							// because if "sameChildLinkedPattern" returns true
							// it
							// means
							// that src->target
							thePTNodeBaseOnTheNewEdge.get(childPTNode)
									.add(childPTNode.getData().getTargetPatternNode());

							// fillDestPatternNodesCorrectly(childPTNode.getData(),
							// srcPatternNode,
							// thePTNodeBaseOnTheNewEdge.get(childPTNode),
							// destLabel,
							// destIsAFocus, tempDestStepsFromRoot,
							// relationshipType);

							// if
							// (thePTNodeBaseOnTheNewEdge.get(childPTNode).size()
							// ==
							// 0) {
							// System.err.println(
							// "no dest pattern is found for childPTNode!
							// thePTNodeBaseOnTheNewEdge.get(childPTNode).size()
							// ==
							// 0");
							// thePTNodeBaseOnTheNewEdge.get(childPTNode).add(childPTNode.getData().targetPatternNode);
							//// throw new Exception(
							//// "no dest pattern is found for childPTNode!
							// thePTNodeBaseOnTheNewEdge.get(childPTNode).size()
							// ==
							// 0");
							//
							//
							// }

							if (DummyProperties.debugMode) {
								System.out.println("the pattern is found in the children!: "
										+ childPTNode.getData().getMappedGraphString());
							}

						}
					}
				}

				if (thePTNodeBaseOnTheNewEdge.isEmpty() && prefixTreeNode.getLinkedNodes() != null) {
					// search in linked node if the new pattern is there
					// we should add the new potential matches there.
					for (PrefixTreeNode<IPrefixTreeNodeData> linkedNode : prefixTreeNode.getLinkedNodes()) {
						if (sameChildLinkedPattern(linkedNode, srcPatternNode, tempDestPatternNode,
								tempDestStepsFromRoot, relationshipType)) {
							thePTNodeBaseOnTheNewEdge.put(linkedNode, new ArrayList<PatternNode>());

							// because we checked it just with target node.
							thePTNodeBaseOnTheNewEdge.get(linkedNode).add(linkedNode.getData().getTargetPatternNode());
							// fillDestPatternNodesCorrectly(linkedNode.getData(),
							// srcPatternNode,
							// thePTNodeBaseOnTheNewEdge.get(linkedNode),
							// destLabel,
							// destIsAFocus,
							// tempDestStepsFromRoot, relationshipType);

							// if
							// (thePTNodeBaseOnTheNewEdge.get(linkedNode).size()
							// == 0) {
							// System.err.println(
							// "no dest pattern is found for linkedNode!
							// thePTNodeBaseOnTheNewEdge.get(linkedNode).size()
							// ==
							// 0");
							//
							// thePTNodeBaseOnTheNewEdge.get(linkedNode).add(tempDestPatternNode);
							//
							// }
							if (DummyProperties.debugMode) {
								System.out.println("the pattern is found in the linked nodes! "
										+ linkedNode.getData().getMappedGraphString());
							}
						}
					}
				}

				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newerPatternGraph = null;

				if (thePTNodeBaseOnTheNewEdge.isEmpty()) {
					newerPatternGraph = createNewPatternFromParentPattern(prefixTreeNode, srcPatternNode,
							tempDestPatternNode, relationshipType);
				}

				if (thePTNodeBaseOnTheNewEdge.isEmpty()) {
					boolean foundInPrevSeen = false;
					for (Integer newCreatedOrTouchedPTNodeIndex : newCreatedOrTouchedPTNodes) {
						PrefixTreeNode<IPrefixTreeNodeData> temp = prefixTreeNodeIndex
								.get(newCreatedOrTouchedPTNodeIndex);
						if (preIsoChecking(newerPatternGraph, temp.getData().getPatternGraph())
								&& getIsomorphism(newerPatternGraph, temp.getData().getPatternGraph())
										.isomorphismExists()) {
							// it means that we could reach from parent pattern
							// to
							// this pattern
							addIncNodeLink(prefixTreeNode, temp);

							if (DummyProperties.debugMode) {
								System.out.println(
										"the pattern is found in the newly touched or created patterns so we don't need go further! "
												+ temp.getData().getMappedGraphString());
							}

							// so we need a node-link and then nothing
							// because before we enumerated it
							foundInPrevSeen = true;
							break;
						}
					}

					if (foundInPrevSeen) {
						continue;
					}
				}

				if (thePTNodeBaseOnTheNewEdge.isEmpty()) {
					// TODO: thinking for a better perfomance.
					// may be PTNode by level indexing

					int childLevel = prefixTreeNode.getLevel() + 1;
					for (Integer ptIndex : prefixTreeNodeIndex.keySet()) {
						if (prefixTreeNodeIndex.get(ptIndex).getLevel() == childLevel) {

							/**
							 * // we don't need check isomorphism for children
							 * // of this prefixTreeNode because we checked //
							 * earlier if
							 * (prefixTreeNode.getChildren().contains(
							 * prefixTreeNodeIndex.get(ptIndex)) ||
							 * (prefixTreeNode.getLinkedNodes() != null &&
							 * prefixTreeNode.getLinkedNodes()
							 * .contains(prefixTreeNodeIndex.get(ptIndex)))) {
							 * continue; }
							 **/

							// TODO: see if we can get corresponding nodes from
							// ISO checking
							VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso;
							if (preIsoChecking(newerPatternGraph,
									prefixTreeNodeIndex.get(ptIndex).getData().getPatternGraph())
									&& (iso = getIsomorphism(newerPatternGraph,
											prefixTreeNodeIndex.get(ptIndex).getData().getPatternGraph()))
													.isomorphismExists()) {

								// if (srcNodeId == 4 && destNodeId == 8) {
								// System.out.println("");
								// }

								thePTNodeBaseOnTheNewEdge.put(prefixTreeNodeIndex.get(ptIndex),
										new ArrayList<PatternNode>());

								// int steps = tempDestStepsFromRoot;

								Iterator<GraphMapping<PatternNode, DefaultLabeledEdge>> mappingItr = iso.getMappings();
								// what is iteration for? if there is two
								// possible
								// mapping?
								PatternNode tempDestPatternNodeIso = null;
								while (mappingItr.hasNext()) {
									GraphMapping<PatternNode, DefaultLabeledEdge> map = mappingItr.next();
									// make sure that this "true" is correct
									// rational here is I'm giving the source
									// node
									// of the newerPatternGraph
									// and I request to get the corresponding
									// node
									// in the previous prefix
									// tree node.

									srcPatternNode = map.getVertexCorrespondence(srcPatternNode, true);
									tempDestPatternNodeIso = map.getVertexCorrespondence(tempDestPatternNode, true);
									break;
								}
								thePTNodeBaseOnTheNewEdge.get(prefixTreeNodeIndex.get(ptIndex))
										.add(tempDestPatternNodeIso);

								// fillDestPatternNodesCorrectly(prefixTreeNodeIndex.get(ptIndex).getData(),
								// srcPatternNode,
								// thePTNodeBaseOnTheNewEdge.get(prefixTreeNodeIndex.get(ptIndex)),
								// destLabel,
								// destIsAFocus, steps, relationshipType);

								// if
								// (thePTNodeBaseOnTheNewEdge.get(prefixTreeNodeIndex.get(ptIndex)).size()
								// == 0) {
								// System.err.println(
								// "dest pattern is not found from SGI
								// thePTNodeBaseOnTheNewEdge.get(prefixTreeNodeIndex.get(ptIndex)).size()
								// == 0");
								// thePTNodeBaseOnTheNewEdge.get(prefixTreeNodeIndex.get(ptIndex))
								// .add(tempDestPatternNodeIso);
								//
								// }

								addIncNodeLink(prefixTreeNode, prefixTreeNodeIndex.get(ptIndex));

								// srcPatternNode =
								// fillSourcePatternNodeForSGICase(
								// prefixTreeNodeIndex.get(ptIndex).getData().getPatternGraph(),
								// srcPatternNode);
								if (DummyProperties.debugMode) {
									System.out.println("the pattern is found by SGI on a level! "
											+ prefixTreeNodeIndex.get(ptIndex).getData().getMappedGraphString());
								}

								break;
							}
						}
					}
				}
				boolean newPrefixTreeNodeIsCreated = false;
				if (!thePTNodeBaseOnTheNewEdge.isEmpty()) {
					for (PrefixTreeNode<IPrefixTreeNodeData> thePTNode : thePTNodeBaseOnTheNewEdge.keySet()) {
						// TODO: important optimization we may reach to previous
						// seen children but
						// don't understand that it has been seen before

						if (thePTNodeBaseOnTheNewEdge.get(thePTNode).size() > 1) {
							if (DummyProperties.debugMode) {
								System.out.println(" different destpattern nodes for update: "
										+ (thePTNodeBaseOnTheNewEdge.get(thePTNode).size()));
							}
						}
						if (!newCreatedOrTouchedPTNodes.contains(thePTNode.getData().getPatternPrefixTreeNodeIndex())) {
							for (PatternNode destPatternNode : thePTNodeBaseOnTheNewEdge.get(thePTNode)) {

								// if
								// (prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
								// == 199) {
								// System.out.println();
								// }
								thePTNode.getData().addNewMatchForUpdate(prefixTreeNode, srcPatternNode, srcNodeId,
										destPatternNode, destNodeId, this.labelAdjacencyIndexer);
							}

							if (!thePTNode.getData().isValid())
								thePTNode.getData().setValid(checkValidityAtLeastOneMatchForEachPatternNode(thePTNode));
						}

					}

				}

				// if we couldn't find it yet we should create a new PTNode
				else {
					thePTNodeBaseOnTheNewEdge.put(createNewPrefixTreeNode(prefixTreeNode, newerPatternGraph,
							srcPatternNode, tempDestPatternNode, srcNodeId, destNodeId, newCreatedOrTouchedPTNodes,
							relationshipType, tempDestStepsFromRoot, !FRESH_SOURCE, snapshot), null);
					newPrefixTreeNodeIsCreated = true;
					if (DummyProperties.debugMode)
						System.out.println("the pattern isn't found, so a new pattern is created!");
				}

				if (traversalQueue.size() > 1) {
					throw new Exception("why traversalQueue has more items?!");
				}

				if (DummyProperties.debugMode)
					System.out.println("thePTNodeBaseOnTheNewEdge size: " + thePTNodeBaseOnTheNewEdge.size());

				for (PrefixTreeNode<IPrefixTreeNodeData> thePTNode : thePTNodeBaseOnTheNewEdge.keySet()) {
					// thePTNode.getData().isValid() =
					// checkValidityAtLeastOneMatchForEachPatternNode(thePTNode);

					if (newPrefixTreeNodeIsCreated || !newCreatedOrTouchedPTNodes
							.contains(thePTNode.getData().getPatternPrefixTreeNodeIndex())) {
						newCreatedOrTouchedPTNodes.add(thePTNode.getData().getPatternPrefixTreeNodeIndex());

						// if (hasSameNeighborsWithLessMatch(thePTNode)) {
						// thePTNode.getData().isValid() = false;
						// thePTNode.getData().isFrequent() = false;
						// thePTNode.getData().isMaximalFrequent() = false;
						// thePTNode.getData().isVerified = true;
						// }
						//
						// if (!thePTNode.getData().isValid())
						// continue;
						if (DummyProperties.debugMode)
							System.out.println("start from here: the prifixTree node related to the new added edge: "
									+ thePTNode.getData().getMappedGraphString());

						if (!traversalQueue.contains(thePTNode))
							traversalQueue.add(thePTNode);

						generatePrefixTreeFromHere(topKFrequentPatterns, newCreatedOrTouchedPTNodes,
								PrefixTreeMode.UPDATE, snapshot, threshold);
					}
				}
			}
		}

	}

	private void addIncNodeLink(PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			PrefixTreeNode<IPrefixTreeNodeData> temp) throws Exception {

		if (!prefixTreeNode.getChildren().contains(temp)
				&& (prefixTreeNode.getLinkedNodes() == null || !prefixTreeNode.getLinkedNodes().contains(temp))) {
			if (DummyProperties.debugMode) {
				System.out.println("INC: add node link from " + prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
						+ " to " + temp.getData().getPatternPrefixTreeNodeIndex());
			}
			prefixTreeNode.addNodeLink(temp);
		}

		// because prefixTreeNode right now has at least one
		// child
		if (prefixTreeNode.getData().isMaximalFrequent() && temp.getData().isFrequent()) {
			prefixTreeNode.getData().setCanBeMaximalFrequent(false);
			prefixTreeNode.getData().setMaximalFrequent(false, prefixTreeNode, this);
			mfpPrefixTreeNodes.remove(prefixTreeNode);
			prefixTreeNode.getData().removeFromTopK(this, prefixTreeNode);
		}

	}

	// TODO: may be better to find invalid prefix nodes at the time that we
	// shrink them or add new matches to them
	// not at the time that we want to expand them
	private boolean checkValidityAtLeastOneMatchForEachPatternNode(
			PrefixTreeNode<IPrefixTreeNodeData> thePTNodeBaseOnTheNewEdge) {

		checkValidityAtLeastOneMatchForEachPatternNodeStartTime = System.nanoTime();

		boolean isValid = true;
		for (PatternNode patternNode : thePTNodeBaseOnTheNewEdge.getData().getMatchedNodes()
				.getDataGraphMatchNodeOfAbsPNode().keySet()) {
			if (thePTNodeBaseOnTheNewEdge.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
					.size() < 1) {
				isValid = false;
				break;
			}
		}
		checkValidityAtLeastOneMatchForEachPatternNodeDuration += ((System.nanoTime()
				- checkValidityAtLeastOneMatchForEachPatternNodeStartTime) / 1e6);
		return isValid;
	}

	// TODO: may be we have to remove some from top-k even if they are valid
	// yet.
	public void shrinkForNewDeletedEdge(Node srcNode, Node destNode, boolean srcIsAFocus, boolean destIsAFocus,
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
			int snapshot, double threshold,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns, String relationshipType)
			throws Exception {

		// queue for processing this node and all of it's children
		Queue<PrefixTreeNode<IPrefixTreeNodeData>> shrinkageQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();
		shrinkageQueue.add(prefixTreeNode);

		Integer srcNodeId = (int) srcNode.getId();
		Integer destNodeId = (int) destNode.getId();
		String destLabel = destNode.getLabels().iterator().next().name();
		String sourceLabel = srcNode.getLabels().iterator().next().name();

		if (DummyProperties.debugMode) {
			System.out.println("srcNodeId: " + srcNodeId + ", destNodeId: " + destNodeId + ", destLabel:" + destLabel);
		}

		while (!shrinkageQueue.isEmpty()) {

			// get any affected prefixtree node (desendants of the main pt node)
			PrefixTreeNode<IPrefixTreeNodeData> thisNode = shrinkageQueue.poll();

			// if we checked it before we just move on
			if (newCreatedOrTouchedPTNodes.contains(thisNode.getData().getPatternPrefixTreeNodeIndex())) {
				continue;
			}

			// we should add it as a seen case for this edge deletion
			newCreatedOrTouchedPTNodes.add(thisNode.getData().getPatternPrefixTreeNodeIndex());
			if (DummyProperties.debugMode) {
				System.out.println("DEL-Affected: Before remove: " + thisNode.getData());
			}

			// commented becasue if I make it invalid then I don't know if it
			// was mfp or not
			// downward property
			// if (!thisNode.getParent().getData().isValid()) {
			// thisNode.getData().setPatternAsInvalid(thisNode, this, snapshot);
			// }

			// we should remove the corresponding destNodeId in the
			// destPatternNode
			if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId) == null) {
				if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) != null) {
					Iterator<PatternNode> destIterator = thisNode.getData().getMatchedNodes()
							.getPatternNodeOfNeo4jNode().get(destNodeId).iterator();

					while (destIterator.hasNext()) {
						PatternNode destPatternNode = destIterator.next();
						// if
						// (!thisNode.getData().getPatternGraph().vertexSet().contains(destPatternNode))
						// {
						// System.out.println();
						// }
						for (DefaultLabeledEdge e : thisNode.getData().getPatternGraph()
								.incomingEdgesOf(destPatternNode)) {
							if (thisNode.getData().getPatternGraph().getEdgeSource(e).getLabel().equals(sourceLabel)
									&& e.getType().equals(relationshipType)) {
								removeDestNodeForDelete(destIterator, thisNode, destNodeId, destPatternNode,
										topKFrequentPatterns, snapshot, threshold, relationshipType);
								break;
							}
						}
					}
				}
			} else {
				HashSet<PatternNode> srcPatternNodes = new HashSet<PatternNode>(
						thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId));

				for (PatternNode srcPatternNode : srcPatternNodes) {

					if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) != null) {
						Iterator<PatternNode> destIterator = thisNode.getData().getMatchedNodes()
								.getPatternNodeOfNeo4jNode().get(destNodeId).iterator();

						// TODO:it's a patch
						HashSet<PatternNode> patternNodesToIterate = new HashSet<PatternNode>();
						patternNodesToIterate.addAll(
								thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId));

						for (PatternNode destPatternNode : patternNodesToIterate) {

							// A->B->C->B
							if (!thisNode.getData().getPatternGraph().containsEdge(srcPatternNode, destPatternNode)) {
								if (DummyProperties.debugMode)
									System.out.println("this case is happened!");
								continue;
							}

							removeDestNodeForDelete(destIterator, thisNode, destNodeId, destPatternNode,
									topKFrequentPatterns, snapshot, threshold, relationshipType);

						}
					}
				}
			}

			for (PrefixTreeNode<IPrefixTreeNodeData> childNode : thisNode.getChildren()) {
				// if (childNode.getData().isValid())
				shrinkageQueue.add(childNode);

			}

			if (thisNode.getLinkedNodes() != null)
				for (PrefixTreeNode<IPrefixTreeNodeData> linkedNode : thisNode.getLinkedNodes()) {
					// if (linkedNode.getData().isValid())
					shrinkageQueue.add(linkedNode);
				}
			if (DummyProperties.debugMode) {
				System.out.println("DEL-Affected: After remove: " + thisNode.getData());
			}
		}
	}

	private void removeDestNodeForDelete(Iterator<PatternNode> destIterator,
			PrefixTreeNode<IPrefixTreeNodeData> thisNode, Integer destNodeId, PatternNode destPatternNode,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns, int snapshot,
			double threshold, String relationshipType) throws Exception {

		try {// TODO: it's a patch
			destIterator.remove();
		} catch (Exception exc) {
			return;
		}

		thisNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(destPatternNode).remove(destNodeId);

		// System.out.println("remove destNodeId: " + destNodeId + ",
		// destPatternNode: " + destPatternNode
		// + " patternPrefixTreeNodeIndex:" +
		// thisNode.getData().getPatternPrefixTreeNodeIndex());

		if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) == null
				|| thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId).size() == 0) {
			thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().remove(destNodeId);
			bitmap.removeNodeIdFromPatternId(destNodeId, thisNode.getData().getPatternPrefixTreeNodeIndex());
		}

		// we don't remove "destPatternNode",
		// we just remove its corresponding node ids

		boolean wasMFP = thisNode.getData().isMaximalFrequent();
		boolean wasFrequent = thisNode.getData().isFrequent();
		double prevTotalSupp = thisNode.getData().getTotalSupportFrequency();

		// if it's not valid we don't need to do anything further because we
		// already did!
		if (!thisNode.getData().isValid()) {
			return;
		}

		else if (thisNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(destPatternNode)
				.size() == 0) {
			// if it's invalid all it's children cannot have
			// support greater than zero
			thisNode.getData().setPatternAsInvalid(thisNode, this, snapshot);

			if (wasMFP) {
				thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
			} else if (wasFrequent) {
				thisNode.getData().freqToNonFreqHandling(thisNode);
			}

		} else {

			// thisNode.getData().setPatternAsUnEvaluated();

			double lowerbound = 0;
			if (topKFrequentPatterns.size() > 0) {
				if (topKFrequentPatterns.peekLast().getData() == null) {
					System.err.println("in shrink: topKFrequentPatterns.peekLast().getData() == null");
					topKFrequentPatterns.removeLast();
				} else {
					lowerbound = topKFrequentPatterns.peekLast().getData().getTotalSupportFrequency();
				}
			}

			DualSimulationHandler.computeSupport(dataGraph, thisNode, snapshot, this);

			// if support didn't change, we can easily return
			if (prevTotalSupp == thisNode.getData().getTotalSupportFrequency()) {
				return;
			}

			if (wasFrequent) {

				// estimateUpperbound(thisNode.getParent(), thisNode,
				// allNodesOfFocusType, destPatternNode,
				// relationshipType, snapshot);

				// if it has the potential to be a mfp pattern

				// if it cannot be a frequent pattern
				if (thisNode.getData().getTotalSupportFrequency() < threshold) {

					if (wasMFP)
						thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
					else
						thisNode.getData().freqToNonFreqHandling(thisNode);

				}

				// if it had the potential to be inside of the topk
				// and it's frequent yet
				else if (wasMFP && thisNode.getData().getTotalSupportFrequency() < lowerbound) {

					// so, as it's a MFP and it's freq yet,
					// nothing will be changed about it.
					// just it should be removed from top-k
					thisNode.getData().removeFromTopK(this, thisNode);

				} else if (wasMFP) {

					if (!thisNode.getData().isValid() || thisNode.getData().getTotalSupportFrequency() < threshold) {
						// it cannot be maximal frequent
						// and we should find one mfp in its ancestors
						thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
					}
					// it can be mfp but it cannot be inside of topk
					else if (thisNode.getData().getTotalSupportFrequency() < lowerbound) {
						thisNode.getData().removeFromTopK(this, thisNode);
					}

				}
			}

		}
	}

	private boolean sameChildLinkedPattern(PrefixTreeNode<IPrefixTreeNodeData> childPTNode, PatternNode srcPatternNode,
			PatternNode tempTargetPatternNode, Integer tempDestStepsFromRoot, String relationshipType)
			throws Exception {

		if (srcPatternNode != childPTNode.getData().getSourcePatternNode()) {
			return false;
		}

		if (childPTNode.getData().getTargetPatternNode() == tempTargetPatternNode) {
			return true;
		}

		// if
		// (childPTNode.getData().incomingRelTypesOfPatternNodes.get(tempTargetPatternNode)
		// == null) {
		// throw new Exception(
		// "childPTNode.getData().incomingRelTypesOfPatternNodes.get(tempTargetPatternNode)==null");
		// }

		return childPTNode.getData().getTargetPatternNode().getType().equals(tempTargetPatternNode.getType())
				&& childPTNode.getData().getStepsFromRootOfPatternNodes()
						.get(childPTNode.getData().getTargetPatternNode()).equals(tempDestStepsFromRoot)
				&& childPTNode.getData().getPatternGraph()
						.getEdge(srcPatternNode, childPTNode.getData().getTargetPatternNode()).getType()
						.equals(relationshipType);

	}

	private ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> createNewPatternFromParentPattern(
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode, PatternNode srcPatternNode, PatternNode destPatternNode,
			String relationshipType) {
		ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
				DefaultLabeledEdge.class);

		// TODO: it seems that we don't need to add vertexes first then edges
		// it seems that the second for can handle both jobs
		for (PatternNode patternNode : prefixTreeNode.getData().getPatternGraph().vertexSet()) {
			newPatternGraph.addVertex(patternNode);
		}

		for (DefaultLabeledEdge e : prefixTreeNode.getData().getPatternGraph().edgeSet()) {
			newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
		}

		if (!newPatternGraph.vertexSet().contains(destPatternNode)) {
			newPatternGraph.addVertex(destPatternNode);
		}

		newPatternGraph.addEdge(srcPatternNode, destPatternNode, new DefaultLabeledEdge(relationshipType));

		return newPatternGraph;
	}

	// private PatternNode fillSourcePatternNodeForSGICase(
	// ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>
	// getPatternGraph(),
	// PatternNode srcPatternNode) {
	//
	// for (PatternNode patternNode :
	// getPatternGraph().getPatternGraph().vertexSet()) {
	// if (patternNode.isFocus() == srcPatternNode.isFocus()
	// && patternNode.stepsFromRoot == srcPatternNode.stepsFromRoot
	// && patternNode.getLabel() == srcPatternNode.getLabel()
	// &&
	// getPatternGraph().incomingRelTypesOfPatternNodes.get(patternNode).equals()
	// patternNode.incomingRelTypes.equals(srcPatternNode.incomingRelTypes)) {
	// return patternNode;
	// }
	// }
	// return null;
	// }

	private void fillDestPatternNodesCorrectly(IPrefixTreeNodeData prefixTreeNodeData, PatternNode srcPatternNode,
			ArrayList<PatternNode> destPatternNodes, String destLabel, boolean destIsAFocus, int stepsFromRoot,
			String relationshipType) {

		for (DefaultLabeledEdge e : prefixTreeNodeData.getPatternGraph().outgoingEdgesOf(srcPatternNode)) {
			if (e.getType().equals(relationshipType)) {
				PatternNode destPatternNode = prefixTreeNodeData.getPatternGraph().getEdgeTarget(e);
				if (destPatternNode.isFocus() == destIsAFocus && prefixTreeNodeData.getStepsFromRootOfPatternNodes()
						.get(destPatternNode).equals(stepsFromRoot) && destPatternNode.getLabel().equals(destLabel)) {
					destPatternNodes.add(destPatternNode);
				}
			}
		}
	}

	public int bfsTraverse(PrefixTreeNode<IPrefixTreeNodeData> rootNode) {
		if (DummyProperties.debugMode)
			System.out.println("starting prefixTree BFS Traversal");
		Queue<PrefixTreeNode<IPrefixTreeNodeData>> bfsQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();
		bfsQueue.add(rootNode);
		int cnt = 0;
		while (!bfsQueue.isEmpty()) {
			PrefixTreeNode<IPrefixTreeNodeData> queueNode = bfsQueue.poll();
			cnt++; // root count included
			for (int i = 0; i < queueNode.getChildren().size(); i++) {
				bfsQueue.add(queueNode.getChildren().get(i));
			}

			numberOfTotalAllMatches += queueNode.getData().getNumerOfAllMatches();
			if (DummyProperties.debugMode) {
				System.out.println(queueNode.getData().getMappedGraphString());
				System.out.print("isMaximalFrequent: " + queueNode.getData().isMaximalFrequent() + ", ");
				System.out.print("isFrequent: " + queueNode.getData().isFrequent() + ", ");
				System.out.print("isValid: " + queueNode.getData().isValid() + ", ");
				System.out.print("isCorrect: " + queueNode.getData().isCorrect() + ", ");
				System.out.println("isVisited: " + queueNode.getData().isVisited());
				System.out.println("totalSup: " + queueNode.getData().getTotalSupportFrequency());
				System.out.println();

			}
		}
		if (DummyProperties.debugMode) {
			System.out.println("number of traversed nodes: " + cnt);
			System.out.println();
		}

		return cnt;

	}

	private void printTreeDualSim(PrefixTreeNode<IPrefixTreeNodeData> rootNode, GraphDatabaseService graphdb,
			PrefixTreeAlg1BiSim prefixTree) {
		Queue<PrefixTreeNode<IPrefixTreeNodeData>> bfsQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();
		bfsQueue.add(rootNode);
		Map<PatternNode, HashSet<Integer>> dsim;
		while (!bfsQueue.isEmpty()) {
			PrefixTreeNode<IPrefixTreeNodeData> queueNode = bfsQueue.poll();

			for (int i = 0; i < queueNode.getChildren().size(); i++) {
				bfsQueue.add(queueNode.getChildren().get(i));
			}

			if (queueNode.getData().getPatternGraph() != null) {
				dsim = BatDualSimulation.run(graphdb, queueNode.getData().getPatternGraph(), prefixTree);
				print(queueNode.getData().getPatternGraph());
				System.out.print("{");
				for (PatternNode patternNode : dsim.keySet()) {
					System.out.print(patternNode.getType() + "=[");
					for (Integer nodeId : dsim.get(patternNode)) {
						System.out.print(nodeId + ",");
					}
					System.out.print("]");
				}
				System.out.println("}");
				System.out.println();
			}
			// System.out.println(queueNode.getData().getMappedGraphString());

		}

	}

	@Override
	public MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> getTopKFrequentPatterns() {
		return this.topKFrequentPatterns;
	}

	public PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> getMfpPrefixTreeNodes() {
		return this.mfpPrefixTreeNodes;
	}

	public HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>> getPrefixTreeNodeIndex() {
		return this.prefixTreeNodeIndex;
	}

	@Override
	public Indexer getLabelAdjacencyIndexer() {
		return this.labelAdjacencyIndexer;
	}

	@Override
	public GraphDatabaseService getDataGraph() {
		return this.dataGraph;
	}

	@Override
	public double getThreshold() {
		return this.snapshotTopkMonitoring.threshold;
	}

	@Override
	public Bitmap getBitmap() {
		return this.bitmap;
	}

	@Override
	public PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> getMipPrefixTreeNodes() {
		return mipPrefixTreeNodes;
	}

	@Override
	public double getDurationOfIsoChecking() {
		return isoTimeDuration;
	}

	@Override
	public double getDurationOfNewPrefixTreeGeneration() {
		return creationOfNewPrefixTreeNodeDuration;
	}

	@Override
	public int getNumberOfComputeSupport() {
		return numberOfComputeSupport;
	}

	@Override
	public double getDurationOfComputeSupport() {
		return computeSupportDuration;
	}

	@Override
	public void incNumberOfComputeSupport() {
		numberOfComputeSupport++;
	}

	@Override
	public void updateDurationOfComputeSupport(double newDuration) {
		computeSupportDuration += newDuration;
	}

	@Override
	public void resetNumberOfIsoChecking() {
		numberOfIsoCheckingRequest = 0;
		numberOfRealIsoChecking = 0;

	}

	@Override
	public void resetDurationOfIsoChecking() {
		isoTimeDuration = 0;

	}

	public void resetNumberOfComputeSupport() {
		numberOfComputeSupport = 0;

	}

	public void resetDurationOfComputeSupport() {
		computeSupportDuration = 0;

	}

	public void resetDurationOfNewPrefixTreeGeneration() {
		creationOfNewPrefixTreeNodeDuration = 0;
	}

	@Override
	public double getDurationOfBiSimChecking() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNumberOfIsoCheckingRequest() {
		return numberOfIsoCheckingRequest;
	}

	@Override
	public long getNumberOfRealIsoChecking() {
		return numberOfRealIsoChecking;
	}

	@Override
	public long getNumberOfBiSimCheckingRequest() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNumberOfRealBiSimChecking() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void incrementBiSimCheckingRequest() {
		// TODO Auto-generated method stub

	}

	@Override
	public void incrementRealBiSimChecking() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDurationOfBiSimChecking(double newDuration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetDurationOfBiSimChecking() {

	}
}

// @Deprecated
// public PrefixTreeNode<PrefixTreeNodeData> generateForTopK(
// MinMaxPriorityQueue<PrefixTreeNode<PrefixTreeNodeData>> topKFrequentPatterns,
// int k, int snapshot)
// throws Exception {
//
// try (Transaction tx1 = dataGraph.beginTx()) {
// int prefixTreeProcessingLevel = 1;
// // while we didn't reach to the end of all d-hops pattern for these
// // nodes
// while (!traversalQueue.isEmpty()) {
//
// System.out.println();
// System.out.println("traversalQueue.size: " +
// traversalQueue.size());
//
// PrefixTreeNode<PrefixTreeNodeData> tempProcessingNode =
// traversalQueue.poll();
//
// if (prefixTreeProcessingLevel < tempProcessingNode.getLevel()) {
// // new sibling will be created soon.
// // old ones should be cleared
// sameLevelPrefixTreeNodes.clear();
// // going to the next level
// prefixTreeProcessingLevel = tempProcessingNode.getLevel();
// System.out.println("prefixTreeProcessingLevel: " +
// prefixTreeProcessingLevel);
//
// }
//
// System.out.println("generator processing node:");
// System.out.println(tempProcessingNode.getData().getMappedGraphString());
//
// // if size of the top-k is less than k every pattern
// // should be added to the topk list
// if (topKFrequentPatterns.size() < k /*
// * && tempProcessingNode.
// * getData().foundAllFocuse
// */) {
// computeSupport(dataGraph, tempProcessingNode, snapshot,
// topKFrequentPatterns);
// topKFrequentPatterns.add(tempProcessingNode);
// System.out.println(tempProcessingNode.getData().getMappedGraphString()
// + " -> supp:"
// + tempProcessingNode.getData().getSupportFrequency(snapshot));
// }
// // upperbound of this is greater than the lowerbound of the
// // current top-k
// else {
//
// // TODO: if k < number of distinct focus types
// // we have some problem
// double lowerbound =
// topKFrequentPatterns.peekLast().getData().getTotalSupportFrequency();
//
// // estimating upperbound:
// estimateUpperbound(tempProcessingNode.getParent(), tempProcessingNode,
// allNodesOfFocusType);
//
// System.out.println("lowerbound: " + lowerbound + " ->
// upperboundEstimation:"
// + tempProcessingNode.getData().upperboundEstimation);
//
// // >= : because we prefer to find maximual frequent pattern
// if ((tempProcessingNode.getData().upperboundEstimation < lowerbound)
// && tempProcessingNode.getData().foundAllFocuses) {
// continue;
// }
//
// // call dual-simulation for this pattern
// computeSupport(dataGraph, tempProcessingNode, snapshot,
// topKFrequentPatterns);
//
// if (tempProcessingNode.getData().getSupportFrequency(snapshot) == 0.0d)
// continue;
//
// if ((tempProcessingNode.getData().getTotalSupportFrequency() < lowerbound)
// && tempProcessingNode.getData().foundAllFocuses)
// continue;
//
// topKFrequentPatterns.add(tempProcessingNode);
// }
//
// if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
// continue;
// }
//
// processQueueNode(dataGraph, traversalQueue, sameLevelPrefixTreeNodes,
// tempProcessingNode, null,
// PrefixTreeMode.BATCH);
// }
//
// tx1.success();
//
// return emptyPTRootNode;
// } catch (Exception e) {
// e.printStackTrace();
// }
// return null;
// }

class PairStrings {
	public String key;
	public String value;

	public PairStrings(String key, String value) {
		this.key = key;
		this.value = value;

	}

}