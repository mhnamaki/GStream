package src.simpleTests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.StringTokenizer;

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
import src.utilities.Dummy.DummyProperties;
import src.utilities.Dummy;
import src.utilities.Stemmer;

public class CitationPlay {

	private String dataGraphPath = "";
	private GraphDatabaseService dataGraph;
	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();

	public static int consume = 0;

	public CitationPlay(String[] args) {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {
		CitationPlay cp = new CitationPlay(args);
		// cp.seeWhatsInsideOfEdgeUpdates();
		// cp.reversePapersAuthorsRels();
		// cp.findTypeToType();

		// cp.getPaperToPaperInEachYear();

		// cp.get5MostCitedInEachYear();

		// cp.panamaGetAllYears();
		// cp.keepPubAndPapers();

//		cp.updatePubLabelToTheirCat();

	}

	private void updatePubLabelToTheirCat() {
		File storeDir = new File(
				"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationDemoSnapshots/citationPub_what/citationDemoPaperPubFinal/gn.graphdb");
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int cntNodes = 0;
		int noCat = 0;
		for (Node node : dataGraph.getAllNodes()) {
			if (node.getLabels().iterator().next().name().equals("Publication_Venue")) {
				if (node.hasProperty("cat")) {
					node.removeLabel(Label.label("Publication_Venue"));
					node.addLabel(Label.label("Venue_" + node.getProperty("cat").toString()));
					cntNodes++;
					if (cntNodes % 10000 == 0) {
						tx1.success();
						tx1.close();

						tx1 = dataGraph.beginTx();
					}
				} else {
					noCat++;
					node.removeLabel(Label.label("Publication_Venue"));
					node.addLabel(Label.label("Venue_No_Cat"));
				}
			}
		}

		System.out.println("cntNodes: " + cntNodes);
		System.out.println("noCat: " + noCat);

		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}

	private void keepPubAndPapers() {
		File storeDir = new File(
				"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationDemoSnapshots/ciationWoAuthorsAndKeyword.graphdb");
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int cntNodes = 1;
		int cntRels = 1;
		for (Node node : dataGraph.getAllNodes()) {
			if (!node.getLabels().iterator().next().name().toLowerCase().equals("paper")
					&& !node.getLabels().iterator().next().name().toLowerCase().equals("publication_venue")) {

				for (Relationship rel : node.getRelationships()) {
					rel.delete();
					if (cntRels % 10000 == 0) {
						tx1.success();
						tx1.close();
						tx1 = dataGraph.beginTx();
						System.out.println("cntRels: " + cntRels);

					}
					cntRels++;
				}

				node.delete();

				if (cntNodes % 10000 == 0) {
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
					System.out.println("cntNodes: " + cntNodes);

				}
				cntNodes++;
			}
		}

		System.out.println("cntNodes: " + cntNodes);
		System.out.println("cntRels: " + cntRels);
		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void panamaGetAllYears() throws Exception {
		File storeDir = new File(
				"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v2.2.graphdb");
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");
		Transaction tx1 = dataGraph.beginTx();

		// int cnt = 0;
		// for (Node node : dataGraph.getAllNodes()) {
		// if (node.hasLabel(Label.label("Entity")) &&
		// node.hasProperty("countries")
		// &&
		// node.getProperty("countries").toString().toLowerCase().equals("china"))
		// {
		// cnt++;
		// String l1 = node.hasProperty("inactivation_date") ?
		// node.getProperty("inactivation_date").toString() : "";
		// String l2 = node.hasProperty("struck_off_date") ?
		// node.getProperty("struck_off_date").toString() : "";
		// System.out.println(
		// node.getProperty("name") + " " +
		// node.getProperty("incorporation_date") + " " + l1 + " " + l2);
		// }
		// }
		//
		// System.out.println(cnt);

		// for (Node node : dataGraph.getAllNodes()) {
		// if (node.hasLabel(Label.label("Entity"))) {
		// int seenCountry = 0;
		// int seenJurisdicion = 0;
		// HashSet<String> cntJurSet = new HashSet<String>();
		// for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
		// Node endNode = rel.getEndNode();
		// String lbl = endNode.getLabels().iterator().next().name();
		// if (lbl.toLowerCase().contains("country") && !lbl.contains("XXX")) {
		// seenCountry++;
		// cntJurSet.add(lbl);
		// }
		// if (lbl.toLowerCase().contains("jurisdiction") &&
		// !lbl.contains("XXX")) {
		// seenJurisdicion++;
		// cntJurSet.add(lbl);
		// }
		// }
		// if (seenCountry > 1) {
		// System.out.println("entity with more than one country: " +
		// node.getLabels().iterator().next() + " "
		// + cntJurSet);
		// }
		// if (seenJurisdicion > 1) {
		// System.out.println("entity with more than one jurisdiction: " +
		// node.getLabels().iterator().next()
		// + " " + cntJurSet);
		// }
		// }
		// }

		HashSet<Date> allDatesSet = new HashSet<Date>();
		ArrayList<Date> allDatesArr = new ArrayList<Date>();
		SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
		SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MMM-dd");
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Entity")) && (node.hasProperty("incorporation_date")
					|| node.hasProperty("inactivation_date") || node.hasProperty("struck_off_date"))) {
				if (node.hasProperty("incorporation_date")) {
					allDatesSet.add(sdfYear.parse(getYear(node.getProperty("incorporation_date").toString())));
				}
				if (node.hasProperty("inactivation_date")) {
					allDatesSet.add(sdfYear.parse(getYear(node.getProperty("inactivation_date").toString())));
				}
				if (node.hasProperty("struck_off_date")) {
					allDatesSet.add(sdfYear.parse(getYear(node.getProperty("struck_off_date").toString())));
				}
			}
		}

