package src.IsomorphismTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.utilities.DefaultLabeledEdge;
import src.utilities.Dummy;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;

public class IsoTester {
	public static double computeSGISupport(GraphDatabaseService dataGraph,
			PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode, int snapshot, IPrefixTree prefixTree)
			throws Exception {

		prefixTree.incNumberOfComputeSupport();

		double startTime = System.nanoTime();

		if (!tempProcessingNode.getData().isValid()) {
			throw new Exception(
					"non-valid patterns shouldn't be check for dual-sim, because they lose other match nodes!");
		}

		tempProcessingNode.getData().setVerified(true);

		Map<PatternNode, HashSet<Integer>> dsim = cutomizedMatchListSGI(tempProcessingNode, dataGraph, prefixTree);

		// compute support - should be in method
		double count = 0;
		for (PatternNode ptNode : dsim.keySet()) {
			if (ptNode.isFocus()) {
				count += dsim.get(ptNode).size();
			}
		}

		double snapshotSupp = count / Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
		tempProcessingNode.getData().setSupportFrequency(snapshot, snapshotSupp);

		if (count == 0) {
			tempProcessingNode.getData().setPatternAsInvalid(tempProcessingNode, prefixTree, snapshot);
		}

		prefixTree.updateDurationOfComputeSupport((System.nanoTime() - startTime) / 1e6);

		return tempProcessingNode.getData().getSupportFrequency(snapshot);
	}

	private static HashMap<PatternNode, HashSet<Integer>> cutomizedMatchListSGI(
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
				.setDataGraphMatchNodeOfAbsPNode(customizedMatchList(dataGraph,
						tempProcessingNode.getData().getPatternGraph(),
						tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode(), prefixTree));

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

	private static HashMap<PatternNode, HashSet<Integer>> customizedMatchList(GraphDatabaseService dataGraph,
			DirectedGraph<PatternNode, DefaultLabeledEdge> queryGraph, HashMap<PatternNode, HashSet<Integer>> dsim,
			IPrefixTree prefixTree) {

		BiMap<PatternNode, Integer> patternGraphIdMap;
		patternGraphIdMap = HashBiMap.create();
		int patternNodeId = 0;
		patternGraphIdMap.clear();

		for (PatternNode patternNode : queryGraph.vertexSet()) {
			patternGraphIdMap.put(patternNode, patternNodeId++);
		}

		// based on new induced graph index ids not neo4j node ids
		HashMap<PatternNode, HashSet<Integer>> newDsim = new HashMap<PatternNode, HashSet<Integer>>();
		for (PatternNode patternNode : dsim.keySet()) {
			newDsim.put(patternNode, new HashSet<Integer>());
			for (Integer neo4jNodeId : dsim.get(patternNode)) {
				newDsim.get(patternNode)
						.add(prefixTree.getLabelAdjacencyIndexer().neo4jNodeOfVf2Index.inverse().get(neo4jNodeId));
			}
		}

		VF2MatcherInduced vf2Matcher = new VF2MatcherInduced();
		HashMap<PatternNode, HashSet<Integer>> resultsMap = vf2Matcher.matchWithInitialMappedNode(
				prefixTree.getLabelAdjacencyIndexer().inducedGraphForVF2, queryGraph, newDsim, patternGraphIdMap,
				prefixTree);

		// based on neo4j node ids not new induced graph index ids
		dsim.clear();
		for (PatternNode patternNode : resultsMap.keySet()) {
			dsim.put(patternNode, new HashSet<Integer>());
			for (Integer nodeIndex : resultsMap.get(patternNode)) {
				dsim.get(patternNode)
						.add(prefixTree.getLabelAdjacencyIndexer().neo4jNodeOfVf2Index.get(nodeIndex));
			}
		}

		return dsim;

	}
}
