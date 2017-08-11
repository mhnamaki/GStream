package src.dataset.syntheticGraphCreator;

import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import org.jgrapht.graph.ClassBasedVertexFactory;
import org.jgrapht.graph.DefaultDirectedGraph;
import src.dataset.dataSnapshot.SnapshotCreator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

/**
 * This program gets some information such as the number of nodes, number of
 * node/edge labels, proportion on edges in G0, and creates a neo4j data graph,
 * G0, and Deltas (for stream graphs). Graph generation is done using jgrapht's
 * scale free network tools. Created by shayan on 7/12/16.
 *
 * @parameter -numberOfNodes: The integer number of nodes for the generated
 *            synthetic graph
 * @parameter -numberOfNodeLabels: The integer number of maximum node labels
 *            present in the graph
 * @parameter -numberOfRelationshipTypes: The integer number of maximum
 *            relationship types present in the graph
 * @parameter -proportionOfG0Relationships: The float value representing the
 *            number proportion of total relationships which will be in G0
 * @parameter -neo4jPath: installation path for neo4j (because we need to call
 *            neo4j-import to create graph)
 * @parameter -storeDir: the directory to save everything
 * @parameter -labelWordsPath: path to a text file that contains words,
 *            separated by '\n'
 * @parameter -minYear: integer value representing the smallest year for
 *            timestamp
 * @parameter -maxYear: integer value representing the largest year for
 *            timestamp
 *
 *
 * @author Shayan Monadjemi
 * @date July 12th, 2016
 */
public class SyntheticScaleFreeGraphCreator {

	private static enum StreamMode {
		INS, DEL, MIX
	};

