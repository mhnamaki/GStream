package src.utilities;
 
import src.base.IPrefixTree;
import src.optSubAlg.prefixTree.PrefixTreeSubOpt;

public class WinMine {
	public static void winMine(IPrefixTree prefixTree, int snapshot) {
		// TODO: we may just update supports for MFP and MIP patterns.
		for (Integer patternTreeNodeIndex : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData().shiftSupportsValues();
		}

	}
}
