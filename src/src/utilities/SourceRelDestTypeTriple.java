package src.utilities;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import src.utilities.Dummy.DummyProperties;

public class SourceRelDestTypeTriple {
	public String srcNodeType;
	public String destNodeType;
	public String relationshipType;

	public SourceRelDestTypeTriple(String srcNodeType, String destNodeType, String relationshipType) {
		this.srcNodeType = srcNodeType;
		this.destNodeType = destNodeType;
		this.relationshipType = relationshipType;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
				append(srcNodeType).append(destNodeType).append(relationshipType).toHashCode();
	}

	@Override
	public boolean equals(Object that) {
		if (that == null)
			return false;

		if (this == that)
			return true;

		if (!(that instanceof SourceRelDestTypeTriple))
			return false;

		SourceRelDestTypeTriple thatSourceDestPairNodeType = (SourceRelDestTypeTriple) that;

		return this.srcNodeType.equals(thatSourceDestPairNodeType.srcNodeType)
				&& this.destNodeType.equals(thatSourceDestPairNodeType.destNodeType)
				&& this.relationshipType.equals(thatSourceDestPairNodeType.relationshipType);

	}

	@Override
	public String toString() {
		return srcNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType
				+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + destNodeType;
	}

}
