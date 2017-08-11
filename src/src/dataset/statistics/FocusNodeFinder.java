package src.dataset.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class FocusNodeFinder {

	public static void main(String[] args) throws Exception {
		// String dataGraphPath =
		// "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationV4/citation.graphdb";
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/panama_GTAR_V3.0.db";
		String importantPropKeysStr = "countries,company_type,jurisdiction,status";// "countries";//
		// "sourceID,company_type,jurisdiction,status,countries";
		//String importantPropKeysStr = "group";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-importantPropKeys")) {
				importantPropKeysStr = args[++i];
			}
		}

		File fout = new File("possibleFocusNodes.txt");
		FileOutputStream fos = new FileOutputStream(fout);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		String[] importantKeys = importantPropKeysStr.split(",");

		HashMap<String, Integer> map = new HashMap<String, Integer>();

		try (Transaction tx1 = dataGraph.beginTx()) {

			HashSet<String> lbls = new HashSet<String>();
			if (!importantPropKeysStr.equals("")) {
				for (Node node : dataGraph.getAllNodes()) {
					String label = node.getLabels().iterator().next().name();

					lbls.add(label);
					Map<String, Object> allProps = node.getAllProperties();

					// if(allProps.containsKey("status") ||
					// allProps.containsKey("Status") ||
					// allProps.containsKey("status ")){
					// System.out.println();
					// }

					for (int i = 0; i < importantKeys.length; i++) {
						if (allProps.containsKey(importantKeys[i])) {
							String value = allProps.get(importantKeys[i]).toString();
							String finalVal = label + "|" + importantKeys[i] + ":" + value;
							if (map.containsKey(finalVal)) {
								map.put(finalVal, map.get(finalVal) + 1);
							} else {
								map.put(finalVal, 1);
							}
						}
					}
				}
			} else {
				for (Node node : dataGraph.getAllNodes()) {
					String label = node.getLabels().iterator().next().name();

					if (map.containsKey(label)) {
						map.put(label, map.get(label) + 1);
					} else {
						map.put(label, 1);
					}
				}

			}

			Map<String, Integer> sortedNewMap = map.entrySet().stream()
					.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			sortedNewMap.forEach((key, val) -> {
				try {
					bw.write(key + " = " + val.toString());
					// bw.write(key);
					bw.newLine();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			HashMap<String, HashSet<PairOfString>> valueOfType = new HashMap<String, HashSet<PairOfString>>();
			for (String focus : map.keySet()) {
				String[] splittedFocus = focus.split("\\|");
				String type = splittedFocus[0];
				String propsStr = splittedFocus[1];
				String[] splittedPropVal = propsStr.split(":");
				// if(splittedPropVal.length<2){
				// System.out.println(propsStr);
				// }
				PairOfString pos = new PairOfString(splittedPropVal[0], splittedPropVal[1]);
				valueOfType.putIfAbsent(type, new HashSet<PairOfString>());
				valueOfType.get(type).add(pos);
			}

			// Entity|jurisdiction:BVI,country:ttt
			//
			JSONArray jsonArr = new JSONArray();
			for (String type : valueOfType.keySet()) {
				lbls.remove(type);
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("text", type);

				JSONArray children = new JSONArray();
				for (PairOfString propKeyValue : valueOfType.get(type)) {
					JSONObject jsonObjChild = new JSONObject();
					jsonObjChild.put("id", type + "|" + propKeyValue);
					jsonObjChild.put("text", propKeyValue.toString());
					children.add(jsonObjChild);
				}

				jsonObj.put("children", children);
				jsonArr.add(jsonObj);
			}

			// labels without props:
			for (String label : lbls) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("text", label);
				JSONArray children = new JSONArray();
				JSONObject jsonObjChild = new JSONObject();
				jsonObjChild.put("id", label);
				jsonObjChild.put("text", label);
				children.add(jsonObjChild);
				jsonObj.put("children", children);
				jsonArr.add(jsonObj);
			}

			bw.close();
			tx1.success();

			try (FileWriter file = new FileWriter("focuses.json")) {

				file.write(jsonArr.toJSONString());
				file.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

class PairOfString {
	String key;
	String value;

	public PairOfString(String key, String value) {
		this.key = key;
		this.value = value;

	}

	@Override
	public String toString() {
		return this.key + ":" + this.value;
	}
}
