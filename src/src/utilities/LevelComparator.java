package src.utilities;
 
import java.util.Comparator;

import src.base.IPrefixTreeNodeData;

public class LevelComparator implements Comparator<PrefixTreeNode<IPrefixTreeNodeData>> {

	@Override
	public int compare(PrefixTreeNode<IPrefixTreeNodeData> o1, PrefixTreeNode<IPrefixTreeNodeData> o2) {
		return Integer.compare(o1.getData().getPatternGraph().edgeSet().size(),
				o2.getData().getPatternGraph().edgeSet().size());
	}

}
