package src.utilities;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import edu.stanford.nlp.pipeline.CoreNLPProtos.Relation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy.DummyProperties.SnapOrStat;

/**
 * This program computes the number of nodes with any label, d hops from every
 * node in the graph
 * 
 * @author Shayan Monadjemi
 * @date June 16, 2016
 */
public class Indexer {
	public static final Integer AFTER = 1;
	public static final Integer BEFORE = -1;

	// The structure V*Sigma*d Path of the neo4j graph database
	GraphDatabaseService dataGraph;

	// nodeId => Adj Label, +-, numbers of that label
	public HashMap<Integer, HashMap<String, HashMap<Integer, int[]>>> labelAdjMatrix = new HashMap<>();

	static String path = "/home/shayan/Documents/WSU/Data/offshoreneo4j.data/panama.graphdb";

	// nodeId => label in d-hops => which distinct node ids?
	public HashMap<Integer, HashMap<String, HashMap<Integer, HashSet<Integer>>>> distinctNodesOfDHopsAway = new HashMap<>();

	private Integer dHops;
	private HashMap<String, HashSet<Integer>> focusNodesOfAllTypes;
	public int maxNodeId = 0;

	public int numberOfNodesInGraph0 = 0;

	public HashMap<Integer, NodeInfo> dataGraphNodeInfos = new HashMap<Integer, NodeInfo>();

	public HashMap<Integer, HashMap<Integer, String>> relTypeOfSrcAndDest = new HashMap<Integer, HashMap<Integer, String>>();

	public HashMap<PatternNode, HashSet<Integer>> candidateSetOfAPatternNode = new HashMap<PatternNode, HashSet<Integer>>();

	public HashMap<Integer, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>> prefixTreeNodesOfALevel = new HashMap<Integer, HashSet<PrefixTreeNode<IPrefixTreeNodeData>>>();

	public HashMap<SourceRelDestTypeTriple, CorrespondsOfSrcRelDest> correspondsOfSrcRelDestType = new HashMap<SourceRelDestTypeTriple, CorrespondsOfSrcRelDest>();

	public HashMap<PrefixTreeNode<IPrefixTreeNodeData>, HashMap<PrefixTreeNode<IPrefixTreeNodeData>, SourceRelDestTypeTriple>> parentChildDifference = new HashMap<PrefixTreeNode<IPrefixTreeNodeData>, HashMap<PrefixTreeNode<IPrefixTreeNodeData>, SourceRelDestTypeTriple>>();

	public static HashSet<Integer> usefulStaticNodes = new HashSet<Integer>();

	// private Integer vf2NodeIndex = 0;
	// public SimpleDirectedGraph<SimpleCustomNode, DefaultEdge>
	// inducedGraphForVF2 = new SimpleDirectedGraph<SimpleCustomNode,
	// DefaultEdge>(
	// DefaultEdge.class);

	// public BiMap<Integer, Integer> neo4jNodeOfVf2Index;
	public HashMap<Integer, SimpleCustomNode> simpleNodeOfNeo4jNodeId = new HashMap<Integer, SimpleCustomNode>();
	private String finalDataGraphPath;
	private SnapOrStat snapOrStat;

	// public static HashSet<Integer> weirdNodesSet = new HashSet<Integer>();

	// in the bitmap each bit represents both pattern id and pattern node id
	// here we'll decode it.
	public static HashMap<Integer, Integer> patternIdOfPatternNodeId = new HashMap<Integer, Integer>();
	public static Integer patternNodeIdCounter = 0;

	public Indexer(GraphDatabaseService dataGraph, Integer dHops,
			HashMap<String, HashSet<Integer>> focusNodesOfAllTypes, String finalDataGraphPath, SnapOrStat snapOrStat) {
		this.dataGraph = dataGraph;
		this.dHops = dHops;
		this.focusNodesOfAllTypes = focusNodesOfAllTypes;
		this.finalDataGraphPath = finalDataGraphPath;
		this.snapOrStat = snapOrStat;

		// neo4jNodeOfVf2Index = HashBiMap.create();
	}

