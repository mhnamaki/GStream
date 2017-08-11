package src.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import src.alg1.prefixTree.PrefixTreeAlg1;
import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;

public class Bitmap2 extends Bitmap {

	private static String dataGraphPath;
	private static GraphDatabaseService dataGraph;

	public boolean isInitialized = false;

	// public static RoaringBitmap[] patternNodeIndexOfNodeIds;

	// node Id -> <pattern Id, pattern node Id> as pattern node index
	public static RoaringBitmap[] patternNodeIndexOfNodeIds;

	// index of array corresponds to the pattern Id -> <candidates and matches> 
	public static ArrayList<CandidateSetMatchSetPair> nodeIdsOfPatternNodeIndex = new ArrayList<CandidateSetMatchSetPair>();

	public Bitmap2() {

	}

	public void store(HashMap<Integer, PrefixTreeNode<IPrefixTreeNodeData>> prefixTreeNodeIndex,
			GraphDatabaseService dataGraph) {

		// for each pattern, we want to save the node ids which is there.
		long maxNodeId = 0;
		int nodeCnt = 0;
		Transaction tx1 = dataGraph.beginTx();
		for (Node node : dataGraph.getAllNodes()) {
			maxNodeId = Math.max(maxNodeId, node.getId());
			nodeCnt++;
		}
		tx1.success();
		tx1.close();

		System.out.println("maxNodeId: " + maxNodeId);
		System.out.println("nodeCnt: " + nodeCnt);

		// RoaringBitmap[] rBitmapsOfPatterns = new
		// RoaringBitmap[prefixTreeNodeIndex.size()];
		// for (Integer patternId : prefixTreeNodeIndex.keySet()) {
		// rBitmapsOfPatterns[patternId] = new RoaringBitmap();
		// for (ConcreteMatch concreteMatch :
		// prefixTreeNodeIndex.get(patternId).getData().matchGraphs) {
		// for (Long nodeId : concreteMatch.patternNodeOfNeo4jNode.keySet()) {
		// rBitmapsOfPatterns[patternId].add(nodeId, nodeId + 1);
		// }
		// }
		// }

	}

	public RoaringBitmap[] storeAllPatternIndexByNodeId(IPrefixTree prefixTree) {

		patternNodeIndexOfNodeIds = new RoaringBitmap[(int) Math.max(
				prefixTree.getLabelAdjacencyIndexer().maxNodeId + 1,
				prefixTree.getLabelAdjacencyIndexer().numberOfNodesInGraph0)];

		for (int i = 0; i < patternNodeIndexOfNodeIds.length; i++) {
			patternNodeIndexOfNodeIds[i] = new RoaringBitmap();
		}

		for (Integer patternId : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			for (Integer nodeId : prefixTree.getPrefixTreeNodeIndex().get(patternId).getData().getMatchedNodes()
					.getPatternNodeOfNeo4jNode().keySet()) {
				patternNodeIndexOfNodeIds[nodeId.intValue()].add(patternId);
			}

		}

		for (int i = 0; i < patternNodeIndexOfNodeIds.length; i++) {
			patternNodeIndexOfNodeIds[i].runOptimize();
		}

		isInitialized = true;
		return patternNodeIndexOfNodeIds;
	}

	// TODO: may be if we give "this" to other consturctor then we don't need to
	// define it as a static
	public RoaringBitmap[] storeOnePatternIndexByNodeId(IPrefixTree prefixTree, Integer patternId) {

		if (prefixTree.getPrefixTreeNodeIndex().get(patternId) != null) {
			for (Integer nodeId : prefixTree.getPrefixTreeNodeIndex().get(patternId).getData().getMatchedNodes()
					.getPatternNodeOfNeo4jNode().keySet()) {
				patternNodeIndexOfNodeIds[nodeId.intValue()].add(patternId);
			}
		}
		return patternNodeIndexOfNodeIds;
	}

	public void updateBitmapByPairOfPatternAndNodeId(Integer patternId, Integer nodeId) {
		patternNodeIndexOfNodeIds[nodeId].add(patternId);
	}

	public void printBitmap() {

	}

	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			}
		}

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		RoaringBitmap[] rr2 = new RoaringBitmap[5];
		for (int i = 0; i < 5; i++) {
			rr2[i] = new RoaringBitmap();
		}
		try (Transaction tx1 = dataGraph.beginTx()) {

			for (Node node : dataGraph.getAllNodes()) {
				int div = ((int) node.getId() % 5);
				rr2[div].add(((int) node.getId()));
			}

			System.out.println();

			rr2[0].forEach(new IntConsumer() {
				@Override
				public void accept(int value) {
					System.out.println(value);
				}
			});

		} catch (Exception exc) {
			exc.printStackTrace();
		}

	}

	public void removeOnePatternIndexForAllNodesHavingIt(Integer patternPrefixTreeNodeIndex) {
		for (int i = 0; i < patternNodeIndexOfNodeIds.length; i++) {
			patternNodeIndexOfNodeIds[i].remove(patternPrefixTreeNodeIndex);
		}
	}

	public void removeNodeIdFromPatternId(Integer destNodeId, Integer patternPrefixTreeNodeIndex) {
		// System.out.println("bitmap.removeNodeIdFromPatternId(" + destNodeId +
		// ", " + patternPrefixTreeNodeIndex + ")");
		patternNodeIndexOfNodeIds[destNodeId].remove(patternPrefixTreeNodeIndex);

	}
}