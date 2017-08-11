package src.simpleTests;

import java.io.File;
import java.nio.file.Path;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;
import src.utilities.Dummy.DummyFunctions;

public class CheckRelInsDelTime {

	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_dynamic_v2.2_FRESH/g0.graphdb";
	private static String dateFormat = "dd-MMM-yyyy";
	private static String deltaEFileOrFiles = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_dynamic_v2.2_FRESH/";
	private static GraphDatabaseService dataGraph;

	public static void main(String[] args) throws Exception {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-deltaEFileOrFiles")) {
				deltaEFileOrFiles = args[++i];
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			}
		}

		Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
		if (newDGPath == null) {
			throw new Exception("newDGPath is null!");
		}

		dataGraphPath = newDGPath.toString();

		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaEFileOrFiles, dateFormat);

		double handlingDataStreamStartTime = System.nanoTime();
		// double handlingDataStreamDuration = 0.0d;
		double readNextEdgeActionTime = 0.0d;
		double edgeInsertionTime = 0.0d;
		double edgeDeletionTime = 0.0d;

		Transaction tx1 = dataGraph.beginTx();
		StreamEdge nextStreamEdge = null;
		int i = 0;
		while (true) {

			nextStreamEdge = snapshotSim.getNextStreamEdge();

			if (nextStreamEdge == null)
				break;

			if ((i % 100000) == 0) {
				tx1.success();
				tx1.close();
				// System.out.println("tx is commited in " + i + "-th
				// transaction!");
				tx1 = dataGraph.beginTx();
			}

			i++;

			double readNextEdgeActionTimeStartTime = System.nanoTime();

			Node srcNode = dataGraph.getNodeById(nextStreamEdge.getSourceNode());
			Node destNode = dataGraph.getNodeById(nextStreamEdge.getDestinationNode());
			String srcLabel = srcNode.getLabels().iterator().next().name();
			String destLabel = destNode.getLabels().iterator().next().name();

			useLabels(srcLabel, destLabel);
			//
			System.out.println(i + ": "
					// + " nextStreamEdge: "
							+ nextStreamEdge.getSourceNode()
							// + "_" + srcLabel
							+ " -> " + nextStreamEdge.getDestinationNode()
					// + "_"
					// + destLabel
					// + " => isAdded? "
					// + nextStreamEdge.isAdded()
					);

			readNextEdgeActionTime += ((System.nanoTime() - readNextEdgeActionTimeStartTime) / 1e6);

			String relationship = nextStreamEdge.getRelationshipType();
			// then add/remove new relationship between them
			if (nextStreamEdge.isAdded()) {
				double edgeInsertionStartTime = System.nanoTime();
				srcNode.createRelationshipTo(destNode, RelationshipType.withName(relationship));
				edgeInsertionTime += ((System.nanoTime() - edgeInsertionStartTime) / 1e6);
			} else {
				double edgeDeletionStartTime = System.nanoTime();
				// for-loop over less degree node
				if (srcNode.getDegree(Direction.OUTGOING) <= destNode.getDegree(Direction.INCOMING)) {
					for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
							RelationshipType.withName(relationship))) {
						if (rel.getEndNode().getId() == destNode.getId()) {
							rel.delete();
						}
					}
				} else {
					for (Relationship rel : destNode.getRelationships(Direction.INCOMING,
							RelationshipType.withName(relationship))) {
						if (rel.getStartNode().getId() == srcNode.getId()) {
							rel.delete();
						}
					}
				}
				edgeDeletionTime += ((System.nanoTime() - edgeDeletionStartTime) / 1e6);
			}
		}
		double handlingDataStreamDuration = ((System.nanoTime() - handlingDataStreamStartTime) / 1e6);

		System.out.println("handlingDataStreamDuration: " + handlingDataStreamDuration);
		System.out.println("edgeDeletionTime: " + edgeDeletionTime);
		System.out.println("edgeInsertionTime: " + edgeInsertionTime);
		System.out.println("readNextEdgeActionTime: " + readNextEdgeActionTime);

		tx1.success();
		tx1.close();

		System.out.println("before shutdown!");
		dataGraph.shutdown();
		System.out.println("after shutdown!");
	}

	private static void useLabels(String srcLabel, String destLabel) {
		int i = 0;
		int j=0;
		if (srcLabel != destLabel) {
			i++;
		}
		j = i;

	}

}
