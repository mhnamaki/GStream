package src.dataset.dataSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;

public class SnapshotCreatorBasedOnEdgeTime {
	private String completeDataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Summer2017/GStreamICDE18/IDSDB/ids17.db";
	private String storeDir = "/Users/mnamaki/Documents/Education/PhD/Summer2017/GStreamICDE18/IDSDB/deltas/";
	private String timeFormat = "yyyy-MM-dd'T'hh:mm:ss";
	private static SimpleDateFormat dateFormat;
	private static final Date DEFAULT_MIN_DATE = new Date(-5364633600000l); // 01-JAN-1800
	private static final Date DEFAULT_MAX_DATE = new Date(32503708800000l); // 01-JAN-3000

	static TreeMap<Date, ArrayList<StreamEdge>> sortedRelsByTime;
	int startingRelationshipCount = 0;
	int deltaSize = 500000;

	private void run() throws Exception {

		System.out.print("Copying database \nFROM: " + completeDataGraphPath + " \nTO: " + storeDir + "g0.graphdb\n");

		FileUtils.copyRecursively(new File(completeDataGraphPath), new File(storeDir + "g0.graphdb"));

		File completeDataGraphFile = new File(storeDir + "g0.graphdb");

		GraphDatabaseService completeDataGraph = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(completeDataGraphFile)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		dateFormat = new SimpleDateFormat(timeFormat);

		System.out.print("Starting the sortingRelationshipsByYear --- ");
		printDate();

		sortedRelsByTime = getSortedRelationshipsByTimeStamp(completeDataGraph);
		System.out.println("sortedRelsByTime size:" + sortedRelsByTime.size());

		System.out.println("Emptying all the edges on complete graph...");

		removeAllRelationships(completeDataGraph);

		System.out.print("Starting the generateDelta_N --- ");
		printDate();

		generateDelta_N_Relationships(sortedRelsByTime, deltaSize, storeDir);

		completeDataGraph.shutdown();

		System.out.print("Successfully finished --- ");
		printDate();

	}

	private static TreeMap<Date, ArrayList<StreamEdge>> getSortedRelationshipsByTimeStamp(
			GraphDatabaseService dataGraph) throws Exception {

		TreeMap<Date, ArrayList<StreamEdge>> sortedRels = new TreeMap<>();

		Transaction tx = dataGraph.beginTx();

		for (Relationship r : dataGraph.getAllRelationships()) {

			Date rDate = null;
			if (!r.hasProperty("startTime") && !r.hasProperty("endTime")) {
				rDate = DEFAULT_MIN_DATE;
				StreamEdge e = new StreamEdge(true, rDate, r.getStartNode().getId(), r.getEndNode().getId(),
						r.getType().name());
				sortedRels.putIfAbsent(rDate, new ArrayList<>());
				sortedRels.get(rDate).add(e);
			} else {

				if (r.hasProperty("startTime")) {
					rDate = dateFormat.parse(r.getProperty("startTime").toString());
					StreamEdge e = new StreamEdge(true, rDate, r.getStartNode().getId(), r.getEndNode().getId(),
							r.getType().name());
					sortedRels.putIfAbsent(rDate, new ArrayList<>());
					sortedRels.get(rDate).add(e);
				}
				if (r.hasProperty("endTime")) {
					rDate = dateFormat.parse(r.getProperty("endTime").toString());
					StreamEdge e = new StreamEdge(false, rDate, r.getStartNode().getId(), r.getEndNode().getId(),
							r.getType().name());
					sortedRels.putIfAbsent(rDate, new ArrayList<>());
					sortedRels.get(rDate).add(e);
				}
			}
		}

		tx.success();
		tx.close();

		return sortedRels;
	}

	private void removeAllRelationships(GraphDatabaseService dataGraph) {
		Transaction tx = dataGraph.beginTx();
		int count = 0;

		for (Relationship r : dataGraph.getAllRelationships()) {

			if (count % 10000 == 0) {
				tx.success();
				tx.close();
				tx = dataGraph.beginTx();
			}

			r.delete();

			count++;
		}
		tx.success();
		tx.close();
	}

	private void printDate() {
		Date dNow = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		System.out.println("Current Date: " + ft.format(dNow));
	}

	private static void generateDelta_N_Relationships(TreeMap<Date, ArrayList<StreamEdge>> sortedRelationships,
			int howManyRelationshipsPerDelta, String storePath) throws Exception {
		int deltaNumber = 0;
		int count = 0;

		Date currentTimeStamp = sortedRelationships.keySet().iterator().next();

		ArrayList<StreamEdge> currentYear = sortedRelationships.get(currentTimeStamp);

		StreamEdge currentEdge;

		int edgeInADate = 0;

		while (true) {
			if (sortedRelationships.size() == 0) {
				break;
			}

			String fileName = String.format("Delta%03d.txt", deltaNumber);
			File file = new File(storePath + "/" + fileName);
			FileWriter writer = new FileWriter(file);

			while (count < howManyRelationshipsPerDelta) {
				if (currentYear.size() == edgeInADate) {
					if (sortedRelationships.size() == 1) {
						System.out.println("No more relationships left");
						sortedRelationships.remove(currentTimeStamp);
						break;
					} else {
						sortedRelationships.remove(currentTimeStamp);
						currentTimeStamp = sortedRelationships.keySet().iterator().next();
						currentYear = sortedRelationships.get(currentTimeStamp);
						edgeInADate = 0;
					}
				}

				currentEdge = currentYear.get(edgeInADate++);

				if (currentEdge.isAdded()) {
					writer.write("+," + dateFormat.format(currentEdge.getTimeStamp()) + ","
							+ currentEdge.getSourceNode() + "," + currentEdge.getDestinationNode() + ","
							+ currentEdge.getRelationshipType() + "\n");
				} else {

					// DO NOT DELETE ALL EDGES AT THE "END OF THE WORLD"
					if (currentEdge.getTimeStamp().compareTo(DEFAULT_MAX_DATE) < 0) {
						writer.write("-," + dateFormat.format(currentEdge.getTimeStamp()) + ","
								+ currentEdge.getSourceNode() + "," + currentEdge.getDestinationNode() + ","
								+ currentEdge.getRelationshipType() + "\n");
					}
				}
				count++;
			}

			// System.out.println("relCount < RelationshipCount: " + count + ",
			// " + howManyRelationshipsPerDelta);

			count = 0;

			writer.close();
			deltaNumber++;
			if (deltaNumber % 100 == 0) {
				System.out.println(
						deltaNumber + "Deltas created\nRemaining keys: " + sortedRelationships.keySet().size());
			}
		}

		System.out.println("Successfully created " + (deltaNumber + 1) + " deltas");
	}

	public static void main(String[] args) throws Exception {

		SnapshotCreatorBasedOnEdgeTime ss = new SnapshotCreatorBasedOnEdgeTime();
		ss.run();
	}

}
