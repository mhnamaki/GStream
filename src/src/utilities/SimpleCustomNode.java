package src.utilities;

import org.neo4j.graphdb.Node;

public class SimpleCustomNode {
	public Integer nodeIndex; // incremental
	public Node neo4jNode;

	public SimpleCustomNode(Node neo4jNode) {
		this.neo4jNode = neo4jNode;
	}

	public void updateNodeIndex(Integer nodeIndex){
		this.nodeIndex = nodeIndex;
	}

	@Override
	public int hashCode() {
		return (int) neo4jNode.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		SimpleCustomNode other = (SimpleCustomNode) obj;
		if (this.neo4jNode.getId() != other.neo4jNode.getId())
			return false;

		return true;
	}

}
