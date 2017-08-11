package src.dataset.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class TokenizePropertyValues {

	// getting a dataset path and printing the ....
	public static void main(String[] args) throws IOException {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/IDSAttack/idsJun17_V2.db";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g")
				// .setConfig(GraphDatabaseSettings.allow_store_upgrade, "true")
				.newGraphDatabase();

		// System.out.println("dataset: " + dataGraphPath);

		// File fout = new File("propValueWords.txt");
		// FileOutputStream fos = new FileOutputStream(fout);
		//
		// BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		HashMap<String, Integer> tokensFrequenciesMap = new HashMap<String, Integer>();

		try (Transaction tx1 = dataGraph.beginTx()) {

			for (Node node : dataGraph.getAllNodes()) {
				Map<String, Object> allPropertiesOfNode = node.getAllProperties();
				for (String key : allPropertiesOfNode.keySet()) {
					if (key.toLowerCase().contains("time")) {
						continue;
					}
					String value = allPropertiesOfNode.get(key).toString().toLowerCase().trim();

					StringTokenizer st = new StringTokenizer(value);
					while (st.hasMoreTokens()) {
						String word = node.getLabels().iterator().next().name() + "|" + key + ":\""
								+ st.nextToken().trim() + "\"";
						tokensFrequenciesMap.putIfAbsent(word, 0);
						tokensFrequenciesMap.put(word, tokensFrequenciesMap.get(word) + 1);
					}
				}
			}

			Map<String, Integer> sortedNewMap = tokensFrequenciesMap.entrySet().stream()
					.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			sortedNewMap.forEach((key, val) -> {
				if (val > 2) {
					System.out.println(key + "   :       " + val);
				}
			});
			tx1.success();
		} catch (Exception e) {
			// TODO: handle exception
		}

		dataGraph.shutdown();
	}

}
