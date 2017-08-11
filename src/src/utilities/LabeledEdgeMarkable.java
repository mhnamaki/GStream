package src.utilities;

import org.jgrapht.graph.DefaultEdge;

public class LabeledEdgeMarkable extends DefaultEdge {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2155667711195248092L;

	public LabeledEdgeMarkable(String label, Integer relationshipId) {
		setTypeAndId(label, relationshipId);
	}

	private String relType = "NOTYPE";
	public boolean isImportant = false;
	public Integer relationshipId;

	public String getType() {
		return relType;
	}

	public Integer getRelationshipId() {
		return relationshipId;
	}

	public void setTypeAndId(String type, Integer relationshipId) {
		this.relType = type;
		this.relationshipId = relationshipId;
	}

	@Override
	public String toString() {
		return relType;

	}

}
