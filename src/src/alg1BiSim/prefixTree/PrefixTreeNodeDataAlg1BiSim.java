package src.alg1BiSim.prefixTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.graph.ListenableDirectedGraph;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import src.base.IMatchNodes;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.utilities.DefaultLabeledEdge;
import src.utilities.DualSimulationHandler;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy.DummyProperties.Direction;
import src.utilities.Dummy.DummyProperties.PrefixTreeMode;
import src.utilities.Indexer;

//TODO: make sure that "numberOfFrequentChildrenAndLinked" is maintained

//prefix-tree node data
public class PrefixTreeNodeDataAlg1BiSim implements IPrefixTreeNodeData {

	private PatternNode patternRootNode = null;

	public PatternNode sourcePatternNode;
	public PatternNode targetPatternNode;
	public HashMap<PatternNode, HashMap<String, Integer>> incomingRelTypesOfPatternNodes = new HashMap<PatternNode, HashMap<String, Integer>>();
	public HashMap<PatternNode, Integer> stepsFromRootOfPatternNodes = new HashMap<PatternNode, Integer>();
	// public HashMap<Integer, HashSet<PatternNode>> patternNodesOfStepsFromRoot
	// = new HashMap<Integer, HashSet<PatternNode>>();

	public HashMap<PatternNode, HashMap<String, Integer>> frequencyOfNextNeighborOfSameType = new HashMap<PatternNode, HashMap<String, Integer>>();

	// from source to target we have connected through a unique relationshipType
	public String relationshipType;

	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph = null;

	// collection of all concrete patterns:
	public MatchNodesAlg1BiSim matchNodes = null;

	// public int maxHopsFromRoot = 0;

	// ??
	public boolean canBeMaximalFrequent = true;

	// if no child of this is frequent
	public boolean isMaximalFrequent = false;

	// the first infrequent node in a branch to be MFP
	public boolean isMinimalInFrequent = false;

	// just if satisfied the threshold condition
	public boolean isFrequent = false;

	// if we ran dual-simulation for this
	public boolean isVerified = false;

	// if it has at least one possible match set for each pattern node in it
	public boolean isValid = true;

	// if the # of matches for same type/sibling are less than the number of
	// them
	private boolean isCorrect = true;

	public boolean isVisited = false;

	public int numberOfFrequentChildrenAndLinked = 0;

	public boolean isDanglingPattern = false;

	// we have to maintain which focus nodes we've seen so far to not select
	// another node of same type for another focus.
	public HashSet<String> typeOfUnSeenFocusNodes;

	public Double[] supportFrequency = new Double[DummyProperties.NUMBER_OF_SNAPSHOTS];
	public LinkedList<Double> supportFrequencyWindowing = new LinkedList<Double>();

	public double totalSupportFrequency = 0.0d;
	public boolean foundAllFocuses = false;

	// public double totalUpperboundEstimation =
	// DummyProperties.NUMBER_OF_SNAPSHOTS;
	// public double[] snapshotUB = new
	// double[DummyProperties.NUMBER_OF_SNAPSHOTS];
	// public int lastSeenSnapshot = 0;

	public HashMap<Integer, HashSet<PatternNode>> newUnexpandedNodesOfPatternNodes;
	public HashMap<PatternNode, HashSet<Integer>> newUnexpandedPatternsNodesOfNeo4jNodes;

	public PrefixTreeMode prefixTreeMode = PrefixTreeMode.BATCH;
	public Integer patternPrefixTreeNodeIndex;

	public double getTotalSupportFrequency() {
		return this.totalSupportFrequency;
	}

	public void setTotalSupportFrequency(double totalSupportFrequency) {
		this.totalSupportFrequency = totalSupportFrequency;
	}

	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> getPatternGraph() {
		return this.patternGraph;
	}

	public void setPatternGraph(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph) {
		this.patternGraph = patternGraph;
	}

	public int getPatternPrefixTreeNodeIndex() {
		return this.patternPrefixTreeNodeIndex;
	}

	public void setPatternPrefixTreeNodeIndex(int patternPrefixTreeNodeIndex) {
		this.patternPrefixTreeNodeIndex = patternPrefixTreeNodeIndex;
	}

	public HashMap<Integer, HashSet<PatternNode>> getNewUnexpandedNodesOfPatternNodes() {
		return this.newUnexpandedNodesOfPatternNodes;
	}

	public IMatchNodes getMatchedNodes() {
		return this.matchNodes;
	}

	public HashMap<PatternNode, HashMap<String, Integer>> getIncomingRelTypesOfPatternNodes() {
		return this.incomingRelTypesOfPatternNodes;
	}

	public HashMap<PatternNode, Integer> getStepsFromRootOfPatternNodes() {
		return this.stepsFromRootOfPatternNodes;
	}

	public boolean isMaximalFrequent() {
		return this.isMaximalFrequent;
	}

	public boolean isValid() {
		return this.isValid;
	}

	public boolean isVerified() {
		return this.isVerified;
	}

	public boolean isVisited() {
		return this.isVisited;
	}

	public PatternNode getSourcePatternNode() {
		return this.sourcePatternNode;
	}

	public PatternNode getTargetPatternNode() {
		return this.targetPatternNode;
	}

	public HashMap<PatternNode, HashSet<Integer>> getNewUnexpandedPatternsNodesOfNeo4jNodes() {
		return this.newUnexpandedPatternsNodesOfNeo4jNodes;
	}

	public boolean isFrequent() {
		return this.isFrequent;
	}

	// usage: for root node;
	public PrefixTreeNodeDataAlg1BiSim(Set<String> set) {
		typeOfUnSeenFocusNodes = new HashSet<String>();
		typeOfUnSeenFocusNodes = (HashSet<String>) ((HashSet<String>) set).clone();
		this.isVisited = true;
	}

