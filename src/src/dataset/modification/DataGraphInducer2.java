package src.dataset.modification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class DataGraphInducer2 {

	private static String dataGraphPath;
	private static int maxNumberOfNodes;
	private static String neo4jImportPath;
	private static String nodesRelFileDir;
	private static String futureDbPath;

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-maxNumberOfNodes")) {
				maxNumberOfNodes = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-neo4jImportPath")) {
				neo4jImportPath = args[++i];
				if (!neo4jImportPath.endsWith("/")) {
					neo4jImportPath += "/";
				}
			} else if (args[i].equals("-nodesRelFileDir")) {
				nodesRelFileDir = args[++i];
				if (!nodesRelFileDir.endsWith("/")) {
					nodesRelFileDir += "/";
				}
			} else if (args[i].equals("-futureDbPath")) {
				futureDbPath = args[++i];
			}

		}

		if (dataGraphPath == null || maxNumberOfNodes == 0 || futureDbPath == null || nodesRelFileDir == null
				|| neo4jImportPath == null) {
			throw new Exception("Input parameters needed!");
		}

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataGraphPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		try (Transaction tx1 = dataGraph.beginTx()) {
			HashSet<String> distinctLabels = new HashSet<String>();
			ArrayList<Long> allNodes = new ArrayList<Long>();
			HashSet<Long> visitedNodes = new HashSet<Long>();
			for (Node node : dataGraph.getAllNodes()) {
				allNodes.add(node.getId());
				for (Label lbl : node.getLabels()) {
					distinctLabels.add(lbl.name());
				}
			}

			File foutNodeType = new File("nodes.txt");
			FileOutputStream fosNodeType = new FileOutputStream(foutNodeType);
			BufferedWriter bwNodeType = new BufferedWriter(new OutputStreamWriter(fosNodeType));
			// TODO: handling properties, we have to have different files then
			// we can have the different properties
			bwNodeType.write("unique:ID,:Label");
			bwNodeType.newLine();

			HashSet<String> distinctRelTypes = new HashSet<String>();
			for (Relationship relationship : dataGraph.getAllRelationships()) {
				distinctRelTypes.add(relationship.getType().name());
			}

			File foutRel = new File("Relationships.txt");
			FileOutputStream fosRel = new FileOutputStream(foutRel);
			BufferedWriter bwRel = new BufferedWriter(new OutputStreamWriter(fosRel));
			bwRel.write(":START_ID,:END_ID,:TYPE");
			bwRel.newLine();

			Queue<Long> nodesWaitingForExpansion = new LinkedList<Long>();
			Random random = new Random();
			int cnt = 0;
			while (cnt < maxNumberOfNodes) {
				int nodeIndex = random.nextInt(allNodes.size());
				nodesWaitingForExpansion.add(allNodes.get(nodeIndex));
				allNodes.remove(allNodes.get(nodeIndex));

				while (!nodesWaitingForExpansion.isEmpty()) {
					Node currNode = dataGraph.getNodeById(nodesWaitingForExpansion.poll());
					if (!visitedNodes.contains(currNode.getId())) {
						bwNodeType.write(currNode.getId() + "," + currNode.getLabels().iterator().next().name());
						bwNodeType.newLine();
						visitedNodes.add(currNode.getId());
						cnt++;
					}

					for (Relationship rel : currNode.getRelationships(Direction.OUTGOING)) {
						bwRel.write(rel.getStartNode().getId() + "," + rel.getEndNode().getId() + ","
								+ rel.getType().name());
						bwRel.newLine();

						if (!visitedNodes.contains(rel.getEndNode().getId())) {
							nodesWaitingForExpansion.add(rel.getEndNode().getId());
							allNodes.remove(rel.getEndNode().getId());
						}
					}
				}
			}

			bwNodeType.close();

			bwRel.close();

			deleteDirIfExist(futureDbPath);

			Process p = Runtime.getRuntime().exec(neo4jImportPath + "neo4j-import --into " + futureDbPath + " --nodes "
					+ nodesRelFileDir + "nodes.txt --relationships " + nodesRelFileDir + "Relationships.txt");
			p.waitFor();
			int exitVal = p.exitValue();

			System.out.println("proc.exitValue(): " + exitVal);

			System.out.println("program is finished properly!");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("some err!");
		}

	}

	private static void deleteDirIfExist(String directoryPath) throws Exception {
		Path directory = Paths.get(directoryPath);
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
