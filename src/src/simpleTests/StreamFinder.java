package src.simpleTests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.joda.time.format.DateTimeFormat;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;
import src.utilities.Dummy.DummyFunctions;

public class StreamFinder {
	private static String allFocusLinesPath = "syntheticFocuses.txt";
	private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/synthFinal/snapshot/g0.graphdb";
	private String allDeltaEFilesPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/synthFinal/snapshot";
	private GraphDatabaseService dataGraph;
	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();

	public HashSet<Integer> allFocusNodesSet = new HashSet<Integer>();

	private HashSet<String> focusLabelSet = new HashSet<String>();

	public SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");

	public StreamFinder(String[] args) {

	}

	public static void main(String[] args) throws Exception {

		StreamFinder sf = new StreamFinder(args);
		// sf.updateLabelsOfIDS();
		// sf.runGetRealStream();
		// sf.runMakeSyntheticStream();
		// sf.mergeTwoDeltas();
		// sf.removeAffDeleteStreams();

		//sf.getNonAffStream();
		sf.getNonAffInsStream();

		// sf.findFocusNodesBasedOnAffected();

		// sf.removeDeleteStream();
	}

	private void removeDeleteStream() throws Exception {

		File foutFocus = new File("newDeltaIns.txt");
		FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		BufferedWriter bwNewDeltaIns = new BufferedWriter(new OutputStreamWriter(fosFocus));

		File foutFocus2 = new File("newDeltaDel.txt");
		FileOutputStream fosFocus2 = new FileOutputStream(foutFocus2);
		BufferedWriter bwNewDeltaDel = new BufferedWriter(new OutputStreamWriter(fosFocus2));

		File foutFocus3 = new File("nonAffIns.txt");
		FileOutputStream fosFocus3 = new FileOutputStream(foutFocus3);
		BufferedWriter bwNewDeltaNonAff = new BufferedWriter(new OutputStreamWriter(fosFocus3));

		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		HashSet<Long> targetOfFocusNodes = new HashSet<Long>();

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			focusLabelSet.clear();
			allFocusNodesSet.clear();
			fillSet(line);
			fillFocusNodesOfRequestedTypes(dataGraph);

			System.out.println();
			System.out.println();
			System.out.println(line);

			// first scan
			SnapshotSimulator snapshotSim1 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy-MM-dd HH:mm:ss");
			StreamEdge streamEdge1 = null;
			int index = 0;
			while (true) {
				streamEdge1 = snapshotSim1.getNextStreamEdge();
				index++;
				if (streamEdge1 == null)
					break;

				if (index > 120000)
					break;

				long srcNodeId = streamEdge1.getSourceNode();
				long destNodeId = streamEdge1.getDestinationNode();

				if (allFocusNodesSet.contains(srcNodeId)) {
					targetOfFocusNodes.add(destNodeId);
				}

			}

			// adding target from init graph
			for (Integer focusNodeId : allFocusNodesSet) {
				for (Relationship rel : dataGraph.getNodeById(focusNodeId).getRelationships(Direction.OUTGOING)) {
					targetOfFocusNodes.add(rel.getEndNode().getId());
				}
			}
		}

		HashSet<String> packetLenVal = new HashSet<String>();
		packetLenVal.add("112");
		packetLenVal.add("44");
		packetLenVal.add("238");
		packetLenVal.add("100");

		HashSet<String> focusVal = new HashSet<String>();
		focusVal.add("t1");
		focusVal.add("t2");
		focusVal.add("t3");

		String ipTTL = "52";

