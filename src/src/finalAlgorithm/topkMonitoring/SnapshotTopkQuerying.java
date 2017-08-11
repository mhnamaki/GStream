package src.finalAlgorithm.topkMonitoring;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.json.simple.JSONArray;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import com.google.common.collect.MinMaxPriorityQueue;

import edu.stanford.nlp.io.EncodingPrintWriter.err;
import server.EchoServer;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;
import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;
import src.optAlg.prefixTree.PrefixTreeOpt;
import src.finalAlgorithm.prefixTree.PrefixTreeOptBiSim;
import src.finalAlgorithm.prefixTree.PrefixTreeOptBiSim2;
import src.utilities.Bitmap;
import src.utilities.Configuration;
import src.utilities.CorrectnessChecking;
import src.utilities.CorrespondsOfSrcRelDest;
import src.utilities.DebugHelper;
import src.utilities.DefaultLabeledEdge;
import src.utilities.Dummy;
import src.utilities.InfoHolder;
import src.utilities.NodeInfo;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;
import src.utilities.SourceRelDestTriple;
import src.utilities.SourceRelDestTypeTriple;
import src.utilities.SupportComparator;
import src.utilities.SupportHelper;
import src.utilities.TimeLogger;
import src.utilities.TopKHandler;
import src.utilities.Visualizer;
import src.utilities.WinMine;
import statDualSim.StatDualSimulationHandler;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy.DummyProperties.PrefixTreeMode;
import src.utilities.EdgeStreamCacheItem;
import src.utilities.Indexer;

public class SnapshotTopkQuerying {
	public HashMap<Integer, HashSet<Integer>> topksOfAllSnapshots = new HashMap<Integer, HashSet<Integer>>();;
	public HashMap<Integer, Double> processingTimeOfEdges = new HashMap<Integer, Double>();

	public String dataGraphPath;
	public String dateFormat;
	public boolean debugMode;

	public GraphDatabaseService dataGraph;

	public String deltaEFileOrFiles;

	public int numberOfSnapshots;
	public int numberOfAllFocusNodes = 0;

	public String dataGraphName;
	SimpleDateFormat idsFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

