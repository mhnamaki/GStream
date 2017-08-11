package src.IsomorphismTest;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import com.google.common.collect.BiMap;

import src.utilities.DefaultLabeledEdge;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Indexer;
import src.utilities.PatternNode;
import src.utilities.SimpleCustomNode;

public class VF2StateInduced {
	public int[] core_1; // stores for each model graph node to which pattern
							// graph node it maps ("-1" indicates no mapping)
	public int[] core_2; // stores for each pattern graph node to which model
							// graph node it maps ("-1" indicates no mapping)

	public int[] in_1; // stores for each model graph node the depth in the
						// search tree at which it entered "T_1 in" or the
						// mapping ("-1" indicates that the node is not part of
						// the set)
	public int[] in_2; // stores for each pattern graph node the depth in the
						// search tree at which it entered "T_2 in" or the
						// mapping ("-1" indicates that the node is not part of
						// the set)
	public int[] out_1; // stores for each model graph node the depth in the
						// search tree at which it entered "T_1 out" or the
						// mapping ("-1" indicates that the node is not part of
						// the set)
	public int[] out_2; // stores for each pattern graph node the depth in the
						// search tree at which it entered "T_2 out" or the
						// mapping ("-1" indicates that the node is not part of
						// the set)

	// sets mentioned in the paper
	public HashSet<Integer> T1in;
	public HashSet<Integer> T1out;
	public HashSet<Integer> T2in;
	public HashSet<Integer> T2out;

	// sets storing yet unmapped nodes
	public HashSet<Integer> unmapped1;
	public HashSet<Integer> unmapped2;

	public int depth = 0; // current depth of the search tree

	// result processing
	// public int[][] resultMatrix;
	public HashMap<PatternNode, HashSet<Integer>> resultMap;
	public Vector<Integer> nodeResult;
	public int matchNumber;

	public SimpleDirectedGraph<SimpleCustomNode, DefaultEdge> modelGraph;
	public DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph;
	public BiMap<PatternNode, Integer> patternGraphIdMap;

	int modelSize = 0;
	int patternSize = 0;

	Indexer indexer;

	public void reinit() {
		depth = 0;
		// matchNumber = 0;
		T1in.clear();
		T1out.clear();
		T2in.clear();
		T2out.clear();

		unmapped1.clear();
		unmapped2.clear();

		nodeResult.clear();

		// initialize values ("-1" means no mapping / not contained in the set)
		// initially, all sets are empty and no nodes are mapped
		for (int i = 0; i < modelSize; i++) {
			core_1[i] = -1;
			in_1[i] = -1;
			out_1[i] = -1;
			unmapped1.add(i);
		}
		for (int i = 0; i < patternSize; i++) {
			core_2[i] = -1;
			in_2[i] = -1;
			out_2[i] = -1;
			unmapped2.add(i);
		}
	}

	// initializes a new state
	public VF2StateInduced(SimpleDirectedGraph<SimpleCustomNode, DefaultEdge> modelGraph,
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph2, BiMap<PatternNode, Integer> patternGraphIdMap,
			Indexer indexer) {

		resultMap = new HashMap<PatternNode, HashSet<Integer>>();

		this.modelGraph = modelGraph;
		this.patternGraph = patternGraph2;
		this.patternGraphIdMap = patternGraphIdMap;

		this.indexer = indexer;

		modelSize = modelGraph.vertexSet().size();
		// for (Node n : modelGraph.getAllNodes()) {
		// modelSize++;
		// }

		modelSize = (int) (modelSize * 1.001);

		patternSize = patternGraph2.vertexSet().size();

		T1in = new HashSet<Integer>(modelSize * 2);
		T1out = new HashSet<Integer>(modelSize * 2);
		T2in = new HashSet<Integer>(modelSize * 2);
		T2out = new HashSet<Integer>(modelSize * 2);

		unmapped1 = new HashSet<Integer>(modelSize * 2);
		unmapped2 = new HashSet<Integer>(patternSize * 2);

		core_1 = new int[modelSize];
		core_2 = new int[patternSize];

		in_1 = new int[modelSize];
		in_2 = new int[patternSize];
		out_1 = new int[modelSize];
		out_2 = new int[patternSize];

		// resultMatrix = new int[1000][patternSize];
		nodeResult = new Vector<Integer>();
		matchNumber = 0;
		// initialize values ("-1" means no mapping / not contained in the set)
		// initially, all sets are empty and no nodes are mapped
		for (int i = 0; i < modelSize; i++) {
			core_1[i] = -1;
			in_1[i] = -1;
			out_1[i] = -1;
			unmapped1.add(i);
		}
		for (int i = 0; i < patternSize; i++) {
			core_2[i] = -1;
			in_2[i] = -1;
			out_2[i] = -1;
			unmapped2.add(i);
		}

		// namaki added
		// for (int i = 0; i < 1000; i++) {
		// for (int j = 0; j < patternSize; j++) {
		// resultMatrix[i][j] = -1;
		// }
		// }

		for (PatternNode patternNode : patternGraphIdMap.keySet()) {
			resultMap.put(patternNode, new HashSet<Integer>());
		}

	}

