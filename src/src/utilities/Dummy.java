package src.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import src.base.IPrefixTreeNodeData;
import src.finalAlgorithm.prefixTree.PrefixTreeOptBiSim;
import src.finalAlgorithm.prefixTree.PrefixTreeOptBiSim2;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;

public class Dummy {
	public static class DummyProperties {
		public static boolean softwareMode = false;
		public static boolean visualize = false;
		public static boolean debugMode = false;
		public static boolean incMode = false;
		public static boolean windowMode = false;
		public static int WINDOW_SIZE = 0;
		public static int NUMBER_OF_SNAPSHOTS = 1;
		public static int NUMBER_OF_ALL_FOCUS_NODES = 0;
		public static String SEPARATOR_LABEL_AND_RELTYPE = "#";
		public static boolean bigDataTestMode = false;

		public static enum Direction {
			INCOMING, OUTGOING
		}

		public static enum SnapOrStat {
			Snap, Stat
		}

		public static enum PrefixTreeMode {
			BATCH, UPDATE
		};

		public static enum DatasetName {
			PANAMA, CITATION, IDS, N10000
		}

		public static DatasetName selectedDataset = DatasetName.PANAMA;
		public static Integer startingIncYear = 1990;
		public static Integer reportingStep = 1;

	}

	public static class DummyFunctions {
		public static void sleepAndWakeUp(int milisecondsOfSleep) throws Exception {
			System.out.println("sleeping..." + new Date());
			System.gc();
			System.runFinalization();
			Thread.sleep(milisecondsOfSleep);
			System.gc();
			System.runFinalization();
			Thread.sleep(milisecondsOfSleep);
			System.out.println("waking up..." + new Date());
		}