		allDatesArr.addAll(allDatesSet);
		Collections.sort(allDatesArr);

		for (Date currentDate : allDatesArr) {
			File foutCurrentDate = new File("panamaYears/" + sdfYear.format(currentDate) + ".txt");
			FileOutputStream fosCurrentDate = new FileOutputStream(foutCurrentDate);

			BufferedWriter bwCurrentDate = new BufferedWriter(new OutputStreamWriter(fosCurrentDate));

			System.out.println(sdfYear.format(currentDate));
			HashMap<String, Integer> CountryFrequency = new HashMap<String, Integer>();
			HashMap<String, Integer> JurisdictionFrequency = new HashMap<String, Integer>();
			for (Node node : dataGraph.getAllNodes()) {
				if (node.hasLabel(Label.label("Entity")) && (node.hasProperty("incorporation_date")
						|| node.hasProperty("inactivation_date") || node.hasProperty("struck_off_date"))) {

					Date nodeCreationDate = new Date();
					nodeCreationDate.setTime(0);

					if (node.hasProperty("incorporation_date")) {
						nodeCreationDate = sdfYear.parse(getYear(node.getProperty("incorporation_date").toString()));
					}

					Date nodeDeletionDate = new Date();
					nodeDeletionDate.setTime(Long.MAX_VALUE);
					if (node.hasProperty("inactivation_date")) {
						nodeDeletionDate = sdfYear.parse(getYear(node.getProperty("inactivation_date").toString()));
					}
					if (node.hasProperty("struck_off_date")) {
						Date tempDate = sdfYear.parse(getYear(node.getProperty("struck_off_date").toString()));
						if (tempDate.compareTo(nodeDeletionDate) < 0) {
							nodeDeletionDate = tempDate;
						}
					}

					if (nodeCreationDate.compareTo(currentDate) <= 0 && nodeDeletionDate.compareTo(currentDate) > 0) {
						if (node.hasProperty("countries")) {
							CountryFrequency.putIfAbsent(node.getProperty("countries").toString().toLowerCase(), 0);
							CountryFrequency.put(node.getProperty("countries").toString().toLowerCase(),
									CountryFrequency.get(node.getProperty("countries").toString().toLowerCase()) + 1);
						}
						if (node.hasProperty("jurisdiction")) {
							JurisdictionFrequency.putIfAbsent(node.getProperty("jurisdiction").toString().toLowerCase(),
									0);
							JurisdictionFrequency.put(node.getProperty("jurisdiction").toString().toLowerCase(),
									JurisdictionFrequency.get(node.getProperty("jurisdiction").toString().toLowerCase())
											+ 1);
						}
					}
				}
			}

			if (CountryFrequency.size() > 0) {
				Map<String, Integer> sortedNewMap = CountryFrequency.entrySet().stream()
						.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
								.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				sortedNewMap.forEach((key, val) -> {
					try {
						if (CitationPlay.consume < 10) {
							bwCurrentDate.write(sdfYear.format(currentDate) + "#" + key + "#" + val);
							bwCurrentDate.newLine();
							CitationPlay.consume++;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});

				CitationPlay.consume = 0;
			}

			bwCurrentDate.write("--------Jurisdictions---------");
			bwCurrentDate.newLine();
			bwCurrentDate.newLine();
			bwCurrentDate.newLine();

			if (JurisdictionFrequency.size() > 0) {
				Map<String, Integer> sortedNewMap = JurisdictionFrequency.entrySet().stream()
						.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
								.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				sortedNewMap.forEach((key, val) -> {
					try {
						if (CitationPlay.consume < 10) {
							bwCurrentDate.write(sdfYear.format(currentDate) + "#" + key + "#" + val);
							bwCurrentDate.newLine();
							CitationPlay.consume++;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});

				CitationPlay.consume = 0;

				bwCurrentDate.close();
			}
		}

		tx1.success();
		dataGraph.shutdown();
	}

	private String getYear(String string) {
		return string.split("-")[2];
	}

	private void get5MostCitedInEachYear() throws Exception {

		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");
		Transaction tx1 = dataGraph.beginTx();

		File foutYear = new File("citationYears/mostCitedInEachYear/" + "allYears.txt");
		FileOutputStream fosYear = new FileOutputStream(foutYear);
		BufferedWriter bwYear = new BufferedWriter(new OutputStreamWriter(fosYear));

		File foutInDeg = new File("citationYears/mostCitedInEachYear/" + "highestInDegreePaper.txt");
		FileOutputStream fosInDeg = new FileOutputStream(foutInDeg);
		BufferedWriter bwInDeg = new BufferedWriter(new OutputStreamWriter(fosInDeg));

		for (File file : DummyFunctions.getFilesInTheDirfinder("citationYears/")) {

			FileInputStream fis = new FileInputStream(file.getAbsolutePath());
			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			HashMap<Integer, Integer> frequencyMap = new HashMap<Integer, Integer>();
			int highestInDegreeNodeId = 0;
			int highestInDegreeValue = 0;
			while ((line = br.readLine()) != null) {
				// 3462794#8#On the Number of Multiplications for the Evaluation
				// of a Polynomial and Some of Its Derivatives
				String[] splittedLines = line.split("#");
				int nodeId = Integer.parseInt(splittedLines[0]);
				int inDegree = Integer.parseInt(splittedLines[1]);
				frequencyMap.putIfAbsent(nodeId, 0);
				frequencyMap.put(nodeId, frequencyMap.get(nodeId) + 1);

				if (inDegree > highestInDegreeValue) {
					highestInDegreeValue = inDegree;
					highestInDegreeNodeId = nodeId;
				}
			}

			br.close();

			// end of the year
			if (frequencyMap.size() > 0) {
				Map<Integer, Integer> sortedNewMap = frequencyMap.entrySet().stream()
						.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
								.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				sortedNewMap.forEach((key, val) -> {
					try {
						if (CitationPlay.consume < 20) {
							Node node = dataGraph.getNodeById(key);

							bwYear.write(file.getName() + "#" + key + "#" + node.getDegree(Direction.INCOMING) + "#"
									+ node.getProperty("Title") + "#" + node.getProperty("Year") + "#" + val);
							bwYear.newLine();
							CitationPlay.consume++;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});

				CitationPlay.consume = 0;
			}

			bwInDeg.write(file.getName() + "#" + highestInDegreeNodeId + "#" + highestInDegreeValue);
			bwInDeg.newLine();
		}

		bwYear.close();
		bwInDeg.close();
		tx1.success();
		dataGraph.shutdown();
	}

	private void getPaperToPaperInEachYear() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");
		Transaction tx1 = dataGraph.beginTx();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");

		Date minDate = new Date();
		minDate.setTime(Long.MAX_VALUE);
		Date maxDate = new Date();
		maxDate.setTime(0);

		HashSet<Date> allDatesSet = new HashSet<Date>();
		ArrayList<Date> allDatesArr = new ArrayList<Date>();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper")) && node.hasProperty("Year")) {
				String dateStr = node.getProperty("Year").toString();
				try {
					Date date = dateFormat.parse(dateStr);
					if (date.compareTo(minDate) < 0) {
						minDate = date;
					}
					if (date.compareTo(maxDate) > 0) {
						maxDate = date;
					}
					allDatesSet.add(date);
				} catch (Exception exc) {
					if (!dateStr.equals("NO_YEAR"))
						System.err.println(dateStr);
				}

			}
		}

		System.out.println("minDate:" + minDate);
		System.out.println("maxDate:" + maxDate);

		allDatesArr.addAll(allDatesSet);
		Collections.sort(allDatesArr);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
		System.out.println("all dates:");

		for (Date currentDate : allDatesArr) {

			System.out.println(sdf.format(currentDate));
			File foutCurrentDate = new File("citationYears/" + sdf.format(currentDate) + ".txt");
			FileOutputStream fosCurrentDate = new FileOutputStream(foutCurrentDate);

			BufferedWriter bwCurrentDate = new BufferedWriter(new OutputStreamWriter(fosCurrentDate));

			for (Node node : dataGraph.getAllNodes()) {
				if (node.hasLabel(Label.label("Paper")) && node.hasProperty("Year")
						&& !node.getProperty("Year").equals("NO_YEAR")) {
					String dateStr = node.getProperty("Year").toString();
					Date paperDate = dateFormat.parse(dateStr);
					if (paperDate.equals(currentDate)) {
						for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
							if (rel.getEndNode().hasLabel(Label.label("Paper"))) {
								bwCurrentDate.write(
										rel.getEndNode().getId() + "#" + rel.getEndNode().getDegree(Direction.INCOMING)
												+ "#" + rel.getEndNode().getProperty("Title"));
								bwCurrentDate.newLine();
								bwCurrentDate.flush();
							}
						}
					}

				}
			}
			bwCurrentDate.close();
		}

		tx1.success();
		dataGraph.shutdown();

	}

	private void seeWhatsInsideOfEdgeUpdates() throws Exception {

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");

		Transaction tx1 = dataGraph.beginTx();

		HashSet<String> allLabels = new HashSet<String>();
		for (Label lbl : dataGraph.getAllLabels()) {
			allLabels.add(lbl.name().toString());
		}

		allLabels.remove("Paper");
		allLabels.remove("Auther");
		allLabels.remove("Author");
		allLabels.remove("Publication_Venue");

		SnapshotSimulator snapshotSim = new SnapshotSimulator(
				"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/CitationSnapshot/", "yyyy");

		int ignoranceCnt = 1101911;
		int importantCnt = 110000;

		for (int i = 0; i < ignoranceCnt; i++) {
			snapshotSim.getNextStreamEdge();
		}

		System.out.println("rad shod1");

		HashMap<String, Integer> typeToTypeFreq = new HashMap<String, Integer>();
		HashMap<Long, Integer> srcNodeIdsFrequency = new HashMap<Long, Integer>();
		HashMap<Long, Integer> destNodeIdsFrequency = new HashMap<Long, Integer>();

		for (int i = 0; i < importantCnt; i++) {
			StreamEdge nextStreamEdge = snapshotSim.getNextStreamEdge();

			long srcNodeId = nextStreamEdge.getSourceNode();
			long destNodeId = nextStreamEdge.getDestinationNode();
			Node srcNode = dataGraph.getNodeById(srcNodeId);
			Node destNode = dataGraph.getNodeById(destNodeId);
			String srcLbl = srcNode.getLabels().iterator().next().name();
			String destLbl = destNode.getLabels().iterator().next().name();

			String ttt = srcLbl + "->" + destLbl;
			typeToTypeFreq.putIfAbsent(ttt, 0);
			typeToTypeFreq.put(ttt, typeToTypeFreq.get(ttt) + 1);

			srcNodeIdsFrequency.putIfAbsent(srcNodeId, 0);
			srcNodeIdsFrequency.put(srcNodeId, srcNodeIdsFrequency.get(srcNodeId) + 1);

			destNodeIdsFrequency.putIfAbsent(destNodeId, 0);
			destNodeIdsFrequency.put(destNodeId, destNodeIdsFrequency.get(destNodeId) + 1);

		}

		System.out.println("rad shod2");

		{
			Set<Entry<String, Integer>> set = typeToTypeFreq.entrySet();
			List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(set);
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});

			for (Map.Entry<String, Integer> entry : list) {
				System.out.println(entry.getKey() + "#" + entry.getValue());
			}
		}

