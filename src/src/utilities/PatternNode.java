package src.utilities;

public class PatternNode {
	private String nodeType;
	private boolean isFocus = false;
	// public int stepsFromRoot = 0;

	public PatternNode(String nodeType, boolean isFocus
	// ,int stepsFromRoot
	) {
		this.nodeType = nodeType;
		this.isFocus = isFocus;
		// this.stepsFromRoot = stepsFromRoot;

	}

	public PatternNode(String nodeType
	// ,int stepsFromRoot
	) {
		this.nodeType = nodeType;
		// this.stepsFromRoot = stepsFromRoot;

	}
	//
	// /**
	// * using just for the first born focus nodes
	// *
	// * @param nodeType:
	// * label of the node
	// */
	// public PatternNode(String nodeType) {
	// this.nodeType = nodeType;
	// this.isFocus = true;
	// this.stepsFromRoot = 0;
	// }

	public String getLabel() {
		return nodeType;
	}

	public String getType() {
		if (!isFocus)
			return nodeType;

		// TODO: assumption: user gave us distinct labels.
		// otherwise we have to have some indexes for same types but with
		// different prop key values
		return nodeType + "*";
	}

	public boolean isFocus() {
		return isFocus;
	}

	@Override
	public String toString() {
		return "(" + this.getType()
		// + "_" + this.stepsFromRoot
		 + "_" + this.hashCode()
				+ ") ";
	}

}
