package src.dataset.statistics;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.neo4j.cypher.internal.compiler.v2_3.commands.expressions.Property;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.BranchOrderingPolicies;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

public class DatasetPlaying {

	// getting a dataset path and printing the ....
	public static void main(String[] args) {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_dynamic_v2.2/g0.graphdb";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		// number of nodes
		try (Transaction tx1 = dataGraph.beginTx()) {
			int allNodesCnt = 0;

			HashSet<String> importantKeys = new HashSet<String>();
			importantKeys.add("jurisdiction");
			importantKeys.add("country_codes");
			importantKeys.add("company_type:Sundry");
			
			for (Node node : dataGraph.getAllNodes()) {
				if (node.getLabels().iterator().next().name().toString().toLowerCase().equals("entity")) {
					if (node.getDegree(Direction.OUTGOING) > 1) {
						Map<String, Object> allProp = node.getAllProperties();
						for (String propKey : importantKeys) {
							if (node.hasProperty(propKey))
								System.out.println(propKey + ":" + allProp.get(propKey));
						}
					}

				}
			}

			System.out.println("number of nodes: " + allNodesCnt);

			tx1.success();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

}