	public SnapshotTopkQuerying(String[] args) throws Exception {
		// HashMap<String, String> nodeKeyProp = new HashMap<String, String>();

		// nodeKeyProp.put("Author", "Name");
		// nodeKeyProp.put("Paper", "Index");
		// nodeKeyProp.put("Publication_Venue", "Venue_Name");

		dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Summer2017/GStreamICDE18/IDSDB/deltas/g0.graphdb";
		deltaEFileOrFiles = "/Users/mnamaki/Documents/Education/PhD/Summer2017/GStreamICDE18/IDSDB/deltas/";
		numberOfSnapshots = 100;
		dateFormat = "yyyy-MM-dd'T'hh:mm:ss";

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-deltaEFileOrFiles")) {
				deltaEFileOrFiles = args[++i];
			} else if (args[i].equals("-numberOfSnapshots")) {
				numberOfSnapshots = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			}

		}

		Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
		if (newDGPath == null) {
			throw new Exception("newDGPath is null!");
		}

		dataGraphPath = newDGPath.toString();

	}

	public void snapshotTopkMonitor() throws Exception {

		File fout = new File("results.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		// Queries
		ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> ircQuery = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
				DefaultLabeledEdge.class);

		PatternNode ipNodeSender = new PatternNode("IP", true);
		PatternNode portNodeSender = new PatternNode("Port_6667", false);
		PatternNode portNodeReceiver = new PatternNode("Port", false);
		PatternNode ipNodeReceiver = new PatternNode("IP", true);

		DefaultLabeledEdge ipToPortEdge = new DefaultLabeledEdge("out_port");
		DefaultLabeledEdge ircEdge = new DefaultLabeledEdge("IRC/tcp_ip");
		DefaultLabeledEdge portToIPEdge = new DefaultLabeledEdge("in_port");

		ircQuery.addVertex(ipNodeSender);
		ircQuery.addVertex(portNodeSender);
		ircQuery.addVertex(portNodeReceiver);
		ircQuery.addVertex(ipNodeReceiver);

		ircQuery.addEdge(ipNodeSender, portNodeSender, ipToPortEdge);
		ircQuery.addEdge(portNodeSender, portNodeReceiver, ircEdge);
		ircQuery.addEdge(portNodeReceiver, ipNodeReceiver, portToIPEdge);

		ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> ddosQuery = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
				DefaultLabeledEdge.class);

		PatternNode ipNodeAttacker = new PatternNode("IP", true);
		PatternNode portNodeAttacker = new PatternNode("Port", false);
		PatternNode portNodeWebServer = new PatternNode("Port_80", false);
		PatternNode ipNodeWebServer = new PatternNode("192.168.5.122", true);

		DefaultLabeledEdge attackerToPortEdge = new DefaultLabeledEdge("out_port");
		DefaultLabeledEdge httpWebEdge = new DefaultLabeledEdge("HTTPWeb/tcp_ip");
		DefaultLabeledEdge portToWebServerEdge = new DefaultLabeledEdge("in_port");

		ddosQuery.addVertex(ipNodeAttacker);
		ddosQuery.addVertex(portNodeAttacker);
		ddosQuery.addVertex(portNodeWebServer);
		ddosQuery.addVertex(ipNodeWebServer);

		ddosQuery.addEdge(ipNodeAttacker, portNodeAttacker, attackerToPortEdge);
		ddosQuery.addEdge(portNodeAttacker, portNodeWebServer, httpWebEdge);
		ddosQuery.addEdge(portNodeWebServer, ipNodeWebServer, portToWebServerEdge);

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir).setConfig("cache_type", "none")
				.setConfig(GraphDatabaseSettings.pagecache_memory, "245760").newGraphDatabase();

		Dummy.DummyProperties.NUMBER_OF_SNAPSHOTS = numberOfSnapshots;

		Transaction tx1 = dataGraph.beginTx();

		SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaEFileOrFiles, dateFormat);

		int allEdgesUpdates = -1;
		int addedEdges = 0;
		int removedEdges = 0;

		Node srcNode = null;
		Node destNode = null;

		Integer srcNodeId = null;
		Integer destNodeId = null;

		int i = 0;

		Date oldSnapshotTime = new Date();
		oldSnapshotTime = idsFormatter.parse("2010-06-15T02:00:00");

		long MAX_DURATION = MILLISECONDS.convert(5, MINUTES);

		int snapshot = 0;

		// for (int i = 0; i < numberOfTransactionInASnapshot *
		// (numberOfSnapshots - 1); i++) {
		while (snapshot < (numberOfSnapshots - 1)) {

			allEdgesUpdates++;

			StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

			allEdgesUpdates++;

			if (nextStreamEdge == null) {
				break;
			}

			if ((nextStreamEdge.getTimeStamp().getTime() - oldSnapshotTime.getTime()) > MAX_DURATION) {

				// snapshotIsChanged = true;
				oldSnapshotTime = nextStreamEdge.getTimeStamp();
				snapshot++;
				if (DummyProperties.debugMode) {
					System.out.println("currentSnapshot Time: " + nextStreamEdge.getTimeStamp());
					System.out.println("currentSnapshot number: " + snapshot);
				}

				runQueryAndCaptureInfo(bw, snapshot, ircQuery, "irc", nextStreamEdge.getTimeStamp());
				runQueryAndCaptureInfo(bw, snapshot, ddosQuery, "ddos", nextStreamEdge.getTimeStamp());

			}

			srcNodeId = (int) nextStreamEdge.getSourceNode();
			destNodeId = (int) nextStreamEdge.getDestinationNode();
			srcNode = dataGraph.getNodeById(srcNodeId);
			destNode = dataGraph.getNodeById(destNodeId);

			System.out.println("nextStreamEdge:" + srcNode.getLabels().iterator().next().name() + "_" + srcNodeId + "-"
					+ nextStreamEdge.getRelationshipType() + " -> " + destNode.getLabels().iterator().next().name()
					+ "_" + destNodeId + " => isAdded? " + nextStreamEdge.isAdded() + " t:"
					+ nextStreamEdge.getTimeStamp());

			if (srcNode.hasProperty("uri") && destNode.hasProperty("uri")) {
				System.out.println(srcNode.getProperty("uri").toString() + "-" + nextStreamEdge.getRelationshipType()
						+ " -> " + destNode.getProperty("uri").toString());
			}

			handlingDataStream(nextStreamEdge, srcNode, destNode, srcNodeId, destNodeId);

			if ((i % 10000) == 0) {
				tx1.success();
				tx1.close();
				// System.out.println("tx is commited in " + i + "-th
				// transaction!");
				tx1 = dataGraph.beginTx();

			}
			i++;

		}

		tx1.success();
		tx1.close();

		dataGraph.shutdown();

		System.gc();
		System.runFinalization();

		bw.close();

		System.out.println("end of program!");

	}

	private void runQueryAndCaptureInfo(BufferedWriter bw, int snapshot,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> query, String name, Date date) throws Exception {
		HashMap<PatternNode, HashSet<Integer>> dsim = StatDualSimulationHandler.computeMatchSetOfAPatternStat(dataGraph,
				query);

		int focusMatchCount = 0;
		HashSet<Integer> distinctFocuses = new HashSet<Integer>();
		for (PatternNode patternNode : dsim.keySet()) {
			if (patternNode.isFocus()) {
				focusMatchCount += dsim.get(patternNode).size();
				distinctFocuses.addAll(dsim.get(patternNode));
			}
		}

		bw.write(name + "," + snapshot + "," + distinctFocuses.size() + "," + focusMatchCount + "," + date.toString()
				+ "\n");
		bw.flush();

	}

	public SnapshotTopkQuerying(String focusSetPath, int maxHops, int maxEdges, String dataGraphPath, boolean debugMode,
			int k, double threshold, String deltaEFileOrFiles, int numberOfTransactionInASnapshot,
			int numberOfSnapshots, String dateFormat, int numberOfIgnoranceInitEdges, int edgeStreamCacheCapacity,
			boolean windowMode, int windowSizeL) {

		this.dataGraphPath = dataGraphPath;
		this.debugMode = debugMode;

		this.deltaEFileOrFiles = deltaEFileOrFiles;

		this.numberOfSnapshots = numberOfSnapshots;
		this.dateFormat = dateFormat;

		DummyProperties.windowMode = windowMode;

		DummyProperties.WINDOW_SIZE = windowSizeL;

	}

	public static void main(String[] args) throws Exception {
		SnapshotTopkQuerying snapshotTopkMonitoring = new SnapshotTopkQuerying(args);
		snapshotTopkMonitoring.snapshotTopkMonitor();
	}

	private StreamEdge handlingDataStream(StreamEdge nextStreamEdge, Node srcNode, Node destNode, Integer srcNodeId,
			Integer destNodeId) {

		if (nextStreamEdge.isAdded()) {
			srcNode.createRelationshipTo(destNode, RelationshipType.withName(nextStreamEdge.getRelationshipType()));
		} else {

			for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING)) {
				if (rel.getStartNode().getId() == srcNodeId && rel.getEndNode().getId() == destNodeId) {
					rel.delete();
					break;
				}
			}
		}

		return nextStreamEdge;

	}

}
