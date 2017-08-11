package src.dataset.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.internal.compiler.v2_3.pipes.matching.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class GoThroughDataset {

	public static String dbPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/Synthetics/Syn1MNodes_300L_1RType/batchesDel/0.graphdb";
	//public static int numberOfPrefixChars = 4;
	public static GraphDatabaseService knowledgeGraph;

	public GoThroughDataset() {
		File graph = new File(dbPath);
		knowledgeGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(graph)
				// .setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		registerShutdownHook(knowledgeGraph);
		System.out.println("after knowledge graph initialization");

		ArrayList<Nodess> nodes = new ArrayList<Nodess>();
		try (Transaction tx1 = knowledgeGraph.beginTx()) {

			
			for (Node node : knowledgeGraph.getAllNodes()) {
				if (node.getLabels().iterator().next().name().toLowerCase().equals("unvacillating")) {
					for (Relationship firstRel : node.getRelationships(Direction.OUTGOING)) {
						Node firstNeighborNode = firstRel.getEndNode();
						if (firstNeighborNode.getLabels().iterator().next().name().toLowerCase().equals("millihenries")) {
							for (Relationship secondRel : firstNeighborNode.getRelationships(Direction.OUTGOING)) {
								Node secondNeighborNode = secondRel.getEndNode();
								if (secondNeighborNode.getLabels().iterator().next().name().toLowerCase().equals("millihenries")) {
									for (Relationship anotherRel : node.getRelationships(Direction.OUTGOING)) {
										Node lastNode = anotherRel.getEndNode();
										if (lastNode.getId() == secondNeighborNode.getId()) {
											Nodess myNodes = new Nodess(node.getId(), firstNeighborNode.getId(),
													secondNeighborNode.getId());
											nodes.add(myNodes);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		knowledgeGraph.shutdown();
	}

	public static void main(String[] args) {
		GoThroughDataset g = new GoThroughDataset();
	}

	class Nodess {
		long firstNode;
		long secondNode;
		long thirdNode;

		public Nodess(long firstNode, long secondNode, long thirdNode) {
			this.firstNode = firstNode;
			this.secondNode = secondNode;
			this.thirdNode = thirdNode;
		}
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
