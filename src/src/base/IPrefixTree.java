package src.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.MinMaxPriorityQueue;

import src.utilities.Bitmap;
import src.utilities.Bitmap2;
import src.utilities.DefaultLabeledEdge;
import src.utilities.Indexer;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;

public interface IPrefixTree {

	MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> getTopKFrequentPatterns();

	PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> getMfpPrefixTreeNodes();

	HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>> getPrefixTreeNodeIndex();

	Indexer getLabelAdjacencyIndexer();

	GraphDatabaseService getDataGraph();

	double getThreshold();

	Bitmap getBitmap();

	PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> getMipPrefixTreeNodes();

	boolean preIsoChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern);

	VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsomorphism(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern);

	public double getDurationOfIsoChecking();

	public double getDurationOfBiSimChecking();

	public double getDurationOfNewPrefixTreeGeneration();

	public long getNumberOfIsoCheckingRequest();

	public long getNumberOfRealIsoChecking();

	public int getNumberOfComputeSupport();
	
	public long getNumberOfBiSimCheckingRequest();
	
	public long getNumberOfRealBiSimChecking();

	public void incNumberOfComputeSupport();

	public double getDurationOfComputeSupport();

	public void updateDurationOfComputeSupport(double newDuration);

	public void resetNumberOfIsoChecking();

	public void resetDurationOfIsoChecking();

	public void resetNumberOfComputeSupport();

	public void resetDurationOfComputeSupport();

	public void resetDurationOfNewPrefixTreeGeneration();

	public void incrementBiSimCheckingRequest();

	public void incrementRealBiSimChecking();

	public void updateDurationOfBiSimChecking(double newDuration);

	void resetDurationOfBiSimChecking();

}
