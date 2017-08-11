package src.dataset.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class FocusGen {

	public static void main(String[] args) throws Exception {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/Panamas/panamaDyn2M10K/0.graphdb";
		HashSet<String> usefulProps = new HashSet<String>();
		usefulProps.add("sourceID");
		usefulProps.add("status");
		usefulProps.add("countries");
		usefulProps.add("jurisdiction");
		usefulProps.add("company_type");
		usefulProps.add("name");

		HashSet<String> usefulLabel = new HashSet<String>();
		usefulLabel.add("Entity");
		usefulLabel.add("ACTIVE");
		usefulLabel.add("Discontinued");
		usefulLabel.add("Audit_Licence");

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			}
		}

		File readme = new File(dataGraphPath + "/Focuses.txt");
		FileWriter readmeWriter = new FileWriter(readme);

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		try (Transaction tx1 = dataGraph.beginTx()) {

			// HashMap<String, HashSet<String>> labelAndProp = new
			// HashMap<String, HashSet<String>>();
			// for (Node node : dataGraph.getAllNodes()) {
			// for (Label lbl : node.getLabels()) {
			// labelAndProp.putIfAbsent(lbl.toString(), new HashSet<String>());
			// Map<String, Object> props = node.getAllProperties();
			// for (String key : props.keySet()) {
			// labelAndProp.get(lbl.toString()).add(key);
			// }
			// }
			// }
			// for(String label :labelAndProp.keySet()){
			// System.out.println(label);
			// for(String key :labelAndProp.get(label)){
			// System.out.println("\t" + key);
			// }
			//
			// }

			int progress = 0;
			HashMap<String, HashSet<String>> focuses = new HashMap<String, HashSet<String>>();
			for (Node node : dataGraph.getAllNodes()) {
				progress++;
				for (Label lbl : node.getLabels()) {
					focuses.putIfAbsent(lbl.toString(), new HashSet<String>());
				}
				if (progress % 100000 == 0) {
					System.out.println("progress:" + progress);
				}
			}
			System.out.println("first pass:" + progress);

			progress = 0;
			for (Node node : dataGraph.getAllNodes()) {
				progress++;
				for (Label lbl : node.getLabels()) {
					if (usefulLabel.contains(lbl.toString())) {
						Map<String, Object> props = node.getAllProperties();
						for (String key : props.keySet()) {
							if (usefulProps.contains(key)) {
								String str = key + ":" + props.get(key).toString().replace("'", "");
								if (str.length() < 20) {
									focuses.get(lbl.toString()).add(str);
								}
							}
						}
					}
				}
				if (progress % 100000 == 0) {
					System.out.println("progress:" + progress);
				}
			}
			System.out.println("second pass:" + progress);

			int idCnt = 0;
			String output = "[";
			for (String label : focuses.keySet()) {
				System.out.println("label: " + label);
				output += "{ text:'" + label + "'," + "children:[";
				for (String keyVal : focuses.get(label)) {
					output += "{id:" + idCnt++ + ", text:'" + keyVal + "'},";
					if (idCnt % 100000 == 0) {
						System.out.println("idCnt: " + idCnt);
					}
				}
				output += "]},";
			}
			output += "]";

			System.out.println("third pass done!");

			readmeWriter.write(output);
			readmeWriter.close();

			tx1.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
