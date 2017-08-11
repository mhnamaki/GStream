
package src.graphMaker;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

public class GraphCreator2 {

	public static HashSet<Node> nodeCreatedSoFar = new HashSet<Node>();

	public static int maxDegree = 5;
	public static int maxNumberOfLabels = 1;
	public static int numberOfNodes = 500;
	public static String dbPath = "/home/shayan/Documents/WSU/Data/Synthetic";
	public static String wordFilePath = "words.txt";
	public static float edgeToNodesRatio = 3;
	public static int numberOfDistinctLabels = 50;

	private static enum RelTypes implements RelationshipType {
		NOTYPE
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-numberOfNodes")) {
				numberOfNodes = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-maxDegree")) {
				maxDegree = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-maxNumberOfLabels")) {
				maxNumberOfLabels = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dbPath")) {
				dbPath = args[++i];
			} else if (args[i].equals("-wordFilePath")) {
				wordFilePath = args[++i];
			} else if (args[i].equals("-edgeToNodesRation")) {
				edgeToNodesRatio = Float.parseFloat(args[++i]);
			} else if (args[i].equals("-numberOfDistinctLabels")) {
				numberOfDistinctLabels = Integer.parseInt(args[++i]);
			}

		}

		if (dbPath == null || numberOfNodes == 0 || maxNumberOfLabels == 0 || maxDegree == 0) {
			throw new Exception("dbPath==null || numberOfNodes==0 ||  maxNumberOfLabels==0 || maxDegree ==0");
		}
		if (((numberOfNodes - 1) / 2) < edgeToNodesRatio) {
			throw new Exception(
					"cannot satisfy the edgeToNodesRatio the complete graph for this number of nodes has less edges than this ratio");
		}
		try {
			createSyntheticG();
		} catch (Exception exc) {
			exc.printStackTrace();
		}

	}

	private static void deleteDirectory(String path) throws Exception {
		Path directory = Paths.get(path);
		if (java.nio.file.Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

			});
		}

	}

	private static void createSyntheticG() throws Exception {
		try {
			deleteDirectory(dbPath);
		} catch (Exception exc) {
			exc.printStackTrace();
			return;
		}
		LinkedList<MyNode> nodesWaitingForNeighbors = new LinkedList<MyNode>();
		ArrayList<Long> nodeIdsInTheGraph = new ArrayList<Long>();
		ArrayList<String> wordsList = getAllPossibleWords(wordFilePath);
		System.out.println("all word size: " + wordsList.size());
		Random random = new Random();
		int sizeOfWordsList = wordsList.size();
		File storeDir = new File(dbPath);

		GraphDatabaseService newRandomGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		registerShutdownHook(newRandomGraph);
		int numberOfRelationships = 0;
		try (Transaction tx = newRandomGraph.beginTx()) {
			int numberOfNodeCreated = 0;
			while (numberOfNodeCreated < numberOfNodes) {
				if (nodesWaitingForNeighbors.isEmpty()) {
					int degreeForThisNode = random.nextInt(maxDegree - 1) + 1;
					int numberOfLabels = 1;
					if (maxNumberOfLabels > 1)
						numberOfLabels = random.nextInt(maxNumberOfLabels - 1) + 1;
					Node tempNode = newRandomGraph.createNode();
					// tempNode.setProperty("uniqueManualId", tempNode.getId());
					nodeIdsInTheGraph.add(tempNode.getId());
					numberOfNodeCreated++;
					for (int labelIndex = 0; labelIndex < numberOfLabels; labelIndex++) {
						// for having more repeating labels.
						int wordIndex = random.nextInt(Math.min(sizeOfWordsList, numberOfDistinctLabels));
						tempNode.addLabel(Label.label(wordsList.get(wordIndex)));
					}

					nodesWaitingForNeighbors.add(new MyNode(tempNode.getId(), degreeForThisNode));
				} else {
					MyNode nodeWaitingForNeighbor = nodesWaitingForNeighbors.getFirst();

					for (int neighborIndex = 0; neighborIndex < nodeWaitingForNeighbor.degree; neighborIndex++) {
						Node neighborNode = newRandomGraph.createNode();
						// neighborNode.setProperty("uniqueManualId",
						// neighborNode.getId());
						nodeIdsInTheGraph.add(neighborNode.getId());

						// random relation direction
						if (random.nextInt(2) % 2 == 1) {
							newRandomGraph.getNodeById(nodeWaitingForNeighbor.nodeId).createRelationshipTo(neighborNode,
									GraphCreator2.RelTypes.NOTYPE);
						} else {
							neighborNode.createRelationshipTo(newRandomGraph.getNodeById(nodeWaitingForNeighbor.nodeId),
									GraphCreator2.RelTypes.NOTYPE);
						}
						numberOfRelationships++;
						numberOfNodeCreated++;
						int degreeForThisNode = random.nextInt(maxDegree - 1) + 1;
						int numberOfLabels = 1;
						if (maxNumberOfLabels > 1)
							numberOfLabels = random.nextInt(maxNumberOfLabels - 1) + 1;
						for (int labelIndex = 0; labelIndex < numberOfLabels; labelIndex++) {
							int wordIndex = random.nextInt(Math.min(sizeOfWordsList, numberOfDistinctLabels));
							neighborNode.addLabel(Label.label(wordsList.get(wordIndex)));
						}
						// because it's already have a relationship.
						// (degreeForThisNode - 1)
						nodesWaitingForNeighbors.add(new MyNode(neighborNode.getId(), degreeForThisNode - 1));
					}
					nodesWaitingForNeighbors.remove(nodeWaitingForNeighbor);
				}
			}

			// for changing this tree like graph to cyclic graph
			int currentNumberOfNodes = nodeIdsInTheGraph.size();
			while ((numberOfRelationships / currentNumberOfNodes) < edgeToNodesRatio) {
				long firstNodeId = random.nextInt(currentNumberOfNodes);
				long secondNodeId = random.nextInt(currentNumberOfNodes);

				if (!existsRelationship(newRandomGraph, firstNodeId, secondNodeId)) {
					newRandomGraph.getNodeById(firstNodeId).createRelationshipTo(
							newRandomGraph.getNodeById(secondNodeId), GraphCreator2.RelTypes.NOTYPE);
					numberOfRelationships++;
				}
			}

			System.out.println("program finished properly!");
			tx.success();
		} catch (Exception exc) {
			exc.printStackTrace();
			newRandomGraph.shutdown();
		}
		newRandomGraph.shutdown();
	}

	private static boolean existsRelationship(GraphDatabaseService randomGraph, long firstNodeId, long secondNodeId) {
		Node firstNode = randomGraph.getNodeById(firstNodeId);
		for (Relationship rel : firstNode.getRelationships()) {
			if (rel.getOtherNode(firstNode).getId() == secondNodeId) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<String> getAllPossibleWords(String filePath) throws Exception {
		ArrayList<String> wordsList = new ArrayList<String>();
		HashSet<String> wordsSeenSoFar = new HashSet<String>();
		FileReader fileReader = new FileReader(filePath);

		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			line = line.trim().replaceAll("[^A-Za-z0-9 ]", "");
			if (!wordsSeenSoFar.contains(line) && line.length() > 4) {
				wordsList.add(line);
				wordsSeenSoFar.add(line);
			}
		}
		bufferedReader.close();
		return wordsList;
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

}