		public static boolean deleteCompletely(Path rootPath) {
			try {
				Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						// System.out.println("delete file: " +
						// file.toString());
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						// System.out.println("delete dir: " + dir.toString());
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		// public static void printIfDebugLn() {
		// printIfDebugLn("");
		// }
		//
		// public static void printIfDebugLn(Object line) {
		// if (DummyProperties.debugMode) {
		// System.out.println(line);
		// }
		// }

		public static int getNumberOfAllNodes(GraphDatabaseService dataGraph) {
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {
				numberOfAllNodes++;
			}
			return numberOfAllNodes;
		}

		public static int getNumberOfAllRels(GraphDatabaseService dataGraph) {
			int numberOfAllRelationships = 0;
			for (Relationship rel : dataGraph.getAllRelationships()) {
				numberOfAllRelationships++;
			}
			return numberOfAllRelationships;
		}

		public static HashSet<String> getDifferentLabels(GraphDatabaseService dataGraph) {
			HashSet<String> differentLabels = new HashSet<String>();
			for (Label label : dataGraph.getAllLabels()) {
				differentLabels.add(label.name().intern());
			}
			return differentLabels;
		}

		public static double getAvgOutDegrees(GraphDatabaseService dataGraph) {
			double allDegrees = 0.0d;
			double avgDegrees = 0.0d;
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {

				// if(numberOfAllNodes==7){
				// System.out.println(numberOfAllNodes);
				allDegrees += node.getDegree(Direction.OUTGOING);
				numberOfAllNodes++;
			}
			avgDegrees = allDegrees / numberOfAllNodes;
			return avgDegrees;
		}

		public static double getAvgInDegrees(GraphDatabaseService dataGraph) {
			double allDegrees = 0.0d;
			double avgDegrees = 0.0d;
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {

				// if(numberOfAllNodes==7){
				// System.out.println(numberOfAllNodes);
				allDegrees += node.getDegree(Direction.INCOMING);
				numberOfAllNodes++;
			}
			avgDegrees = allDegrees / numberOfAllNodes;
			return avgDegrees;
		}

		public static double getAvgDegrees(GraphDatabaseService dataGraph) {
			double allDegrees = 0.0d;
			double avgDegrees = 0.0d;
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {

				// if(numberOfAllNodes==7){
				// System.out.println(numberOfAllNodes);
				allDegrees += node.getDegree(Direction.BOTH);
				numberOfAllNodes++;
			}
			avgDegrees = allDegrees / numberOfAllNodes;
			return avgDegrees;
		}

		public static double getAvgOutDegreeOfFocusNodes(GraphDatabaseService dataGraph, HashSet<Integer> allFocusNodes,
				int numberOfAllFocusNodes) throws Exception {
			double avgOutDegreeOfFocusNodes = 0;
			for (Integer nodeId : allFocusNodes) {
				avgOutDegreeOfFocusNodes += dataGraph.getNodeById(nodeId).getDegree(Direction.OUTGOING);
			}

			// if (avgOutDegreeOfFocusNodes == 0) {
			// System.err.println("avgOutDegreeOfFocusNodes is zero!");
			// return 0;
			// }

			return avgOutDegreeOfFocusNodes / numberOfAllFocusNodes;
		}

		public static double getAverageOfDoubleArray(ArrayList<Double> arrayOfDoubles) {
			if (arrayOfDoubles.size() == 0) {
				return 0;
			}

			double sum = 0;
			for (double val : arrayOfDoubles) {
				sum += val;
			}
			return sum / (arrayOfDoubles.size());
		}

		public static double getTotalSumOfDoubleArray(ArrayList<Double> arrayOfDoubles) {
			double sum = 0;
			for (double val : arrayOfDoubles) {
				sum += val;
			}
			return sum;
		}

		public static Path copyG0andGetItsNewPath(String dataGraphPath) throws Exception {

			System.out.println("copying is started..." + new Date());
			String parentDir = dataGraphPath.substring(0, dataGraphPath.lastIndexOf("/"));

			Path sourcePath = Paths.get(dataGraphPath);
			Path destinationPath = Paths.get(parentDir + "/target.db");

			if (Files.exists(destinationPath)) {
				Dummy.DummyFunctions.deleteCompletely(destinationPath);
			}

			Files.walkFileTree(sourcePath,
					new CopyDirVisitor(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING));
			System.out.println("copying is finished..." + new Date());
			return destinationPath;

		}

		public static void registerShutdownHook(final GraphDatabaseService dataGraph) {
			// Registers a shutdown hook for the Neo4j instance so that it
			// shuts down nicely when the VM exits (even if you "Ctrl-C" the
			// running application).
			// Runtime.getRuntime().addShutdownHook(new Thread() {
			// @Override
			// public void run() {
			// dataGraph.shutdown();
			// }
			// });

		}

		public static HashSet<String> getDifferentRelType(GraphDatabaseService dataGraph) {

			HashSet<String> relTypes = new HashSet<String>();
			for (RelationshipType relType : dataGraph.getAllRelationshipTypes()) {
				relTypes.add(relType.name().intern());
			}
			return relTypes;
		}

		public static File[] getFilesInTheDirfinder(String dirName) {
			File dir = new File(dirName);

			File[] files = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".txt") || filename.startsWith("Delta");
				}
			});

			if (files != null && files.length > 1)
				Arrays.sort(files);