	public Indexer(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				path = args[++i];
			} else if (args[i].equals("-dHops")) {
				dHops = Integer.parseInt(args[++i]);
			}
		}

		if (path.equals("")) {
			throw new Exception("-dataGraphPath should be filled with a neo4j graph db path");
		}
		File storeDir = new File(path);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

	}

	public static void main(String[] args) throws Exception {
		Indexer lblAdjIndexer = new Indexer(args);

		// TODO:test shavad

		// lblAdjIndexer.generateLabelAdjMatrix();
		// lblAdjIndexer.printLabelAdjMatrix();
		//
		// lblAdjIndexer.generateDistinctNodesAdjMatrix();
		// lblAdjIndexer.printDistinctNodesAdjMatrix(lblAdjIndexer.dHops);

	}

	private void printDistinctNodesAdjMatrix(Integer maxDHops) {
		System.out.println();
		for (Integer sourceNodeId : distinctNodesOfDHopsAway.keySet()) {
			System.out.print(sourceNodeId + ": " + "{");
			for (String l : distinctNodesOfDHopsAway.get(sourceNodeId).keySet()) {
				System.out.print(l + "=[");
				for (int hop = -maxDHops; hop <= maxDHops; hop++) {
					if (distinctNodesOfDHopsAway.get(sourceNodeId).get(l).containsKey(hop)) {
						System.out.print(hop + "=>(");
						for (Integer adjNodeId : distinctNodesOfDHopsAway.get(sourceNodeId).get(l).get(hop)) {
							System.out.print(adjNodeId + ", ");
						}
						System.out.print("), ");
					}
				}
				System.out.print("], ");
			}
			System.out.println("}");

		}
	}

	public void generateDistinctNodesAdjMatrix(int realHops, String focus) {

		try (Transaction tx1 = dataGraph.beginTx()) {

			HashSet<Integer> allFocusNodes = new HashSet<Integer>();
			for (String label : focusNodesOfAllTypes.keySet()) {
				allFocusNodes.addAll(focusNodesOfAllTypes.get(label));
			}

			findUsefulNodesSet(realHops, dataGraph, allFocusNodes, focus);

			generateRelTypeMap();
			generateDegreeMap();

			distinctNodesOfDHopsAway.clear();
			distinctNodesOfDHopsAway = new HashMap<>();

			// First, load all node ids in the label adjacency matrix
			numberOfNodesInGraph0 = 0;
			for (Node n : dataGraph.getAllNodes()) {
				numberOfNodesInGraph0++;

				int id = (int) n.getId();

				maxNodeId = Math.max(maxNodeId, id);

				// For Performance and memory
				if (!usefulStaticNodes.contains(id))
					continue;

				distinctNodesOfDHopsAway.put(id, new HashMap<>());

			}

			// Use the recursive helper function to compute the neighborhood
			// nodes up to d hops away
			for (Integer nodeId : distinctNodesOfDHopsAway.keySet()) {
				addNextNodesToMatrix(nodeId, nodeId, 1, 0, dataGraph);
				addPreviousNodesToMatrix(nodeId, nodeId, -1, 0, dataGraph);
			}

			// printDistinctNodesAdjMatrix(realHops);
			System.out.println();
			tx1.success();
			tx1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void generateRelTypeMap() {

		for (Relationship rel : dataGraph.getAllRelationships()) {
			int sourceId = (int) rel.getStartNode().getId();
			int destId = (int) rel.getEndNode().getId();

			// For Performance and memory
			if (!usefulStaticNodes.contains(sourceId) && !usefulStaticNodes.contains(destId))
				continue;

			this.relTypeOfSrcAndDest.putIfAbsent(sourceId, new HashMap<Integer, String>());
			this.relTypeOfSrcAndDest.get(sourceId).put(destId, rel.getType().name().intern());

		}

	}

	private void generateDegreeMap() {
		dataGraphNodeInfos.clear();
		//
		dataGraphNodeInfos = new HashMap<Integer, NodeInfo>();
		//
		for (Node node : dataGraph.getAllNodes()) {

			// For Performance and memory
			if (!usefulStaticNodes.contains((int) node.getId())) {
				continue;
			}

			// HashSet<Integer> nextNodeIds = new HashSet<Integer>();
			// for (Relationship r : node.getRelationships(Direction.OUTGOING))
			// {
			// nextNodeIds.add((int) r.getEndNode().getId());
			// }
			//
			// HashSet<Integer> prevNodeIds = new HashSet<Integer>();
			// for (Relationship r : node.getRelationships(Direction.INCOMING))
			// {
			// prevNodeIds.add((int) r.getStartNode().getId());
			// }

			dataGraphNodeInfos.put((int) node.getId(), new NodeInfo(node.getLabels().iterator().next().name(),
					node.getDegree(Direction.INCOMING), node.getDegree(Direction.OUTGOING)));
		}
		// System.out.println();
	}

	private void addPreviousNodesToMatrix(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph2) {
		if (currentHop > maxDHops) {

			// for (Integer cId :
			// this.dataGraphNodeInfos.get(thisId).prevNodeIds) {
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.INCOMING)) {
				Node c = r.getStartNode();
				int cId = (int) c.getId();

				if (Math.abs(maxDHops) > 1)
					addPreviousNodesToMatrix(parentId, cId, maxDHops, currentHop - 1, dataGraph);

				if (usefulStaticNodes.contains(cId)) {
					// if (this.dataGraphNodeInfos.containsKey(cId)) {
					// String label_RelType =
					// this.dataGraphNodeInfos.get(cId).nodeLabel
					// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
					// + this.relTypeOfSrcAndDest.get(cId).get(thisId);
					String label_RelType = c.getLabels().iterator().next().name()
							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
							+ this.relTypeOfSrcAndDest.get(cId).get(thisId);
					label_RelType = label_RelType.intern();
					if (!distinctNodesOfDHopsAway.get(parentId).containsKey(label_RelType)) {
						distinctNodesOfDHopsAway.get(parentId).put(label_RelType,
								new HashMap<Integer, HashSet<Integer>>());
					}
					if (!distinctNodesOfDHopsAway.get(parentId).get(label_RelType).containsKey(currentHop - 1)) {
						distinctNodesOfDHopsAway.get(parentId).get(label_RelType).put(currentHop - 1,
								new HashSet<Integer>());
					}
					distinctNodesOfDHopsAway.get(parentId).get(label_RelType).get(currentHop - 1).add(cId);

					// else {
					// System.out.println();
					// }

				}
			}
		} else
			return;

	}

	private void addNextNodesToMatrix(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph) {

		// recursion base case: as Integer as we haven't
		// exceeded the max number of hops, keep going
		if (currentHop < maxDHops) {
			// Node c;

			// for (Integer cId :
			// this.dataGraphNodeInfos.get(thisId).nextNodeIds) {
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.OUTGOING)) {
				Node c = r.getEndNode();
				int cId = (int) c.getId();

				if (Math.abs(maxDHops) > 1)
					addNextNodesToMatrix(parentId, cId, maxDHops, currentHop + 1, dataGraph);

				if (usefulStaticNodes.contains(cId)) {
					// if (this.dataGraphNodeInfos.containsKey(cId)) {
					// for (Label l : c.getLabels()) {

					// String label_RelType =
					// this.dataGraphNodeInfos.get(cId).nodeLabel
					// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE +
					// relTypeOfSrcAndDest.get(thisId).get(cId);
					String label_RelType = c.getLabels().iterator().next().name()
							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relTypeOfSrcAndDest.get(thisId).get(cId);
					label_RelType = label_RelType.intern();
					if (!distinctNodesOfDHopsAway.get(parentId).containsKey(label_RelType)) {
						distinctNodesOfDHopsAway.get(parentId).put(label_RelType,
								new HashMap<Integer, HashSet<Integer>>());
					}
					if (!distinctNodesOfDHopsAway.get(parentId).get(label_RelType).containsKey(currentHop + 1)) {
						distinctNodesOfDHopsAway.get(parentId).get(label_RelType).put(currentHop + 1,
								new HashSet<Integer>());
					}
					distinctNodesOfDHopsAway.get(parentId).get(label_RelType).get(currentHop + 1).add(cId);
					// }
					// }
					// else {
					// System.out.println();
					// }
				}
			}
		} else
			return;

	}

	/**
	 * This method populated the three dimensional structure with every neighbor
	 * label for every node, d hops away from the node
	 * 
	 * @param d
	 *            the number of MAXIMUM hops (edges) from the parent node to go.
	 *            (Example: d=3 will calculate 1 hop,2 hops, and 3 hops away.
	 */

	public void generateLabelAdjMatrix() {

		try (Transaction tx1 = dataGraph.beginTx()) {
			// First, load all node ids in the label adjacency matrix
			for (Node n : dataGraph.getAllNodes()) {
				labelAdjMatrix.put((int) n.getId(), new HashMap<>());
			}
			numberOfNodesInGraph0 = labelAdjMatrix.size();

			// Use the recursive helper function to compute the neighborhood
			// labels
			// up to d hops away
			for (Integer nodeId : labelAdjMatrix.keySet()) {
				addNextLabelsToMatrix(nodeId, nodeId, dHops, 0, dataGraph);
				addPreviousLabelsToMatrix(nodeId, nodeId, -dHops, 0, dataGraph);
			}
			tx1.success();
		} catch (Exception e) {

		}

	}

	private void addPreviousLabelsToMatrix(Integer parentid, Integer thisid, int maxdHops, int currentHop,
			GraphDatabaseService gdb) {
		// recursion base case: as Integer as we
		// haven't
		if (currentHop > maxdHops) // recursion base case: as Integer as we
									// haven't
		// exceeded
		// the
		// max number of hops, keep going
		{
			Node c;
			for (Relationship r : gdb.getNodeById(thisid).getRelationships(Direction.INCOMING)) {
				c = r.getStartNode();

				addPreviousLabelsToMatrix(parentid, (int) c.getId(), maxdHops, currentHop - 1, gdb);
				for (Label l : c.getLabels()) {
					if (labelAdjMatrix.get(parentid).containsKey(l.name())) {
						if (!labelAdjMatrix.get(parentid).get(l.name()).containsKey(-1)) {
							labelAdjMatrix.get(parentid).get(l.name()).put(-1, new int[-maxdHops + 1]);
						}
					} else {
						labelAdjMatrix.get(parentid).put(l.name(), new HashMap<Integer, int[]>());
						labelAdjMatrix.get(parentid).get(l.name()).put(-1, new int[-maxdHops + 1]);
					}

					labelAdjMatrix.get(parentid).get(l.name()).get(-1)[-currentHop + 1]++;
				}
			}
		} else
			return;
	}

	/**
	 * This recursive method computes the number of neighbor nodes with every
	 * label
	 * 
	 * @param parentid
	 *            the starting node's id from which we are counting hops (where
	 *            hop = 0)
	 * @param thisid
	 *            the current node's id, whose children we'll be processing
	 * @param dhop
	 *            the number of maximum hops
	 * @param h
	 *            the number of hops away from parent (starting) node
	 * @param gdb
	 *            the neo4j graph database
	 */
	public void addNextLabelsToMatrix(Integer parentid, Integer thisid, int maxdHops, int currentHop,
			GraphDatabaseService gdb) {
		if (currentHop < maxdHops) // recursion base case: as Integer as we
									// haven't
									// exceeded
		// the
		// max number of hops, keep going
		{
			Node c;
			try (Transaction tx1 = gdb.beginTx()) {

				for (Relationship r : gdb.getNodeById(thisid).getRelationships(Direction.OUTGOING)) {
					c = r.getEndNode();

					addNextLabelsToMatrix(parentid, (int) c.getId(), maxdHops, currentHop + 1, gdb);
					for (Label l : c.getLabels()) {
						if (labelAdjMatrix.get(parentid).containsKey(l.name())) {
							if (!labelAdjMatrix.get(parentid).get(l.name()).containsKey(+1)) {
								labelAdjMatrix.get(parentid).get(l.name()).put(+1, new int[maxdHops + 1]);
							}
						} else {
							labelAdjMatrix.get(parentid).put(l.name(), new HashMap<Integer, int[]>());
							labelAdjMatrix.get(parentid).get(l.name()).put(+1, new int[maxdHops + 1]);
						}

						labelAdjMatrix.get(parentid).get(l.name()).get(+1)[currentHop + 1]++;
					}

				}
			} catch (Exception e) {

			}
		} else
			return;
	}

	/**
	 * This method prints our neighborhood label adjacency matrix
	 */
	public void printLabelAdjMatrix() {
		for (Integer id : labelAdjMatrix.keySet()) {
			System.out.print(id + ": " + "{");
			for (String l : labelAdjMatrix.get(id).keySet()) {
				System.out.print(l + "= [");
				if (labelAdjMatrix.get(id).get(l) != null) {
					if (labelAdjMatrix.get(id).get(l).get(-1) != null) {
						for (int i = 1; i < labelAdjMatrix.get(id).get(l).get(-1).length; i++) {
							System.out.print(-i + ":" + labelAdjMatrix.get(id).get(l).get(-1)[i] + " ");
						}
					}
					if (labelAdjMatrix.get(id).get(l).get(+1) != null) {
						for (int i = 1; i < labelAdjMatrix.get(id).get(l).get(+1).length; i++) {
							System.out.print(i + ":" + labelAdjMatrix.get(id).get(l).get(+1)[i] + " ");
						}
					}
				}
				System.out.print("], ");
			}
			System.out.println("}");

		}
	}

	// public HashSet<Integer> getNextIds(Integer srcId) {
	// // HashSet<Integer> nextIdsSet = new HashSet<Integer>();
	// // for (String lbl : distinctNodesOfDHopsAway.get(srcId).keySet()) {
	// //
	// nextIdsSet.addAll(distinctNodesOfDHopsAway.get(srcId).get(lbl).get(AFTER));
	// // }
	// return this.dataGraphNodeInfos.get(srcId).nextNodeIds;
	// }

	// public HashSet<Integer> getPrevIds(Integer srcId) {
	// return this.dataGraphNodeInfos.get(srcId).prevNodeIds;
	// }

	// public void addNewNode(Node node) {
	// distinctNodesOfDHopsAway.put((int) node.getId(), new HashMap<String,
	// HashMap<Integer, HashSet<Integer>>>());
	// }

	// public void updateNeighborhood(GraphDatabaseService dataGraph, Integer
	// srcNodeId, Integer destNodeId,
	// int maxAllowedHops, boolean isAdded) {
	//
	// // TODO: could be faster if we used previous indexing information
	//
	// if (!isAdded) {
	// this.distinctNodesOfDHopsAway.get(srcNodeId).clear();
	// this.distinctNodesOfDHopsAway.get(destNodeId).clear();
	// }
	//
	// // just next of src and its previous nodes would be changed.
	// addNextNodesToMatrix(srcNodeId, srcNodeId, 1, 0, dataGraph);
	//
	//
	// // just previous of dest and its next nodes would be changed.
	// addPreviousNodesToMatrix(destNodeId, destNodeId, -1, 0, dataGraph);
	//
	// }

	public void updateNeighborhood(GraphDatabaseService dataGraph, Integer srcNodeId, Integer destNodeId,
			String relationshipType, int maxAllowedHops, boolean isAdded) {

		if (isAdded) {

			// generating the neighborhood index by demand:
			if (!distinctNodesOfDHopsAway.containsKey(srcNodeId)) {
				distinctNodesOfDHopsAway.put(srcNodeId, new HashMap<String, HashMap<Integer, HashSet<Integer>>>());
			}

			if (!distinctNodesOfDHopsAway.containsKey(destNodeId)) {
				distinctNodesOfDHopsAway.put(destNodeId, new HashMap<String, HashMap<Integer, HashSet<Integer>>>());
			}

			// for (Integer cId :
			// this.dataGraphNodeInfos.get(srcNodeId).nextNodeIds) {

			// NEXT
			String label_RelType = dataGraph.getNodeById(destNodeId).getLabels().iterator().next().name()
					// this.dataGraphNodeInfos.get(destNodeId).nodeLabel
					+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType;
			label_RelType = label_RelType.intern();

			if (!distinctNodesOfDHopsAway.get(srcNodeId).containsKey(label_RelType)) {
				distinctNodesOfDHopsAway.get(srcNodeId).put(label_RelType, new HashMap<Integer, HashSet<Integer>>());
			}
			if (!distinctNodesOfDHopsAway.get(srcNodeId).get(label_RelType).containsKey(AFTER)) {
				distinctNodesOfDHopsAway.get(srcNodeId).get(label_RelType).put(AFTER, new HashSet<Integer>());
			}
			distinctNodesOfDHopsAway.get(srcNodeId).get(label_RelType).get(AFTER).add(destNodeId);

			// Previous
			label_RelType = dataGraph.getNodeById(srcNodeId).getLabels().iterator().next().name()
					// this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
					+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType;
			label_RelType = label_RelType.intern();
			if (!distinctNodesOfDHopsAway.get(destNodeId).containsKey(label_RelType)) {
				distinctNodesOfDHopsAway.get(destNodeId).put(label_RelType, new HashMap<Integer, HashSet<Integer>>());
			}
			if (!distinctNodesOfDHopsAway.get(destNodeId).get(label_RelType).containsKey(BEFORE)) {
				distinctNodesOfDHopsAway.get(destNodeId).get(label_RelType).put(BEFORE, new HashSet<Integer>());
			}
			distinctNodesOfDHopsAway.get(destNodeId).get(label_RelType).get(BEFORE).add(srcNodeId);

		} else {

			// NEXT

			if (distinctNodesOfDHopsAway.containsKey(srcNodeId)
					&& distinctNodesOfDHopsAway.get(srcNodeId).get(// this.dataGraphNodeInfos.get(destNodeId).nodeLabel
							dataGraph.getNodeById(destNodeId).getLabels().iterator().next().name()
									+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType) != null
					&& distinctNodesOfDHopsAway.get(srcNodeId)
							.get(// this.dataGraphNodeInfos.get(destNodeId).nodeLabel
									dataGraph.getNodeById(destNodeId).getLabels().iterator().next().name()
											+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
							.get(AFTER) != null) {
				distinctNodesOfDHopsAway.get(srcNodeId)
						.get(// this.dataGraphNodeInfos.get(destNodeId).nodeLabel
								dataGraph.getNodeById(destNodeId).getLabels().iterator().next().name()
										+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
						.get(AFTER).remove(destNodeId);
			}

			// Previous
			if (distinctNodesOfDHopsAway.containsKey(destNodeId)
					&& distinctNodesOfDHopsAway.get(destNodeId).get(// this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
							dataGraph.getNodeById(srcNodeId).getLabels().iterator().next().name()
									+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType) != null
					&& distinctNodesOfDHopsAway.get(destNodeId)
							.get(// this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
									dataGraph.getNodeById(srcNodeId).getLabels().iterator().next().name()
											+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
							.get(BEFORE) != null) {
				distinctNodesOfDHopsAway.get(destNodeId)
						.get(// this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
								dataGraph.getNodeById(srcNodeId).getLabels().iterator().next().name()
										+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
						.get(BEFORE).remove(srcNodeId);
			}

		}
	}

	public void findUsefulNodesSet(int dHops, GraphDatabaseService g0Graph, HashSet<Integer> allFocusNodes,
			String focus) throws Exception {

		// TraversalDescription graphTraverse =
		// g0Graph.traversalDescription().uniqueness(Uniqueness.NODE_GLOBAL)
		// .evaluator(Evaluators.fromDepth(-dHops -
		// 1)).evaluator(Evaluators.toDepth(dHops + 1));

		// "Paper|Title:recommendation,Venue_DM"
		// "PaperTitlerecommendationVenue_DM"
		String replacedFocus = focus.replace("|", "").replace(":", "").replace(",", "");
		String filePath = "usefulNodeIndex/" + replacedFocus + "_" + dHops + ".txt";
		File indexOfUsefulsNodeFile = new File(filePath);

		if (indexOfUsefulsNodeFile.exists()) {
			Scanner linReader = new Scanner(indexOfUsefulsNodeFile);

			while (linReader.hasNext()) {
				usefulStaticNodes.add(Integer.parseInt(linReader.nextLine()));
			}
			linReader.close();

		} else if (snapOrStat == SnapOrStat.Snap) {

			File storeDir = new File(finalDataGraphPath);
			GraphDatabaseService finalDataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
					.setConfig("cache_type", "none").setConfig(GraphDatabaseSettings.pagecache_memory, "245760")
					.newGraphDatabase();

			// DummyFunctions.registerShutdownHook(finalDataGraph);

			for (Integer nodeId : allFocusNodes) {
				usefulStaticNodes.add(nodeId);
			}
			HashSet<Integer> allNodesInTwoHopsSet = new HashSet<>();
			int allNodesInTwoHops = 0;
			Transaction tx100 = finalDataGraph.beginTx();
			for (Integer nodeId : allFocusNodes) {
				TraversalDescription twoHopsTraversal = finalDataGraph.traversalDescription().breadthFirst()
						.uniqueness(Uniqueness.NODE_GLOBAL).evaluator(Evaluators.toDepth(dHops));

				for (Node currentNode : twoHopsTraversal.traverse(finalDataGraph.getNodeById(nodeId)).nodes()) {
					allNodesInTwoHopsSet.add((int) currentNode.getId());
					allNodesInTwoHops++;
				}
			}

			usefulStaticNodes.addAll(allNodesInTwoHopsSet);

			// System.out.println("usefulStaticNodes:");
			// for (Integer nodeId : usefulStaticNodes) {
			// System.out.println(nodeId);
			// }
			// System.out.println();

			// HashSet<Long> relsSet = new HashSet<Long>();
			// int affectedRels = 0;
			// for (Relationship rel : finalDataGraph.getAllRelationships()) {
			// if (usefulStaticNodes.contains((int) rel.getStartNode().getId())
			// && usefulStaticNodes.contains((int) rel.getEndNode().getId())) {
			// affectedRels++;
			// relsSet.add(rel.getId());
			// }
			// }

			tx100.success();
			finalDataGraph.shutdown();

			if (DummyProperties.debugMode) {
				System.out.println("allFocusNodes: " + allFocusNodes.size());
				System.out.println("allNodesInTwoHopsSet: " + allNodesInTwoHopsSet.size());
				System.out.println("allNodesInTwoHops: " + allNodesInTwoHops);
				// System.out.println("affectedRels: " + affectedRels);
				// System.out.println("relsSet: " + relsSet.size());

			}
			// relsSet.clear();
			// relsSet = null;
			allNodesInTwoHopsSet.clear();
			allNodesInTwoHopsSet = null;

			System.gc();
			System.runFinalization();

			BufferedWriter out = new BufferedWriter(new FileWriter(filePath), 32768);
			for (Integer usefulNodeId : usefulStaticNodes) {
				out.write(usefulNodeId.toString() + "\n");
			}
			out.close();

		} else if (snapOrStat == SnapOrStat.Stat) {

			for (Integer nodeId : allFocusNodes) {
				usefulStaticNodes.add(nodeId);
			}
			HashSet<Integer> allNodesInTwoHopsSet = new HashSet<>();
			int allNodesInTwoHops = 0;

			for (Integer nodeId : allFocusNodes) {
				TraversalDescription twoHopsTraversal = dataGraph.traversalDescription().breadthFirst()
						.uniqueness(Uniqueness.NODE_GLOBAL).evaluator(Evaluators.toDepth(dHops));

				for (Node currentNode : twoHopsTraversal.traverse(dataGraph.getNodeById(nodeId)).nodes()) {
					allNodesInTwoHopsSet.add((int) currentNode.getId());
					allNodesInTwoHops++;
				}
			}

			usefulStaticNodes.addAll(allNodesInTwoHopsSet);

			if (DummyProperties.debugMode) {
				System.out.println("allFocusNodes: " + allFocusNodes.size());
				System.out.println("allNodesInTwoHopsSet: " + allNodesInTwoHopsSet.size());
				System.out.println("allNodesInTwoHops: " + allNodesInTwoHops);
				// System.out.println("affectedRels: " + affectedRels);
				// System.out.println("relsSet: " + relsSet.size());

			}
			// relsSet.clear();
			// relsSet = null;
			allNodesInTwoHopsSet.clear();
			allNodesInTwoHopsSet = null;

			System.gc();
			System.runFinalization();

			BufferedWriter out = new BufferedWriter(new FileWriter("usefulNodeIndex/" + replacedFocus + ".txt"), 32768);
			for (Integer usefulNodeId : usefulStaticNodes) {
				out.write(usefulNodeId.toString() + "\n");
			}
			out.close();

		}

	}

	private void addPreviousNodesSet(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph) {
		if (currentHop > maxDHops) {
			Node c;
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.INCOMING)) {
				c = r.getStartNode();
				int cId = (int) c.getId();

				if (Math.abs(maxDHops) > 1)
					addPreviousNodesSet(parentId, cId, maxDHops, currentHop - 1, dataGraph);

				usefulStaticNodes.add(cId);

			}

		} else
			return;

	}

	private void addNextNodesSet(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph) {
		// recursion base case: as Integer as we haven't
		// exceeded the max number of hops, keep going
		if (currentHop < maxDHops) {
			Node c;
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.OUTGOING)) {
				c = r.getEndNode();
				int cId = (int) c.getId();
				if (Math.abs(maxDHops) > 1)
					addNextNodesSet(parentId, cId, maxDHops, currentHop + 1, dataGraph);

				usefulStaticNodes.add(cId);
			}
		} else
			return;

	}

	// public void checkForExistenceInNeighborhoodIndex(IPrefixTree prefixTree,
	// int srcNodeId, int destNodeId,
	// Node srcNode, Node destNode) {
	// // neighborhood indexing by demand for memory usage
	// if
	// (!prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.containsKey(srcNodeId))
	// {
	// HashSet<Integer> nextNodeIds = new HashSet<Integer>();
	// for (Relationship r : srcNode.getRelationships(Direction.OUTGOING)) {
	// nextNodeIds.add((int) r.getEndNode().getId());
	// }
	//
	// HashSet<Integer> prevNodeIds = new HashSet<Integer>();
	// for (Relationship r : srcNode.getRelationships(Direction.INCOMING)) {
	// prevNodeIds.add((int) r.getStartNode().getId());
	// }
	//
	// dataGraphNodeInfos.put((int) srcNode.getId(),
	// new NodeInfo(srcNode.getLabels().iterator().next().name(),
	// srcNode.getDegree(Direction.INCOMING),
	// srcNode.getDegree(Direction.OUTGOING), prevNodeIds, nextNodeIds));
	// }
	//
	// if
	// (!prefixTree.getLabelAdjacencyIndexer().dataGraphNodeInfos.containsKey(destNodeId))
	// {
	// HashSet<Integer> nextNodeIds = new HashSet<Integer>();
	// for (Relationship r : destNode.getRelationships(Direction.OUTGOING)) {
	// nextNodeIds.add((int) r.getEndNode().getId());
	// }
	//
	// HashSet<Integer> prevNodeIds = new HashSet<Integer>();
	// for (Relationship r : destNode.getRelationships(Direction.INCOMING)) {
	// prevNodeIds.add((int) r.getStartNode().getId());
	// }
	//
	// dataGraphNodeInfos.put((int) destNode.getId(),
	// new NodeInfo(destNode.getLabels().iterator().next().name(),
	// destNode.getDegree(Direction.INCOMING),
	// destNode.getDegree(Direction.OUTGOING), prevNodeIds, nextNodeIds));
	// }
	// }

	// public void addEdgeToVF2InducedGraph(Node sourceNode, Node targetNode) {
	//
	// SimpleCustomNode sSrcNode = new SimpleCustomNode(sourceNode);
	// SimpleCustomNode sTargetNode = new SimpleCustomNode(targetNode);
	//
	// if (this.inducedGraphForVF2.addVertex(sSrcNode)) {
	// simpleNodeOfNeo4jNodeId.put((int) sSrcNode.neo4jNode.getId(), sSrcNode);
	// neo4jNodeOfVf2Index.put(vf2NodeIndex, (int) sSrcNode.neo4jNode.getId());
	// sSrcNode.updateNodeIndex(vf2NodeIndex++);
	//
	// }
	//
	// if (this.inducedGraphForVF2.addVertex(sTargetNode)) {
	// simpleNodeOfNeo4jNodeId.put((int) sTargetNode.neo4jNode.getId(),
	// sTargetNode);
	// neo4jNodeOfVf2Index.put(vf2NodeIndex, (int)
	// sTargetNode.neo4jNode.getId());
	// sTargetNode.updateNodeIndex(vf2NodeIndex++);
	// }
	// this.inducedGraphForVF2.addEdge(sSrcNode, sTargetNode);
	//
	// }
	//
	// public void addEdgeToVF2InducedGraph(GraphDatabaseService dataGraph,
	// Integer srcNodeId, Integer destNodeId) {
	// addEdgeToVF2InducedGraph(dataGraph.getNodeById(srcNodeId),
	// dataGraph.getNodeById(destNodeId));
	// }
	//
	// public void removeEdgeFromVF2InducedGraph(GraphDatabaseService dataGraph,
	// Integer srcNodeId, Integer destNodeId) {
	// this.inducedGraphForVF2.removeEdge(simpleNodeOfNeo4jNodeId.get(srcNodeId),
	// simpleNodeOfNeo4jNodeId.get(destNodeId));
	// }
	//
	// public void addFocusAsVertices(GraphDatabaseService dataGraph,
	// HashSet<Integer> allFocusNodes) {
	//
	// for (Integer neo4jNodeId : allFocusNodes) {
	// SimpleCustomNode focusNode = new
	// SimpleCustomNode(dataGraph.getNodeById(neo4jNodeId));
	// this.inducedGraphForVF2.addVertex(focusNode);
	// simpleNodeOfNeo4jNodeId.put((int) focusNode.neo4jNode.getId(),
	// focusNode);
	// neo4jNodeOfVf2Index.put(vf2NodeIndex, (int) focusNode.neo4jNode.getId());
	// focusNode.updateNodeIndex(vf2NodeIndex++);
	// }
	//
	// }

}
