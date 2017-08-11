package src.utilities;

import java.util.HashSet;

public class GoBackToPrevHolder {
	public HashSet<Integer> newNodeIds;
	public boolean goBackToPrev;
	public PatternNode destPatternNode;
	// int destStepsFromRoot;

	public GoBackToPrevHolder(HashSet<Integer> newNodeIds, boolean goBackToPrev,
			PatternNode destPatternNode/*
										 * , int destStepsFromRoot
										 */) {
		this.newNodeIds = newNodeIds;
		this.goBackToPrev = goBackToPrev;
		this.destPatternNode = destPatternNode;
		// this.destStepsFromRoot = destStepsFromRoot;
	}
}
