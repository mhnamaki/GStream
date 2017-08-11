package src.dataset.dataSnapshot;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.register.Register;

import src.utilities.Dummy.DummyFunctions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This program will convert one big graph database into a smaller database plus
 * multiple additions in the forms of edge streams.
 *
 * @parameter -completeDataGraphPath: the absolute path of the complete
 *            datagraph
 * @parameter -storeDir: the absolute path of the directory to store everything
 * @parameter -addTimeStampKey: the string that is the key to the property
 *            marking the addition timestamp
 * @parameter -deleteTimeStampKey: the string that is the key to the property
 *            marking the deletion timestamp
 * @parameter -timeFormat: a string showing the time format used at the value of
 *            time stamps in the database. Example: "dd-MMM-yyyy"; look at
 *            http://docs.oracle.com/javase/6/docs/api/java/text/
 *            SimpleDateFormat.html
 * @parameter -startingRelationshipCount: the number of relationships in G0
 * @parameter -deltaSize: the number of edge transactions in each delta file
 *
 * @author Shayan Monadjemi
 * @date June 27th, 2016
 */
public class SnapshotCreator {

	String completeDataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/panama_GTAR_V3.0.db";
	String storeDir = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/onlyDeltaEs/";
	//String addTimeStampKey = "Year";
	String addTimeStampKey = "incorporation_date";
	String deleteTimeStampKey = "inactivation_date";
	//String deleteTimeStampKey = "";
	String timeFormat = "dd-MMM-yyyy";
	//String timeFormat = "yyyy";

	private static final Date DEFAULT_MIN_DATE = new Date(-5364633600000l); // 01-JAN-1800
	private static final Date DEFAULT_MAX_DATE = new Date(32503708800000l); // 01-JAN-3000

	static DateFormat dateFormat;

	// static int startingNodeCount;
	int startingRelationshipCount = 0;
	int deltaSize = 1000000;

	// static TreeMap<Integer, ArrayList<Long>> sortedNodesByYear;
	static TreeMap<Date, ArrayList<StreamEdge>> sortedRelsByTime;

