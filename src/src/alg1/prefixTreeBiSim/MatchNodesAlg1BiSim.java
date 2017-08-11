package src.alg1.prefixTreeBiSim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import src.utilities.PatternNode;
import src.base.IMatchNodes;
import src.utilities.Dummy.DummyFunctions;

public class MatchNodesAlg1BiSim implements IMatchNodes {
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

	public MatchNodesAlg1BiSim(HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode,
			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode/*
																			 * ,
																			 * Long
																			 * dgPatternRootNodeId
																			 */) {
		this.dataGraphMatchNodeOfAbsPNode = dataGraphMatchNodeOfAbsPNode;
		this.patternNodeOfNeo4jNode = patternNodeOfNeo4jNode;
		// this.dgPatternRootNodeId = dgPatternRootNodeId;
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

	// public void printPatternNodeOfNeo4jNode() {
	// DummyFunctions.printIfDebug("printPatternNodeOfNeo4jNode");
	// for (Long nodeId : patternNodeOfNeo4jNode.keySet()) {
	// DummyFunctions.printIfDebug(nodeId + " --> " +
	// patternNodeOfNeo4jNode.get(nodeId).getType() + "_"
	// + patternNodeOfNeo4jNode.get(nodeId).isFocus());
	// }
	// }

}
