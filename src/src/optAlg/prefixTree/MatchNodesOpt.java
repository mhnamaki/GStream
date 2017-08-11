package src.optAlg.prefixTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import src.utilities.PatternNode;
import src.base.IMatchNodes;
import src.utilities.Dummy.DummyFunctions;

public class MatchNodesOpt implements IMatchNodes {
	// public DirectedGraph<DataGraphMatchNode, DefaultEdge> concreteMatchGraph;
	public HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode;
	public HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode;
	// public Long dgPatternRootNodeId;
	// private boolean isDone;

	public HashMap<PatternNode, HashSet<Integer>> getDataGraphMatchNodeOfAbsPNode() {
		return dataGraphMatchNodeOfAbsPNode;
	}

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfNeo4jNode() {
		return patternNodeOfNeo4jNode;
	}

	public MatchNodesOpt(HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode,
			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode) {
		this.dataGraphMatchNodeOfAbsPNode = dataGraphMatchNodeOfAbsPNode;
		this.patternNodeOfNeo4jNode = patternNodeOfNeo4jNode;
	}

	// public void setDone() {
	// isDone = true;
	// }
	//
	// public boolean isDone() {
	// return isDone;
	// }

	public String getDataGraphMatchOfAbsPNode() {
		String returnValue = "";
		for (PatternNode pNode : dataGraphMatchNodeOfAbsPNode.keySet()) {
			returnValue += pNode + "--> " + dataGraphMatchNodeOfAbsPNode.get(pNode) + "\n";
		}
		return returnValue;
	}

	public void setDataGraphMatchNodeOfAbsPNode(HashMap<PatternNode, HashSet<Integer>> result) {
		this.dataGraphMatchNodeOfAbsPNode = result;

	}

	// @Override
	// public HashMap<Integer, HashSet<PatternNode>>
	// getPatternNodeOfCandidateNeo4jNode() {
	// // TODO Auto-generated method stub
	// return null;
	// }

	// @Override
	// public HashMap<PatternNode, HashSet<Integer>>
	// getDataGraphCandidateNodeOfAbsPNode() {
	// TODO Auto-generated method stub
	// return null;
	// }

	// public void printPatternNodeOfNeo4jNode() {
	// DummyFunctions.printIfDebug("printPatternNodeOfNeo4jNode");
	// for (Long nodeId : patternNodeOfNeo4jNode.keySet()) {
	// DummyFunctions.printIfDebug(nodeId + " --> " +
	// patternNodeOfNeo4jNode.get(nodeId).getType() + "_"
	// + patternNodeOfNeo4jNode.get(nodeId).isFocus());
	// }
	// }

}
