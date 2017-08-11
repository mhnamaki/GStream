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

public class IDSPlay {

	private static final String outputFile = "reasonableFocusSetIDS.txt";
	private static final String middleFile = "focusSetIDS.txt";
	private static String allFocusLinesPath = "/Users/mnamaki/Desktop/IDSTokens200.txt";
	private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/IDSAttack/idsJun17_V3.db";

	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	String focusSetPath = null;
	GraphDatabaseService dataGraph;

	public IDSPlay(String[] args) throws Exception {
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

		IDSPlay fsc = new IDSPlay(args);
		// fsc.run();
		System.out.println("start");
		// fsc.findNodesWithHighDegree();

		fsc.play();

	}

	private void play() {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
		Transaction tx1 = dataGraph.beginTx();

		HashSet<String> keySet = new HashSet<String>();
		HashSet<String> valueSet = new HashSet<String>();
		int cnt = 0;
		Node node = dataGraph.getNodeById(401916);

		HashSet<String> endPointsType = new HashSet<String>();

		for (Node node1: dataGraph.getAllNodes()) {
			for (Relationship rel : node1.getRelationships()) {
				// Node otherNode = rel.getOtherNode(node);
				// System.out.println(otherNode.getId() + " " +
				// otherNode.getAllProperties());
				endPointsType.add(rel.getStartNode().getLabels().iterator().next() + "-" + rel.getType() + "-" + rel.getEndNode().getLabels().iterator().next());
			}
		}
		// for (Node node : dataGraph.getAllNodes()) {
		// if
		// (node.getLabels().iterator().next().name().toString().toLowerCase().equals("alert"))
		// {
		// if (node.hasProperty("tag ") && !node.getProperty("tag
		// ").toString().toLowerCase().equals("normal")) {
		// System.out.println(node.getId() + " => " + node.getAllProperties());
		// }
		// }
		// for (Sبوفهtring key2 : node.getAllProperties().keySet()) {
		// keySet.add(key2);
		// valueSet.add(node.getAllProperties().get(key2).toString().toLowerCase());
		// cnt++;
		// if (cnt % 1000000 == 0) {
		// System.out.println("Cnt:" + cnt);
		// }
		// }
		// }

		// System.out.println("keys");
		//
		// for (String k : keySet) {
		// System.out.println(k);
		// }
		// System.out.println();
		// System.out.println();
		//
		System.out.println("endPointsType");
		for (String v : endPointsType) {
			System.out.println(v);
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}

	private void findNodesWithHighDegree() {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
		Transaction tx1 = dataGraph.beginTx();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.getDegree() > 175533) {
				for (Relationship rel : node.getRelationships(Direction.INCOMING)) {
					rel.delete();
				}
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
				System.out.println("commited");
			}
		}

		System.out.println("all rels: " + DummyFunctions.getNumberOfAllRels(dataGraph));

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void run() throws Exception {

		File fout0 = new File(outputFile);
		FileOutputStream fos0 = new FileOutputStream(fout0);
		BufferedWriter bwCitation = new BufferedWriter(new OutputStreamWriter(fos0));

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");

		Transaction tx1 = dataGraph.beginTx();
		System.out.println("AvgOutDegrees: " + DummyFunctions.getAvgOutDegrees(dataGraph));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);
		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}

		File fout = new File("test2.txt");
		FileOutputStream fos = new FileOutputStream(fout);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		bw.write("");
		bw.close();
		bwCitation.close();
		br.close();

		// HashSet<Long> logsWithMultiHosts = new HashSet<Long>();
		Label alertLbl = Label.label("Alert");
		Label logLbl = Label.label("Log");
		Label hostLbl = Label.label("Host");

		int cnt = 0;
		for (Node node : dataGraph.getAllNodes()) {

			// if (cnt % 1000000 == 0) {
			// System.out.println("NOPN:" + cnt);
			// }

			if (node.hasLabel(alertLbl) && node.getDegree() < 100000) {
				// int relCnt = 0;
				HashSet<Long> hosts = new HashSet<Long>();
				for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
					// relCnt++;
					// if (relCnt % 1000000 == 0) {
					// System.out.println("NOPR:" + relCnt);
					// }
					if (rel.getEndNode().hasLabel(hostLbl))
						hosts.add(rel.getEndNode().getId());
				}

				if (hosts.size() > 1) {
					System.out.println(node.getId() + ", " + hosts);
				}
			}

			cnt++;
		}

		tx1.success();

		dataGraph.shutdown();
		System.out.println("finished!");

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
