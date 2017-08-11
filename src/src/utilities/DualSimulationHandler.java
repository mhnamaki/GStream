package src.utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue; 

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.MinMaxPriorityQueue;

import src.alg1.prefixTree.PrefixTreeAlg1;
import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.dualSimulation.gtar.BatDualSimulation;
import src.utilities.Dummy.DummyProperties;

public class DualSimulationHandler {

	public static boolean isBiDualSimulated(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode2, IPrefixTree prefixTree) {

		double startTime = System.nanoTime();

		if (!preBiSimChecking(p1, prefixTreeNode2.getData().getPatternGraph(), prefixTree)) {
			prefixTree.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
			return false;
		}

		prefixTree.incrementRealBiSimChecking();
		if (isDualSim(p1, prefixTreeNode2, prefixTree) && isDualSim(prefixTreeNode2, p1, prefixTree)) {
			prefixTree.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
			return true;
		}

		prefixTree.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
		return false;
	}

	public static boolean isDualSim(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode2, IPrefixTree prefixTree) {

		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : p1.vertexSet()) {
			for (PatternNode patternNode2 : prefixTreeNode2.getData().getPatternGraph().vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())) {
					HashSet<String> incomingRelTypes = new HashSet<String>();
					for (DefaultLabeledEdge e : p1.incomingEdgesOf(patternNode1)) {
						incomingRelTypes.add(e.getType());
					}

					if (((!prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode2)
							|| prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes().get(patternNode2)
									.size() == 0)
							&& incomingRelTypes.size() == 0)
							|| (prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes().get(patternNode2) != null
									&& incomingRelTypes.equals(prefixTreeNode2.getData()
											.getIncomingRelTypesOfPatternNodes().get(patternNode2).keySet()))) {
						dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
						dsim.get(patternNode1).add(patternNode2);

					}
				}
			}
		}

		if (dsim.size() == p1.vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation.customizedMatchList(p1,
					prefixTreeNode2.getData().getPatternGraph(), dsim, prefixTree);

			if (!result.isEmpty() && result.size() == p1.vertexSet().size()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDualSim(PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode1,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p2, IPrefixTree prefixTree) {

		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : prefixTreeNode1.getData().getPatternGraph().vertexSet()) {
			for (PatternNode patternNode2 : p2.vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())) {
					HashSet<String> incomingRelTypes = new HashSet<String>();
					for (DefaultLabeledEdge e : p2.incomingEdgesOf(patternNode2)) {
						incomingRelTypes.add(e.getType());
					}

					if (((!prefixTreeNode1.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode1)
							|| prefixTreeNode1.getData().getIncomingRelTypesOfPatternNodes().get(patternNode1)
									.size() == 0)
							&& incomingRelTypes.size() == 0)
							|| (prefixTreeNode1.getData().getIncomingRelTypesOfPatternNodes().get(patternNode1) != null
									&& incomingRelTypes.equals(prefixTreeNode1.getData()
											.getIncomingRelTypesOfPatternNodes().get(patternNode1).keySet()))) {
						dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
						dsim.get(patternNode1).add(patternNode2);

					}
				}
			}
		}

		if (dsim.size() == prefixTreeNode1.getData().getPatternGraph().vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation
					.customizedMatchList(prefixTreeNode1.getData().getPatternGraph(), p2, dsim, prefixTree);

			if (!result.isEmpty() && result.size() == prefixTreeNode1.getData().getPatternGraph().vertexSet().size()) {
				return true;
			}
		}
		return false;
	}

	// public static boolean isDualSim(PrefixTreeNode<IPrefixTreeNodeData>
	// prefixTreeNode1,
	// PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode2, IPrefixTree
	// prefixTree) {
	//
	// HashMap<PatternNode, HashSet<PatternNode>> dsim = new
	// HashMap<PatternNode, HashSet<PatternNode>>();
	// for (PatternNode patternNode1 :
	// prefixTreeNode1.getData().getPatternGraph().vertexSet()) {
	// for (PatternNode patternNode2 :
	// prefixTreeNode2.getData().getPatternGraph().vertexSet()) {
	// if (patternNode1.equals(patternNode2)) {
	// dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
	// dsim.get(patternNode1).add(patternNode2);
	// } else if (patternNode1.getType().equals(patternNode2.getType())) {
	// if
	// (prefixTreeNode1.getData().getIncomingRelTypesOfPatternNodes().get(patternNode1).keySet().equals(
	// prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes().get(patternNode2).keySet()))
	// dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
	// dsim.get(patternNode1).add(patternNode2);
	// }
	// }
	// }
	//
	// if (dsim.size() == p1.vertexSet().size()) {
	// HashMap<PatternNode, HashSet<PatternNode>> result =
	// BatDualSimulation.customizedMatchList(p1, p2, dsim,
	// prefixTree);
	//
	// if (!result.isEmpty() && result.size() == p1.vertexSet().size()) {
	// return true;
	// }
	// }
	// return false;
	// }

	public static HashMap<PatternNode, HashSet<PatternNode>> getDualSimIfAny(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode2, IPrefixTree prefixTree) {
		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : p1.vertexSet()) {
			for (PatternNode patternNode2 : prefixTreeNode2.getData().getPatternGraph().vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())) {
					HashSet<String> incomingRelTypes = new HashSet<String>();
					for (DefaultLabeledEdge e : p1.incomingEdgesOf(patternNode1)) {
						incomingRelTypes.add(e.getType());
					}

					if (!prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode2)
							&& incomingRelTypes.size() > 0) {
						// not a match
					} else if ((!prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode2)
							&& incomingRelTypes.size() == 0)
							|| incomingRelTypes.equals(prefixTreeNode2.getData().getIncomingRelTypesOfPatternNodes()
									.get(patternNode2).keySet())) {
						dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
						dsim.get(patternNode1).add(patternNode2);

					}
				}
			}
		}

		if (dsim.size() == p1.vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation.customizedMatchList(p1,
					prefixTreeNode2.getData().getPatternGraph(), dsim, prefixTree);

			if (!result.isEmpty() && result.size() == p1.vertexSet().size()) {
				return result;
			}
		}
		return null;
	}

	public static double computeSupport(GraphDatabaseService dataGraph,
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, int snapshot, IPrefixTree prefixTree, double alpha)
			throws Exception {

		prefixTree.incNumberOfComputeSupport();

		// if (DummyProperties.incMode) {
		// Logger.log("# of computeSupport: " +
		// prefixTree.getNumberOfComputeSupport());
		// }
		double startTime = System.nanoTime();

		if (!tempProcessingNode.getData().isValid()) {
			throw new Exception(
					"non-valid patterns shouldn't be check for dual-sim, because they lose other match nodes!");
		}

		tempProcessingNode.getData().setVerified(true);

		Map<PatternNode, HashSet<Integer>> dsim = cutomizedMatchListDualSim(tempProcessingNode, dataGraph, prefixTree);

		// FOR-DEBUG START
		// if (dsim == null) {
		// tempProcessingNode.getData().setPatternAsInvalid(tempProcessingNode,
		// prefixTree, snapshot);
		// return 0.0d;
		// }
		// FOR-DEBUG END

		// compute support - should be in method
		double count = 0;
		for (PatternNode ptNode : dsim.keySet()) {
			if (ptNode.isFocus()) {
				count += dsim.get(ptNode).size();
			}
		}

		double snapshotSupp = count / Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
		tempProcessingNode.getData().setSupportFrequency(snapshot, snapshotSupp, alpha, false);

		// tempProcessingNode.getData().snapshotUB[snapshot] = snapshotSupp;
		// tempProcessingNode.getData().setTotalUpperbound(snapshot);

		if (count == 0) {
			tempProcessingNode.getData().setPatternAsInvalid(tempProcessingNode, prefixTree, snapshot);
		}

		prefixTree.updateDurationOfComputeSupport((System.nanoTime() - startTime) / 1e6);

		return tempProcessingNode.getData().getSupportFrequency(snapshot);
	}

	public static HashMap<PatternNode, HashSet<Integer>> cutomizedMatchListDualSim(
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, GraphDatabaseService dataGraph,
			IPrefixTree prefixTree) {

		if (tempProcessingNode.getData().getPatternGraph().vertexSet().size() <= 1) {
			return tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();
		}

		// if (DummyProperties.incMode) {
		// Logger.log("Before compute support for " +
		// tempProcessingNode.getData().getPatternPrefixTreeNodeIndex()
		// + "," + " isMFP:" + tempProcessingNode.getData().isMaximalFrequent()
		// + ","
		// + tempProcessingNode.getData().getNumerOfAllMatches());
		// Logger.log("BCS: V:" +
		// tempProcessingNode.getData().getPatternGraph().vertexSet().size() +
		// ", E:"
		// + tempProcessingNode.getData().getPatternGraph().edgeSet().size());
		// }
		tempProcessingNode.getData().getMatchedNodes()
				.setDataGraphMatchNodeOfAbsPNode(BatDualSimulation.customizedMatchList(dataGraph,
						tempProcessingNode.getData().getPatternGraph(), tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode(),
						prefixTree));
		// if (DummyProperties.incMode) {
		// Logger.log("After compute support: " +
		// tempProcessingNode.getData().getNumerOfAllMatches());
		// }

		// updating reverse side also:
		Iterator<Map.Entry<Integer, HashSet<PatternNode>>> patternNodeOfNeo4jNodeItr = tempProcessingNode.getData()
				.getMatchedNodes().getPatternNodeOfNeo4jNode().entrySet().iterator();
		while (patternNodeOfNeo4jNodeItr.hasNext()) {
			Integer nodeId = patternNodeOfNeo4jNodeItr.next().getKey();
			HashSet<PatternNode> patternNodesSet = tempProcessingNode.getData().getMatchedNodes()
					.getPatternNodeOfNeo4jNode().get(nodeId);
			Iterator<PatternNode> patternNodesSetItr = patternNodesSet.iterator();
			while (patternNodesSetItr.hasNext()) {
				PatternNode patternNode = patternNodesSetItr.next();

				// FOR-DEBUG START
				// if
				// (tempProcessingNode.getData().matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode)
				// == null) {
				// // TODO: it's just a patch, we should find
				// // the original problem
				// patternNodesSetItr.remove();
				// continue;
				// }
				// FOR-DEBUG END

				if (!tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						.contains(nodeId)) {
					if (tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(nodeId) == null
							|| tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(nodeId)
									.size() == 1) {
						// removing the whole 0 -> {p1} from
						// patternNodeOfNeo4jNode
						patternNodeOfNeo4jNodeItr.remove();
						if (prefixTree.getBitmap().isInitialized)
							prefixTree.getBitmap().removeNodeIdFromPatternId(nodeId,
									tempProcessingNode.getData().getPatternPrefixTreeNodeIndex());
					} else {
						// OR removing p2 of 0 -> {p1,p2} from
						// patternNodeOfNeo4jNode
						patternNodesSetItr.remove();
					}

				}
			}
		}
		return tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();

	}

	// public static double computeSupportBiSimOpt(GraphDatabaseService
	// dataGraph,
	// PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, int snapshot,
	// IPrefixTree prefixTree)
	// throws Exception {
	//
	// prefixTree.incNumberOfComputeSupport();
	//
	// double startTime = System.nanoTime();
	//
	// if (!tempProcessingNode.getData().isValid()) {
	// throw new Exception(
	// "non-valid patterns shouldn't be check for dual-sim, because they lose
	// other match nodes!");
	// }
	//
	// tempProcessingNode.getData().setVerified(true);
	//
	// Map<PatternNode, HashSet<Integer>> dsim =
	// cutomizedMatchListDualSimBiSimOpt(tempProcessingNode, dataGraph,
	// prefixTree);
	//
	//
	//
	// // compute support - should be in method
	// double count = 0;
	// for (PatternNode ptNode : dsim.keySet()) {
	// if (ptNode.isFocus()) {
	// count += dsim.get(ptNode).size();
	// }
	// }
	//
	// double snapshotSupp = count /
	// Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
	// tempProcessingNode.getData().setSupportFrequency(snapshot, snapshotSupp);
	//
	// // tempProcessingNode.getData().snapshotUB[snapshot] = snapshotSupp;
	// // tempProcessingNode.getData().setTotalUpperbound(snapshot);
	//
	// if (count == 0) {
	// tempProcessingNode.getData().setPatternAsInvalid(tempProcessingNode,
	// prefixTree, snapshot);
	// }
	//
	// prefixTree.updateDurationOfComputeSupport((System.nanoTime() - startTime)
	// / 1e6);
	//
	// return tempProcessingNode.getData().getSupportFrequency(snapshot);
	// }

	// public static HashMap<PatternNode, HashSet<Integer>>
	// cutomizedMatchListDualSimBiSimOpt(
	// PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
	// GraphDatabaseService dataGraph,
	// IPrefixTree prefixTree) {
	//
	// HashMap<PatternNode, HashSet<Integer>> newMatchNodeOfAbsPNode = new
	// HashMap<PatternNode, HashSet<Integer>>();
	// for (PatternNode patternNode :
	// tempProcessingNode.getData().getMatchedNodes()
	// .getDataGraphCandidateNodeOfAbsPNode().keySet()) {
	// newMatchNodeOfAbsPNode.put(patternNode, new HashSet<Integer>());
	// for (Integer nodeId :
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphCandidateNodeOfAbsPNode()
	// .get(patternNode)) {
	// newMatchNodeOfAbsPNode.get(patternNode).add(nodeId);
	// }
	// }
	//
	// if (tempProcessingNode.getData().getPatternGraph().vertexSet().size() <=
	// 1) {
	// tempProcessingNode.getData().getMatchedNodes().setDataGraphMatchNodeOfAbsPNode(newMatchNodeOfAbsPNode);
	// return
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphCandidateNodeOfAbsPNode();
	// }
	//
	// tempProcessingNode.getData().getMatchedNodes()
	// .setDataGraphMatchNodeOfAbsPNode(BatDualSimulation.customizedMatchList(dataGraph,
	// tempProcessingNode.getData().getPatternGraph(), newMatchNodeOfAbsPNode,
	// prefixTree));
	//
	// for (PatternNode patternNode :
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
	// .keySet()) {
	// for (Integer nodeId :
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
	// .get(patternNode)) {
	// tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().putIfAbsent(nodeId,
	// new HashSet<PatternNode>());
	// tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(nodeId).add(patternNode);
	// }
	// }
	//
	// return
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();
	//
	// }

	public static boolean preBiSimChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p2, IPrefixTree prefixTree) {

		prefixTree.incrementBiSimCheckingRequest();

		HashSet<String> allTypesOf1V = new HashSet<String>();

		for (PatternNode patternNode1 : p1.vertexSet()) {
			allTypesOf1V.add(patternNode1.getType());
		}

		HashSet<String> allTypesOf2V = new HashSet<String>();
		for (PatternNode patternNode2 : p2.vertexSet()) {
			allTypesOf2V.add(patternNode2.getType());
		}

		if (allTypesOf1V.size() != allTypesOf2V.size() || !allTypesOf1V.containsAll(allTypesOf2V)) {
			return false;
		}

		HashSet<String> allTypesOf1E = new HashSet<String>();
		for (DefaultLabeledEdge e : p1.edgeSet()) {
			allTypesOf1E.add(e.getType());
		}

		HashSet<String> allTypesOf2E = new HashSet<String>();
		for (DefaultLabeledEdge e : p2.edgeSet()) {
			allTypesOf2E.add(e.getType());
		}

		if (allTypesOf1E.size() != allTypesOf2E.size() || !allTypesOf1E.containsAll(allTypesOf2E)) {
			return false;
		}

		return true;
	}

	private static HashMap<PatternNode, HashSet<PatternNode>> getBiSimMapIfAnyInside(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> sourceGraphPattern,
			PrefixTreeNode<IPrefixTreeNodeData> targetGraphPrefixNode, IPrefixTree prefixTree) {

		if (isDualSim(targetGraphPrefixNode, sourceGraphPattern, prefixTree)) {
			return getDualSimIfAny(sourceGraphPattern, targetGraphPrefixNode, prefixTree);
		}

		return null;
	}

	public static HashMap<PatternNode, HashSet<PatternNode>> getBiSimMapIfAny(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> sourceGraphPattern,
			PrefixTreeNode<IPrefixTreeNodeData> targetGraphPrefixNode, IPrefixTree prefixTree) {

		double startTime = System.nanoTime();
		HashMap<PatternNode, HashSet<PatternNode>> map = getBiSimMapIfAnyInside(sourceGraphPattern,
				targetGraphPrefixNode, prefixTree);
		prefixTree.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
		return map;
	}

}
