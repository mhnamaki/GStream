package src.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;

import com.google.common.collect.MinMaxPriorityQueue;

import src.alg1.prefixTree.PrefixTreeAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.dualSimulation.gtar.BatDualSimulation;
import src.finalAlgorithm.prefixTree.PrefixTreeOptBiSim;

public class DebugHelper {
	public static void printParentChildRelationship(IPrefixTree prefixTree) {
		for (PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode : prefixTree
				.getLabelAdjacencyIndexer().parentChildDifference.keySet()) {
			System.out.print(parentPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + "=> [");
			for (PrefixTreeNode<IPrefixTreeNodeData> childPrefixTreeNode : prefixTree
					.getLabelAdjacencyIndexer().parentChildDifference.get(parentPrefixTreeNode).keySet()) {
				System.out.print(childPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + ", ");
			}

			System.out.println("]");
		}
	}

	public static void printIsomorphicPatterns(IPrefixTree prefixTree) {
		System.out.println("isomorphic patterns in tree if any?");

		for (Integer prefixTreeIndex1 : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			for (Integer prefixTreeIndex2 : prefixTree.getPrefixTreeNodeIndex().keySet()) {

				if (prefixTreeIndex1 < prefixTreeIndex2) {
					// if (prefixTreeIndex1 == 11 && prefixTreeIndex2 == 16) {
					// System.out.println(prefixTreeIndex1 + " iso " +
					// prefixTreeIndex2);
					// }
					boolean isPreIsoChecking = prefixTree.preIsoChecking(
							prefixTree.getPrefixTreeNodeIndex().get(prefixTreeIndex1).getData().getPatternGraph(),
							prefixTree.getPrefixTreeNodeIndex().get(prefixTreeIndex2).getData().getPatternGraph());

					VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = prefixTree.getIsomorphism(
							prefixTree.getPrefixTreeNodeIndex().get(prefixTreeIndex1).getData().getPatternGraph(),
							prefixTree.getPrefixTreeNodeIndex().get(prefixTreeIndex2).getData().getPatternGraph());

					if (iso != null && iso.isomorphismExists()) {
						System.out.println("g1:" + prefixTreeIndex1 + ", g2:" + prefixTreeIndex2 + " isPreIsoChecking:"
								+ isPreIsoChecking + " graph:"
								+ prefixTree.getPrefixTreeNodeIndex().get(prefixTreeIndex1).getData());
					}
				}
			}
		}

		System.out.println("end");
	}

