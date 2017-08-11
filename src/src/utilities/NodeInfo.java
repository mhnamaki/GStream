package src.utilities;

import java.util.HashSet;

public class NodeInfo {
	public String nodeLabel;
	public Integer outDegree;
	public Integer inDegree;
	// public HashSet<Integer> nextNodeIds;
	// public HashSet<Integer> prevNodeIds;

	// public boolean isFocus = false;

	public NodeInfo(String nodeLabel, Integer inDegree, Integer outDegree/*, HashSet<Integer> prevNodeIds,
			HashSet<Integer> nextNodeIds*/) {
		this.nodeLabel = nodeLabel;
		this.inDegree = inDegree;
		this.outDegree = outDegree;
		// this.nextNodeIds = nextNodeIds;
		// this.prevNodeIds = prevNodeIds;
	}

	// public void setFocus() {
	// this.isFocus = true;
	// }

	public void incrementInDegree() {
		this.inDegree++;
	}

	public void decrementInDegree() {
		this.inDegree--;
	}

	public void incrementOutDegree() {
		this.outDegree++;
	}

	public void decrementOutDegree() {
		this.outDegree--;
	}

	// public void addNextNode(Integer nextNodeId) {
	// this.nextNodeIds.add(nextNodeId);
	// }
	//
	// public void addPrevNode(Integer prevNodeId) {
	// this.prevNodeIds.add(prevNodeId);
	// }
	//
	// public void removeNextNode(Integer nextNodeId) {
	// this.nextNodeIds.remove(nextNodeId);
	// }
	//
	// public void removePrevNode(Integer prevNodeId) {
	// }
}