		System.out.println();
		System.out.println();

		{
			HashMap<String, Integer> keywordFreq = new HashMap<String, Integer>();

			Set<Entry<Long, Integer>> set = srcNodeIdsFrequency.entrySet();
			List<Entry<Long, Integer>> list = new ArrayList<Entry<Long, Integer>>(set);
			Collections.sort(list, new Comparator<Map.Entry<Long, Integer>>() {

				@Override
				public int compare(Entry<Long, Integer> o1, Entry<Long, Integer> o2) {
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});

			for (

			Map.Entry<Long, Integer> entry : list) {
				Long nodeId = entry.getKey();
				String paperTitle = dataGraph.getNodeById(nodeId).getProperty("Title").toString();

				StringTokenizer st = new StringTokenizer(paperTitle);
				while (st.hasMoreTokens()) {
					String next = st.nextToken();
					Stemmer stemmer = new Stemmer();
					String stemmed = stemmer.stem(next);
					if (allLabels.contains(stemmed)) {
						keywordFreq.putIfAbsent(next, 0);
						keywordFreq.put(next, keywordFreq.get(next) + 1);
					}
				}

				System.out.println(nodeId + "#" + dataGraph.getNodeById(nodeId).getLabels().iterator().next().name()
						+ "#" + paperTitle + "#" + entry.getValue());

			}

			System.out.println();
			System.out.println();
			System.out.println("keyword frequencies");

			for (String keyword : keywordFreq.keySet()) {
				focusLabelPropValSet.clear();
				allNodesOfFocusType.clear();
				focusLabelSet.clear();
				fillSet("Paper|Title:\"" + keyword + "\"");
				int numberOfAllFocuses = fillFocusNodesOfRequestedTypes(dataGraph);
				System.out.println(keyword + "#" + keywordFreq.get(keyword) + "#" + numberOfAllFocuses);
			}
		}

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
		}