	public static void main(String[] args) throws Exception {

		int MIN_YEAR = 1800; // Smallest year for the timestamp
		int MAX_YEAR = 2016; // Largest year for the timestamp

		int numberOfNodes = 100; // total number of nodes in the complete data
								// graph
		int numberOfNodeLabels = 10; // maximum number of node labels
		int numberOfRelationshipTypes = 3; // maximum number f edge labels
		double proportionOfG0Relationships = 1; // proportion of nodes present
													// in G0
		int numberOfG0Relationships;
		int numberOfEachDeltaRelationships = 100000; // each delta file will
														// contain this many
														// relationships

		String labelWordsPath = "/home/shayan/Documents/WSU/TopKGStream/words.txt";
		// words to choose from as labels, separated by \n
		String neo4jPath = "/home/shayan/Programs/neo4j-community-3.0.1";
		// neo4j installation path
		String storeDir = "/home/shayan/Documents/WSU/Data/Synthetic/";
		// Directory to store graph, g0, and deltas

		//
		// store
		// graph,
		// g0,
		// and
		// deltas

		final String DATE_FORMAT = "yyyy"; // format of timestamp
		final String ADD_TIMESTAMP_KEY = "addTime";
		final String DELETE_TIMESTAMP_KEY = "deleteTime";

		StreamMode streamMode = StreamMode.MIX;

		/**
		 * check for arguments here
		 */

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-labelWordsPath")) {
				labelWordsPath = args[++i];
			} else if (args[i].equals("-maxYear")) {
				MAX_YEAR = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-minYear")) {
				MIN_YEAR = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfNodes")) {
				numberOfNodes = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfNodeLabels")) {
				numberOfNodeLabels = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-numberOfRelationshipTypes")) {
				numberOfRelationshipTypes = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-neo4jPath")) {
				neo4jPath = args[++i];
				if (!neo4jPath.endsWith("/")) {
					neo4jPath += "/";
				}
			} else if (args[i].equals("-storeDir")) {
				storeDir = args[++i];
				if (!storeDir.endsWith("/")) {
					storeDir += "/";
				}
			} else if (args[i].equals("-proportionOfG0Relationships")) {
				proportionOfG0Relationships = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-streamMode")) {
				switch (args[++i].toLowerCase()) {
				case "insertion":
					streamMode = StreamMode.INS;
					break;
				case "deletion":
					streamMode = StreamMode.DEL;
					break;
				case "mixed":
					streamMode = StreamMode.MIX;
					break;
				}
			}

		}

		String completeDataGraphPath = storeDir + "completeGraph.graphdb";
		String nodesPath = storeDir + "nodes.csv";
		String relationshipsPath = storeDir + "relationships.csv";

		int totalNumberOfEdges;

		File nodeFile = new File(nodesPath);
		File relationshipFile = new File(relationshipsPath);

		FileWriter nodeWriter = new FileWriter(nodeFile);
		FileWriter relationshipWriter = new FileWriter(relationshipFile);

		ArrayList<String> wordsList = getAllPossibleWords(labelWordsPath);
		System.out.println("all word size: " + wordsList.size());

		String[] nodeLabels = populateWordList(wordsList, numberOfNodeLabels);
		String[] relationshipTypes = populateWordList(wordsList, numberOfRelationshipTypes);

		System.out.print("Generating scale free graph...\n");

		double startTime = System.nanoTime();

		Graph<Vertex, Edge> myG = new DefaultDirectedGraph<Vertex, Edge>(Edge.class);
		ScaleFreeGraphGenerator<Vertex, Edge> scaleFreeGraphGenerator = new ScaleFreeGraphGenerator<Vertex, Edge>(
				numberOfNodes);
		
		VertexFactory<Vertex> vertexFactory = new ClassBasedVertexFactory<Vertex>(Vertex.class);
		scaleFreeGraphGenerator.generateGraph(myG, Vertex::new, null);

		System.out.println("Generating scale free graph: " + (System.nanoTime() - startTime) / 1e6);

		System.out.println("Number of edges in the generated graph: " + myG.edgeSet().size());
		totalNumberOfEdges = myG.edgeSet().size();

		numberOfG0Relationships = (int) (totalNumberOfEdges * proportionOfG0Relationships);

		startTime = System.nanoTime();

		double[] probs = new double[nodeLabels.length];
		double[] probabilityRanges = new double[nodeLabels.length];
		for (int i = 0; i < probabilityRanges.length; i++) {
			double current = Math.pow(2, (-1.0 * (i + 1)));
			probs[i] = current;
			double sum = current;
			for (int j = 0; j < i; j++) {
				sum += probs[j];
			}
			probabilityRanges[i] = sum;
		}

		System.out.println("probabilityRanges time: " + (System.nanoTime() - startTime) / 1e6);

		int vid = 0;

		startTime = System.nanoTime();
		// boolean randomly = false;
		int currentIndex = nodeLabels.length - 1;
		String nodeLabel;
		for (Vertex v : myG.vertexSet()) {
			if (currentIndex > 0) {
				nodeLabel = nodeLabels[currentIndex--];
			} else {
				double random = Math.random();
				int vi = getIndexForRandomNumber(random, probabilityRanges);
				nodeLabel = nodeLabels[vi];

			}

			v.label = nodeLabel;
			v.nodeId = vid;
			vid++;

		}

		System.out.println("setting labels time: " + (System.nanoTime() - startTime) / 1e6);

		HashMap<String, String> relTypes = new HashMap<>();

		startTime = System.nanoTime();
		currentIndex = relationshipTypes.length - 1;

		for (int i = 0; i < nodeLabels.length; i++) {
			for (int j = 0; j < nodeLabels.length; j++) {
				String relType;
				if (currentIndex > 0) {
					relType = relationshipTypes[currentIndex];
					currentIndex--;
				} else {
					int ei = (int) (Math.random() * relationshipTypes.length);
					relType = relationshipTypes[ei];
				}

				String n = nodeLabels[i] + "_" + nodeLabels[j];
				relTypes.put(n, relType);
			}
		}

		for (Edge e : myG.edgeSet()) {

			int at = (int) (Math.random() * (MAX_YEAR - MIN_YEAR)) + MIN_YEAR;
			int dt = (int) (Math.random() * (MAX_YEAR - MIN_YEAR)) + MIN_YEAR;
			while (dt < at) {
				dt = (int) (Math.random() * (MAX_YEAR - MIN_YEAR)) + MIN_YEAR;
			}

			String relType = relTypes.get(myG.getEdgeSource(e).getLabel() + "_" + myG.getEdgeTarget(e).getLabel());

			e.setTimeStamps(at, dt);
			e.relationshipType = relType;

		}
		System.out.println("setting relType time: " + (System.nanoTime() - startTime) / 1e6);

		System.out.print("Scale free graph created.\nCreating neo4j import csv files for the graph...\n");

		startTime = System.nanoTime();

		nodeWriter.write("unique:ID,:LABEL\n");
		String firstRelFileLine = ":START_ID,:END_ID,:TYPE";
		if (streamMode != StreamMode.DEL) {
			firstRelFileLine += "," + ADD_TIMESTAMP_KEY;
		}
		if (streamMode != StreamMode.INS) {
			firstRelFileLine += "," + DELETE_TIMESTAMP_KEY;
		}
		firstRelFileLine += "\n";
		relationshipWriter.write(firstRelFileLine);

		for (Vertex v : myG.vertexSet()) {
			nodeWriter.write(v.getNodeId() + "," + v.label + "\n");
		}
		for (Edge e : myG.edgeSet()) {
			String edgeLine = myG.getEdgeSource(e).getNodeId() + "," + myG.getEdgeTarget(e).nodeId + ","
					+ e.relationshipType;

			if (streamMode != StreamMode.DEL) {
				edgeLine += "," + e.addTimeStamp;
			}
			if (streamMode != StreamMode.INS) {
				edgeLine += "," + e.deleteTimeStamp;
			}
			edgeLine += "\n";

			relationshipWriter.write(edgeLine);
		}

		nodeWriter.close();
		relationshipWriter.close();

		System.out.println("write node/rels files time: " + (System.nanoTime() - startTime) / 1e6);

		System.out.print("neo4j import files created.\nCreating neo4j dataGraph...\n");

		startTime = System.nanoTime();

		String neo4jCommand = neo4jPath + "/bin/neo4j-import --into " + completeDataGraphPath + " --nodes "
				+ nodeFile.getAbsolutePath() + " --relationships " + relationshipFile.getAbsolutePath();

		Process p = Runtime.getRuntime().exec(neo4jCommand);
		p.waitFor();
		int exitVal = p.exitValue();

		System.out.println("proc.exitValue(): " + exitVal);
		if (exitVal == 0)
			System.out.println("Neo4j imported properly!");
		else {
			System.out.println("ERROR: Neo4j messed up");
			System.exit(1);
			String line;
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
			in.close();
		}

		System.out.println("neo4j import time: " + (System.nanoTime() - startTime) / 1e6);

		System.out.print("Creating snapshots...\n");

		startTime = System.nanoTime();

		src.dataset.dataSnapshot.SnapshotCreator snapshotCreator = new SnapshotCreator(completeDataGraphPath,
				numberOfG0Relationships, storeDir, numberOfEachDeltaRelationships, ADD_TIMESTAMP_KEY,
				DELETE_TIMESTAMP_KEY, DATE_FORMAT);

		snapshotCreator.run();

		System.out.println("creation of snapshot time: " + (System.nanoTime() - startTime) / 1e6);

		System.out.print("DONE\n");

	}

	/**
	 * This method reads a file containing words with delimeter '\n', and adds
	 * all the valid alphabetic words to an ArrayList.
	 * 
	 * @param filePath
	 *            the .txt file containing the words
	 * @return an ArrayList with all the words in it
	 * @throws Exception
	 *             in case the file does not exist
	 */
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

	/**
	 * Populated an array of given size with words from our word bank. The words
	 * added to the array will be removed from the arraylist to prevent repeated
	 * labels.
	 * 
	 * @param fullList
	 *            word bank
	 * @param num
	 *            size of the returned array (number of needed words)
	 * @return an array of num words
	 */
	public static String[] populateWordList(ArrayList<String> fullList, int num) {
		String[] newList = new String[num];
		Random rnd = new Random();
		HashSet<Integer> seenIndices = new HashSet<Integer>();

		for (int i = 0; i < newList.length; i++) {

			int rndVal = rnd.nextInt(fullList.size());

			while (seenIndices.contains(rndVal)) {
				rndVal = rnd.nextInt(fullList.size());
			}

			seenIndices.add(rndVal);

			newList[i] = fullList.get(rndVal);

			fullList.remove(rndVal);

		}

		return newList;
	}

	public static int getIndexForRandomNumber(double random, double[] probabilityRanges) {

		if (random >= 0 && random < probabilityRanges[0]) {
			return 0;
		} else if (random >= probabilityRanges[probabilityRanges.length - 1] && random < 1) {
			return probabilityRanges.length - 1;
		} else {
			for (int i = 1; i < probabilityRanges.length - 1; i++) {
				if (random >= probabilityRanges[i - 1] && random < probabilityRanges[i]) {
					return i;
				}
			}
		}

		return 0;

	}

}
