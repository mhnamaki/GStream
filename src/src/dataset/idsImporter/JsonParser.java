package src.dataset.idsImporter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JsonParser {

	public String jsonFilesDirectoryPath = "/Users/mnamaki/Documents/workspace/wsu.eecs.mlkd.KGQuery/RunnableJarsForCluster/jsons/temp";
	public String neo4jPath = "/Users/mnamaki/Documents/Education/PhD/Fall2015/BigData/Neo4j/neo4j-community-2.3.1";
	public String futureDBPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/IDSAttack/idsAttacks.db";

	public String Alert_NODE_Path = "Alert_NODE.csv";
	public String Application_NODE_Path = "Application_NODE.csv";
	public String Host_NODE_Path = "Host_NODE.csv";
	public String Log_NODE_Path = "Log_NODE.csv";
	public String Protocol_NODE_Path = "Protocol_NODE.csv";
	public String Relationships_Path = "relationships.csv";

	final public int Alert_NODE = 0;

	final public int Host_NODE = 1;

	final public int Application_NODE = 2;

	final public int Protocol_NODE = 3; // Note that HOST_NODE denotes log
										// sources
	final public int Log_NODE = 4;

	HashMap<Long, HashSet<Long>> idsRelationships = new HashMap<Long, HashSet<Long>>();
	ArrayList<IDSNode> idsNodes = new ArrayList<IDSNode>();

	HashMap<String, Long> nodeIdOfHostName = new HashMap<String, Long>();
	HashMap<String, Long> nodeIdOfApplication = new HashMap<String, Long>();
	HashMap<String, Long> nodeIdOfProtocol = new HashMap<String, Long>();

	HashSet<Long> logNodeIds = new HashSet<Long>();
	HashSet<Long> alertNodeIds = new HashSet<Long>();
	HashSet<Long> addedNodeIds = new HashSet<Long>();

	public JsonParser(String jsonFilesDirectoryPath2, String neo4jPath2, String futureDBPath2) {
		this.jsonFilesDirectoryPath = jsonFilesDirectoryPath2;
		this.neo4jPath = neo4jPath2;
		this.futureDBPath = futureDBPath2;
	}

	public static void main(String[] args) throws Exception {
		try {
			String jsonFilesDirectoryPath = null;
			String neo4jPath = null;
			String futureDBPath = null;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-jsonFilesDirectoryPath")) {
					jsonFilesDirectoryPath = args[++i];
				} else if (args[i].equals("-neo4jPath")) {
					neo4jPath = args[++i];
				} else if (args[i].equals("-futureDBPath")) {
					futureDBPath = args[++i];
				}
			}
			if (futureDBPath == null || jsonFilesDirectoryPath == null) {
				throw new Exception("args! jsonFilesDirectoryPath futureDBPath");
			}
			JsonParser jp = new JsonParser(jsonFilesDirectoryPath, neo4jPath, futureDBPath);

			System.out.println("readJsonFiles");
			jp.readJsonFiles();

			System.out.println("writeNodes");
			jp.writeNodes();

			System.out.println("writeRelationships");
			jp.writeRelationships();

			if (neo4jPath != null) {
				System.out.println("importToNeo4j");
				jp.importToNeo4j();
			}

		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private void importToNeo4j() throws Exception {

		System.out.println("CSV files read; Building the Neo4j database");
		String command = neo4jPath + "/bin/neo4j-import --id-type INTEGER --into " + futureDBPath + " --nodes:Alert "
				+ Alert_NODE_Path + " --nodes:Application " + Application_NODE_Path + " --nodes:Host " + Host_NODE_Path
				+ " --nodes:Log " + Log_NODE_Path + " --nodes:Protocol " + Protocol_NODE_Path
				+ " --relationships:NOTYPE " + Relationships_Path;

		System.out.println(command);

		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		int exitVal = p.exitValue();

		System.out.println("proc.exitValue(): " + exitVal);

		if (exitVal == 0)
			System.out.println("program is finished properly!");
		else {
			String line;
			System.out.println("ERROR: Neo4j messed up");
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
			in.close();
		}

	}

	private void writeRelationships() throws Exception {
		File relationshipFile = new File("relationships.csv");
		FileOutputStream fos = new FileOutputStream(relationshipFile);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		bw.write(":START_ID,:END_ID\n");

		for (Long sourceNodeId : idsRelationships.keySet()) {
			for (Long destNodeId : idsRelationships.get(sourceNodeId)) {
				bw.write(sourceNodeId + "," + destNodeId);
				bw.newLine();
			}
		}
		bw.close();

	}

	private void writeNodes() throws Exception {

		int numberOfAllNodeTypes = 5;

		File[] nodesFiles = new File[numberOfAllNodeTypes];
		nodesFiles[Alert_NODE] = new File("Alert_NODE.csv");
		nodesFiles[Host_NODE] = new File("Host_NODE.csv");
		nodesFiles[Application_NODE] = new File("Application_NODE.csv");
		nodesFiles[Protocol_NODE] = new File("Protocol_NODE.csv");
		nodesFiles[Log_NODE] = new File("Log_NODE.csv");

		FileOutputStream[] foses = new FileOutputStream[numberOfAllNodeTypes];
		for (int i = 0; i < numberOfAllNodeTypes; i++) {
			foses[i] = new FileOutputStream(nodesFiles[i]);
		}

		BufferedWriter[] bws = new BufferedWriter[numberOfAllNodeTypes];
		for (int i = 0; i < numberOfAllNodeTypes; i++) {
			bws[i] = new BufferedWriter(new OutputStreamWriter(foses[i]));
		}

		// writing headers:
		bws[Alert_NODE].write("nodeId:ID,startTime,endTime,tag \n");
		bws[Host_NODE].write("nodeId:ID,hostName \n");
		bws[Application_NODE].write("nodeId:ID,appName \n");
		bws[Protocol_NODE].write("nodeId:ID,protocolName \n");
		bws[Log_NODE].write("nodeId:ID,protocol,tos,ipttl,packetlen,startTime \n");

		// writing inside of files:
		for (int i = 0; i < idsNodes.size(); i++) {
			IDSNode idsNode = idsNodes.get(i);
			int nodeType = Math.toIntExact(idsNode.nodeType);

			bws[nodeType].write(idsNode.nodeId + ",");

			switch (nodeType) {
			case Alert_NODE:
				bws[nodeType].write(idsNode.getPropertyByKey("impactStartTime") + ",");
				bws[nodeType].write(idsNode.getPropertyByKey("impactEndTime") + ",");
				bws[nodeType].write(idsNode.getPropertyByKey("tag") + "\n");
				break;
			case Host_NODE:
				bws[nodeType].write(idsNode.getPropertyByKey("hostName") + "\n");
				break;
			case Application_NODE:
				bws[nodeType].write(idsNode.getPropertyByKey("appName") + "\n");
				break;
			case Protocol_NODE:
				bws[nodeType].write(idsNode.getPropertyByKey("protocolName") + "\n");
				break;
			case Log_NODE:

				bws[nodeType].write(idsNode.getPropertyByKey("protocol") + ",");
				bws[nodeType].write(idsNode.getPropertyByKey("tos") + ",");
				bws[nodeType].write(idsNode.getPropertyByKey("ipttl") + ",");
				bws[nodeType].write(idsNode.getPropertyByKey("packetlen") + ",");
				bws[nodeType].write(idsNode.getPropertyByKey("timestamp") + "\n");

				break;

			default:
				System.err.println("not identified nodeTypes");
				break;
			}
		}

		for (int i = 0; i < numberOfAllNodeTypes; i++) {
			bws[i].close();
		}

	}

	@SuppressWarnings("unchecked")
	public void readJsonFiles() {
		JSONParser parser = new JSONParser();
		try {

			int cnt = 0;
			int maxIdAtTheEndOfThePrevDay = 0;
			for (File file : getFilesInTheDirfinder(jsonFilesDirectoryPath)) {
				System.out.println("catched file " + file.getName());
				Object totalArr = parser.parse(new FileReader(file.getAbsolutePath()));
				JSONArray jsonObjects = (JSONArray) totalArr;

				HashMap<Long, Long> originalIdOfThisDayId = new HashMap<Long, Long>();

				Iterator<JSONObject> iteratorAll = jsonObjects.iterator();
				while (iteratorAll.hasNext()) {

					JSONObject jsonObject = (JSONObject) iteratorAll.next();
					Long nodeID = (Long) jsonObject.get("nodeID");
					nodeID += maxIdAtTheEndOfThePrevDay;
					Long nodeType = (Long) jsonObject.get("nodeType");

					// if (!nodeIdsSet.contains(nodeID)) {
					// nodeIdsSet.add(nodeID);
					// } else {
					// System.err.println(nodeID);
					// }

					String hostName;

					String appName;

					String protocolName;

					Long impactStartTime;
					String tag;
					Long impactEndTime;

					String protocol;
					String tos;
					String ipttl;
					String packetlen;
					Long timestamp;

					boolean duplicatedId = false;
					IDSNode idsNode = new IDSNode(nodeID, nodeType);

					int nodeTypeInt = Math.toIntExact(nodeType);
					switch (nodeTypeInt) {
					case Alert_NODE:
						if (alertNodeIds.contains(nodeID)) {
							System.err.println(file.getName() + " -> ALERT: " + nodeID);
						} else {
							alertNodeIds.add(nodeID);
						}
						impactStartTime = (Long) jsonObject.get("impactStartTime");
						impactEndTime = (Long) jsonObject.get("impactEndTime");
						tag = (String) jsonObject.get("tag");

						idsNode.addProperty("impactStartTime", impactStartTime.toString());
						idsNode.addProperty("impactEndTime", impactEndTime.toString());
						idsNode.addProperty("tag", tag);

						// if (nodeID == 90692) {
						// System.out.println(file.getName());
						// System.out.println("nodeID==90692 impactStartTime:" +
						// impactStartTime + " impactEndTime:"
						// + impactEndTime + " tag:" + tag);
						// System.out.println();
						// }
						break;

					case Host_NODE:
						hostName = (String) jsonObject.get("hostName");
						if (!nodeIdOfHostName.containsKey(hostName)) {
							nodeIdOfHostName.put(hostName, nodeID);
							idsNode.addProperty("hostName", hostName);
						} else {
							originalIdOfThisDayId.put(nodeID, nodeIdOfHostName.get(hostName));
							nodeID = nodeIdOfHostName.get(hostName);
							duplicatedId = true;
						}
						break;

					case Application_NODE:
						appName = (String) jsonObject.get("appName");
						if (!nodeIdOfApplication.containsKey(appName)) {
							nodeIdOfApplication.put(appName, nodeID);
							idsNode.addProperty("appName", appName);
						} else {
							originalIdOfThisDayId.put(nodeID, nodeIdOfApplication.get(appName));
							nodeID = nodeIdOfApplication.get(appName);
							duplicatedId = true;
						}
						break;

					case Protocol_NODE:
						protocolName = (String) jsonObject.get("protocolName");
						if (!nodeIdOfProtocol.containsKey(protocolName)) {
							nodeIdOfProtocol.put(protocolName, nodeID);
							idsNode.addProperty("protocolName", protocolName);
						} else {
							originalIdOfThisDayId.put(nodeID, nodeIdOfProtocol.get(protocolName));
							nodeID = nodeIdOfProtocol.get(protocolName);
							duplicatedId = true;
						}
						break;

					case Log_NODE:
						if (logNodeIds.contains(nodeID)) {
							System.err.println(file.getName() + " -> LOG: " + nodeID);
						} else {
							logNodeIds.add(nodeID);
						}
						protocol = (String) jsonObject.get("protocol");
						tos = (String) jsonObject.get("tos");
						ipttl = (String) jsonObject.get("ipttl");
						packetlen = (String) jsonObject.get("packetlen");
						timestamp = (Long) jsonObject.get("timestamp");

						idsNode.addProperty("protocol", protocol);
						idsNode.addProperty("tos", tos);
						idsNode.addProperty("ipttl", ipttl);
						idsNode.addProperty("packetlen", packetlen);
						idsNode.addProperty("timestamp", timestamp.toString());
						break;

					default:
						System.err.println("not identified nodeTypes");
						break;
					}

					if (!duplicatedId) {

						if (addedNodeIds.contains(nodeID)) {
							System.err.println("repeating node: " + nodeID);
						} else {
							addedNodeIds.add(nodeID);
						}

						idsNodes.add(idsNode);

					}

					JSONArray graphEdges = (JSONArray) jsonObject.get("graphEdge");
					if (graphEdges != null) {

						Iterator<JSONObject> iterator = graphEdges.iterator();
						while (iterator.hasNext()) {
							long temp = 0;
							idsRelationships.putIfAbsent(nodeID, new HashSet<Long>());
							JSONObject targetObj = (JSONObject) iterator.next();
							long destId = (Long) targetObj.get("target");
							temp = destId;
							if (originalIdOfThisDayId.containsKey(destId + maxIdAtTheEndOfThePrevDay)) {
								destId = originalIdOfThisDayId.get(destId + maxIdAtTheEndOfThePrevDay);
							} else {
								destId += maxIdAtTheEndOfThePrevDay;
							}

							idsRelationships.get(nodeID).add(destId);
						}
					}

					if (nodeID == null) {
						System.out.println();
					}
					cnt++;
				}
				maxIdAtTheEndOfThePrevDay = cnt;

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private File[] getFilesInTheDirfinder(String dirName) {
		File dir = new File(dirName);

		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".json");
			}
		});

		if (files != null && files.length > 1)
			Arrays.sort(files);

		// for (int i = 0; i < files.length; i++) {
		//
		// }
		return files;

	}

}

class IDSNode {
	Long nodeId;
	Long nodeType;
	HashMap<String, String> properties = new HashMap<String, String>();

	public IDSNode(Long nodeId, Long nodeType) {
		this.nodeId = nodeId;
		this.nodeType = nodeType;
	}

	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

	public String getPropertyByKey(String key) {
		return properties.get(key);
	}

}

class IndexAndNodeIdPair {
	Integer indexInArr;
	Long nodeId;

	public IndexAndNodeIdPair(Integer indexInArr, Long nodeId) {
		this.indexInArr = indexInArr;
		this.nodeId = nodeId;
	}
}