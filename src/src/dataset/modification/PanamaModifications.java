package src.dataset.modification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import scala.Dynamic;

public class PanamaModifications {

	public static void main(String[] args) throws Exception {

		PanamaModifications pm = new PanamaModifications();
		// pm.addContinents();

		pm.findHighDegreeNodes();
		// pm.removeSource();
		// pm.updateCountryNodeLabel();

	}

	private void findHighDegreeNodes() {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v4.0_continent.graphdb";

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataGraphPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		try (Transaction tx1 = dataGraph.beginTx()) {
			for (Node node : dataGraph.getAllNodes()) {
				if (node.getDegree() > 100000) {
					System.out.println(node.getId() + ", deg: " + node.getDegree() + " , label:"
							+ node.getLabels().iterator().next().name() + " prop: " + node.getAllProperties());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		dataGraph.shutdown();
	}

	private void removeSource() {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v4.0_continent.graphdb";

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataGraphPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		try (Transaction tx1 = dataGraph.beginTx()) {

			int cnt = 0;
			for (Node node : dataGraph.getAllNodes()) {
				if (node.getLabels().iterator().next().name().toLowerCase().equals("source")) {
					for (Relationship r : node.getRelationships()) {
						r.delete();
						cnt++;
					}
					System.out.println(node.getId() + " " + node.getAllProperties());
					node.delete();

				}
			}
			System.out.println("cnt : " + cnt);
			tx1.success();
			//tx1.close();
		} catch (

		Exception e) {
			e.printStackTrace();
		}

		dataGraph.shutdown();

	}

	private void updateCountryNodeLabel() throws Exception {

		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v3.0_continent.graphdb";

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataGraphPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		try (Transaction tx1 = dataGraph.beginTx()) {

			for (Node node : dataGraph.getAllNodes()) {
				if (node.getLabels().iterator().next().name().toLowerCase().contains("country_")) {
					System.out.println(node.getLabels().iterator().next().name().toLowerCase());
					node.removeLabel(Label.label(node.getLabels().iterator().next().name()));
					node.addLabel(Label.label("Country"));

				}
			}

			tx1.success();
			// tx1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		dataGraph.shutdown();
	}

	private void addContinents() throws Exception {
		String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v3.0_continent.graphdb";

		HashMap<String, HashSet<String>> countriesContinents = new HashMap<String, HashSet<String>>();

		FileInputStream fis = new FileInputStream("contriesWithContinent.txt");

		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		String currentContinent = null;

		while ((line = br.readLine()) != null) {
			if (line.contains("[")) {
				currentContinent = line.replace("[", "").replace("]", "").trim();
				countriesContinents.put(currentContinent, new HashSet<String>());
				continue;
			}

			if (currentContinent != null) {
				countriesContinents.get(currentContinent).add(line.toLowerCase().trim());
			}

		}

		br.close();

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataGraphPath))
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

		try (Transaction tx1 = dataGraph.beginTx()) {

			HashMap<String, Long> continentsNodeMap = new HashMap<String, Long>();
			for (String continent : countriesContinents.keySet()) {
				Node node = dataGraph.createNode();
				node.addLabel(Label.label(continent));
				continentsNodeMap.put(continent, node.getId());
			}

			int entities = 0;
			int countries = 0;
			HashSet<Long> linkedNode = new HashSet<Long>();
			for (String continent : countriesContinents.keySet()) {
				System.out.println(continent + "\n");
				for (Node node : dataGraph.getAllNodes()) {

					if (linkedNode.contains(node.getId())) {
						continue;
					}

					if (node.getLabels().iterator().next().name().toLowerCase().equals("entity")
							&& node.hasProperty("countries")) {
						String countryOfEntity = node.getProperty("countries").toString().toLowerCase().trim();
						if (countriesContinents.get(continent).contains(countryOfEntity)) {
							dataGraph.getNodeById(continentsNodeMap.get(continent)).createRelationshipTo(node,
									RelationshipType.withName("continent_Of"));

							entities++;
							linkedNode.add(node.getId());

							for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
								Node countryNode = rel.getOtherNode(node);
								if (linkedNode.contains(countryNode.getId())) {
									continue;
								}
								if (countryNode.getLabels().iterator().next().name().toLowerCase()
										.contains("country_")) {
									dataGraph.getNodeById(continentsNodeMap.get(continent)).createRelationshipTo(
											countryNode, RelationshipType.withName("continent_Of"));

									countries++;
									linkedNode.add(countryNode.getId());
									System.out.println(continent + " : " + countryOfEntity + " entityNode: "
											+ node.getId() + " , countryNode: " + countryNode.getId());
								}
							}

						}

					}
				}
			}

			System.out.println("ent: " + entities);
			System.out.println("countr: " + countries);

			tx1.success();
			// tx1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
