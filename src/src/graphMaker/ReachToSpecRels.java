package src.graphMaker;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

import src.alg1.prefixTree.PrefixTreeAlg1;
import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;
import src.utilities.Bitmap;
import src.utilities.Dummy;
import src.utilities.InfoHolder;
import src.utilities.PatternNode;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Dummy.DummyProperties;

public class ReachToSpecRels {

	public String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/pana_spec_2005/ooo/g0.graphdb";
	public String dateFormat = "dd-MMM-yyyy";
	public boolean debugMode;

	public GraphDatabaseService dataGraph;

	public String deltaEFileOrFiles = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/pana_spec_2005/ooo/";

	private int maxInsertedEdges = Integer.MAX_VALUE;
	private int maxDeletedEdges;

	public void addRemoveRels() throws Exception {

		if (maxInsertedEdges == 0) {
			Dummy.DummyProperties.debugMode = debugMode;

			// initialize data graph
			File storeDir = new File(dataGraphPath);
			dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
					.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

			DummyFunctions.registerShutdownHook(dataGraph);

			Transaction tx1 = dataGraph.beginTx();

			SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaEFileOrFiles, dateFormat);

			// int allEdgesUpdates = -1;
			// int addedEdges = 0;
			// int removedEdges = 0;
			// int nonAffectedInsertedEdges = 0;
			// int nonAffectedDeletedEdges = 0;
			// double handlingDataStreamDuration = 0;
			// ArrayList<Double> insertionResponseTimes = new
			// ArrayList<Double>();
			// ArrayList<Double> deletionResponseTimes = new
			// ArrayList<Double>();
			Node srcNode = null;
			Node destNode = null;
			String srcLabel = null;
			String destLabel = null;
			Integer srcNodeId = null;
			Integer destNodeId = null;
			// Integer numberOfAffectedPatterns = 0;
			// Integer numberOfAffectedPatternsWithoutRecheck = 0;

			// Double allEdgesResponseTime = 0.0d;
			// ArrayList<PrefixTreeNode<PrefixTreeNodeData>> affectedPatterns =
			// new
			// ArrayList<PrefixTreeNode<PrefixTreeNodeData>>();
			// int numberOfInsertedMinusDeletedEdges = 0;
			int numberOfDeletedMinusInsertedEdges = 0;
			int cnt = 0;
			while (numberOfDeletedMinusInsertedEdges < maxDeletedEdges) {

				StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

				if (nextStreamEdge == null)
					break;

				srcNodeId = (int) nextStreamEdge.getSourceNode();
				destNodeId = (int) nextStreamEdge.getDestinationNode();
				srcNode = dataGraph.getNodeById(srcNodeId);
				destNode = dataGraph.getNodeById(destNodeId);
				srcLabel = srcNode.getLabels().iterator().next().name();
				destLabel = destNode.getLabels().iterator().next().name();

				if (cnt % 100000 == 0) {
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
				if (cnt % 5000 == 0)
					System.out.println(cnt + ": " + " nextStreamEdge: " + srcLabel + "_" + srcNodeId + " -> "
							+ destLabel + "_" + destNodeId + " => isAdded? " + nextStreamEdge.isAdded()
							+ "   numberOfDeletedMinusInserted = " + numberOfDeletedMinusInsertedEdges);
				cnt++;

				if (nextStreamEdge.isAdded()) {
					numberOfDeletedMinusInsertedEdges--;
					srcNode.createRelationshipTo(destNode,
							RelationshipType.withName(nextStreamEdge.getRelationshipType()));
				} else {
					numberOfDeletedMinusInsertedEdges++;
					for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
							RelationshipType.withName(nextStreamEdge.getRelationshipType()))) {
						if (rel.getEndNode().getId() == destNodeId) {
							rel.delete();
						}
					}
				}
			}

			System.out.println(snapshotSim.getCurrentDeltaFileName());
			System.out.println(snapshotSim.getCurrentEdgeStreamLine());
			System.out.println("cnt: " + cnt);

			int relCnt = 0;
			for (Relationship rel : dataGraph.getAllRelationships()) {
				relCnt++;
			}
			System.out.println("relCnt: " + relCnt);

			int nodeCnt = 0;
			for (Node node : dataGraph.getAllNodes()) {
				nodeCnt++;
			}
			System.out.println("nodeCnt: " + nodeCnt);

			tx1.success();
			tx1.close();

