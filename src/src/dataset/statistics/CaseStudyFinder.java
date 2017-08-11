package src.dataset.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class CaseStudyFinder {

	public static void main(String[] args) throws Exception {
		// String dataGraphPath =
		// "/home/shayan/Documents/WSU/Data/Synthetic/100k/g0.graphdb";
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v2.2.graphdb";

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		int allUSEnities = 0;

		try (Transaction tx1 = dataGraph.beginTx()) {

			Map<String, Integer> jurisdictionFrequency = new HashMap<String, Integer>();
			for (Node node : dataGraph.getAllNodes()) {
				String label = node.getLabels().iterator().next().name();
				if (label.equals("Entity")) {
					if (node.hasProperty("countries") && node.hasProperty("jurisdiction") && node.hasProperty("status")) {
						//if ((node.getProperty("countries").toString().equals("USA")
							//	|| node.getProperty("countries").toString().equals("United States"))
							//	&& 
								//node.getProperty("status").toString().equals("Inactivated")) {

							jurisdictionFrequency.putIfAbsent(node.getProperty("jurisdiction").toString(), 0);
							jurisdictionFrequency.put(node.getProperty("jurisdiction").toString(),
									jurisdictionFrequency.get(node.getProperty("jurisdiction")) + 1);

							//allUSEnities++;
						//}
					}
				}
			}

			System.out.println("allUSEnities:" +allUSEnities);
			//double allUSEnitiesD = allUSEnities;
			
			Map<String, Integer> sortedNewMap = jurisdictionFrequency.entrySet().stream()
					.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			sortedNewMap.forEach((key, val) -> {
				System.out.println(key + " = " + jurisdictionFrequency.get(key) + " : "
						+ (jurisdictionFrequency.get(key)));
			});
			tx1.success();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

}