	/**
	 * usage: for first born focus labels;
	 * 
	 * @param sourceAbstractPatternNode:
	 *            first born focus pattern node
	 * @param srcDataGraphPatternNodes:
	 *            focus candidates
	 * @param focusLabelSet
	 *            ??
	 * @param patternPrefixTreeNodeIndex:
	 *            the index of this pattern in the prefixTree
	 */
	public PrefixTreeNodeDataAlg1BiSim(PatternNode sourceAbstractPatternNode, HashSet<Integer> srcDataGraphPatternNodes,
			HashSet<String> focusLabelSet, Integer patternPrefixTreeNodeIndex, Indexer labelAdjacencyIndexer) {

		labelAdjacencyIndexer.candidateSetOfAPatternNode.putIfAbsent(sourceAbstractPatternNode, new HashSet<Integer>());
		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(sourceAbstractPatternNode)
				.addAll(srcDataGraphPatternNodes);

		this.patternPrefixTreeNodeIndex = patternPrefixTreeNodeIndex;
		this.sourcePatternNode = sourceAbstractPatternNode;
		this.patternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(DefaultLabeledEdge.class);
		patternGraph.addVertex(sourceAbstractPatternNode);
		stepsFromRootOfPatternNodes.put(sourceAbstractPatternNode, 0);
		// patternNodesOfStepsFromRoot.putIfAbsent(0, new
		// HashSet<PatternNode>());
		// patternNodesOfStepsFromRoot.get(0).add(sourceAbstractPatternNode);

		// each focus label can translate to multiple data nodes.
		HashMap<PatternNode, HashSet<Integer>> dataGraphNodeOfAbsPttnNode = new HashMap<PatternNode, HashSet<Integer>>();
		HashMap<Integer, HashSet<PatternNode>> abstractPatternNodeOfNeo4jNode = new HashMap<Integer, HashSet<PatternNode>>();

		dataGraphNodeOfAbsPttnNode.put(sourceAbstractPatternNode, new HashSet<Integer>());

		for (Integer srcDataGraphPatternNodeId : srcDataGraphPatternNodes) {

			dataGraphNodeOfAbsPttnNode.get(sourceAbstractPatternNode).add(srcDataGraphPatternNodeId);

			abstractPatternNodeOfNeo4jNode.put(srcDataGraphPatternNodeId, new HashSet<PatternNode>());
			abstractPatternNodeOfNeo4jNode.get(srcDataGraphPatternNodeId).add(sourceAbstractPatternNode);

		}

		matchNodes = new MatchNodesAlg1BiSim(dataGraphNodeOfAbsPttnNode, abstractPatternNodeOfNeo4jNode);

		// pattern root node:
		patternRootNode = sourceAbstractPatternNode;

		typeOfUnSeenFocusNodes = (HashSet<String>) focusLabelSet.clone();
		typeOfUnSeenFocusNodes.remove(sourceAbstractPatternNode.getLabel());
		if (typeOfUnSeenFocusNodes.size() == 0) {
			foundAllFocuses = true;
		}

		if (!DummyProperties.windowMode) {
			this.supportFrequency[0] = (double) (srcDataGraphPatternNodes.size()
					/ DummyProperties.NUMBER_OF_ALL_FOCUS_NODES);

			for (int i = 1; i < this.supportFrequency.length; i++) {
				this.supportFrequency[i] = 0d;
			}

			this.totalSupportFrequency = this.supportFrequency[0];
		} else {
			this.supportFrequencyWindowing
					.add((double) (srcDataGraphPatternNodes.size() / DummyProperties.NUMBER_OF_ALL_FOCUS_NODES));
			for (int i = 1; i < DummyProperties.WINDOW_SIZE; i++) {
				this.supportFrequencyWindowing.add(0d);
			}
		}

		this.frequencyOfNextNeighborOfSameType.put(sourceAbstractPatternNode, new HashMap<String, Integer>());
	}

	/**
	 * adding a regular prefix-tree-node
	 * 
	 * @param newAbsPattern
	 * @param patternRootNode
	 * @param parentPTNodeData
	 * @param parentMatchedNodes
	 * @param srcAbstractPatternNode
	 * @param destAbstractPatternNode
	 * @param srcDataGraphPatternNodeId
	 * @param destDataGraphPatternNodeId
	 * @param patternPrefixTreeNodeIndex
	 * @param freshSource
	 */
	public PrefixTreeNodeDataAlg1BiSim(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern,
			PatternNode patternRootNode, IPrefixTreeNodeData parentPTNodeData, IMatchNodes parentMatchedNodes,
			PatternNode srcAbstractPatternNode, PatternNode destAbstractPatternNode, Set<Integer> newNodeIds,
			Integer patternPrefixTreeNodeIndex, String relationshipType, Integer destStepsFromRoot, int snapshot,
			Indexer labelAdjacencyIndexer, boolean isDanglingPattern) {

		if (!DummyProperties.windowMode) {
			for (int i = 0; i < this.supportFrequency.length; i++) {
				if (this.supportFrequency[i] == null)
					this.supportFrequency[i] = 0d;
			}
		} else {
			for (int i = 0; i < DummyProperties.WINDOW_SIZE; i++) {
				this.supportFrequencyWindowing.add(0d);
			}
		}

		this.isDanglingPattern = isDanglingPattern;
		labelAdjacencyIndexer.candidateSetOfAPatternNode.putIfAbsent(destAbstractPatternNode, new HashSet<Integer>());
		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(destAbstractPatternNode).addAll(newNodeIds);

		this.patternPrefixTreeNodeIndex = patternPrefixTreeNodeIndex;
		basicOperation(newAbsPattern, patternRootNode, srcAbstractPatternNode, destAbstractPatternNode,
				relationshipType);

		// updating maxHopsFromRoot
		// maxHopsFromRoot = Math.max(maxHopsFromRoot,
		// destAbstractPatternNode.stepsFromRoot);

		// TODO: may be we can remove this if
		if (matchNodes == null) {
			HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode = new HashMap<PatternNode, HashSet<Integer>>();
			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode = new HashMap<Integer, HashSet<PatternNode>>();
			this.matchNodes = new MatchNodesAlg1BiSim(dataGraphMatchNodeOfAbsPNode, patternNodeOfNeo4jNode);
		}

		// TODO: may be we can make it more efficient if we don't copy
		// corresponding src nodes
		// warm-up child from parent:
		// if (this.patternPrefixTreeNodeIndex == 11) {
		// System.out.println();
		// }
		// indexing over next type nodes
		for (PatternNode patternNode : parentPTNodeData.getFrequencyOfNextNeighborOfSameType().keySet()) {
			this.frequencyOfNextNeighborOfSameType.put(patternNode, new HashMap<String, Integer>());
			for (String nextType : parentPTNodeData.getFrequencyOfNextNeighborOfSameType().get(patternNode).keySet()) {
				this.frequencyOfNextNeighborOfSameType.get(patternNode).put(nextType,
						parentPTNodeData.getFrequencyOfNextNeighborOfSameType().get(patternNode).get(nextType));
			}
		}

		// for (DefaultLabeledEdge e :
		// this.patternGraph.incomingEdgesOf(destAbstractPatternNode)) {
		// PatternNode prevPatternNode = this.patternGraph.getEdgeSource(e);
		String nextType = destAbstractPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
				+ relationshipType;
		// if (this.frequencyOfNextNeighborOfSameType.get(prevPatternNode) ==
		// null) {
		// System.out.println();
		// }
		//
		this.frequencyOfNextNeighborOfSameType.get(srcAbstractPatternNode).putIfAbsent(nextType, 0);
		this.frequencyOfNextNeighborOfSameType.get(srcAbstractPatternNode).put(nextType,
				this.frequencyOfNextNeighborOfSameType.get(srcAbstractPatternNode).get(nextType) + 1);
		// }

		this.frequencyOfNextNeighborOfSameType.putIfAbsent(destAbstractPatternNode, new HashMap<String, Integer>());

		for (PatternNode patternNode : parentMatchedNodes.getDataGraphMatchNodeOfAbsPNode().keySet()) {

			this.matchNodes.dataGraphMatchNodeOfAbsPNode.put(patternNode, new HashSet<Integer>());
			// if (freshSource) {
			// if (patternNode != srcAbstractPatternNode) {
			// for (Integer nodeId :
			// parentMatchedNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode))
			// {
			// this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).add(nodeId);
			// }
			// }
			// } else {
			for (Integer nodeId : parentMatchedNodes.getDataGraphMatchNodeOfAbsPNode().get(patternNode)) {
				this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).add(nodeId);
			}
			// }

		}
		this.matchNodes.dataGraphMatchNodeOfAbsPNode.putIfAbsent(destAbstractPatternNode, new HashSet<Integer>());

