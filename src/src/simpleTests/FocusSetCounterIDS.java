package src.simpleTests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import edu.stanford.nlp.pipeline.CoreNLPProtos.Relation;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class FocusSetCounterIDS {

	private static final String outputFile = "reasonableFocusSetIDS.txt";
	// private static final String middleFile = "focusSetIDS.txt";
	private static String allFocusLinesPath = "/Users/mnamaki/Desktop/IDSTokens.txt";
	private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationV4/citation.graphdb";

	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	String focusSetPath = null;
	GraphDatabaseService dataGraph;

	public FocusSetCounterIDS(String[] args) throws Exception {
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

		FocusSetCounterIDS fsc = new FocusSetCounterIDS(args);
		// fsc.run();
		// fsc.typeToType();
		fsc.getUsefulNodes();

	}

	private void getUsefulNodes() throws Exception {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
		Transaction tx1 = dataGraph.beginTx();

		int numberOfAllFocusNodes = 0;
		HashSet<Integer> allNodesAfterSomeHopsSet = new HashSet<Integer>();
		int allNodesAfterSomeHops = 0;

		HashSet<Integer> focusNodeIds = new HashSet<Integer>();

		FileInputStream fis = new FileInputStream("recommendationFocuses.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		while ((line = br.readLine()) != null) {
			focusNodeIds.add(Integer.parseInt(line.trim()));
		}

		br.close();

		for (Integer focusNodeId : focusNodeIds) {

			HashSet<Integer> tempNeighbors = new HashSet<Integer>();
			addNextNodesSet(focusNodeId, focusNodeId, 2, 0, dataGraph, tempNeighbors);
			addPreviousNodesSet(focusNodeId, focusNodeId, -2, 0, dataGraph, tempNeighbors);

			allNodesAfterSomeHops += tempNeighbors.size();
			allNodesAfterSomeHopsSet.addAll(tempNeighbors);
		}
		
		System.out.println("allNodesAfterSomeHops: " + allNodesAfterSomeHops);
		System.out.println("allNodesAfterSomeHopsSet: " + allNodesAfterSomeHopsSet.size());

		HashSet<Long> rels = new HashSet<Long>();
		for(Relationship rel : dataGraph.getAllRelationships()){
			if(allNodesAfterSomeHopsSet.contains((int)rel.getStartNode().getId()) && allNodesAfterSomeHopsSet.contains((int)rel.getEndNode().getId())){
				rels.add(rel.getId());
			}
		}
		System.out.println("rels: " + rels.size());
		
		tx1.success();
		dataGraph.shutdown();
	}

	private void typeToType() {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
		Transaction tx1 = dataGraph.beginTx();
		HashSet<String> ttt = new HashSet<String>();
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (ttt.add(rel.getStartNode().getLabels().iterator().next().name() + "->"
					+ rel.getEndNode().getLabels().iterator().next().name())) {
				System.out.println(rel.getStartNode().getLabels().iterator().next().name() + "->"
						+ rel.getEndNode().getLabels().iterator().next().name());
			}
		}
		tx1.success();
		dataGraph.shutdown();

	}

	private void run() throws Exception {
		File fout0 = new File(outputFile);
		FileOutputStream fos0 = new FileOutputStream(fout0);
		BufferedWriter bwCitation = new BufferedWriter(new OutputStreamWriter(fos0));

		bwCitation.write(
				"writable#numberOfAllFocusNodes#avgOutDegree#allNodesAfterSomeHopsSet.size()#allNodesAfterSomeHops#\n");

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		// DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		System.out.println("AvgOutDegrees: " + DummyFunctions.getAvgOutDegrees(dataGraph));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			String labelPropertyValue = line.split(":")[0] + ":" + line.split(":")[1];
			String writable = labelPropertyValue.trim();
			// if (labelPropertyValue[0].trim().contains("Author")
			// || labelPropertyValue[0].trim().contains("Publication")) {
			// continue;
			// }

			// File fout = new File(middleFile);
			// FileOutputStream fos = new FileOutputStream(fout);
			//
			// BufferedWriter bw = new BufferedWriter(new
			// OutputStreamWriter(fos));

			// String writable = labelPropertyValue[0].trim() + "|" +
			// labelPropertyValue[1].trim() + ":" + "\""
			// + labelPropertyValue[2].trim() + "\"";

			// String writable = line;

			// bw.write(writable);
			// bw.close();

			System.out.println(writable);
			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			fillSetFromFile(writable);
			fillFocusNodesOfRequestedTypes(bwCitation, writable);
			System.out.println();
		}

		br.close();

		tx1.success();

		bwCitation.close();
		dataGraph.shutdown();
		System.out.println("finished!");

	}

	private void fillSetFromFile(String line) throws Exception {
		// the format should be like:
		// NodeType | key1:value1, key2:value2
		// FileInputStream fis = new FileInputStream(focusSetPath);
		// BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		// String line = null;
		// while ((line = br.readLine()) != null) {
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
		// }
		// br.close();
	}

	private void fillFocusNodesOfRequestedTypes(BufferedWriter bwCitation, String writable) throws Exception {

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		double sumAllOutDegree = 0d;
		int maxDegree = 0;
		double avgOutDegree = 0;

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
								maxDegree = Math.max(maxDegree, node.getDegree(Direction.OUTGOING));
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
		HashSet<Integer> allNodesAfterSomeHopsSet = new HashSet<Integer>();
		int allNodesAfterSomeHops = 0;
		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				System.err.println(("no items for \"" + key + "\""));
				return;
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
			for (Integer nodeId : allNodesOfFocusType.get(key)) {

				HashSet<Integer> tempNeighbors = new HashSet<Integer>();
				addNextNodesSet(nodeId, nodeId, 2, 0, dataGraph, tempNeighbors);
				addPreviousNodesSet(nodeId, nodeId, -2, 0, dataGraph, tempNeighbors);

				// Traverser tr =
				// graphTraverse.traverse(dataGraph.getNodeById(nodeId));

				//
				// for (Node node : tr.nodes()) {
				// tempNeighbors.add(node.getId());
				// }

				allNodesAfterSomeHops += tempNeighbors.size();
				allNodesAfterSomeHopsSet.addAll(tempNeighbors);
			}
		}

		System.out.println("numberOfAllFocusNodes: " + numberOfAllFocusNodes);
		avgOutDegree = sumAllOutDegree / numberOfAllFocusNodes;
		System.out.println("focust avgOutDegree: " + avgOutDegree);
		System.out.println("allNodesAfterSomeHops Distinct: " + allNodesAfterSomeHopsSet.size());
		System.out.println("allNodesAfterSomeHops: " + allNodesAfterSomeHops);

		int maxDegreeOfAllNodes = 0;
		HashSet<String> lbls = new HashSet<String>();
		for (Integer nodeId : allNodesAfterSomeHopsSet) {
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

		if (avgOutDegree < 3.5 && avgOutDegree > 0 && lbls.size() < 50 && allNodesAfterSomeHops < 1500
				&& allNodesAfterSomeHopsSet.size() < 500 && maxDegree < 100 && maxDegreeOfAllNodes < 100
				&& allNodesAfterSomeHopsSet.size() > 50) {
			bwCitation.write(writable + "#" + numberOfAllFocusNodes + "#" + avgOutDegree + "#"
					+ allNodesAfterSomeHopsSet.size() + "#" + allNodesAfterSomeHops + "#" + "\n");
			bwCitation.flush();
		}
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
		// recursion base case: as Integer as we haven't
		// exceeded the max number of hops, keep going
		if (currentHop < maxDHops) {
			Node c;
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.INCOMING)) {
				c = r.getEndNode();
				int cId = (int) c.getId();
				if (Math.abs(maxDHops) > 1)
					addPreviousNodesSet(parentId, cId, maxDHops, currentHop - 1, dataGraph, tempNeighbors);

				tempNeighbors.add(cId);
			}
		} else
			return;

	}

	class PairStrings {
		public String key;
		public String value;

		public PairStrings(String key, String value) {
			this.key = key;
			this.value = value;

		}

	}
}
