/*
This is a version of Peng's src.dualSimulation code which supports Neo4j graphs.
Modified by: Shayan Monadjemi
June 7th, 2016
 */

package src.dualSimulation.gtar;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.alg1.prefixTree.PrefixTreeAlg1;
import src.base.IPrefixTree;
import src.utilities.DefaultLabeledEdge;
import src.utilities.PatternNode;

import java.io.File;
import java.util.*;

/**
 * Batch Dual Simulation.
 */
public class BatDualSimulation {

	public static HashMap<Integer, HashSet<Integer>> run(GraphDatabaseService G, GraphDatabaseService Q) {

		Map<Integer, HashSet<Integer>> dsim = utility.createVerticesMap(Q, G);

		boolean noStop = true;
		try (Transaction tx1 = G.beginTx()) {
			try (Transaction tx2 = Q.beginTx()) {
				while (noStop) {
					noStop = false;
					for (Integer uId : dsim.keySet()) {
						for (Iterator<Integer> it = dsim.get(uId).iterator(); it.hasNext();) {
							Integer vId = it.next();
							Node u = Q.getNodeById(uId);
							Node v = G.getNodeById(vId);

							if ((!utility.isNextSimulated(dsim, v, u)) || (!utility.isPrevSimulated(dsim, v, u))) {
								it.remove();
								noStop = true;
							}
						}
					}
				}
				tx2.success();
			} catch (Exception e) {

			}
			tx1.success();
		} catch (Exception e) {

		}

		HashMap<Integer, HashSet<Integer>> simRes = new HashMap<>();
		for (Integer uId : dsim.keySet()) {
			if ((dsim.get(uId) != null) && (!dsim.get((uId)).isEmpty())) {
				simRes.put(uId, dsim.get(uId));
			}
		}

		return simRes;
	}

	public static HashMap<PatternNode, HashSet<PatternNode>> customizedMatchList(
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph1,
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph2,
			HashMap<PatternNode, HashSet<PatternNode>> dsim, IPrefixTree prefixTree) {

		boolean noStop = true;
		// try (Transaction tx1 = dataGraph.beginTx()) {
		while (noStop) {
			noStop = false;
			for (PatternNode uId : dsim.keySet()) {
				for (Iterator<PatternNode> it = dsim.get(uId).iterator(); it.hasNext();) {
					PatternNode vId = it.next();
					//PatternNode u = null;
//					for (PatternNode n : patternGraph2.vertexSet()) {
//						if (n.equals(uId)) {
//							u = n;
//							break;
//						}
//					}
					// Node v = dataGraph.getNodeById(vId);

					if ((!utility.isNextSimulated(patternGraph1, dsim, vId, uId, patternGraph2))
							|| (!utility.isPrevSimulated(patternGraph1, dsim, vId, uId, patternGraph2))) {
						it.remove();
						noStop = true;
					}
				}
			}
		}

		HashMap<PatternNode, HashSet<PatternNode>> simRes = new HashMap<>();
		for (PatternNode uId : dsim.keySet()) {
			//simRes.putIfAbsent(uId, new HashSet<PatternNode>());
			if ((dsim.get(uId) != null) && (!dsim.get((uId)).isEmpty())) {
				simRes.put(uId, dsim.get(uId));
			}
		}

		return simRes;
	}