	public static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	public SnapshotCreator(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-completeDataGraphPath")) {
				this.completeDataGraphPath = args[++i];
			} else if (args[i].equals("-startingRelationshipCount")) {
				this.startingRelationshipCount = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-storeDir")) {
				this.storeDir = args[++i];
			} else if (args[i].equals("-deltaSize")) {
				this.deltaSize = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-addTimeStampKey")) {
				this.addTimeStampKey = args[++i];
			} else if (args[i].equals("-deleteTimeStampKey")) {
				this.deleteTimeStampKey = args[++i];
			} else if (args[i].equals("-timeFormat")) {
				this.timeFormat = args[++i];
			}
		}
	}

	public SnapshotCreator(String completeDataGraphPath, int startingRelationshipCount, String storeDir, int deltaSize,
			String addTimeStampKey, String deleteTimeStampKey, String timeFormat) {
		this.completeDataGraphPath = completeDataGraphPath;
		this.startingRelationshipCount = startingRelationshipCount;
		this.storeDir = storeDir;
		this.deltaSize = deltaSize;
		this.addTimeStampKey = addTimeStampKey;
		this.deleteTimeStampKey = deleteTimeStampKey;
		this.timeFormat = timeFormat;
	}

	public static void main(String[] args) throws Exception {
		SnapshotCreator snapshotCreator = new SnapshotCreator(args);
		snapshotCreator.run();
	}

	/**
	 * Runs the snapshot creator
	 * 
	 * @throws Exception
	 *             if files do not exist
	 */
	public void run() throws Exception {

		System.out.println("startingRelationshipCount:" + startingRelationshipCount);

		System.out.println("start");

		if (!storeDir.trim().endsWith("/")) {
			this.storeDir = storeDir.trim() + "/";
		}

		System.out.print("Copying database \nFROM: " + completeDataGraphPath + " \nTO: " + storeDir + "g0.graphdb\n");
		try {
			FileUtils.copyRecursively(new File(completeDataGraphPath), new File(storeDir + "g0.graphdb"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		File completeDataGraphFile = new File(storeDir + "g0.graphdb");

		GraphDatabaseService completeDataGraph = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(completeDataGraphFile)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		registerShutdownHook(completeDataGraph);

		dateFormat = new SimpleDateFormat(timeFormat);

		System.out.print("Starting the sortingRelationshipsByYear --- ");
		printDate();

		sortedRelsByTime = getSortedRelationshipsByTimeStamp(completeDataGraph, addTimeStampKey, deleteTimeStampKey);
		System.out.println(sortedRelsByTime.size());

		Iterator<Date> dates = sortedRelsByTime.keySet().iterator();
		while (dates.hasNext()) {
			System.out.println(dates.next());
		}

		System.out.println("Emptying all the edges on complete graph...");

		removeAllRelationships(completeDataGraph);

//		System.out.print("Starting the generateG0 --- ");
//		printDate();
//
//		generateG0Relationships(completeDataGraph, startingRelationshipCount, sortedRelsByTime);
//
//		System.out.print("Starting the generateDelta_N --- ");
//		printDate();

		generateDelta_N_Relationships(sortedRelsByTime, deltaSize, storeDir);

		completeDataGraph.shutdown();

		System.out.print("Successfully finished --- ");
		printDate();

	}

	/**
	 * removes all relationships in dataGraph to prepare for G0
	 * 
	 * @param dataGraph
	 */
	private static void removeAllRelationships(GraphDatabaseService dataGraph) {
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

	/**
	 * prints current date and time
	 */
	private static void printDate() {
		Date dNow = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		System.out.println("Current Date: " + ft.format(dNow));
	}

	/**
	 * this file starts adding a given number of relationships to a graph that
	 * has no relationships (only nodes)
	 * 
	 * @param dataGraph
	 * @param RelationshipCount
	 *            number of relationships to add
	 * @param sortedRelationships
	 *            source of relationships to choose from (earliest to latest,
	 *            sorted by date)
	 * @throws Exception
	 */
	private static void generateG0Relationships(GraphDatabaseService dataGraph, int RelationshipCount,
			TreeMap<Date, ArrayList<StreamEdge>> sortedRelationships) throws Exception {
		int insRelCount = 0;
		int relCount = 0;

		Transaction tx = dataGraph.beginTx();

		ArrayList<StreamEdge> currentYear = sortedRelationships.get(sortedRelationships.keySet().iterator().next());
		StreamEdge currentEdge;

		int edgeInADate = 0;
		while (insRelCount < RelationshipCount) {
			if (currentYear == null || currentYear.size() == edgeInADate) {
				if (sortedRelationships.size() == 1) {
					System.out.println("No more relationships left");
					break;
				} else {
					sortedRelationships.remove(sortedRelationships.keySet().iterator().next());
					currentYear = sortedRelationships.get(sortedRelationships.keySet().iterator().next());
					edgeInADate = 0;
				}
			}

			currentEdge = currentYear.get(edgeInADate++);

			if (currentEdge.isAdded()) {
				dataGraph.getNodeById(currentEdge.getSourceNode()).createRelationshipTo(
						dataGraph.getNodeById(currentEdge.getDestinationNode()),
						RelationshipType.withName(currentEdge.getRelationshipType()));

				insRelCount++;

			} else {
				// TODO: more efficient to use cypher here for edge deletion?
				if (dataGraph.getNodeById(currentEdge.getSourceNode()).getDegree(Direction.OUTGOING) <= dataGraph
						.getNodeById(currentEdge.getDestinationNode()).getDegree(Direction.INCOMING)) {
					for (Relationship r : dataGraph.getNodeById(currentEdge.getSourceNode())
							.getRelationships(Direction.OUTGOING)) {
						if (r.getEndNode().getId() == currentEdge.getDestinationNode()) {
							r.delete();
							break;
						}

					}
				} else {
					for (Relationship r : dataGraph.getNodeById(currentEdge.getDestinationNode())
							.getRelationships(Direction.INCOMING)) {
						if (r.getStartNode().getId() == currentEdge.getSourceNode()) {
							r.delete();
							break;
						}
					}

				}
			}

			relCount++;

			if (relCount % 100000 == 0) {
				// System.out.println("start commit: " +
				// System.currentTimeMillis());
				tx.success();
				tx.close();
				tx = dataGraph.beginTx();
				// System.out.println("end commit: " +
				// System.currentTimeMillis());
			}
			if (relCount % 50000 == 0) {
				System.out.println("relCount: " + relCount + " sortedRelationships: " + sortedRelationships.size());
			}
			if (relCount % 500000 == 0) {
				System.gc();
				System.runFinalization();
				DummyFunctions.sleepAndWakeUp(1000);
			}

		}

		// edgeInADate-1 used;
		System.out.println("currentYearSize: " + currentYear.size() + ", last used edgeInADate:" + (edgeInADate - 1));
		currentYear.subList(0, edgeInADate - 1).clear();
		System.out.println("new currentYearSize: " + currentYear.size());

		tx.success();
		tx.close();

	}

	/**
	 * This method creates files that store edge insertion/deletion
	 * 
	 * @param sortedRelationships
	 * @param howManyRelationshipsPerDelta
	 * @param storePath
	 * @throws Exception
	 *             if files don't exist
	 */
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
					writer.write(/*"+\t" + dateFormat.format(currentEdge.getTimeStamp()) + "\t"
							+ */currentEdge.getSourceNode() + "\t" + currentEdge.getDestinationNode() + "\t"
							+ currentEdge.getRelationshipType() + "\n");
				} else {

					// DO NOT DELETE ALL EDGES AT THE "END OF THE WORLD"
					if (currentEdge.getTimeStamp().compareTo(DEFAULT_MAX_DATE) < 0) {
						writer.write(/*"-\t" + dateFormat.format(currentEdge.getTimeStamp()) + "\t"
								+ */currentEdge.getSourceNode() + "\t" + currentEdge.getDestinationNode() + "\t"
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

	/**
	 * Sorts all relationships in the graph by timestamp
	 * 
	 * @param dataGraph
	 * @param insertionTimeStampKey
	 * @param deletionTimeStampKey
	 * @return a tree map with every 'date' as key, and an arraye list of
	 *         StreamEdges with that date as time stamp
	 */
	private static TreeMap<Date, ArrayList<StreamEdge>> getSortedRelationshipsByTimeStamp(
			GraphDatabaseService dataGraph, String insertionTimeStampKey, String deletionTimeStampKey) {
		TreeMap<Date, ArrayList<StreamEdge>> sortedRels = new TreeMap<>();

		Transaction tx = dataGraph.beginTx();

		for (Relationship r : dataGraph.getAllRelationships()) {
			// System.out.print("size: " + sortedRels.keySet().size());

			/**
			 * insertions
			 */
			Date timeStamp = getTimeStamp(r, insertionTimeStampKey, true);

			// System.out.println(" time: " + timeStamp);
			if (sortedRels.containsKey(timeStamp)) {
				StreamEdge e = new StreamEdge(true, timeStamp, r.getStartNode().getId(), r.getEndNode().getId(),
						r.getType().name());
				sortedRels.get(timeStamp).add(e);
			} else {
				sortedRels.put(timeStamp, new ArrayList<StreamEdge>());
				StreamEdge e = new StreamEdge(true, timeStamp, r.getStartNode().getId(), r.getEndNode().getId(),
						r.getType().name());
				sortedRels.get(timeStamp).add(e);
			}

			/**
			 * deletions
			 */

			if (!deletionTimeStampKey.equals("")) {

				timeStamp = getTimeStamp(r, deletionTimeStampKey, false);
				if (sortedRels.containsKey(timeStamp)) {
					StreamEdge e = new StreamEdge(false, timeStamp, r.getStartNode().getId(), r.getEndNode().getId(),
							r.getType().name());
					sortedRels.get(timeStamp).add(e);
				} else {
					sortedRels.put(timeStamp, new ArrayList<StreamEdge>());
					StreamEdge e = new StreamEdge(false, timeStamp, r.getStartNode().getId(), r.getEndNode().getId(),
							r.getType().name());
					sortedRels.get(timeStamp).add(e);
				}
			}

		}

		tx.success();
		tx.close();

		return sortedRels;
	}

	/**
	 *
	 * @param directoryPath
	 * @throws Exception
	 */
	private static void deleteDirIfExist(String directoryPath) throws Exception {
		java.nio.file.Path directory = Paths.get(directoryPath);
		Files.walkFileTree(directory, new SimpleFileVisitor<java.nio.file.Path>() {
			// @Override
			public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			// @Override
			public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});

	}

	/**
	 * This method gets a relationship and returns the addition/deletion time
	 * stamp. First, it checks if the relationship itself has a property
	 * containing timestamp. if not, it checks the nodes for timestamp
	 * information. if not, it will return the default minimum(maximum)
	 * timestamp for deletion(addition).
	 * 
	 * @param r
	 *            the relationship
	 * @param timeStampPropertyKey
	 * @param edgeAddition
	 *            boolean true if addition, false if deletion
	 * @return the timestamp
	 */
	static Date getTimeStamp(Relationship r, String timeStampPropertyKey, boolean edgeAddition) {

		Date t1, t2;

		if (edgeAddition) {
			t1 = DEFAULT_MIN_DATE;
			t2 = DEFAULT_MIN_DATE;
		} else {
			t1 = DEFAULT_MAX_DATE;
			t2 = DEFAULT_MAX_DATE;
		}

		if (r.hasProperty(timeStampPropertyKey)) {
			try {
				t1 = dateFormat.parse(r.getProperty(timeStampPropertyKey).toString());
				return t1;
			} catch (Exception e) {
				if (edgeAddition)
					return getLaterDate(t1, t2);
				else
					return getEarlierDate(t1, t2);
			}
		} else {
			if (r.getStartNode().getAllProperties().keySet().contains(timeStampPropertyKey)) {
				try {
					t1 = dateFormat.parse(r.getStartNode().getProperty(timeStampPropertyKey).toString());
				} catch (Exception e) {
					if (r.getStartNode().hasProperty(timeStampPropertyKey))
						System.err.println("ERROR " + r.getStartNode().getProperty(timeStampPropertyKey).toString());
				}
			}

			if (r.getEndNode().getAllProperties().keySet().contains(timeStampPropertyKey)) {
				try {
					t2 = dateFormat.parse(r.getEndNode().getProperty(timeStampPropertyKey).toString());
				} catch (Exception e) {
					if (r.getEndNode().hasProperty(timeStampPropertyKey))
						System.err.println("ERROR " + r.getEndNode().getProperty(timeStampPropertyKey).toString());
				}
			}

			if (edgeAddition)
				return getLaterDate(t1, t2);
			else
				return getEarlierDate(t1, t2);
		}

	}

	/**
	 * This method is used to compare two dates and get the one that happened
	 * before the other
	 * 
	 * @param d1
	 * @param d2
	 * @return the earlier one
	 */
	private static Date getEarlierDate(Date d1, Date d2) {
		if (d1.compareTo(d2) < 0)
			return d1;
		else
			return d2;
	}

	/**
	 * This method is used to compare two dates and get the one that happened
	 * after the other
	 * 
	 * @param d1
	 * @param d2
	 * @return the later one
	 */
	private static Date getLaterDate(Date d1, Date d2) {
		if (d1.compareTo(d2) > 0)
			return d1;
		else
			return d2;
	}

}
