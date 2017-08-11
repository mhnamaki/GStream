package src.utilities;

import java.util.Iterator;

import src.base.IPrefixTreeNodeData;

public class CorrectnessChecking {
	public static void checkingDownwardProperty(PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode) {
		Iterator<PrefixTreeNode<IPrefixTreeNodeData>> prefixTreeNodeItr = prefixTreeNode.getChildren().iterator();
		while (prefixTreeNodeItr.hasNext()) {
			PrefixTreeNode<IPrefixTreeNodeData> child = prefixTreeNodeItr.next();
			if (prefixTreeNode.getLevel() > 1 && child.getData().getTotalSupportFrequency() > prefixTreeNode.getData()
					.getTotalSupportFrequency()) {
				System.err.println("parent: " + prefixTreeNode.getData().getMappedGraphString() + " -> child: "
						+ child.getData().getMappedGraphString() + " Support: "
						+ prefixTreeNode.getData().getTotalSupportFrequency() + " -> "
						+ child.getData().getTotalSupportFrequency());
			}
		}

	}
}