			dataGraph.shutdown();
		}

		if (maxDeletedEdges == 0) {

			Dummy.DummyProperties.debugMode = debugMode;

			// initialize data graph
			File storeDir = new File(dataGraphPath);
			dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
					.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

			DummyFunctions.registerShutdownHook(dataGraph);

			Transaction tx1 = dataGraph.beginTx();

			SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaEFileOrFiles, dateFormat);

			// int allEdgesUpdates = -1;
			// int addedEdges = 0;
			// int removedEdges = 0;
			// int nonAffectedInsertedEdges = 0;
			// int nonAffectedDeletedEdges = 0;
			// double handlingDataStreamDuration = 0;
			// ArrayList<Double> insertionResponseTimes = new
			// ArrayList<Double>();
			// ArrayList<Double> deletionResponseTimes = new
			// ArrayList<Double>();
			Node srcNode = null;
			Node destNode = null;
			String srcLabel = null;
			String destLabel = null;
			Integer srcNodeId = null;
			Integer destNodeId = null;
			// Integer numberOfAffectedPatterns = 0;
			// Integer numberOfAffectedPatternsWithoutRecheck = 0;

			// Double allEdgesResponseTime = 0.0d;
			// ArrayList<PrefixTreeNode<PrefixTreeNodeData>> affectedPatterns =
			// new
			// ArrayList<PrefixTreeNode<PrefixTreeNodeData>>();
			int numberOfInsertedMinusDeletedEdges = 0;
			int numberOfDeletedMinusInsertedEdges = 0;
			int cnt = 0;
			while (numberOfInsertedMinusDeletedEdges < maxInsertedEdges) {

				StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

				if (nextStreamEdge == null)
					break;

				srcNodeId = (int) nextStreamEdge.getSourceNode();
				destNodeId = (int) nextStreamEdge.getDestinationNode();
				srcNode = dataGraph.getNodeById(srcNodeId);
				destNode = dataGraph.getNodeById(destNodeId);
				srcLabel = srcNode.getLabels().iterator().next().name();
				destLabel = destNode.getLabels().iterator().next().name();

				if ((cnt % 100000) == 0) {
					tx1.success();
					tx1.close();
					// System.out.println("tx is commited in " + i + "-th
					// transaction!");
					tx1 = dataGraph.beginTx();

				}
				
				if (cnt % 100000 == 0)
					System.out.println(cnt + ": " + " nextStreamEdge: " + srcLabel + "_" + srcNodeId + " -> "
							+ destLabel + "_" + destNodeId + " => isAdded? " + nextStreamEdge.isAdded());
				cnt++;

				
				if (nextStreamEdge.isAdded()) {
					numberOfInsertedMinusDeletedEdges++;
					srcNode.createRelationshipTo(destNode,
							RelationshipType.withName(nextStreamEdge.getRelationshipType()));
				} else {
					numberOfInsertedMinusDeletedEdges--;
					for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
							RelationshipType.withName(nextStreamEdge.getRelationshipType()))) {
						if (rel.getEndNode().getId() == destNodeId) {
							rel.delete();
						}
					}
				}
			}

			System.out.println(snapshotSim.getCurrentDeltaFileName());
			System.out.println(snapshotSim.getCurrentEdgeStreamLine());
			System.out.println("cnt: " + cnt);

			

			tx1.success();
			tx1.close();

			tx1 = dataGraph.beginTx();
			
			int relCnt = 0;
			for (Relationship rel : dataGraph.getAllRelationships()) {
				relCnt++;
			}
			System.out.println("relCnt: " + relCnt);

			int nodeCnt = 0;
			for (Node node : dataGraph.getAllNodes()) {
				nodeCnt++;
			}
			System.out.println("nodeCnt: " + nodeCnt);
			
			tx1.success();
			tx1.close();
			dataGraph.shutdown();
		}

	}

	public ReachToSpecRels(String[] args) throws Exception {
		// HashMap<String, String> nodeKeyProp = new HashMap<String, String>();

		// nodeKeyProp.put("Author", "Name");
		// nodeKeyProp.put("Paper", "Index");
		// nodeKeyProp.put("Publication_Venue", "Venue_Name");

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-deltaEFileOrFiles")) {
				deltaEFileOrFiles = args[++i];
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			} else if (args[i].equals("-maxInsertedEdges")) {
				maxInsertedEdges = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-maxDeletedEdges")) {
				maxDeletedEdges = Integer.parseInt(args[++i]);
			}
		}

		if (dataGraphPath == null || (maxInsertedEdges == 0 && maxDeletedEdges == 0) || deltaEFileOrFiles == null
				|| dateFormat == null) {
			throw new Exception(
					"input parameters: dataGraphPath and maxInsertedEdges(or)maxDeletedEdges and deltaEFileOrFiles and dateFormat");
		}

		// Path newDGPath =
		// DummyFunctions.copyG0andGetItsNewPath(dataGraphPath);
		// if (newDGPath == null) {
		// throw new Exception("newDGPath is null!");
		// }
		//
		// dataGraphPath = newDGPath.toString();

	}

	// public ReachToSpecRels(String focusSetPath, int maxHops, int maxEdges,
	// String dataGraphPath, boolean debugMode,
	// int k, double threshold, String deltaEFileOrFiles, int
	// numberOfTransactionInASnapshot,
	// int numberOfSnapshots, String dateFormat, int numberOfIgnoranceInitEdges)
	// {
	//
	// this.dataGraphPath = dataGraphPath;
	// this.debugMode = debugMode;
	// this.deltaEFileOrFiles = deltaEFileOrFiles;
	// this.dateFormat = dateFormat;
	// this.maxInsertedEdges = maxInsertedEdges;
	//
	// }

	public static void main(String[] args) throws Exception {
		ReachToSpecRels reachToSpecRels = new ReachToSpecRels(args);
		reachToSpecRels.addRemoveRels();
	}

}
