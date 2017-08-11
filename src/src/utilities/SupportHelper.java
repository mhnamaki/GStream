package src.utilities;
 
import src.base.IPrefixTree;
import src.utilities.Dummy.DummyProperties;

public class SupportHelper {

	public static void carryOverTheSupport(IPrefixTree prefixTree, int snapshot, int startingWindow, double alpha) {

		for (Integer patternTreeNodeIndex : prefixTree.getPrefixTreeNodeIndex().keySet()) {

			if (!DummyProperties.windowMode) {

				double value;

				value = prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
						.getSupportFrequency(snapshot - 1);

				// value += alpha *
				// prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
				// .getSupportFrequency(snapshot - 1);

				if (prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
						.getSupportFrequency(snapshot) <= 0) {
					prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
							.setSupportFrequency(snapshot, value, alpha, true);
				}
			} else {

				double value;

				value = prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
						.getSupportFrequency(snapshot - startingWindow - 1);

				// value += alpha *
				// prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
				// .getSupportFrequency(snapshot - startingWindow - 1);

				// prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData().setSupportFrequency(snapshot,
				// alpha *
				// prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
				// .getSupportFrequency(snapshot - startingWindow - 1));

				if (prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
						.getSupportFrequency(snapshot) <= 0) {
					prefixTree.getPrefixTreeNodeIndex().get(patternTreeNodeIndex).getData()
							.setSupportFrequency(snapshot, value, alpha, true);
				}
			}
		}
	}

}
