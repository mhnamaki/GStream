package src.dataset.ImportCitation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import src.utilities.Dummy.DummyFunctions;

public class CitationExporter {
	public static GraphDatabaseService dataGraph;
	public String[] dataGraphPathes = new String[] {
			"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v2.2.graphdb",
			"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/IDSAttack/idsJun17_V3.db",
			"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationV4/citation.graphdb" };

	public static void main(String[] args) throws Exception {
		CitationExporter ce = new CitationExporter();
		// ce.init();
		//
		// ce.run();

		// ce.importToNeo4j();

	//	ce.updateLabels();
	}

	private void updateLabels() {
		File storeDir = new File(
				"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/allDatabaseCite_IDS.db");
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println(storeDir.getAbsolutePath());
		Transaction tx1 = dataGraph.beginTx();

		HashMap<String, String> mapOfLbl = new HashMap<String, String>();
		int i = 0;
		for (Label lbl : dataGraph.getAllLabels()) {
			mapOfLbl.put(lbl.name().toString(), "m" + i);
			System.out.println(lbl.name().toString() + " -> " + "m" + i);
			i++;
		}

		int cnt = 0;
		for (Node node : dataGraph.getAllNodes()) {
			cnt++;
			Label lbl = node.getLabels().iterator().next();
			node.removeLabel(lbl);
			node.addLabel(Label.label(mapOfLbl.get(lbl.name().toString())));

			if (cnt % 100000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
				System.out.println(cnt);
			}
		}

		tx1.success();
		tx1.close();

		dataGraph.shutdown();

	}

