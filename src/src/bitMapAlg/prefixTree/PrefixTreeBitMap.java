package src.bitMapAlg.prefixTree;

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
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.bitMapAlg.topkMonitoring.SnapshotTopkMonitoringBitMap;
import src.dualSimulation.gtar.BatDualSimulation;
import src.opt‌BiSimAlg.topkMonitoring.SnapshotTopkMonitoringOptBiSim;
import src.utilities.Bitmap;
import src.utilities.Bitmap2;
import src.utilities.DefaultLabeledEdge;
import src.utilities.DualSimulationHandler;
import src.utilities.Dummy;
import src.utilities.Indexer;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;
import src.utilities.SourceRelDestTriple;
import src.utilities.SupportComparator;
import src.utilities.Visualizer;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy.DummyProperties.Direction;
import src.utilities.Dummy.DummyProperties.PrefixTreeMode;

//TODO: we don't need to do isomorphism on all lowerlevel. we can use edge index information.
//TODO: make sure that isMaximalFrequent, isFrequent, isValid, isVerfied, isVisited, MFP queue, top-k queue are up to date at each state of the program.
//TODO: make sure that top-k list/MFP list don't have duplicated items in them 

public class PrefixTreeBitMap implements IPrefixTree {
	int cntVisualization = 0;
	SnapshotTopkMonitoringBitMap snapshotTopkMonitoring;
	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	private int maxAllowedHops;
	private int maxAllowedEdges;
	private String dataGraphPath;
	GraphDatabaseService dataGraph;
	public static final boolean The_Focus_Node = true;
	public static final boolean FRESH_SOURCE = true;
	public String whatIsFocus = "";

	// when we initialize a new child, we should add it here also
	public HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>> prefixTreeNodeIndex = new HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>>();
	// public HashMap<Integer, BiSimGroup> biSimGroupIndex = new
	// HashMap<Integer, BiSimGroup>();

	public int numberOfPatternsInPrefixTree = 0;
	public int numberOfBiSimGroupsInPrefixTree = 0;

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
	// ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> sameLevelPrefixTreeNodes;

	public PrefixTreeNode<IPrefixTreeNodeData> emptyPTRootNode = null;
	boolean goBackToPrev = false;
	public Bitmap2 bitmap;
	public int numberOfAllFocusNodes = 0;
	public int numberOfTotalAllMatches = 0;

	private double isoTimeStart = 0d;
	private double isoTimeDuration = 0d;

	private long numberOfIsoCheckingRequest = 0;
	private long numberOfRealIsoChecking = 0;

	private long numberOfBiSimCheckingRequest = 0;
	private long numberOfRealBiSimChecking = 0;

	private double biSimTimeDuration = 0d;

	private double creationOfNewPrefixTreeNodeStart = 0d;
	private double creationOfNewPrefixTreeNodeDuration = 0d;

	private double danglingCreationStartTime = 0d;
	public double danglingCreationDuration = 0d;
	public int numberOfDangling = 0;

	private double checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime = 0d;
	public double checkSameTypeSameStepsFromRootHasEnoughMatchesDuration = 0d;

	private double checkValidityAtLeastOneMatchForEachPatternNodeStartTime = 0d;
	public double checkValidityAtLeastOneMatchForEachPatternNodeDuration = 0d;

	private double processQueueNodeStartTime = 0d;
	public double processQueueNodeDuration = 0d;

	private double generatePrefixTreeFromHereStartTime = 0d;
	public double generatePrefixTreeFromHereDuration = 0d;

	private int numberOfComputeSupport = 0;
	private double computeSupportDuration = 0d;

	public PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> mfpPrefixTreeNodes = new PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>>(
			100, new SupportComparator());

	public PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> mipPrefixTreeNodes = new PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>>(
			100, new SupportComparator());

	MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns;

	public PrefixTreeBitMap(String[] args) throws Exception {

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

	public PrefixTreeBitMap(SnapshotTopkMonitoringBitMap snapshotTopkMonitoring) throws Exception {
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
	public PrefixTreeBitMap(String focusSetPath, int maxAllowedHops, int maxAllowedEdges,
			GraphDatabaseService dataGraph, boolean debugMode, Bitmap2 bitMap,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) throws Exception {

		this.focusSetPath = focusSetPath;
		this.maxAllowedHops = maxAllowedHops;
		this.maxAllowedEdges = maxAllowedEdges;
		this.dataGraph = dataGraph;
		this.debugMode = debugMode;
		this.bitmap = bitMap;
		emptyPTRootNode = initializePrefixTree();
		this.topKFrequentPatterns = topKFrequentPatterns;
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
			whatIsFocus += line;
		}
		br.close();
	}

	public static void main(String[] args) throws Exception {

		PrefixTreeBitMap prefixTree = new PrefixTreeBitMap(args);
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
		IPrefixTreeNodeData emptyPTRootData = new PrefixTreeNodeDataBitMap(focusLabelSet);
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
		// sameLevelPrefixTreeNodes = new
		// ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();

		labelAdjacencyIndexer = new Indexer(dataGraph, 1, allNodesOfFocusType);

		// we don't need to worry about their existence after that.
		for (String focusLabel : allNodesOfFocusType.keySet()) {

			PatternNode focusNode = new PatternNode(focusLabel, The_Focus_Node);

			HashSet<Integer> dgGraphMatchNodes = new HashSet<Integer>();
			for (Integer nodeId : allNodesOfFocusType.get(focusLabel)) {
				dgGraphMatchNodes.add(nodeId);
			}
			IPrefixTreeNodeData firstLevelChildData = new PrefixTreeNodeDataBitMap(focusNode, dgGraphMatchNodes,
					focusLabelSet, numberOfPatternsInPrefixTree, this.labelAdjacencyIndexer);

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

		generatePrefixTreeFromHereStartTime = System.nanoTime();
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

			// if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
			// == 0) {
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

			if (!tempProcessingNode.getData().isValid()) {
				tempProcessingNode.getData().setVisited(true);
				continue;
			}

			boolean createdDangling = false;
			try {

				boolean wasMFP = tempProcessingNode.getData().isMaximalFrequent();
				boolean wasMIP = tempProcessingNode.getData().isMinimalInFrequent();

				// check for dual-simulation in an incremental way
				if (wasMFP || wasMIP || !tempProcessingNode.getData().isVisited()
						|| tempProcessingNode.getData().getPrefixTreeMode() == PrefixTreeMode.UPDATE) {
					DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, snapshot, this);

					if (/* maxAllowedHops > 1 && */ !tempProcessingNode.getData().isDanglingPattern()
							&& tempProcessingNode.getData().getPatternGraph().edgeSet().size() >= 2
							&& tempProcessingNode.getData().getPatternGraph().edgeSet().size() < maxAllowedEdges) {

						createdDangling = createDanglingPrefixTreeNodesIfNeeded(tempProcessingNode,
								newCreatedOrTouchedPTNodes, 0);

					}
				}

				// if
				// (!checkSameTypeSameStepsFromRootHasEnoughMatches(tempProcessingNode))
				// {
				// tempProcessingNode.getData().setCorrectness(false,
				// tempProcessingNode, this, 0);
				// continue;
				// }

				if (!tempProcessingNode.getData().isValid()) {
					tempProcessingNode.getData().setVisited(true);
					continue;
				}

				// if it's frequent right now
				// new frequent child
				if (!tempProcessingNode.getData().isVisited()
						&& tempProcessingNode.getData().getTotalSupportFrequency() < threshold) {
					tempProcessingNode.getData().setMinimalInFrequent(true, tempProcessingNode, this);
				}

				else if ((!tempProcessingNode.getData().isVisited() || wasMIP)
						&& tempProcessingNode.getData().getTotalSupportFrequency() >= threshold) {

					tempProcessingNode.getData().setAsMFP(tempProcessingNode, tempProcessingNode.getParent(),
							tempProcessingNode.getSuperNodeLinks(), this, snapshot);
				}

				// if it's not frequent right now
				else if (wasMFP && tempProcessingNode.getData().getTotalSupportFrequency() < threshold) {
					tempProcessingNode.getData().maxFreqToNonFreqHandling(tempProcessingNode, this, snapshot);
				}

				// we just offer new MFP to the top-k list
				if (// !tempProcessingNode.getData().isDanglingPattern() &&
				tempProcessingNode.getData().isMaximalFrequent() && tempProcessingNode.getData().getFoundAllFocuses()) {
					tempProcessingNode.getData().addToTopK(this, tempProcessingNode);
				}

				tempProcessingNode.getData().setVisited(true);

				if (DummyProperties.debugMode)
					System.out.println(tempProcessingNode.getData().getMappedGraphString() + " -> supp:"
							+ tempProcessingNode.getData().getSupportFrequency(snapshot));

			} catch (Exception exc) {

				System.out.println(tempProcessingNode.getData().getMappedGraphString());
				throw exc;
			}

			if (!tempProcessingNode.getData().isValid()) {
				continue;
			}

			if (createdDangling) {
				if (Dummy.DummyProperties.debugMode) {
					System.out.println("createdDangling: " + tempProcessingNode.getData());
				}
				continue;
			}

			if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
				continue;
			}

			if (prefixTreeProcessingLevel < tempProcessingNode.getLevel()) {

				// Visualizer.visualizeALevel(this, prefixTreeProcessingLevel,
				// cntVisualization++);

				// new sibling will be created soon.
				// old ones should be cleared
				// sameLevelPrefixTreeNodes.clear();
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
			// if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
			// == 6) {
			// System.out.println();
			// }

			processQueueNodeStartTime = System.nanoTime();
			processQueueNode(dataGraph, traversalQueue, prefixTreeProcessingLevel + 1, tempProcessingNode,
					newCreatedOrTouchedPTNodes, prefixTreeMode, snapshot);

			processQueueNodeDuration += ((System.nanoTime() - processQueueNodeStartTime) / 1e6);
			//// FOR-DEBUG END}

			generatePrefixTreeFromHereDuration += ((System.nanoTime() - generatePrefixTreeFromHereStartTime) / 1e6);
		}

		// if (DummyProperties.debugMode) {
		// Visualizer.visualizeALevel(this, prefixTreeProcessingLevel,
		// cntVisualization++);
		// Visualizer.visualizeALevel(this, prefixTreeProcessingLevel + 1,
		// cntVisualization++);
		// }

		if (DummyProperties.debugMode)
			System.out.println("finishing queue!");

		return emptyPTRootNode;

	}

	private boolean createDanglingPrefixTreeNodesIfNeeded(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			HashSet<Integer> newCreatedOrTouchedPTNodes, int snapshot) throws Exception {
		if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex() == 175) {
			System.out.println("");
		}
		danglingCreationStartTime = System.nanoTime();

		boolean isCreated = false;
		if (DummyProperties.debugMode) {
			System.out.println("");
			System.out.println("createDanglingPrefixTreeNodesIfNeeded START");
		}

		// checking for neighbors if source node is the neighbor of focus
		// Set<DefaultLabeledEdge> edges =
		// tempProcessingNode.getData().getPatternGraph().getAllEdges(patternRootNode,
		// tempProcessingNode.getData().getSourcePatternNode());

		// boolean sourcePatternNodeIsACandidateForDangling = false;
		// if
		// (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
		// .get(tempProcessingNode.getData().getSourcePatternNode())
		// .size() != this.labelAdjacencyIndexer.candidateSetOfAPatternNode
		// .get(tempProcessingNode.getData().getSourcePatternNode()).size())
		// {
		// minCorrespondingPatternNode.add(e);
		// }

		// ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>
		// currentPatternGraph = tempProcessingNode.getData()
		// .getPatternGraph();
		// PatternNode sourceNode =
		// tempProcessingNode.getData().getSourcePatternNode();
		// PatternNode targetNode =
		// tempProcessingNode.getData().getTargetPatternNode();

		MyGraphPaths myGraphPaths = new MyGraphPaths(tempProcessingNode);

		HashMap<PatternNode, SelectedMinPatternNodeWithItsPath> minCorrespondingPatternNode = new HashMap<PatternNode, SelectedMinPatternNodeWithItsPath>();

		for (PatternNode rootNode : myGraphPaths.confirmedPaths.keySet()) {
			for (MyGraphPath myGraphPath : myGraphPaths.confirmedPaths.get(rootNode)) {
				if (myGraphPath.path.size() < 3)
					continue;
				for (int i = 1; i < myGraphPath.path.size(); i++) {

					// if
					// (tempProcessingNode.getParent().getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					// .get(myGraphPath.path.get(i)) == null) {
					// System.out.println();
					// }

					if (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
							.get(myGraphPath.path.get(i)).size() != labelAdjacencyIndexer.candidateSetOfAPatternNode
									.get(myGraphPath.path.get(i)).size()) {
						minCorrespondingPatternNode.put(myGraphPath.path.get(i),
								new SelectedMinPatternNodeWithItsPath(myGraphPath, i));
						break;
					}
				}
			}
		}

