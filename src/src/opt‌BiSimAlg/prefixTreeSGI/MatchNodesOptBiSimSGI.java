package src.opt‌BiSimAlg.prefixTreeSGI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import src.utilities.PatternNode;
import src.base.IMatchNodes;
import src.utilities.Dummy.DummyFunctions;

public class MatchNodesOptBiSimSGI implements IMatchNodes {
	// public DirectedGraph<DataGraphMatchNode, DefaultEdge> concreteMatchGraph;
	public HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode;
	public HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode;
	// public Long dgPatternRootNodeId;
	// private boolean isDone;

	// public HashMap<PatternNode, HashSet<Integer>>
	// dataGraphCandidateNodeOfAbsPNode;
	// public HashMap<Integer, HashSet<PatternNode>>
	// patternNodeOfCandidateNeo4jNode;

	public HashMap<PatternNode, HashSet<Integer>> getDataGraphMatchNodeOfAbsPNode() {
		return dataGraphMatchNodeOfAbsPNode;
	}

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfNeo4jNode() {
		return patternNodeOfNeo4jNode;
	}

	// public HashMap<PatternNode, HashSet<Integer>>
	// getDataGraphCandidateNodeOfAbsPNode() {
	// return dataGraphCandidateNodeOfAbsPNode;
	// }

	// public HashMap<Integer, HashSet<PatternNode>>
	// getPatternNodeOfCandidateNeo4jNode() {
	// return patternNodeOfCandidateNeo4jNode;
	// }

	public MatchNodesOptBiSimSGI(HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode,
			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode) {
		this.dataGraphMatchNodeOfAbsPNode = dataGraphMatchNodeOfAbsPNode;
		this.patternNodeOfNeo4jNode = patternNodeOfNeo4jNode;

	}

	public String getDataGraphMatchOfAbsPNode() {
		String returnValue = "";
		for (PatternNode pNode : dataGraphMatchNodeOfAbsPNode.keySet()) {
			returnValue += pNode + "--> " + dataGraphMatchNodeOfAbsPNode.get(pNode) + "\n";
		}
		return returnValue;
	}

	public void setDataGraphMatchNodeOfAbsPNode(HashMap<PatternNode, HashSet<Integer>> result) {
		this.dataGraphMatchNodeOfAbsPNode = result;
		// this.patternNodeOfNeo4jNode = new HashMap<>();

	}

	// public void printPatternNodeOfNeo4jNode() {
	// DummyFunctions.printIfDebug("printPatternNodeOfNeo4jNode");
	// for (Long nodeId : patternNodeOfNeo4jNode.keySet()) {
	// DummyFunctions.printIfDebug(nodeId + " --> " +
	// patternNodeOfNeo4jNode.get(nodeId).getType() + "_"
	// + patternNodeOfNeo4jNode.get(nodeId).isFocus());
	// }
	// }

}
