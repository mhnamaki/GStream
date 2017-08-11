package src.utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import src.base.IPrefixTreeNodeData;

public class PrefixTreeNode<T> {

	private List<PrefixTreeNode<T>> children = new ArrayList<PrefixTreeNode<T>>();

	// linkedNode will link this PTnode to another PTNode with the same pattern.
	private List<PrefixTreeNode<T>> linkedNodes;

	private HashSet<PrefixTreeNode<T>> childrenLinksSet = new HashSet<PrefixTreeNode<T>>();

	// each node has just one parent. However, some upper-level nodes can reach
	// to this node using linkedNode.
	private PrefixTreeNode<T> parent = null;
	private List<PrefixTreeNode<T>> superNodeLink = null;

	private T data = null;

	private Integer nodeLevel = null;

	public PrefixTreeNode(T data) {
		this.data = data;
	}

	public PrefixTreeNode(T data, PrefixTreeNode<T> parent) {
		this.data = data;
		this.parent = parent;
	}

	public List<PrefixTreeNode<T>> getChildren() {
		return children;
	}

	public HashSet<PrefixTreeNode<T>> getChildrenLinksSet() {
		return childrenLinksSet;
	}

	public List<PrefixTreeNode<T>> getLinkedNodes() {
		return linkedNodes;
	}

	public void setParent(PrefixTreeNode<T> parent) {
		this.parent = parent;
	}

	public void setSuperNodeLink(PrefixTreeNode<T> superLinkNode) {

		if (this.superNodeLink == null) {
			this.superNodeLink = new ArrayList<PrefixTreeNode<T>>();
		}
		this.superNodeLink.add(superLinkNode);
	}

	public void addChild(T data) {
		PrefixTreeNode<T> child = new PrefixTreeNode<T>(data);
		child.setParent(this);
		this.children.add(child);
		this.childrenLinksSet.add(child);
		child.nodeLevel = this.nodeLevel + 1;
	}

	public int getLevel() {
		return this.nodeLevel;
	}

	public void addChild(PrefixTreeNode<T> child) {
		child.setParent(this);
		this.children.add(child);
		this.childrenLinksSet.add(child);
		child.nodeLevel = this.nodeLevel + 1;
	}

	public void addNodeLink(PrefixTreeNode<T> nodeLink) {
		if (this.linkedNodes == null) {
			this.linkedNodes = new ArrayList<PrefixTreeNode<T>>();
		}
		this.linkedNodes.add(nodeLink);
		this.childrenLinksSet.add(nodeLink);
		nodeLink.setSuperNodeLink(this);
	}

	public T getData() {
		return this.data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean isRoot() {
		return (this.parent == null);
	}

	public boolean isLeaf() {
		if (this.children.size() == 0)
			return true;
		else
			return false;
	}

	public void removeParent() {
		this.parent = null;
	}

	public void setRootLevel() {
		this.nodeLevel = 0;
	}

	public PrefixTreeNode<T> getParent() {
		return this.parent;
	}

	public List<PrefixTreeNode<T>> getSuperNodeLinks() {
		return this.superNodeLink;
	}

	public void removeChildren(PrefixTreeNode<T> tempProcessingNode) {
		if (this.children != null)
			this.children.remove(tempProcessingNode);
		
		this.childrenLinksSet.remove(tempProcessingNode);
	}

	public void removeLinkedNode(PrefixTreeNode<T> tempProcessingNode) {
		if (this.linkedNodes != null)
			this.linkedNodes.remove(tempProcessingNode);
		
		this.childrenLinksSet.remove(tempProcessingNode);
	}

	public void removeAllChildren(PrefixTreeNode<IPrefixTreeNodeData> tempProcessingNode) {
		this.children = null;
		this.childrenLinksSet.clear();
	}

	public void removeAllReferences() {

		this.getParent().removeChildren(this);
		if (this.getLinkedNodes() != null) {
			for (PrefixTreeNode<T> superLinkNode : this.getLinkedNodes()) {
				superLinkNode.removeLinkedNode(this);
			}
		}
		this.setParent(null);

		this.nodeLevel = null;
		children = null;
		linkedNodes = null;
		parent = null;
		superNodeLink = null;
		data = null;

	}
}
