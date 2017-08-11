package src.simpleTests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import src.utilities.Dummy.DummyFunctions;

public class WhoAreNeighborsOfFocus {

	// private static String allFocusLinesPath =
	// "/Users/mnamaki/Desktop/PanamaTokens2.txt";
	//private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/Panamas/Pan2MRels10k/0.graphdb";
	private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/PanamaBatches5k/0.graphdb";

	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	String focusSetPath = null;
	GraphDatabaseService dataGraph;

	public static void main(String[] args) throws Exception {
		WhoAreNeighborsOfFocus fsc = new WhoAreNeighborsOfFocus();
		fsc.run();

	}

	private void run() throws Exception {

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		System.out.println("AvgOutDegrees: " + DummyFunctions.getAvgOutDegrees(dataGraph));

		fillSetFromFile("focusSetPanama.txt");
		fillFocusNodesOfRequestedTypes();
		System.out.println();

		tx1.success();

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

	private boolean fillFocusNodesOfRequestedTypes() throws Exception {

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		double sumAllOutDegree = 0;
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

		double numberOfAllFocusNodes = 0;
		HashSet<Integer> allNodesAfterSomeHopsSet = new HashSet<Integer>();
		int allNodesAfterSomeHops = 0;
		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				System.err.println("no items for \"" + key + "\"");
				return false;
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
			for (Integer nodeId : allNodesOfFocusType.get(key)) {

				HashSet<Integer> tempNeighbors = new HashSet<Integer>();
				addNextNodesSet(nodeId, nodeId, 2, 0, dataGraph, tempNeighbors);

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

		HashSet<String> lblsSet = new HashSet<String>();
		for (Integer nodeId : allNodesAfterSomeHopsSet) {
			for (Label lbl : dataGraph.getNodeById(nodeId).getLabels()) {
				lblsSet.add(lbl.toString());
			}
		}
		for (String lbl : lblsSet) {
			System.out.print(lbl + ", ");
		}
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();

		// if (avgOutDegree < 5 && avgOutDegree > 0 &&
		// allNodesAfterSomeHopsSet.size() < 1000
		// && allNodesAfterSomeHops < 1500) {
		// bwCitation.write(writable + "#" + numberOfAllFocusNodes + "#" +
		// avgOutDegree + "#"
		// + allNodesAfterSomeHopsSet.size() + "#" + allNodesAfterSomeHops + "#"
		// + "\n");
		// bwCitation.flush();

		return true;
		// }
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
}