		// Log|ipttl:"52"
		// Log|packetlen:"112"
		// Log|packetlen:"44"
		// Log|packetlen:"238"
		// Log|packetlen:"100"
		//
		HashSet<Long> allFocusNodes = new HashSet<Long>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Log"))) {
				if (node.hasProperty("packetlen") && packetLenVal.contains(node.getProperty("packetlen").toString())) {
					allFocusNodes.add(node.getId());
				}
				if (node.hasProperty("ipttl") && node.getProperty("ipttl").toString().equals("52")) {
					allFocusNodes.add(node.getId());
				}
				if (node.hasProperty("focus") && focusVal.contains(node.getProperty("focus").toString())) {
					allFocusNodes.add(node.getId());
				}

			}
		}

		SnapshotSimulator snapshotSim1 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		StreamEdge streamEdge1 = null;
		int index = 0;
		int writtenIndex = 0;
		while (true) {
			streamEdge1 = snapshotSim1.getNextStreamEdge();
			index++;

			if (streamEdge1 == null)
				break;

			if (writtenIndex > 200000)
				break;

			long srcNodeId = streamEdge1.getSourceNode();
			long destNodeId = streamEdge1.getDestinationNode();

			if (streamEdge1.isAdded()
					&& (allFocusNodes.contains(srcNodeId) || targetOfFocusNodes.contains(srcNodeId))) {
				bwNewDeltaIns.write(index + "#" + line + "#" + (streamEdge1.isAdded() ? "+," : "-,")
						+ sdf.format(streamEdge1.getTimeStamp()) + "," + srcNodeId + "," + destNodeId + ","
						+ streamEdge1.getRelationshipType() + "\n");
				bwNewDeltaIns.flush();

			} else if (!streamEdge1.isAdded()
					&& (allFocusNodes.contains(srcNodeId) || targetOfFocusNodes.contains(srcNodeId))) {
				bwNewDeltaDel.write(index + "#" + line + "#" + "-," + sdf.format(streamEdge1.getTimeStamp()) + ","
						+ srcNodeId + "," + destNodeId + "," + streamEdge1.getRelationshipType() + "\n");
				bwNewDeltaDel.flush();

			} else if (streamEdge1.isAdded()) {
				bwNewDeltaNonAff.write(index + "#" + line + "#" + "+," + sdf.format(streamEdge1.getTimeStamp()) + ","
						+ srcNodeId + "," + destNodeId + "," + streamEdge1.getRelationshipType() + "\n");
				bwNewDeltaNonAff.flush();
				writtenIndex++;
			}

		}

		tx1.success();
		tx1.close();

		bwNewDeltaDel.close();
		bwNewDeltaIns.close();
		bwNewDeltaNonAff.close();

		dataGraph.shutdown();

	}

	private void findFocusNodesBasedOnAffected() throws Exception {
		SnapshotSimulator snapshotSim1 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy-MM-dd HH:mm:ss");
		StreamEdge streamEdge1 = null;
		int index = 0;

		HashMap<Long, Integer> nodeIds = new HashMap<Long, Integer>();
		HashMap<Long, ArrayList<Integer>> indexOfSeenNode = new HashMap<Long, ArrayList<Integer>>();

		while (true) {
			streamEdge1 = snapshotSim1.getNextStreamEdge();
			index++;
			if (streamEdge1 == null)
				break;

			if (index > 140000)
				break;

			long srcNodeId = streamEdge1.getSourceNode();

			if (streamEdge1.isAdded()) {
				nodeIds.putIfAbsent(srcNodeId, 0);
				nodeIds.put(srcNodeId, nodeIds.get(srcNodeId) + 1);
				indexOfSeenNode.putIfAbsent(srcNodeId, new ArrayList<>());
				indexOfSeenNode.get(srcNodeId).add(index);
			}

		}

		Set<Entry<Long, Integer>> set = nodeIds.entrySet();
		List<Entry<Long, Integer>> list = new ArrayList<Entry<Long, Integer>>(set);
		Collections.sort(list, new Comparator<Map.Entry<Long, Integer>>() {
			@Override
			public int compare(Entry<Long, Integer> o1, Entry<Long, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		for (Map.Entry<Long, Integer> entry : list) {
			System.out.print(entry.getKey() + " ==== " + entry.getValue() + " $$$###: ");
			for (Integer indices : indexOfSeenNode.get(entry.getKey())) {
				System.out.print(indices + ", ");
			}
			System.out.println();
		}

	}

	private void updateLabelsOfIDS() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Host"))) {
				if (node.hasProperty("hostName ")) {
					node.removeLabel(Label.label("HostName"));
					node.addLabel(Label.label(node.getProperty("hostName ").toString()));

					System.out.println(node.getProperty("hostName ").toString());
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();

				}
			}
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void removeAffDeleteStreams() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// get all focus node ids

		File foutnewDelta = new File("newDelta000.txt");
		FileOutputStream fosnewDelta = new FileOutputStream(foutnewDelta);
		BufferedWriter newDeltaBW = new BufferedWriter(new OutputStreamWriter(fosnewDelta));

		File foutFocus = new File("focusStream.txt");
		FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		BufferedWriter bwFocus = new BufferedWriter(new OutputStreamWriter(fosFocus));

		File foutFTarget = new File("targetFocusStream.txt");
		FileOutputStream fosFTarget = new FileOutputStream(foutFTarget);
		BufferedWriter bwFTarget = new BufferedWriter(new OutputStreamWriter(fosFTarget));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		HashSet<Long> targetOfFocusNodes = new HashSet<Long>();
		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			focusLabelSet.clear();
			// allFocusNodesSet.clear();
			fillSet(line);
			fillFocusNodesOfRequestedTypes(dataGraph);

			System.out.println();
			System.out.println();
			System.out.println(line);

			// first scan
			SnapshotSimulator snapshotSim1 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy-MM-dd HH:mm:ss");
			StreamEdge streamEdge1 = null;
			int index = 0;
			while (true) {
				streamEdge1 = snapshotSim1.getNextStreamEdge();
				index++;
				if (streamEdge1 == null)
					break;

				if (index > 140000)
					break;

				long srcNodeId = streamEdge1.getSourceNode();
				long destNodeId = streamEdge1.getDestinationNode();

				if (allFocusNodesSet.contains(srcNodeId)) {

					// bwFocus.write(index + "#" + line + "#" +
					// (streamEdge1.isAdded() ? "+," : "-,")
					// + streamEdge1.getTimeStamp() + "," + srcNodeId + "," +
					// destNodeId + ","
					// + streamEdge1.getRelationshipType() + "\n");
					// bwFocus.flush();

					targetOfFocusNodes.add(destNodeId);
				}

			}

			// adding target from init graph
			for (Integer focusNodeId : allFocusNodesSet) {
				for (Relationship rel : dataGraph.getNodeById(focusNodeId).getRelationships(Direction.OUTGOING)) {
					targetOfFocusNodes.add(rel.getEndNode().getId());
				}
			}

		}
		// second scan
		SnapshotSimulator snapshotSim2 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy-MM-dd HH:mm:ss");
		StreamEdge streamEdge2 = null;
		int index = 0;
		while (true) {
			streamEdge2 = snapshotSim2.getNextStreamEdge();
			index++;
			if (streamEdge2 == null)
				break;

			if (index > 200000)
				break;

			long srcNodeId = streamEdge2.getSourceNode();
			long destNodeId = streamEdge2.getDestinationNode();

			if ((targetOfFocusNodes.contains(srcNodeId) || allFocusNodesSet.contains(srcNodeId)
					|| targetOfFocusNodes.contains(destNodeId)) && !streamEdge2.isAdded()) {

				bwFTarget.write(index + "#" + line + "#" + (streamEdge2.isAdded() ? "+," : "-,")
						+ dateFormat.format(streamEdge2.getTimeStamp()) + "," + srcNodeId + "," + destNodeId + ","
						+ streamEdge2.getRelationshipType() + "\n");
				bwFTarget.flush();
			} else {
				newDeltaBW.write((streamEdge2.isAdded() ? "+," : "-,") + dateFormat.format(streamEdge2.getTimeStamp())
						+ "," + srcNodeId + "," + destNodeId + "," + streamEdge2.getRelationshipType() + "\n");
			}

		}

		br.close();
		bwFocus.close();
		bwFTarget.close();
		newDeltaBW.close();
		tx1.success();
		dataGraph.shutdown();

	}

	private void mergeTwoDeltas() throws Exception {

		File foutFocus = new File("mergedDelta.txt");
		FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		BufferedWriter bwMergedDelta = new BufferedWriter(new OutputStreamWriter(fosFocus));

		FileInputStream fis = new FileInputStream("focusStream.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader brSynthetic = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		HashMap<String, ArrayList<String>> affectedEdges = new HashMap<String, ArrayList<String>>();

		while ((line = brSynthetic.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			affectedEdges.putIfAbsent(line.split("#")[0].trim(), new ArrayList<String>());
			affectedEdges.get(line.split("#")[0].trim()).add(line.split("#")[1].trim());
		}

		SnapshotSimulator snapshotSim1 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy");
		StreamEdge streamEdge1 = null;
		Random rnd = new Random();
		int totalAffectedEdge = 0;

		for (int i = 0; i < 12; i++) {

			int cnt = 0;

			for (String keyFocus : affectedEdges.keySet()) {
				int rndInt = rnd.nextInt(3);
				ArrayList<String> lines = affectedEdges.get(keyFocus);
				for (int j = 0; j < rndInt && j < lines.size(); j++) {
					bwMergedDelta.write(lines.get(j) + "\n");
					cnt++;
				}

				if (lines.size() > 0)
					lines.subList(0, Math.min(rndInt, lines.size())).clear();
			}

			for (int k = 0; k < (10000 - cnt); k++) {
				streamEdge1 = snapshotSim1.getNextStreamEdge();

				if (streamEdge1 == null)
					break;

				bwMergedDelta.write("+," + "2010" + "," + streamEdge1.getSourceNode() + ","
						+ streamEdge1.getDestinationNode() + "," + streamEdge1.getRelationshipType() + "\n");
				bwMergedDelta.flush();
			}

			totalAffectedEdge += cnt;
		}

		System.out.println("totalAffectedEdge: " + totalAffectedEdge);
		bwMergedDelta.close();
		brSynthetic.close();

	}

	private void runMakeSyntheticStream() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "1g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// get all focus node ids
		File foutFocus = new File("insertStream.txt");
		FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		BufferedWriter bwFocus = new BufferedWriter(new OutputStreamWriter(fosFocus));

		File foutFTarget = new File("deleteStream.txt");
		FileOutputStream fosFTarget = new FileOutputStream(foutFTarget);
		BufferedWriter bwFTarget = new BufferedWriter(new OutputStreamWriter(fosFTarget));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		HashSet<Long> alreadyUsedPapers = new HashSet<Long>();

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			focusLabelSet.clear();
			allFocusNodesSet.clear();
			fillSet(line);
			int numberOfAllFocusNodes = fillFocusNodesOfRequestedTypes(dataGraph);

			System.out.println();
			System.out.println();
			System.out.println(line);

			ArrayList<Long> targetNodes = new ArrayList<Long>();

			// find papers less than 5 degree
			for (Node node : dataGraph.getAllNodes()) {

				if (targetNodes.size() > 19) {
					break;
				}

				if (alreadyUsedPapers.contains(node.getId())) {
					continue;
				}
				if (allFocusNodesSet.contains(node.getId())) {
					continue;
				}
				if (!node.getLabels().iterator().next().name().equals("m17")) {
					continue;
				}
				if (node.getDegree(Direction.OUTGOING) > 6) {
					continue;
				}

				targetNodes.add(node.getId());

			}

			alreadyUsedPapers.addAll(targetNodes);

			while (targetNodes.size() > 0) {
				for (Integer focusNodeId : allFocusNodesSet) {
					if (targetNodes.size() > 0) {
						Long targetNodeId = targetNodes.remove(0);
						bwFocus.write(line + "#" + "+," + "2020-01-01 00:00:00," + focusNodeId + "," + targetNodeId
								+ "," + "N" + "\n");
						bwFocus.flush();
					} else
						break;
				}
			}
			bwFocus.write("\n");

			HashSet<String> deletedRels = new HashSet<String>();
			int delCnt = 0;
			int traverse = 0;
			boolean foundANewOne = true;
			while (delCnt < 20 && traverse < 20) {
				traverse++;
				for (Integer focusNodeId : allFocusNodesSet) {
					if (delCnt == 20)
						break;
					boolean foundForThis = false;
					for (Relationship rel1 : dataGraph.getNodeById(focusNodeId).getRelationships(Direction.OUTGOING)) {
						Node endNode = rel1.getEndNode();
						for (Relationship rel2 : endNode.getRelationships(Direction.OUTGOING)) {
							if (!deletedRels.contains(endNode.getId() + "," + rel2.getEndNode().getId())) {
								bwFTarget.write(line + "#" + "-," + "2030-01-01 00:00:00," + endNode.getId() + ","
										+ rel2.getEndNode().getId() + "," + "N" + "\n");

								deletedRels.add(endNode.getId() + "," + rel2.getEndNode().getId());
								bwFTarget.flush();
								delCnt++;
								foundForThis = true;
								break;
							}
						}
						if (foundForThis)
							break;
					}
				}
			}
			bwFTarget.write("\n");

		}

		br.close();
		bwFocus.close();
		bwFTarget.close();
		tx1.success();
		dataGraph.shutdown();

	}

	private void runGetRealStream() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// get all focus node ids
		File foutFocus = new File("focusStream.txt");
		FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		BufferedWriter bwFocus = new BufferedWriter(new OutputStreamWriter(fosFocus));

		File foutFTarget = new File("targetFocusStream.txt");
		FileOutputStream fosFTarget = new FileOutputStream(foutFTarget);
		BufferedWriter bwFTarget = new BufferedWriter(new OutputStreamWriter(fosFTarget));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			focusLabelSet.clear();
			allFocusNodesSet.clear();
			fillSet(line);
			fillFocusNodesOfRequestedTypes(dataGraph);

			System.out.println();
			System.out.println();
			System.out.println(line);

			HashSet<Long> targetOfFocusNodes = new HashSet<Long>();

			// first scan
			SnapshotSimulator snapshotSim1 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy");
			StreamEdge streamEdge1 = null;
			int index = 0;
			while (true) {
				streamEdge1 = snapshotSim1.getNextStreamEdge();
				index++;
				if (streamEdge1 == null)
					break;

				if (index > 120000)
					break;

				long srcNodeId = streamEdge1.getSourceNode();
				long destNodeId = streamEdge1.getDestinationNode();

				if (allFocusNodesSet.contains(srcNodeId)) {

					bwFocus.write(index + "#" + line + "#" + (streamEdge1.isAdded() ? "+," : "-,")
							+ streamEdge1.getTimeStamp() + "," + srcNodeId + "," + destNodeId + ","
							+ streamEdge1.getRelationshipType() + "\n");
					bwFocus.flush();

					targetOfFocusNodes.add(destNodeId);
				}

			}

			// adding target from init graph
			for (Integer focusNodeId : allFocusNodesSet) {
				for (Relationship rel : dataGraph.getNodeById(focusNodeId).getRelationships(Direction.OUTGOING)) {
					targetOfFocusNodes.add(rel.getEndNode().getId());
				}
			}

			// second scan
			SnapshotSimulator snapshotSim2 = new SnapshotSimulator(allDeltaEFilesPath, "yyyy");
			StreamEdge streamEdge2 = null;
			index = 0;
			while (true) {
				streamEdge2 = snapshotSim2.getNextStreamEdge();
				index++;
				if (streamEdge2 == null)
					break;

				if (index > 120000)
					break;

				long srcNodeId = streamEdge2.getSourceNode();
				long destNodeId = streamEdge2.getDestinationNode();

				if (targetOfFocusNodes.contains(srcNodeId)) {

					bwFTarget.write(index + "#" + line + "#" + (streamEdge2.isAdded() ? "+," : "-,")
							+ streamEdge2.getTimeStamp() + "," + srcNodeId + "," + destNodeId + ","
							+ streamEdge2.getRelationshipType() + "\n");
					bwFTarget.flush();
				}

			}
		}

		br.close();
		bwFocus.close();
		bwFTarget.close();
		tx1.success();
		dataGraph.shutdown();
	}

	private void fillSet(String line) throws Exception {

		String[] labelAndPropKeyVals = line.trim().split("\\|");
		ArrayList<PairStrings> propKeyValues = new ArrayList<PairStrings>();
		if (labelAndPropKeyVals.length == 1) {
			focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
		} else if (labelAndPropKeyVals.length > 1) {
			String[] keyValuePairs = labelAndPropKeyVals[1].split(",");
			for (int i = 0; i < keyValuePairs.length; i++) {
				String[] separatedKeyValue = keyValuePairs[i].split(":");
				propKeyValues.add(new PairStrings(separatedKeyValue[0], separatedKeyValue[1].replace("\"", "")));
			}

		}
		// Assumption: distinct labels
		focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);

	}

	private int fillFocusNodesOfRequestedTypes(GraphDatabaseService dataGraph2) throws Exception {

		int numberOfAllFocusNodes = 0;
		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			ArrayList<PairStrings> propVals = focusLabelPropValSet.get(focusLabel);
			for (Node node : dataGraph.getAllNodes()) {
				// boolean isCandidate = true;
				if (!node.hasLabel(Label.label(focusLabel))) {
					continue;
				}
				if (propVals.size() > 0) {
					for (PairStrings pairString : propVals) {

						if (node.hasProperty(pairString.key)) {
							if (DummyFunctions.isContain(node.getProperty(pairString.key).toString().toLowerCase(),
									pairString.value.toLowerCase())
									|| node.getProperty(pairString.key).toString().toLowerCase()
											.equals(pairString.value.toLowerCase())) {
								allNodesOfFocusType.get(focusLabel).add((int) node.getId());
								break;
							}
						}

					}
				} else {
					allNodesOfFocusType.get(focusLabel).add((int) node.getId());
				}

			}
		}

		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				throw new Exception("no items for \"" + key + "\"");
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();

			for (Integer focusNodeId : allNodesOfFocusType.get(key)) {
				allFocusNodesSet.add(focusNodeId);
			}

		}

		return numberOfAllFocusNodes;
	}

	private void getNonAffInsStream() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "1g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// get all focus node ids
		// File foutFocus = new File("nonAffInsertStream.txt");
		// FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		// BufferedWriter bwFocus = new BufferedWriter(new
		// OutputStreamWriter(fosFocus));

		File foutFTarget = new File("nonAffInsertStream.txt");
		FileOutputStream fosFTarget = new FileOutputStream(foutFTarget);
		BufferedWriter bwFTarget = new
				BufferedWriter(new OutputStreamWriter(fosFTarget));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		HashSet<String> acceptedLbls = new HashSet<String>();
		acceptedLbls.add("m0");
		acceptedLbls.add("m7");
		acceptedLbls.add("m18");
		acceptedLbls.add("m10");

		int cnt = 0;

		HashSet<String> insertedRels = new HashSet<String>();

		for (Node node1 : dataGraph.getAllNodes()) {
			for (Node node2 : dataGraph.getAllNodes()) {
				if (node1.getId() != node2.getId() && !insertedRels.contains(node1.getId() + "_" + node2.getId())) {
					String lblS = node1.getLabels().iterator().next().name().toString();
					String lblE = node2.getLabels().iterator().next().name().toString();
					if (acceptedLbls.contains(lblS) && acceptedLbls.contains(lblE)) {
						String rel = node1.getId() + "_" + node2.getId();
						insertedRels.add(rel);

						bwFTarget.write("+,2040-01-01 00:00:00," + node1.getId() + "," + node2.getId() + ",N");
						bwFTarget.newLine();
						cnt++;

						if (cnt > 10000) {
							break;
						}

					}
				}
			}
		}

		br.close();
		// bwFocus.close();
		bwFTarget.close();
		tx1.success();
		dataGraph.shutdown();

	}

	private void getNonAffStream() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "1g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// get all focus node ids
		// File foutFocus = new File("nonAffInsertStream.txt");
		// FileOutputStream fosFocus = new FileOutputStream(foutFocus);
		// BufferedWriter bwFocus = new BufferedWriter(new
		// OutputStreamWriter(fosFocus));

		File foutFTarget = new File("nonAffDeleteStream.txt");
		FileOutputStream fosFTarget = new FileOutputStream(foutFTarget);
		BufferedWriter bwFTarget = new BufferedWriter(new OutputStreamWriter(fosFTarget));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		HashSet<String> acceptedLbls = new HashSet<String>();
		acceptedLbls.add("m0");
		acceptedLbls.add("m7");
		acceptedLbls.add("m18");
		acceptedLbls.add("m10");

		int cnt = 0;
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (cnt > 500000) {
				break;
			}
			String lblS = rel.getStartNode().getLabels().iterator().next().name().toString();
			String lblE = rel.getEndNode().getLabels().iterator().next().name().toString();

			if (acceptedLbls.contains(lblS) && acceptedLbls.contains(lblE)) {
				System.out.println(cnt++);
				;
				bwFTarget.write(
						"-,2040-01-01 00:00:00," + rel.getStartNode().getId() + "," + rel.getEndNode().getId() + ",N");
				bwFTarget.newLine();
			}

		}

		br.close();
		// bwFocus.close();
		bwFTarget.close();
		tx1.success();
		dataGraph.shutdown();

	}

	class PairStrings {
		public String key;
		public String value;

		public PairStrings(String key, String value) {
			this.key = key;
			this.value = value;

		}

	}

}
