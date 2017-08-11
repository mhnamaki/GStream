package src.utilities;

import org.jgrapht.graph.DefaultEdge;

public class EdgeStreamCacheItem extends DefaultEdge {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3309119118508328957L;
	public int sourceNodeId;
	public int destNodeId;
	public String srcType;
	public String relationshipType;
	public String destType;

	public boolean isAdded;
	public boolean isImportant;
	public Integer relationshipId;

	public EdgeStreamCacheItem(int sourceNodeId, int destNodeId, String srcType, String relationshipType,
			String destType, boolean isAdded) {
		this.sourceNodeId = sourceNodeId;
		this.destNodeId = destNodeId;
		this.relationshipType = relationshipType;
		this.srcType = srcType;
		this.destType = destType;
		this.isAdded = isAdded;

	}

	@Override
	public String toString() {
		return this.srcType + "_" + this.sourceNodeId + "-" + this.relationshipType + "->" + this.destType + "_"
				+ this.destNodeId;
	}
}
