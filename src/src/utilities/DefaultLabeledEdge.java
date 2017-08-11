package src.utilities;

import org.jgrapht.graph.DefaultEdge;

public class DefaultLabeledEdge extends DefaultEdge {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2155667711195248092L;

	public DefaultLabeledEdge(String label) {
		setType(label);
	}

	private String relType = "NOTYPE";

	public String getType() {
		return relType;
	}

	public void setType(String type) {
		this.relType = type;
	}

	@Override
	public String toString() {
		return relType;

	}

}
