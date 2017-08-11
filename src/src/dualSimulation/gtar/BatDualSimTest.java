package src.dualSimulation.gtar;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.neo4j.cypher.internal.frontend.v2_3.ast.functions.Abs;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.alg1.prefixTree.PrefixTreeAlg1;

import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.utilities.PatternNode;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by shayan on 6/16/16.
 */
public class BatDualSimTest {

	public static void main(String[] args) {
		/**
		 * Test with PrefixTree
		 */

		String dataGraphPath = "-dataGraphPath /home/shayan/Documents/WSU/Data/ExpansionTestData1/ExpansionTestGraphDatabase";
		String focusSetPath = "-focusSetPath /home/shayan/Documents/WSU/Data/ExpansionTestData1";
		String maxAllowedHops = "-maxAllowedHops 3";

		String[] arguments = { dataGraphPath, focusSetPath, maxAllowedHops };

		// try
		// {
		// PrefixTree prefixTree = new PrefixTree(arguments);
		// PrefixTreeNode<PrefixTreeNodeData> rootNode = prefixTree.generate();
		// }
		// catch (Exception e )
		// {
		//
		// }

		// DirectedGraph<PatternNode, DefaultEdge> Q = new
		// DefaultDirectedGraph<PatternNode, DefaultEdge>(DefaultEdge.class);
		// PatternNode zero = new PatternNode("mongst");
		// PatternNode one = new PatternNode("prentice");
		// PatternNode two = new PatternNode("midst");
		// PatternNode three = new PatternNode("midst");
		//
		// Q.addVertex(zero);
		// Q.addVertex(one);
		// Q.addVertex(two);
		// Q.addVertex(three);
		//
		// Q.addEdge(zero, one);
		// Q.addEdge(two, three);
		//
		//
		// File storeDir = new
		// File("/home/shayan/Documents/WSU/Data/Synthetic/SyntheticG.graphdb");
		// GraphDatabaseService dataGraph = new
		// GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
		// .setConfig(GraphDatabaseSettings.pagecache_memory,
		// "6g").newGraphDatabase();
		// System.out.println("All data ready to go. Running dualSim...");
		//
		//
		//
		// Map<PatternNode, List<Long>> dsim = BatDualSimulation.run(dataGraph,
		// Q);
		//
		// System.out.println(dsim);

	}
}
