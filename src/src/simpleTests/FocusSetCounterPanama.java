package src.simpleTests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import src.utilities.Dummy.DummyFunctions;

public class FocusSetCounterPanama {

	private static final String outputFile = "reasonableFocusSetPanamaAlaki.txt";
	private static final String middleFile = "focusSetPanama.txt";
	private static String allFocusLinesPath = "/Users/mnamaki/Documents/workspace/TopKGStream/possibleFocusNodesSmall.txt";
	private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/panama_GTAR_V3.0.db";

	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	String focusSetPath = null;
	GraphDatabaseService dataGraph;

	public FocusSetCounterPanama(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-allFocusLinesPath")) {
				allFocusLinesPath = args[++i];
			} else if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			}
		}
		if (allFocusLinesPath.equals("") || dataGraphPath.equals("")) {
			throw new Exception("allFocusLinesPath  or dataGraphPath ");
		}
	}

	public static void main(String[] args) throws Exception {

		FocusSetCounterPanama fsc = new FocusSetCounterPanama(args);
		// fsc.run();

		// fsc.findTopJurisdictionForCountries();
		// fsc.findTrend();

		// fsc.getIdea();

		// fsc.removeSomeNodes();

		fsc.findNoTypeAndLabels();
	}

	private void findNoTypeAndLabels() {

		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		Transaction tx1 = dataGraph.beginTx();

		int cnt = 1;
		// int nodeCnt = 1;
		HashSet<Long> set = new HashSet<Long>();
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (rel.getType().toString().toLowerCase().equals("notype")) {
				// set.add(rel.getStartNode().getLabels().iterator().next() +
				// "-" + rel.getType() + "->"
				// + rel.getEndNode().getLabels().iterator().next());
				set.add(rel.getEndNode().getId());

			}
		}

		for (Long s : set) {
			Node endNode = dataGraph.getNodeById(s);
			for (Relationship corruptRel : endNode.getRelationships()) {
				corruptRel.delete();
				cnt++;
			}
			endNode.delete();

			if (cnt % 100000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
			}

		}

		// System.out.println("relCnt: " + relCnt);
		// System.out.println("nodeCnt: " + nodeCnt);

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void removeSomeNodes() throws Exception {

		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		// HashSet<String> labels = new HashSet<String>();
		// FileInputStream fis = new FileInputStream(new
		// File("shouldRemoveFromPanama.txt"));
		//
		// // Construct BufferedReader from InputStreamReader
		// BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		//
		// String line = null;
		// while ((line = br.readLine()) != null) {
		// labels.add(line.trim());
		// }

		// br.close();

		Transaction tx1 = dataGraph.beginTx();

		int relCnt = 1;
		int nodeCnt = 1;
		for (Node node : dataGraph.getAllNodes()) {
			String lbl = node.getLabels().iterator().next().name();
			if (lbl.equals("Officer")) {
				for (Relationship rel : node.getRelationships()) {
					rel.delete();
					relCnt++;
					if (relCnt % 10000 == 0) {
						tx1.success();
						tx1.close();
						tx1 = dataGraph.beginTx();
					}
				}
				nodeCnt++;
				node.delete();
				if (nodeCnt % 10000 == 0) {
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
			}
		}

		System.out.println("relCnt: " + relCnt);
		System.out.println("nodeCnt: " + nodeCnt);

		tx1.close();
		tx1.success();
		dataGraph.shutdown();

	}

	private void getIdea() {

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		Transaction tx1 = dataGraph.beginTx();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.getLabels().iterator().next().name().toLowerCase().equals("asia")) {
				for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
					if (rel.getOtherNode(node).getLabels().iterator().next().name().toLowerCase()
							.contains("country_")) {
						System.out.println(rel.getOtherNode(node).getLabels());
					}
				}
			}
		}

		tx1.success();

		dataGraph.shutdown();
	}

	private void findTrend() throws Exception {
		HashSet<String> asianCountries = new HashSet<String>();

		FileInputStream fis = new FileInputStream("asianCountries.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			asianCountries.add(line.trim().toLowerCase());

		}

		br.close();

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		HashMap<String, ArrayList<Integer>> asianEntities = new HashMap<String, ArrayList<Integer>>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Entity"))) {
				if (node.hasProperty("countries")) {
					if (asianCountries.contains(node.getProperty("countries").toString().trim().toLowerCase())) {
						asianEntities.putIfAbsent(node.getProperty("countries").toString().trim().toLowerCase(),
								new ArrayList<Integer>());
						asianEntities.get(node.getProperty("countries").toString().trim().toLowerCase())
								.add((int) node.getId());

					}
				}
			}
		}

		System.out.println("asianEntities:" + asianEntities.size());
		int ent = 0;
		for (String cntry : asianEntities.keySet()) {
			ent += asianEntities.get(cntry).size();
		}
		System.out.println("ent: " + ent);
		HashMap<Integer, HashMap<String, Integer>> freqOfJurisdictionIn5Years = new HashMap<Integer, HashMap<String, Integer>>();

		SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
		SimpleDateFormat sdfTotal = new SimpleDateFormat("dd-MMM-yyyy");
		int step = 5;
		for (int year = 1950; year < 2021; year += step) {
			freqOfJurisdictionIn5Years.putIfAbsent(year, new HashMap<String, Integer>());
			Date currentDate = new Date();
			Date futureDate = new Date();

			currentDate = sdfYear.parse(String.valueOf(year));
			futureDate = sdfYear.parse(String.valueOf(year + step + 1));

			for (String country : asianEntities.keySet()) {
				ArrayList<Integer> entityIDs = asianEntities.get(country);
				for (Integer nodeId : entityIDs) {
					Node node = dataGraph.getNodeById(nodeId);
					if (node.hasProperty("incorporation_date") && node.hasProperty("inactivation_date")) {
						Date incorporation_date = sdfTotal.parse(node.getProperty("incorporation_date").toString());
						Date inactivation_date = sdfTotal.parse(node.getProperty("inactivation_date").toString());
						if (currentDate.getYear() > incorporation_date.getYear()
								&& futureDate.getYear() < inactivation_date.getYear()) {

							if (node.hasProperty("jurisdiction")) {
								freqOfJurisdictionIn5Years.get(year).putIfAbsent(
										node.getProperty("jurisdiction").toString().trim().toLowerCase(), 0);
								freqOfJurisdictionIn5Years.get(year)
										.put(node.getProperty("jurisdiction").toString().trim().toLowerCase(),
												freqOfJurisdictionIn5Years.get(year).get(node
														.getProperty("jurisdiction").toString().trim().toLowerCase())
														+ 1);
							}

						}
					} else if (node.hasProperty("incorporation_date") && !node.hasProperty("inactivation_date")) {
						// System.out.println("just active");
						Date incorporation_date = sdfTotal.parse(node.getProperty("incorporation_date").toString());

						if (currentDate.getYear() > incorporation_date.getYear()) {

							if (node.hasProperty("jurisdiction")) {
								freqOfJurisdictionIn5Years.get(year).putIfAbsent(
										node.getProperty("jurisdiction").toString().trim().toLowerCase(), 0);
								freqOfJurisdictionIn5Years.get(year)
										.put(node.getProperty("jurisdiction").toString().trim().toLowerCase(),
												freqOfJurisdictionIn5Years.get(year).get(node
														.getProperty("jurisdiction").toString().trim().toLowerCase())
														+ 1);
							}

						}
					} else if (!node.hasProperty("incorporation_date") && !node.hasProperty("inactivation_date")) {
						// System.out.println("none of them");
						if (node.hasProperty("jurisdiction")) {
							freqOfJurisdictionIn5Years.get(year)
									.putIfAbsent(node.getProperty("jurisdiction").toString().trim().toLowerCase(), 0);
							freqOfJurisdictionIn5Years.get(year)
									.put(node.getProperty("jurisdiction").toString().trim().toLowerCase(),
											freqOfJurisdictionIn5Years.get(year).get(
													node.getProperty("jurisdiction").toString().trim().toLowerCase())
													+ 1);
						}
					}
				}
			}
		}

		for (Integer yearLocal : freqOfJurisdictionIn5Years.keySet()) {
			System.out.println(yearLocal);
			for (String jurisdiction : freqOfJurisdictionIn5Years.get(yearLocal).keySet()) {
				System.out.println(jurisdiction + ";" + freqOfJurisdictionIn5Years.get(yearLocal).get(jurisdiction));
			}
		}
		tx1.success();
		tx1.close();

	}

	private void findTopJurisdictionForCountries() throws Exception {
		HashSet<String> asianCountries = new HashSet<String>();

		FileInputStream fis = new FileInputStream("asianCountries.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			asianCountries.add(line.trim().toLowerCase());

		}

		br.close();

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// for (Node node : dataGraph.getAllNodes()) {
		// if (node.hasLabel(Label.label("Entity")) && node.hasProperty("name"))
		// {
		// if
		// (node.getProperty("name").toString().toLowerCase().contains("australian")
		// &&
		// node.getProperty("countries").toString().toLowerCase().contains("singapore"))
		// {
		//
		// System.out.println(node.getId() + ", ");
		// }
		// }
		// }

		// for (Node node : dataGraph.getAllNodes()) {
		// if (node.hasLabel(Label.label("Entity")) &&
		// node.hasProperty("countries") && node.hasProperty("status")) {
		// if
		// (node.getProperty("status").toString().toLowerCase().contains("active")
		// &&
		// node.getProperty("countries").toString().toLowerCase().contains("singapore")
		// &&
		// node.getProperty("jurisdiction").toString().toLowerCase().equals("bvi"))
		// {
		// System.out.println(node.getId() + ", ");
		// }
		// }
		// }

		HashMap<String, ArrayList<Integer>> asianEntities = new HashMap<String, ArrayList<Integer>>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Entity"))) {
				if (node.hasProperty("countries") && node.hasProperty("status")) {
					if (asianCountries.contains(node.getProperty("countries").toString().trim().toLowerCase())
							&& node.getProperty("status").toString().toLowerCase().contains("active")) {
						asianEntities.putIfAbsent(node.getProperty("countries").toString().trim().toLowerCase(),
								new ArrayList<Integer>());
						asianEntities.get(node.getProperty("countries").toString().trim().toLowerCase())
								.add((int) node.getId());

					}
				}
			}
		}

		System.out.println("asianEntities size:" + asianEntities.size());

		ArrayList<Holder> holders = new ArrayList<Holder>();
		for (String cntry : asianEntities.keySet()) {
			Holder h = new Holder();
			h.country = cntry;
			for (Node node : dataGraph.getAllNodes()) {
				if (asianEntities.get(cntry).contains((int) node.getId())) {
					if (node.getProperty("name").toString().length() < 15)
						System.out.println(node.getProperty("name"));
					if (node.hasProperty("jurisdiction")) {
						h.jurisdictioFrequency
								.putIfAbsent(node.getProperty("jurisdiction").toString().trim().toLowerCase(), 0);

						h.jurisdictioFrequency.put(node.getProperty("jurisdiction").toString().trim().toLowerCase(),
								h.jurisdictioFrequency
										.get(node.getProperty("jurisdiction").toString().trim().toLowerCase()) + 1);
					}
				}
			}

			holders.add(h);

		}

		for (Holder h : holders) {
			// System.out.println(h.country);

			Map<String, Integer> sortedNewMap = h.jurisdictioFrequency.entrySet().stream()
					.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			sortedNewMap.forEach((key, val) -> {
				try {
					// System.out.println(key + "#" + val);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			// System.out.println();
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void run() throws Exception {
		File fout0 = new File(outputFile);
		FileOutputStream fos0 = new FileOutputStream(fout0);
		BufferedWriter bwCitation = new BufferedWriter(new OutputStreamWriter(fos0));

		// writable + "#" + numberOfAllFocusNodes + "#" + avgDegree + "#" +
		// allNodesInTwoHopsSet.size()
		// + "#" + allNodesInTwoHops + "#" + lbls.size() + "#" +
		// maxDegreeOfAllNodes + "#" + maxDegreeOfAllNodes
		// + "#" + "\n"
		bwCitation.write(
				"writable#numberOfAllFocusNodes#avgDegree#allNodesInTwoHopsSet#allNodesInTwoHops#lbls#maxDegreeOfAllNodes#maxDegreeOfFocuses\n");

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		System.out.println("AvgInDegrees: " + DummyFunctions.getAvgInDegrees(dataGraph));
		System.out.println("AvgOutDegrees: " + DummyFunctions.getAvgOutDegrees(dataGraph));
		System.out.println("AvgDegrees: " + DummyFunctions.getAvgDegrees(dataGraph));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			// String[] labelPropertyValue = line.split(":")[0].split("#");
			// if (labelPropertyValue[0].trim().contains("Author")
			// || labelPropertyValue[0].trim().contains("Publication")) {
			// continue;
			// }

			File fout = new File(middleFile);
			FileOutputStream fos = new FileOutputStream(fout);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

			// String writable = labelPropertyValue[0].trim() + "|" +
			// labelPropertyValue[1].trim() + ":" + "\""
			// + labelPropertyValue[2].trim() + "\"";

			if (line.contains("=")) {
				line = line.substring(0, line.indexOf('='));
			}

			String writable = line;

			bw.write(writable);
			bw.close();

			System.out.println();
			System.out.println(writable);
			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			fillSetFromFile(middleFile);
			fillFocusNodesOfRequestedTypes(bwCitation, writable);
			System.out.println();
		}

		br.close();

		tx1.success();

		bwCitation.close();
		dataGraph.shutdown();
		System.out.println("finished!");

	}

	private void fillSetFromFile(String focusSetPath) throws Exception {
		// the format should be like:
		// NodeType | key1:value1, key2:value2
		FileInputStream fis = new FileInputStream(focusSetPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] labelAndPropKeyVals = line.trim().split("\\|");
			ArrayList<PairStrings> propKeyValues = new ArrayList<PairStrings>();
			if (labelAndPropKeyVals.length == 1) {
				focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
			} else if (labelAndPropKeyVals.length > 1) {
				String[] keyValuePairs = labelAndPropKeyVals[1].split(",");
				for (int i = 0; i < keyValuePairs.length; i++) {
					String[] separatedKeyValue = keyValuePairs[i].split(":");
					propKeyValues.add(new PairStrings(separatedKeyValue[0], separatedKeyValue[1].replace("\"", "")));
				}

			}
			// Assumption: distinct labels
			focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
		}
		br.close();
	}

	private void fillFocusNodesOfRequestedTypes(BufferedWriter bwCitation, String writable) throws Exception {

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		double sumAllOutDegree = 0d;
		double sumAllInDegree = 0d;
		double sumAllDegree = 0d;
		int maxDegree = 0;
		double avgOutDegree = 0;
		double avgInDegree = 0;
		double avgDegree = 0;

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			ArrayList<PairStrings> propVals = focusLabelPropValSet.get(focusLabel);
			for (Node node : dataGraph.getAllNodes()) {
				// boolean isCandidate = true;
				if (!node.hasLabel(Label.label(focusLabel))) {
					continue;
				}
				if (propVals.size() > 0) {
					for (PairStrings pairString : propVals) {

						if (node.hasProperty(pairString.key)) {
							if (node.getProperty(pairString.key).toString().toLowerCase()
									.equals(pairString.value.toLowerCase())
									|| DummyFunctions.isContain(
											node.getProperty(pairString.key).toString().toLowerCase(),
											pairString.value.toLowerCase())) {
								allNodesOfFocusType.get(focusLabel).add((int) node.getId());
								sumAllOutDegree += node.getDegree(Direction.OUTGOING);
								sumAllInDegree += node.getDegree(Direction.OUTGOING);
								sumAllDegree += node.getDegree(Direction.BOTH);
								maxDegree = Math.max(maxDegree, node.getDegree(Direction.BOTH));
								break;
							}
						}

					}
				} else {
					allNodesOfFocusType.get(focusLabel).add((int) node.getId());
				}

			}
		}

		// TraversalDescription graphTraverse =
		// dataGraph.traversalDescription().evaluator(Evaluators.toDepth(2));
		//
		// for (RelationshipType relType : dataGraph.getAllRelationshipTypes())
		// {
		// graphTraverse = graphTraverse.relationships(relType,
		// Direction.OUTGOING);
		// }

		int numberOfAllFocusNodes = 0;
		HashSet<Integer> allNodesInTwoHopsSet = new HashSet<Integer>();
		int allNodesInTwoHops = 0;

		// System.out.println(allNodesOfFocusType);

		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				System.err.println(("no items for \"" + key + "\""));
				return;
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
			System.out.println("numberOfAllFocusNodes: " + numberOfAllFocusNodes);
			avgOutDegree = sumAllOutDegree / numberOfAllFocusNodes;
			avgInDegree = sumAllInDegree / numberOfAllFocusNodes;
			avgDegree = sumAllDegree / numberOfAllFocusNodes;
			System.out.println("focus avgInDegree: " + avgInDegree);
			System.out.println("focus avgOutDegree: " + avgOutDegree);
			System.out.println("focus avgDegree: " + avgDegree);

			for (Integer nodeId : allNodesOfFocusType.get(key)) {
				String output = "";
				TraversalDescription twoHopsTraversal = dataGraph.traversalDescription().breadthFirst()
						.uniqueness(Uniqueness.NODE_GLOBAL).evaluator(Evaluators.toDepth(2));

				for (Node currentNode : twoHopsTraversal.traverse(dataGraph.getNodeById(nodeId)).nodes()) {
					allNodesInTwoHopsSet.add((int) currentNode.getId());
					allNodesInTwoHops++;
				}

			}
		}

		System.out.println("allNodesAfterSomeHops Distinct: " + allNodesInTwoHopsSet.size());
		System.out.println("allNodesAfterSomeHops: " + allNodesInTwoHops);

		int maxDegreeOfAllNodes = 0;
		HashSet<String> lbls = new HashSet<String>();
		for (Integer nodeId : allNodesInTwoHopsSet) {
			String lbl = dataGraph.getNodeById(nodeId).getLabels().iterator().next().name().toString();
			maxDegreeOfAllNodes = Math.max(dataGraph.getNodeById(nodeId).getDegree(Direction.OUTGOING),
					maxDegreeOfAllNodes);
			if (lbls.add(lbl)) {
				System.out.print(lbl + ", ");
			}
		}

		System.out.println();
		System.out.println("maxDegree of focuses: " + maxDegree);
		System.out.println("maxDegree of all: " + maxDegreeOfAllNodes);
		System.out.println("all related lbls: " + lbls.size());
		System.out.println();

		bwCitation.write(writable + "#" + numberOfAllFocusNodes + "#" + avgDegree + "#" + allNodesInTwoHopsSet.size()
				+ "#" + allNodesInTwoHops + "#" + lbls.size() + "#" + maxDegreeOfAllNodes + "#" + maxDegree + "#"
				+ "\n");
		bwCitation.flush();

	}

	private void addNextNodesSet(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph, HashSet<Integer> tempNeighbors) {
		// recursion base case: as Integer as we haven't
		// exceeded the max number of hops, keep going
		if (currentHop < maxDHops) {
			Node c;
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.OUTGOING)) {
				c = r.getEndNode();
				int cId = (int) c.getId();
				if (Math.abs(maxDHops) > 1)
					addNextNodesSet(parentId, cId, maxDHops, currentHop + 1, dataGraph, tempNeighbors);

				tempNeighbors.add(cId);
			}
		} else
			return;

	}

	private void addPreviousNodesSet(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph, HashSet<Integer> tempNeighbors) {
		if (currentHop > maxDHops) {
			Node c;
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.INCOMING)) {
				c = r.getStartNode();
				int cId = (int) c.getId();

				if (Math.abs(maxDHops) > 1)
					addPreviousNodesSet(parentId, cId, maxDHops, currentHop - 1, dataGraph, tempNeighbors);

				tempNeighbors.add(cId);

			}

		} else
			return;

	}

}

class Holder {
	String country;
	HashMap<String, Integer> jurisdictioFrequency = new HashMap<String, Integer>();
}