		for (PatternNode minPatternNode : minCorrespondingPatternNode.keySet()) {

			Set<Integer> remainingNodeIdsView = Sets.symmetricDifference(
					this.labelAdjacencyIndexer.candidateSetOfAPatternNode.get(minPatternNode), tempProcessingNode
							.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(minPatternNode));

			HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
			remainingNodeIds.addAll(remainingNodeIdsView);

			SelectedMinPatternNodeWithItsPath smpnwip = minCorrespondingPatternNode.get(minPatternNode);

			PatternNode prevMinPatternNode = smpnwip.myGraphPath.path.get(smpnwip.selectedMinPatternNode - 1);
			Direction prevDirection = smpnwip.myGraphPath.directions.get(smpnwip.selectedMinPatternNode);

			Set<DefaultLabeledEdge> incomingOrOutgoingEdges;
			if (prevDirection == Direction.OUTGOING) {
				incomingOrOutgoingEdges = tempProcessingNode.getData().getPatternGraph()
						.incomingEdgesOf(minPatternNode);
			} else {
				incomingOrOutgoingEdges = tempProcessingNode.getData().getPatternGraph()
						.outgoingEdgesOf(minPatternNode);
			}

			for (DefaultLabeledEdge incomingOrOutgoinE : incomingOrOutgoingEdges) {

				PatternNode oldDanglingPatternNode = null;
				if (prevDirection == Direction.OUTGOING) {

					if (tempProcessingNode.getData().getPatternGraph()
							.getEdgeSource(incomingOrOutgoinE) != prevMinPatternNode)
						continue;

					for (DefaultLabeledEdge eee : tempProcessingNode.getData().getPatternGraph()
							.outgoingEdgesOf(prevMinPatternNode)) {

						PatternNode dest = tempProcessingNode.getData().getPatternGraph().getEdgeTarget(eee);
						if (dest != minPatternNode) {
							if (dest.getType().equals(minPatternNode.getType())
									&& tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(dest)
											.equals(tempProcessingNode.getData().getStepsFromRootOfPatternNodes()
													.get(minPatternNode))
									&& tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
											.get(minPatternNode).equals(tempProcessingNode.getData()
													.getIncomingRelTypesOfPatternNodes().get(dest))) {
								remainingNodeIds.removeAll(tempProcessingNode.getData().getMatchedNodes()
										.getDataGraphMatchNodeOfAbsPNode().get(dest));

								if (tempProcessingNode.getData().getPatternGraph().outDegreeOf(dest) == 0) {
									oldDanglingPatternNode = dest;
								}
							}
						}
					}
				} else {

					if (tempProcessingNode.getData().getPatternGraph()
							.getEdgeTarget(incomingOrOutgoinE) != prevMinPatternNode)
						continue;

					for (DefaultLabeledEdge eee : tempProcessingNode.getData().getPatternGraph()
							.incomingEdgesOf(prevMinPatternNode)) {

						PatternNode dest = tempProcessingNode.getData().getPatternGraph().getEdgeSource(eee);
						if (dest != minPatternNode) {
							if (!tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
									.containsKey(minPatternNode)
									&& tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
											.containsKey(dest)) {

							} else if (dest.getType().equals(minPatternNode.getType())
									&& tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(dest)
											.equals(tempProcessingNode.getData().getStepsFromRootOfPatternNodes()
													.get(minPatternNode))
									&& ((tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
											.get(minPatternNode) == null
											&& tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
													.get(dest) == null)
											|| (tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
													.get(minPatternNode).equals(tempProcessingNode.getData()
															.getIncomingRelTypesOfPatternNodes().get(dest))))) {
								remainingNodeIds.removeAll(tempProcessingNode.getData().getMatchedNodes()
										.getDataGraphMatchNodeOfAbsPNode().get(dest));

								if (tempProcessingNode.getData().getPatternGraph().inDegreeOf(dest) == 0) {
									oldDanglingPatternNode = dest;
								}
							}
						}
					}
				}

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					if (patternNode != minPatternNode && patternNode.getLabel().equals(minPatternNode.getLabel())
							&& tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
									.get(patternNode).size() == 1) {
						remainingNodeIds.removeAll(tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
					}
				}

				if (remainingNodeIds.size() > 0) {

					String newNodeType = minPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
							+ incomingOrOutgoinE.getType();

					int max = 0;

					if (prevDirection == Direction.OUTGOING) {
						for (Integer nodeId : tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(prevMinPatternNode)) {

							if (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId).containsKey(newNodeType)
									&& labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId).get(newNodeType)
											.containsKey(Indexer.AFTER))
								max = Math.max(labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId)
										.get(newNodeType).get(Indexer.AFTER).size(), max);
						}

						if (max > tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
								.get(prevMinPatternNode).get(newNodeType)) {

							isCreated = innerDanglingCreation(oldDanglingPatternNode, remainingNodeIds,
									tempProcessingNode, snapshot, incomingOrOutgoinE, minPatternNode,
									prevMinPatternNode, newCreatedOrTouchedPTNodes, prevDirection);
						}
					} else {
						for (Integer nodeId : tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(prevMinPatternNode)) {

							if (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId).containsKey(newNodeType)
									&& labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId).get(newNodeType)
											.containsKey(Indexer.BEFORE))
								max = Math.max(labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId)
										.get(newNodeType).get(Indexer.BEFORE).size(), max);
						}

						if (max > tempProcessingNode.getData().getFrequencyOfPrevNeighborOfSameType()
								.get(prevMinPatternNode).get(newNodeType)) {

							isCreated = innerDanglingCreation(oldDanglingPatternNode, remainingNodeIds,
									tempProcessingNode, snapshot, incomingOrOutgoinE, minPatternNode,
									prevMinPatternNode, newCreatedOrTouchedPTNodes, prevDirection);
						}
					}

				}
			}
		}
		if (DummyProperties.debugMode) {
			// DebugHelper.printGlobalCandidateSet(this);
			System.out.println("createDanglingPrefixTreeNodesIfNeeded END");
			System.out.println("");
		}

		danglingCreationDuration += ((System.nanoTime() - danglingCreationStartTime) / 1e6);
		return isCreated;

	}

	private void createFromOldDangling(PatternNode oldDanglingPatternNode, HashSet<Integer> remainingNodeIds,
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, int snapshot) throws Exception {
		this.labelAdjacencyIndexer.candidateSetOfAPatternNode.get(oldDanglingPatternNode).addAll(remainingNodeIds);

		for (Integer nodeId : remainingNodeIds) {
			tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().putIfAbsent(nodeId,
					new HashSet<PatternNode>());
			tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(nodeId)
					.add(oldDanglingPatternNode);
		}

		DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, snapshot, this);

		if (DummyProperties.debugMode) {
			System.out.println("adding some new matches in dangling proc: " + tempProcessingNode.getData());

		}
	}

	private boolean innerDanglingCreation(PatternNode oldDanglingPatternNode, HashSet<Integer> remainingNodeIds,
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, int snapshot, DefaultLabeledEdge incomingOrOutgoinE,
			PatternNode minPatternNode, PatternNode prevMinPatternNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
			Direction prevDirection) throws Exception {
		boolean isCreated = false;

		if (oldDanglingPatternNode != null) {
			createFromOldDangling(oldDanglingPatternNode, remainingNodeIds, tempProcessingNode, snapshot);
		} else {

			ArrayList<PatternGraphAndPreMatches> newerPatternGraphs = new ArrayList<PatternGraphAndPreMatches>();
			// DefaultLabeledEdge.class

			SetView<Integer> intersectionOfTargetNodeAndRemainingIds = Sets.intersection(remainingNodeIds,
					tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
							.get(tempProcessingNode.getData().getTargetPatternNode()));

			SetView<Integer> disjointOfTargetNodeAndRemainingIds = Sets.difference(remainingNodeIds,
					tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
							.get(tempProcessingNode.getData().getTargetPatternNode()));

			// if there is any intersection we should create
			// a
			// pattern for that
			if (intersectionOfTargetNodeAndRemainingIds.size() > 0) {

				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newerPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
						DefaultLabeledEdge.class);

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					newerPatternGraph.addVertex(patternNode);
				}

				for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
					newerPatternGraph.addEdge(newerPatternGraph.getEdgeSource(e), newerPatternGraph.getEdgeTarget(e),
							e);
				}

				PatternNode danglingPatternNode = tempProcessingNode.getData().getTargetPatternNode();

				newerPatternGraph.addVertex(danglingPatternNode);

				if (prevDirection == Direction.OUTGOING) {
					newerPatternGraph.addEdge(
							tempProcessingNode.getData().getPatternGraph().getEdgeSource(incomingOrOutgoinE),
							danglingPatternNode, new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				} else {
					newerPatternGraph.addEdge(
							tempProcessingNode.getData().getPatternGraph().getEdgeTarget(incomingOrOutgoinE),
							danglingPatternNode, new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				}

				newerPatternGraphs.add(new PatternGraphAndPreMatches(newerPatternGraph,
						intersectionOfTargetNodeAndRemainingIds, danglingPatternNode));
			}

			// if there is no intersection => 0 and
			// remainingNodeIds >0
			// or some intersections
			if (intersectionOfTargetNodeAndRemainingIds.size() != remainingNodeIds.size()) {

				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newerPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
						DefaultLabeledEdge.class);

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					newerPatternGraph.addVertex(patternNode);
				}

				for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
					newerPatternGraph.addEdge(newerPatternGraph.getEdgeSource(e), newerPatternGraph.getEdgeTarget(e),
							e);
				}

				PatternNode danglingPatternNode = new PatternNode(minPatternNode.getLabel(), false);
				newerPatternGraph.addVertex(danglingPatternNode);

				if (prevDirection == Direction.OUTGOING) {
					newerPatternGraph.addEdge(
							tempProcessingNode.getData().getPatternGraph().getEdgeSource(incomingOrOutgoinE),
							danglingPatternNode, new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				} else {// ?? make sure that it's correct
					newerPatternGraph.addEdge(danglingPatternNode,
							tempProcessingNode.getData().getPatternGraph().getEdgeTarget(incomingOrOutgoinE),
							new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				}

				newerPatternGraphs.add(new PatternGraphAndPreMatches(newerPatternGraph,
						disjointOfTargetNodeAndRemainingIds, danglingPatternNode));
			}

			for (PatternGraphAndPreMatches newPatternGraphAndPreMatches : newerPatternGraphs) {
				boolean hadBiSimulInTheLevel = false;
				if (tempProcessingNode.getData().getPrefixTreeMode() == PrefixTreeMode.BATCH) {
					if (this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
							.get(tempProcessingNode.getLevel() + 1) != null) {
						for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeSibling : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
								.get(tempProcessingNode.getLevel() + 1)) {

							if (DualSimulationHandler.isBiDualSimulated(newPatternGraphAndPreMatches.patternGraph,
									prefixTreeSibling, this)) {

								if (DummyProperties.debugMode) {
									System.out.println("DANGLING: simultanous siblings: add node link from "
											+ tempProcessingNode.getData().getPatternPrefixTreeNodeIndex() + " to "
											+ prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex());
								}
								addIncNodeLink(tempProcessingNode, prefixTreeSibling, this);
								hadBiSimulInTheLevel = true;
								isCreated = true;
							}
						}
					}
				} else {
					if (this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
							.get(tempProcessingNode.getLevel() + 1) != null) {
						for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeSibling : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
								.get(tempProcessingNode.getLevel() + 1)) {

							HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;
							// if(tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()==31){
							// System.out.println();
							// }
							if (DualSimulationHandler.preBiSimChecking(prefixTreeSibling.getData().getPatternGraph(),
									newPatternGraphAndPreMatches.patternGraph, this)
									&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(
											newPatternGraphAndPreMatches.patternGraph, prefixTreeSibling,
											this)) != null) {

								for (PatternNode danglingPTN : dualBiSimMap
										.get(newPatternGraphAndPreMatches.danglingPatternNode)) {
									prefixTreeSibling.getData().addNewMatchForUpdateDangling(tempProcessingNode,
											danglingPTN, remainingNodeIds, this.labelAdjacencyIndexer);
								}

								if (DummyProperties.debugMode) {
									System.out.println(
											"DANGLING: new match for a child node! simultanous siblings: add node link from "
													+ tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
													+ " to "
													+ prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex());
								}
								addIncNodeLink(tempProcessingNode, prefixTreeSibling, this);
								hadBiSimulInTheLevel = true;
								isCreated = true;

								traversalQueue.add(prefixTreeSibling);
								newCreatedOrTouchedPTNodes
										.add(prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex());
							}
						}
					}
				}
				if (!hadBiSimulInTheLevel) {

					if (DummyProperties.debugMode) {
						System.out.println("new dangling child: "
								+ tempProcessingNode.getData().getPatternGraph().getEdgeSource(incomingOrOutgoinE)
								+ " -> " + newPatternGraphAndPreMatches.danglingPatternNode + " rem. nodeIds:"
								+ newPatternGraphAndPreMatches.preMatches);
						System.out.println(" from " + tempProcessingNode.getData());
					}

					numberOfDangling++;

					createNewPrefixTreeNode(tempProcessingNode, newPatternGraphAndPreMatches.patternGraph,
							prevMinPatternNode, newPatternGraphAndPreMatches.danglingPatternNode,
							newPatternGraphAndPreMatches.preMatches, newCreatedOrTouchedPTNodes,
							incomingOrOutgoinE.getType(),
							tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(minPatternNode), snapshot,
							null, true, prevDirection);

					isCreated = true;

				}
			}
		}
		return isCreated;
	}

	private boolean checkSameTypeSameStepsFromRootHasEnoughMatches(
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode) {

		checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime = System.nanoTime();

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
						checkSameTypeSameStepsFromRootHasEnoughMatchesDuration += ((System.nanoTime()
								- checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime) / 1e6);
						return false;
					}
				}

			}
		}
		checkSameTypeSameStepsFromRootHasEnoughMatchesDuration += ((System.nanoTime()
				- checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime) / 1e6);
		return true;
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

			if (DummyProperties.debugMode) {
				System.out.println();
				System.out.println("traversalQueue.size: " + traversalQueue.size());
			}

			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode = traversalQueue.poll();

			// if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
			// == 7) {
			// System.out.println();
			// }

			// TODO: it's a patch
			checkValidityAtLeastOneMatchForEachPatternNode(tempProcessingNode);

			// if (!tempProcessingNode.getData().isValid())
			// tempProcessingNode.getData()
			// .setValid(checkValidityAtLeastOneMatchForEachPatternNode(tempProcessingNode));

			// if (!tempProcessingNode.getData().isValid())
			// // TODO:make sure that "tempProcessingNode" doesn't take memory
			// continue;

			boolean createdDangling = false;
			try {
				// if
				// (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
				// == 942) {
				// System.out.println();
				// }
				// // check for dual-simulation in an incremental way
				DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, 0, this);
				// tempProcessingNode.getData()
				// .getTotalSupportFrequency() =
				// tempProcessingNode.getData().supportFrequency[0];
				if (DummyProperties.debugMode)
					System.out.println(tempProcessingNode.getData().getMappedGraphString() + " -> supp:"
							+ tempProcessingNode.getData().getSupportFrequency(0));

				if (/* maxAllowedHops > 1 && */ !tempProcessingNode.getData().isDanglingPattern()
						&& tempProcessingNode.getData().getPatternGraph().edgeSet().size() >= 2
						&& tempProcessingNode.getData().getPatternGraph().edgeSet().size() < maxAllowedEdges) {
					createdDangling = createDanglingPrefixTreeNodesIfNeeded(tempProcessingNode, null, 0);
				}

			} catch (Exception exc) {
				System.out.println(tempProcessingNode.getData().getMappedGraphString());
				throw exc;
			}

			hasSameNeighborsWithLessMatch(tempProcessingNode);
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

			if (createdDangling) {
				if (DummyProperties.debugMode)
					System.out.println("createdDangling: " + tempProcessingNode.getData());
				continue;
			}

			if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
				continue;
			}

			if (prefixTreeProcessingLevel < tempProcessingNode.getLevel()) {

				// if (DummyProperties.debugMode) {
				// Visualizer.visualizeALevel(this, prefixTreeProcessingLevel,
				// cntVisualization++);
				// System.out.println();
				// }

				// new sibling will be created soon.
				// old ones should be cleared
				// sameLevelPrefixTreeNodes.clear();
				// going to the next level
				prefixTreeProcessingLevel = tempProcessingNode.getLevel();
				if (DummyProperties.debugMode)
					System.out.println("prefixTreeProcessingLevel G0: " + prefixTreeProcessingLevel);
				// if (prefixTreeProcessingLevel == 5) {
				// // System.out.println();
				// }

			}

			// if (tempProcessingNode != null &&
			// tempProcessingNode.getData().isValid()) {
			if (DummyProperties.debugMode) {
				System.out.println("generator processing node:");
				System.out.println(tempProcessingNode.getData().getMappedGraphString());
			}

			// if (tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
			// == 1) {
			// System.out.println();
			// }

			processQueueNode(dataGraph, traversalQueue, prefixTreeProcessingLevel + 1, tempProcessingNode, null,
					PrefixTreeMode.BATCH, 0);
			// }
		}

		// if (DummyProperties.debugMode) {
		// Visualizer.visualizeALevel(this, prefixTreeProcessingLevel,
		// cntVisualization++);
		// Visualizer.visualizeALevel(this, prefixTreeProcessingLevel + 1,
		// cntVisualization++);
		//
		// }

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
							if (node.getProperty(pairString.key).toString().toLowerCase()
									.equals(pairString.value.toLowerCase())
									|| DummyFunctions.isContain(
											node.getProperty(pairString.key).toString().toLowerCase(),
											pairString.value.toLowerCase())) {
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

	private void addNewChildrenOrMatches(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
			PatternNode destPatternNode, Integer srcDataGpNodeId, Integer newNodeId,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrc,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer destStepsFromRoot, String relationshipType,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc,
			int snapshot, Direction direction, int prefixTreeProcessingLevel,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc) throws Exception {
		// for all other same-level
		// children
		// of the prefix-tree:
		boolean itWasBisimulated = false;
		if (tempProcessingNodeData.getPrefixTreeMode() == PrefixTreeMode.BATCH) {
			if (this.labelAdjacencyIndexer.prefixTreeNodesOfALevel.get(prefixTreeProcessingLevel) != null) {
				for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeSibling : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
						.get(prefixTreeProcessingLevel)) {
					// if
					// (prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex()
					// == 38) {
					// System.out.println();
					// }

					if (DualSimulationHandler.isBiDualSimulated(newPatternGraph, prefixTreeSibling, this)) {

						// if yes:
						// if (check sgi ==
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
							System.out.println("BATCH: simultanous siblings: add node link from "
									+ tempProcessingNode.getData().getPatternPrefixTreeNodeIndex() + " to "
									+ prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex());
						}
						addIncNodeLink(tempProcessingNode, prefixTreeSibling, this);

						// }

						break;
						// so, this child
						// doesn't
						// need any
						// from here.
					}
				}
			}
			if (!itWasBisimulated && this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
					.containsKey(tempProcessingNode.getLevel() + 1)) {

				for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
						.get(tempProcessingNode.getLevel() + 1)) {

					if (DualSimulationHandler.isBiDualSimulated(newPatternGraph, prefixTreeNode, this)) {

						if (DummyProperties.debugMode) {
							System.out.println("BATCH: sameLevels: add node link from "
									+ tempProcessingNode.getData().getPatternPrefixTreeNodeIndex() + " to "
									+ prefixTreeNode.getData().getPatternPrefixTreeNodeIndex());
						}

						itWasBisimulated = true;
						addIncNodeLink(tempProcessingNode, prefixTreeNode, this);

						// }

						break;
					}
				}
			}
		} else {
			// TODO:may be we can handle
			// it
			// without SGI
			for (PrefixTreeNode<IPrefixTreeNodeData> child : tempProcessingNode.getChildren()) {

				HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;

				if (DualSimulationHandler.preBiSimChecking(child.getData().getPatternGraph(), newPatternGraph, this)
						&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(newPatternGraph, child,
								this)) != null) {

					if (dualBiSimMap == null || dualBiSimMap.size() == 0) {
						if (Dummy.DummyProperties.debugMode) {
							System.err.println("no dest pattern is found after a successful SGI for children nodes!!");
						}
						continue;
					}

					itWasBisimulated = true;

					PatternNode tempSrcPatternNode = null;
					tempSrcPatternNode = dualBiSimMap.get(srcPatternNode).iterator().next();

					for (PatternNode destPtn : dualBiSimMap.get(destPatternNode)) {
						// if
						// (child.getData().getPatternPrefixTreeNodeIndex()
						// == 2
						// && srcDataGpNodeId == 1 &&
						// newNodeId == 3) {
						// System.out.println();
						// }
						child.getData().addNewMatchForUpdate(tempProcessingNode, tempSrcPatternNode, srcDataGpNodeId,
								destPtn, newNodeId, this.labelAdjacencyIndexer);
					}

					if (DummyProperties.debugMode) {
						System.out.println(child.getData().getMappedGraphString());
						System.out.println("new match for a child node!");
					}
					traversalQueue.add(child);
					newCreatedOrTouchedPTNodes.add(child.getData().getPatternPrefixTreeNodeIndex());
					break;
				}
			}

			if (!itWasBisimulated && tempProcessingNode.getLinkedNodes() != null) {

				for (PrefixTreeNode<IPrefixTreeNodeData> child : tempProcessingNode.getLinkedNodes()) {

					HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;

					if (DualSimulationHandler.preBiSimChecking(child.getData().getPatternGraph(), newPatternGraph, this)
							&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(newPatternGraph, child,
									this)) != null) {

						if (dualBiSimMap == null || dualBiSimMap.size() == 0) {
							if (Dummy.DummyProperties.debugMode) {
								System.out.println("no dest pattern is found after a successful SGI for linked nodes!");
							}
							continue;
						}

						itWasBisimulated = true;

						PatternNode tempSrcPatternNode = null;
						tempSrcPatternNode = dualBiSimMap.get(srcPatternNode).iterator().next();
						// if
						// (child.getData().getPatternPrefixTreeNodeIndex()
						// == 2
						// && srcDataGpNodeId == 1 &&
						// newNodeId == 3) {
						// System.out.println();
						// }
						for (PatternNode destPtn : dualBiSimMap.get(destPatternNode)) {
							child.getData().addNewMatchForUpdate(tempProcessingNode, tempSrcPatternNode,
									srcDataGpNodeId, destPtn, newNodeId, this.labelAdjacencyIndexer);
						}

						if (DummyProperties.debugMode)
							System.out.println("new match for a linked  node using SGI!");

						traversalQueue.add(child);
						newCreatedOrTouchedPTNodes.add(child.getData().getPatternPrefixTreeNodeIndex());
						break;
					}
				}

			}

			if (!itWasBisimulated && this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
					.containsKey(tempProcessingNode.getLevel() + 1)) {
				// if
				// (numberOfPatternsInPrefixTree
				// == 11) {
				// System.out.println();
				// }

				for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNodeByLevel : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
						.get(tempProcessingNode.getLevel() + 1)) {

					if (tempProcessingNode.getChildrenLinksSet().contains(prefixTreeNodeByLevel))
						// it's
						// checked
						// before
						continue;

					HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;
					if (DualSimulationHandler.preBiSimChecking(prefixTreeNodeByLevel.getData().getPatternGraph(),
							newPatternGraph, this)
							&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(newPatternGraph,
									prefixTreeNodeByLevel, this)) != null) {
						itWasBisimulated = true;

						PatternNode tempSrcPatternNode = null;
						tempSrcPatternNode = dualBiSimMap.get(srcPatternNode).iterator().next();
						for (PatternNode destPtn : dualBiSimMap.get(destPatternNode)) {
							prefixTreeNodeByLevel.getData().addNewMatchForUpdate(tempProcessingNode, tempSrcPatternNode,
									srcDataGpNodeId, destPtn, newNodeId, this.labelAdjacencyIndexer);
						}

						addIncNodeLink(tempProcessingNode, prefixTreeNodeByLevel, this);

						if (DummyProperties.debugMode)
							System.out.println("new match for a SGI node in INC mode!");

						traversalQueue.add(prefixTreeNodeByLevel);
						newCreatedOrTouchedPTNodes.add(prefixTreeNodeByLevel.getData().getPatternPrefixTreeNodeIndex());

						break;
					}

					// }
				}

			}
		}

		if (!itWasBisimulated) {
			HashSet<Integer> newNodeIds = new HashSet<Integer>();
			newNodeIds.add(newNodeId);
			PrefixTreeNode<IPrefixTreeNodeData> newChild = createNewPrefixTreeNode(tempProcessingNode, newPatternGraph,
					srcPatternNode, destPatternNode, newNodeIds, newCreatedOrTouchedPTNodes, relationshipType,
					destStepsFromRoot, snapshot, newChildrenOfTheSrc, false, direction);

			if (!goBackToPrev)
				newlySeenPatternNodeForThisSrc.put(
						destPatternNode.getType() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType,
						newChild);
			else
				seenPatternNodeFromPreviousNodesForThisSrc.put(destPatternNode, newChild);
		}
	}

	private void outgoingExpansion(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcOutgoing,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, String otherNodeLabelRelType,
			int snapshot, int prefixTreeProcessingLevel,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc) throws Exception {

		HashSet<Integer> sameLabelNeighborNodes = labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
				.get(otherNodeLabelRelType).get(Indexer.AFTER);

		if (sameLabelNeighborNodes == null || sameLabelNeighborNodes.size() == 0) {
			return;
		}

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

			int separatorIndex = otherNodeLabelRelType.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
			String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
			String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);

			// FOR-DEBUG START:
			// if
			// (tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode)
			// == null) {
			// System.out.println();
			// }
			// FOR-DEBUG END:

			Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
			if (DummyProperties.debugMode) {
				System.out.println("outgoingExpansion => srcNodeId:" + srcDataGpNodeId + ", newNodeId:" + newNodeId);
				// if (tempProcessingNodeData.getPatternPrefixTreeNodeIndex() ==
				// 0 && srcDataGpNodeId == 5
				// && newNodeId == 6) {
				// System.out.println();
				// }
			}

			// TODO: may be make some problem for us
			// our previousExpansion is not very
			// stable

			// if a src node Id wants to be
			// expanded,
			// it shouldn't have a sibling
			// containing that node Id
			// and expanded before.
			// boolean expandedBefore = false;
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

			// for (DefaultLabeledEdge e :
			// tempProcessingNodeData.getPatternGraph()
			// .outgoingEdgesOf(srcPatternNode)) {
			// //
			// // // if from this src I went to the
			// // // target with the newNodeId
			// // // and out degree of that is more
			// // // than 0 which means that it
			// // // verified before.
			// // // or it has just that node we
			// // // shouldn't expand to that node
			// // // again
			// if
			// (tempProcessingNode.getData().getMatchedNodes()
			// .getDataGraphCandidateNodeOfAbsPNode()
			// .get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e))
			// .contains(newNodeId)
			// && (/*
			// * tempProcessingNodeData
			// * .getPatternGraph()
			// * .outDegreeOf(
			// * tempProcessingNodeData
			// * .getPatternGraph()
			// * .getEdgeTarget(e)) > 0 ||
			// */
			// tempProcessingNode.getData().getMatchedNodes().getDataGraphCandidateNodeOfAbsPNode().get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e)).size()
			// == 1)) {
			// expandedBefore = true;
			// break;
			// }
			//
			// }
			//
			// if (expandedBefore) {
			// if (DummyProperties.debugMode)
			// System.out.println(
			// "expanded before from " + srcDataGpNodeId + "
			// to " + newNodeId);
			// continue;
			// }

			// if (srcDataGpNodeId == 0 && newNodeId
			// == 3
			// &&
			// tempProcessingNodeData.getPatternPrefixTreeNodeIndex()
			// == 5) {
			// System.out.println();
			// }

			// goBackToPrev = false;
			ArrayList<PatternNode> destPatternNodes = new ArrayList<PatternNode>();
			getDestPatternNodeAndCheckForGoBackToPrev(destPatternNodes, tempProcessingNodeData, srcPatternNode,
					srcDataGpNodeId, newNodeId, destLabel, relationshipType, destStepsFromRoot);

			if (tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().containsKey(srcPatternNode)
					&& tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().get(srcPatternNode)
							.containsKey(otherNodeLabelRelType)) {
				if (!goBackToPrev && tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().get(srcPatternNode)
						.get(otherNodeLabelRelType) > 0)
					continue;
				else if (goBackToPrev && tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType()
						.get(srcPatternNode).get(otherNodeLabelRelType) >= sameLabelNeighborNodes.size()) {
					continue;
				}
			}

			for (PatternNode destPatternNode : destPatternNodes) {

				int destInDegree = 1;
				int incomingFromSameType = 1;

				if (goBackToPrev) {
					// b1 or b3 a->b->d->b and a->b
					destStepsFromRoot = Math.min(destStepsFromRoot,
							tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(destPatternNode));

					destInDegree += tempProcessingNodeData.getPatternGraph().inDegreeOf(destPatternNode);

					for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
							.incomingEdgesOf(destPatternNode)) {
						if (tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
								.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
							incomingFromSameType++;
						}
					}

				}

				if (destInDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree) {
					if (DummyProperties.debugMode) {
						System.out.println("cont: destInDegree:" + destInDegree + " > in degree in data graph:"
								+ labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree);
					}
					continue;
				}

				// if
				// (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
				// .get(srcPatternNode.getLabel() +
				// DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
				// + relationshipType) == null) {
				// System.out.println("");
				// }
				if (incomingFromSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
						.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
						.get(Indexer.BEFORE).size()) {
					if (DummyProperties.debugMode) {
						System.out.println("cont. incomingFromSameType: " + incomingFromSameType
								+ " prev index type in data graph:"
								+ labelAdjacencyIndexer.distinctNodesOfDHopsAway
										.get(newNodeId).get(srcPatternNode.getLabel()
												+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
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
					if (seenPatternNodeFromPreviousNodesForThisSrcOutgoing.containsKey(destPatternNode)) {
						seenPrefixTreeNode = seenPatternNodeFromPreviousNodesForThisSrcOutgoing.get(destPatternNode);
					}
				} else {
					if (newlySeenPatternNodeForThisSrcOutgoing.containsKey(
							newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
						// if this expansion has
						// seen
						// before
						// add it to the group of
						// that
						// prefix-tree node
						seenPrefixTreeNode = newlySeenPatternNodeForThisSrcOutgoing
								.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
					}
				}

				if (seenPrefixTreeNode != null) {
					// double start =
					// System.nanoTime();

					IPrefixTreeNodeData prefixTreeNodeData = seenPrefixTreeNode.getData();

					PatternNode tempDestPatternNode = seenPrefixTreeNode.getData().getTargetPatternNode();

					prefixTreeNodeData.addNewMatch(tempDestPatternNode, newNodeId, this.labelAdjacencyIndexer);

					if (DummyProperties.debugMode) {
						System.out.println("prev pattern seen:");
						System.out.println(prefixTreeNodeData.getMappedGraphString());
					}

					seenPatternNodeFromPreviousNodesForThisSrcOutgoing.put(tempDestPatternNode, seenPrefixTreeNode);

					if (newCreatedOrTouchedPTNodes != null)
						newCreatedOrTouchedPTNodes.add(seenPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex());
				} else {

					// make a new pattern for SGI
					// checking
					// and add it as
					// a new child if possible
					ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
							DefaultLabeledEdge.class);

					for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
						newPatternGraph.addVertex(patternNode);
					}

					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
						newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
					}

					if (!newPatternGraph.vertexSet().contains(destPatternNode)) {
						newPatternGraph.addVertex(destPatternNode);
					}

					newPatternGraph.addEdge(srcPatternNode, destPatternNode, new DefaultLabeledEdge(relationshipType));

					// if
					// (srcPatternNode.getLabel().equals("b")
					// &&
					// destPatternNode.getLabel().equals("d"))
					// {
					// System.out.println();
					// }
					// HashSet<Integer>
					// prevNodesOfSrcType =
					// labelAdjacencyIndexer.distinctNodesOfDHopsAway
					// .get(newNodeId)
					// .get(srcPatternNode.getLabel()
					// +
					// Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
					// + relationshipType)
					// .get(Indexer.BEFORE);
					//
					// int sameTypeIncomingEdgeCnt = 0;
					// for (DefaultLabeledEdge e :
					// newPatternGraph
					// .incomingEdgesOf(destPatternNode))
					// {
					// if
					// (newPatternGraph.getEdgeSource(e).getType()
					// .equals(srcPatternNode.getType())
					// &&
					// e.getType().equals(relationshipType))
					// {
					// sameTypeIncomingEdgeCnt++;
					// }
					// }
					// if (sameTypeIncomingEdgeCnt >
					// prevNodesOfSrcType.size()) {
					// if (DummyProperties.debugMode)
					// System.out.println(
					// "newPatternGraph.inDegreeOf(destPatternNode)
					// > prevNodesOfSrcType.size()");
					// continue;
					// }

					addNewChildrenOrMatches(tempProcessingNode, tempProcessingNodeData, newPatternGraph, srcPatternNode,
							destPatternNode, srcDataGpNodeId, newNodeId, newlySeenPatternNodeForThisSrcOutgoing,
							newCreatedOrTouchedPTNodes, destStepsFromRoot, relationshipType,
							seenPatternNodeFromPreviousNodesForThisSrcOutgoing, snapshot,
							Dummy.DummyProperties.Direction.OUTGOING, prefixTreeProcessingLevel, newChildrenOfTheSrc);

				}
			}
		}
	}

	private void outgoingExpansion2(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcOutgoing,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, String otherNodeLabelRelType,
			int snapshot, int prefixTreeProcessingLevel,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc) throws Exception {

		HashSet<Integer> sameLabelNeighborNodes = new HashSet<Integer>();

		if (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId).get(otherNodeLabelRelType)
				.containsKey(Indexer.AFTER)) {
			sameLabelNeighborNodes.addAll(labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
					.get(otherNodeLabelRelType).get(Indexer.AFTER));
		}

		if (sameLabelNeighborNodes == null || sameLabelNeighborNodes.size() == 0) {
			return;
		}

		// for (Integer newNodeId : sameLabelNeighborNodes) {

		int separatorIndex = otherNodeLabelRelType.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
		String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
		String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);
		Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
		if (DummyProperties.debugMode) {
			// System.out.println("outgoingExpansion => srcNodeId:" +
			// srcDataGpNodeId + ", newNodeId:" + newNodeId);
			// if (tempProcessingNodeData.getPatternPrefixTreeNodeIndex() == 8
			// && srcDataGpNodeId == 0) {
			// System.out.println();
			// }
		}

		// TODO: may be make some problem for us
		// our previousExpansion is not very
		// stable

		// goBackToPrev = false;
		// ArrayList<PatternNode> destPatternNodes = new
		// ArrayList<PatternNode>();
		ArrayList<GoBackToPrevHolder> destPatternNodes = new ArrayList<GoBackToPrevHolder>();

		getDestPatternNodeAndCheckForGoBackToPrev2(destPatternNodes, tempProcessingNodeData, srcPatternNode,
				srcDataGpNodeId, sameLabelNeighborNodes, destLabel, relationshipType, destStepsFromRoot);

		if (tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().containsKey(srcPatternNode)
				&& tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().get(srcPatternNode)
						.containsKey(otherNodeLabelRelType)) {
			if (!goBackToPrev && tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().get(srcPatternNode)
					.get(otherNodeLabelRelType) > 0)
				return;
			else if (goBackToPrev && tempProcessingNodeData.getFrequencyOfNextNeighborOfSameType().get(srcPatternNode)
					.get(otherNodeLabelRelType) >= sameLabelNeighborNodes.size()) {
				return;
			}
		}

		for (GoBackToPrevHolder destPatternNode : destPatternNodes) {
			destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
			int destInDegree = 1;
			int incomingFromSameType = 1;

			if (destPatternNode.goBackToPrev) {
				// b1 or b3 a->b->d->b and a->b
				destStepsFromRoot = Math.min(destStepsFromRoot,
						tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(destPatternNode.destPatternNode));

				destInDegree += tempProcessingNodeData.getPatternGraph().inDegreeOf(destPatternNode.destPatternNode);

				for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
						.incomingEdgesOf(destPatternNode.destPatternNode)) {
					if (tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
							.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
						incomingFromSameType++;
					}
				}
			}

			HashSet<Integer> newNodeIdsMustBeRemoved = new HashSet<Integer>();
			for (Integer newNodeId : destPatternNode.newNodeIds) {
				if (destInDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree) {
					if (DummyProperties.debugMode) {
						System.out.println("cont: destInDegree:" + destInDegree + " > in degree in data graph:"
								+ labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree);
					}
					newNodeIdsMustBeRemoved.add(newNodeId);
				}
			}
			destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
			newNodeIdsMustBeRemoved.clear();

			for (Integer newNodeId : destPatternNode.newNodeIds) {
				if (incomingFromSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
						.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
						.get(Indexer.BEFORE).size()) {
					if (DummyProperties.debugMode) {
						System.out.println("cont. incomingFromSameType: " + incomingFromSameType
								+ " prev index type in data graph:"
								+ labelAdjacencyIndexer.distinctNodesOfDHopsAway
										.get(newNodeId).get(srcPatternNode.getLabel()
												+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
										.get(Indexer.BEFORE).size());
					}
					newNodeIdsMustBeRemoved.add(newNodeId);
				}
			}
			destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);

			if (destPatternNode.newNodeIds.size() == 0)
				continue;

			// finding the new node type;
			String newNodeType = null;

			// if we've seen it in this pattern
			// before...

			newNodeType = destPatternNode.destPatternNode.getType();

			PrefixTreeNode<IPrefixTreeNodeData> seenPrefixTreeNode = null;
			if (destPatternNode.goBackToPrev) {
				if (seenPatternNodeFromPreviousNodesForThisSrcOutgoing.containsKey(destPatternNode)) {
					seenPrefixTreeNode = seenPatternNodeFromPreviousNodesForThisSrcOutgoing.get(destPatternNode);
				}
			} else {
				if (newlySeenPatternNodeForThisSrcOutgoing
						.containsKey(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
					// if this expansion has
					// seen
					// before
					// add it to the group of
					// that
					// prefix-tree node
					seenPrefixTreeNode = newlySeenPatternNodeForThisSrcOutgoing
							.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
				}
			}

			if (seenPrefixTreeNode != null) {
				// double start =
				// System.nanoTime();

				IPrefixTreeNodeData prefixTreeNodeData = seenPrefixTreeNode.getData();

				PatternNode tempDestPatternNode = seenPrefixTreeNode.getData().getTargetPatternNode();

				prefixTreeNodeData.addNewMatchSet(tempDestPatternNode, destPatternNode.newNodeIds,
						this.labelAdjacencyIndexer);

				if (DummyProperties.debugMode) {
					System.out.println("prev pattern seen:");
					System.out.println(prefixTreeNodeData.getMappedGraphString());
				}

				seenPatternNodeFromPreviousNodesForThisSrcOutgoing.put(tempDestPatternNode, seenPrefixTreeNode);

				if (newCreatedOrTouchedPTNodes != null)
					newCreatedOrTouchedPTNodes.add(seenPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex());
			} else {

				// make a new pattern for SGI
				// checking
				// and add it as
				// a new child if possible
				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
						DefaultLabeledEdge.class);

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					newPatternGraph.addVertex(patternNode);
				}

				for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
					newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
				}

				if (!newPatternGraph.vertexSet().contains(destPatternNode.destPatternNode)) {
					newPatternGraph.addVertex(destPatternNode.destPatternNode);
				}

				newPatternGraph.addEdge(srcPatternNode, destPatternNode.destPatternNode,
						new DefaultLabeledEdge(relationshipType));

				addNewChildrenOrMatches2(tempProcessingNode, tempProcessingNodeData, newPatternGraph, srcPatternNode,
						destPatternNode, srcDataGpNodeId, destPatternNode.newNodeIds,
						newlySeenPatternNodeForThisSrcOutgoing, newCreatedOrTouchedPTNodes, destStepsFromRoot,
						relationshipType, seenPatternNodeFromPreviousNodesForThisSrcOutgoing, snapshot,
						Dummy.DummyProperties.Direction.OUTGOING, prefixTreeProcessingLevel, newChildrenOfTheSrc);

			}
		}
		// } for all same nodes
	}

	private void addNewChildrenOrMatches2(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
			GoBackToPrevHolder destPatternNode, Integer srcDataGpNodeId, HashSet<Integer> newNodeIds,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrc,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer destStepsFromRoot, String relationshipType,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc,
			int snapshot, Direction direction, int prefixTreeProcessingLevel,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc) throws Exception {

		// for all other same-level
		// children
		// of the prefix-tree:
		boolean itWasBisimulated = false;
		if (tempProcessingNodeData.getPrefixTreeMode() == PrefixTreeMode.BATCH) {
			if (this.labelAdjacencyIndexer.prefixTreeNodesOfALevel.get(prefixTreeProcessingLevel) != null) {
				for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeSibling : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
						.get(prefixTreeProcessingLevel)) {
					// if
					// (prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex()
					// == 38) {
					// System.out.println();
					// }

					if (DualSimulationHandler.isBiDualSimulated(newPatternGraph, prefixTreeSibling, this)) {

						// if yes:
						// if (check sgi ==
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
							System.out.println("BATCH: simultanous siblings: add node link from "
									+ tempProcessingNode.getData().getPatternPrefixTreeNodeIndex() + " to "
									+ prefixTreeSibling.getData().getPatternPrefixTreeNodeIndex());
						}
						addIncNodeLink(tempProcessingNode, prefixTreeSibling, this);

						// }

						break;
						// so, this child
						// doesn't
						// need any
						// from here.
					}
				}
			}
			if (!itWasBisimulated && this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
					.containsKey(tempProcessingNode.getLevel() + 1)) {

				for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
						.get(tempProcessingNode.getLevel() + 1)) {

					if (DualSimulationHandler.isBiDualSimulated(newPatternGraph, prefixTreeNode, this)) {

						if (DummyProperties.debugMode) {
							System.out.println("BATCH: sameLevels: add node link from "
									+ tempProcessingNode.getData().getPatternPrefixTreeNodeIndex() + " to "
									+ prefixTreeNode.getData().getPatternPrefixTreeNodeIndex());
						}

						itWasBisimulated = true;
						addIncNodeLink(tempProcessingNode, prefixTreeNode, this);

						// }

						break;
					}
				}
			}
		} else {
			// TODO:may be we can handle
			// it
			// without SGI
			for (PrefixTreeNode<IPrefixTreeNodeData> child : tempProcessingNode.getChildren()) {

				HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;

				if (DualSimulationHandler.preBiSimChecking(child.getData().getPatternGraph(), newPatternGraph, this)
						&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(newPatternGraph, child,
								this)) != null) {

					if (dualBiSimMap == null || dualBiSimMap.size() == 0) {
						if (Dummy.DummyProperties.debugMode) {
							System.err.println("no dest pattern is found after a successful SGI for children nodes!!");
						}
						continue;
					}

					itWasBisimulated = true;

					PatternNode tempSrcPatternNode = null;
					tempSrcPatternNode = dualBiSimMap.get(srcPatternNode).iterator().next();

					for (PatternNode destPtn : dualBiSimMap.get(destPatternNode.destPatternNode)) {
						// if
						// (child.getData().getPatternPrefixTreeNodeIndex()
						// == 2
						// && srcDataGpNodeId == 1 &&
						// newNodeId == 3) {
						// System.out.println();
						// }
						child.getData().addNewMatchSetForUpdate(tempProcessingNode, tempSrcPatternNode, srcDataGpNodeId,
								destPtn, newNodeIds, this.labelAdjacencyIndexer);
					}

					if (DummyProperties.debugMode) {
						System.out.println(child.getData().getMappedGraphString());
						System.out.println("new match for a child node!");
					}
					traversalQueue.add(child);
					newCreatedOrTouchedPTNodes.add(child.getData().getPatternPrefixTreeNodeIndex());
					break;
				}
			}

			if (!itWasBisimulated && tempProcessingNode.getLinkedNodes() != null) {

				for (PrefixTreeNode<IPrefixTreeNodeData> child : tempProcessingNode.getLinkedNodes()) {

					HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;

					if (DualSimulationHandler.preBiSimChecking(child.getData().getPatternGraph(), newPatternGraph, this)
							&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(newPatternGraph, child,
									this)) != null) {

						if (dualBiSimMap == null || dualBiSimMap.size() == 0) {
							if (Dummy.DummyProperties.debugMode) {
								System.out.println("no dest pattern is found after a successful SGI for linked nodes!");
							}
							continue;
						}

						itWasBisimulated = true;

						PatternNode tempSrcPatternNode = null;
						tempSrcPatternNode = dualBiSimMap.get(srcPatternNode).iterator().next();
						// if
						// (child.getData().getPatternPrefixTreeNodeIndex()
						// == 2
						// && srcDataGpNodeId == 1 &&
						// newNodeId == 3) {
						// System.out.println();
						// }
						for (PatternNode destPtn : dualBiSimMap.get(destPatternNode.destPatternNode)) {
							child.getData().addNewMatchSetForUpdate(tempProcessingNode, tempSrcPatternNode,
									srcDataGpNodeId, destPtn, newNodeIds, this.labelAdjacencyIndexer);
						}

						if (DummyProperties.debugMode)
							System.out.println("new match for a linked  node using SGI!");

						traversalQueue.add(child);
						newCreatedOrTouchedPTNodes.add(child.getData().getPatternPrefixTreeNodeIndex());
						break;
					}
				}

			}

			if (!itWasBisimulated && this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
					.containsKey(tempProcessingNode.getLevel() + 1)) {
				// if
				// (numberOfPatternsInPrefixTree
				// == 11) {
				// System.out.println();
				// }

				for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNodeByLevel : this.labelAdjacencyIndexer.prefixTreeNodesOfALevel
						.get(tempProcessingNode.getLevel() + 1)) {

					if (tempProcessingNode.getChildrenLinksSet().contains(prefixTreeNodeByLevel))
						// it's
						// checked
						// before
						continue;

					HashMap<PatternNode, HashSet<PatternNode>> dualBiSimMap;
					if (DualSimulationHandler.preBiSimChecking(prefixTreeNodeByLevel.getData().getPatternGraph(),
							newPatternGraph, this)
							&& (dualBiSimMap = DualSimulationHandler.getBiSimMapIfAny(newPatternGraph,
									prefixTreeNodeByLevel, this)) != null) {
						itWasBisimulated = true;

						PatternNode tempSrcPatternNode = null;
						tempSrcPatternNode = dualBiSimMap.get(srcPatternNode).iterator().next();
						for (PatternNode destPtn : dualBiSimMap.get(destPatternNode.destPatternNode)) {
							prefixTreeNodeByLevel.getData().addNewMatchSetForUpdate(tempProcessingNode,
									tempSrcPatternNode, srcDataGpNodeId, destPtn, newNodeIds,
									this.labelAdjacencyIndexer);
						}

						addIncNodeLink(tempProcessingNode, prefixTreeNodeByLevel, this);

						if (DummyProperties.debugMode)
							System.out.println("new match for a SGI node in INC mode!");

						traversalQueue.add(prefixTreeNodeByLevel);
						newCreatedOrTouchedPTNodes.add(prefixTreeNodeByLevel.getData().getPatternPrefixTreeNodeIndex());

						break;
					}

					// }
				}

			}
		}

		if (!itWasBisimulated) {
			// HashSet<Integer> newNodeIds = new HashSet<Integer>();
			// newNodeIds.add(newNodeId);
			PrefixTreeNode<IPrefixTreeNodeData> newChild = createNewPrefixTreeNode(tempProcessingNode, newPatternGraph,
					srcPatternNode, destPatternNode.destPatternNode, newNodeIds, newCreatedOrTouchedPTNodes,
					relationshipType, destStepsFromRoot, snapshot, newChildrenOfTheSrc, false, direction);

			if (!destPatternNode.goBackToPrev)
				newlySeenPatternNodeForThisSrc.put(destPatternNode.destPatternNode.getType()
						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType, newChild);
			else
				seenPatternNodeFromPreviousNodesForThisSrc.put(destPatternNode.destPatternNode, newChild);
		}

	}

	private boolean thisTypeExistedBeforeInThePattern(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			String destLabel) {
		for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
			if (patternNode.getLabel().equals(destLabel)) {
				return true;
			}
		}
		return false;

	}

	private void processQueueNode(GraphDatabaseService dataGraph,
			Queue<PrefixTreeNode<IPrefixTreeNodeData>> traversalQueue, int prefixTreeProcessingLevel,
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
			PrefixTreeMode prefixTreeMode, int snapshot) throws Exception {

		// while we are inside of this method we expand the same pattern to
		// generate all the possible children

		// get the pattern
		// for all nodes in the pattern
		IPrefixTreeNodeData tempProcessingNodeData = tempProcessingNode.getData();

		HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc = new HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>>();
		for (PatternNode srcPatternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			newChildrenOfTheSrc.put(srcPatternNode, new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>());

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
			if (tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) >= maxAllowedHops) {
				if (DummyProperties.debugMode)
					System.out.println("maxAllowedHops for srcAbstractPatternNode:" + srcPatternNode.getType() + ""
							+ srcPatternNode.isFocus());
				continue;
			}

			// if (tempProcessingNodeData.getPatternPrefixTreeNodeIndex() == 1)
			// {
			// System.out.println();
			// }
			if (DummyProperties.debugMode) {
				System.out.println("srcAbstractPatternNode:" + srcPatternNode.getType() + "" + srcPatternNode.isFocus()
						+ " hashCode:" + srcPatternNode.hashCode() + " stepsFromRoot: "
						+ tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode));
			}

			// String: the destination because source are same
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcOutgoing = new HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>>();
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing = new HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>>();

			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcIncoming = new HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>>();
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming = new HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>>();

			// int matchGraphIndex = -1;
			// for all match nodes in this prefix-tree node and for this src
			// pattern node
			
			Bitmap2.nodeIdsOfPatternNodeIndex
			Indexer.
			tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
			.get(srcPatternNode);
			for (Integer srcDataGpNodeId : tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.get(srcPatternNode)) {

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
				// boolean expandedSrcBefore = false;
				// for (DefaultLabeledEdge e1 :
				// tempProcessingNodeData.getPatternGraph()
				// .incomingEdgesOf(srcPatternNode)) {
				// PatternNode parentOfSrcPattern =
				// tempProcessingNodeData.getPatternGraph().getEdgeSource(e1);
				// for (DefaultLabeledEdge e2 :
				// tempProcessingNodeData.getPatternGraph()
				// .outgoingEdgesOf(parentOfSrcPattern)) {
				// if (e2.getType().equals(e1.getType())
				// && tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2)
				// != srcPatternNode
				// &&
				// tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2).getType()
				// .equals(srcPatternNode.getType())) {
				//
				// if
				// (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				// .get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2))
				// .contains(srcDataGpNodeId)
				// &&
				// (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				// .size() == 1
				// /*
				// * || tempProcessingNodeData.
				// * getPatternGraph(). outDegreeOf(
				// * tempProcessingNodeData.
				// * getPatternGraph(). getEdgeTarget(e2))
				// * > 0
				// */)
				//
				// ) {
				// expandedSrcBefore = true;
				// break;
				// }
				//
				// }
				//
				// }
				// if (expandedSrcBefore) {
				// break;
				// }
				//
				// }
				// if (expandedSrcBefore)
				// continue;

				// matchGraphIndex++;

				// System.out.println("srcNodeId:" +
				// srcDataGpNode.patternGNodeId);

				// if ((labelAdjacencyIndexer.dataGraphNodeInfos
				// .get(srcDataGpNodeId).outDegree >
				// tempProcessingNodeData.getPatternGraph()
				// .outDegreeOf(srcPatternNode))) {

				if (!tempProcessingNode.getData().getFoundAllFocuses()) {

					// first expandToFocuses;
					expandToFocusesFirst(tempProcessingNode, tempProcessingNodeData,
							seenPatternNodeFromPreviousNodesForThisSrcOutgoing, newlySeenPatternNodeForThisSrcOutgoing,
							newCreatedOrTouchedPTNodes, srcDataGpNodeId, snapshot, newChildrenOfTheSrc,
							prefixTreeProcessingLevel, srcPatternNode,
							seenPatternNodeFromPreviousNodesForThisSrcIncoming, newlySeenPatternNodeForThisSrcIncoming);
				}

				else {
					// of all possible labels_reltype
					for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway
							.get(srcDataGpNodeId).keySet()) {

						if (Dummy.DummyProperties.debugMode)
							System.out.println("outgoing expansion");

						outgoingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
								seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
								newlySeenPatternNodeForThisSrcOutgoing, newCreatedOrTouchedPTNodes, srcDataGpNodeId,
								otherNodeLabelRelType, snapshot, prefixTreeProcessingLevel, newChildrenOfTheSrc);
					}
					for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway
							.get(srcDataGpNodeId).keySet()) {

						if (Dummy.DummyProperties.debugMode)
							System.out.println("incoming expansion");

						incomingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
								seenPatternNodeFromPreviousNodesForThisSrcIncoming,
								newlySeenPatternNodeForThisSrcIncoming, newCreatedOrTouchedPTNodes, srcDataGpNodeId,
								otherNodeLabelRelType, snapshot, prefixTreeProcessingLevel, newChildrenOfTheSrc);
					}
				}

			}
		}
		tempProcessingNodeData.renewNewUnexpandedNodesOfPatternNodes();
		tempProcessingNodeData.renewNewUnexpandedPatternsNodesOfNeo4jNodes();
	}

	private void expandToFocusesFirst(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcOutgoing,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, int snapshot,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc,
			int prefixTreeProcessingLevel, PatternNode srcPatternNode,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcIncoming)
			throws Exception {

		for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
				.keySet()) {
			for (String unSeenFocusType : tempProcessingNode.getData().getTypeOfUnSeenFocusNodes()) {
				if (!otherNodeLabelRelType
						.startsWith(unSeenFocusType + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE))
					continue;

				// System.out.println("outgoing expansion");
				outgoingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
						seenPatternNodeFromPreviousNodesForThisSrcOutgoing, newlySeenPatternNodeForThisSrcOutgoing,
						newCreatedOrTouchedPTNodes, srcDataGpNodeId, otherNodeLabelRelType, snapshot,
						prefixTreeProcessingLevel, newChildrenOfTheSrc);
			}
		}
		for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
				.keySet()) {

			for (String unSeenFocusType : tempProcessingNode.getData().getTypeOfUnSeenFocusNodes()) {
				if (!otherNodeLabelRelType
						.startsWith(unSeenFocusType + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE))
					continue;

				// System.out.println("incoming expansion");
				incomingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
						seenPatternNodeFromPreviousNodesForThisSrcIncoming, newlySeenPatternNodeForThisSrcIncoming,
						newCreatedOrTouchedPTNodes, srcDataGpNodeId, otherNodeLabelRelType, snapshot,
						prefixTreeProcessingLevel, newChildrenOfTheSrc);
			}
		}

	}

	private void incomingExpansion(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcIncoming,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, String otherNodeLabelRelType,
			int snapshot, int prefixTreeProcessingLevel,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc) throws Exception {

		HashSet<Integer> sameLabelNeighborNodes = labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
				.get(otherNodeLabelRelType).get(Indexer.BEFORE);

		if (sameLabelNeighborNodes == null || sameLabelNeighborNodes.size() == 0) {
			return;
		}

		for (Integer newNodeId : sameLabelNeighborNodes) {

			int separatorIndex = otherNodeLabelRelType.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
			String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
			String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);

			Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
			if (DummyProperties.debugMode) {
				System.out.println("incomingExpansion => srcNodeId:" + srcDataGpNodeId + ", newNodeId:" + newNodeId);
				// if (tempProcessingNodeData.getPatternPrefixTreeNodeIndex() ==
				// 1 && srcDataGpNodeId == 5
				// && newNodeId == 3) {
				// System.out.println();
				// }
			}

			// TODO: may be make some problem for us
			// our previousExpansion is not very
			// stable

			// goBackToPrev = false;
			ArrayList<PatternNode> destPatternNodes = new ArrayList<PatternNode>();
			getDestPatternNodeAndCheckForGoBackToPrevIncoming(destPatternNodes, tempProcessingNodeData, srcPatternNode,
					srcDataGpNodeId, newNodeId, destLabel, relationshipType, destStepsFromRoot);

			if (tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().containsKey(srcPatternNode)
					&& tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().get(srcPatternNode)
							.containsKey(otherNodeLabelRelType)) {
				if (!goBackToPrev && tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().get(srcPatternNode)
						.get(otherNodeLabelRelType) > 0)
					continue;
				else if (goBackToPrev && tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType()
						.get(srcPatternNode).get(otherNodeLabelRelType) >= sameLabelNeighborNodes.size()) {
					continue;
				}
			}

			for (PatternNode destPatternNode : destPatternNodes) {

				int destOutDegree = 1;
				int outgoingToSameType = 1;

				if (goBackToPrev) {
					// b1 or b3 a->b->d->b and a->b
					destStepsFromRoot = Math.min(destStepsFromRoot,
							tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(destPatternNode));

					destOutDegree += tempProcessingNodeData.getPatternGraph().outDegreeOf(destPatternNode);

					for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
							.outgoingEdgesOf(destPatternNode)) {
						if (tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
								.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
							outgoingToSameType++;
						}
					}

				}

				if (destOutDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree) {
					if (DummyProperties.debugMode) {
						System.out.println("cont: destOutDegree:" + destOutDegree + " > out degree in data graph:"
								+ labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree);
					}
					continue;
				}

				if (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
						.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
						.get(Indexer.BEFORE) != null
						&& (outgoingToSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway
								.get(newNodeId).get(srcPatternNode.getLabel()
										+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
								.get(Indexer.AFTER).size())) {
					if (DummyProperties.debugMode) {
						System.out.println("cont. outgoingToSameType: " + outgoingToSameType
								+ " prev index type in data graph:"
								+ labelAdjacencyIndexer.distinctNodesOfDHopsAway
										.get(newNodeId).get(srcPatternNode.getLabel()
												+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
										.get(Indexer.AFTER).size());
					}
					continue;
				}

				// finding the new node type;
				String newNodeType = null;

				// if we've seen it in this pattern
				// before...

				newNodeType = destPatternNode.getType();

				PrefixTreeNode<IPrefixTreeNodeData> seenPrefixTreeNode = null;
				if (goBackToPrev) {
					if (seenPatternNodeFromPreviousNodesForThisSrcIncoming.containsKey(destPatternNode)) {
						seenPrefixTreeNode = seenPatternNodeFromPreviousNodesForThisSrcIncoming.get(destPatternNode);
					}
				} else {
					if (newlySeenPatternNodeForThisSrcIncoming.containsKey(
							newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
						// if this expansion has
						// seen
						// before
						// add it to the group of
						// that
						// prefix-tree node
						seenPrefixTreeNode = newlySeenPatternNodeForThisSrcIncoming
								.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
					}
				}

				if (seenPrefixTreeNode != null) {
					// double start =
					// System.nanoTime();

					IPrefixTreeNodeData prefixTreeNodeData = seenPrefixTreeNode.getData();

					PatternNode tempDestPatternNode = seenPrefixTreeNode.getData().getTargetPatternNode();

					prefixTreeNodeData.addNewMatch(tempDestPatternNode, newNodeId, this.labelAdjacencyIndexer);

					if (DummyProperties.debugMode) {
						System.out.println("prev pattern seen:");
						System.out.println(prefixTreeNodeData.getMappedGraphString());
					}

					seenPatternNodeFromPreviousNodesForThisSrcIncoming.put(tempDestPatternNode, seenPrefixTreeNode);

					if (newCreatedOrTouchedPTNodes != null)
						newCreatedOrTouchedPTNodes.add(seenPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex());
				} else {

					// make a new pattern for SGI
					// checking
					// and add it as
					// a new child if possible
					ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
							DefaultLabeledEdge.class);

					for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
						newPatternGraph.addVertex(patternNode);
					}

					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
						newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
					}

					if (!newPatternGraph.vertexSet().contains(destPatternNode)) {
						newPatternGraph.addVertex(destPatternNode);
					}

					newPatternGraph.addEdge(destPatternNode, srcPatternNode, new DefaultLabeledEdge(relationshipType));

					addNewChildrenOrMatches(tempProcessingNode, tempProcessingNodeData, newPatternGraph, srcPatternNode,
							destPatternNode, srcDataGpNodeId, newNodeId, newlySeenPatternNodeForThisSrcIncoming,
							newCreatedOrTouchedPTNodes, destStepsFromRoot, relationshipType,
							seenPatternNodeFromPreviousNodesForThisSrcIncoming, snapshot,
							Dummy.DummyProperties.Direction.INCOMING, prefixTreeProcessingLevel, newChildrenOfTheSrc);

				}
			}
		}
	}

	private void incomingExpansion2(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode,
			HashMap<PatternNode, PrefixTreeNode<IPrefixTreeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming,
			HashMap<String, PrefixTreeNode<IPrefixTreeNodeData>> newlySeenPatternNodeForThisSrcIncoming,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, String otherNodeLabelRelType,
			int snapshot, int prefixTreeProcessingLevel,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc) throws Exception {

		HashSet<Integer> sameLabelNeighborNodes = new HashSet<Integer>();

		if (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId).get(otherNodeLabelRelType)
				.containsKey(Indexer.BEFORE)) {
			sameLabelNeighborNodes.addAll(labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
					.get(otherNodeLabelRelType).get(Indexer.BEFORE));
		}

		if (sameLabelNeighborNodes == null || sameLabelNeighborNodes.size() == 0) {
			return;
		}

		int separatorIndex = otherNodeLabelRelType.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
		String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
		String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);
		Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;

		// if (DummyProperties.debugMode) {
		// System.out.println("incomingExpansion => srcNodeId:" +
		// srcDataGpNodeId + ", newNodeId:" + newNodeId);
		// if (tempProcessingNodeData.getPatternPrefixTreeNodeIndex() == 1 &&
		// srcDataGpNodeId == 0) {
		// System.out.println();
		// }
		// }

		// TODO: may be make some problem for us
		// our previousExpansion is not very
		// stable

		// goBackToPrev = false;
		ArrayList<GoBackToPrevHolder> destPatternNodes = new ArrayList<GoBackToPrevHolder>();
		getDestPatternNodeAndCheckForGoBackToPrevIncoming2(destPatternNodes, tempProcessingNodeData, srcPatternNode,
				srcDataGpNodeId, sameLabelNeighborNodes, destLabel, relationshipType, destStepsFromRoot);

		if (tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().containsKey(srcPatternNode)
				&& tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().get(srcPatternNode)
						.containsKey(otherNodeLabelRelType)) {
			if (!goBackToPrev && tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().get(srcPatternNode)
					.get(otherNodeLabelRelType) > 0)
				return;
			else if (goBackToPrev && tempProcessingNodeData.getFrequencyOfPrevNeighborOfSameType().get(srcPatternNode)
					.get(otherNodeLabelRelType) >= sameLabelNeighborNodes.size()) {
				return;
			}
		}

		for (GoBackToPrevHolder destPatternNode : destPatternNodes) {
			destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
			int destOutDegree = 1;
			int outgoingToSameType = 1;

			if (goBackToPrev) {
				// b1 or b3 a->b->d->b and a->b
				destStepsFromRoot = Math.min(destStepsFromRoot,
						tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(destPatternNode.destPatternNode));

				destOutDegree += tempProcessingNodeData.getPatternGraph().outDegreeOf(destPatternNode.destPatternNode);

				for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
						.outgoingEdgesOf(destPatternNode.destPatternNode)) {
					if (tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
							.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
						outgoingToSameType++;
					}
				}

			}

			HashSet<Integer> newNodeIdsMustBeRemoved = new HashSet<Integer>();
			for (Integer newNodeId : destPatternNode.newNodeIds) {
				if (destOutDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree) {
					if (DummyProperties.debugMode) {
						System.out.println("cont: destOutDegree:" + destOutDegree + " > out degree in data graph:"
								+ labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree);
					}
					newNodeIdsMustBeRemoved.add(newNodeId);
				}
			}
			destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
			newNodeIdsMustBeRemoved.clear();

			for (Integer newNodeId : destPatternNode.newNodeIds) {
				// if
				// (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
				// .get(srcPatternNode.getLabel() +
				// DummyProperties.SEPARATOR_LABEL_AND_RELTYPE +
				// relationshipType)
				// .get(Indexer.AFTER) == null) {
				// System.out.println();
				// }

				if (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
						.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
						.get(Indexer.BEFORE) != null
						&& (outgoingToSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway
								.get(newNodeId).get(srcPatternNode.getLabel()
										+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
								.get(Indexer.AFTER).size())) {
					if (DummyProperties.debugMode) {
						System.out.println("cont. outgoingToSameType: " + outgoingToSameType
								+ " prev index type in data graph:"
								+ labelAdjacencyIndexer.distinctNodesOfDHopsAway
										.get(newNodeId).get(srcPatternNode.getLabel()
												+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
										.get(Indexer.AFTER).size());
					}
					newNodeIdsMustBeRemoved.add(newNodeId);
				}
			}
			destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);

			if (destPatternNode.newNodeIds.size() == 0)
				continue;

			// finding the new node type;
			String newNodeType = null;

			// if we've seen it in this pattern
			// before...

			newNodeType = destPatternNode.destPatternNode.getType();

			PrefixTreeNode<IPrefixTreeNodeData> seenPrefixTreeNode = null;
			if (goBackToPrev) {
				if (seenPatternNodeFromPreviousNodesForThisSrcIncoming.containsKey(destPatternNode)) {
					seenPrefixTreeNode = seenPatternNodeFromPreviousNodesForThisSrcIncoming.get(destPatternNode);
				}
			} else {
				if (newlySeenPatternNodeForThisSrcIncoming
						.containsKey(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
					// if this expansion has
					// seen
					// before
					// add it to the group of
					// that
					// prefix-tree node
					seenPrefixTreeNode = newlySeenPatternNodeForThisSrcIncoming
							.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
				}
			}

			if (seenPrefixTreeNode != null) {
				// double start =
				// System.nanoTime();

				IPrefixTreeNodeData prefixTreeNodeData = seenPrefixTreeNode.getData();

				PatternNode tempDestPatternNode = seenPrefixTreeNode.getData().getTargetPatternNode();

				prefixTreeNodeData.addNewMatchSet(tempDestPatternNode, destPatternNode.newNodeIds,
						this.labelAdjacencyIndexer);

				if (DummyProperties.debugMode) {
					System.out.println("prev pattern seen:");
					System.out.println(prefixTreeNodeData.getMappedGraphString());
				}

				seenPatternNodeFromPreviousNodesForThisSrcIncoming.put(tempDestPatternNode, seenPrefixTreeNode);

				if (newCreatedOrTouchedPTNodes != null)
					newCreatedOrTouchedPTNodes.add(seenPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex());
			} else {

				// make a new pattern for SGI
				// checking
				// and add it as
				// a new child if possible
				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
						DefaultLabeledEdge.class);

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					newPatternGraph.addVertex(patternNode);
				}

				for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
					newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
				}

				if (!newPatternGraph.vertexSet().contains(destPatternNode.destPatternNode)) {
					newPatternGraph.addVertex(destPatternNode.destPatternNode);
				}

				newPatternGraph.addEdge(destPatternNode.destPatternNode, srcPatternNode,
						new DefaultLabeledEdge(relationshipType));

				addNewChildrenOrMatches2(tempProcessingNode, tempProcessingNodeData, newPatternGraph, srcPatternNode,
						destPatternNode, srcDataGpNodeId, destPatternNode.newNodeIds,
						newlySeenPatternNodeForThisSrcIncoming, newCreatedOrTouchedPTNodes, destStepsFromRoot,
						relationshipType, seenPatternNodeFromPreviousNodesForThisSrcIncoming, snapshot,
						Dummy.DummyProperties.Direction.INCOMING, prefixTreeProcessingLevel, newChildrenOfTheSrc);

			}
		}
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
			PatternNode destPatternNode, Set<Integer> newNodeIds, HashSet<Integer> newCreatedOrTouchedPTNodes,
			String relationshipType, Integer destStepsFromRoot, int snapshot,
			HashMap<PatternNode, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> newChildrenOfTheSrc,
			boolean isDanglingPattern, Direction direction) {

		// if (numberOfPatternsInPrefixTree == 35) {
		// System.out.println();
		// }

		creationOfNewPrefixTreeNodeStart = System.nanoTime();

		// if (direction == Direction.INCOMING) {
		// if (tempProcessingNode.getData().getPatternRootNodes()
		// .contains(tempProcessingNode.getData().getSourcePatternNode())) {
		// tempProcessingNode.getData().removePatternRootNode(tempProcessingNode.getData().getSourcePatternNode());
		// tempProcessingNode.getData().setPatternRootNode(tempProcessingNode.getData().getTargetPatternNode());
		// }
		// }

		if (destPatternNode.isFocus()) {
			destStepsFromRoot = 0;
			tempProcessingNode.getData().setPatternRootNode(destPatternNode);
		}

		IPrefixTreeNodeData prefixTreeNodeData = new PrefixTreeNodeDataBitMap(newPatternGraph,
				tempProcessingNode.getData().getPatternRootNodes(), tempProcessingNode.getData(),
				tempProcessingNode.getData().getMatchedNodes(), srcPatternNode, destPatternNode, newNodeIds,
				numberOfPatternsInPrefixTree, relationshipType, destStepsFromRoot, snapshot, this.labelAdjacencyIndexer,
				isDanglingPattern, direction);

		PrefixTreeNode<IPrefixTreeNodeData> newChild = new PrefixTreeNode<IPrefixTreeNodeData>(prefixTreeNodeData);

		prefixTreeNodeIndex.put(numberOfPatternsInPrefixTree++, newChild);

		// this.getLabelAdjacencyIndexer().parentChildDifference.putIfAbsent(tempProcessingNode,
		// new HashMap<PrefixTreeNode<IPrefixTreeNodeData>,
		// SourceRelDestTypeTriple>());
		//
		// this.getLabelAdjacencyIndexer().parentChildDifference.get(tempProcessingNode).put(newChild,
		// new SourceRelDestTypeTriple(srcPatternNode.getType(),
		// destPatternNode.getType(), relationshipType));

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

			// DebugHelper.printGlobalCandidateSet(this);
		}

		this.labelAdjacencyIndexer.prefixTreeNodesOfALevel.putIfAbsent(newChild.getLevel(),
				new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>());
		this.labelAdjacencyIndexer.prefixTreeNodesOfALevel.get(newChild.getLevel()).add(newChild);

		if (newChildrenOfTheSrc != null) {
			newChildrenOfTheSrc.putIfAbsent(srcPatternNode, new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>());
			newChildrenOfTheSrc.get(srcPatternNode).add(newChild);
		}

		creationOfNewPrefixTreeNodeDuration += ((System.nanoTime() - creationOfNewPrefixTreeNodeStart) / 1e6);
		return newChild;

	}

	private PatternNode getDestPatternNodeAndCheckForGoBackToPrev(ArrayList<PatternNode> destPatternNodes,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
			Integer newNodeId, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		PatternNode destPatternNode = null;
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
			if (destPatternNode == null) {
				destPatternNodes.add(new PatternNode(otherNodeLabel));
			}

			// destDataGraphPatternNode = new
			// DataGraphMatchNode(newNodeId,
			// destPatternNode.getType(),
			// srcDataGpNode.stepsFromRoot + 1,
			// dataGraph.getNodeById(newNodeId).getDegree(Direction.OUTGOING));
		}

		return destPatternNode;
	}

	private void getDestPatternNodeAndCheckForGoBackToPrev2(ArrayList<GoBackToPrevHolder> destPatternNodes,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
			HashSet<Integer> newNodeIds, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		// PatternNode destPatternNode = null;
		// goBackToPrev = false;
		HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
		remainingNodeIds.addAll(newNodeIds);
		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			if ((patternNode != srcPatternNode)
					&& (!tempProcessingNodeData.getPatternGraph().containsEdge(srcPatternNode, patternNode))) {

				HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
				for (Integer newNodeId : newNodeIds) {
					if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
							.contains(newNodeId)) {
						goBackToPrevNodeIds.add(newNodeId);
					}
				}

				if (goBackToPrevNodeIds.size() > 0) {
					destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, patternNode));
				}

				remainingNodeIds.removeAll(goBackToPrevNodeIds);

			}
		}
		if (remainingNodeIds.size() > 0 && newNodeIds.contains(srcDataGpNodeId)) {
			// handling self-loop
			HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
			goBackToPrevNodeIds.add(srcDataGpNodeId);
			remainingNodeIds.remove(srcDataGpNodeId);
			destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, srcPatternNode));
			goBackToPrev = true;
		}

		if (remainingNodeIds.size() > 0 && tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
			// if we can find another focus
			// node, if
			// anything
			// remaining to find.

			for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
				HashSet<Integer> newNodeIdForTheType = new HashSet<Integer>();
				SetView<Integer> interesected = Sets.intersection(allNodesOfFocusType.get(type), newNodeIds);
				newNodeIdForTheType.addAll(interesected);
				remainingNodeIds.removeAll(interesected);
				if (newNodeIdForTheType.size() > 0) {
					destPatternNodes.add(new GoBackToPrevHolder(newNodeIdForTheType, false,
							new PatternNode(otherNodeLabel, The_Focus_Node)));
				}
			}
		}

		// if we already found all the focus
		// nodes, all this
		// labels is not in our focus list
		if (remainingNodeIds.size() > 0) {
			destPatternNodes.add(new GoBackToPrevHolder(remainingNodeIds, false, new PatternNode(otherNodeLabel)));
		}

		// return destPatternNodes;
	}

	private PatternNode getDestPatternNodeAndCheckForGoBackToPrevIncoming(ArrayList<PatternNode> destPatternNodes,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
			Integer newNodeId, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		PatternNode destPatternNode = null;
		goBackToPrev = false;
		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			if ((patternNode != srcPatternNode)
					&& (!tempProcessingNodeData.getPatternGraph().containsEdge(patternNode, srcPatternNode))) {
				if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						.contains(newNodeId)) {
					destPatternNodes.add(patternNode);
					goBackToPrev = true;
				}
			}
		}
		if (!goBackToPrev && srcDataGpNodeId == newNodeId) {
			// handling self-loop
			destPatternNodes.add(srcPatternNode);
			goBackToPrev = true;
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
			if (destPatternNode == null) {
				destPatternNodes.add(new PatternNode(otherNodeLabel));
			}
		}
		return destPatternNode;
	}

	private void getDestPatternNodeAndCheckForGoBackToPrevIncoming2(ArrayList<GoBackToPrevHolder> destPatternNodes,
			IPrefixTreeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
			HashSet<Integer> newNodeIds, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		// PatternNode destPatternNode = null;
		// goBackToPrev = false;
		HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
		remainingNodeIds.addAll(newNodeIds);
		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			if ((patternNode != srcPatternNode)
					&& (!tempProcessingNodeData.getPatternGraph().containsEdge(patternNode, srcPatternNode))) {

				HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
				for (Integer newNodeId : newNodeIds) {
					if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
							.contains(newNodeId)) {
						goBackToPrevNodeIds.add(newNodeId);
					}
				}
				if (goBackToPrevNodeIds.size() > 0) {
					destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, patternNode));
				}

				remainingNodeIds.removeAll(goBackToPrevNodeIds);
			}
		}

		if (remainingNodeIds.size() > 0 && newNodeIds.contains(srcDataGpNodeId)) {
			// handling self-loop
			HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
			goBackToPrevNodeIds.add(srcDataGpNodeId);
			remainingNodeIds.remove(srcDataGpNodeId);
			destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, srcPatternNode));
		}

		if (remainingNodeIds.size() > 0 && tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
			// if we can find another focus
			// node, if
			// anything
			// remaining to find.
			for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
				HashSet<Integer> newNodeIdForTheType = new HashSet<Integer>();
				SetView<Integer> interesected = Sets.intersection(allNodesOfFocusType.get(type), newNodeIds);
				newNodeIdForTheType.addAll(interesected);
				remainingNodeIds.removeAll(interesected);
				if (newNodeIdForTheType.size() > 0) {
					destPatternNodes.add(new GoBackToPrevHolder(newNodeIdForTheType, false,
							new PatternNode(otherNodeLabel, The_Focus_Node)));
				}
			}
		}
		// if we already found all the focus
		// nodes, all this
		// labels is not in our focus list
		if (remainingNodeIds.size() > 0) {
			destPatternNodes.add(new GoBackToPrevHolder(remainingNodeIds, false, new PatternNode(otherNodeLabel)));
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

		// SHAYAN
		// TODO: finalize it
		// if two patterns don't have same number of nodes?
		if (abstractPatternGraph.vertexSet().size() != newAbsPattern.vertexSet().size())
			return false;

		// if two patterns don't have same number of edges?
		if (abstractPatternGraph.edgeSet().size() != newAbsPattern.edgeSet().size())
			return false;

		// TODO: degree-distribution & label distribution checking
		// before isomorphism checking

		// if they don't have same label distribution?

		// .....

		return true;

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

	public void expandForNewInsertedEdge(PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			HashSet<Integer> newCreatedOrTouchedPTNodes, int snapshot, double threshold,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) throws Exception {

		newCreatedOrTouchedPTNodes.add(prefixTreeNode.getData().getPatternPrefixTreeNodeIndex());

		if (DummyProperties.debugMode)
			System.out.println("start from here: the prifixTree node related to the new added edge: "
					+ prefixTreeNode.getData().getMappedGraphString());

		// if (prefixTreeNode.getData().getPatternPrefixTreeNodeIndex() == 4) {
		// System.out.println();
		// }

		if (!traversalQueue.contains(prefixTreeNode))
			traversalQueue.add(prefixTreeNode);

		generatePrefixTreeFromHere(topKFrequentPatterns, newCreatedOrTouchedPTNodes, PrefixTreeMode.UPDATE, snapshot,
				threshold);
	}

	private void addIncNodeLink(PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			PrefixTreeNode<IPrefixTreeNodeData> temp, IPrefixTree prefixTree) throws Exception {

		if (!prefixTreeNode.getChildrenLinksSet().contains(temp)) {
			if (DummyProperties.debugMode) {
				System.out.println("INC: add node link from " + prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
						+ " to " + temp.getData().getPatternPrefixTreeNodeIndex());
			}
			prefixTreeNode.addNodeLink(temp);

			// this.getLabelAdjacencyIndexer().parentChildDifference.putIfAbsent(prefixTreeNode,
			// new HashMap<PrefixTreeNode<IPrefixTreeNodeData>,
			// SourceRelDestTypeTriple>());

			// this.getLabelAdjacencyIndexer().parentChildDifference.get(prefixTreeNode).put(temp,
			// new SourceRelDestTypeTriple(srcPatternNode.getType(),
			// destPatternNode.getType(), relationshipType));
		}

		// because prefixTreeNode right now has at least one
		// child
		if (prefixTreeNode.getData().isMaximalFrequent() && temp.getData().isFrequent()) {
			prefixTreeNode.getData().setCanBeMaximalFrequent(false);
			prefixTreeNode.getData().setMaximalFrequent(false, prefixTreeNode, prefixTree);
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
				// isValid = false;
				break;
			}
		}
		checkValidityAtLeastOneMatchForEachPatternNodeDuration += ((System.nanoTime()
				- checkValidityAtLeastOneMatchForEachPatternNodeStartTime) / 1e6);
		return isValid;
	}

	private void removeDestNode(PrefixTreeNode<IPrefixTreeNodeData> thisNode, Iterator<PatternNode> destIterator,
			Integer destNodeId, PatternNode destPatternNode, int snapshot) throws Exception {

		destIterator.remove();

		thisNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(destPatternNode).remove(destNodeId);

		if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) == null
				|| thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId).size() == 0) {
			thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().remove(destNodeId);
			bitmap.removeNodeIdFromPatternId(destNodeId, thisNode.getData().getPatternPrefixTreeNodeIndex());
		}

		boolean wasMFP = thisNode.getData().isMaximalFrequent();
		boolean wasFrequent = thisNode.getData().isFrequent();

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

		}

	}

	private void updateDeleteAffectedPrefixTreeNode(PrefixTreeNode<IPrefixTreeNodeData> thisNode,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns, int snapshot,
			double threshold) throws Exception {

		boolean wasMFP = thisNode.getData().isMaximalFrequent();
		// boolean wasFrequent = thisNode.getData().isFrequent();
		double prevTotalSupp = thisNode.getData().getTotalSupportFrequency();

		// if it's not valid we don't need to do anything further because we
		// already did!
		if (!thisNode.getData().isValid()) {
			return;
		}

		if (wasMFP) {

			double lowerbound = 0;
			if (topKFrequentPatterns.size() > 0) {
				lowerbound = topKFrequentPatterns.peekLast().getData().getTotalSupportFrequency();
			}

			DualSimulationHandler.computeSupport(dataGraph, thisNode, snapshot, this);

			// if support didn't change, we can easily return
			if (prevTotalSupp == thisNode.getData().getTotalSupportFrequency()) {
				return;
			}

			// if (wasFrequent) {

			// if it has the potential to be a mfp pattern

			// if it cannot be a frequent pattern
			if (thisNode.getData().getTotalSupportFrequency() < threshold) {
				thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
			}

			// if it had the potential to be inside of the topk
			// and it's frequent yet
			// ??
			// else if (thisNode.getData().getTotalSupportFrequency() <
			// lowerbound) {
			//
			// // so, as it's a MFP and it's freq yet,
			// // nothing will be changed about it.
			// // just it should be removed from top-k
			// thisNode.getData().removeFromTopK(this, thisNode);
			//
			// }
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
		if (DummyProperties.incMode)
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
			PrefixTreeBitMap prefixTree) {
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

	public void shrinkForNewDeletedEdgesInBatch(
			ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> deleteAffectedPatternsArr,
			HashMap<PrefixTreeNode<IPrefixTreeNodeData>, HashSet<SourceRelDestTriple>> deleteAffectedPatternsMap,
			int snapshot, double threshold,
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) throws Exception {

		for (PrefixTreeNode<IPrefixTreeNodeData> thisNode : deleteAffectedPatternsArr) {

			HashSet<SourceRelDestTriple> sourceRelDestTriples = deleteAffectedPatternsMap.get(thisNode);

			for (SourceRelDestTriple sourceRelDestTriple : sourceRelDestTriples) {

				Integer srcNodeId = sourceRelDestTriple.srcNodeId;
				String sourceLabel = this.labelAdjacencyIndexer.dataGraphNodeInfos.get(srcNodeId).nodeLabel;
				Integer destNodeId = sourceRelDestTriple.destNodeId;
				String destLabel = this.labelAdjacencyIndexer.dataGraphNodeInfos.get(destNodeId).nodeLabel;

				if (DummyProperties.debugMode) {
					System.out.println(
							"srcNodeId: " + srcNodeId + ", destNodeId: " + destNodeId + ", destLabel:" + destLabel);
				}

				if (DummyProperties.debugMode) {
					System.out.println("DEL-Affected: Before remove: " + thisNode.getData());
				}

				// we should remove the corresponding destNodeId in the
				// destPatternNode
				if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId) == null) {
					if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) != null) {
						Iterator<PatternNode> destIterator = thisNode.getData().getMatchedNodes()
								.getPatternNodeOfNeo4jNode().get(destNodeId).iterator();

						while (destIterator.hasNext()) {
							PatternNode destPatternNode = destIterator.next();
							if (!thisNode.getData().getPatternGraph().vertexSet().contains(destPatternNode)) {
								throw new Exception(
										"!thisNode.getData().getPatternGraph().vertexSet().contains(destPatternNode):"
												+ destNodeId);
							}
							for (DefaultLabeledEdge e : thisNode.getData().getPatternGraph()
									.incomingEdgesOf(destPatternNode)) {
								if (thisNode.getData().getPatternGraph().getEdgeSource(e).getLabel().equals(sourceLabel)
										&& e.getType().equals(sourceRelDestTriple.relationshipType)) {
									removeDestNode(thisNode, destIterator, destNodeId, destPatternNode, snapshot);
									// removeDestNodeForDelete(destIterator,
									// thisNode, destNodeId, destPatternNode,
									// topKFrequentPatterns, snapshot,
									// threshold, relationshipType);
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
							while (destIterator.hasNext()) {
								PatternNode destPatternNode = destIterator.next();
								// A->B->C->B
								if (!thisNode.getData().getPatternGraph().containsEdge(srcPatternNode,
										destPatternNode)) {
									if (DummyProperties.debugMode)
										System.out.println("this case is happened!");
									continue;
								}
								removeDestNode(thisNode, destIterator, destNodeId, destPatternNode, snapshot);
								// removeDestNodeForDelete(destIterator,
								// thisNode, destNodeId, destPatternNode,
								// topKFrequentPatterns, snapshot, threshold,
								// sourceRelDestTriple.relationshipType);

							}
						}
					}
				}

			}
			if (DummyProperties.debugMode) {
				System.out.println("DEL-Affected: After remove: " + thisNode.getData());
			}

			updateDeleteAffectedPrefixTreeNode(thisNode, topKFrequentPatterns, snapshot, threshold);
		}
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

	@Override
	public void resetDurationOfBiSimChecking() {
		biSimTimeDuration = 0;

	}

	@Override
	public void resetNumberOfComputeSupport() {
		numberOfComputeSupport = 0;

	}

	@Override
	public void resetDurationOfComputeSupport() {
		computeSupportDuration = 0;

	}

	@Override
	public void resetDurationOfNewPrefixTreeGeneration() {
		creationOfNewPrefixTreeNodeDuration = 0;

	}

	@Override
	public long getNumberOfIsoCheckingRequest() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public long getNumberOfRealIsoChecking() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsomorphism(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {

		return getIsoChecker(abstractPatternGraph, newAbsPattern);
	}

	@Override
	public void incrementBiSimCheckingRequest() {
		numberOfBiSimCheckingRequest++;
	}

	@Override
	public void incrementRealBiSimChecking() {
		numberOfRealBiSimChecking++;
	}

	@Override
	public void updateDurationOfBiSimChecking(double newDuration) {
		biSimTimeDuration += newDuration;
	}

	@Override
	public double getDurationOfBiSimChecking() {
		return biSimTimeDuration;
	}

	@Override
	public long getNumberOfBiSimCheckingRequest() {
		// TODO Auto-generated method stub
		return numberOfBiSimCheckingRequest;
	}

	@Override
	public long getNumberOfRealBiSimChecking() {
		// TODO Auto-generated method stub
		return numberOfRealBiSimChecking;
	}

	public void resetNumberIfBiSimChecking() {
		this.numberOfBiSimCheckingRequest = 0;
		this.numberOfRealBiSimChecking = 0;

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

class PatternGraphAndPreMatches {
	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph;
	public SetView<Integer> preMatches;
	public PatternNode danglingPatternNode;

	public PatternGraphAndPreMatches(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph,
			SetView<Integer> preMatches, PatternNode danglingPatternNode) {
		this.patternGraph = patternGraph;
		this.preMatches = preMatches;
		this.danglingPatternNode = danglingPatternNode;
	}

}

class MyGraphPaths {

	HashMap<PatternNode, ArrayList<MyGraphPath>> confirmedPaths = new HashMap<PatternNode, ArrayList<MyGraphPath>>();

	public HashMap<PatternNode, ArrayList<MyGraphPath>> getPaths() {
		return confirmedPaths;
	}

	public MyGraphPaths(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode) {

		ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> graphPattern = tempProcessingNode.getData()
				.getPatternGraph();
		HashSet<PatternNode> rootNodes = tempProcessingNode.getData().getPatternRootNodes();
		// PatternNode destNode = null;
		// if (tempProcessingNode.getData().getGrowthDirection() ==
		// Direction.OUTGOING) {
		// destNode = tempProcessingNode.getData().getTargetPatternNode();
		// } else {
		// destNode = tempProcessingNode.getData().getSourcePatternNode();
		// }

		for (PatternNode rootNode : rootNodes) {
			HashSet<PatternNode> visitedPatternNodes = new HashSet<PatternNode>();
			confirmedPaths.put(rootNode, new ArrayList<MyGraphPath>());
			LinkedList<MyGraphPath> queue = new LinkedList<MyGraphPath>();
			queue.add(new MyGraphPath(rootNode, null, new ArrayList<PatternNode>(), new ArrayList<Direction>()));
			visitedPatternNodes.add(rootNode);

			while (!queue.isEmpty()) {
				MyGraphPath node = queue.poll();
				int lastElement = node.path.size() - 1;
				boolean notHavingAnyOutgoing = true;
				boolean notHavingAnyIncoming = true;

				for (DefaultLabeledEdge e : graphPattern.outgoingEdgesOf(node.path.get(lastElement))) {
					if (!visitedPatternNodes.contains(graphPattern.getEdgeTarget(e))) {
						queue.add(new MyGraphPath(graphPattern.getEdgeTarget(e), Direction.OUTGOING, node.path,
								node.directions));
						visitedPatternNodes.add(graphPattern.getEdgeTarget(e));
						notHavingAnyOutgoing = false;
					}
				}
				for (DefaultLabeledEdge e : graphPattern.incomingEdgesOf(node.path.get(lastElement))) {
					if (!visitedPatternNodes.contains(graphPattern.getEdgeSource(e))) {
						queue.add(new MyGraphPath(graphPattern.getEdgeSource(e), Direction.INCOMING, node.path,
								node.directions));
						visitedPatternNodes.add(graphPattern.getEdgeSource(e));
						notHavingAnyIncoming = false;
					}
				}

				if (notHavingAnyIncoming && notHavingAnyOutgoing)
					confirmedPaths.get(rootNode).add(node);
			}
		}
	}
}

class MyGraphPath {
	ArrayList<PatternNode> path = new ArrayList<PatternNode>();
	ArrayList<Direction> directions = new ArrayList<Direction>();

	public MyGraphPath(PatternNode patternNode, Direction direction, ArrayList<PatternNode> parentPath,
			ArrayList<Direction> parentDirections) {
		this.path.addAll(parentPath);
		this.path.add(patternNode);
		this.directions.addAll(parentDirections);
		this.directions.add(direction);
	}
}

class SelectedMinPatternNodeWithItsPath {
	MyGraphPath myGraphPath;
	int selectedMinPatternNode;

	public SelectedMinPatternNodeWithItsPath(MyGraphPath myGraphPath, int selectedMinPatternNode) {
		this.myGraphPath = myGraphPath;
		this.selectedMinPatternNode = selectedMinPatternNode;
	}
}
