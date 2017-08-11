package src.dataset.idsImporter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class UpdateLabelsOfIDS {

	public static void main(String[] args) throws IOException {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/IDSAttack/idsJun17.db";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		Transaction tx1 = dataGraph.beginTx();

		/////////////// Update labels /////////////
		for (Node node : dataGraph.getAllNodes()) {
			Label lbl = node.getLabels().iterator().next();
			String lblStr = lbl.name().toString();
			if (lblStr.equals("Application")) {
				node.removeLabel(lbl);
				node.addLabel(Label.label(node.getProperty("appName ").toString()));
				System.out.println("Application: " + node.getId() + " -> " + node.getProperty("appName ").toString());
			} else if (lblStr.equals("Protocol")) {
				node.removeLabel(lbl);
				node.addLabel(Label.label(node.getProperty("protocolName ").toString()));
				System.out.println("Protocol: " + node.getId() + " -> " + node.getProperty("protocolName ").toString());
			}
		}

		// HashSet<String> allPossibleRelSrcDestType = new HashSet<String>();
		// int i = 0;
		// for (Relationship rel : dataGraph.getAllRelationships()) {
		// if (i % 1000000 == 0) {
		// System.out.println("rel: " + i);
		// }
		// String src = rel.getStartNode().getLabels().iterator().next().name();
		// String dest = rel.getEndNode().getLabels().iterator().next().name();
		// allPossibleRelSrcDestType.add(src + "_" + dest);
		// }
		//
		// for (String rel : allPossibleRelSrcDestType) {
		// System.out.println(rel);
		// }

//		for (Node node : dataGraph.getAllNodes()) {
//			if (node.getLabels().iterator().next().name().equals("Log")) {
//				System.out.println(node.getId());
//			}
//		}

		tx1.success();
		tx1.close();

		dataGraph.shutdown();

	}
}
