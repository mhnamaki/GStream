package src.optSubAlg.topkMonitoring;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
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

import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;
import src.optSubAlg.prefixTree.PrefixTreeSubOpt;
import src.utilities.Bitmap;
import src.utilities.CorrectnessChecking;
import src.utilities.CorrespondsOfSrcRelDest;
import src.utilities.DebugHelper;
import src.utilities.DefaultLabeledEdge;
import src.utilities.Dummy;
import src.utilities.InfoHolder;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;
import src.utilities.SourceRelDestTriple;
import src.utilities.SourceRelDestTypeTriple;
import src.utilities.SupportComparator;
import src.utilities.SupportHelper;
import src.utilities.TimeLogger;
import src.utilities.TopKHandler;
import src.utilities.Visualizer;
import src.utilities.WinMine;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy.DummyProperties.PrefixTreeMode;
import src.utilities.EdgeStreamCacheItem;

//TODO: Changing the implementation for dgOfAbsPattern & patternOfNeo4jNode to a BIMAP
//TODO: reduce the # of isomorphism
//TODO: time outputs are not working properly

public class SnapshotTopkMonitoringSubOpt {

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
	private int edgeStreamCacheCapacity = 1000;
	private boolean windowMode = false;
	private int windowSizeL = 2;
	private int startingWindow = 0;
	private int endingWindow = 1; // 0, 1, 2

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
		PrefixTreeSubOpt prefixTree = new PrefixTreeSubOpt(this);

		// because in future we want to check if in prev-d-hops any focus
		// nodes exist we maintain their id's here.
		HashSet<Integer> allFocusNodes = new HashSet<Integer>();
		for (String focusLabel : prefixTree.allNodesOfFocusType.keySet()) {
			allFocusNodes.addAll(prefixTree.allNodesOfFocusType.get(focusLabel));
			// for (Integer nodeId :
			// prefixTree.allNodesOfFocusType.get(focusLabel)) {
			// prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(nodeId).setFocus();
			// }
			numberOfAllFocusNodes += prefixTree.allNodesOfFocusType.get(focusLabel).size();
		}

		// STAT Of the DB START
		double avgG0OutDegreeOfFocusNodes = DummyFunctions.getAvgOutDegreeOfFocusNodes(dataGraph, allFocusNodes,
				numberOfAllFocusNodes);
		// STAT Of the DB END

		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;

		// specific for VF2 to induce a smaller graph:
		prefixTree.getLabelAdjacencyIndexer().addFocusAsVertices(dataGraph, allFocusNodes);
		for (Integer srcNodeId : prefixTree.getLabelAdjacencyIndexer().relTypeOfSrcAndDest.keySet()) {
			for (Integer destNodeId : prefixTree.getLabelAdjacencyIndexer().relTypeOfSrcAndDest.get(srcNodeId)
					.keySet()) {
				prefixTree.getLabelAdjacencyIndexer().addEdgeToVF2InducedGraph(dataGraph, srcNodeId, destNodeId);
			}
		}

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

		if (DummyProperties.debugMode)
			System.out.println("before inc start");

		SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaEFileOrFiles, dateFormat);

		DummyProperties.incMode = true;

		int allEdgesUpdates = -1;
		int addedEdges = 0;
		int removedEdges = 0;
		int nonAffectedInsertedEdges = 0;
		int nonAffectedDeletedEdges = 0;
		int affectedEdgesForExpansion = 0;
		int affectedEdgesForDeletion = 0;

		double handlingDataStreamStartTime = 0;
		double insertionResponseTimeStartTime = 0.0d;
		double insertionPutMatchesResponseTimeStartTime = 0.0d;
		double insertionForExpansionResponseTimeStartTime = 0.0d;

		double deletionResponseTimeStartTime = 0.0d;
		double creationOfInducedGraphStartTime = 0.0d;
		double updateNeighborhoodStartTime = 0.0d;

		double handlingDataStreamDuration = 0;
		double insertionResponseDuration = 0.0d;
		double insertionPutMatchesResponseDuration = 0.0d;
		double insertionForExpansionResponseDuration = 0.0d;
		double deletionResponseDuration = 0.0d;
		double creationOfInducedGraphDuration = 0.0d;
		double updateNeighborhoodDuration = 0.0d;
		double allEdgesResponseTimeDuration = 0.0d;

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

		ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> insertAffectedPatternsArr = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();
		HashSet<PrefixTreeNode<IPrefixTreeNodeData>> insertAffectedPatternsSet = new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>();

		ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> deleteAffectedPatternsArr = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();

		HashMap<PrefixTreeNode<IPrefixTreeNodeData>, HashSet<SourceRelDestTriple>> deleteAffectedPatternsMap = new HashMap<PrefixTreeNode<IPrefixTreeNodeData>, HashSet<SourceRelDestTriple>>();

		ArrayList<EdgeStreamCacheItem> edgesStreamCache = new ArrayList<EdgeStreamCacheItem>(edgeStreamCacheCapacity);

		int prevSnapshot = 0;
		boolean streamIsOver = false;
		double starttime = 0;
		double winMineStart = 0d;
		double winMineDuraion = 0d;

		double carryOverStart = 0d;
		double carryOverDuraion = 0d;
		for (int i = 0; i < numberOfTransactionInASnapshot * (numberOfSnapshots - 1); i++) {

			allEdgesUpdates++;

			if (allEdgesUpdates < numberOfIgnoranceInitEdges) {
				i = -1;
				snapshotSim.getNextStreamEdge();
				continue;
			}

			if (DummyProperties.debugMode)
				System.out.println("after numberOfIgnoranceInitEdges");

			// because we started from G0 (so new E makes other snapshots)
			int snapshot = (i / numberOfTransactionInASnapshot) + 1;

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

			// if (snapshot > 2)
			// break;

			DirectedGraph<Integer, EdgeStreamCacheItem> cacheStreamGraph = new DefaultDirectedGraph<Integer, EdgeStreamCacheItem>(
					EdgeStreamCacheItem.class);

			edgesStreamCache.clear();

			int allCachedEdges = 0;
			HashMap<Integer, Integer> newlyTouchedFocusNodesOrSrcPatternNodes = new HashMap<Integer, Integer>();

			boolean anyNewDeletedEdge = false;
			deleteAffectedPatternsMap.clear();
			deleteAffectedPatternsArr.clear();

			boolean anyNewInsertedEdge = false;
			insertAffectedPatternsSet.clear();
			insertAffectedPatternsArr.clear();

			while (allCachedEdges < edgeStreamCacheCapacity && ((i / numberOfTransactionInASnapshot) + 1) == snapshot) {

				// if (snapshot > 2)
				// break;

				StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

				if (nextStreamEdge == null) {
					streamIsOver = true;
					break;
				}

				// TODO: should be removed, just for the sake of insert
				// experiments
				// if (!nextStreamEdge.isAdded()) {
				// allEdgesForIgn--;
				// //i = Math.max(i--, 0);
				// continue;
				// } else {
				// allEdgesForIgn++;
				// }

				allCachedEdges++;

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

				boolean srcIsAFocus = false;
				srcIsAFocus = allFocusNodes.contains(srcNodeId);

				boolean destIsAFocus = false;
				if (srcLabel != destLabel || !srcIsAFocus) {
					destIsAFocus = allFocusNodes.contains(destNodeId);
				}

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
					System.out.println((allCachedEdges + i) + ": " + " nextStreamEdge: " + srcLabel + "_" + srcNodeId
							+ " -> " + destLabel + "_" + destNodeId + " => isAdded? " + nextStreamEdge.isAdded());

				// handling insertion/deletion of an edge and degree indexing
				// maintenance

				handlingDataStreamStartTime = System.nanoTime();
				handlingDataStream(nextStreamEdge, prefixTree, srcNode, destNode, srcNodeId, destNodeId);
				handlingDataStreamDuration += ((System.nanoTime() - handlingDataStreamStartTime) / 1e6);

				starttime = System.nanoTime();
				updateNeighborhoodStartTime = System.nanoTime();
				prefixTree.getLabelAdjacencyIndexer().updateNeighborhood(dataGraph, srcNodeId, destNodeId,
						nextStreamEdge.getRelationshipType(), maxAllowedHops, nextStreamEdge.isAdded());
				updateNeighborhoodDuration += ((System.nanoTime() - updateNeighborhoodStartTime) / 1e6);

				creationOfInducedGraphStartTime = System.nanoTime();
				EdgeStreamCacheItem eCacheStream = new EdgeStreamCacheItem(srcNodeId, destNodeId,
						srcLabel + (srcIsAFocus ? "*" : ""), nextStreamEdge.getRelationshipType(),
						destLabel + (destIsAFocus ? "*" : ""), nextStreamEdge.isAdded());

				edgesStreamCache.add(eCacheStream);

				if (nextStreamEdge.isAdded()) {
					if (srcIsAFocus)
						newlyTouchedFocusNodesOrSrcPatternNodes.putIfAbsent(srcNodeId, 0);
					else {
						MinStepHolder minStepHolder = new MinStepHolder();

						findMinStepSrcPatternNodeForInsert(prefixTree, srcNodeId, destNodeId,
								patternIndexByNodeIdBitMap[srcNodeId], patternIndexByNodeIdBitMap[destNodeId],
								minStepHolder);

						if (minStepHolder.minStep < Integer.MAX_VALUE)
							newlyTouchedFocusNodesOrSrcPatternNodes.putIfAbsent(srcNodeId, minStepHolder.minStep);
					}

					cacheStreamGraph.addVertex(srcNodeId);
					cacheStreamGraph.addVertex(destNodeId);
					cacheStreamGraph.addEdge(srcNodeId, destNodeId, eCacheStream);
					addedEdges++;
					anyNewInsertedEdge = true;

				} else {
					cacheStreamGraph.removeEdge(srcNodeId, destNodeId);
					removedEdges++;
					anyNewDeletedEdge = true;
				}

				creationOfInducedGraphDuration += ((System.nanoTime() - creationOfInducedGraphStartTime) / 1e6);

				// if no incoming edge and this is not a focus node
				if (prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).inDegree == 0
						&& !srcIsAFocus) {
					if (!nextStreamEdge.isAdded()) {
						// nonAffectedDeletedEdges++;
						deletionResponseDuration += ((System.nanoTime() - starttime) / 1e6);
					} else {
						// nonAffectedInsertedEdges++;
						insertionResponseDuration += ((System.nanoTime() - starttime) / 1e6);
					}

					allEdgesResponseTimeDuration += ((System.nanoTime() - starttime) / 1e6);
					// System.out.println("src has no incoming and is not
					// focus!");
					continue;
				}

			}

			if (DummyProperties.debugMode)
				System.out.println("after first batch of cachestream");

			if (streamIsOver && allCachedEdges == 0) {
				break;
			}

			if (anyNewInsertedEdge) {
				// int importantCounter = 0;
				insertionResponseTimeStartTime = System.nanoTime();

				Queue<CacheGraphStreamQueueItem> nodesToBeTraversedQueue = new LinkedList<CacheGraphStreamQueueItem>();

				for (Integer srcNodeIdInGi : newlyTouchedFocusNodesOrSrcPatternNodes.keySet()) {

					nodesToBeTraversedQueue.clear();

					nodesToBeTraversedQueue.add(new CacheGraphStreamQueueItem(srcNodeIdInGi,
							newlyTouchedFocusNodesOrSrcPatternNodes.get(srcNodeIdInGi)));

					while (!nodesToBeTraversedQueue.isEmpty()) {
						CacheGraphStreamQueueItem queueItem = nodesToBeTraversedQueue.poll();
						if (queueItem.hopAway < maxAllowedHops) {
							// if
							// (!cacheStreamGraph.vertexSet().contains(queueItem.nodeId))
							// {
							// System.out.println();
							// }
							for (EdgeStreamCacheItem e : cacheStreamGraph.outgoingEdgesOf(queueItem.nodeId)) {
								if (!e.isImportant) {
									e.isImportant = true;
									// importantCounter++;
									nodesToBeTraversedQueue.add(new CacheGraphStreamQueueItem(
											cacheStreamGraph.getEdgeTarget(e), queueItem.hopAway + 1));
								}

							}
						}
					}
				}

				HashMap<SourceRelDestTypeTriple, HashSet<EdgeStreamCacheItem>> edgeStreamsOfSrcRelTypeDest = new HashMap<SourceRelDestTypeTriple, HashSet<EdgeStreamCacheItem>>();
				for (EdgeStreamCacheItem e : cacheStreamGraph.edgeSet()) {
					if (e.isImportant) {
						SourceRelDestTypeTriple key = new SourceRelDestTypeTriple(e.srcType, e.destType,
								e.relationshipType);
						edgeStreamsOfSrcRelTypeDest.putIfAbsent(key, new HashSet<EdgeStreamCacheItem>());
						edgeStreamsOfSrcRelTypeDest.get(key).add(e);
					} else {
						nonAffectedInsertedEdges++;
					}
				}

				// we should use indexer here to find all patterns that can
				// accept these new edges,
				// just add matches to them.
				for (SourceRelDestTypeTriple srcRelDestKey : edgeStreamsOfSrcRelTypeDest.keySet()) {

					if (!prefixTree.labelAdjacencyIndexer.correspondsOfSrcRelDestType.containsKey(srcRelDestKey))
						continue;

					CorrespondsOfSrcRelDest correspondsOfSrcRelDest = prefixTree.labelAdjacencyIndexer.correspondsOfSrcRelDestType
							.get(srcRelDestKey);

					// for all patterns that have this src-rel->dest in them
					for (Integer patternGraphIndex : correspondsOfSrcRelDest.patternGraphsIndex) {

						// for all possible source/dest patterns nodes in all of
						// these patterns

						for (PatternNode possibleSrcPatternNode : correspondsOfSrcRelDest.possibleSrcPatternNodes) {
							for (PatternNode possibleDestPatternNode : correspondsOfSrcRelDest.possibleDestPatternNodes) {
								Set<DefaultLabeledEdge> edges = prefixTree.prefixTreeNodeIndex.get(patternGraphIndex)
										.getData().getPatternGraph()
										.getAllEdges(possibleSrcPatternNode, possibleDestPatternNode);

								if (edges != null && edges.size() > 0) {

									// boolean isUpdated = false;
									for (EdgeStreamCacheItem sameTypeEdgeStreamCacheItem : edgeStreamsOfSrcRelTypeDest
											.get(srcRelDestKey)) {

										IPrefixTreeNodeData prefixTreeNodeData = prefixTree.prefixTreeNodeIndex
												.get(patternGraphIndex).getData();

										// for one-edge pattern we don't need to
										// check
										if (prefixTreeNodeData.getPatternGraph().edgeSet().size() > 1) {

											// expansion reduction without
											// requiring an exact
											// dual-simulation:

											if (prefixTreeNodeData.getPatternGraph()
													.outDegreeOf(possibleSrcPatternNode) > prefixTree
															.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(
																	sameTypeEdgeStreamCacheItem.sourceNodeId).outDegree)
												continue;

											if (prefixTreeNodeData.getPatternGraph()
													.inDegreeOf(possibleSrcPatternNode) > prefixTree
															.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(
																	sameTypeEdgeStreamCacheItem.sourceNodeId).inDegree)
												continue;

											if (prefixTreeNodeData.getPatternGraph()
													.outDegreeOf(possibleDestPatternNode) > prefixTree
															.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(
																	sameTypeEdgeStreamCacheItem.destNodeId).outDegree)
												continue;

											if (prefixTreeNodeData.getPatternGraph()
													.inDegreeOf(possibleDestPatternNode) > prefixTree
															.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(
																	sameTypeEdgeStreamCacheItem.destNodeId).inDegree)
												continue;

											boolean satisfyNextType = true;
											for (String nextTypeKey : prefixTreeNodeData
													.getFrequencyOfNextNeighborOfSameType().get(possibleSrcPatternNode)
													.keySet()) {

												if (!prefixTree.getLabelAdjacencyIndexer().distinctNodesOfDHopsAway
														.get(sameTypeEdgeStreamCacheItem.sourceNodeId)
														.containsKey(nextTypeKey)
														|| !prefixTree
																.getLabelAdjacencyIndexer().distinctNodesOfDHopsAway
																		.get(sameTypeEdgeStreamCacheItem.sourceNodeId)
																		.get(nextTypeKey).containsKey(1)) {
													satisfyNextType = false;
													break;
												}

												if (prefixTreeNodeData.getFrequencyOfNextNeighborOfSameType()
														.get(possibleSrcPatternNode)
														.get(nextTypeKey) > prefixTree
																.getLabelAdjacencyIndexer().distinctNodesOfDHopsAway
																		.get(sameTypeEdgeStreamCacheItem.sourceNodeId)
																		.get(nextTypeKey).get(1).size()) {
													satisfyNextType = false;
													break;
												}
											}

											if (!satisfyNextType) {
												continue;
											}

											for (String nextTypeKey : prefixTreeNodeData
													.getFrequencyOfNextNeighborOfSameType().get(possibleDestPatternNode)
													.keySet()) {
												if (!prefixTree.getLabelAdjacencyIndexer().distinctNodesOfDHopsAway
														.get(sameTypeEdgeStreamCacheItem.destNodeId)
														.containsKey(nextTypeKey)
														|| !prefixTree
																.getLabelAdjacencyIndexer().distinctNodesOfDHopsAway
																		.get(sameTypeEdgeStreamCacheItem.destNodeId)
																		.get(nextTypeKey).containsKey(1)) {
													satisfyNextType = false;
													break;
												}

												if (prefixTreeNodeData.getFrequencyOfNextNeighborOfSameType()
														.get(possibleDestPatternNode)
														.get(nextTypeKey) > prefixTree
																.getLabelAdjacencyIndexer().distinctNodesOfDHopsAway
																		.get(sameTypeEdgeStreamCacheItem.destNodeId)
																		.get(nextTypeKey).get(1).size()) {
													satisfyNextType = false;
													break;
												}
											}

											if (!satisfyNextType) {
												continue;
											}
										}

										// add matches for possible source
										// nodes
										prefixTreeNodeData.addImmediateMatches(possibleSrcPatternNode,
												sameTypeEdgeStreamCacheItem.sourceNodeId);

										// add matches for possible dest nodes
										prefixTreeNodeData.addImmediateMatches(possibleDestPatternNode,
												sameTypeEdgeStreamCacheItem.destNodeId);

										insertAffectedPatternsSet
												.add(prefixTree.prefixTreeNodeIndex.get(patternGraphIndex));
									}

								}
							}
						}

						if (insertAffectedPatternsSet.contains(prefixTree.prefixTreeNodeIndex.get(patternGraphIndex)))
							prefixTree.prefixTreeNodeIndex.get(patternGraphIndex).getData()
									.setPrefixTreeMode(PrefixTreeMode.UPDATE);
					}

					// we should also separate the computeSupport for verify and
					// do
					// some kind of neighbor checking for removing
					// nonsense matches without verification
				}
				BooleanInside booleanInsideForIns = new BooleanInside();
				for (SourceRelDestTypeTriple srcRelDestKey : edgeStreamsOfSrcRelTypeDest.keySet()) {
					for (EdgeStreamCacheItem eCacheItem : edgeStreamsOfSrcRelTypeDest.get(srcRelDestKey)) {
						if (eCacheItem.isAdded) {
							booleanInsideForIns.isFound = false;
							findAffectedParentPatternsForInsertAndAddNewUnexpandedMatch(booleanInsideForIns,
									insertAffectedPatternsSet, prefixTree, eCacheItem.sourceNodeId,
									eCacheItem.destNodeId, patternIndexByNodeIdBitMap[eCacheItem.sourceNodeId],
									patternIndexByNodeIdBitMap[eCacheItem.destNodeId]);

							if (booleanInsideForIns.isFound) {
								affectedEdgesForExpansion++;
							}
						}
					}
				}

				insertionResponseDuration += ((System.nanoTime() - insertionResponseTimeStartTime) / 1e6);
				insertionPutMatchesResponseDuration += ((System.nanoTime() - insertionPutMatchesResponseTimeStartTime)
						/ 1e6);
			}

			if (DummyProperties.debugMode)
				System.out.println("after put matches");
			// findAffectedPatterns(affectedPatterns, prefixTree, srcNodeId,
			// destNodeId,
			// patternIndexByNodeIdBitMap[srcNodeId],
			// patternIndexByNodeIdBitMap[destNodeId],
			// nextStreamEdge.isAdded())

			if (anyNewDeletedEdge) {

				deletionResponseTimeStartTime = System.nanoTime();
				BooleanInside booleanInsideForDel = new BooleanInside();
				for (EdgeStreamCacheItem eStreamCacheItem : edgesStreamCache) {
					if (!eStreamCacheItem.isAdded) {
						booleanInsideForDel.isFound = false;
						findAffectedPatternsForDelete(booleanInsideForDel, deleteAffectedPatternsMap, prefixTree,
								eStreamCacheItem.sourceNodeId, eStreamCacheItem.destNodeId,
								patternIndexByNodeIdBitMap[eStreamCacheItem.sourceNodeId],
								patternIndexByNodeIdBitMap[eStreamCacheItem.destNodeId],
								eStreamCacheItem.relationshipType);

						if (!booleanInsideForDel.isFound) {
							nonAffectedDeletedEdges++;
						}
					}
				}

				deleteAffectedPatternsArr.addAll(deleteAffectedPatternsMap.keySet());

				sortPatternsInAscending(deleteAffectedPatternsArr);

				prefixTree.shrinkForNewDeletedEdgesInBatch(deleteAffectedPatternsArr, deleteAffectedPatternsMap,
						snapshot, threshold, topKFrequentPatterns);

				deletionResponseDuration += ((System.nanoTime() - deletionResponseTimeStartTime) / 1e6);

			}

			insertionResponseTimeStartTime = System.nanoTime();
			insertionForExpansionResponseTimeStartTime = System.nanoTime();
			// convert set to array for sorting
			for (PrefixTreeNode<IPrefixTreeNodeData> affectedPatternIndex : insertAffectedPatternsSet) {
				insertAffectedPatternsArr.add(affectedPatternIndex);
			}
			numberOfAffectedPatterns += insertAffectedPatternsArr.size();

			sortPatternsInAscending(insertAffectedPatternsArr);
			HashSet<Integer> newCreatedOrTouchedPTNodes = new HashSet<Integer>();

			for (int p = 0; p < insertAffectedPatternsArr.size(); p++) {

				// TODO: FOR-DEBUG IS ADDED
				// if (affectedPatterns.get(p) == null ||
				// affectedPatterns.get(p).getData() == null) {
				// continue;
				// }

				// System.out.println("affectedPatterns: " +
				// affectedPatterns.get(p).getData().getMappedGraphString());
				// because we checked everything from there, so, we
				// don't need to check it again

				if (
				// FOR-DEBUG Started
				// affectedPatterns.get(p) != null &&
				// affectedPatterns.get(p).getData() != null &&
				// above line is for wrong pattern which we deleted!
				!newCreatedOrTouchedPTNodes
						.contains(insertAffectedPatternsArr.get(p).getData().getPatternPrefixTreeNodeIndex())) {

					// System.out.println(
					// "generator is " +
					// affectedPatterns.get(p).getData().patternPrefixTreeNodeIndex);

					// if
					// (affectedPatterns.get(p).getData().patternPrefixTreeNodeIndex
					// == 2) {
					// System.out.println();
					// }

					if (insertAffectedPatternsArr.get(p).getData()
							.getNewUnexpandedPatternsNodesOfNeo4jNodes() != null) {

						prefixTree.expandForNewInsertedEdge(insertAffectedPatternsArr.get(p),
								newCreatedOrTouchedPTNodes, snapshot, threshold, topKFrequentPatterns);
					}
				} else {
					numberOfAffectedPatternsWithoutRecheck++;
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

			insertionResponseDuration += ((System.nanoTime() - insertionResponseTimeStartTime) / 1e6);
			insertionForExpansionResponseDuration += ((System.nanoTime() - insertionForExpansionResponseTimeStartTime)
					/ 1e6);

			// System.out.println("traversal after stream edge: " +
			// nextStreamEdge.getSourceNode() + " -> "
			// + nextStreamEdge.getDestinationNode() + " => isAdded? " +
			// nextStreamEdge.isAdded());
			// prefixTree.bfsTraverse(prefixTree.emptyPTRootNode);

			i += (allCachedEdges - 1);
			allEdgesResponseTimeDuration += ((System.nanoTime() - starttime) / 1e6);
		}

		System.out.println("final traversal");
		int numberOfAllPatterns = prefixTree.bfsTraverse(prefixTree.emptyPTRootNode);

		// the order should be like this:
		// we should first visualize then printTopk
		if (DummyProperties.debugMode)
			Visualizer.visualizeTopK(topKFrequentPatterns);

		// DebugHelper.printSubGraphIsomorphicTopkPatterns(topKFrequentPatterns);

		if (DummyProperties.debugMode)
			DebugHelper.printIsomorphicPatterns(prefixTree);

		TopKHandler.printTopK(topKFrequentPatterns);

		double avgGTOutDegreeOfFocusNodes = DummyFunctions.getAvgOutDegreeOfFocusNodes(dataGraph, allFocusNodes,
				numberOfAllFocusNodes);
		int numberOfAllRelationshipsGT = Dummy.DummyFunctions.getNumberOfAllRels(dataGraph);

		ArrayList<InfoHolder> timeInfos = new ArrayList<InfoHolder>();

		timeInfos.add(new InfoHolder(0, "Nodes", numberOfAllNodes));
		timeInfos.add(new InfoHolder(1, "G0 Relationships", numberOfAllRelationshipsG0));
		timeInfos.add(new InfoHolder(2, "GT Relationships", numberOfAllRelationshipsGT));

		timeInfos.add(new InfoHolder(3, "Distinct Labels", differentLabels.size()));
		timeInfos.add(new InfoHolder(4, "Distinct RelTypes", differentRelTypes.size()));

		timeInfos.add(new InfoHolder(5, "Average of Total Degrees", avgDegrees));
		timeInfos.add(new InfoHolder(6, "Average of G0 Focus Out Degrees", avgG0OutDegreeOfFocusNodes));
		timeInfos.add(new InfoHolder(7, "Average of GT Focus Out Degrees", avgGTOutDegreeOfFocusNodes));

		timeInfos.add(new InfoHolder(8, "k", k));
		timeInfos.add(new InfoHolder(9, "Threshold", threshold));
		timeInfos.add(new InfoHolder(10, "Requested Transactions In A Snapshot", numberOfTransactionInASnapshot));
		timeInfos.add(new InfoHolder(11, "Requested Snapshots", numberOfSnapshots));

		timeInfos.add(new InfoHolder(12, "Diameter", maxAllowedHops));
		timeInfos.add(new InfoHolder(13, "Max Edges", maxAllowedEdges));
		timeInfos.add(new InfoHolder(14, "Focus Nodes", numberOfAllFocusNodes));

		timeInfos.add(new InfoHolder(15, "All Patterns", numberOfAllPatterns));
		timeInfos.add(new InfoHolder(16, "All G0 Patterns", numberOfAllG0Patterns));
		timeInfos.add(new InfoHolder(17, "All Inc Patterns", (numberOfAllPatterns - numberOfAllG0Patterns)));

		timeInfos.add(new InfoHolder(18, "TotalAllMatches", prefixTree.numberOfTotalAllMatches));
		timeInfos.add(new InfoHolder(19, "TotalAllMatches G0", initNumberOfTotalAllMatches));
		timeInfos.add(new InfoHolder(20, "TotalAllMatches Just Inc",
				(prefixTree.numberOfTotalAllMatches - initNumberOfTotalAllMatches)));

		timeInfos.add(new InfoHolder(21, "PrefixTree G0 Generation Time", g0PrefixTreeGenerationDuration));

		timeInfos.add(new InfoHolder(22, "All edges response time", allEdgesResponseTimeDuration));
		timeInfos.add(new InfoHolder(23, "All Insertion Response Time", insertionResponseDuration));
		timeInfos.add(
				new InfoHolder(24, "All Insertion For Expansion Response Time", insertionForExpansionResponseDuration));

		timeInfos.add(new InfoHolder(25, "Exp > checkSameTypeSameStepsFromRootHasEnoughMatches",
				prefixTree.checkSameTypeSameStepsFromRootHasEnoughMatchesDuration));
		timeInfos.add(new InfoHolder(26, "Exp > checkValidityAtLeastOneMatchForEachPatternNode",
				prefixTree.checkValidityAtLeastOneMatchForEachPatternNodeDuration));
		timeInfos.add(new InfoHolder(27, "Exp > processQueueNode", prefixTree.processQueueNodeDuration));

		timeInfos.add(
				new InfoHolder(28, "All Insertion For Put Matches Response Time", insertionPutMatchesResponseDuration));
		timeInfos.add(new InfoHolder(29, "All Deletion Response Time", deletionResponseDuration));

		timeInfos.add(new InfoHolder(30, "Total deltaE", (addedEdges + removedEdges)));
		timeInfos.add(new InfoHolder(31, "All inserted edges", addedEdges));
		timeInfos.add(new InfoHolder(32, "All deleted edges", removedEdges));

		timeInfos.add(new InfoHolder(33, "affected inserted edges", (addedEdges - nonAffectedInsertedEdges)));
		timeInfos.add(new InfoHolder(34, "affected deleted edges", (removedEdges - nonAffectedDeletedEdges)));

		timeInfos.add(new InfoHolder(35, "All Affected Patterns In All Rounds", numberOfAffectedPatterns));
		timeInfos.add(new InfoHolder(36, "Affected Patterns Without Recheck", numberOfAffectedPatternsWithoutRecheck));

		timeInfos.add(new InfoHolder(37, "Total physical edge insertion/deletion time", handlingDataStreamDuration));

		timeInfos.add(new InfoHolder(38, "Iso Checking Time G0", isoCheckingTimeG0));
		timeInfos.add(new InfoHolder(39, "Number Of Iso Checking Request G0", numberOfIsoCheckingRequestG0));
		timeInfos.add(new InfoHolder(39, "Number Of Real Iso Checking G0", numberOfRealIsoCheckingG0));
		timeInfos.add(new InfoHolder(40, "Creation Of New PrefixTree Node G0", newPrefixTreeGenerationG0));
		timeInfos.add(new InfoHolder(41, "Num. of support computations G0", numberOfSupportComputationsG0));
		timeInfos.add(new InfoHolder(42, "Support computational time G0", computaionalSupportTimeG0));

		timeInfos.add(new InfoHolder(43, "Iso Checking Time Inc", prefixTree.getDurationOfIsoChecking()));
		timeInfos.add(
				new InfoHolder(44, "Number Of Iso Checking Request Inc", prefixTree.getNumberOfIsoCheckingRequest()));
		timeInfos.add(new InfoHolder(44, "Number Of Real Iso Checking Inc", prefixTree.getNumberOfRealIsoChecking()));
		timeInfos.add(new InfoHolder(45, "Creation Of New PrefixTree Node Inc",
				prefixTree.getDurationOfNewPrefixTreeGeneration()));
		timeInfos.add(new InfoHolder(46, "Num. of support computations Inc", prefixTree.getNumberOfComputeSupport()));
		timeInfos.add(new InfoHolder(47, "Support computational time Inc", prefixTree.getDurationOfComputeSupport()));

		timeInfos.add(new InfoHolder(48, "Induced G duration", creationOfInducedGraphDuration));
		timeInfos.add(new InfoHolder(49, "Update neighborhood info", updateNeighborhoodDuration));
		timeInfos.add(new InfoHolder(50, "Update BitMap Duration", updateBitMapTimeDuration));
		timeInfos.add(new InfoHolder(51, "carryOverDuraion", carryOverDuraion));
		timeInfos.add(new InfoHolder(52, "winMineDuraion", winMineDuraion));
		
		TimeLogger.LogTime(
				"IncSub_" + (DummyProperties.windowMode ? "Win" : "Inc") + "_" + DummyProperties.WINDOW_SIZE + ".txt",
				true, timeInfos);

		tx1.success();
		tx1.close();

		dataGraph.shutdown();
	}

	public SnapshotTopkMonitoringSubOpt(String[] args) throws Exception {
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
			} else if (args[i].equals("-edgeStreamCacheCapacity")) {
				edgeStreamCacheCapacity = Integer.parseInt(args[++i]);
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

	public SnapshotTopkMonitoringSubOpt(String focusSetPath, int maxHops, int maxEdges, String dataGraphPath,
			boolean debugMode, int k, double threshold, String deltaEFileOrFiles, int numberOfTransactionInASnapshot,
			int numberOfSnapshots, String dateFormat, int numberOfIgnoranceInitEdges, int edgeStreamCacheCapacity,
			boolean windowMode, int windowSizeL) {

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
		this.edgeStreamCacheCapacity = edgeStreamCacheCapacity;
		this.windowMode = windowMode;
		DummyProperties.windowMode = windowMode;
		this.windowSizeL = windowSizeL;
		DummyProperties.WINDOW_SIZE = windowSizeL;
		this.endingWindow = windowSizeL - 1;

	}

	public static void main(String[] args) throws Exception {
		SnapshotTopkMonitoringSubOpt snapshotTopkMonitoring = new SnapshotTopkMonitoringSubOpt(args);
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

	private void findAffectedParentPatternsForInsertAndAddNewUnexpandedMatch(BooleanInside booleanInsideForIns,
			HashSet<PrefixTreeNode<IPrefixTreeNodeData>> affectedPatterns, IPrefixTree prefixTree, int srcNodeId,
			int destNodeId, RoaringBitmap srcRoaringBitmap, RoaringBitmap destRoaringBitmap) {

		srcRoaringBitmap.forEach(new IntConsumer() {
			@Override
			public void accept(int value) {

				IPrefixTreeNodeData prefixTNodeData = prefixTree.getPrefixTreeNodeIndex().get(value).getData();
				if (prefixTNodeData.getPatternGraph().edgeSet().size() < maxAllowedEdges) {
					for (PatternNode patternNode : prefixTNodeData.getMatchedNodes().getPatternNodeOfNeo4jNode()
							.get(srcNodeId)) {

						if (prefixTNodeData.getStepsFromRootOfPatternNodes().get(patternNode) < maxAllowedHops) {
							affectedPatterns.add(prefixTree.getPrefixTreeNodeIndex().get(value));
							// we know that this srcNodeId existed before, we
							// just want to make it as newUnexpanded one;

							if (prefixTNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes() == null) {
								prefixTNodeData.renewNewUnexpandedPatternsNodesOfNeo4jNodes();
							}

							prefixTNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes().putIfAbsent(patternNode,
									new HashSet<Integer>());
							prefixTNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes().get(patternNode).add(srcNodeId);

							if (prefixTNodeData.getNewUnexpandedNodesOfPatternNodes() == null) {
								prefixTNodeData.renewNewUnexpandedNodesOfPatternNodes();
							}

							prefixTNodeData.getNewUnexpandedNodesOfPatternNodes().putIfAbsent(srcNodeId,
									new HashSet<PatternNode>());
							prefixTNodeData.getNewUnexpandedNodesOfPatternNodes().get(srcNodeId).add(patternNode);

							prefixTNodeData.setPrefixTreeMode(PrefixTreeMode.UPDATE);

							booleanInsideForIns.isFound = true;

						}
					}
				}

			}

		});
	}

	private void findMinStepSrcPatternNodeForInsert(IPrefixTree prefixTree, int srcNodeId, int destNodeId,
			RoaringBitmap srcRoaringBitmap, RoaringBitmap destRoaringBitmap, MinStepHolder minStepHolder) {

		srcRoaringBitmap.forEach(new IntConsumer() {
			@Override
			public void accept(int value) {
				int minSteps = Integer.MAX_VALUE;

				IPrefixTreeNodeData prefixTNodeData = prefixTree.getPrefixTreeNodeIndex().get(value).getData();
				if (prefixTNodeData.getPatternGraph().edgeSet().size() < maxAllowedEdges) {
					for (PatternNode patternNode : prefixTNodeData.getMatchedNodes().getPatternNodeOfNeo4jNode()
							.get(srcNodeId)) {

						if (prefixTNodeData.getStepsFromRootOfPatternNodes().get(patternNode) < maxAllowedHops) {
							minSteps = Math.min(prefixTNodeData.getStepsFromRootOfPatternNodes().get(patternNode),
									minSteps);

							if (minSteps == 1) {
								minStepHolder.minStep = 1;
								return;
							}
							break;
						}
					}
				}
				minStepHolder.minStep = minSteps;
			}

		});
	}

	private void findAffectedPatternsForDelete(BooleanInside booleanInsideForDel,
			HashMap<PrefixTreeNode<IPrefixTreeNodeData>, HashSet<SourceRelDestTriple>> deleteAffectedPatternsMap,
			IPrefixTree prefixTree, int srcNodeId, int destNodeId, RoaringBitmap srcRoaringBitmap,
			RoaringBitmap destRoaringBitmap, String relationshipType) {

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
				for (PatternNode srcPatternNode : tempData.getMatchedNodes().getPatternNodeOfNeo4jNode()
						.get(srcNodeId)) {

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
								deleteAffectedPatternsMap.putIfAbsent(prefixTree.getPrefixTreeNodeIndex().get(value),
										new HashSet<SourceRelDestTriple>());

								deleteAffectedPatternsMap.get(prefixTree.getPrefixTreeNodeIndex().get(value))
										.add(new SourceRelDestTriple(srcNodeId, destNodeId, relationshipType));

								booleanInsideForDel.isFound = true;
								// break;
							}
						}

						// if (isFound)
						// break;

					}
				}
			}

		});

	}

	private StreamEdge handlingDataStream(StreamEdge nextStreamEdge, IPrefixTree prefixTree, Node srcNode,
			Node destNode, Integer srcNodeId, Integer destNodeId) {

		// neighborhood indexing by demand for memory usage
		prefixTree.getLabelAdjacencyIndexer().checkForExistenceInNeighborhoodIndex(prefixTree, srcNodeId, destNodeId,
				srcNode, destNode);

		// specific for incSub:
		if (nextStreamEdge.isAdded())
			prefixTree.getLabelAdjacencyIndexer().addEdgeToVF2InducedGraph(dataGraph, srcNodeId, destNodeId);
		else
			prefixTree.getLabelAdjacencyIndexer().removeEdgeFromVF2InducedGraph(dataGraph, srcNodeId, destNodeId);

		String relationship = nextStreamEdge.getRelationshipType();
		// then add/remove new relationship between them
		if (nextStreamEdge.isAdded()) {
			// TODO: different rels between two nodes not supported here
			if (!prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).nextNodeIds
					.contains(destNodeId)) {
				srcNode.createRelationshipTo(destNode, RelationshipType.withName(relationship));
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).incrementOutDegree();
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(destNodeId).incrementInDegree();
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).addNextNode(destNodeId);
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(destNodeId).addPrevNode(srcNodeId);
				prefixTree.getLabelAdjacencyIndexer().relTypeOfSrcAndDest.putIfAbsent(srcNodeId,
						new HashMap<Integer, String>());
				prefixTree.getLabelAdjacencyIndexer().relTypeOfSrcAndDest.get(srcNodeId).put(destNodeId, relationship);
			}
		} else {
			boolean existed = false;
			// for-loop over less degree node
			if (prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos
					.get(srcNodeId).outDegree <= prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos
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
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).decrementOutDegree();
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(destNodeId).decrementInDegree();

				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(srcNodeId).removeNextNode(destNodeId);
				prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.get(destNodeId).removePrevNode(srcNodeId);

				// if
				// (prefixTree.getLabelAdjacencyIndexer().relTypeOfSrcAndDest.get(srcNodeId)
				// != null)
				prefixTree.getLabelAdjacencyIndexer().relTypeOfSrcAndDest.get(srcNodeId).remove(destNodeId);
			}
		}

		return nextStreamEdge;

	}

}

class MinStepHolder {
	public Integer minStep = Integer.MAX_VALUE;
}

class CacheGraphStreamQueueItem {
	public Integer nodeId;
	public Integer hopAway;

	public CacheGraphStreamQueueItem(Integer nodeId, Integer hopAway) {
		this.nodeId = nodeId;
		this.hopAway = hopAway;
	}
}

class BooleanInside {
	boolean isFound = false;
}