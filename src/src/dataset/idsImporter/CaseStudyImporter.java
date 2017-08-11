package src.dataset.idsImporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class CaseStudyImporter {

	final static int SrcIPaddressIndex = 0;
	final static int SrcPIndex = 1;
	final static int AppIndex = 2;
	final static int ProtocolIndex = 3;
	final static int DstPIndex = 4;
	final static int DstIPaddressIndex = 5;
	final static int StartTime = 6;
	final static int EndTime = 7;
	final static int AttackOrNot = 8;

	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Summer2017/GStreamICDE18/IDSDB/ids17.db";
	private static int numberOfCreatedNodes = 0;
	private static int numberOfCreatedRels = 0;
	private static String csvFilePath = "/Users/mnamaki/Documents/Education/PhD/Summer2017/GStreamICDE18/IDS/mergedFlows.csv";

	public static void main(String[] args) throws Exception {

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		FileInputStream fis = new FileInputStream(csvFilePath);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		HashMap<String, IPNodeInfo> nodeInfoOfAnIP = new HashMap<String, IPNodeInfo>();

		Transaction tx1 = dataGraph.beginTx();

		String line = null;
		while ((line = br.readLine()) != null) {
			String[] splittedValues = line.split(",");

			String srcIPAddress = splittedValues[SrcIPaddressIndex];
			Integer srcPortNumber = Integer.parseInt(splittedValues[SrcPIndex]);

			createIPAndPort(dataGraph, nodeInfoOfAnIP, srcIPAddress, srcPortNumber, "out_port");

			Node srcPortNode = dataGraph
					.getNodeById(nodeInfoOfAnIP.get(srcIPAddress).portNodeIdOfPort.get(srcPortNumber));

			String destIPAddress = splittedValues[DstIPaddressIndex];
			Integer destPortNumber = Integer.parseInt(splittedValues[DstPIndex]);

			createIPAndPort(dataGraph, nodeInfoOfAnIP, destIPAddress, destPortNumber, "in_port");

			Node destPortNode = dataGraph
					.getNodeById(nodeInfoOfAnIP.get(destIPAddress).portNodeIdOfPort.get(destPortNumber));

			// Relationship communicationRel = getExistedRel(dataGraph,
			// srcPortNode, destPortNode);

			// if (communicationRel == null) {

			Relationship communicationRel = srcPortNode.createRelationshipTo(destPortNode,
					RelationshipType.withName(splittedValues[AppIndex] + "/" + splittedValues[ProtocolIndex]));

			communicationRel.setProperty("startTime", splittedValues[StartTime]);

			long endDateTimeLong = formatter.parse(splittedValues[EndTime]).getTime() + 300001;

			Date endDate = new Date(endDateTimeLong);
			communicationRel.setProperty("endTime", formatter.format(endDate));
			communicationRel.setProperty("status", splittedValues[AttackOrNot]);

			// } else {
			//
			// Date oldStartDateTime =
			// formatter.parse(communicationRel.getProperty("startTime").toString());
			// Date oldEndDateTime =
			// formatter.parse(communicationRel.getProperty("endTime").toString());
			//
			// Date newStartDateTime =
			// formatter.parse(splittedValues[StartTime]);
			// Date newEndDateTime = formatter.parse(splittedValues[EndTime]);
			//
			// if (oldStartDateTime.compareTo(newStartDateTime) > 0) {
			// communicationRel.setProperty("startTime",
			// splittedValues[StartTime]);
			// }
			//
			// if (oldEndDateTime.compareTo(newEndDateTime) < 0) {
			// communicationRel.setProperty("endTime", splittedValues[EndTime]);
			// }
			//
			// }

			numberOfCreatedRels++;

		}

		tx1.success();
		tx1.close();

		br.close();

		System.out.println("numberOfCreatedNodes: " + numberOfCreatedNodes);
		System.out.println("numberOfCreatedRels: " + numberOfCreatedRels);

		dataGraph.shutdown();

	}

	private static Relationship getExistedRel(GraphDatabaseService dataGraph, Node srcPortNode, Node destPortNode) {
		for (Relationship rel : srcPortNode.getRelationships()) {
			if (rel.getOtherNode(srcPortNode).getId() == destPortNode.getId()) {
				return rel;
			}
		}
		return null;
	}

	private static void createIPAndPort(GraphDatabaseService dataGraph, HashMap<String, IPNodeInfo> nodeInfoOfAnIP,
			String ipAddress, Integer portNumber, String inOutPort) {

		Node ipNode = null;
		Node portNode = null;
		if (nodeInfoOfAnIP.containsKey(ipAddress)) {
			ipNode = dataGraph.getNodeById(nodeInfoOfAnIP.get(ipAddress).ipNodeId);

		} else {
			// create an ip node
			ipNode = dataGraph.createNode();
			numberOfCreatedNodes++;

			if (ipAddress.equals("192.168.5.122")) { // webserver
				ipNode.addLabel(Label.label(ipAddress));
			} else {
				ipNode.addLabel(Label.label("IP"));
			}

			ipNode.setProperty("ip_" + 0, ipAddress);

			String[] ipSections = ipAddress.split("\\.");
			for (int i = 1; i < 4; i++) {
				String newProp = "";
				for (int j = 1; j <= 4; j++) {
					if (j <= i) {
						newProp += ipSections[j - 1];
						if (j < 4) {
							newProp += ".";
						}
					} else {
						newProp += "*";
						if (j < 4) {
							newProp += ".";
						}
					}
				}
				ipNode.setProperty("ip_" + i, newProp);
			}

			nodeInfoOfAnIP.put(ipAddress, new IPNodeInfo(ipNode.getId()));

		}

		ipNode.setProperty("uri", ipAddress);

		if (nodeInfoOfAnIP.get(ipAddress).portNodeIdOfPort.containsKey(portNumber)) {
			portNode = dataGraph.getNodeById(nodeInfoOfAnIP.get(ipAddress).portNodeIdOfPort.get(portNumber));
		} else {

			// create a port node for ip
			portNode = dataGraph.createNode();
			numberOfCreatedNodes++;

			if (portNumber == 80 || portNumber == 6667) {
				portNode.addLabel(Label.label("Port_" + Integer.toString(portNumber)));
			} else {
				portNode.addLabel(Label.label("Port"));
			}

			portNode.setProperty("number", portNumber);
			portNode.setProperty("uri", ipAddress + "_" + portNumber);

			// create a rel between ip and port
			if (inOutPort.contains("out")) {
				ipNode.createRelationshipTo(portNode, RelationshipType.withName(inOutPort));
			} else {
				portNode.createRelationshipTo(ipNode, RelationshipType.withName(inOutPort));
			}

			numberOfCreatedRels++;

			// store the information of ip and port in map
			nodeInfoOfAnIP.get(ipAddress).portNodeIdOfPort.put(portNumber, portNode.getId());
		}

	}

}

class IPNodeInfo {
	Long ipNodeId;
	HashMap<Integer, Long> portNodeIdOfPort;

	public IPNodeInfo(Long ipNodeId) {
		this.ipNodeId = ipNodeId;
		portNodeIdOfPort = new HashMap<Integer, Long>();
	}
}