	private static boolean isSubgraphIsomorphic(PrefixTreeNode<IPrefixTreeNodeData> bigGraphPTN,
			PrefixTreeNode<IPrefixTreeNodeData> subgraphGraphPTN) {
		VF2SubgraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = new VF2SubgraphIsomorphismInspector<PatternNode, DefaultLabeledEdge>(
				bigGraphPTN.getData().getPatternGraph(), subgraphGraphPTN.getData().getPatternGraph(),
				new Comparator<PatternNode>() {

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

		return iso.isomorphismExists();
	}

	public static void printSubGraphIsomorphicTopkPatterns(
			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topkFrequentPatterns) {

		System.out.println("subgraph iso finder in topk if any?");

		ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> newTopkArr = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>(
				topkFrequentPatterns.size());

		newTopkArr.addAll(topkFrequentPatterns);

		for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode1 : newTopkArr) {
			for (PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode2 : newTopkArr) {
				if (prefixTreeNode1 != prefixTreeNode2) {

					if (isSubgraphIsomorphic(prefixTreeNode1, prefixTreeNode2)) {
						System.err.println("g1:" + prefixTreeNode1.getData().getPatternPrefixTreeNodeIndex() + ", g2:"
								+ prefixTreeNode2.getData().getPatternPrefixTreeNodeIndex());
					}

				}
			}
		}
		System.out.println("end");
	}

	public static void printBiSimulatedPatterns(IPrefixTree prefixTree) {
		System.out.println("");
		System.out.println("BiSimulated Patterns in tree if any?");
		int cnt = 0;

		for (Integer biggerIndex : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			for (Integer smallerIndex : prefixTree.getPrefixTreeNodeIndex().keySet()) {

				if (smallerIndex < biggerIndex
						&& prefixTree.getPrefixTreeNodeIndex().get(biggerIndex).getData().getPatternGraph().edgeSet()
								.size() >= 1
						&& prefixTree.getPrefixTreeNodeIndex().get(smallerIndex).getData().getPatternGraph().edgeSet()
								.size() >= 1) {

					if (DualSimulationHandler.isBiDualSimulated(
							prefixTree.getPrefixTreeNodeIndex().get(biggerIndex).getData().getPatternGraph(),
							prefixTree.getPrefixTreeNodeIndex().get(smallerIndex), prefixTree)) {
						cnt++;
						System.out.println("Bi-SiMulaTED");
						System.out.println("bigger: " + prefixTree.getPrefixTreeNodeIndex().get(biggerIndex).getData());
						System.out
								.println("smaller: " + prefixTree.getPrefixTreeNodeIndex().get(smallerIndex).getData());
						System.out.println();
					}

				}
			}
		}
		System.out.println();
		System.out.println("bi simul cnt: " + cnt);
	}

	private static boolean isDualSim(Integer index1, Integer index2, IPrefixTree prefixTree) {
		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : prefixTree.getPrefixTreeNodeIndex().get(index1).getData().getPatternGraph()
				.vertexSet()) {
			for (PatternNode patternNode2 : prefixTree.getPrefixTreeNodeIndex().get(index2).getData().getPatternGraph()
					.vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())
						// &&
						// prefixTree.getPrefixTreeNodeIndex().get(biggerIndex).getData()
						// .getIncomingRelTypesOfPatternNodes().get(patternNode1)
						// .equals(prefixTree.getPrefixTreeNodeIndex().get(prefixTreeIndex2).getData()
						// .getIncomingRelTypesOfPatternNodes().get(patternNode2))
						&& prefixTree.getPrefixTreeNodeIndex().get(index1).getData().getStepsFromRootOfPatternNodes()
								.get(patternNode1).equals(prefixTree.getPrefixTreeNodeIndex().get(index2).getData()
										.getStepsFromRootOfPatternNodes().get(patternNode2))) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				}
			}
		}

		if (dsim.size() == prefixTree.getPrefixTreeNodeIndex().get(index1).getData().getPatternGraph().vertexSet()
				.size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation.customizedMatchList(
					prefixTree.getPrefixTreeNodeIndex().get(index1).getData().getPatternGraph(),
					prefixTree.getPrefixTreeNodeIndex().get(index2).getData().getPatternGraph(), dsim, prefixTree);

			if (!result.isEmpty() && result.size() == prefixTree.getPrefixTreeNodeIndex().get(index1).getData()
					.getPatternGraph().vertexSet().size()) {
				return true;
			} else {
				// System.out.println("NOT simulated");
				// System.out.println("index1: " +
				// prefixTree.getPrefixTreeNodeIndex().get(index1).getData());
				// System.out.println("index2: " +
				// prefixTree.getPrefixTreeNodeIndex().get(index2).getData());
				// System.out.println("res: " + dsim);
				// System.out.println();
			}
		}
		return false;
	}

	public static void printGlobalCandidateSet(IPrefixTree prefixTree) {
		System.out.println();
		for (PatternNode patternNode : prefixTree.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.keySet()) {
			System.out.print(patternNode + ": ");
			for (Integer nodeId : prefixTree.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.get(patternNode)) {
				System.out.print(nodeId + ", ");
			}
			System.out.println();
		}
		System.out.println();

	}

	public static void printAllCandidates(IPrefixTree prefixTree) {
		HashSet<Integer> allCandidatesSet = new HashSet<Integer>();
		System.out.println("allCandidateSet:");
		for (PatternNode patternNode : prefixTree.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.keySet()) {
			for (Integer nodeId : prefixTree.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.get(patternNode)) {
				allCandidatesSet.add(nodeId);
			}

		}

		ArrayList<Integer> canIdsOrdered = new ArrayList<Integer>();
		canIdsOrdered.addAll(allCandidatesSet);
		Collections.sort(canIdsOrdered);

		System.out.print("[");
		for (Integer id : canIdsOrdered) {
			System.out.print(id + ", ");
		}
		System.out.print("]");

	}

	public static void printAffectedPatterns(ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> insertAffectedPatternsArr) {
		System.out.print("affected patterns:");
		for (int p = 0; p < insertAffectedPatternsArr.size(); p++) {
			System.out.print(insertAffectedPatternsArr.get(p).getData().getPatternPrefixTreeNodeIndex() + ", ");
		}
		System.out.println();

	}

	public static void logAllMatchesOfAPattern(PrefixTreeNode<IPrefixTreeNodeData> pattern) {
		String str = "";
		for (PatternNode patternNode : pattern.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().keySet()) {
			str += pattern.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode);
		}

		Logger.logAllMatchesOfAPattern(str);
	}
	// public static void testSubGraphIsomorphicParentChild(IPrefixTree
	// prefixTree) {
	// for (PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode :
	// prefixTree
	// .getLabelAdjacencyIndexer().parentChildDifference.keySet()) {
	// for (PrefixTreeNode<IPrefixTreeNodeData> childPrefixTreeNode : prefixTree
	// .getLabelAdjacencyIndexer().parentChildDifference.get(parentPrefixTreeNode).keySet())
	// {
	//
	// if (parentPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex() == 9
	// && childPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex() == 11) {
	// System.out.println();
	// }
	// if (!isSubgraphIsomorphic(childPrefixTreeNode, parentPrefixTreeNode)) {
	// System.err.println("parent is not subgraph iso of its child");
	// System.err.println("parent: " +
	// parentPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + " " +
	// parentPrefixTreeNode.getData().getPatternGraph());
	// System.err.println("parent: " + parentPrefixTreeNode.getData());
	// System.err.println("child: " +
	// childPrefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + " " +
	// childPrefixTreeNode.getData().getPatternGraph());
	// System.err.println("child: " + childPrefixTreeNode.getData());
	// System.err.println();
	// }
	// }
	// }
	// }

	public static void getAllMatchesOrderedByNodeId(IPrefixTree prefixTree) {

		HashSet<Integer> matchIds = new HashSet<Integer>();
		for (Integer patternId : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			for (PatternNode patternNode : prefixTree.getPrefixTreeNodeIndex().get(patternId).getData()
					.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().keySet()) {
				matchIds.addAll(prefixTree.getPrefixTreeNodeIndex().get(patternId).getData().getMatchedNodes()
						.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
			}
		}

		ArrayList<Integer> matchIdsOrdered = new ArrayList<Integer>();
		matchIdsOrdered.addAll(matchIds);
		Collections.sort(matchIdsOrdered);

		System.out.print("[");
		for (Integer id : matchIdsOrdered) {
			System.out.print(id + ",");
		}
		System.out.println("]");

	}

	public static void printIfLevelIsNotConsistentWithNumberOfEdges(IPrefixTree prefixTree) {
		System.out.println("not from same level if any?");
		for (Integer patternId : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = prefixTree.getPrefixTreeNodeIndex().get(patternId);

			if (prefixTreeNode.getData().getPatternGraph().edgeSet().size() != (prefixTreeNode.getLevel() - 1)) {
				System.out.println(prefixTreeNode.getData() + "parent: " + prefixTreeNode.getParent().getData());
			}

		}
		System.out.println("end");

	}

	public static void printPatternWithDuplicateMatches(IPrefixTree prefixTree) {
		HashSet<PrefixTreeNode<IPrefixTreeNodeData>> patterns = new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>();

		for (Integer patternId : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = prefixTree.getPrefixTreeNodeIndex().get(patternId);
			ArrayList<PatternNode> patternNodesArr = new ArrayList<PatternNode>();
			patternNodesArr.addAll(prefixTreeNode.getData().getPatternGraph().vertexSet());
			for (int i = 0; i < patternNodesArr.size(); i++) {
				for (int j = 0; j < patternNodesArr.size(); j++) {
					if (i < j) {
						PatternNode patternNode1 = patternNodesArr.get(i);
						PatternNode patternNode2 = patternNodesArr.get(j);
						if (prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
								.get(patternNode1).size() != prefixTreeNode.getData().getMatchedNodes()
										.getDataGraphMatchNodeOfAbsPNode().get(patternNode2).size())
							continue;

						boolean sameMatches = true;
						for (Integer nodeId1 : prefixTreeNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(patternNode1)) {
							boolean isFoundEqual = false;
							for (Integer nodeId2 : prefixTreeNode.getData().getMatchedNodes()
									.getDataGraphMatchNodeOfAbsPNode().get(patternNode2)) {
								if (nodeId1 == nodeId2) {
									isFoundEqual = true;
									break;
								}
							}
							if (!isFoundEqual) {
								sameMatches = false;
								break;
							}
						}
						if (sameMatches) {
							patterns.add(prefixTreeNode);
						}
					}
				}
			}
		}

		if (patterns.size() > 0) {
			System.out.println("duplicated matches?");
			for (PrefixTreeNode<IPrefixTreeNodeData> pattern : patterns) {
				System.out.println(pattern.getData());
			}
			System.out.println("end");
			System.out.println();
		}

	}
}
