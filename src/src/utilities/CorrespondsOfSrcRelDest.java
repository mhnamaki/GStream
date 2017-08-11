package src.utilities;

import java.util.HashSet;

public class CorrespondsOfSrcRelDest {

	public HashSet<Integer> patternGraphsIndex = new HashSet<Integer>();
	public HashSet<PatternNode> possibleSrcPatternNodes = new HashSet<PatternNode>();
	public HashSet<PatternNode> possibleDestPatternNodes = new HashSet<PatternNode>();

	public CorrespondsOfSrcRelDest() {

	}

	public CorrespondsOfSrcRelDest(HashSet<Integer> patternGraphsIndex, HashSet<PatternNode> possibleSrcPatternNodes,
			HashSet<PatternNode> possibleDestPatternNodes) {
		this.patternGraphsIndex = patternGraphsIndex;
		this.possibleSrcPatternNodes = possibleSrcPatternNodes;
		this.possibleDestPatternNodes = possibleDestPatternNodes;
	}

	public void addCorresponingPattern(Integer patternGraphIndex) {
		this.patternGraphsIndex.add(patternGraphIndex);
	}

	public void addCorresponingSrcPatternNode(PatternNode possibleSrcPatternNode) {
		this.possibleSrcPatternNodes.add(possibleSrcPatternNode);
	}

	public void addCorresponingDestPatternNode(PatternNode possibleDestPatternNode) {
		this.possibleDestPatternNodes.add(possibleDestPatternNode);
	}

	public void addCorresponding(Integer patternGraphIndex, PatternNode srcAbstractPatternNode,
			PatternNode destAbstractPatternNode) {

		patternGraphsIndex.add(patternGraphIndex);
		possibleSrcPatternNodes.add(srcAbstractPatternNode);
		possibleDestPatternNodes.add(destAbstractPatternNode);

	}

}