	public String getSetContent() {
		String s = "";
		s += "T1in:  " + setContent(T1in) + "\n";
		s += "T1out: " + setContent(T1out) + "\n";
		s += "T2in:  " + setContent(T2in) + "\n";
		s += "T2out: " + setContent(T2out) + "\n";
		s += "un1:   " + setContent(unmapped1) + "\n";
		s += "un2:   " + setContent(unmapped2) + "\n";
		return s;
	}

	// temporary methods
	private String setContent(Collection<Integer> c) {
		String s = "";
		for (Integer i : c)
			s += i + " ";
		return s;
	}

	// set membership tests
	public Boolean inM1(int nodeId) {
		return (core_1[nodeId] > -1);
	}

	public Boolean inM2(int nodeId) {
		return (core_2[nodeId] > -1);
	}

	public Boolean inT1in(int nodeId) {
		return ((core_1[nodeId] == -1) && (in_1[nodeId] > -1));
	}

	public Boolean inT2in(int nodeId) {
		return ((core_2[nodeId] == -1) && (in_2[nodeId] > -1));
	}

	public Boolean inT1out(int nodeId) {
		return ((core_1[nodeId] == -1) && (out_1[nodeId] > -1));
	}

	public Boolean inT2out(int nodeId) {
		return ((core_2[nodeId] == -1) && (out_2[nodeId] > -1));
	}

	public Boolean inT1(int nodeId) {
		return (this.inT1in(nodeId) || this.inT1out(nodeId));
	}

	public Boolean inT2(int nodeId) {
		return (this.inT2in(nodeId) || this.inT2out(nodeId));
	}

	public Boolean inN1tilde(int nodeId) {
		return ((core_1[nodeId] == -1) && (in_1[nodeId] == -1) && (out_1[nodeId] == -1));
	}

	public Boolean inN2tilde(int nodeId) {
		return ((core_2[nodeId] == -1) && (in_2[nodeId] == -1) && (out_2[nodeId] == -1));
	}

	// extends the current matching by the pair (n,m) -> going down one level in
	// the search tree
	// n is the id of a model graph node
	// m is the id of a pattern graph node
	public void match(int n, int m) {
		// include pair (n,m) into the mapping
		core_1[n] = m;
		core_2[m] = n;
		unmapped1.remove(n);
		unmapped2.remove(m);

		T1in.remove(n);
		T1out.remove(n);
		T2in.remove(m);
		T2out.remove(m);

		depth++;// increase depth (we moved down one level in the search tree)

		// update in/out arrays
		// updates needed for nodes entering Tin/Tout sets on this level
		// no updates needed for nodes which entered these sets before

		// Node nTmp = modelGraph.getNodeById(n);
		PatternNode mTmp = patternGraphIdMap.inverse().get(m);

		// Node nTmp = getNodeByProperty(modelGraph, n);
		// Node mTmp = getNodeByProperty(patternGraph, m);

		// cycle through nodes pointing towards n
		// for (Relationship e : nTmp.getRelationships(Direction.INCOMING)) {
		// if (in_1[(int) e.getStartNode().getId()] == -1) {
		// in_1[(int) e.getStartNode().getId()] = depth; // update
		// // in_1
		// if (!inM1((int) e.getStartNode().getId()))
		// T1in.add((int) e.getStartNode().getId()); // update
		// // T1in
		// }
		// }
		if (!indexer.dataGraphNodeInfos.get(indexer.neo4jNodeOfVf2Index.get(n)).prevNodeIds.isEmpty()) {
			for (Integer startNeo4jNodeId : indexer.dataGraphNodeInfos
					.get(indexer.neo4jNodeOfVf2Index.get(n)).prevNodeIds) {

				//TODO: it was a patch
				if (indexer.neo4jNodeOfVf2Index.inverse().get(startNeo4jNodeId) == null)
					continue;

				int startNodeId = indexer.neo4jNodeOfVf2Index.inverse().get(startNeo4jNodeId);
				if (in_1[startNodeId] == -1) {
					in_1[startNodeId] = depth; // update
												// in_1
					if (!inM1(startNodeId))
						T1in.add(startNodeId); // update
												// T1in
				}
			}
		}

		// cycle through nodes n points to
		// for (Relationship e : nTmp.getRelationships(Direction.OUTGOING)) {
		// if (out_1[(int) e.getEndNode().getId()] == -1) {
		// out_1[(int) e.getEndNode().getId()] = depth; // update
		// // out_1
		// if (!inM1((int) e.getEndNode().getId()))
		// T1out.add((int) e.getEndNode().getId()); // update
		// // T1out
		// }
		// }
		if (!indexer.dataGraphNodeInfos.get(indexer.neo4jNodeOfVf2Index.get(n)).nextNodeIds.isEmpty()) {
			for (Integer endNeo4jNodeId : indexer.dataGraphNodeInfos
					.get(indexer.neo4jNodeOfVf2Index.get(n)).nextNodeIds) {
				
				//TODO: it was a patch
				if (indexer.neo4jNodeOfVf2Index.inverse().get(endNeo4jNodeId) == null)
					continue;
				
				int endNodeId = indexer.neo4jNodeOfVf2Index.inverse().get(endNeo4jNodeId);
				if (out_1[endNodeId] == -1) {
					out_1[endNodeId] = depth; // update
												// out_1
					if (!inM1((int) endNodeId))
						T1out.add((int) endNodeId); // update
													// T1out
				}
			}
		}

		// cycle through nodes pointing towards m
		// for (Relationship e : mTmp.getRelationships(Direction.INCOMING)) {
		// if (in_2[(int) e.getStartNode().getId()] == -1) {
		// in_2[(int) e.getStartNode().getId()] = depth; // update
		// // in_2
		// if (!inM2((int) e.getStartNode().getId()))
		// T2in.add((int) e.getStartNode().getId()); // update
		// // T2in
		// }
		// }

		for (DefaultLabeledEdge e : patternGraph.incomingEdgesOf(mTmp)) {
			if (in_2[(int) patternGraphIdMap.get(patternGraph.getEdgeSource(e))] == -1) {
				in_2[(int) patternGraphIdMap.get(patternGraph.getEdgeSource(e))] = depth; // update
				// in_2
				if (!inM2((int) patternGraphIdMap.get(patternGraph.getEdgeSource(e))))
					T2in.add((int) patternGraphIdMap.get(patternGraph.getEdgeSource(e))); // update
				// T2in
			}
		}

		// cycle through nodes m points to
		// for (Relationship e : mTmp.getRelationships(Direction.OUTGOING)) {
		// if (out_2[(int) e.getEndNode().getId()] == -1) {
		// out_2[(int) e.getEndNode().getId()] = depth; // update
		// // out_2
		// if (!inM2((int) e.getEndNode().getId()))
		// T2out.add((int) e.getEndNode().getId()); // update
		// // T2out
		// }
		// }

		for (DefaultLabeledEdge e : patternGraph.outgoingEdgesOf(mTmp)) {
			if (out_2[(int) patternGraphIdMap.get(patternGraph.getEdgeTarget(e))] == -1) {
				out_2[(int) patternGraphIdMap.get(patternGraph.getEdgeTarget(e))] = depth; // update
				// out_2
				if (!inM2((int) patternGraphIdMap.get(patternGraph.getEdgeTarget(e))))
					T2out.add((int) patternGraphIdMap.get(patternGraph.getEdgeTarget(e))); // update
				// T2out
			}
		}
	}

