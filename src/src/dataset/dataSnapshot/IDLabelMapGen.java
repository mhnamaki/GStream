package src.dataset.dataSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class IDLabelMapGen {

	static String completeDataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/panama_GTAR_V3.0.db";
	static String storeDir = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/onlyDeltaEs/";

	public static void main(String[] args) throws Exception {
		File completeDataGraphFile = new File(completeDataGraphPath);

		GraphDatabaseService completeDataGraph = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(completeDataGraphFile)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		File idLabelMap = new File(storeDir + "/idLabelMap.txt");
		FileWriter idLabelMapWriter = new FileWriter(idLabelMap);

		Transaction tx1 = completeDataGraph.beginTx();

		for (Node node : completeDataGraph.getAllNodes()) {
			idLabelMapWriter.write(node.getId() + "\t " + node.getLabels().iterator().next().name().toString()
					.replaceAll("\t", " ").replaceAll("\n", " ").trim() + "\t");

			// idLabelMapWriter.write("[");
			for (String key : node.getPropertyKeys()) {
				idLabelMapWriter.write(key.trim() + ":" + node.getProperty(key).toString().replaceAll(":", " ")
						.replaceAll("\t", " ").replaceAll("\n", " ").trim() + "\t");
			}
			// idLabelMapWriter.write("]\n");
			idLabelMapWriter.write("\n");
		}

		idLabelMapWriter.flush();
		idLabelMapWriter.close();

		tx1.success();
		completeDataGraph.shutdown();

	}
}