	public static HashMap<PatternNode, HashSet<Integer>> customizedMatchList(GraphDatabaseService dataGraph,
			DirectedGraph<PatternNode, DefaultLabeledEdge> queryGraph, HashMap<PatternNode, HashSet<Integer>> dsim,
			IPrefixTree prefixTree) {

		boolean noStop = true;
		// try (Transaction tx1 = dataGraph.beginTx()) {
		while (noStop) {
			noStop = false;
			for (PatternNode uId : dsim.keySet()) {
				for (Iterator<Integer> it = dsim.get(uId).iterator(); it.hasNext();) {
					Integer vId = it.next();
					PatternNode u = null;
					for (PatternNode n : queryGraph.vertexSet()) {
						if (n.equals(uId)) {
							u = n;
							break;
						}
					}
					// Node v = dataGraph.getNodeById(vId);

					if ((!utility.isNextSimulated(prefixTree.getLabelAdjacencyIndexer(), dsim, vId, u, queryGraph))
							|| (!utility.isPrevSimulated(prefixTree.getLabelAdjacencyIndexer(), dsim, vId, u,
									queryGraph))) {
						it.remove();
						noStop = true;
					}
				}
			}
		}
		//
		// tx1.success();
		// } catch (Exception e) {
		//
		// }

		HashMap<PatternNode, HashSet<Integer>> simRes = new HashMap<>();
		for (PatternNode uId : dsim.keySet()) {
			simRes.putIfAbsent(uId, new HashSet<Integer>());
			if ((dsim.get(uId) != null) && (!dsim.get((uId)).isEmpty())) {
				simRes.put(uId, dsim.get(uId));
			}
		}

		return simRes;

	}

	public static HashMap<PatternNode, HashSet<Integer>> run(GraphDatabaseService G,
			DirectedGraph<PatternNode, DefaultLabeledEdge> Q, IPrefixTree prefixTree) {

		HashMap<PatternNode, HashSet<Integer>> dsim = utility.createVerticesMap(Q, G);

		return customizedMatchList(G, Q, dsim, prefixTree);
	}

	public static Map<Integer, HashSet<Integer>> run(GraphDatabaseService G, GraphDatabaseService Q,
			Map<Integer, HashSet<Integer>> dsim) {

		boolean noStop = true;
		try (Transaction tx1 = G.beginTx()) {
			try (Transaction tx2 = Q.beginTx()) {
				while (noStop) {
					noStop = false;
					for (Integer uId : dsim.keySet()) {
						for (Iterator<Integer> it = dsim.get(uId).iterator(); it.hasNext();) {
							Integer vId = it.next();
							Node u = Q.getNodeById(uId);
							Node v = G.getNodeById(vId);

							if ((!utility.isNextSimulated(dsim, v, u)) || (!utility.isPrevSimulated(dsim, v, u))) {
								it.remove();
								noStop = true;
							}
						}
					}
				}
				tx2.success();
			} catch (Exception e) {

			}
			tx1.success();
		} catch (Exception e) {

		}

		Map<Integer, HashSet<Integer>> simRes = new HashMap<>();
		for (Integer uId : dsim.keySet()) {
			if ((dsim.get(uId) != null) && (!dsim.get((uId)).isEmpty())) {
				simRes.put(uId, dsim.get(uId));
			}
		}

		return simRes;
	}

	// Tests.
	public static void main(String[] args) {
		// test_yago_Q1_bat();
		// test_yago_Q2_bat();
		// test_yago_Q1();
		// test_yago_Q2();
		unit_test();
	}

