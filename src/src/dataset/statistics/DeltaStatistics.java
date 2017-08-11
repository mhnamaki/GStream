package src.dataset.statistics;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.dataset.dataSnapshot.SnapshotSimulator;
import src.dataset.dataSnapshot.StreamEdge;

public class DeltaStatistics {

	public static void main(String[] args) throws Exception {
		String dateFormat = "yyyy";
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citation/citation.graphdb";
		String deltaDirectory = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationSnapshot/";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraph")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-deltaDirectory")) {
				deltaDirectory = args[++i];
			} else if (args[i].equals("-dateFormat")) {
				dateFormat = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println("dataset: " + dataGraphPath);

		File readme = new File(dataGraphPath + "/DeltaInfos.txt");
		FileWriter readmeWriter = new FileWriter(readme);
		File readme2 = new File(dataGraphPath + "/NodeRelUpdateInfos.txt");
		FileWriter readmeWriter2 = new FileWriter(readme2);

		Transaction tx2 = dataGraph.beginTx();

		SnapshotSimulator snapshotSim = new SnapshotSimulator(deltaDirectory, dateFormat);

		Node srcNode = null;
		Node destNode = null;
		String srcLabel = null;
		String destLabel = null;
		Integer srcNodeId = null;
		Integer destNodeId = null;

		int addedEdges = 0;
		int removedEdges = 0;

		HashMap<String, Integer> relTypeFrequency = new HashMap<String, Integer>();

		for (Relationship rel : dataGraph.getAllRelationships()) {
			relTypeFrequency.putIfAbsent(rel.getType().name(), 0);
			relTypeFrequency.put(rel.getType().name(), relTypeFrequency.get(rel.getType().name()) + 1);
		}

		HashMap<String, Integer> updatesInEachTimestamps = new HashMap<String, Integer>();
		HashMap<String, Integer> updatesForEachRelType = new HashMap<String, Integer>();
		HashMap<String, Integer> updatesForEachNodeType = new HashMap<String, Integer>();
		HashMap<Integer, Integer> updateForEachNodeId = new HashMap<Integer, Integer>();

		HashMap<String, SumAndTheNumberOfThem> degreeForEachNodeType = new HashMap<String, SumAndTheNumberOfThem>();
		HashMap<Integer, HashSet<Integer>> updateIdOfEachNodeId = new HashMap<Integer, HashSet<Integer>>();
		HashMap<Integer, DoubleIntPair> closenessOfANodeIdUpdates = new HashMap<Integer, DoubleIntPair>();

		HashMap<Integer, HashSet<Integer>> sameDegreeNodeIds = new HashMap<Integer, HashSet<Integer>>();

		// DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		DateFormat df = new SimpleDateFormat(dateFormat);

		int updateId = 0;
		while (true) {
			StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

			updateId++;
			if (nextStreamEdge == null)
				break;

			if (nextStreamEdge.isAdded())
				addedEdges++;
			else
				removedEdges++;

			srcNodeId = (int) nextStreamEdge.getSourceNode();
			destNodeId = (int) nextStreamEdge.getDestinationNode();
			srcNode = dataGraph.getNodeById(srcNodeId);
			destNode = dataGraph.getNodeById(destNodeId);
			srcLabel = srcNode.getLabels().iterator().next().name();
			destLabel = destNode.getLabels().iterator().next().name();

			Date timestampDate = nextStreamEdge.getTimeStamp();
			String timestamp = timestampDate.toString();
			try {
				timestamp = df.format(timestampDate);
			} catch (Exception exc) {

			}

			String relType = nextStreamEdge.getRelationshipType();

			updatesInEachTimestamps.putIfAbsent(timestamp, 0);
			updatesInEachTimestamps.put(timestamp, updatesInEachTimestamps.get(timestamp) + 1);

			updateForEachNodeId.putIfAbsent(srcNodeId, 0);
			updateForEachNodeId.put(srcNodeId, updateForEachNodeId.get(srcNodeId) + 1);

			updateForEachNodeId.putIfAbsent(destNodeId, 0);
			updateForEachNodeId.put(destNodeId, updateForEachNodeId.get(destNodeId) + 1);

			updatesForEachRelType.putIfAbsent(relType, 0);
			updatesForEachRelType.put(relType, updatesForEachRelType.get(relType) + 1);

			updatesForEachNodeType.putIfAbsent(srcLabel, 0);
			updatesForEachNodeType.put(srcLabel, updatesForEachNodeType.get(srcLabel) + 1);

			updatesForEachNodeType.putIfAbsent(destLabel, 0);
			updatesForEachNodeType.put(destLabel, updatesForEachNodeType.get(destLabel) + 1);

			updateIdOfEachNodeId.putIfAbsent(srcNodeId, new HashSet<Integer>());
			updateIdOfEachNodeId.get(srcNodeId).add(updateId);

			updateIdOfEachNodeId.putIfAbsent(destNodeId, new HashSet<Integer>());
			updateIdOfEachNodeId.get(destNodeId).add(updateId);

		}

		for (Integer nodeId : updateIdOfEachNodeId.keySet()) {
			DoubleIntPair doubleIntPair = new DoubleIntPair();
			doubleIntPair.number = updateIdOfEachNodeId.get(nodeId).size();
			doubleIntPair.value = getStdDev(updateIdOfEachNodeId.get(nodeId));

			closenessOfANodeIdUpdates.put(nodeId, doubleIntPair);
		}

		for (Node node : dataGraph.getAllNodes()) {
			sameDegreeNodeIds.putIfAbsent(node.getDegree(), new HashSet<Integer>());
			sameDegreeNodeIds.get(node.getDegree()).add((int) node.getId());
		}

		for (Node node : dataGraph.getAllNodes()) {
			String lbl = node.getLabels().iterator().next().name();
			degreeForEachNodeType.putIfAbsent(lbl, new SumAndTheNumberOfThem());
			degreeForEachNodeType.get(lbl).numberOfThem++;
			degreeForEachNodeType.get(lbl).sum += node.getDegree();
		}

		readmeWriter.write("all updates:" + (addedEdges + removedEdges) + "\n");
		readmeWriter.write("addedEdges:" + (addedEdges) + "\n");
		readmeWriter.write("removedEdges:" + (removedEdges) + "\n");
		readmeWriter.write("\n");
		readmeWriter.write("\n");
		readmeWriter.flush();

		HashMap<Integer, SumAndTheNumberOfThem> updatesPerDegree = new HashMap<Integer, SumAndTheNumberOfThem>();
		for (Integer degree : sameDegreeNodeIds.keySet()) {
			updatesPerDegree.putIfAbsent(degree, new SumAndTheNumberOfThem());
			for (Integer nodeId : sameDegreeNodeIds.get(degree)) {
				if (updateForEachNodeId.containsKey(nodeId)) {
					updatesPerDegree.get(degree).numberOfThem++;
					updatesPerDegree.get(degree).sum += updateForEachNodeId.get(nodeId);
				}
			}
		}

		readmeWriter.write("updates per degree \n");
		readmeWriter.write("degree; total updates; nodes with this degree; avg updates; \n");
		for (Integer degree : updatesPerDegree.keySet()) {
			readmeWriter.write(degree + ";" + updatesPerDegree.get(degree).sum + ";"
					+ updatesPerDegree.get(degree).numberOfThem + ";"
					+ (updatesPerDegree.get(degree).sum / updatesPerDegree.get(degree).numberOfThem) + "\n");
		}
		readmeWriter.write("\n");
		readmeWriter.write("\n");
		readmeWriter.flush();

		readmeWriter.write("updates per relType \n");
		readmeWriter.write("relType;updates; frequency; \n");
		for (String relType : updatesForEachRelType.keySet()) {
			readmeWriter.write(
					relType + ";" + updatesForEachRelType.get(relType) + ";" + relTypeFrequency.get(relType) + "\n");
		}
		readmeWriter.write("\n");
		readmeWriter.write("\n");
		readmeWriter.flush();

		readmeWriter.write("updates per nodeType \n");
		readmeWriter.write("nodeType;total degree; nodes with this degree;avgDegree; updates \n");
		for (String nodeType : updatesForEachNodeType.keySet()) {
			readmeWriter
					.write(nodeType + ";" + degreeForEachNodeType.get(nodeType).sum + ";"
							+ degreeForEachNodeType.get(nodeType).numberOfThem + ";"
							+ (degreeForEachNodeType.get(nodeType).sum
									/ degreeForEachNodeType.get(nodeType).numberOfThem)
							+ ";" + updatesForEachNodeType.get(nodeType) + "\n");
		}
		readmeWriter.write("\n");
		readmeWriter.write("\n");
		readmeWriter.flush();

		readmeWriter.write("updates in each timestamps \n");
		readmeWriter.write("timestamp;updates; \n");
		for (String timestamp : updatesInEachTimestamps.keySet()) {
			readmeWriter.write(timestamp + ";" + updatesInEachTimestamps.get(timestamp) + "\n");
		}
		readmeWriter.write("\n");
		readmeWriter.write("\n");
		readmeWriter.flush();

		readmeWriter2.write("closeness Of A NodeId Updates \n");
		readmeWriter2.write("nodeId;degree;updates;std \n");
		for (Integer nodeId : closenessOfANodeIdUpdates.keySet()) {
			if (closenessOfANodeIdUpdates.get(nodeId).number > 1) {
				readmeWriter2.write(nodeId + ";" + dataGraph.getNodeById(nodeId).getDegree() + ";"
						+ closenessOfANodeIdUpdates.get(nodeId).number + ";"
						+ closenessOfANodeIdUpdates.get(nodeId).value + "\n");
			}
		}
		readmeWriter.write("\n");

		readmeWriter.flush();
		readmeWriter.close();

		readmeWriter2.flush();
		readmeWriter2.close();

		tx2.success();
		tx2.close();
		dataGraph.shutdown();

	}

	static double getMean(HashSet<Integer> data) {
		double sum = 0.0;
		for (double a : data)
			sum += a;
		return sum / data.size();
	}

	static double getVariance(HashSet<Integer> data) {
		double mean = getMean(data);
		double temp = 0;
		for (double a : data)
			temp += (a - mean) * (a - mean);
		return temp / data.size();
	}

	static double getStdDev(HashSet<Integer> data) {
		return Math.sqrt(getVariance(data));
	}
}

class SumAndTheNumberOfThem {
	double sum;
	int numberOfThem;
}

class DoubleIntPair {
	double value;
	int number;
}