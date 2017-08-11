package src.finalAlgorithm.prefixTree;

import java.util.HashSet;

import src.base.IPrefixTreeNodeData;
import src.utilities.PrefixTreeNode;

public class BiSimGroup {
	PrefixTreeNode<IPrefixTreeNodeData> minRepresentative;
	HashSet<PrefixTreeNode<IPrefixTreeNodeData>> groupOfBiSimPrefixTreeNodes = new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>();

	public BiSimGroup(PrefixTreeNode<IPrefixTreeNodeData> minRepresentative) {
		this.minRepresentative = minRepresentative;
		groupOfBiSimPrefixTreeNodes.add(minRepresentative);
	}
}