	private static void unit_test() {
		Map<Integer, HashSet<Integer>> dsim;

		System.out.println("Test Batch Dual Simulation.");
		// Graph G = new Graph("data/test/G2-V.txt", "data/test/G2-E.txt");
		// Graph Q = new Graph("data/test/Q2-V.txt", "data/test/Q2-E.txt");
		// Graph G = new Graph("data/yago/yago-V.txt", "data/yago/yago-E.txt");
		// Graph Q = new Graph("data/yago/yago-Q2-V.txt",
		// "data/yago/yago-Q2-E.txt");
		String gPath = "/home/shayan/Documents/WSU/Data/Synthetic/SyntheticG.graphdb";
		String qPath = "/home/shayan/Documents/WSU/Data/Synthetic/SyntheticQ.graphdb";
		GraphDatabaseService G = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(gPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
		GraphDatabaseService Q = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(qPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		// G.printGraph();
		// Q.printGraph();

		double startTime = System.nanoTime();
		dsim = run(G, Q);
		double endTime = System.nanoTime();

		System.out.println((endTime - startTime) / 1000000000.0);
		System.out.println(dsim);
	}

	/*
	 * private static void test_yago_Q1_bat() { Map<Integer, List<Integer>>
	 * dsim, rIdx; int M = 2373567; int N = 50; int fold; double tStamp[] = new
	 * double[N]; Integer nEdges[] = new Integer[N];
	 * 
	 * System.out.println("Testing Batch Dual Simulation -- Yago Q1....");
	 * 
	 * Graph Q = new Graph("data/yago/yago-Q1-V.txt",
	 * "data/yago/yago-Q1-E.txt"); System.out.println("query data loaded");
	 * 
	 * Graph G = new Graph(); G.loadVertices("data/yago/yago-V.txt");
	 * System.out.println("graph data vertices Loaded");
	 * 
	 * // dsim = utility.createVerticesMap(Q, G); rIdx =
	 * utility.createReversedMap(dsim);
	 * 
	 * System.out.println("Candidate Sets are Initialized");
	 * 
	 * String StreamEFile = "data/yago/yago-E.txt";
	 * 
	 * try { Scanner scanner = new Scanner(new File(StreamEFile));
	 * 
	 * for (fold = 1; fold <= N; fold++) { int cnt = 0; while
	 * (scanner.hasNextLine()) { // Edge e = G.parseEdge(scanner.nextLine()); //
	 * G.addEdge(e); cnt++; if (cnt > M / N) { break; } }
	 * 
	 * double t0 = System.nanoTime(); // run(G, Q); double t1 =
	 * System.nanoTime(); nEdges[fold-1] = G.getNumOfEdges(); tStamp[fold-1] =
	 * (t1 - t0) / 1000000000.0; } scanner.close(); } catch
	 * (FileNotFoundException except) { except.printStackTrace(); }
	 * 
	 * for (Integer n : nEdges) { System.out.println(n); } for (double t :
	 * tStamp) { System.out.println(t); } }
	 * 
	 * private static void test_yago_Q2_bat() { Map<Integer, List<Integer>>
	 * dsim, rIdx; int M = 2373567; int N = 50; int fold; double tStamp[] = new
	 * double[N]; Integer nEdges[] = new Integer[N];
	 * 
	 * System.out.println("Testing Batch Dual Simulation -- Yago Q2....");
	 * 
	 * Graph Q = new Graph("data/yago/yago-Q2-V.txt",
	 * "data/yago/yago-Q2-E.txt"); System.out.println("query data loaded");
	 * 
	 * Graph G = new Graph(); G.loadVertices("data/yago/yago-V.txt");
	 * System.out.println("graph data vertices Loaded");
	 * 
	 * // dsim = utility.createVerticesMap(Q, G); rIdx =
	 * utility.createReversedMap(dsim);
	 * 
	 * System.out.println("Candidate Sets are Initialized");
	 * 
	 * String StreamEFile = "data/yago/yago-E.txt";
	 * 
	 * try { Scanner scanner = new Scanner(new File(StreamEFile));
	 * 
	 * for (fold = 1; fold <= N; fold++) { int cnt = 0; while
	 * (scanner.hasNextLine()) { Edge e = G.parseEdge(scanner.nextLine());
	 * G.addEdge(e); cnt++; if (cnt > M / N) { break; } }
	 * 
	 * double t0 = System.nanoTime(); // run(G, Q); double t1 =
	 * System.nanoTime(); nEdges[fold-1] = G.getNumOfEdges(); tStamp[fold-1] =
	 * (t1 - t0) / 1000000000.0; } scanner.close(); } catch
	 * (FileNotFoundException except) { except.printStackTrace(); }
	 * 
	 * for (Integer n : nEdges) { System.out.println(n); } for (double t :
	 * tStamp) { System.out.println(t); } }
	 * 
	 * private static void test_yago_Q1() { Map<Integer, List<Integer>> dsim,
	 * rIdx; int M = 1007; int N = 50; int fold; double tStamp[] = new
	 * double[N]; Integer nEdges[] = new Integer[N];
	 * 
	 * System.out.println("Testing Batch Dual Simulation -- Yago Q1....");
	 * 
	 * Graph Q = new Graph("data/yago/yago-Q1-V.txt",
	 * "data/yago/yago-Q1-E.txt"); System.out.println("query data loaded");
	 * 
	 * Graph G = new Graph(); G.loadVertices("data/yago/yago-V.txt");
	 * System.out.println("graph data vertices Loaded");
	 * 
	 * // dsim = utility.createVerticesMap(Q, G); rIdx =
	 * utility.createReversedMap(dsim);
	 * 
	 * System.out.println("Candidate Sets are Initialized");
	 * 
	 * String StreamEFile = "data/yago/yago-E.txt";
	 * 
	 * try { Scanner scanner = new Scanner(new File(StreamEFile));
	 * 
	 * for (fold = 1; fold <= N; fold++) { int cnt = 0; while
	 * (scanner.hasNextLine()) { Edge e = G.parseEdge(scanner.nextLine());
	 * G.addEdge(e); if (rIdx.get(e.getSrcId()) != null &&
	 * rIdx.get(e.getDstId()) != null) { cnt++; } if (cnt > M / N) { break; } }
	 * 
	 * double t0 = System.nanoTime(); // run(G, Q); double t1 =
	 * System.nanoTime(); nEdges[fold-1] = G.getNumOfEdges(); tStamp[fold-1] =
	 * (t1 - t0) / 1000000000.0; } scanner.close(); } catch
	 * (FileNotFoundException except) { except.printStackTrace(); }
	 * 
	 * for (Integer n : nEdges) { System.out.println(n); } for (double t :
	 * tStamp) { System.out.println(t); } }
	 * 
	 * private static void test_yago_Q2() { Map<Integer, List<Integer>> dsim,
	 * rIdx; int M = 3845; int N = 50; int fold; double tStamp[] = new
	 * double[N]; Integer nEdges[] = new Integer[N];
	 * 
	 * System.out.println("Testing Batch Dual Simulation -- Yago Q2....");
	 * 
	 * Graph Q = new Graph("data/yago/yago-Q2-V.txt",
	 * "data/yago/yago-Q2-E.txt"); System.out.println("query data loaded");
	 * 
	 * Graph G = new Graph(); G.loadVertices("data/yago/yago-V.txt");
	 * System.out.println("graph data vertices Loaded");
	 * 
	 * // dsim = utility.createVerticesMap(Q, G); rIdx =
	 * utility.createReversedMap(dsim);
	 * 
	 * System.out.println("Candidate Sets are Initialized");
	 * 
	 * String StreamEFile = "data/yago/yago-E.txt";
	 * 
	 * try { Scanner scanner = new Scanner(new File(StreamEFile));
	 * 
	 * for (fold = 1; fold <= N; fold++) { int cnt = 0; while
	 * (scanner.hasNextLine()) { Edge e = G.parseEdge(scanner.nextLine());
	 * G.addEdge(e); if (rIdx.get(e.getSrcId()) != null &&
	 * rIdx.get(e.getDstId()) != null) { cnt++; } if (cnt > M / N) { break; } }
	 * 
	 * double t0 = System.nanoTime(); // run(G, Q); double t1 =
	 * System.nanoTime(); nEdges[fold-1] = G.getNumOfEdges(); tStamp[fold-1] =
	 * (t1 - t0) / 1000000000.0; } scanner.close(); } catch
	 * (FileNotFoundException except) { except.printStackTrace(); }
	 * 
	 * for (Integer n : nEdges) { System.out.println(n); } for (double t :
	 * tStamp) { System.out.println(t); } }
	 */
}