		for (Integer nodeId : parentMatchedNodes.getPatternNodeOfNeo4jNode().keySet()) {
			this.matchNodes.patternNodeOfNeo4jNode.put(nodeId, new HashSet<PatternNode>());
			for (PatternNode patternNode : parentMatchedNodes.getPatternNodeOfNeo4jNode().get(nodeId)) {
				// if (freshSource) {
				// if (patternNode != srcAbstractPatternNode) {
				// this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
				// }
				// } else {
				this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
				// }

			}
			if (this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).size() == 0) {
				this.matchNodes.patternNodeOfNeo4jNode.remove(nodeId);
			}

		}

		for (Integer newNodeId : newNodeIds) {
			this.matchNodes.patternNodeOfNeo4jNode.putIfAbsent(newNodeId, new HashSet<PatternNode>());
		}

		// for reltype parent-child data warm-up
		for (

		PatternNode patternNode : parentPTNodeData.getIncomingRelTypesOfPatternNodes().keySet()) {
			this.incomingRelTypesOfPatternNodes.put(patternNode, new HashMap<String, Integer>());
		}
		for (PatternNode patternNode : parentPTNodeData.getIncomingRelTypesOfPatternNodes().keySet()) {
			for (String relType : parentPTNodeData.getIncomingRelTypesOfPatternNodes().get(patternNode).keySet()) {
				this.incomingRelTypesOfPatternNodes.get(patternNode).put(relType,
						parentPTNodeData.getIncomingRelTypesOfPatternNodes().get(patternNode).get(relType));
			}
		}

		if (!this.incomingRelTypesOfPatternNodes.containsKey(destAbstractPatternNode)) {
			this.incomingRelTypesOfPatternNodes.put(destAbstractPatternNode, new HashMap<String, Integer>());
		}

		if (!this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).containsKey(relationshipType)) {
			this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).put(relationshipType, 1);
		} else {
			this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).put(relationshipType,
					this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).get(relationshipType) + 1);
		}

		// for steps from root from parent
		for (PatternNode patternNode : parentPTNodeData.getStepsFromRootOfPatternNodes().keySet()) {
			this.stepsFromRootOfPatternNodes.put(patternNode,
					parentPTNodeData.getStepsFromRootOfPatternNodes().get(patternNode));
		}
		this.stepsFromRootOfPatternNodes.put(destAbstractPatternNode, destStepsFromRoot);

		// inverse update:
		// for (Integer stepsFromRoot :
		// parentPTNodeData.getPatternNodesOfStepsFromRoot().keySet()) {
		// this.patternNodesOfStepsFromRoot.putIfAbsent(stepsFromRoot, new
		// HashSet<PatternNode>());
		// for (PatternNode patternNode :
		// parentPTNodeData.getPatternNodesOfStepsFromRoot().get(stepsFromRoot))
		// {
		// // because may be dest pattern node changed its steps from root.
		// if (patternNode != destAbstractPatternNode) {
		// this.patternNodesOfStepsFromRoot.get(stepsFromRoot).add(patternNode);
		// }
		// }
		// }
		//
		// this.patternNodesOfStepsFromRoot.putIfAbsent(destStepsFromRoot, new
		// HashSet<PatternNode>());
		// this.patternNodesOfStepsFromRoot.get(destStepsFromRoot).add(destAbstractPatternNode);

		// because in case of static we could reach from patterns from other
		// sides
		// in case of dynamic we are limited for this so we should use parent
		// information more

		// if (freshSource) {
		// this.matchNodes.patternNodeOfNeo4jNode.putIfAbsent(srcDataGraphPatternNodeId,
		// new HashSet<PatternNode>());
		// addNewMatch(srcAbstractPatternNode, srcDataGraphPatternNodeId);
		// }
		for (Integer newNodeId : newNodeIds) {
			addNewMatch(destAbstractPatternNode, newNodeId, labelAdjacencyIndexer);
		}

		this.foundAllFocuses = parentPTNodeData.getFoundAllFocuses();
		if (parentPTNodeData.getTypeOfUnSeenFocusNodes() != null
				&& parentPTNodeData.getTypeOfUnSeenFocusNodes().size() > 0) {
			this.typeOfUnSeenFocusNodes = (HashSet<String>) parentPTNodeData.getTypeOfUnSeenFocusNodes().clone();
			// removing an unseen focus node if we've seen it right now:
			this.typeOfUnSeenFocusNodes.remove(destAbstractPatternNode.getLabel());
			if (this.typeOfUnSeenFocusNodes.size() == 0) {
				foundAllFocuses = true;
			}
		}

	}

	private void basicOperation(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern,
			PatternNode patternRootNode, PatternNode srcAbstractPatternNode, PatternNode destAbstractPatternNode,
			String relationshipType) {

		// this first node in this pattern
		this.patternRootNode = patternRootNode;

		this.sourcePatternNode = srcAbstractPatternNode;
		this.targetPatternNode = destAbstractPatternNode;
		this.relationshipType = relationshipType;

		// abstract pattern
		// we have the new abs pattern here because we had to do a SGI checking
		this.patternGraph = newAbsPattern;

	}

	public void addNewMatch(PatternNode destAbstractPatternNode, Integer destDataGraphPatternNodeId,
			Indexer labelAdjacencyIndexer) {

		if (DummyProperties.debugMode
				&& this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(destAbstractPatternNode) == null) {
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
			System.out.println("destOrSrcAbstractPatternNode: " + destAbstractPatternNode
					+ ", destOrSrcDataGraphPatternNodeId: " + destDataGraphPatternNodeId
					+ ", destOrSrcPatternNode hashCode: " + destAbstractPatternNode.hashCode());
		}

		this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(destAbstractPatternNode).add(destDataGraphPatternNodeId);

		if (!this.matchNodes.patternNodeOfNeo4jNode.containsKey(destDataGraphPatternNodeId)) {
			this.matchNodes.patternNodeOfNeo4jNode.put(destDataGraphPatternNodeId, new HashSet<PatternNode>());
		}

		this.matchNodes.patternNodeOfNeo4jNode.get(destDataGraphPatternNodeId).add(destAbstractPatternNode);

		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(destAbstractPatternNode).add(destDataGraphPatternNodeId);
	}

	public void addNewMatchForUpdate(PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			PatternNode srcAbstractPatternNode, Integer srcDataGraphPatternNodeId, PatternNode destAbstractPatternNode,
			Integer destDataGraphPatternNodeId, Indexer labelAdjacencyIndexer) {

		// if (this.patternPrefixTreeNodeIndex == 199) {
		// System.out.println();
		// }

		if (DummyProperties.debugMode) {
			System.out.println("before updating new matches!");
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
		}

		addNewMatch(srcAbstractPatternNode, srcDataGraphPatternNodeId, labelAdjacencyIndexer);
		addNewMatch(destAbstractPatternNode, destDataGraphPatternNodeId, labelAdjacencyIndexer);

		if (this.isVisited)
			this.prefixTreeMode = PrefixTreeMode.UPDATE;

		// we have to inherit new matches from parents and consider them as new
		// matches also
		if (parentPrefixTreeNode.getData().getNewUnexpandedNodesOfPatternNodes() != null) {
			if (this.newUnexpandedNodesOfPatternNodes == null)
				this.newUnexpandedNodesOfPatternNodes = new HashMap<Integer, HashSet<PatternNode>>();

			for (Integer nodeId : parentPrefixTreeNode.getData().getNewUnexpandedNodesOfPatternNodes().keySet()) {
				this.newUnexpandedNodesOfPatternNodes.putIfAbsent(nodeId, new HashSet<PatternNode>());
				for (PatternNode unexpandedPatternNode : parentPrefixTreeNode.getData()
						.getNewUnexpandedNodesOfPatternNodes().get(nodeId)) {
					if (this.patternGraph.vertexSet().contains(unexpandedPatternNode)) {
						this.newUnexpandedNodesOfPatternNodes.get(nodeId).add(unexpandedPatternNode);
					} else {
						// find corresponding one:
						// we may come from a prefix node that it's not its
						// parent/superlinknode
						// so, parent pattern nodes are totally unrelated with
						// this
						for (PatternNode patternNode : this.patternGraph.vertexSet()) {
							if (twoPatternNodesAreSame(unexpandedPatternNode, parentPrefixTreeNode, patternNode,
									this)) {
								this.newUnexpandedNodesOfPatternNodes.get(nodeId).add(patternNode);
								break;
							}
						}

					}

				}

				if (this.matchNodes.patternNodeOfNeo4jNode.get(nodeId) != null) {
					this.matchNodes.patternNodeOfNeo4jNode.get(nodeId)
							.addAll(this.newUnexpandedNodesOfPatternNodes.get(nodeId));
				} else {
					// b2
					this.matchNodes.patternNodeOfNeo4jNode.put(nodeId, new HashSet<PatternNode>());
					HashSet<PatternNode> newUnexpandedPatternNodes = this.newUnexpandedNodesOfPatternNodes.get(nodeId);

					for (PatternNode patternNode : this.patternGraph.vertexSet()) {
						for (PatternNode unExpPN : newUnexpandedPatternNodes) {
							if (twoPatternNodesAreSame(unExpPN, parentPrefixTreeNode, patternNode, this)) {
								this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
							}
						}
					}
				}
			}
		}
		// TODO: may be bi-directional map helps us to reduce both bugs and
		// slowness
		if (parentPrefixTreeNode.getData().getNewUnexpandedNodesOfPatternNodes() != null) {
			if (this.newUnexpandedPatternsNodesOfNeo4jNodes == null)
				this.newUnexpandedPatternsNodesOfNeo4jNodes = new HashMap<PatternNode, HashSet<Integer>>();

			for (Integer nodeId : this.newUnexpandedNodesOfPatternNodes.keySet()) {
				for (PatternNode unexpandedPatternNode : this.newUnexpandedNodesOfPatternNodes.get(nodeId)) {
					this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(unexpandedPatternNode,
							new HashSet<Integer>());
					this.newUnexpandedPatternsNodesOfNeo4jNodes.get(unexpandedPatternNode).add(nodeId);
				}
			}

			for (PatternNode unexpandedPatternNode : this.newUnexpandedPatternsNodesOfNeo4jNodes.keySet()) {

				if (this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(unexpandedPatternNode) != null) {
					this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(unexpandedPatternNode)
							.addAll(this.newUnexpandedPatternsNodesOfNeo4jNodes.get(unexpandedPatternNode));
				} else {
					// b2
					for (PatternNode itSelfPatternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
						if (twoPatternNodesAreSame(unexpandedPatternNode, parentPrefixTreeNode, itSelfPatternNode,
								this)) {
							this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(itSelfPatternNode)
									.addAll(parentPrefixTreeNode.getData().getNewUnexpandedPatternsNodesOfNeo4jNodes()
											.get(unexpandedPatternNode));
						}
					}
				}
			}

		}

		if (this.newUnexpandedPatternsNodesOfNeo4jNodes == null) {
			this.newUnexpandedPatternsNodesOfNeo4jNodes = new HashMap<PatternNode, HashSet<Integer>>();
		}
		this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(srcAbstractPatternNode, new HashSet<Integer>());
		this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(destAbstractPatternNode, new HashSet<Integer>());

		if (this.newUnexpandedNodesOfPatternNodes == null) {
			this.newUnexpandedNodesOfPatternNodes = new HashMap<Integer, HashSet<PatternNode>>();
		}

		this.newUnexpandedNodesOfPatternNodes.putIfAbsent(srcDataGraphPatternNodeId, new HashSet<PatternNode>());
		this.newUnexpandedNodesOfPatternNodes.putIfAbsent(destDataGraphPatternNodeId, new HashSet<PatternNode>());

		this.newUnexpandedPatternsNodesOfNeo4jNodes.get(srcAbstractPatternNode).add(srcDataGraphPatternNodeId);
		this.newUnexpandedPatternsNodesOfNeo4jNodes.get(destAbstractPatternNode).add(destDataGraphPatternNodeId);

		this.newUnexpandedNodesOfPatternNodes.get(srcDataGraphPatternNodeId).add(srcAbstractPatternNode);
		this.newUnexpandedNodesOfPatternNodes.get(destDataGraphPatternNodeId).add(destAbstractPatternNode);

		for (PatternNode patternNode : parentPrefixTreeNode.getData().getMatchedNodes()
				.getDataGraphMatchNodeOfAbsPNode().keySet()) {
			if (patternNode != srcAbstractPatternNode && patternNode != destAbstractPatternNode) {
				if (this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode) != null) {
					this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).addAll(parentPrefixTreeNode.getData()
							.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode));
				} else {
					for (PatternNode thisPatternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
						if (twoPatternNodesAreSame(patternNode, parentPrefixTreeNode, thisPatternNode, this)) {
							this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(thisPatternNode)
									.addAll(parentPrefixTreeNode.getData().getMatchedNodes()
											.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
						}
					}
				}
			}
		}

		for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
			if (patternNode != srcAbstractPatternNode && patternNode != destAbstractPatternNode) {
				for (Integer nodeId : this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode)) {
					boolean newlyAdded = false;
					if (this.matchNodes.patternNodeOfNeo4jNode.get(nodeId) != null) {
						newlyAdded = this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
					} else {
						this.matchNodes.patternNodeOfNeo4jNode.putIfAbsent(nodeId, new HashSet<PatternNode>());
						newlyAdded = this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
					}
					if (newlyAdded) {
						// add to unexpanded:
						this.newUnexpandedNodesOfPatternNodes.putIfAbsent(nodeId, new HashSet<PatternNode>());
						this.newUnexpandedNodesOfPatternNodes.get(nodeId).add(patternNode);
						this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(patternNode, new HashSet<Integer>());
						this.newUnexpandedPatternsNodesOfNeo4jNodes.get(patternNode).add(nodeId);
					}
				}
			}
		}

		this.isVerified = false;

		if (DummyProperties.debugMode) {
			// if (this.patternPrefixTreeNodeIndex == 199) {
			// System.out.println();
			// }
			System.out.println("after updating new matches!");
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
			System.out.println("srcAbstractPatternNode: " + srcAbstractPatternNode + ", srcDataGraphPatternNodeId: "
					+ srcDataGraphPatternNodeId + ", srcPatternNode hashCode: " + srcAbstractPatternNode.hashCode());
			System.out.println("destAbstractPatternNode: " + destAbstractPatternNode + ", destDataGraphPatternNodeId: "
					+ destDataGraphPatternNodeId + ", destPatternNode hashCode: " + destAbstractPatternNode.hashCode());
			System.out.println("end");
			System.out.println();
		}
	}

	public void addNewMatchForUpdateDangling(PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			PatternNode destAbstractPatternNode, HashSet<Integer> destDataGraphPatternNodeIds,
			Indexer labelAdjacencyIndexer) {

		// if (this.patternPrefixTreeNodeIndex == 199) {
		// System.out.println();
		// }

		if (DummyProperties.debugMode) {
			System.out.println("before updating new matches!");
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
		}

		for (Integer destDataGraphPatternNodeId : destDataGraphPatternNodeIds)
			addNewMatch(destAbstractPatternNode, destDataGraphPatternNodeId, labelAdjacencyIndexer);

		if (this.isVisited)
			this.prefixTreeMode = PrefixTreeMode.UPDATE;

		// we have to inherit new matches from parents and consider them as new
		// matches also
		if (parentPrefixTreeNode.getData().getNewUnexpandedNodesOfPatternNodes() != null) {
			if (this.newUnexpandedNodesOfPatternNodes == null)
				this.newUnexpandedNodesOfPatternNodes = new HashMap<Integer, HashSet<PatternNode>>();
			for (Integer nodeId : parentPrefixTreeNode.getData().getNewUnexpandedNodesOfPatternNodes().keySet()) {
				this.newUnexpandedNodesOfPatternNodes.putIfAbsent(nodeId, new HashSet<PatternNode>());
				for (PatternNode unexpandedPatternNode : parentPrefixTreeNode.getData()
						.getNewUnexpandedNodesOfPatternNodes().get(nodeId)) {
					if (this.patternGraph.vertexSet().contains(unexpandedPatternNode)) {
						this.newUnexpandedNodesOfPatternNodes.get(nodeId).add(unexpandedPatternNode);
					} else {
						// find corresponding one:
						// we may come from a prefix node that it's not its
						// parent/superlinknode
						// so, parent pattern nodes are totally unrelated with
						// this
						for (PatternNode patternNode : this.patternGraph.vertexSet()) {
							if (twoPatternNodesAreSame(unexpandedPatternNode, parentPrefixTreeNode, patternNode,
									this)) {
								this.newUnexpandedNodesOfPatternNodes.get(nodeId).add(patternNode);
								break;
							}
						}

					}

				}

				if (this.matchNodes.patternNodeOfNeo4jNode.get(nodeId) != null) {
					this.matchNodes.patternNodeOfNeo4jNode.get(nodeId)
							.addAll(this.newUnexpandedNodesOfPatternNodes.get(nodeId));
				} else {
					// b2
					this.matchNodes.patternNodeOfNeo4jNode.put(nodeId, new HashSet<PatternNode>());
					HashSet<PatternNode> newUnexpandedPatternNodes = this.newUnexpandedNodesOfPatternNodes.get(nodeId);

					for (PatternNode patternNode : this.patternGraph.vertexSet()) {
						for (PatternNode unExpPN : newUnexpandedPatternNodes) {
							if (twoPatternNodesAreSame(unExpPN, parentPrefixTreeNode, patternNode, this)) {
								this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
							}
						}
					}
				}
			}
		}
		// TODO: may be bi-directional map helps us to reduce both bugs and
		// slowness
		if (parentPrefixTreeNode.getData().getNewUnexpandedNodesOfPatternNodes() != null) {
			if (this.newUnexpandedPatternsNodesOfNeo4jNodes == null)
				this.newUnexpandedPatternsNodesOfNeo4jNodes = new HashMap<PatternNode, HashSet<Integer>>();

			for (Integer nodeId : this.newUnexpandedNodesOfPatternNodes.keySet()) {
				for (PatternNode unexpandedPatternNode : this.newUnexpandedNodesOfPatternNodes.get(nodeId)) {
					this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(unexpandedPatternNode,
							new HashSet<Integer>());
					this.newUnexpandedPatternsNodesOfNeo4jNodes.get(unexpandedPatternNode).add(nodeId);
				}
			}

			for (PatternNode unexpandedPatternNode : this.newUnexpandedPatternsNodesOfNeo4jNodes.keySet()) {

				if (this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(unexpandedPatternNode) != null) {
					this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(unexpandedPatternNode)
							.addAll(this.newUnexpandedPatternsNodesOfNeo4jNodes.get(unexpandedPatternNode));
				} else {
					// b2
					for (PatternNode itSelfPatternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
						if (twoPatternNodesAreSame(unexpandedPatternNode, parentPrefixTreeNode, itSelfPatternNode,
								this)) {
							this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(itSelfPatternNode)
									.addAll(parentPrefixTreeNode.getData().getNewUnexpandedPatternsNodesOfNeo4jNodes()
											.get(unexpandedPatternNode));
						}
					}
				}
			}

		}

		if (this.newUnexpandedPatternsNodesOfNeo4jNodes == null) {
			this.newUnexpandedPatternsNodesOfNeo4jNodes = new HashMap<PatternNode, HashSet<Integer>>();
		}

		this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(destAbstractPatternNode, new HashSet<Integer>());

		if (this.newUnexpandedNodesOfPatternNodes == null) {
			this.newUnexpandedNodesOfPatternNodes = new HashMap<Integer, HashSet<PatternNode>>();
		}

		for (Integer destDataGraphPatternNodeId : destDataGraphPatternNodeIds)
			this.newUnexpandedNodesOfPatternNodes.putIfAbsent(destDataGraphPatternNodeId, new HashSet<PatternNode>());

		for (Integer destDataGraphPatternNodeId : destDataGraphPatternNodeIds)
			this.newUnexpandedPatternsNodesOfNeo4jNodes.get(destAbstractPatternNode).add(destDataGraphPatternNodeId);

		for (Integer destDataGraphPatternNodeId : destDataGraphPatternNodeIds)
			this.newUnexpandedNodesOfPatternNodes.get(destDataGraphPatternNodeId).add(destAbstractPatternNode);

		for (PatternNode patternNode : parentPrefixTreeNode.getData().getMatchedNodes()
				.getDataGraphMatchNodeOfAbsPNode().keySet()) {
			if (patternNode != destAbstractPatternNode) {
				if (this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode) != null) {
					this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).addAll(parentPrefixTreeNode.getData()
							.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode));
				} else {
					for (PatternNode thisPatternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
						if (twoPatternNodesAreSame(patternNode, parentPrefixTreeNode, thisPatternNode, this)) {
							this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(thisPatternNode)
									.addAll(parentPrefixTreeNode.getData().getMatchedNodes()
											.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
						}
					}
				}
			}
		}

		for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
			if (patternNode != destAbstractPatternNode) {
				for (Integer nodeId : this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode)) {
					if (this.matchNodes.patternNodeOfNeo4jNode.get(nodeId) != null) {
						this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
					} else {
						this.matchNodes.patternNodeOfNeo4jNode.putIfAbsent(nodeId, new HashSet<PatternNode>());
						this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
					}
				}
			}
		}

		this.isVerified = false;

		if (DummyProperties.debugMode) {
			// if (this.patternPrefixTreeNodeIndex == 199) {
			// System.out.println();
			// }
			System.out.println("after updating new matches!");
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
			System.out.println("destAbstractPatternNode: " + destAbstractPatternNode + ", destDataGraphPatternNodeId: "
					+ destDataGraphPatternNodeIds + ", destPatternNode hashCode: "
					+ destAbstractPatternNode.hashCode());
			System.out.println("end");
			System.out.println();
		}
	}

	private boolean twoPatternNodesAreSame(PatternNode unexpandedPatternNode,
			PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode, PatternNode patternNode,
			PrefixTreeNodeDataAlg1BiSim prefixTreeNodeData) {

		if (patternNode.getType().equals(unexpandedPatternNode.getType())
				&& prefixTreeNodeData.stepsFromRootOfPatternNodes.get(patternNode)
						.equals(parentPrefixTreeNode.getData().getStepsFromRootOfPatternNodes()
								.get(unexpandedPatternNode))
				&& ((prefixTreeNodeData.incomingRelTypesOfPatternNodes.get(patternNode) == null && parentPrefixTreeNode
						.getData().getIncomingRelTypesOfPatternNodes().get(unexpandedPatternNode) == null)
						|| prefixTreeNodeData.incomingRelTypesOfPatternNodes.get(patternNode)
								.equals(parentPrefixTreeNode.getData().getIncomingRelTypesOfPatternNodes()
										.get(unexpandedPatternNode)))) {
			return true;
		}

		return false;
	}

	public PatternNode getPatternRootNode() {
		return patternRootNode;
	}

	public String getMappedGraphString() {

		String returnValue = " << pattern index: " + this.patternPrefixTreeNodeIndex + " > ";

		int allMatchesForThisPattern = 0;
		// int allCandidatesForThisPattern = 0;
		if (this.matchNodes != null) {
			allMatchesForThisPattern = getNumerOfAllMatches();
		}
		// if (this.matchNodes != null) {
		// allCandidatesForThisPattern = getNumerOfAllCandidates();
		// }

		// returnValue += " allCandidatesForThisPattern: " +
		// allCandidatesForThisPattern + " , ";
		returnValue += " allMatchesForThisPattern: " + allMatchesForThisPattern + " > \n";

		if (this.patternGraph != null) {
			ArrayList<String> absGraphEdges = new ArrayList<String>();
			for (DefaultLabeledEdge e : this.patternGraph.edgeSet()) {
				absGraphEdges.add((this.patternGraph.getEdgeSource(e).getType()) + "_"
						+ this.patternGraph.getEdgeSource(e).hashCode() + "_" + e.getType() + "->"
						+ (this.patternGraph.getEdgeTarget(e).getType()) + "_"
						+ this.patternGraph.getEdgeTarget(e).hashCode() + ", ");
			}
			Collections.sort(absGraphEdges);

			for (String e : absGraphEdges) {
				returnValue += e;
			}
			returnValue += " >> ";

			// returnValue += "\n candidates: [ ";
			// for (PatternNode patternNode :
			// this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
			//
			// returnValue += " " + patternNode.getType() + "_" +
			// patternNode.hashCode() + "=>";
			// int c = 0;
			// returnValue += " ( ";
			// for (Integer nodeId :
			// this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode)) {
			// c++;
			// if (c > 100) {
			// returnValue += "...";
			// break;
			// }
			// returnValue += nodeId + ", ";
			//
			// }
			// returnValue += ")";
			//
			// }
			// returnValue += " ] ";

			if (this.matchNodes.dataGraphMatchNodeOfAbsPNode != null) {
				returnValue += "\n matches: [ ";
				for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {

					returnValue += " " + patternNode.getType() + "_" + patternNode.hashCode() + "=>";
					int c = 0;
					returnValue += " ( ";
					for (Integer nodeId : this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode)) {
						c++;
						if (c > 100) {
							returnValue += "...";
							break;
						}
						returnValue += nodeId + ", ";

					}
					returnValue += ")";

				}
				returnValue += " ] ";
			}

			returnValue += "\n srcPN? " + this.sourcePatternNode;
			returnValue += " tgtPN? " + this.targetPatternNode;
			returnValue += " isMFP? " + isMaximalFrequent;
			returnValue += " isVisited? " + isVisited;
			returnValue += " isValid? " + isValid;
			returnValue += " isVerified? " + this.isVerified;
			returnValue += " mode? " + this.prefixTreeMode;
			returnValue += " isDangling? false";
			returnValue += " edgeSet size: " + this.patternGraph.edgeSet().size();
			returnValue += " vertexSet size: " + this.patternGraph.vertexSet().size();
			returnValue += "\n";

			returnValue += "newUnexpandedNodesOfPatternNodes: " + this.newUnexpandedNodesOfPatternNodes + "\n";
			returnValue += "newUnexpandedPatternsNodesOfNeo4jNodes: " + this.newUnexpandedPatternsNodesOfNeo4jNodes
					+ "\n";
		}
		return returnValue;
	}

	@Override
	public String toString() {
		return this.getMappedGraphString();
	}

	public void setPatternAsInvalid(PrefixTreeNode<IPrefixTreeNodeData> thisNode, IPrefixTree prefixTree, int snapshot)
			throws Exception {

		// set pattern as an invalid pattern

		this.isValid = false;
		this.isVerified = false;
		this.isFrequent = false;
		this.isMaximalFrequent = false;
		// if it was in the border list it should be removed
		prefixTree.getMfpPrefixTreeNodes().remove(thisNode);
		this.canBeMaximalFrequent = false;

		// if it was in the topk list it should be removed and replaced
		if (prefixTree.getTopKFrequentPatterns() != null)
			thisNode.getData().removeFromTopK(prefixTree, thisNode);

	}

	public void setPatternAsUnEvaluated() {
		this.isVerified = false;

	}

	public void setSupportFrequency(int snapshot, double supp) {
		if (!DummyProperties.windowMode) {
			this.supportFrequency[snapshot] = supp;
			// because this the most updated version of support,
			// otherwise we've carried over to this snapshot
			this.totalSupportFrequency = this.supportFrequency[snapshot];
		} else {
			if (snapshot < DummyProperties.WINDOW_SIZE) {
				this.supportFrequencyWindowing.set(snapshot, supp);
				this.totalSupportFrequency += supp;
			} else {
				// because we are updating the last window always
				this.totalSupportFrequency += (supp - this.supportFrequencyWindowing.getLast());
			}
		}

	}

	/**
	 * public void setTotalUpperbound(int snapshot) {
	 * 
	 * this.totalUpperboundEstimation = 0.0d;
	 * 
	 * // until here we have a valid upperbound for each snapshot for (int i =
	 * 0; i < snapshot; i++) { this.totalUpperboundEstimation +=
	 * this.supportFrequency[i]; this.snapshotUB[i] = this.supportFrequency[i];
	 * }
	 * 
	 * this.totalUpperboundEstimation += this.snapshotUB[snapshot];
	 * 
	 * // from here we consider max possible upperbound for each snapshot for
	 * (int i = snapshot + 1; i < this.snapshotUB.length; i++) {
	 * this.snapshotUB[i] = 0; // this.totalUpperboundEstimation +=
	 * this.snapshotUB[i]; }
	 * 
	 * this.lastSeenSnapshot = snapshot;
	 * 
	 * }
	 * 
	 * 
	 * 
	 * /*
	 * 
	 * 
	 */

	public void setAsMFP(PrefixTreeNode<IPrefixTreeNodeData> thisNode,
			PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			List<PrefixTreeNode<IPrefixTreeNodeData>> superNodeLinks, IPrefixTree prefixTree, int snapshot)
			throws Exception {

		if (DummyProperties.debugMode) {
			System.out.println("make this node mfp: " + this);
		}

		this.isMaximalFrequent = true;
		prefixTree.getMfpPrefixTreeNodes().add(thisNode);
		this.isFrequent = true;
		this.isVerified = true;

		if (parentPrefixTreeNode != null) {
			parentPrefixTreeNode.getData().setMaximalFrequent(false, parentPrefixTreeNode, prefixTree);
			prefixTree.getMfpPrefixTreeNodes().remove(parentPrefixTreeNode);
			removeFromTopK(prefixTree, parentPrefixTreeNode);
			if (DummyProperties.debugMode) {
				System.out.println("make parent node non-mfp: " + parentPrefixTreeNode.getData());
			}
		}
		if (superNodeLinks != null) {
			for (PrefixTreeNode<IPrefixTreeNodeData> superNodeLink : superNodeLinks) {
				superNodeLink.getData().setMaximalFrequent(false, superNodeLink, prefixTree);
				prefixTree.getMfpPrefixTreeNodes().remove(superNodeLink);
				removeFromTopK(prefixTree, superNodeLink);
				if (DummyProperties.debugMode) {
					System.out.println("make parent node non-mfp: " + superNodeLink.getData());
				}
			}
		}

	}

	public void makeParentsMFPIfNoOtherFrequentChild(int snapshot, PrefixTreeNode<IPrefixTreeNodeData> thisNode,
			PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			List<PrefixTreeNode<IPrefixTreeNodeData>> superNodeLinks, IPrefixTree prefixTree) throws Exception {

		HashSet<Integer> seenPTNodes = new HashSet<Integer>();

		// because "thisNode" was MFP representative of all its ancestors, we
		// should find a MFP replacement for all of possible parent branches
		if (parentPrefixTreeNode != null && parentPrefixTreeNode.getData().isValid()) {
			findFreqOrNewMFPInAncestors(parentPrefixTreeNode, prefixTree, snapshot, seenPTNodes);
		}

		// because "thisNode" was MFP representative of all its ancestors, we
		// should find a MFP replacement for all of possible parent branches
		if (superNodeLinks != null) {
			for (PrefixTreeNode<IPrefixTreeNodeData> superNodeLink : superNodeLinks) {

				if (seenPTNodes.contains(superNodeLink.getData().getPatternPrefixTreeNodeIndex())) {
					continue;
				}

				if (superNodeLink.getData().isValid())
					findFreqOrNewMFPInAncestors(superNodeLink, prefixTree, snapshot, seenPTNodes);
			}
		}
	}

	private void findFreqOrNewMFPInAncestors(PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			IPrefixTree prefixTree, int snapshot, HashSet<Integer> seenPTNodes) throws Exception {

		// because we have multiple parentship(superlinks),
		// we have to do it with a queue.
		Queue<PrefixTreeNode<IPrefixTreeNodeData>> ancestorsQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();

		ancestorsQueue.add(parentPrefixTreeNode);

		// while there is a parent
		while (!ancestorsQueue.isEmpty()) {

			PrefixTreeNode<IPrefixTreeNodeData> tempParent = ancestorsQueue.poll();

			if (seenPTNodes.contains(tempParent.getData().getPatternPrefixTreeNodeIndex())) {
				continue;
			}

			seenPTNodes.add(tempParent.getData().getPatternPrefixTreeNodeIndex());

			// if it's not verified we should verify it first
			if (tempParent.getData().isValid()) {

				if (!tempParent.getData().isVerified()) {
					DualSimulationHandler.computeSupport(prefixTree.getDataGraph(), tempParent, snapshot, prefixTree);
				}
				// right now we are sure that it's verified, so if it's frequent
				// it can be mfp or not.
				// however, we don't need to go upper.
				if (tempParent.getData().isValid()
						&& tempParent.getData().getTotalSupportFrequency() >= prefixTree.getThreshold()) {
					checkIfThisCanBeMFPSetIt(tempParent, prefixTree, snapshot);
				}
			}

			// if the parent is invalid we checked this branch before
			// if it's valid but infrequent we should go up to check more
			if (tempParent.getData().isValid() && !tempParent.getData().isFrequent()) {
				if (tempParent.getParent() != null) {
					ancestorsQueue.add(tempParent.getParent());
				}
				if (tempParent.getSuperNodeLinks() != null) {
					ancestorsQueue.addAll(tempParent.getSuperNodeLinks());
				}
			}

		}

	}

	private boolean checkIfThisCanBeMFPSetIt(PrefixTreeNode<IPrefixTreeNodeData> parent, IPrefixTree prefixTree,
			int snapshot) throws Exception {
		// if non of my descendants is frequent I'm MFP!
		// otherwise i cannot be mfp
		Queue<PrefixTreeNode<IPrefixTreeNodeData>> descendantsQueue = new LinkedList<PrefixTreeNode<IPrefixTreeNodeData>>();

		descendantsQueue.addAll(parent.getChildren());
		if (parent.getLinkedNodes() != null) {
			descendantsQueue.addAll(parent.getLinkedNodes());
		}

		while (!descendantsQueue.isEmpty()) {
			PrefixTreeNode<IPrefixTreeNodeData> tempChild = descendantsQueue.poll();

			if (tempChild.getData().isValid() && !tempChild.getData().isVerified()) {
				DualSimulationHandler.computeSupport(prefixTree.getDataGraph(), tempChild, snapshot, prefixTree);
			}

			if (tempChild.getData().isValid() && tempChild.getData().isVerified()
					&& tempChild.getData().getTotalSupportFrequency() >= prefixTree.getThreshold()) {
				return false;
			}
		}

		parent.getData().setMaximalFrequent(true, parent, prefixTree);
		prefixTree.getMfpPrefixTreeNodes().add(parent);
		if (DummyProperties.debugMode) {
			System.out.println("make parent mfp: " + parent.getData());
		}
		return true;

	}

	public void freqToNonFreqHandling(PrefixTreeNode<IPrefixTreeNodeData> thisNode) {
		this.isFrequent = false;
	}

	public void maxFreqToNonFreqHandling(PrefixTreeNode<IPrefixTreeNodeData> thisNode, IPrefixTree prefixTree,
			int snapshot) throws Exception {
		// it cannot be maximal frequent
		// and we should find one mfp in its ancestors
		this.isMaximalFrequent = false;
		this.isFrequent = false;
		prefixTree.getMfpPrefixTreeNodes().remove(thisNode);
		removeFromTopK(prefixTree, thisNode);

		makeParentsMFPIfNoOtherFrequentChild(snapshot, thisNode, thisNode.getParent(), thisNode.getSuperNodeLinks(),
				prefixTree);

	}

	// maintaining at least k elements in the top-k if we have enough mfp
	public void removeFromTopK(IPrefixTree prefixTree, PrefixTreeNode<IPrefixTreeNodeData> thisNode) throws Exception {

		prefixTree.getTopKFrequentPatterns().remove(thisNode);

		// if (thisNode.getData().isValid && !thisNode.getData().isVerified) {
		// throw new Exception("not verified removed from top-k how to know if
		// it's mfp yet or not?!");
		// }

		// TODO: make sure that before we reach here, we verified it
		if (thisNode.getData().isMaximalFrequent())
			prefixTree.getMfpPrefixTreeNodes().add(thisNode);

	}

	public boolean addToTopK(IPrefixTree prefixTree, PrefixTreeNode<IPrefixTreeNodeData> thisNode) {

		if (!this.foundAllFocuses)
			return false;

		boolean isInTheTopkList = prefixTree.getTopKFrequentPatterns().contains(thisNode);

		// if it doesnt exist in the topk list
		// we should try to add it
		if (!isInTheTopkList)
			isInTheTopkList = prefixTree.getTopKFrequentPatterns().offer(thisNode);

		// if we could add it or it was in the topk before
		// we can remove it from mfp queue
		if (isInTheTopkList) {
			prefixTree.getMfpPrefixTreeNodes().remove(thisNode);
		}

		return isInTheTopkList;
	}

	public int getNumerOfAllMatches() {
		int allMatchesForThisPattern = 0;
		if (this.matchNodes != null) {
			for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
				allMatchesForThisPattern += this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).size();
			}
		}
		return allMatchesForThisPattern;
	}

	public void setMaximalFrequent(boolean isMaximalFrequent) {
		this.isMaximalFrequent = isMaximalFrequent;

	}

	public void setVerified(boolean isVerified) {
		this.isVerified = isVerified;

	}

	public void setVisited(boolean isVisited) {
		this.isVisited = isVisited;
	}

	public boolean getFoundAllFocuses() {
		return this.foundAllFocuses;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;

	}

	public String getRelationshipType() {
		return this.relationshipType;
	}

	public void updateNumberOfFrequentChildrenAndLinked(int updateValue) {
		this.numberOfFrequentChildrenAndLinked += updateValue;

	}

	public PrefixTreeMode getPrefixTreeMode() {
		return this.prefixTreeMode;
	}

	public void setCanBeMaximalFrequent(boolean canBeMFP) {
		this.canBeMaximalFrequent = canBeMFP;
	}

	public HashSet<String> getTypeOfUnSeenFocusNodes() {
		return this.typeOfUnSeenFocusNodes;
	}

	public void renewNewUnexpandedNodesOfPatternNodes() {
		this.newUnexpandedNodesOfPatternNodes = new HashMap<Integer, HashSet<PatternNode>>();
	}

	public void renewNewUnexpandedPatternsNodesOfNeo4jNodes() {
		this.newUnexpandedPatternsNodesOfNeo4jNodes = new HashMap<PatternNode, HashSet<Integer>>();

	}

	public void setFrequent(boolean isFrequent) {
		this.isFrequent = isFrequent;
	}

	public boolean canBeMaximalFrequent() {
		return canBeMaximalFrequent;
	}

	@Override
	public void setMaximalFrequent(boolean isMaximalFrequent, PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			IPrefixTree prefixTree) throws Exception {

		if (isMaximalFrequent != this.isMaximalFrequent) {
			this.isMaximalFrequent = isMaximalFrequent;
			if (isMaximalFrequent) {
				if (!prefixTree.getMfpPrefixTreeNodes().contains(prefixTreeNode))
					prefixTree.getMfpPrefixTreeNodes().add(prefixTreeNode);

				addToTopK(prefixTree, prefixTreeNode);

				for (PrefixTreeNode<IPrefixTreeNodeData> child : prefixTreeNode.getChildren()) {
					setMinimalInFrequent(true, child, prefixTree);
				}
				if (prefixTreeNode.getLinkedNodes() != null) {
					for (PrefixTreeNode<IPrefixTreeNodeData> child : prefixTreeNode.getLinkedNodes()) {
						setMinimalInFrequent(true, child, prefixTree);
					}
				}

			} else {
				prefixTree.getMfpPrefixTreeNodes().remove(prefixTreeNode);
				prefixTree.getTopKFrequentPatterns().remove(prefixTreeNode);
			}
		}

	}

	@Override
	public void addImmediateMatches(PatternNode possiblePatternNode, int nodeId) throws Exception {
		throw new Exception("not implemented exception");

	}

	@Override
	public void setPrefixTreeMode(PrefixTreeMode newMode) {
		this.prefixTreeMode = newMode;
	}

	@Override
	public boolean isMinimalInFrequent() throws Exception {
		return this.isMinimalInFrequent;
	}

	@Override
	public void setMinimalInFrequent(boolean isMIP, PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			IPrefixTree prefixTree) throws Exception {

		if (this.isMinimalInFrequent != isMIP) {
			this.isMinimalInFrequent = isMIP;
			if (this.isMinimalInFrequent) {
				if (!prefixTree.getMipPrefixTreeNodes().contains(prefixTreeNode))
					prefixTree.getMipPrefixTreeNodes().add(prefixTreeNode);
			} else {
				prefixTree.getMipPrefixTreeNodes().remove(prefixTreeNode);
			}
		}

	}

	@Override
	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfNextNeighborOfSameType() {
		return this.frequencyOfNextNeighborOfSameType;
	}

	@Override
	public void setPatternAsIncorrect(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, IPrefixTree prefixTreeOpt,
			int snapshot) throws Exception {
		this.isCorrect = false;
		this.canBeMaximalFrequent = false;
		this.setMaximalFrequent(false, tempProcessingNode, prefixTreeOpt);

	}

	@Override
	public boolean isCorrect() {
		return this.isCorrect;
	}

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodesOfStepsFromRoot() {
		return null;
	}
	// public void addRelType(String relationshipType) {
	//
	// if (relationshipType != null) {
	// if (incomingRelTypes.containsKey(relationshipType)) {
	// incomingRelTypes.put(relationshipType,
	// incomingRelTypes.get(relationshipType) + 1);
	// } else {
	// incomingRelTypes.put(relationshipType, 1);
	// }
	// }
	//
	// }
	//
	// public void removeRelType(String relationshipType) {
	// incomingRelTypes.put(relationshipType,
	// incomingRelTypes.get(relationshipType) - 1);
	// }

	@Override
	public void setCorrectness(boolean isCorrect, PrefixTreeNode<IPrefixTreeNodeData> thisNode, IPrefixTree prefixTree,
			int snapshot) throws Exception {
		this.isCorrect = isCorrect;

		if (isMaximalFrequent) {
			prefixTree.getMfpPrefixTreeNodes().remove(thisNode);
			prefixTree.getTopKFrequentPatterns().remove(thisNode);
			this.isMaximalFrequent = false;
		}
		if (isMinimalInFrequent) {
			prefixTree.getMipPrefixTreeNodes().remove(thisNode);
			this.isMinimalInFrequent = false;
		}
		// if (!isCorrect) {
		// if (isMaximalFrequent) {
		// maxFreqToNonFreqHandling(thisNode, prefixTree, snapshot);
		// } else if (isFrequent) {
		// freqToNonFreqHandling(thisNode);
		// }
		// }
	}

	@Override
	public boolean isDanglingPattern() {
		return isDanglingPattern;
	}

	@Override
	public Double getSupportFrequency(int snapshot) {
		if (!DummyProperties.windowMode)
			return supportFrequency[snapshot];
		else {
			if (snapshot < DummyProperties.WINDOW_SIZE)
				return supportFrequencyWindowing.get(snapshot);
			else
				return totalSupportFrequency;
		}
	}

	@Override
	public Double[] getSupportFrequencies() {
		if (!DummyProperties.windowMode)
			return this.supportFrequency;
		else {
			return this.supportFrequencyWindowing.toArray(new Double[this.supportFrequencyWindowing.size()]);
		}
	}

	@Override
	public void shiftSupportsValues() {
		this.totalSupportFrequency -= this.supportFrequencyWindowing.getFirst();

		this.supportFrequencyWindowing.removeFirst();
		this.supportFrequencyWindowing.addLast(0d);
	}

	@Override
	public HashSet<PatternNode> getPatternRootNodes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfPrevNeighborOfSameType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Direction getGrowthDirection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPatternRootNode(PatternNode targetPatternNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removePatternRootNode(PatternNode oldRootNode) {
		// TODO Auto-generated method stub
		
	}
}

/// **
// * usage: previous seen pattern so it's without any concrete match
// *
// * @param newAbsPattern
// * @param patternRootNode
// * @param srcAbstractPatternNode
// * @param destAbstractPatternNode
// * @param relationshipType
// */
// @Deprecated
// public PrefixTreeNodeData(ListenableDirectedGraph<PatternNode,
/// DefaultLabeledEdge> newAbsPattern,
// PatternNode patternRootNode, PatternNode srcAbstractPatternNode, PatternNode
/// destAbstractPatternNode,
// String relationshipType) {
//
// basicOperation(newAbsPattern, patternRootNode, srcAbstractPatternNode,
/// destAbstractPatternNode,
// relationshipType);
// }