	// removes the pair (n,m) from the current mapping -> going up one level in
	// the search tree
	// n is the id of a model graph node
	// m is the id of a pattern graph node
	public void backtrack(int n, int m) {

		// remove mapping for pair (n,m)
		core_1[n] = -1;
		core_2[m] = -1;
		unmapped1.add(n);
		unmapped2.add(m);

		// if any node entered Tin/Tout sets when the mapping was extended by
		// pair (n,m), undo this
		for (int i = 0; i < core_1.length; i++) {
			if (in_1[i] == depth) {
				in_1[i] = -1;
				T1in.remove(i);
			}
			if (out_1[i] == depth) {
				out_1[i] = -1;
				T1out.remove(i);
			}
		}
		for (int i = 0; i < core_2.length; i++) {
			if (in_2[i] == depth) {
				in_2[i] = -1;
				T2in.remove(i);
			}
			if (out_2[i] == depth) {
				out_2[i] = -1;
				T2out.remove(i);
			}
		}

		// put n / m back into Tin and Tout sets if necessary
		if (inT1in(n))
			T1in.add(n);
		if (inT1out(n))
			T1out.add(n);
		if (inT2in(m))
			T2in.add(m);
		if (inT2out(m))
			T2out.add(m);

		depth--; // decrease depth of search tree -> we move up one level
	}

	// keep track of mappings found by the algorithm
	// simply prints these mappings to the console
	// format: ([modelNode],[patternNode]) ...
	public void printMapping() {

		System.out.print("Mapping found: ");
		for (int i = 0; i < core_2.length; i++) {
			System.out.print("(" + core_2[i] + "-" + i + ") ");
		}
		System.out.println();

		for (int i = 0; i < core_2.length; i++) {
			System.out.print("(" + core_2[i] + "-" + patternGraphIdMap.inverse().get(i) + ") ");
		}
		System.out.println();

	}

	public void storeMapping() {
		for (int i = 0; i < core_2.length; i++) {
			// resultMatrix[matchNumber][i] = core_2[i];
			if (!nodeResult.contains(core_2[i]))
				nodeResult.addElement(core_2[i]);

			resultMap.get(patternGraphIdMap.inverse().get(i)).add(core_2[i]);
		}
		matchNumber++;
	}

	// public Node getNodeByProperty(GraphDatabaseService graph, int
	// uniqueManualId) {
	// return graph.getNodeById(DummyProperties.nodeIdByUniqueManualIdMap.get(
	// graph).get(uniqueManualId));
	// }

	// public int getUniqueManualIdByNode(Node nodeItem) {
	// return DummyProperties.uniqueManualIdByNodeIdMap.get(
	// nodeItem.getGraphDatabase()).get(nodeItem.getId());
	// }
}
