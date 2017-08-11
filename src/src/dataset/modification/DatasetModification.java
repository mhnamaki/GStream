package src.dataset.modification;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class DatasetModification {
	private static BatchInserter db;
	private static BatchInserterIndex index;

	public static void main(String[] args) throws Exception {
		String dataGraphPath = "/Users/mnamaki/Data-Panama/neo4j-mac/panama-papers/ICIJ Panama Papers/panama_data_for_neo4j/databases/panama.graphdb";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		String[] usefulPropertyKeys = new String[] { "sourceID", "company_type", "jurisdiction", "status",
				"countries" };

		Transaction tx1 = dataGraph.beginTx();
		int counter = 0;
		HashMap<Long, ArrayList<String>> usefulPropsMap = new HashMap<Long, ArrayList<String>>();
		HashMap<Long, ArrayList<Label>> prevLabelsMap = new HashMap<Long, ArrayList<Label>>();

		int cntNodes = 0;
		for (Node node : dataGraph.getAllNodes()) {
			cntNodes++;
			if ((cntNodes % 10000) == 0) {
				System.out.println("cntNodes: " + cntNodes);
			}
			ArrayList<Label> prevLabels = new ArrayList<Label>();
			for (Label label : node.getLabels()) {
				prevLabels.add(label);
			}

			prevLabelsMap.put(node.getId(), prevLabels);

			ArrayList<String> propValues = new ArrayList<String>();
			for (int i = 0; i < usefulPropertyKeys.length; i++) {
				Map<String, Object> allProperties = node.getAllProperties();
				if (allProperties.containsKey(usefulPropertyKeys[i])) {
					Object objValue = node.getProperty(usefulPropertyKeys[i]);
					if (objValue != null) {
						propValues.add(usefulPropertyKeys[i] + ":" + objValue.toString());
					}
				}

			}

			usefulPropsMap.put(node.getId(), propValues);

		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

		System.out.println("usefulPropsMap size: " + usefulPropsMap.size());

		Map<String, String> config = new HashMap<String, String>();

		config.put("dbms.pagecache.pagesize", "4g");
		config.put("node_auto_indexing", "true");
		db = BatchInserters.inserter(storeDir, config);
		BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(db);
		index = indexProvider.nodeIndex("dataSetIndex", MapUtil.stringMap("type", "exact"));
		System.out.println("After filling the Props map ");

		for (Long nodeId : usefulPropsMap.keySet()) {
			for (String propValue : usefulPropsMap.get(nodeId)) {
				prevLabelsMap.get(nodeId).add(Label.label(propValue));
			}

			db.setNodeLabels(nodeId, prevLabelsMap.get(nodeId).toArray(new Label[prevLabelsMap.get(nodeId).size()]));

		}

		counter++;
		if ((counter % 200000) == 0) {
			index.flush();
			System.out.println(counter + " progress.");
			// tx1.success();
			// tx1.close();
			// tx1 = knowledgeGraph.beginTx();
		}

		index.flush();

		System.gc();
		System.runFinalization();

		/// System.out.println("uriProps size: " + uriProps.size());
		System.out.println("counter: " + counter);
		System.out.println("indexProvider shutting down");
		indexProvider.shutdown();

		System.out.println("db shutting down");
		db.shutdown();

		System.out.println("knowledgeGraph shutting down");
		dataGraph.shutdown();

		System.out.println("completed");

	}

}
