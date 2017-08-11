package src.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgrapht.graph.ListenableDirectedGraph;

import src.alg1.prefixTree.PrefixTreeAlg1;
import src.utilities.DefaultLabeledEdge;
import src.utilities.Dummy.DummyProperties.Direction;
import src.utilities.Dummy.DummyProperties.PrefixTreeMode;
import src.utilities.Indexer;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;

public interface IPrefixTreeNodeData {
	public double getTotalSupportFrequency();

	//public void setTotalSupportFrequency(double totalSupportFrequency);

	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> getPatternGraph();

	public void setPatternGraph(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph);

	public int getPatternPrefixTreeNodeIndex();

	public void setPatternPrefixTreeNodeIndex(int patternPrefixTreeNodeIndex);

	public HashMap<Integer, HashSet<PatternNode>> getNewUnexpandedNodesOfPatternNodes();

	public IMatchNodes getMatchedNodes();

	public HashMap<PatternNode, HashMap<String, Integer>> getIncomingRelTypesOfPatternNodes();

	public HashMap<PatternNode, Integer> getStepsFromRootOfPatternNodes();

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodesOfStepsFromRoot();

	public boolean isMaximalFrequent();

	public boolean isValid();

	public boolean isVerified();

	public boolean isVisited();

	public PatternNode getSourcePatternNode();

	public PatternNode getTargetPatternNode();

	public HashMap<PatternNode, HashSet<Integer>> getNewUnexpandedPatternsNodesOfNeo4jNodes();

	public void setPatternAsInvalid(PrefixTreeNode<IPrefixTreeNodeData> thisNode, IPrefixTree prefixTree, int snapshot)
			throws Exception;

	public String getMappedGraphString();

	public void addNewMatchForUpdate(PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			PatternNode srcAbstractPatternNode, Integer srcDataGraphPatternNodeId, PatternNode destAbstractPatternNode,
			Integer destDataGraphPatternNodeId, Indexer labelAdjacencyIndexer);

	public HashSet<PatternNode> getPatternRootNodes();
	public PatternNode getPatternRootNode();

	public void setPatternAsUnEvaluated();

	public void setSupportFrequency(int snapshot, double supp, double alpha, boolean fromCarryOver);

	public void setAsMFP(PrefixTreeNode<IPrefixTreeNodeData> thisNode,
			PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			List<PrefixTreeNode<IPrefixTreeNodeData>> superNodeLinks, IPrefixTree prefixTree, int snapshot)
			throws Exception;

	public void makeParentsMFPIfNoOtherFrequentChild(int snapshot, PrefixTreeNode<IPrefixTreeNodeData> thisNode,
			PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			List<PrefixTreeNode<IPrefixTreeNodeData>> superNodeLinks, IPrefixTree prefixTree, double alpha) throws Exception;

	public void freqToNonFreqHandling(PrefixTreeNode<IPrefixTreeNodeData> thisNode);

	public void maxFreqToNonFreqHandling(PrefixTreeNode<IPrefixTreeNodeData> thisNode, IPrefixTree prefixTree,
			int snapshot, double alpha) throws Exception;

	public void removeFromTopK(IPrefixTree prefixTree, PrefixTreeNode<IPrefixTreeNodeData> thisNode) throws Exception;

	public boolean addToTopK(IPrefixTree prefixTree, PrefixTreeNode<IPrefixTreeNodeData> thisNode);

	public int getNumerOfAllMatches();

	public void setMaximalFrequent(boolean isMaximalFrequent, PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			IPrefixTree prefixTree) throws Exception;

	public boolean isFrequent();

	public void setVerified(boolean isVerified);

	public Double getSupportFrequency(int snapshot);

	public void setVisited(boolean isVisited);

	public boolean getFoundAllFocuses();

	public void setValid(boolean isValid);

	public String getRelationshipType();

	public void updateNumberOfFrequentChildrenAndLinked(int updateValue);

	public PrefixTreeMode getPrefixTreeMode();

	public void addNewMatch(PatternNode destPatternNode, Integer newNodeId, Indexer indexer);

	public void setCanBeMaximalFrequent(boolean b);

	public HashSet<String> getTypeOfUnSeenFocusNodes();

	public void renewNewUnexpandedNodesOfPatternNodes();

	public void renewNewUnexpandedPatternsNodesOfNeo4jNodes();

	public void setFrequent(boolean isFrequent);

	public boolean canBeMaximalFrequent();

	public Double[] getSupportFrequencies();

	public void addImmediateMatches(PatternNode possiblePatternNode, int nodeId) throws Exception;

	public void setPrefixTreeMode(PrefixTreeMode newMode);

	public boolean isMinimalInFrequent() throws Exception;

	public void setMinimalInFrequent(boolean isMIP, PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode,
			IPrefixTree prefixTree) throws Exception;

	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfNextNeighborOfSameType();
	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfPrevNeighborOfSameType();

	public void setPatternAsIncorrect(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, IPrefixTree prefixTreeOpt,
			int snapshot) throws Exception;

	public boolean isCorrect();
	// public HashMap<Integer, HashSet<PatternNode>>
	// getNewUnexpandedNodesOfPatternNodes();
	// public HashMap<PatternNode, HashSet<Integer>>
	// getNewUnexpandedPatternsNodesOfNeo4jNodes();

	public void setCorrectness(boolean isCorrect, PrefixTreeNode<IPrefixTreeNodeData> thisNode, IPrefixTree prefixTree,
			int snapshot) throws Exception;

	public boolean isDanglingPattern();

	public void addNewMatchForUpdateDangling(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			PatternNode danglingPatternNode, HashSet<Integer> remainingNodeIds, Indexer labelAdjacencyIndexer);

	//public boolean getUpdatedAt(int i);

	public void shiftSupportsValues();

	public Direction getGrowthDirection();

	public void setPatternRootNode(PatternNode targetPatternNode);

	void removePatternRootNode(PatternNode oldRootNode);

	public void addNewMatchSet(PatternNode tempDestPatternNode, HashSet<Integer> newNodeIds,
			Indexer labelAdjacencyIndexer);

	public void addNewMatchSetForUpdate(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode,
			PatternNode tempSrcPatternNode, Integer srcDataGpNodeId, PatternNode destPtn, HashSet<Integer> newNodeIds,
			Indexer labelAdjacencyIndexer);

	void addNewMatchSetForUpdate(PrefixTreeNode<IPrefixTreeNodeData> parentPrefixTreeNode,
			PatternNode srcAbstractPatternNode, HashSet<Integer> srcDataGraphPatternNodeId,
			PatternNode destAbstractPatternNode, HashSet<Integer> destDataGraphPatternNodeIds,
			Indexer labelAdjacencyIndexer);
}
