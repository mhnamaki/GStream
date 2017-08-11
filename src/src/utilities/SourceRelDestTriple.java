package src.utilities;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SourceRelDestTriple {
	public Integer srcNodeId;
	public Integer destNodeId;
	public String relationshipType;

	public SourceRelDestTriple(Integer srcNodeId, Integer destNodeId, String relationshipType) {
		this.srcNodeId = srcNodeId;
		this.destNodeId = destNodeId;
		this.relationshipType = relationshipType;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(11, 13). // two randomly chosen prime numbers
				append(srcNodeId).append(destNodeId).append(relationshipType).toHashCode();
	}

	@Override
	public boolean equals(Object that) {
		if (that == null)
			return false;

		if (this == that)
			return true;

		if (!(that instanceof SourceRelDestTriple))
			return false;

		SourceRelDestTriple thatSourceDestPairNodeId = (SourceRelDestTriple) that;

		return this.srcNodeId == thatSourceDestPairNodeId.srcNodeId
				&& this.destNodeId == thatSourceDestPairNodeId.destNodeId
				&& this.relationshipType.equals(thatSourceDestPairNodeId.relationshipType);

	}

}