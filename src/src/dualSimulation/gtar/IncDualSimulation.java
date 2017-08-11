package src.dualSimulation.gtar;

//import src.dualSimulation.core.Edge;
//import src.dualSimulation.core.Graph;
//import src.dualSimulation.core.Vertex;
import src.dualSimulation.core.utility.Tuple2;
import src.utilities.Indexer;
import src.utilities.PatternNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

////TODO: assumption: the relationships added already.
//
///**
// * Incremental Dual Simulation.
// */
public class IncDualSimulation {
//
//	public static HashMap<PatternNode, HashSet<Integer>> run(GraphDatabaseService G,
//			DirectedGraph<PatternNode, DefaultEdge> Q, List<Relationship> edgeList) throws IOException {
//		HashMap<PatternNode, HashSet<Integer>> verIdx = utility.createVerticesMap(Q, G);
//		HashMap<Integer, HashSet<PatternNode>> revIdx = utility.createReversedMap(verIdx);
//
//		HashMap<PatternNode, HashSet<Integer>> cptSim = BatDualSimulation.run(G, Q);
//
//		HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet = new HashMap<>();
//		for (PatternNode uId : verIdx.keySet()) {
//			for (Integer vId : verIdx.get(uId)) {
//				simSet.put(new Tuple2<>(uId, vId), false);
//			}
//		}
//		for (PatternNode uId : cptSim.keySet()) {
//			for (Integer vId : cptSim.get(uId)) {
//				simSet.put(new Tuple2<>(uId, vId), cptSim.get(uId).contains(vId));
//			}
//		}
//
//		HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet = new HashMap<>();
//		for (PatternNode uId : verIdx.keySet()) {
//			for (Integer vId : verIdx.get(uId)) {
//				canSet.put(new Tuple2<>(uId, vId), new Tuple2<>(false, false));
//			}
//		}
//
//		for (int i = 0; i < edgeList.size(); i++) {
//
//			Relationship e = edgeList.get(i);
//
//			// G.addEdge(e);
//			updateCanSet(canSet, e, G, Q, revIdx);
//			updateSimSet(simSet, e, G, Q, revIdx, canSet);
//		}
//		HashMap<PatternNode, HashSet<Integer>> simRes = getSimResults(simSet);
//		// System.out.println("simRes:" + simRes);
//
//		return simRes;
//	}
//
//	public static HashMap<PatternNode, HashSet<Integer>> run(GraphDatabaseService G,
//			DirectedGraph<PatternNode, DefaultEdge> Q, List<Relationship> edgeList,
//			HashMap<PatternNode, HashSet<Integer>> verIdx, HashMap<Integer, HashSet<PatternNode>> revIdx,
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet) throws IOException {
//
//		// edgeList: new coming edges
//
//		for (int i = 0; i < edgeList.size(); i++) {
//			Relationship e = edgeList.get(i);
//
//			// adding new edges into the data graph
//			// G.addEdge(e);
//
//			// updating the candidates set based on that
//			updateCanSet(canSet, e, G, Q, revIdx);
//
//			// updating the simulation set based on that
//			updateSimSet(simSet, e, G, Q, revIdx, canSet);
//		}
//		HashMap<PatternNode, HashSet<Integer>> simRes = getSimResults(simSet);
//
//		return simRes;
//	}
//
//	public static HashMap<PatternNode, HashSet<Integer>> runforDel(GraphDatabaseService G,
//			DirectedGraph<PatternNode, DefaultEdge> Q, Relationship e, HashMap<PatternNode, HashSet<Integer>> verIdx,
//			HashMap<Integer, HashSet<PatternNode>> revIdx, HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet) throws IOException {
//
//		updateCanSetForDel(canSet, e, G, Q, revIdx);
//		updateSimSetForDel(simSet, e, G, Q, revIdx, canSet);
//		HashMap<PatternNode, HashSet<Integer>> simRes = new HashMap<>();
//
//		return simRes;
//	}
//
//	public static HashMap<PatternNode, HashSet<Integer>> getCanResults(
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet) {
//		HashMap<PatternNode, HashSet<Integer>> canRes = new HashMap<>();
//		for (Tuple2<PatternNode, Integer> tup : canSet.keySet()) {
//			Tuple2<Boolean, Boolean> stats = canSet.get(tup);
//			if (stats._1 && stats._2) {
//				canRes.putIfAbsent(tup._1, new HashSet<>());
//				canRes.get(tup._1).add(tup._2);
//			}
//		}
//
//		return canRes;
//	}
//
//	public static HashMap<PatternNode, HashSet<Integer>> getSimResults(
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet) {
//		HashMap<PatternNode, HashSet<Integer>> simRes = new HashMap<>();
//		for (Tuple2<PatternNode, Integer> tup : simSet.keySet()) {
//			if (simSet.get(tup)) {
//				simRes.putIfAbsent(tup._1, new HashSet<>());
//				simRes.get(tup._1).add(tup._2);
//			}
//		}
//
//		return simRes;
//	}
//
//	public static boolean isSimulated(PatternNode uId, Integer vId,
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet) {
//		if (simSet.get(new Tuple2<>(uId, vId)) == null) {
//			return false;
//		}
//		return simSet.get(new Tuple2<>(uId, vId));
//	}
//
//	public static boolean isCandidate(PatternNode uId, Integer vId,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet) {
//		if (canSet.get(new Tuple2<>(uId, vId)) == null) {
//			return false;
//		}
//		return canSet.get(new Tuple2<>(uId, vId)).equals(new Tuple2<>(true, true));
//	}
//
//	public static boolean attrContains(GraphDatabaseService G, DirectedGraph<PatternNode, DefaultEdge> Q,
//			Set<PatternNode> uIds, Set<Integer> vIds) {
//		Boolean tmp1 = true;
//		for (PatternNode uId : uIds) {
//			Boolean tmp2 = false;
//			for (Integer vId : vIds) {
//				List<Object> uAttr = Q.getVertex(uId).getAttrList();
//				List<Object> vAttr = G.getVertex(vId).getAttrList();
//				if (vAttr.containsAll(uAttr)) {
//					tmp2 = true;
//					break;
//				}
//			}
//			tmp1 = tmp1 && tmp2;
//		}
//
//		return tmp1;
//	}
//
//	public static void updateCanSetForDel(HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet,
//			Relationship e, GraphDatabaseService G, DirectedGraph<PatternNode, DefaultEdge> Q,
//			HashMap<Integer, HashSet<PatternNode>> revIdx) {
//
//		// Vertex uSrc, uDst, vSrc, vDst;
//
//		Integer vSrcId = (int) e.getStartNode().getId();
//		Integer vDstId = (int) e.getEndNode().getId();
//		// vSrc = G.getVertex(vSrcId);
//		// vDst = G.getVertex(vDstId);
//
//		if (revIdx.get(vSrcId) != null && revIdx.get(vDstId) != null) {
//			for (PatternNode uSrcId : revIdx.get(vSrcId)) {
//				for (PatternNode uDstId : revIdx.get(vDstId)) {
//					// uSrc = Q.getVertex(uSrcId);
//					// uDst = Q.getVertex(uDstId);
//
//					// if (canSet.get(new Tuple2<>(uSrcId, vSrcId))._1) {
//					// HashSet<Long> uIds = uSrc.getPrevIds();
//					// HashSet<Long> vIds = vSrc.getPrevIds();
//					// canSet.get(new Tuple2<>(uSrcId, vSrcId))._1 =
//					// attrContains(G, Q, uIds, vIds);
//					// }
//
//					if (canSet.get(new Tuple2<>(uDstId, vDstId))._1) {
//						Set<PatternNode> uIds = uDst.getPrevIds();
//						Set<Integer> vIds = LabelAdjacencyIndexer.getPrevIds(vDstId);
//						canSet.get(new Tuple2<>(uDstId, vDstId))._1 = attrContains(G, Q, uIds, vIds);
//						// System.out.println(uDstId + "->" + vDstId);
//						// System.out.println("uIds:" + uIds);
//						// System.out.println("vIds:" + vIds);
//						// System.out.println(vDst.getPrevNodes());
//						// System.out.println(vDst.getInEdges());
//						// System.out.println(attrContains(G, Q, uIds, vIds));
//					}
//
//					if (canSet.get(new Tuple2<>(uSrcId, vSrcId))._2) {
//						Set<PatternNode> uIds = uSrc.getNextIds();
//						Set<Integer> vIds = LabelAdjacencyIndexer.getNextIds(vSrcId);
//						canSet.get(new Tuple2<>(uSrcId, vSrcId))._2 = attrContains(G, Q, uIds, vIds);
//					}
//
//					// if (canSet.get(new Tuple2<>(uDstId, vDstId))._2) {
//					// HashSet<Long> uIds = uDst.getNextIds();
//					// HashSet<Long> vIds = vDst.getNextIds();
//					// canSet.get(new Tuple2<>(uDstId, vDstId))._2 =
//					// attrContains(G, Q, uIds, vIds);
//					// }
//				}
//			}
//		}
//	}
//
//	public static void updateCanSet(HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet,
//			Relationship e, GraphDatabaseService G, DirectedGraph<PatternNode, DefaultEdge> Q,
//			HashMap<Integer, HashSet<PatternNode>> revIdx) {
//
//		// u: query graph
//		// v: data graph
//
//		// Vertex uSrc, uDst, vSrc, vDst;
//
//		// Id of new coming nodes (data graph)
//		Integer vSrcId = (int) e.getStartNode().getId();
//		Integer vDstId = (int) e.getEndNode().getId();
//
//		// the whole new vertex not just its id
//		// vSrc = G.getVertex(vSrcId);
//		// vDst = G.getVertex(vDstId);
//
//		// if new nodes are related to the query graph nodes in terms of
//		// semantics
//		// TODO: in our case we may not have the new nodes before
//		if (revIdx.get(vSrcId) != null && revIdx.get(vDstId) != null) {
//			for (PatternNode uSrcId : revIdx.get(vSrcId)) {
//				for (PatternNode uDstId : revIdx.get(vDstId)) {
//					// uSrc = Q.getVertex(uSrcId);
//					// uDst = Q.getVertex(uDstId);
//
//					if (Q.incomingEdgesOf(uSrcId).isEmpty()) {
//						canSet.get(new Tuple2<>(uSrcId, vSrcId))._1 = true; // _1
//																			// means
//																			// inEdges
//					}
//					if (Q.outgoingEdgesOf(uDstId).isEmpty()) {
//						canSet.get(new Tuple2<>(uDstId, vDstId))._2 = true; // _2
//																			// means
//																			// outEdges
//					}
//
//					if (!canSet.get(new Tuple2<>(uSrcId, vSrcId))._1) {
//						Set<PatternNode> uIds = Q.incomingEdgesOf(uSrcId);
//
//						Set<Integer> vIds = LabelAdjacencyIndexer.getPrevIds(vSrcId);
//						canSet.get(new Tuple2<>(uSrcId, vSrcId))._1 = attrContains(G, Q, uIds, vIds);
//					}
//
//					if (!canSet.get(new Tuple2<>(uDstId, vDstId))._1) {
//						Set<PatternNode> uIds = Q.outgoingEdgesOf(uDstId);
//						Set<Integer> vIds = LabelAdjacencyIndexer.getPrevIds(vDstId);
//						canSet.get(new Tuple2<>(uDstId, vDstId))._1 = attrContains(G, Q, uIds, vIds);
//					}
//
//					if (!canSet.get(new Tuple2<>(uSrcId, vSrcId))._2) {
//						Set<PatternNode> uIds = uSrc.getNextIds();
//						Set<Integer> vIds = LabelAdjacencyIndexer.getNextIds(vDstId);
//						canSet.get(new Tuple2<>(uSrcId, vSrcId))._2 = attrContains(G, Q, uIds, vIds);
//					}
//
//					if (!canSet.get(new Tuple2<>(uDstId, vDstId))._2) {
//						Set<PatternNode> uIds = uDst.getNextIds();
//						Set<Integer> vIds = LabelAdjacencyIndexer.getNextIds(vDstId);
//						canSet.get(new Tuple2<>(uDstId, vDstId))._2 = attrContains(G, Q, uIds, vIds);
//					}
//				}
//			}
//		}
//	}
//
//	public static void updateSimSet(HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet, Relationship e,
//			GraphDatabaseService G, DirectedGraph<PatternNode, DefaultEdge> Q,
//			HashMap<Integer, HashSet<PatternNode>> revIdx,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet) {
//
//		Integer vSrcId = (int) e.getStartNode().getId();
//		Integer vDstId = (int) e.getEndNode().getId();
//
//		// Vertex vSrc = G.getVertex(vSrcId);
//		// Vertex vDst = G.getVertex(vDstId);
//
//		HashSet<PatternNode> uSrcIdList = revIdx.get(vSrcId);
//		HashSet<PatternNode> uDstIdList = revIdx.get(vDstId);
//
//		if (uSrcIdList != null && uDstIdList != null) {
//			for (PatternNode uSrcId : uSrcIdList) {
//				for (PatternNode uDstId : uDstIdList) {
//					// Vertex uSrc = Q.getVertex(uSrcId);
//					// Vertex uDst = Q.getVertex(uDstId);
//
//					if (isSimulated(uSrcId, vSrcId, simSet) && isSimulated(uDstId, vDstId, simSet)) {
//						// SS: Simulated-Simulated:: Do nothing.
//						// System.out.println("SS");
//					} else if (isSimulated(uSrcId, vSrcId, simSet) && isCandidate(uDstId, vDstId, canSet)) {
//						// SC: Simulated-Candidate
//						// System.out.println("SC");
//						List<Tuple2<PatternNode, Integer>> deltaSimSet = new ArrayList<>();
//						deltaSimSet.add(new Tuple2<>(uDstId, vDstId));
//						if (updateNexts(uDstId, vDstId, Q, G, deltaSimSet, canSet, simSet)
//								&& updatePrevs(uDstId, vDstId, Q, G, deltaSimSet, canSet, simSet)) {
//							for (Tuple2<PatternNode, Integer> tup : deltaSimSet) {
//								simSet.put(tup, true);
//							}
//						}
//					} else if (isCandidate(uSrcId, vSrcId, canSet) && isSimulated(uDstId, vDstId, simSet)) {
//						// CS: Candidate-Simulated
//						// System.out.println("CS");
//						List<Tuple2<PatternNode, Integer>> deltaSimSet = new ArrayList<>();
//						deltaSimSet.add(new Tuple2<>(uSrcId, vSrcId));
//						if (updateNexts(uSrcId, vSrcId, Q, G, deltaSimSet, canSet, simSet)
//								&& updatePrevs(uSrcId, vSrcId, Q, G, deltaSimSet, canSet, simSet)) {
//							for (Tuple2<PatternNode, Integer> tup : deltaSimSet) {
//								simSet.put(tup, true);
//							}
//						}
//					} else if (isCandidate(uSrcId, vSrcId, canSet) && isCandidate(uDstId, vDstId, canSet)) {
//						// CC: Candidate-Candidate
//						// System.out.println("CC");
//						List<Tuple2<PatternNode, Integer>> deltaSimSet = new ArrayList<>();
//						deltaSimSet.add(new Tuple2<>(uSrcId, vSrcId));
//						deltaSimSet.add(new Tuple2<>(uDstId, vDstId));
//						if (updateNexts(uDstId, vDstId, Q, G, deltaSimSet, canSet, simSet)
//								&& updatePrevs(uDstId, vDstId, Q, G, deltaSimSet, canSet, simSet)
//								&& updateNexts(uSrcId, vSrcId, Q, G, deltaSimSet, canSet, simSet)
//								&& updatePrevs(uSrcId, vSrcId, Q, G, deltaSimSet, canSet, simSet)) {
//							for (Tuple2<PatternNode, Integer> tup : deltaSimSet) {
//								simSet.put(tup, true);
//							}
//						}
//					} else {
//						// Non-CC: Do nothing.
//						// System.out.println("Non-CC");
//					}
//				}
//			}
//		}
//	}
//
//	public static void updateSimSetForDel(HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet, Relationship e,
//			GraphDatabaseService G, DirectedGraph<PatternNode, DefaultEdge> Q,
//			HashMap<Integer, HashSet<PatternNode>> revIdx,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet) {
//
//		Integer vSrcId = (int) e.getStartNode().getId();
//		Integer vDstId = (int) e.getEndNode().getId();
//
//		// Vertex vSrc = G.getVertex(vSrcId);
//		// Vertex vDst = G.getVertex(vDstId);
//
//		HashSet<PatternNode> uSrcIdList = revIdx.get(vSrcId);
//		HashSet<PatternNode> uDstIdList = revIdx.get(vDstId);
//
//		if (uSrcIdList != null && uDstIdList != null) {
//			for (PatternNode uSrcId : uSrcIdList) {
//				for (PatternNode uDstId : uDstIdList) {
//					// Vertex uSrc = Q.getVertex(uSrcId);
//					// Vertex uDst = Q.getVertex(uDstId);
//
//					// System.out.println("uSrc, uDst: " + uSrc + "," + uDst);
//
//					if (isSimulated(uSrcId, vSrcId, simSet) && isSimulated(uDstId, vDstId, simSet)) {
//						// System.out.println("Is SS?");
//						// SS: Simulated-Simulated:: Remove simSet recursively.
//
//						updateNextsForDel(uSrcId, vSrcId, Q, G, canSet, simSet);
//						updatePrevsForDel(uDstId, vDstId, Q, G, canSet, simSet);
//					}
//				}
//			}
//		}
//	}
//
//	private static boolean updatePrevsForDel(PatternNode uId, Integer vId, DirectedGraph<PatternNode, DefaultEdge> Q,
//			GraphDatabaseService G, HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet,
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet) {
//		Vertex u = Q.getVertex(uId);
//		// Vertex v = G.getVertex(vId);
//
//		Set<PatternNode> uPrevIds = u.getPrevIds();
//		Set<Integer> vPrevIds = LabelAdjacencyIndexer.getPrevIds(vId);
//
//		Boolean bypass = true;
//		for (PatternNode uPrev : uPrevIds) {
//			Boolean tmp = false;
//			for (Integer vPrev : vPrevIds) {
//				if (isSimulated(uPrev, vPrev, simSet)) {
//					tmp = true;
//					break;
//				}
//			}
//			bypass = bypass && tmp;
//		}
//
//		if (bypass) {
//			return true;
//		} else {
//			Stack<Tuple2<PatternNode, Integer>> stack = new Stack<>();
//			stack.push(new Tuple2<>(uId, vId));
//			Set<Tuple2<PatternNode, Integer>> visitSet = new HashSet<>();
//
//			while (!stack.isEmpty()) {
//				Tuple2<PatternNode, Integer> tup = stack.pop();
//				if (!visitSet.contains(tup)) {
//					visitSet.add(tup);
//					simSet.put(tup, false);
//
//					Vertex uNode = Q.getVertex(tup._1);
//					// Vertex vNode = G.getVertex(tup._2);
//					for (PatternNode uNext : uNode.getNextIds()) {
//						for (Integer vNext : LabelAdjacencyIndexer.getNextIds(tup._2)) {
//							if (isSimulated(uNext, vNext, simSet)) {
//								stack.push(new Tuple2<>(uNext, vNext));
//							}
//						}
//					}
//				}
//			}
//
//			stack = new Stack<>();
//			stack.push(new Tuple2<>(uId, vId));
//			visitSet = new HashSet<>();
//
//			while (!stack.isEmpty()) {
//				Tuple2<PatternNode, Integer> tup = stack.pop();
//				if (!visitSet.contains(tup)) {
//					visitSet.add(tup);
//					simSet.put(tup, false);
//
//					Vertex uNode = Q.getVertex(tup._1);
//					// Vertex vNode = G.getVertex(tup._2);
//
//					for (PatternNode uPrev : uNode.getPrevIds()) {
//						for (Integer vPrev : LabelAdjacencyIndexer.getPrevIds(tup._2)) {
//							if (isSimulated(uPrev, vPrev, simSet)) {
//								stack.push(new Tuple2<>(uPrev, vPrev));
//							}
//						}
//					}
//				}
//			}
//		}
//		return true;
//	}
//
//	private static boolean updatePrevs(PatternNode uId, Integer vId, DirectedGraph<PatternNode, DefaultEdge> Q,
//			GraphDatabaseService G, List<Tuple2<PatternNode, Integer>> deltaSimSet,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet,
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet) {
//		Stack<Tuple2<PatternNode, Integer>> stack = new Stack<>();
//		stack.push(new Tuple2<>(uId, vId));
//		Set<Tuple2<PatternNode, Integer>> visitSet = new HashSet<>();
//		HashMap<Tuple2<PatternNode, Integer>, Tuple2<PatternNode, Integer>> chain = new HashMap<>();
//
//		while (!stack.isEmpty()) {
//			Tuple2<PatternNode, Integer> tup = stack.pop();
//			if (!visitSet.contains(tup)) {
//				visitSet.add(tup);
//				Vertex uNode = Q.getVertex(tup._1);
//				// Vertex vNode = G.getVertex(tup._2);
//
//				for (PatternNode uPrev : uNode.getPrevIds()) {
//					for (Integer vPrev : LabelAdjacencyIndexer.getPrevIds(tup._2)) {
//						if (isSimulated(uPrev, vPrev, simSet) || isCandidate(uPrev, vPrev, canSet)) {
//							stack.push(new Tuple2<>(uPrev, vPrev));
//							chain.put(new Tuple2<>(uPrev, vPrev), tup);
//						} else {
//							Tuple2<PatternNode, Integer> tail = tup;
//							while (chain.get(tail) != null) {
//								tail = chain.remove(tail);
//							}
//						}
//					}
//				}
//			}
//		}
//
//		deltaSimSet.addAll(chain.keySet());
//		deltaSimSet.addAll(chain.values());
//
//		return true;
//	}
//
//	private static boolean updateNextsForDel(PatternNode uId, Integer vId, DirectedGraph<PatternNode, DefaultEdge> Q,
//			GraphDatabaseService G, HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet,
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet) {
//		// Vertex u = Q.getVertex(uId);
//		// Vertex v = G.getVertex(vId);
//
//		Set<PatternNode> uNextIds = u.getNextIds();
//		Set<Integer> vNextIds = LabelAdjacencyIndexer.getNextIds(vId);
//
//		Boolean bypass = true;
//		for (PatternNode uNext : uNextIds) {
//			Boolean tmp = false;
//			for (Integer vNext : vNextIds) {
//				if (isSimulated(uNext, vNext, simSet)) {
//					tmp = true;
//					break;
//				}
//			}
//			bypass = bypass && tmp;
//		}
//
//		if (bypass) {
//			return true;
//		} else {
//			Stack<Tuple2<PatternNode, Integer>> stack = new Stack<>();
//			stack.push(new Tuple2<>(uId, vId));
//			Set<Tuple2<PatternNode, Integer>> visitSet = new HashSet<>();
//			while (!stack.isEmpty()) {
//				Tuple2<PatternNode, Integer> tup = stack.pop();
//				simSet.put(tup, false);
//				if (!visitSet.contains(tup)) {
//					visitSet.add(tup);
//					Vertex uNode = Q.getVertex(tup._1);
//					// Vertex vNode = G.getVertex(tup._2);
//
//					for (PatternNode uPrev : uNode.getPrevIds()) {
//						for (Integer vPrev : LabelAdjacencyIndexer.getPrevIds(tup._2)) {
//							if (isSimulated(uPrev, vPrev, simSet)) {
//								stack.push(new Tuple2<>(uPrev, vPrev));
//							}
//						}
//					}
//				}
//			}
//
//			stack = new Stack<>();
//			stack.push(new Tuple2<>(uId, vId));
//			visitSet = new HashSet<>();
//
//			while (!stack.isEmpty()) {
//				Tuple2<PatternNode, Integer> tup = stack.pop();
//				simSet.put(tup, false);
//				// System.out.println("tup: " + tup);
//				if (!visitSet.contains(tup)) {
//					visitSet.add(tup);
//					Vertex uNode = Q.getVertex(tup._1);
//					// Vertex vNode = G.getVertex(tup._2);
//
//					for (PatternNode uNext : uNode.getNextIds()) {
//						for (Integer vNext : LabelAdjacencyIndexer.getNextIds(tup._2)) {
//							if (isSimulated(uNext, vNext, simSet)) {
//								stack.push(new Tuple2<>(uNext, vNext));
//							}
//						}
//					}
//				}
//			}
//		}
//		return true;
//	}
//
//	private static boolean updateNexts(PatternNode uId, Integer vId, DirectedGraph<PatternNode, DefaultEdge> Q,
//			GraphDatabaseService G, List<Tuple2<PatternNode, Integer>> deltaSimSet,
//			HashMap<Tuple2<PatternNode, Integer>, Tuple2<Boolean, Boolean>> canSet,
//			HashMap<Tuple2<PatternNode, Integer>, Boolean> simSet) {
//		Stack<Tuple2<PatternNode, Integer>> stack = new Stack<>();
//		stack.push(new Tuple2<>(uId, vId));
//		Set<Tuple2<PatternNode, Integer>> visitSet = new HashSet<>();
//		HashMap<Tuple2<PatternNode, Integer>, Tuple2<PatternNode, Integer>> chain = new HashMap<>();
//
//		while (!stack.isEmpty()) {
//			Tuple2<PatternNode, Integer> tup = stack.pop();
//			if (!visitSet.contains(tup)) {
//				visitSet.add(tup);
//				Vertex uNode = Q.getVertex(tup._1);
//				// Vertex vNode = G.getVertex(tup._2);
//
//				for (PatternNode uNext : uNode.getNextIds()) {
//					for (Integer vNext : LabelAdjacencyIndexer.getNextIds(tup._2.intValue())) {
//						if (isSimulated(uNext, vNext, simSet) || isCandidate(uNext, vNext, canSet)) {
//							stack.push(new Tuple2<>(uNext, vNext));
//							chain.put(new Tuple2<>(uNext, vNext), tup);
//						} else {
//							Tuple2<PatternNode, Integer> tail = tup;
//							while (chain.get(tail) != null) {
//								tail = chain.remove(tail);
//							}
//						}
//					}
//				}
//			}
//		}
//
//		deltaSimSet.addAll(chain.keySet());
//		deltaSimSet.addAll(chain.values());
//
//		return true;
//	}
//
//	// private static List<Vertex> loadVertexList(String StreamVFile) throws
//	// IOException {
//	// List<Vertex> vertexList = new ArrayList<>();
//	//
//	// Scanner scanner;
//	// String line;
//	// scanner = new Scanner(new File(StreamVFile));
//	// while (scanner.hasNext()) {
//	// line = scanner.nextLine();
//	// Vertex v = Graph.parseVertex(line);
//	// vertexList.add(v);
//	// }
//	//
//	// return vertexList;
//	// }
//
//	// private static List<Edge> loadEdgeList(String StreamEFile) throws
//	// IOException {
//	// List<Edge> edgeList = new ArrayList<>();
//	//
//	// Scanner scanner;
//	// String line;
//	// scanner = new Scanner(new File(StreamEFile));
//	// while (scanner.hasNextLine()) {
//	// line = scanner.nextLine();
//	// Edge e = Graph.parseEdge(line);
//	// edgeList.add(e);
//	// }
//	//
//	// return edgeList;
//	// }
//
//	private static boolean isSameSimulation(HashMap<PatternNode, HashSet<Integer>> incSim,
//			HashMap<PatternNode, HashSet<Integer>> batSim) {
//		boolean result = true;
//		if (!incSim.keySet().equals(batSim.keySet())) {
//			result = false;
//		} else {
//			for (PatternNode k : incSim.keySet()) {
//				if (incSim.get(k).size() != batSim.get(k).size()) {
//					result = false;
//					break;
//				} else if (!incSim.get(k).containsAll(batSim.get(k))) {
//					result = false;
//					break;
//				}
//			}
//		}
//
//		return result;
//	}
//
//	// Tests.
//	public static void main(String[] args) throws IOException {
//		test_yago_Q1();
//	}
//
//	private static void test_yago_Q1() throws IOException {
//		DirectedGraph<PatternNode, DefaultEdge> Q = new Graph(
//				"/Users/mnamaki/Documents/workspace/DualSim/yago/yago-Q2-V.txt",
//				"/Users/mnamaki/Documents/workspace/DualSim/yago/yago-Q2-E.txt");
//		System.out.println("Pattern Graph Loaded");
//
//		String StreamVFile = "/Users/mnamaki/Documents/workspace/DualSim/yago/yago-V.txt";
//		List<Vertex> vertexList = loadVertexList(StreamVFile);
//		System.out.println("Vertex List is Ready");
//
//		String StreamEFile = "/Users/mnamaki/Documents/workspace/DualSim/yago/yago-E.txt";
//		List<Edge> edgeList = loadEdgeList(StreamEFile);
//		System.out.println("Edge List is Ready");
//
//		GraphDatabaseService G = new Graph();
//		for (Vertex v : vertexList) {
//			G.addVertex(v);
//		}
//		System.out.println("Data Graph Initialized");
//
//		double incInitT = 0, batInitT = 0;
//		double t0, t1, t2, t3;
//
//		t2 = System.nanoTime();
//		HashMap<Long, HashSet<Long>> verIdx = utility.createVerticesMap(Q, G);
//		t3 = System.nanoTime();
//		batInitT = (t3 - t2) / 1000000000.0;
//
//		t0 = System.nanoTime();
//		verIdx = utility.createVerticesMap(Q, G);
//		HashMap<Long, HashSet<Long>> revIdx = utility.createReversedMap(verIdx);
//
//		HashMap<Tuple2<Long>, Boolean> simSet = new HashMap<>();
//		for (Long uId : verIdx.keySet()) {
//			for (Long vId : verIdx.get(uId)) {
//				simSet.put(new Tuple2<>(uId, vId), false);
//			}
//		}
//
//		HashMap<Tuple2<Long>, Tuple2<Boolean>> canSet = new HashMap<>();
//		for (Long uId : verIdx.keySet()) {
//			for (Long vId : verIdx.get(uId)) {
//				canSet.put(new Tuple2<>(uId, vId), new Tuple2<>(false, false));
//			}
//		}
//		t1 = System.nanoTime();
//		incInitT = (t1 - t0) / 1000000000.0;
//
//		List<Edge> canEdges = new ArrayList<>();
//		for (int i = 0; i < edgeList.size(); i++) {
//			Edge e = edgeList.get(i);
//			if (revIdx.get(e.getSrcId()) != null && revIdx.get(e.getDstId()) != null) {
//				canEdges.add(e);
//			}
//		}
//
//		// Start Simulation.
//		// List<Edge> input = canEdges;
//		List<Edge> input = edgeList;
//
//		int N = input.size();
//		int M = 40;
//		double tInc[] = new double[M];
//		double tBat[] = new double[M];
//		Long nEdges[] = new Long[M];
//		int batch = N / M;
//
//		HashMap<Long, HashSet<Long>>[] batSimInc = new HashMap[M];
//		HashMap<Long, HashSet<Long>>[] batSimDec = new HashMap[M];
//		HashMap<Long, HashSet<Long>>[] incSimInc = new HashMap[M];
//		HashMap<Long, HashSet<Long>>[] incSimDec = new HashMap[M];
//
//		System.out.println("Initial Index Built");
//		for (int i = 1; i <= M; i++) {
//			List<Edge> batEdges = new ArrayList<>();
//
//			for (int j = batch * (i - 1); j < batch * i && j < input.size(); j++) {
//				batEdges.add(input.get(j));
//			}
//
//			t0 = System.nanoTime(); // Need to remove the time of add edges.
//			incSimInc[i - 1] = run(G, Q, batEdges, verIdx, revIdx, simSet, canSet);
//			t1 = System.nanoTime();
//			tInc[i - 1] = (t1 - t0) / 1000000000.0;
//
//			t2 = System.nanoTime();
//
//			HashMap<Long, HashSet<Long>> dsim = new HashMap<>(verIdx);
//			for (Long k : verIdx.keySet()) {
//				dsim.put(k, new HashSet<>(verIdx.get(k)));
//			}
//			batSimInc[i - 1] = BatDualSimulation.run(G, Q, dsim);
//
//			t3 = System.nanoTime();
//
//			nEdges[i - 1] = G.getNumOfEdges();
//			tBat[i - 1] = (t3 - t2) / 1000000000.0;
//
//			System.out.println(i);
//			System.out.println("incSim: " + incSimInc[i - 1]);
//			System.out.println("batSim: " + batSimInc[i - 1]);
//
//			boolean compare = isSameSimulation(incSimInc[i - 1], batSimInc[i - 1]);
//			System.out.println("Same? " + compare);
//			System.out.println();
//			if (!compare) {
//				System.out.println("Fatal: incSim and batSim are not same!");
//				return;
//			}
//		}
//		System.out.println("Num of Edges:");
//		for (Long n : nEdges)
//			System.out.println(n);
//		System.out.println();
//
//		System.out.println("Inc Init Time: " + incInitT);
//		System.out.println("IncTime:");
//		for (double t : tInc)
//			System.out.println(t);
//		System.out.println();
//
//		System.out.println("Bat Init Time: " + batInitT);
//		System.out.println("BatTime:");
//		for (double t : tBat)
//			System.out.println(t);
//		System.out.println();
//
//		System.out.println("Testing Edge Deletion");
//
//		HashMap<Long, HashSet<Long>> dsim0 = new HashMap<>(verIdx);
//		for (Long k : verIdx.keySet()) {
//			dsim0.put(k, new ArrayList<>(verIdx.get(k)));
//		}
//
//		incSimDec[M - 1] = getSimResults(simSet);
//		System.out.println("incSim: " + incSimDec[M - 1]);
//		batSimDec[M - 1] = BatDualSimulation.run(G, Q, dsim0);
//		System.out.println("batSim: " + batSimDec[M - 1]);
//		boolean compare0 = isSameSimulation(incSimDec[M - 1], batSimDec[M - 1]);
//		System.out.println("Same? " + compare0);
//		System.out.println();
//
//		for (int i = M; i >= 2; i--) {
//			List<Edge> batEdges = new ArrayList<>();
//
//			for (int j = batch * (i - 1); j < batch * i && j < input.size(); j++) {
//				batEdges.add(input.get(j));
//			}
//
//			tInc[i - 2] = 0;
//			for (int j = 0; j < batEdges.size(); j++) {
//				Edge e = batEdges.get(j);
//				G.removeEdge(e);
//				t0 = System.nanoTime();
//				incSimDec[i - 2] = runforDel(G, Q, e, verIdx, revIdx, simSet, canSet);
//				t1 = System.nanoTime();
//				tInc[i - 2] += (t1 - t0);
//			}
//			incSimDec[i - 2] = getSimResults(simSet);
//			tInc[i - 2] = tInc[i - 2] / 1000000000.0;
//
//			System.out.println(i);
//
//			HashMap<Long, HashSet<Long>> dsim = new HashMap<>(verIdx);
//			for (Long k : verIdx.keySet()) {
//				dsim.put(k, new ArrayList<>(verIdx.get(k)));
//			}
//
//			t0 = System.nanoTime();
//			batSimDec[i - 2] = BatDualSimulation.run(G, Q, dsim);
//			t1 = System.nanoTime();
//
//			tBat[i - 2] = (t1 - t0) / 1000000000.0;
//
//			System.out.println("incSim: " + incSimDec[i - 2]);
//			System.out.println("batSim: " + batSimDec[i - 2]);
//
//			if (isSameSimulation(batSimInc[i - 2], batSimDec[i - 2]))
//				System.out.println("Batch works");
//			boolean compare = isSameSimulation(incSimDec[i - 2], batSimDec[i - 2]);
//
//			System.out.println("Same? " + compare);
//
//			if (!compare) {
//				System.out.println("Fatal: incSim and batSim are not same!");
//				return;
//			}
//
//			System.out.println();
//			nEdges[i - 2] = G.getNumOfEdges();
//			System.out.println(G.getNumOfEdges());
//		}
//
//		System.out.println("Inc Time");
//		for (int i = M; i >= 2; i--) {
//			System.out.println(tInc[i - 2]);
//		}
//		System.out.println();
//
//		System.out.println("Bat Time");
//		for (int i = M; i >= 2; i--) {
//			System.out.println(tBat[i - 2]);
//		}
//		System.out.println();
//
//		System.out.println("Num of Edges");
//		for (int i = M; i >= 2; i--) {
//			System.out.println(nEdges[i - 2]);
//		}
//	}
//
//	private static void unit_test() throws IOException {
//
//		// Graph Q = new Graph("data/test/Q2-V.txt", "data/test/Q2-E.txt");
//		// GraphDatabaseService G = new Graph();
//		// String StreamEFile = "data/test/G2-E.txt";
//		// G.loadVertices("data/test/G2-V.txt");
//		//
//		// List<Edge> edgeList = loadEdgeList(StreamEFile);
//		// run(G, Q, edgeList);
//		//
//		// G.printEdges();
//		//
//		// for (int i = 0; i < edgeList.size(); i++) {
//		// Edge e = edgeList.get(i);
//		// G.removeEdge(e);
//		//
//		// G.printEdges();
//		//
//		// HashMap<Long, HashSet<Long>> batSim = BatDualSimulation.run(G, Q);
//		// }
//	}
//
//	private static void unit_test2() throws IOException {
//
//		// Graph Q = new Graph("data/test/Q5-V.txt", "data/test/Q5-E.txt");
//		// GraphDatabaseService G = new Graph();
//		// String StreamEFile = "data/test/G5-E.txt";
//		// G.loadVertices("data/test/G5-V.txt");
//		//
//		// List<Edge> edgeList = loadEdgeList(StreamEFile);
//		//
//		// // Start
//		//
//		// Q.printGraph();
//		// G.printGraph();
//		//
//		// HashMap<Long, HashSet<Long>> verIdx = utility.createVerticesMap(Q,
//		// G);
//		// HashMap<Long, HashSet<Long>> revIdx =
//		// utility.createReversedMap(verIdx);
//		//
//		// HashMap<Tuple2<Long>, Boolean> simSet = new HashMap<>();
//		// for (Long uId : verIdx.keySet()) {
//		// for (Long vId : verIdx.get(uId)) {
//		// simSet.put(new Tuple2<>(uId, vId), false);
//		// }
//		// }
//		//
//		// HashMap<Tuple2<Long>, Tuple2<Boolean>> canSet = new HashMap<>();
//		// for (Long uId : verIdx.keySet()) {
//		// for (Long vId : verIdx.get(uId)) {
//		// canSet.put(new Tuple2<>(uId, vId), new Tuple2<>(false, false));
//		// }
//		// }
//		//
//		// for (int i = 0; i < edgeList.size(); i++) {
//		// Edge e = edgeList.get(i);
//		// G.addEdge(e);
//		// updateCanSet(canSet, e, G, Q, revIdx);
//		// updateSimSet(simSet, e, G, Q, revIdx, canSet);
//		// }
//		//
//		// Q.printGraph();
//		// G.printGraph();
//		//
//		// HashMap<Long, HashSet<Long>> incSim = getSimResults(simSet);
//		// HashMap<Long, HashSet<Long>> batSim = BatDualSimulation.run(G, Q);
//		// System.out.println(incSim);
//		// System.out.println();
//		//
//		// for (Edge e : edgeList) {
//		//
//		// System.out.println(e.getSrcId() + "->" + e.getDstId());
//		//
//		// G.removeEdge(e);
//		//
//		// incSim = runforDel(G, Q, e, verIdx, revIdx, simSet, canSet);
//		// System.out.println(incSim);
//		// System.out.println();
//		// }
//	}
}
