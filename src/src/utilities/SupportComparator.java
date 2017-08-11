 package src.utilities;

import java.util.Comparator;

import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.base.IPrefixTreeNodeData;

public class SupportComparator implements Comparator<PrefixTreeNode<IPrefixTreeNodeData>> {

	// TODO: why we have some null data or null prefixtree node in MFP list
	// @Override
	// public int compare(PrefixTreeNode<PrefixTreeNodeData> o1,
	// PrefixTreeNode<PrefixTreeNodeData> o2) {
	// double totalSupportFrequency2 = 0;
	// double totalSupportFrequency1 = 0;
	// if (o2 != null && o2.getData() != null)
	// totalSupportFrequency2 = o2.getData().totalSupportFrequency;
	//
	// if (o1 != null && o1.getData() != null)
	// totalSupportFrequency1 = o1.getData().totalSupportFrequency;
	//
	// int result1 = Double.compare(totalSupportFrequency2,
	// totalSupportFrequency1);
	// if (result1 != 0)
	// return result1;
	//
	// int vSize1 = 0;
	// int vSize2 = 0;
	// if (o2 != null && o2.getData() != null && o2.getData().patternGraph !=
	// null)
	// vSize2 = o2.getData().patternGraph.vertexSet().size();
	//
	// if (o1 != null && o1.getData() != null && o1.getData().patternGraph !=
	// null)
	// vSize1 = o1.getData().patternGraph.vertexSet().size();
	//
	// // if having same support what about number of their nodes?
	// int result2 = Integer.compare(vSize2, vSize1);
	// if (result2 != 0)
	// return result2;
	//
	// // if having same support and number of nodes, what about number of
	// // their edges?
	// int eSize1 = 0;
	// int eSize2 = 0;
	// if (o2 != null && o2.getData() != null && o2.getData().patternGraph !=
	// null)
	// eSize2 = o2.getData().patternGraph.edgeSet().size();
	//
	// if (o1 != null && o1.getData() != null && o1.getData().patternGraph !=
	// null)
	// eSize1 = o1.getData().patternGraph.edgeSet().size();
	//
	// int result3 = Integer.compare(eSize2, eSize1);
	// return result3;
	//
	// }

	@Override
	public int compare(PrefixTreeNode<IPrefixTreeNodeData> o1, PrefixTreeNode<IPrefixTreeNodeData> o2) {

		int result1 = Double.compare(o2.getData().getTotalSupportFrequency(), o1.getData().getTotalSupportFrequency());
		if (result1 != 0)
			return result1;

		// if having same support what about number of their nodes?
		int result2 = Integer.compare(o2.getData().getPatternGraph().vertexSet().size(),
				o1.getData().getPatternGraph().vertexSet().size());
		if (result2 != 0)
			return result2;

		// if having same support and number of nodes, what about number of
		// their edges?
		int result3 = Integer.compare(o2.getData().getPatternGraph().edgeSet().size(),
				o1.getData().getPatternGraph().edgeSet().size());
		return result3;

	}

}