package src.dataset.dataSnapshot;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shayan on 6/29/16.
 */
public class StreamEdge {

	private Boolean added;
	private Date timeStamp;
	private long source;
	private long destination;
	private String relationshipType;

	public StreamEdge(String input, String timeFormat) throws Exception {
		// System.out.print(input);

		DateFormat dateFormat = new SimpleDateFormat(timeFormat);
		String[] chunks = input.split(",");

		if (chunks[0].equals("+"))
			setAdded(true);
		else
			setAdded(false);


		setTimeStamp(dateFormat.parse(chunks[1]));
		setSourceNode(Long.parseLong(chunks[2]));
		setDestinationNode(Long.parseLong(chunks[3]));
		setRelationshipType(chunks[4]);
	}

	public StreamEdge(boolean add, Date timeStamp, long sourceNodeId, long destinationNodeId, String relationshipType)
	{
		this.added  = add;
		this.timeStamp = timeStamp;
		this.source = sourceNodeId;
		this.destination = destinationNodeId;
		this.relationshipType = relationshipType;
	}

	public void setAdded(boolean isAdded) {
		added = isAdded;
	}

	public void setTimeStamp(Date s) {
		timeStamp = s;
	}

	public void setSourceNode(long n) {
		source = n;
	}

	public void setDestinationNode(long n) {
		destination = n;
	}

	public void setRelationshipType(String s) {
		relationshipType = s;
	}

	public boolean isAdded() {
		return added;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public long getSourceNode() {
		return source;
	}

	public long getDestinationNode() {
		return destination;
	}

	public String getRelationshipType() {
		return relationshipType;
	}

	public String toString() {
		return added + " " + ":" + timeStamp + " " + source + " -[:" + relationshipType + "]-> " + destination;
	}

}
