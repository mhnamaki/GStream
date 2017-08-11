/*
This file contains methods that are used in the src.dualSimulation algorithm.
Originally written by: Peng
Modified for noe4j by: Shayan Monadjemi
June 7th, 2016
 */

package statDualSim;

import org.jgrapht.DirectedGraph;
import org.neo4j.graphdb.*;

import src.utilities.DefaultLabeledEdge;
import src.utilities.PatternNode;

import java.util.*;

/**
 * Simulation.
 */
public class StatDSUtility {

	public static boolean isNextSimulatedNew(Map<PatternNode, HashSet<Integer>> sim, Integer vId, PatternNode u,
			DirectedGraph<PatternNode, DefaultLabeledEdge> Q, GraphDatabaseService dataGraph) {

		// for each edge in Q : e.g. A*->B
		for (DefaultLabeledEdge e : Q.outgoingEdgesOf(u)) {
			// children of vId with the desired type
			PatternNode uNext = Q.getEdgeTarget(e);

			Set<Integer> vNextIds = new HashSet<Integer>();
			for (Relationship rel : dataGraph.getNodeById(vId).getRelationships(Direction.OUTGOING)) {
				if (rel.getType().name().equals(e.getType())
						&& rel.getEndNode().getLabels().iterator().next().name().equals(uNext.getLabel())) {
					vNextIds.add((int) rel.getEndNode().getId());
				}
			}

			if (vNextIds.isEmpty())
				return false;

			HashSet<Integer> uNextSimMatches = sim.get(uNext);

			HashSet<Integer> intersection = new HashSet<Integer>(vNextIds);
			intersection.retainAll(uNextSimMatches);

			if (intersection.isEmpty())
				return false;
		}

		return true;
	}

	public static boolean isPrevSimulatedNew(Map<PatternNode, HashSet<Integer>> sim, Integer vId, PatternNode u,
			DirectedGraph<PatternNode, DefaultLabeledEdge> Q, GraphDatabaseService dataGraph) {

		for (DefaultLabeledEdge e : Q.incomingEdgesOf(u)) {
			// children of vId with the desired type
			PatternNode uPrev = Q.getEdgeSource(e);

			Set<Integer> vPrevIds = new HashSet<Integer>();
			for (Relationship rel : dataGraph.getNodeById(vId).getRelationships(Direction.INCOMING)) {
				if (rel.getType().name().equals(e.getType())
						&& rel.getStartNode().getLabels().iterator().next().name().equals(uPrev.getLabel())) {
					vPrevIds.add((int) rel.getStartNode().getId());
				}
			}

			if (vPrevIds.isEmpty())
				return false;

			HashSet<Integer> uPrevSimMatches = sim.get(uPrev);

			HashSet<Integer> intersection = new HashSet<Integer>(vPrevIds);
			intersection.retainAll(uPrevSimMatches);

			if (intersection.isEmpty())
				return false;
		}
		return true;
	}
}
