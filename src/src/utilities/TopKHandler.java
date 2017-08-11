package src.utilities;
 
import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.collect.MinMaxPriorityQueue;

import src.alg1.prefixTree.PrefixTreeAlg1;

import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;

public class TopKHandler {
	/**
	 * This function will guarantee that we generate our top-k in maximal
	 * frequent ones so top-k don't include each other.
	 * 
	 * @throws Exception
	 **/
	public static void findTopK(MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns,
			IPrefixTree prefixTree, int k, PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode, double threshold)
			throws Exception {

		HashSet<Integer> visitedPrefixTreeNodes = new HashSet<Integer>();
		findTopK(topKFrequentPatterns, prefixTree, k, prefixTreeNode, threshold, visitedPrefixTreeNodes);

	}

	private static void findTopK(MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns,
			IPrefixTree prefixTree, int k, PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode, double threshold,
			HashSet<Integer> visitedPrefixTreeNodes) throws Exception {

		// if (visitedPrefixTreeNodes.isEmpty()) {
		// for (Integer patternId :
		// prefixTree.getPrefixTreeNodeIndex().keySet()) {
		// prefixTree.getPrefixTreeNodeIndex().get(patternId).getData().setCanBeMaximalFrequent(true);
		// }
		// }

		visitedPrefixTreeNodes.add(prefixTreeNode.getData().getPatternPrefixTreeNodeIndex());

		ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> childrenPrefixTreeNodes = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();
		childrenPrefixTreeNodes.addAll(prefixTreeNode.getChildren());
		if (prefixTreeNode.getLinkedNodes() != null) {
			childrenPrefixTreeNodes.addAll(prefixTreeNode.getLinkedNodes());
		}

		for (PrefixTreeNode<IPrefixTreeNodeData> child : childrenPrefixTreeNodes) {
			if (visitedPrefixTreeNodes.contains(child.getData().getPatternPrefixTreeNodeIndex())) {
				continue;
			}
			if (child.getData().getTotalSupportFrequency() < threshold && child.getData().getFoundAllFocuses()) {
				// because of downward property
				child.getData().setFrequent(false);
				child.getData().setCanBeMaximalFrequent(false);
				child.getData().setMaximalFrequent(false, prefixTreeNode, prefixTree);

				child.getData().setMinimalInFrequent(true, prefixTreeNode, prefixTree);

			} else {
				findTopK(topKFrequentPatterns, prefixTree, k, child, threshold, visitedPrefixTreeNodes);
			}
		}

		// if it's not a frequent pattern
		if (prefixTreeNode.getData().getTotalSupportFrequency() < threshold || !prefixTreeNode.getData().isCorrect()
				|| !prefixTreeNode.getData().isValid()) {
			return;
		}
		// if it's frequent but it cannot be maximal
		else if (!prefixTreeNode.getData().canBeMaximalFrequent()) {
			prefixTreeNode.getData().setFrequent(true);
			prefixTreeNode.getData().setMaximalFrequent(false, prefixTreeNode, prefixTree);

			if (prefixTreeNode.getParent() != null)
				prefixTreeNode.getParent().getData().setCanBeMaximalFrequent(false);

			if (prefixTreeNode.getSuperNodeLinks() != null) {
				for (PrefixTreeNode<IPrefixTreeNodeData> superLink : prefixTreeNode.getSuperNodeLinks()) {
					superLink.getData().setCanBeMaximalFrequent(false);
				}
			}

		}
		// it's frequent and it can be maximal
		else if (prefixTreeNode.getData().canBeMaximalFrequent()) {
			prefixTreeNode.getData().setFrequent(true);
			prefixTreeNode.getData().setCanBeMaximalFrequent(true);
			prefixTreeNode.getData().setMaximalFrequent(true, prefixTreeNode, prefixTree);

			if (prefixTreeNode.getParent() != null)
				prefixTreeNode.getParent().getData().setCanBeMaximalFrequent(false);

			if (prefixTreeNode.getSuperNodeLinks() != null) {
				for (PrefixTreeNode<IPrefixTreeNodeData> superLink : prefixTreeNode.getSuperNodeLinks()) {
					superLink.getData().setCanBeMaximalFrequent(false);
				}
			}

			// // TODO: thinking more about it

			// prefixTreeNode.getData().addToTopK(prefixTree, prefixTreeNode);
		}

	}

	public static void printTopK(MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) {
		System.out.println();
		System.out.println("print top-k list:");
		System.out.println("size: " + topKFrequentPatterns.size());
		while (!topKFrequentPatterns.isEmpty()) {
			PrefixTreeNode<IPrefixTreeNodeData> frequentPaternPrefixTreeNode = topKFrequentPatterns.poll();
			System.out.println(frequentPaternPrefixTreeNode.getData().getMappedGraphString() + " -> support:"
					+ frequentPaternPrefixTreeNode.getData().getTotalSupportFrequency() + " isMFP? "
					+ frequentPaternPrefixTreeNode.getData().isMaximalFrequent());
		}

	}

}
