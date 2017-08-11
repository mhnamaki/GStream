package src.simpleTests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ObjectInputStream.GetField;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import edu.stanford.nlp.util.Sets;
import src.utilities.Dummy.DummyFunctions;

public class FocusSetCounterCitation {

	private static final String outputFile = "reasonableFocusSetCitationV7.txt";
	private static final String middleFile = "focusSetCitation.txt";
	private static String allFocusLinesPath = "/Users/mnamaki/Desktop/CitationTokensV2.txt";
	private String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationDemoSnapshots/citationPub_what/citationDemoPaperPub/g0.graphdb";

	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	String focusSetPath = null;
	GraphDatabaseService dataGraph;

	public FocusSetCounterCitation(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-allFocusLinesPath")) {
				allFocusLinesPath = args[++i];
			} else if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			}
		}
		if (allFocusLinesPath.equals("") || dataGraphPath.equals("")) {
			throw new Exception("allFocusLinesPath  or dataGraphPath ");
		}
	}

	public static void main(String[] args) throws Exception {

		FocusSetCounterCitation fsc = new FocusSetCounterCitation(args);
		// fsc.run();

		// fsc.getFreqPatterns();

		// fsc.matchPubVenue();

		// fsc.updatePaperProperties();

		// fsc.findMostFrequentCitedDBPaperDemo();
		// fsc.getCatToCat();

		// fsc.getXToYExamples();

		// fsc.findVenuesCat();
		// fsc.countPapersBeforeTime();
		// fsc.setDataDrivenRecommPaperVenueToDM();
		// fsc.updateNoYearTo1801();

		// fsc.updateTitleToLowercase();

		// fsc.updateVenuesCat();
		fsc.removeExtraProps();

	}

	private void removeExtraProps() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();
		int cnt = 0;
		int cnt2 = 0;
		int cnt3 = 0;
		HashSet<String> keys = new HashSet<String>();
		for (Node node : dataGraph.getAllNodes()) {
			for (String key : node.getPropertyKeys()) {
				keys.add(key);
			}

			// if (node.hasProperty("Index")) {
			// node.removeProperty("Index");
			// }
			// if (node.hasProperty("1")) {
			// node.removeProperty("1");
			// cnt2++;
			// }
			// if (node.hasProperty("1 ")) {
			// node.removeProperty("1 ");
			// cnt2++;
			// }
			// cnt++;
			// if (cnt % 10000 == 0) {
			// tx1.success();
			// tx1.close();
			// tx1 = dataGraph.beginTx();
			// }
		}

		for (String key : keys) {
			System.out.println("-" + key + "-");
		}

		System.out.println("cnt2: " + cnt2);
		System.out.println("cnt3: " + cnt3);
		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void updateVenuesCat() {
		long[] ids = new long[] { 4235844, 4176952, 4176951, 4176950, 4223567, 4296284, 4052607, 4234438, 4234989,
				4167482, 4167480, 4167543, 4155282, 4167046, 4232617, 4175316, 4175313 };

		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int cnt = 0;
		for (int i = 0; i < ids.length; i++) {
			Node node = dataGraph.getNodeById(ids[i]);
			if (node.getLabels().iterator().next().name().equals("Venue_No_Cat")) {
				node.removeLabel(Label.label("Venue_No_Cat"));
				node.addLabel(Label.label("Venue_AI"));
				node.setProperty("cat", "AI");
				cnt++;
			}
		}

		System.out.println("cnt: " + cnt);
		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}

	private void updateTitleToLowercase() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();
		int cnt = 0;
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper")) && node.hasProperty("Title")
					&& node.getProperty("Title").toString().toLowerCase().contains("recommend")) {
				node.setProperty("Title", node.getProperty("Title").toString().toLowerCase());
				cnt++;
				System.out.println(node.getId());
			}
		}

		System.out.println("cnt:" + cnt);
		tx1.success();
		// tx1.close();
		dataGraph.shutdown();

	}

	private void updateNoYearTo1801() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();
		int cnt = 0;
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasProperty("Year") && node.getProperty("Year").toString().toLowerCase().contains("no_year")) {
				cnt++;
				node.setProperty("Year", 1801);
			}
		}

		System.out.println(cnt);
		tx1.success();
		// tx1.close();
		dataGraph.shutdown();
	}

	private void findVenuesCat() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasProperty("cat") && node.getProperty("cat").toString().equals("AI")) {
				for (Relationship rel : node.getRelationships()) {
					Node otherNode = rel.getOtherNode(node);
					if (otherNode.getLabels().iterator().next().name().toString().toLowerCase().contains("public")) {
						System.out.println(otherNode.getProperty("Venue_Name"));
					}
				}
			}

		}

		tx1.success();
		tx1.close();

	}

	private void findMostFrequentCitedDBPaper() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		HashMap<Long, Integer> freq = new HashMap<Long, Integer>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper")) && node.hasProperty("cat")
			// && node.getProperty("cat").toString().equals("AI")
					&& (
					// node.getProperty("Title").toString().toLowerCase().contains("deep
					// learning")
					// ||
					// node.getProperty("Title").toString().toLowerCase().contains("causality")
					// ||
					// node.getProperty("Title").toString().toLowerCase().contains("anomaly")
					// ||
					node.getProperty("Title").toString().toLowerCase().contains("data-driven")
							// ||
							// node.getProperty("Title").toString().toLowerCase().contains("healthcare")
							|| node.getProperty("Title").toString().toLowerCase().contains("recommendation"))) {

				if (node.getDegree(Direction.INCOMING) >= 5) {
					freq.put(node.getId(), node.getDegree(Direction.INCOMING));
				}
			}
		}

		Map<Long, Integer> sortedNewMap = freq.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		sortedNewMap.forEach((key, val) -> {
			try {
				if (val > 70)
					System.out.println(
							key + "#" + dataGraph.getNodeById(key).getProperty("Title").toString().toLowerCase() + "#"
									+ dataGraph.getNodeById(key).getDegree(Direction.INCOMING) + "#"
									+ dataGraph.getNodeById(key).getProperty("Year").toString());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void findMostFrequentCitedDBPaperDemo() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int cntRecommendation = 0;
		int cntRecomPaperInDM = 0;
		HashMap<Integer, HashMap<String, PatternsOfTheYear>> citingPapersOfTheYear = new HashMap<Integer, HashMap<String, PatternsOfTheYear>>();
		for (Node paperStar : dataGraph.getAllNodes()) {
			// paper*
			if (paperStar.hasLabel(Label.label("Paper"))
					&& paperStar.getProperty("Title").toString().toLowerCase().contains("recommendation")) {
				cntRecommendation++;
				// if (node.getDegree(Direction.INCOMING) >= 5) {
				// freq.put(node.getId(), node.getDegree(Direction.INCOMING));
				// }
				int venueDMId = 0;
				for (Relationship rel : paperStar.getRelationships(Direction.OUTGOING)) {
					Node pubDMStar = rel.getOtherNode(paperStar);
					if (pubDMStar.getLabels().iterator().next().name().equals("Venue_DM")) {
						venueDMId = (int) pubDMStar.getId();
						cntRecomPaperInDM++;
						break;
					}
				}
				if (venueDMId > 0) {
					for (Relationship rel : paperStar.getRelationships(Direction.INCOMING)) {
						Node citingPaper = rel.getOtherNode(paperStar);
						if (citingPaper.getLabels().iterator().next().name().equals("Paper")
								&& citingPaper.hasProperty("Year")
								&& !citingPaper.getProperty("Year").toString().toLowerCase().equals("no_year")) {
							for (Relationship publishedInRel : citingPaper.getRelationships(Direction.OUTGOING)) {
								Node publishedWhereNode = publishedInRel.getOtherNode(citingPaper);
								if (publishedWhereNode.getLabels().iterator().next().name().contains("Venue_")) {
									// System.out.println("[" +
									// paperStar.getId() + ", " + venueDMId + ",
									// "
									// + citingPaper.getId() + ", " +
									// publishedWhereNode.getId() + "]");

									int year = Integer.parseInt(citingPaper.getProperty("Year").toString());
									String publishedWhereLabel = publishedWhereNode.getLabels().iterator().next()
											.name();
									citingPapersOfTheYear.putIfAbsent(year, new HashMap<>());

									citingPapersOfTheYear.get(year).putIfAbsent(publishedWhereLabel,
											new PatternsOfTheYear());

									citingPapersOfTheYear.get(year).get(publishedWhereLabel).addPatternsOfTheYear(
											(int) paperStar.getId(), venueDMId, (int) citingPaper.getId(),
											(int) publishedWhereNode.getId());

								}
							}
						}
					}
				}
			}
		}

		for (Integer year : citingPapersOfTheYear.keySet()) {
			for (String publishedWhere : citingPapersOfTheYear.get(year).keySet()) {
				PatternsOfTheYear pOfYear = citingPapersOfTheYear.get(year).get(publishedWhere);
				System.out.println(
						year + "#" + publishedWhere + "#" + pOfYear.recomPapers.size() + "#" + pOfYear.VenueDmSet.size()
								+ "#" + pOfYear.citingPapers.size() + "#" + pOfYear.venueWhereIdSet.size());
			}
		}

		HashMap<String, HashMap<String, PatternsOfTheYear>> citingPapersOfTheWindow = new HashMap<String, HashMap<String, PatternsOfTheYear>>();
		for (Integer year : citingPapersOfTheYear.keySet()) {
			if (year <= 2005) {
				for (String publishedWhere : citingPapersOfTheYear.get(year).keySet()) {
					PatternsOfTheYear pOfYear = citingPapersOfTheYear.get(year).get(publishedWhere);
					citingPapersOfTheWindow.putIfAbsent("1980-2005", new HashMap<>());
					citingPapersOfTheWindow.get("1980-2005").putIfAbsent(publishedWhere, new PatternsOfTheYear());
					citingPapersOfTheWindow.get("1980-2005").get(publishedWhere).addPatternsOfTheYear(pOfYear);
				}
			}
		}
		// 2006-2010
		for (Integer year : citingPapersOfTheYear.keySet()) {
			if (year > 2005 && year <= 2010) {
				for (String publishedWhere : citingPapersOfTheYear.get(year).keySet()) {
					PatternsOfTheYear pOfYear = citingPapersOfTheYear.get(year).get(publishedWhere);
					citingPapersOfTheWindow.putIfAbsent("2006-2010", new HashMap<>());
					citingPapersOfTheWindow.get("2006-2010").putIfAbsent(publishedWhere, new PatternsOfTheYear());
					citingPapersOfTheWindow.get("2006-2010").get(publishedWhere).addPatternsOfTheYear(pOfYear);
				}
			}
		}
		// 2011-2015
		for (Integer year : citingPapersOfTheYear.keySet()) {
			if (year > 2010 && year <= 2015) {
				for (String publishedWhere : citingPapersOfTheYear.get(year).keySet()) {
					PatternsOfTheYear pOfYear = citingPapersOfTheYear.get(year).get(publishedWhere);
					citingPapersOfTheWindow.putIfAbsent("2011-2015", new HashMap<>());
					citingPapersOfTheWindow.get("2011-2015").putIfAbsent(publishedWhere, new PatternsOfTheYear());
					citingPapersOfTheWindow.get("2011-2015").get(publishedWhere).addPatternsOfTheYear(pOfYear);
				}
			}
		}
		System.out.println();
		System.out.println();
		System.out.println();

		for (String window : citingPapersOfTheWindow.keySet()) {
			for (String publishedWhere : citingPapersOfTheWindow.get(window).keySet()) {
				PatternsOfTheYear pOfYear = citingPapersOfTheWindow.get(window).get(publishedWhere);
				System.out.println(window + "#" + publishedWhere + "#" + pOfYear.recomPapers.size() + "#"
						+ pOfYear.VenueDmSet.size() + "#" + pOfYear.citingPapers.size() + "#"
						+ pOfYear.venueWhereIdSet.size());
			}
		}

		// find no-cat-venues that are not used in previous windows...
		PatternsOfTheYear noCatWindowLess2005 = citingPapersOfTheWindow.get("1980-2005").get("Venue_No_Cat");
		PatternsOfTheYear noCatWindow2006_2010 = citingPapersOfTheWindow.get("2006-2010").get("Venue_No_Cat");
		PatternsOfTheYear noCatWindow2011_2015 = citingPapersOfTheWindow.get("2011-2015").get("Venue_No_Cat");

		Set<Integer> remainingNodeIdsView = com.google.common.collect.Sets
				.difference(noCatWindow2011_2015.venueWhereIdSet, noCatWindow2006_2010.venueWhereIdSet);
		remainingNodeIdsView = com.google.common.collect.Sets.difference(remainingNodeIdsView,
				noCatWindowLess2005.venueWhereIdSet);

		System.out.println();
		System.out.println();

		for (Integer venueNoCatId : remainingNodeIdsView) {
			System.out.println(venueNoCatId + "," + dataGraph.getNodeById(venueNoCatId).getProperty("Venue_Name"));
		}

		System.out.println();
		System.out.println("recom: " + cntRecommendation);
		System.out.println("recom in dm: " + cntRecomPaperInDM);
		// Map<Long, Integer> sortedNewMap = freq.entrySet().stream()
		// .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
		// .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
		// (e1, e2) -> e1, LinkedHashMap::new));
		//
		// sortedNewMap.forEach((key, val) -> {
		// try {
		// if (val > 70)
		// System.out.println(
		// key + "#" +
		// dataGraph.getNodeById(key).getProperty("Title").toString().toLowerCase()
		// + "#"
		// + dataGraph.getNodeById(key).getDegree(Direction.INCOMING) + "#"
		// + dataGraph.getNodeById(key).getProperty("Year").toString());
		//
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// });

		tx1.success();
		// tx1.close();
		dataGraph.shutdown();

	}

	private void getXToYExamples() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		HashSet<Long> mostCitedAssPapersSet = new HashSet<Long>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper")) && node.hasProperty("cat")
					&& node.getProperty("cat").toString().equals("DB")
					&& (node.getProperty("Title").toString().toLowerCase().contains("rule-mining")
							|| node.getProperty("Title").toString().toLowerCase().contains("rule mining")
							|| node.getProperty("Title").toString().toLowerCase().contains("association rules"))) {
				if (node.getDegree(Direction.INCOMING) >= 100) {
					mostCitedAssPapersSet.add(node.getId());
				}
			}
		}

		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper")) && node.hasProperty("cat") && node.hasProperty("Year")
					&& !node.getProperty("Year").toString().toLowerCase().equals("no_year")) {
				Integer pubYear = Integer.parseInt(node.getProperty("Year").toString());
				if (pubYear >= 2010 && pubYear < 2020) {
					for (Relationship rel : node.getRelationships()) {
						Node otherNode = rel.getOtherNode(node);
						if (otherNode.hasLabel(Label.label("Paper")) && otherNode.hasProperty("cat")
								&& otherNode.getProperty("cat").toString().equals("AI")) {
							System.out.println(node.getProperty("Title") + "-->" + otherNode.getProperty("Title"));
						}
					}

				}

			}
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}

	private void getCatToCat() throws Exception {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		HashMap<String, HashSet<String>> pubNameOfCat = new HashMap<String, HashSet<String>>();
		HashSet<Long> mostCitedAssPapersSet = new HashSet<Long>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper")) && node.hasProperty("Title")
			// && node.getProperty("cat").toString().equals("Alg")
					&& (
					// node.getProperty("Title").toString().toLowerCase().contains("deep
					// learning")
					// ||
					// node.getProperty("Title").toString().toLowerCase().contains("causality")
					// ||
					// node.getProperty("Title").toString().toLowerCase().contains("anomaly")
					// ||
					node.getProperty("Title").toString().toLowerCase().contains("data-driven")
							// ||
							// node.getProperty("Title").toString().toLowerCase().contains("healthcare")
							|| node.getProperty("Title").toString().toLowerCase().contains("recommendation"))) {
				if (node.getDegree(Direction.INCOMING) >= 1) {
					// System.out.println(node.getProperty("Year"));
					mostCitedAssPapersSet.add(node.getId());

					// if (node.hasProperty("cat")) {
					// for (Relationship rel : node.getRelationships()) {
					// Node otherNode = rel.getOtherNode(node);
					// if
					// (otherNode.getLabels().iterator().next().name().toString().toLowerCase().contains("public"))
					// {
					// pubNameOfCat.putIfAbsent(node.getProperty("cat").toString(),
					// new HashSet<String>());
					// pubNameOfCat.get(node.getProperty("cat").toString()).add(otherNode.getProperty("Venue_Name").toString());
					// }
					// }
					// }

					// if (node.hasProperty("cat")) {
					// System.out.println(node.getProperty("Title").toString().replace(";","
					// ") + ";" + node.getProperty("Year") + ";"
					// + node.getProperty("cat"));
					// }
				}
			}
		}

		// for (String cat : pubNameOfCat.keySet()) {
		// System.out.println(cat);
		// for (String name : pubNameOfCat.get(cat)) {
		// System.out.println(name);
		// }
		// System.out.println();
		// }

		System.out.println("mostCitedAssPapersSet: " + mostCitedAssPapersSet.size());

		HashMap<String, Integer> freq = new HashMap<String, Integer>();
		SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");

		int step = 5;
		for (int year = 1990; year < 2021; year += step) {
			Date currentDate = new Date();
			Date futureDate = new Date();

			currentDate = sdfYear.parse(String.valueOf(year));
			futureDate = sdfYear.parse(String.valueOf(year + step));

			for (Node node : dataGraph.getAllNodes()) {
				if (node.hasLabel(Label.label("Paper")) && node.hasProperty("cat") && node.hasProperty("Year")
						&& !node.getProperty("Year").toString().toLowerCase().equals("no_year")) {
					Date pubDate = sdfYear.parse(node.getProperty("Year").toString());

					if (currentDate.compareTo(pubDate) >= 0 && futureDate.compareTo(pubDate) > 0) {

						for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
							Node otherNode = rel.getOtherNode(node);
							// if (otherNode.hasLabel(Label.label("Paper")) &&
							// otherNode.hasProperty("cat")) {
							if (mostCitedAssPapersSet.contains(otherNode.getId())) {
								String cat1 = node.getProperty("cat").toString();
								// String cat2 =
								// otherNode.getProperty("cat").toString();
								freq.putIfAbsent(cat1, 0);
								freq.put(cat1, freq.get(cat1) + 1);
							}
						}
					}
				}
			}

			System.out.println(year + "-" + (year + step));
			Map<String, Integer> sortedNewMap = freq.entrySet().stream()
					.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			sortedNewMap.forEach((key, val) -> {
				try {
					System.out.println(key + "#" + val);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			System.out.println();
			System.out.println();

		}

		// for (String catToCat : freq.keySet()) {
		// System.out.println(catToCat + ": " + freq.get(catToCat));
		// }

		tx1.success();
		tx1.close();

	}

	private void countPapersBeforeTime() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int cnt1 = 0;
		int cnt2 = 0;
		int cnt3 = 0;
		int cnt4 = 0;
		int cntTotal = 0;
		HashSet<Long> ll = new HashSet<Long>();
		HashMap<Integer, Integer> freqOfYear = new HashMap<Integer, Integer>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper"))) {
				// if (!node.hasProperty("Year")
				// || (node.hasProperty("Year") &&
				// (node.getProperty("Year").toString().equals("NO_YEAR")
				// || Integer.parseInt(node.getProperty("Year").toString()) <
				// 2000))) {
				// cnt++;
				// }
				// if ((node.hasProperty("Year") &&
				// !node.getProperty("Year").toString().contains("NO_YEAR")
				// && Integer.parseInt(node.getProperty("Year").toString()) <
				// 2001)
				// || (node.hasProperty("Year") &&
				// node.getProperty("Year").toString().contains("NO_YEAR"))) {
				// for(Relationship rel :
				// node.getRelationships(Direction.BOTH)){
				// ll.add(rel.getId());
				// }
				// cnt1++;
				// }
				// cntTotal++;
				if (node.hasProperty("Title") && node.getProperty("Title").toString().contains("recommendation")) {
					if (node.hasProperty("Year")
							&& !node.getProperty("Year").toString().toLowerCase().equals("no_year")) {
						int year = Integer.parseInt(node.getProperty("Year").toString());
						freqOfYear.putIfAbsent(year, 0);
						freqOfYear.put(year, freqOfYear.get(year) + 1);
					}
				}
			}

		}

		for (Integer year : freqOfYear.keySet()) {
			System.out.println(year + " => " + freqOfYear.get(year));
		}
		System.out.println("cnt1:" + cnt1);
		System.out.println("ll:" + ll.size());

		System.out.println("cntTotal:" + cntTotal);
		tx1.success();
		// tx1.close();

		dataGraph.shutdown();

	}

	private void setDataDrivenRecommPaperVenueToDM() {
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		int cntPaper = 0;
		int cntVenues = 0;
		HashSet<Long> venueSet = new HashSet<Long>();
		Transaction tx1 = dataGraph.beginTx();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Paper"))
					&& !node.getProperty("Title").toString().toLowerCase().contains("proceeding")
					&& ((node.getProperty("Title").toString().toLowerCase().contains("data")
							&& node.getProperty("Title").toString().toLowerCase().contains("recommend"))
							|| node.getProperty("Title").toString().toLowerCase().contains("recommendation"))) {

				if (!node.hasProperty("cat")) {
					System.out.println(node.getId() + " # " + node.getProperty("Title").toString() + " # "
							+ (node.hasProperty("cat") ? node.getProperty("cat") : ""));
					node.setProperty("cat", "DM");
					cntPaper++;

					for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
						Node otherNode = rel.getOtherNode(node);
						if (otherNode.getLabels().iterator().next().name().toLowerCase().equals("publication_venue")) {
							otherNode.setProperty("cat", "DM");
							venueSet.add(otherNode.getId());
							cntVenues++;
						}
					}

				}

			}
		}

		System.out.println(" cnt Papers:" + cntPaper);
		System.out.println(" cnt Venues:" + cntVenues);
		System.out.println(" cnt distinct Venues:" + venueSet.size());

		tx1.success();
		// tx1.close();

		dataGraph.shutdown();

	}

	private void serCatForPapers() {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int cnt = 0;
		for (Node node : dataGraph.getAllNodes()) {
			if (node.hasLabel(Label.label("Publication_Venue")) && node.hasProperty("cat")) {
				String cat = node.getProperty("cat").toString();
				for (Relationship rel : node.getRelationships()) {
					Node otherNode = rel.getOtherNode(node);
					if (otherNode.hasLabel(Label.label("Paper"))) {
						otherNode.setProperty("cat", cat);
						cnt++;
						if (cnt % 100000 == 0) {
							tx1.success();
							tx1.close();
							tx1 = dataGraph.beginTx();
							System.out.println(cnt);
						}
					}
				}
			}
		}
		tx1.success();
		tx1.close();

		dataGraph.shutdown();

	}

	private void updatePaperProperties() throws Exception {

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		File[] files = DummyFunctions.getFilesInTheDirfinder("out/");

		int cnt = 0;
		for (File file : files) {
			Transaction tx1 = dataGraph.beginTx();
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			HashSet<Integer> pubVenuesId = new HashSet<Integer>();
			String line = null;
			while ((line = br.readLine()) != null) {
				pubVenuesId.add(Integer.parseInt(line.split("#")[0]));
			}
			br.close();

			for (Node node : dataGraph.getAllNodes()) {
				if (pubVenuesId.contains((int) node.getId()) && !node.hasProperty("cat")) {
					node.setProperty("cat", file.getName().replace(".txt", ""));
					cnt++;
					if (cnt % 1000 == 0) {
						tx1.success();
						tx1.close();
						tx1 = dataGraph.beginTx();
					}
				}
			}

			tx1.success();
			tx1.close();

		}

		dataGraph.shutdown();
	}

	private void matchPubVenue() throws Exception {
		File fout0 = new File("matchPubVenue.txt");
		FileOutputStream fos0 = new FileOutputStream(fout0);
		BufferedWriter bwCitation = new BufferedWriter(new OutputStreamWriter(fos0));

		HashMap<String, ArrayList<String>> wordsOfACategory = new HashMap<String, ArrayList<String>>();
		wordsOfACategory.put("DB", new ArrayList<String>());
		wordsOfACategory.put("AI", new ArrayList<String>());
		wordsOfACategory.put("Alg", new ArrayList<String>());
		wordsOfACategory.put("HW", new ArrayList<String>());
		wordsOfACategory.put("PL", new ArrayList<String>());
		wordsOfACategory.put("Media", new ArrayList<String>());
		wordsOfACategory.put("Sys", new ArrayList<String>());
		wordsOfACategory.put("Bio", new ArrayList<String>());
		// wordsOfACategory.put("General", new ArrayList<String>());

		FileInputStream fisDB = new FileInputStream("venues/DB.txt");
		BufferedReader brDB = new BufferedReader(new InputStreamReader(fisDB));
		String line = null;
		while ((line = brDB.readLine()) != null) {
			wordsOfACategory.get("DB").add(line.replace("\t", "").trim().toLowerCase());
		}
		brDB.close();

		FileInputStream fisAI = new FileInputStream("venues/AI.txt");
		BufferedReader brAI = new BufferedReader(new InputStreamReader(fisAI));
		line = null;
		while ((line = brAI.readLine()) != null) {
			wordsOfACategory.get("AI").add(line.replace("\t", "").trim().toLowerCase());
		}
		brAI.close();

		FileInputStream fisAlg = new FileInputStream("venues/Alg.txt");
		BufferedReader brAlg = new BufferedReader(new InputStreamReader(fisAlg));
		line = null;
		while ((line = brAlg.readLine()) != null) {
			wordsOfACategory.get("Alg").add(line.replace("\t", "").trim().toLowerCase());
		}
		brAlg.close();

		FileInputStream fisHW = new FileInputStream("venues/HW.txt");
		BufferedReader brHW = new BufferedReader(new InputStreamReader(fisHW));
		line = null;
		while ((line = brHW.readLine()) != null) {
			wordsOfACategory.get("HW").add(line.replace("\t", "").trim().toLowerCase());
		}
		brHW.close();

		FileInputStream fisPL = new FileInputStream("venues/PL.txt");
		BufferedReader brPL = new BufferedReader(new InputStreamReader(fisPL));
		line = null;
		while ((line = brPL.readLine()) != null) {
			wordsOfACategory.get("PL").add(line.replace("\t", "").trim().toLowerCase());
		}
		brPL.close();

		FileInputStream fisMedia = new FileInputStream("venues/Media.txt");
		BufferedReader brMedia = new BufferedReader(new InputStreamReader(fisMedia));
		line = null;
		while ((line = brMedia.readLine()) != null) {
			wordsOfACategory.get("Media").add(line.replace("\t", "").trim().toLowerCase());
		}
		brMedia.close();

		FileInputStream fisSys = new FileInputStream("venues/Sys.txt");
		BufferedReader brSys = new BufferedReader(new InputStreamReader(fisSys));
		line = null;
		while ((line = brSys.readLine()) != null) {
			wordsOfACategory.get("Sys").add(line.replace("\t", "").trim().toLowerCase());
		}
		brSys.close();

		FileInputStream fisBio = new FileInputStream("venues/Bio.txt");
		BufferedReader brBio = new BufferedReader(new InputStreamReader(fisBio));
		line = null;
		while ((line = brBio.readLine()) != null) {
			wordsOfACategory.get("Bio").add(line.replace("\t", "").trim().toLowerCase());
		}
		brBio.close();

		// FileInputStream fisGeneral = new
		// FileInputStream("venues/General.txt");
		// BufferedReader brGeneral = new BufferedReader(new
		// InputStreamReader(fisGeneral));
		// line = null;
		// while ((line = brGeneral.readLine()) != null) {
		// wordsOfACategory.get("General").add(line.replace("\t",
		// "").trim().toLowerCase());
		// }
		// brGeneral.close();

		// for (String word : wordsOfACategory.keySet()) {
		// for (String pp : wordsOfACategory.get(word)) {
		//
		// System.out.println(word + "#" + pp);
		// }
		//
		// }

		File[] fout = new File[9];
		FileOutputStream[] fos = new FileOutputStream[9];
		BufferedWriter[] bWriter = new BufferedWriter[9];

		HashMap<String, BufferedWriter> writerOfACategory = new HashMap<String, BufferedWriter>();
		int i = 0;
		for (String category : wordsOfACategory.keySet()) {
			fout[i] = new File("out/" + category + ".txt");
			fos[i] = new FileOutputStream(fout[i]);
			bWriter[i] = new BufferedWriter(new OutputStreamWriter(fos[i]));
			writerOfACategory.put(category, bWriter[i]);
			i++;
		}

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// ArrayList<String> pubVenues = new ArrayList<String>();
		int cntVenue = 0;
		for (Node venueNode : dataGraph.getAllNodes()) {

			if (venueNode.hasLabel(Label.label("Publication_Venue"))) {
				cntVenue++;
				if (cntVenue % 1000 == 0)
					System.out.println("cntVenue: " + cntVenue);

				String venue = venueNode.getProperty("Venue_Name").toString().trim().toLowerCase();
				for (String key : wordsOfACategory.keySet()) {
					for (String keyword : wordsOfACategory.get(key))

						if (keyword.length() > 3) {
							if (venue.matches(".*\\b" + keyword + "\\b.*")) {
								writerOfACategory.get(key)
										.write(venueNode.getId() + "#" + venue + "#" + keyword + "#" + key);
								writerOfACategory.get(key).newLine();
							}
						}
				}
			}
		}

		for (int p = 0; p < 9; p++) {
			bWriter[p].close();
		}
		tx1.success();
		tx1.close();

		dataGraph.shutdown();

	}

	private void getFreqPatterns() throws Exception {

		File fout0 = new File("selfCitedPapers.txt");
		FileOutputStream fos0 = new FileOutputStream(fout0);
		BufferedWriter bwCitation = new BufferedWriter(new OutputStreamWriter(fos0));

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		int maxAuthors = 0;
		String maxAuthersValue = "";
		Transaction tx1 = dataGraph.beginTx();

		ArrayList<String> pubVenues = new ArrayList<String>();
		for (Node paper1 : dataGraph.getAllNodes()) {

			if (paper1.hasLabel(Label.label("Publication_Venue"))) {
				pubVenues.add(paper1.getProperty("Venue_Name").toString());
			}
			// if (paper1.hasLabel(Label.label("Publication_Venue"))) {
			// if (paper1.getProperty("Venue_Name").toString().contains("ICDE")
			// &&
			// !paper1.getProperty("Venue_Name").toString().contains("ICDEA"))
			// System.out.println(paper1.getProperty("Venue_Name"));
			// }
		}

		Collections.sort(pubVenues);

		for (String pub : pubVenues) {
			System.out.println(pub);
		}

		int cnt = 0;
		HashMap<Long, Integer> frequencyOfPubVenue = new HashMap<Long, Integer>();
		HashMap<Long, HashMap<Long, Integer>> frequencyOfAuthorsInAPubVenue = new HashMap<Long, HashMap<Long, Integer>>();

		for (Node paper1 : dataGraph.getAllNodes()) {
			if (paper1.hasLabel(Label.label("Paper"))) {

				HashSet<Long> paper1Authors = new HashSet<Long>();
				long paper1Pub = 0l;
				for (Relationship authorship : paper1.getRelationships(Direction.OUTGOING)) {
					Node author = authorship.getEndNode();
					if (author.hasLabel(Label.label("Author"))) {
						paper1Authors.add(author.getId());
					}
					if (author.hasLabel(Label.label("Publication_Venue"))) {
						paper1Pub = author.getId();
					}
				}

				for (Relationship cited : paper1.getRelationships(Direction.OUTGOING)) {
					Node otherPaper = cited.getEndNode();
					long paper2Pub = 0l;
					if (otherPaper.hasLabel(Label.label("Paper"))) {
						HashSet<Long> paper2Authors = new HashSet<Long>();

						for (Relationship auther2Rel : otherPaper.getRelationships(Direction.OUTGOING)) {
							Node otherAuther = auther2Rel.getEndNode();
							if (otherAuther.hasLabel(Label.label("Author"))) {
								paper2Authors.add(otherAuther.getId());
							}
							if (otherAuther.hasLabel(Label.label("Publication_Venue"))) {
								paper2Pub = otherAuther.getId();
							}

						}

						if (Sets.intersects(paper1Authors, paper2Authors)) {
							paper2Authors.retainAll(paper1Authors);
							if (paper2Authors.size() > 0) {

								// if () {
								if (paper1Pub == paper2Pub) {
									cnt++;
									if (maxAuthors < paper2Authors.size()) {
										System.out.println(
												paper1.getId() + "#" + otherPaper.getId() + "#" + paper2Authors);
									}

									maxAuthors = Math.max(paper2Authors.size(), maxAuthors);

									frequencyOfPubVenue.putIfAbsent(paper1Pub, 0);
									frequencyOfPubVenue.put(paper1Pub, frequencyOfPubVenue.get(paper1Pub) + 1);

									frequencyOfAuthorsInAPubVenue.putIfAbsent(paper1Pub, new HashMap<Long, Integer>());
									for (Long selfCitedAuthor : paper2Authors) {
										frequencyOfAuthorsInAPubVenue.get(paper1Pub).putIfAbsent(selfCitedAuthor, 0);
										frequencyOfAuthorsInAPubVenue.get(paper1Pub).put(selfCitedAuthor,
												frequencyOfAuthorsInAPubVenue.get(paper1Pub).get(selfCitedAuthor) + 1);
									}
								}

							}
						}

					}
				}
			}
		}

		System.out.println("how many of these? " + cnt);
		System.out.println("maxAuthors: " + maxAuthors);

		for (

		Long pubVenueId : frequencyOfAuthorsInAPubVenue.keySet()) {
			HashMap<Long, Integer> frequencyOfAuthorsInAPubVenueLocal = frequencyOfAuthorsInAPubVenue.get(pubVenueId);

			Map<Long, Integer> sortedNewMap = frequencyOfAuthorsInAPubVenueLocal.entrySet().stream()
					.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			sortedNewMap.forEach((key, val) -> {
				try {
					// if (CitationPlay.consume < 10) {
					bwCitation.write(pubVenueId + "#" + key + "#" + val);
					bwCitation.newLine();
					// }
					// CitationPlay.consume++;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}

		bwCitation.close();
		tx1.success();
		tx1.close();

		dataGraph.shutdown();
	}

	private void run() throws Exception {
		File fout0 = new File(outputFile);
		FileOutputStream fos0 = new FileOutputStream(fout0);
		BufferedWriter bwCitation = new BufferedWriter(new OutputStreamWriter(fos0));

		bwCitation.write(
				"writable#numberOfAllFocusNodes#avgOutDegree#allNodesAfterSomeHopsSet.size()#allNodesAfterSomeHops#\n");

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		System.out.println("AvgOutDegrees: " + DummyFunctions.getAvgOutDegrees(dataGraph));

		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			String[] labelPropertyValue = line.split(":")[0].split("#");
			if (labelPropertyValue[0].trim().contains("Author")
					|| labelPropertyValue[0].trim().contains("Publication")) {
				continue;
			}

			File fout = new File(middleFile);
			FileOutputStream fos = new FileOutputStream(fout);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

			String writable = labelPropertyValue[0].trim() + "|" + labelPropertyValue[1].trim() + ":" + "\""
					+ labelPropertyValue[2].trim() + "\"";

			// String writable = line;

			bw.write(writable);
			bw.close();

			System.out.println(writable);
			focusLabelPropValSet.clear();
			allNodesOfFocusType.clear();
			fillSetFromFile(middleFile);
			fillFocusNodesOfRequestedTypes(bwCitation, writable);
			System.out.println();
		}

		br.close();

		tx1.success();

		bwCitation.close();
		dataGraph.shutdown();
		System.out.println("finished!");

	}

	private void fillSetFromFile(String focusSetPath) throws Exception {
		// the format should be like:
		// NodeType | key1:value1, key2:value2
		FileInputStream fis = new FileInputStream(focusSetPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
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
		br.close();
	}

	private void fillFocusNodesOfRequestedTypes(BufferedWriter bwCitation, String writable) throws Exception {

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		double sumAllOutDegree = 0d;
		int maxDegree = 0;
		double avgOutDegree = 0;

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
							if (node.getProperty(pairString.key).toString().toLowerCase()
									.equals(pairString.value.toLowerCase())
									|| DummyFunctions.isContain(
											node.getProperty(pairString.key).toString().toLowerCase(),
											pairString.value.toLowerCase())) {
								allNodesOfFocusType.get(focusLabel).add((int) node.getId());
								sumAllOutDegree += node.getDegree(Direction.OUTGOING);
								maxDegree = Math.max(maxDegree, node.getDegree(Direction.OUTGOING));
								break;
							}
						}

					}
				} else {
					allNodesOfFocusType.get(focusLabel).add((int) node.getId());
				}

			}
		}

		// TraversalDescription graphTraverse =
		// dataGraph.traversalDescription().evaluator(Evaluators.toDepth(2));
		//
		// for (RelationshipType relType : dataGraph.getAllRelationshipTypes())
		// {
		// graphTraverse = graphTraverse.relationships(relType,
		// Direction.OUTGOING);
		// }

		int numberOfAllFocusNodes = 0;
		HashSet<Integer> allNodesAfterSomeHopsSet = new HashSet<Integer>();
		int allNodesAfterSomeHops = 0;
		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				System.err.println(("no items for \"" + key + "\""));
				return;
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
			for (Integer nodeId : allNodesOfFocusType.get(key)) {

				HashSet<Integer> tempNeighbors = new HashSet<Integer>();
				addNextNodesSet(nodeId, nodeId, 2, 0, dataGraph, tempNeighbors);

				// Traverser tr =
				// graphTraverse.traverse(dataGraph.getNodeById(nodeId));

				//
				// for (Node node : tr.nodes()) {
				// tempNeighbors.add(node.getId());
				// }

				allNodesAfterSomeHops += tempNeighbors.size();
				allNodesAfterSomeHopsSet.addAll(tempNeighbors);
			}
		}

		System.out.println("numberOfAllFocusNodes: " + numberOfAllFocusNodes);
		avgOutDegree = sumAllOutDegree / numberOfAllFocusNodes;
		System.out.println("focust avgOutDegree: " + avgOutDegree);
		System.out.println("allNodesAfterSomeHops Distinct: " + allNodesAfterSomeHopsSet.size());
		System.out.println("allNodesAfterSomeHops: " + allNodesAfterSomeHops);

		int maxDegreeOfAllNodes = 0;
		HashSet<String> lbls = new HashSet<String>();
		for (Integer nodeId : allNodesAfterSomeHopsSet) {
			String lbl = dataGraph.getNodeById(nodeId).getLabels().iterator().next().name().toString();
			maxDegreeOfAllNodes = Math.max(dataGraph.getNodeById(nodeId).getDegree(Direction.OUTGOING),
					maxDegreeOfAllNodes);
			if (lbls.add(lbl)) {
				System.out.print(lbl + ", ");
			}
		}

		System.out.println();
		System.out.println("maxDegree of focuses: " + maxDegree);
		System.out.println("maxDegree of all: " + maxDegreeOfAllNodes);
		System.out.println("all related lbls: " + lbls.size());
		System.out.println();

		if (avgOutDegree < 3.5 && avgOutDegree > 0 && lbls.size() < 50 && allNodesAfterSomeHops < 1500
				&& allNodesAfterSomeHopsSet.size() < 500 && maxDegree < 100 && maxDegreeOfAllNodes < 100
				&& allNodesAfterSomeHopsSet.size() > 50) {
			bwCitation.write(writable + "#" + numberOfAllFocusNodes + "#" + avgOutDegree + "#"
					+ allNodesAfterSomeHopsSet.size() + "#" + allNodesAfterSomeHops + "#" + "\n");
			bwCitation.flush();
		}
	}

	private void addNextNodesSet(Integer parentId, Integer thisId, int maxDHops, int currentHop,
			GraphDatabaseService dataGraph, HashSet<Integer> tempNeighbors) {
		// recursion base case: as Integer as we haven't
		// exceeded the max number of hops, keep going
		if (currentHop < maxDHops) {
			Node c;
			for (Relationship r : dataGraph.getNodeById(thisId).getRelationships(Direction.OUTGOING)) {
				c = r.getEndNode();
				int cId = (int) c.getId();
				if (Math.abs(maxDHops) > 1)
					addNextNodesSet(parentId, cId, maxDHops, currentHop + 1, dataGraph, tempNeighbors);

				tempNeighbors.add(cId);
			}
		} else
			return;

	}
}

class PairStrings {
	public String key;
	public String value;

	public PairStrings(String key, String value) {
		this.key = key;
		this.value = value;

	}

}

class PatternsOfTheYear {
	public HashSet<Integer> recomPapers = new HashSet<Integer>();
	public HashSet<Integer> VenueDmSet = new HashSet<Integer>();
	public HashSet<Integer> citingPapers = new HashSet<Integer>();
	public HashSet<Integer> venueWhereIdSet = new HashSet<Integer>();

	public PatternsOfTheYear() {

	}

	public void addPatternsOfTheYear(PatternsOfTheYear pOfYear) {
		this.recomPapers.addAll(pOfYear.recomPapers);
		this.VenueDmSet.addAll(pOfYear.VenueDmSet);
		this.citingPapers.addAll(pOfYear.citingPapers);
		this.venueWhereIdSet.addAll(pOfYear.venueWhereIdSet);

	}

	public void addPatternsOfTheYear(int recomPaper, int venueDm, int citingPaper, int venueWhereId) {
		this.recomPapers.add(recomPaper);
		this.VenueDmSet.add(venueDm);
		this.citingPapers.add(citingPaper);
		this.venueWhereIdSet.add(venueWhereId);
	}
}