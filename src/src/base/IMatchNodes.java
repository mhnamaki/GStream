package src.base;

import java.util.HashMap;
import java.util.HashSet;

import src.utilities.PatternNode;

public interface IMatchNodes {
	public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfNeo4jNode();
	public HashMap<PatternNode, HashSet<Integer>> getDataGraphMatchNodeOfAbsPNode();
	//public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfCandidateNeo4jNode();
	//public HashMap<PatternNode, HashSet<Integer>> getDataGraphCandidateNodeOfAbsPNode();
	public void setDataGraphMatchNodeOfAbsPNode(HashMap<PatternNode, HashSet<Integer>> result);
}