	private void importToNeo4j() throws Exception {
		String command = "/Users/mnamaki/Documents/Education/PhD/Fall2015/BigData/Neo4j/neo4j-community-2.3.1"
				+ "/bin/neo4j-import --id-type INTEGER --into "
				+ "/Users/mnamaki/Documents/Education/PhD/Fall2016/allDatabase.db";
		for (File file : getFilesInTheDirfinder("/Users/mnamaki/Documents/workspace/TopKGStream/allDatasets/")) {

			if (!file.getName().contains("Relationships.csv")) {
				command += " --nodes:" + file.getName().replace(".csv", "") + " " + file.getAbsolutePath();
			}

		}

		command += " --relationships:N "
				+ "/Users/mnamaki/Documents/workspace/TopKGStream/allDatasets/Relationships.csv";

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

	private void run() throws Exception {

		SimpleDateFormat dateFormatIDS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat dateFormatPanama = new SimpleDateFormat("dd-MM-yyyy");
		SimpleDateFormat dateFormatCitation = new SimpleDateFormat("yyyy");
		int prevDatasetMaxId = 0;
		int currentDatasetMaxId = 0;

		File foutRel = new File("Relationships.csv");
		FileOutputStream fosRel = new FileOutputStream(foutRel);
		BufferedWriter bwRel = new BufferedWriter(new OutputStreamWriter(fosRel));
		bwRel.write(":START_ID,:END_ID\n");

		HashSet<String> allSeenLables = new HashSet<String>();

		for (String dataGraphPath : dataGraphPathes) {
			// initialize data graph
			File storeDir = new File(dataGraphPath);
			dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
					.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

			System.out.println(storeDir.getAbsolutePath());
			Transaction tx1 = dataGraph.beginTx();

			int allLabels = 0;
			for (Label lbl : dataGraph.getAllLabels()) {
				allLabels++;
			}

			File[] fouts = new File[allLabels];
			FileOutputStream[] foses = new FileOutputStream[allLabels];
			BufferedWriter[] bws = new BufferedWriter[allLabels];

			HashMap<String, BufferedWriter> writerOfLabel = new HashMap<String, BufferedWriter>();
			HashMap<String, ArrayList<String>> propertyKeysOfLabel = new HashMap<String, ArrayList<String>>();

			int index = 0;
			for (Label lbl : dataGraph.getAllLabels()) {
				String lblStr = lbl.name().toString();
				lblStr = getCorrectedVersion(lbl.name().toString());
				fouts[index] = new File("allDatasets/" + lblStr + ".csv");
				foses[index] = new FileOutputStream(fouts[index]);
				bws[index] = new BufferedWriter(new OutputStreamWriter(foses[index]));

				writerOfLabel.put(lblStr, bws[index]);

				if (!allSeenLables.add(lblStr)) {
					throw new Exception("duplicated labels in different datasets " + lbl.name().toString());
				}

				HashSet<String> allPropertyKeysForThisLabel = new HashSet<String>();
				for (Node node : dataGraph.getAllNodes()) {
					if (node.hasLabel(lbl)) {
						for (String key : node.getPropertyKeys()) {
							allPropertyKeysForThisLabel.add(key);
						}
					}
				}

				ArrayList<String> sortedPropertyKeys = new ArrayList<String>();
				sortedPropertyKeys.addAll(allPropertyKeysForThisLabel);
				Collections.sort(sortedPropertyKeys);
				propertyKeysOfLabel.put(lblStr, sortedPropertyKeys);

				bws[index].write("nodeId:ID");
				for (String sortedKey : sortedPropertyKeys) {
					bws[index].write("," + getSameTimestamp(sortedKey));
				}
				bws[index].write("\n");
				bws[index].flush();

				index++;

			}

			System.out.println("traversing nodes");

			HashSet<Integer> nodeIds = new HashSet<Integer>();
			for (Node node : dataGraph.getAllNodes()) {

				String lbl = node.getLabels().iterator().next().name().toString();
				lbl = getCorrectedVersion(lbl);
				BufferedWriter bw = writerOfLabel.get(lbl);
				int nodeId = (int) (node.getId() + prevDatasetMaxId);
				bw.write(nodeId + "");

				nodeIds.add(nodeId);

				for (String sortedKey : propertyKeysOfLabel.get(lbl)) {
					bw.write(",");
					if (node.hasProperty(sortedKey)) {
						if (getSameTimestamp(sortedKey).equals("startTime")
								|| getSameTimestamp(sortedKey).equals("endTime")) {
							Date date = new Date();
							date.setYear(1800);
							try {
								date = dateFormatPanama.parse(node.getProperty(sortedKey).toString());
							} catch (Exception exc) {
								try {
									date = dateFormatCitation.parse(node.getProperty(sortedKey).toString());
								} catch (Exception exc2) {
									try {
										Timestamp tsDate = new Timestamp(
												Long.parseLong(node.getProperty(sortedKey).toString()));
										date = dateFormatIDS.parse(dateFormatIDS.format(tsDate));
									} catch (Exception exc3) {

									}
								}
							}
							bw.write(dateFormatIDS.format(date));
						} else {
							bw.write(getCorrectedVersion(node.getProperty(sortedKey).toString()));
						}
					}
				}
				bw.write("\n");

				currentDatasetMaxId = (int) Math.max(currentDatasetMaxId, nodeId);

			}

			System.out.println("traversing relationships");

			for (Relationship rel : dataGraph.getAllRelationships()) {
				int startNodeId = (int) (rel.getStartNode().getId() + prevDatasetMaxId);
				int endNodeId = (int) (rel.getEndNode().getId() + prevDatasetMaxId);

				if (!nodeIds.contains(startNodeId) || !nodeIds.contains(endNodeId)) {
					System.err.println("err");
				} else {
					bwRel.write(startNodeId + "," + endNodeId + "\n");
				}

			}

			for (BufferedWriter bw : bws) {
				bw.flush();
				bw.close();
			}

			tx1.success();
			tx1.close();
			dataGraph.shutdown();

			prevDatasetMaxId += currentDatasetMaxId + 5000;
		}

		bwRel.close();
	}

	private String getSameTimestamp(String sortedKey) {
		String retValue = sortedKey;
		switch (sortedKey) {

		case "incorporation_date":
			retValue = "startTime";
			break;

		case "inactivation_date":
			retValue = "endTime";
			break;

		case "Year":
			retValue = "startTime";
			break;

		case "year":
			retValue = "startTime";
			break;

		case "startTime":
			retValue = "startTime";
			break;

		case "endTime":
			retValue = "endTime";
			break;

		case "startTime ":
			retValue = "startTime";
			break;

		case "endTime ":
			retValue = "endTime";
			break;

		default:
			if (sortedKey.toLowerCase().contains("time"))
				retValue = "startTime";
			break;
		}

		return retValue;
	}

	private String getCorrectedVersion(String lblStr) {
		return lblStr.replaceAll("[^A-Za-z0-9 ]", "");
	}

	private void init() throws Exception {

		// DummyFunctions.registerShutdownHook(dataGraph);

	}

	public static File[] getFilesInTheDirfinder(String dirName) {
		File dir = new File(dirName);

		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".csv");
			}
		});

		if (files != null && files.length > 1)
			Arrays.sort(files);

		for (int i = 0; i < files.length; i++) {
			System.out.println("catched file " + i + "; " + files[i].getName());
		}
		return files;

	}
}
