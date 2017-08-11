package src.dataset.ImportCitation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import edu.stanford.nlp.util.StringUtils;
import src.utilities.Dummy.DummyFunctions;
import src.utilities.Stemmer;

public class AddKeywords {
	public static GraphDatabaseService dataGraph;
	public String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/citationV1/citation.graphdb";
	public HashSet<String> stopWords = new HashSet<String>();
	HashMap<String, KeywordInfo> allKeywordsFrequencies = new HashMap<String, KeywordInfo>();

	public HashMap<String, KeywordInfo> preparedKeywords = new HashMap<String, KeywordInfo>();

	public static void main(String[] args) throws Exception {

		AddKeywords ak = new AddKeywords();
		ak.init();

		Transaction tx1 = dataGraph.beginTx();
		// ak.findKeywords();
		ak.readPreparedKeywords(
				"/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/citationKeywords.txt");

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void readPreparedKeywords(String path) throws Exception {
		FileInputStream fis = new FileInputStream(path);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		while ((line = br.readLine()) != null) {
			preparedKeywords.putIfAbsent(line.trim().toLowerCase(), new KeywordInfo());
		}

		br.close();

		for (Node node : dataGraph.getAllNodes()) {
			if (node.getLabels().iterator().next().name().equals("Paper")) {
				String title = node.getProperty("Title").toString();
				StringTokenizer st = new StringTokenizer(title);
				while (st.hasMoreTokens()) {
					String keyword = st.nextToken().trim().toLowerCase();
					if (isCorrectKeywordToAdd(keyword)) {
						String preparedKeyword = getPreparedKeyword(keyword);

						if (preparedKeywords.containsKey(preparedKeyword)) {
							preparedKeywords.get(preparedKeyword).addNodeId(node.getId());
						}
					}
				}
			}
		}

		HashMap<String, Node> newNodeMap = new HashMap<String, Node>();

		for (String keyword : preparedKeywords.keySet()) {
			Node newNode = dataGraph.createNode(Label.label(keyword));
			newNodeMap.put(keyword, newNode);
		}

		for (String keyword : preparedKeywords.keySet()) {
			for (Integer paperNodeId : preparedKeywords.get(keyword).nodeIds) {
				dataGraph.getNodeById(paperNodeId).createRelationshipTo(newNodeMap.get(keyword),
						RelationshipType.withName("hasKeyword"));
				System.out.println(paperNodeId + "->" + newNodeMap.get(keyword).getId());
			}
		}

	}

	private void init() throws Exception {
		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		fillStopWordsSet();

	}

	private void fillStopWordsSet() throws Exception {
		FileInputStream fis = new FileInputStream("stopwords_en.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		while ((line = br.readLine()) != null) {
			stopWords.add(line.trim().toLowerCase());
		}

		br.close();
	}

	public void findKeywords() {

		for (Node node : dataGraph.getAllNodes()) {
			if (node.getLabels().iterator().next().name().equals("Paper")) {
				String title = node.getProperty("Title").toString();
				StringTokenizer st = new StringTokenizer(title);
				while (st.hasMoreTokens()) {
					String keyword = st.nextToken().trim().toLowerCase();
					if (isCorrectKeywordToAdd(keyword)) {
						String preparedKeyword = getPreparedKeyword(keyword);
						allKeywordsFrequencies.putIfAbsent(preparedKeyword, new KeywordInfo());
						allKeywordsFrequencies.get(preparedKeyword).addNodeId(node.getId());
					}
				}
			}
		}
		for (String keyword : allKeywordsFrequencies.keySet()) {
			System.out.println(keyword + "#" + allKeywordsFrequencies.get(keyword).nodeIds.size());
		}
	}

	private boolean isCorrectKeywordToAdd(String keyword) {
		if (keyword.length() < 3)
			return false;
		if (stopWords.contains(keyword))
			return false;

		if (!StringUtils.isAlpha(keyword))
			return false;

		return true;
	}

	private String getPreparedKeyword(String keyword) {
		Stemmer stemmer = new Stemmer();
		keyword = stemmer.stem(keyword);

		return keyword;
	}

}

class KeywordInfo {
	HashSet<Integer> nodeIds = new HashSet<Integer>();

	public KeywordInfo() {

	}

	public void addNodeId(Long nodeId) {
		nodeIds.add(Math.toIntExact(nodeId));
	}
}