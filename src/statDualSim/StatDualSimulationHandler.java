package statDualSim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import src.utilities.DefaultLabeledEdge;
import src.utilities.PatternNode;

public class StatDualSimulationHandler {

	public static HashMap<PatternNode, HashSet<Integer>> computeMatchSetOfAPatternStat(GraphDatabaseService dataGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> query) throws Exception {

		HashMap<PatternNode, HashSet<Integer>> dsim = new HashMap<PatternNode, HashSet<Integer>>();

		for (PatternNode pNode : query.vertexSet()) {
			for (Node node : dataGraph.getAllNodes()) {
				if (node.getLabels().iterator().next().name().equals(pNode.getLabel())) {
					dsim.putIfAbsent(pNode, new HashSet<>());
					dsim.get(pNode).add((int) node.getId());
				}
			}
		}

		return StatDualSimulation.customizedMatchListStat(dataGraph, query, dsim);

	}

}
