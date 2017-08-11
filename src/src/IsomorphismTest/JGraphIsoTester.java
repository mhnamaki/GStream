package src.IsomorphismTest;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.base.IPrefixTreeNodeData;
import src.utilities.DefaultLabeledEdge;
import src.utilities.PatternNode;
import src.utilities.PrefixTreeNode;

public class JGraphIsoTester {

	public static void main(String[] args) {

		PrefixTreeNode<IPrefixTreeNodeData> testPatternTreeNode; 
		
		//String dataGraphPath = "/Users/mnamaki/Documents/workspace/TopKGStream/GraphDBTestSet/test1.db";
		String dataGraphPath = "/Users/mnamaki/Documents/workspace/TopKGStream/GraphDBTestSet/test2.db";
		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
				DefaultLabeledEdge.class);

		// test1
		// PatternNode a0 = new PatternNode("a", false);
		// PatternNode c4 = new PatternNode("c", false);
		// PatternNode b1 = new PatternNode("b", false);
		// patternGraph.addVertex(a0);
		// patternGraph.addVertex(c4);
		// patternGraph.addVertex(b1);
		// patternGraph.addEdge(a0, c4, new DefaultLabeledEdge("noType"));
		// patternGraph.addEdge(c4, b1, new DefaultLabeledEdge("noType"));

		// test2
		PatternNode a0 = new PatternNode("a", false);
		PatternNode b1 = new PatternNode("b", false);
		patternGraph.addVertex(a0);
		patternGraph.addVertex(b1);
		patternGraph.addEdge(a0, b1, new DefaultLabeledEdge("noType"));
		HashMap<PatternNode, HashSet<Integer>> initialMap = new HashMap<PatternNode, HashSet<Integer>>();
		initialMap.put(a0, new HashSet<Integer>());
		initialMap.put(b1, new HashSet<Integer>());
		initialMap.get(a0).add(0);
		initialMap.get(b1).add(1);
		initialMap.get(b1).add(2);
		
		try {
			Transaction tx1 = dataGraph.beginTx();

			VF2Matcher vf2Matcher = new VF2Matcher();
			
			vf2Matcher.match(dataGraph, patternGraph);
			 
		//	vf2Matcher.matchWithInitialMappedNode(dataGraph, patternGraph, initialMap);
			
			

			tx1.success();
		} catch (Exception exc) {

		} finally {
			dataGraph.shutdown();
		}

	}

}