		return numberOfAllFocusNodes;
	}

	private void reversePapersAuthorsRels() {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");

		Transaction tx1 = dataGraph.beginTx();

		int numberOfAllRels = DummyFunctions.getNumberOfAllRels(dataGraph);
		System.out.println("numberOfAllRels before:" + numberOfAllRels);

		int cnt = 1;
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (rel.getStartNode().hasLabel(Label.label("Author")) && rel.getEndNode().hasLabel(Label.label("Paper"))) {
				rel.getEndNode().createRelationshipTo(rel.getStartNode(), rel.getType());
				rel.delete();
			}

			if (cnt % 100000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
				if (cnt % 1000000 == 0)
					System.out.println("rel cnt update" + cnt);
			}

			cnt++;
		}
		System.out.println("numberOfAllRels after:" + numberOfAllRels);

		tx1.success();
		tx1.close();

		dataGraph.shutdown();

	}

	private void findTypeToType() {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);
		System.out.println("after data graph init");

		Transaction tx1 = dataGraph.beginTx();
		System.out.println("AvgOutDegrees: " + DummyFunctions.getAvgOutDegrees(dataGraph));

		HashSet<String> typeToType = new HashSet<String>();
		for (Relationship rel : dataGraph.getAllRelationships()) {
			String ttt = rel.getStartNode().getLabels().iterator().next().name() + "->"
					+ rel.getEndNode().getLabels().iterator().next().name();
			if (typeToType.add(ttt)) {
				System.out.println(ttt);
			}
		}

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