			for (int i = 0; i < files.length; i++) {
				System.out.println("catched file " + i + "; " + files[i].getName());
			}
			return files;

		}

		public static boolean isContain(String source, String subItem) {
			// String pattern = "\\b" + subItem + "\\b";
			// Pattern p = Pattern.compile(pattern);
			// Matcher m = p.matcher(source);
			// return m.find();
			StringTokenizer st = new StringTokenizer(source);
			while (st.hasMoreTokens()) {
				if (st.nextToken().equals(subItem)) {
					return true;
				}
			}
			return false;
		}

		public static void saveTheMiddleResults(GraphDatabaseService dataGraph, int snapshot,
				MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns,
				JSONArray topkInSnapshots, int k) {

			if (!DummyProperties.softwareMode)
				return;

			MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> tempTopk = MinMaxPriorityQueue
					.orderedBy(new SupportComparator()).maximumSize(k).create();
			Iterator<PrefixTreeNode<IPrefixTreeNodeData>> topkInSnapshot = topKFrequentPatterns.iterator();
			while (topkInSnapshot.hasNext()) {
				PrefixTreeNode<IPrefixTreeNodeData> ptNode = topkInSnapshot.next();
				tempTopk.add(ptNode);
			}

			// top-k info
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("snapshot", snapshot);

			JSONArray patternInfos = new JSONArray();
			while (!tempTopk.isEmpty()) {
				PrefixTreeNode<IPrefixTreeNodeData> ptNode = tempTopk.poll();

				JSONObject patternInfo = new JSONObject();
				patternInfo.put("patternID", ptNode.getData().getPatternPrefixTreeNodeIndex());

				HashMap<PatternNode, Integer> patternNodesIndexMap = new HashMap<PatternNode, Integer>();
				int patternNodeIndex = 0;
				JSONArray nodes = new JSONArray();
				for (PatternNode patternNode : ptNode.getData().getPatternGraph().vertexSet()) {
					patternNodesIndexMap.put(patternNode, patternNodeIndex++);
					// ptNode.getData().
					JSONObject nodeInfo = new JSONObject();
					nodeInfo.put("name", patternNode.getType());

					String description = "";
					if (ptNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
							.size() == 1) {
						Integer nodeId = ptNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
								.get(patternNode).iterator().next();
						description = dataGraph.getNodeById(nodeId).getAllProperties().toString().replaceAll("\"", " ")
								.replaceAll("\'", " ");

					}
					nodeInfo.put("description", description);

					if (ptNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
							.size() > 0) {
						JSONArray matches = new JSONArray();
						int cnt = 0;
						for (Integer nodeId : ptNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
								.get(patternNode)) {
							cnt++;
							matches.add(dataGraph.getNodeById(nodeId).getAllProperties().toString()
									.replaceAll("\"", " ").replaceAll("\'", " "));
							if (cnt > 10)
								break;
						}
						nodeInfo.put("matches", matches);
					}
					nodes.add(nodeInfo);
				}
				patternInfo.put("nodes", nodes);

				JSONArray edges = new JSONArray();
				for (DefaultLabeledEdge e : ptNode.getData().getPatternGraph().edgeSet()) {
					JSONObject edgeInfo = new JSONObject();
					edgeInfo.put("source",
							patternNodesIndexMap.get(ptNode.getData().getPatternGraph().getEdgeSource(e)));
					edgeInfo.put("target",
							patternNodesIndexMap.get(ptNode.getData().getPatternGraph().getEdgeTarget(e)));
					edgeInfo.put("description", e.getType());
					edges.add(edgeInfo);
				}
				patternInfo.put("edges", edges);
				patternInfos.add(patternInfo);
			}
			jsonObj.put("patternInfos", patternInfos);

			topkInSnapshots.add(jsonObj);

		}

		public static void printTopKInfo(JSONArray topkInSnapshots) {
			try (FileWriter file = new FileWriter("topKInSnapshots.json")) {

				file.write(topkInSnapshots.toJSONString());
				file.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public static void saveTrendInfo(GraphDatabaseService dataGraph, int snapshot,
				MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns,
				HashMap<Integer, HashSet<Integer>> topksOfAllSnapshots) {

			if (!DummyProperties.softwareMode)
				return;

			// int index = topksOfAllSnapshots.size();
			// topksOfAllSnapshots.add();
			topksOfAllSnapshots.putIfAbsent(snapshot, new HashSet<Integer>());
			Iterator<PrefixTreeNode<IPrefixTreeNodeData>> topkInSnapshot = topKFrequentPatterns.iterator();
			while (topkInSnapshot.hasNext()) {
				PrefixTreeNode<IPrefixTreeNodeData> ptNode = topkInSnapshot.next();
				topksOfAllSnapshots.get(snapshot).add(ptNode.getData().getPatternPrefixTreeNodeIndex());
			}

		}

		public static void printTrendInfo(HashMap<Integer, HashSet<Integer>> topksOfAllSnapshots,
				PrefixTreeOptBiSim2 prefixTree, int numberOfSnapshots, int startingWindow) {
			if (!DummyProperties.softwareMode)
				return;

			// trend info

			JSONArray trends = new JSONArray();
			JSONArray header = new JSONArray();
			header.add("Snapshot");
			// header.add("Total");
			ArrayList<Integer> orderOfPatternsInHeader = new ArrayList<Integer>();
			HashSet<Integer> visitedPatterns = new HashSet<Integer>();

			for (Integer snapshot : topksOfAllSnapshots.keySet()) {
				for (Integer patternId : topksOfAllSnapshots.get(snapshot)) {
					if (!visitedPatterns.contains(patternId)) {
						header.add("P" + patternId);
						orderOfPatternsInHeader.add(patternId);
						visitedPatterns.add(patternId);
					}
				}
			}
			trends.add(header);

			for (Integer snapshot : topksOfAllSnapshots.keySet()) {
				JSONArray trendInASnapshot = new JSONArray();
				trendInASnapshot.add(snapshot);
				// double total = 0d;
				// for (Integer patternId : topksOfAllSnapshots.get(snapshot)) {
				// total +=
				// prefixTree.prefixTreeNodeIndex.get(patternId).getData().getSupportFrequencies()[snapshot];
				// }
				// trendInASnapshot.add(total);

				for (Integer patternId : orderOfPatternsInHeader) {
					trendInASnapshot.add(
							prefixTree.prefixTreeNodeIndex.get(patternId).getData().getSupportFrequency(snapshot - 1));
				}
				trends.add(trendInASnapshot);
			}

			try (FileWriter file = new FileWriter("trendInSnapshots.json")) {
				file.write(trends.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// supportOfEachPatternInEachSnapshot.clear();
		}

		// numberOfAllPatterns,
		// prefixTree.getNumberOfComputeSupport(),addedEdges, removedEdges,
		// numberOfSupportComputationsG0
		public static void printPerformanceInfo(int numberOfAllPatterns, int incPatterns, int incNumOfVerification,
				int addedEdges, int removedEdges, int g0NumOfVerifications, int allEdgesUpdates,
				HashMap<Integer, Double> processingTimeOfEdges) {

			if (!DummyProperties.softwareMode)
				return;

			JSONObject jsonPerformance = new JSONObject();
			jsonPerformance.put("allPatterns", numberOfAllPatterns);
			jsonPerformance.put("incPatterns", incPatterns);
			jsonPerformance.put("incNumOfVerifications", incNumOfVerification);
			jsonPerformance.put("addedEdges", addedEdges);
			jsonPerformance.put("removedEdges", removedEdges);
			jsonPerformance.put("g0NumOfVerifications", g0NumOfVerifications);
			jsonPerformance.put("allEdgesUpdates", allEdgesUpdates);
			jsonPerformance.put("memoryUsage",
					(Math.min(Math.ceil(0.000006 * numberOfAllPatterns * allEdgesUpdates), 6452)));

			JSONObject jsonRunningTime = new JSONObject();
			for (Integer numberOfProcessedEdges : processingTimeOfEdges.keySet()) {
				jsonRunningTime.put(numberOfProcessedEdges, processingTimeOfEdges.get(numberOfProcessedEdges));
			}

			jsonPerformance.put("performance", jsonRunningTime);

			try (FileWriter file = new FileWriter("performance.json")) {
				file.write(jsonPerformance.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// processingTimeOfEdges.clear();
		}

		public static void printFinalJson() throws Exception {
			JSONParser parser = new JSONParser();
			Object topk = parser.parse(new FileReader("topKInSnapshots.json"));
			Object trend = parser.parse(new FileReader("trendInSnapshots.json"));
			Object perf = parser.parse(new FileReader("performance.json"));

			JSONObject theWhole = new JSONObject();
			theWhole.put("topk", topk);
			theWhole.put("trend", trend);
			theWhole.put("perf", perf);

			try (FileWriter file = new FileWriter("theWhole.json")) {
				file.write(theWhole.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	
}
