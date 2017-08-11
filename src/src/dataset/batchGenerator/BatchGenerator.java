package src.dataset.batchGenerator;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;
import src.dataset.dataSnapshot.SnapshotCreator;
import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * This program accepts a Dynamic OR Static Neo4j data base and creates batch
 * graphs.
 * 
 * @argument -completeDataGraphPath [Abs. Path] : The absolute path to a static
 *           data graph, or a dynamic data graph (i.e. a folder with g0.graphdb
 *           and Delta(s).
 * @argument -isDynamic [true/false] : true if the completeDataGraph is dynamic,
 *           false otherwise
 * @argument -storeDir [Abs. Path] : an empty directory where everything will be
 *           saved.
 * @argument -startingRelationshipCount [int] : the number of rels in g0 (if our
 *           complete graph is static)
 * @argument -intervalRelationshipCount [int] : the number of relationship
 *           transactions between each batch
 * @argument -maxNumberOfBatchGraphs [int] : how many batch graphs to create
 * @argument -addTimeStampKey [String] : used to create dynamic graphs, if
 *           complete graph is static
 * @argument -deleteTimeStampKet [String] : used to create dynamic graphs, if
 *           complete graph is static
 * @argument -timeFormat [String] : : used to create dynamic graphs, if complete
 *           graph is static (example: dd-MMM-yyyy)
 * @argument -insert : insert-only mode
 * @argument -delete : delete-only mode
 * @argument -mix : mix insert/delete mode
 *
 *           NOTE: Multiple modes can be activated at once. NOTE: This program
 *           will generate a README.txt file in each database folder, containing
 *           information about that batch.
 *
 *           Created by shayan on 8/5/16.
 */
public class BatchGenerator {

	static int startingRelationshipCount;
	static int intervalRelationshipCount;
	static int maxNumberOfBatchGraphs;
	static int deltaSize = 500000;

	static String completeDataGraphPath;
	static String storeDir;
	static String timeFormat;
	static String addTimeStampKey;
	static String deleteTimeStampKey;
	static String snapshotDirName = "snapshot";
	static String g0DirName = "g0.graphdb";

	static boolean isDynamic = false;
	// static boolean insert;
	// static boolean delete;
	// static boolean mix;

	static Transaction tx;
	static GraphDatabaseService dataGraph;
	static int currentBatch;
	static int numDeleted;
	static int numInserted;
	static String dir;
	static String prevdir;

	public static enum StreamMode {
		INSERT, DELETE, MIX
	}

	static StreamMode streamMode = StreamMode.MIX;

	public static void main(String[] args) throws Exception {

		/**
		 * Check for arguments
		 */

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-completeDataGraphPath")) {
				completeDataGraphPath = args[++i];
				if (!completeDataGraphPath.endsWith("/")) {
					completeDataGraphPath += "/";
				}
			} else if (args[i].equals("-startingRelationshipCount")) {
				startingRelationshipCount = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-intervalRelationshipCount")) {
				intervalRelationshipCount = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-storeDir")) {
				storeDir = args[++i];
				if (!storeDir.endsWith("/")) {
					storeDir += "/";
				}
			} else if (args[i].equals("-addTimeStampKey")) {
				addTimeStampKey = args[++i];
			} else if (args[i].equals("-deleteTimeStampKey")) {
				deleteTimeStampKey = args[++i];
			} else if (args[i].equals("-timeFormat")) {
				timeFormat = args[++i];
			} else if (args[i].equals("-maxNumberOfBatchGraphs")) {
				maxNumberOfBatchGraphs = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-isDynamic")) {
				isDynamic = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-streamMode")) {

				switch (args[++i].toLowerCase()) {
				case "insert":
					streamMode = StreamMode.INSERT;
					break;
				case "delete":
					streamMode = StreamMode.DELETE;
					break;
				case "mix":
					streamMode = StreamMode.MIX;
					break;
				}
			}
		}

		int relCount = 0;
		currentBatch = 0;

		if (!isDynamic) {
			System.out.println("startingRelationshipCount:" +startingRelationshipCount);
			System.out.println("intervalRelationshipCount:" +intervalRelationshipCount);
			SnapshotCreator snapshotCreator = new SnapshotCreator(completeDataGraphPath, startingRelationshipCount,
					storeDir + snapshotDirName, deltaSize, addTimeStampKey, deleteTimeStampKey, timeFormat);
			snapshotCreator.run();
		} else {

			try {
				FileUtils.copyRecursively(new File(completeDataGraphPath), new File(storeDir + snapshotDirName));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		final String ORIGINAL_GRAPH = storeDir + currentBatch + ".graphdb";
		dir = ORIGINAL_GRAPH;

		try {
			FileUtils.copyRecursively(new File(storeDir + snapshotDirName + "/" + g0DirName), new File(dir));

			File dataGraphFile = new File(dir);

			dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dataGraphFile)
					.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

			makeReadme();
		} catch (Exception e) {
			e.printStackTrace();
		}

		SnapshotSimulator snapshotSimulator;

		/**
		 * INSERT ONLY
		 */

		if (streamMode == StreamMode.INSERT) {
			snapshotSimulator = new SnapshotSimulator(storeDir + snapshotDirName, timeFormat);
			int numOfGraphs = 1;
			while (numOfGraphs < maxNumberOfBatchGraphs) {
				int prevBatch = currentBatch;
				currentBatch += intervalRelationshipCount;

				prevdir = dir;
				dir = storeDir + currentBatch + "_INSERT.graphdb";

				try {
					FileUtils.copyRecursively(new File(prevdir), new File(dir));
				} catch (Exception e) {
					e.printStackTrace();
				}

				File dataGraphFile = new File(dir);

				dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dataGraphFile)
						.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
				tx = dataGraph.beginTx();

				StreamEdge streamEdge = snapshotSimulator.getNextStreamEdge();

				relCount = 0;

				while (relCount < intervalRelationshipCount) {

					if (streamEdge == null) {
						System.out.println("no more stream edges");
						break;

					}

					Node srcNode = null;
					Node destNode = null;
					String srcLabel = null;
					String destLabel = null;
					Integer srcNodeId = null;
					Integer destNodeId = null;

					srcNodeId = (int) streamEdge.getSourceNode();
					destNodeId = (int) streamEdge.getDestinationNode();
					srcNode = dataGraph.getNodeById(srcNodeId);
					destNode = dataGraph.getNodeById(destNodeId);
					srcLabel = srcNode.getLabels().iterator().next().name();
					destLabel = destNode.getLabels().iterator().next().name();

					if (streamEdge.isAdded()) {
						srcNode.createRelationshipTo(destNode,
								RelationshipType.withName(streamEdge.getRelationshipType()));
						relCount++;
						numInserted++;
					} else {
						for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
								RelationshipType.withName(streamEdge.getRelationshipType()))) {
							if (rel.getEndNode().getId() == destNodeId) {
								rel.delete();
							}
						}
						relCount--;
						numDeleted++;
					}

					// if(relCount%1000 == 0)
					// {
					// System.out.print(currentBatch +": " + relCount + " ; " +
					// streamEdge);
					// }

					streamEdge = snapshotSimulator.getNextStreamEdge();

				}

				tx.success();
				tx.close();
				//

				if (streamEdge == null) {
					break;
				}

				numOfGraphs++;

				makeReadme();

			}
		}

		/**
		 * Delete Only
		 */

		if (streamMode == StreamMode.DELETE) {
			snapshotSimulator = new SnapshotSimulator(storeDir + snapshotDirName, timeFormat);
			dir = ORIGINAL_GRAPH;
			currentBatch = 0;

			int numOfGraphs = 0;
			while (numOfGraphs < maxNumberOfBatchGraphs) {
				int prevBatch = currentBatch;
				currentBatch += intervalRelationshipCount;

				prevdir = dir;
				dir = storeDir + currentBatch + "_DELETE.graphdb";

				try {
					FileUtils.copyRecursively(new File(prevdir), new File(dir));
				} catch (Exception e) {
					e.printStackTrace();
				}

				File dataGraphFile = new File(dir);

				dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dataGraphFile)
						.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
				tx = dataGraph.beginTx();

				StreamEdge streamEdge = snapshotSimulator.getNextStreamEdge();

				relCount = 0;

				while (relCount < intervalRelationshipCount) {

					if (streamEdge == null) {
						System.out.println("no more stream edges");
						break;

					}

					Node srcNode = null;
					Node destNode = null;
					String srcLabel = null;
					String destLabel = null;
					Integer srcNodeId = null;
					Integer destNodeId = null;

					srcNodeId = (int) streamEdge.getSourceNode();
					destNodeId = (int) streamEdge.getDestinationNode();
					srcNode = dataGraph.getNodeById(srcNodeId);
					destNode = dataGraph.getNodeById(destNodeId);
					srcLabel = srcNode.getLabels().iterator().next().name();
					destLabel = destNode.getLabels().iterator().next().name();

					if (streamEdge.isAdded()) {
						srcNode.createRelationshipTo(destNode,
								RelationshipType.withName(streamEdge.getRelationshipType()));
						relCount--;
						numInserted++;
					} else {
						for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
								RelationshipType.withName(streamEdge.getRelationshipType()))) {
							if (rel.getEndNode().getId() == destNodeId) {
								rel.delete();
							}
						}
						relCount++;
						numDeleted++;
					}

					// if(relCount%1000 == 0)
					// {
					// System.out.print(currentBatch +": " + relCount + " ; " +
					// streamEdge);
					// }

					streamEdge = snapshotSimulator.getNextStreamEdge();

				}

				tx.success();
				tx.close();
				// dataGraph.shutdown();

				if (streamEdge == null) {
					break;
				}

				numOfGraphs++;

				makeReadme();

			}

		}
		/**
		 * Insert/Delete mixed
		 */

		if (streamMode == StreamMode.MIX) {
			snapshotSimulator = new SnapshotSimulator(storeDir + snapshotDirName, timeFormat);
			int numOfGraphs = 0;

			dir = ORIGINAL_GRAPH;
			currentBatch = 0;

			System.out.println("intervalRelationshipCount:" +intervalRelationshipCount);
			
			while (numOfGraphs < maxNumberOfBatchGraphs) {
				int prevBatch = currentBatch;
				currentBatch += intervalRelationshipCount;

				prevdir = dir;
				dir = storeDir + currentBatch + "_MIX.graphdb";

				try {
					FileUtils.copyRecursively(new File(prevdir), new File(dir));
				} catch (Exception e) {
					e.printStackTrace();
				}

				File dataGraphFile = new File(dir);

				dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dataGraphFile)
						.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
				tx = dataGraph.beginTx();

				StreamEdge streamEdge = snapshotSimulator.getNextStreamEdge();

				relCount = 0;
				numInserted = 0;
				numDeleted = 0;

				while (relCount < intervalRelationshipCount) {

					if (streamEdge == null) {
						System.out.println("no more stream edges");
						break;

					}

					Node srcNode = null;
					Node destNode = null;
					String srcLabel = null;
					String destLabel = null;
					Integer srcNodeId = null;
					Integer destNodeId = null;

					srcNodeId = (int) streamEdge.getSourceNode();
					destNodeId = (int) streamEdge.getDestinationNode();
					srcNode = dataGraph.getNodeById(srcNodeId);
					destNode = dataGraph.getNodeById(destNodeId);
					srcLabel = srcNode.getLabels().iterator().next().name();
					destLabel = destNode.getLabels().iterator().next().name();

					if (streamEdge.isAdded()) {
						srcNode.createRelationshipTo(destNode,
								RelationshipType.withName(streamEdge.getRelationshipType()));
						relCount++;
						numInserted++;
					} else {
						for (Relationship rel : srcNode.getRelationships(Direction.OUTGOING,
								RelationshipType.withName(streamEdge.getRelationshipType()))) {
							if (rel.getEndNode().getId() == destNodeId) {
								rel.delete();
							}
						}
						relCount++;
						numDeleted++;
					}

					streamEdge = snapshotSimulator.getNextStreamEdge();

				}

				tx.success();
				tx.close();

				if (streamEdge == null) {
					break;
				}

				numOfGraphs++;

				makeReadme();

			}

		}

	}

	private static void makeReadme() throws Exception {
		File readme = new File(dir + "/README.txt");
		FileWriter readmeWriter = new FileWriter(readme);

		tx = dataGraph.beginTx();

		// count nodes
		int allNodesCnt = 0;
		int allDegrees = 0;
		for (Node node : dataGraph.getAllNodes()) {
			allNodesCnt++;
			allDegrees += node.getDegree(Direction.OUTGOING);
		}
		readmeWriter.write("all out Degrees = " + allDegrees + "\n");
		readmeWriter.write("Number of inserted rels = " + numInserted + "\n");
		readmeWriter.write("Number of deleted rels = " + numDeleted + "\n");

		readmeWriter.write("Number of total processed EdgeStreams = " + (numDeleted + numInserted) + "\n");

		readmeWriter.write("Number of nodes = " + allNodesCnt + "\n");

		// count rels
		int allEdgesCnt = 0;
		for (Relationship edge : dataGraph.getAllRelationships()) {
			allEdgesCnt++;
		}

		readmeWriter.write("Number of relationships = " + allEdgesCnt + "\n");

		// count labels
		TreeMap<String, Integer> distinctLabelsMap = new TreeMap<String, Integer>();
		for (Node node : dataGraph.getAllNodes()) {
			for (Label label : node.getLabels()) {
				if (distinctLabelsMap.containsKey(label.toString())) {
					distinctLabelsMap.put(label.toString(), distinctLabelsMap.get(label.toString()) + 1);
				} else {
					distinctLabelsMap.put(label.toString(), 1);
				}
			}
		}

		readmeWriter.write("Distinct Node Labels = " + distinctLabelsMap.size() + "\n");

		// count types
		HashMap<String, Integer> relTypeFreq = new HashMap<>();

		for (Relationship rel : dataGraph.getAllRelationships()) {

			if (relTypeFreq.containsKey(rel.getType().name())) {
				relTypeFreq.put(rel.getType().name(), relTypeFreq.get(rel.getType().name()) + 1);
			} else {
				relTypeFreq.put(rel.getType().name(), 1);
			}
		}

		readmeWriter.write("Distinct relationship types = " + relTypeFreq.keySet().size() + "\n");

		// Distributions

		readmeWriter.write("--Node Label Distribution\n");
		for (String label : distinctLabelsMap.navigableKeySet()) {
			readmeWriter.write(label + " : " + distinctLabelsMap.get(label) + "\n");
		}

		readmeWriter.write("--Rel Type Distribution\n");
		for (String s : relTypeFreq.keySet()) {
			readmeWriter.write(s + " : " + relTypeFreq.get(s) + "\n");
		}

		readmeWriter.close();

		tx.success();
		dataGraph.shutdown();
	}

}
