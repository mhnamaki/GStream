package src.dataset.syntheticGraphCreator;

import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.stream.Sink;
import org.graphstream.stream.SinkAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * This program gets some information such as the number of nodes, number of
 * node/edge labels, proportion on edges in G0, and creates a neo4j data graph,
 * G0, and Deltas (for stream graphs). Graph generation is done using jgrapht's
 * scale free network tools. Created by shayan on 7/12/16.
 *
 * @parameter -numberOfNodes: The integer number of nodes for the generated
 *            synthetic graph [USE ONLY ONE: numberOfNodes OR numberOfRelationships]
 * @parameter -numberOfRelationships: The integer number of relationships for
 *            the generated synthetic graph [USE ONLY ONE: numberOfNodes OR numberOfRelationships]
 * @parameter -numberOfNodeLabels: The integer number of maximum node labels
 *            present in the graph
 * @parameter -numberOfRelationshipTypes: The integer number of maximum
 *            relationship types present in the graph
 * @parameter -neo4jPath: installation path for neo4j (because we need to call
 *            neo4j-import to create graph)
 * @parameter -storeDir: the directory to save everything
 * @parameter -labelWordsPath: path to a text file that contains words,
 *            separated by '\n'
 *
 * @author Shayan Monadjemi
 * @email sxm137031@utdallas.edu
 * @date August 15th, 2016
 */


public class StreamGraphSyntheticGenerator {

    static int nodesAdded;
    static int edgesAdded;

    final static String DATE_FORMAT = "yyyy-MMM-dd-HH:mm:ss.SSS"; // format of timestamp
    final static String ADD_TIMESTAMP_KEY = "addTime";
    final static String DELETE_TIMESTAMP_KEY = "deleteTime";


    public static void main (String[] args) throws Exception {

        int maxEdgePerEvent = 2;
        int numberOfRelationships = 0;
        int numberOfNodes = 0;
        int numberOfNodeLabels = 0;
        int numberOfRelationshipTypes = 0;

        nodesAdded = 0;
        edgesAdded = 0;

        String storeDir="";
        // words to choose from as labels, separated by \n
        String labelWordsPath = "/home/shayan/Documents/WSU/TopKGStream/words.txt";
        String neo4jPath = "/home/shayan/Programs/neo4j-community-3.0.3";




        /**
         * check for arguments here
         */

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-labelWordsPath")) {
                labelWordsPath = args[++i];
            } else if (args[i].equals("-numberOfNodes")) {
                numberOfNodes = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-numberOfNodeLabels")) {
                numberOfNodeLabels = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-numberOfRelationshipTypes")) {
                numberOfRelationshipTypes = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-neo4jPath")) {
                neo4jPath = args[++i];
                if (!neo4jPath.endsWith("/")) {
                    neo4jPath += "/";
                }
            } else if (args[i].equals("-storeDir")) {
                storeDir = args[++i];
                if (!storeDir.endsWith("/")) {
                    storeDir += "/";
                }
            } else if (args[i].equals("-numberOfRelationships")) {
                numberOfRelationships = Integer.parseInt(args[++i]);
            }

        }

        if((numberOfNodes == 0 && numberOfRelationships == 0) || (numberOfNodes != 0 && numberOfRelationships != 0) ||
               labelWordsPath.equals("") || storeDir.equals("") || neo4jPath.equals("") || numberOfNodeLabels == 0 ||
                numberOfRelationshipTypes == 0)

        {
            throw new Exception("ERROR: Wrong or not enough parameters given \n Usage: -labelWordsPath -storeDir -neo4jPath " +
                    "[-numberOfRelationships OR -numberOfNodes] -numberOfNodeLabels -numberOfRelationshipTypes");
        }


        /**
         * Graph Generation Begins Here
         */

        String completeDataGraphPath = storeDir + "completeGraph.graphdb";
        String nodesPath = storeDir + "tempNodes.csv";
        String relationshipsPath = storeDir + "tempRelationships.csv";
        String addFilePath = storeDir + "tempAdd.txt";


        File nodeFile = new File(nodesPath);
        File relationshipFile = new File(relationshipsPath);
        File addTime = new File(addFilePath);

        FileWriter nodeWriter = new FileWriter(nodeFile);
        FileWriter relationshipWriter = new FileWriter(relationshipFile);
        FileWriter addTimeWriter = new FileWriter(addTime);

        ArrayList<String> wordsList = getAllPossibleWords(labelWordsPath);
        System.out.println("all word size: " + wordsList.size());

        String[] nodeLabels = populateWordList(wordsList, numberOfNodeLabels);
        String[] relationshipTypes = populateWordList(wordsList, numberOfRelationshipTypes);

        System.out.print("Generating scale free graph...\n");

        nodeWriter.write("unique:ID,:LABEL\n");
        String firstRelFileLine = ":START_ID,:END_ID,:TYPE" + "," + ADD_TIMESTAMP_KEY +"," + DELETE_TIMESTAMP_KEY + "\n";

        relationshipWriter.write(firstRelFileLine);

        Sink s = new SinkAdapter(){
            @Override
            public void edgeAdded(String sourceId, long timeId, String edgeId, String fromNodeId, String toNodeId, boolean directed){
//                System.out.printf("Edge %s added from node %s to node %s%n", edgeId,fromNodeId, toNodeId);
                try {
                    relationshipWriter.write(fromNodeId + "," + toNodeId + "\n");
                    addTimeWriter.write(System.currentTimeMillis() + "\n");
                    incEdge();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void nodeAdded(String s, long l, String s1)
            {
//                System.out.printf("Node %s added\n", s1);
                try {
                    nodeWriter.write(s1 + "\n" );
                    incNode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };


        Generator generator = new BarabasiAlbertGenerator(maxEdgePerEvent);

        generator.addSink(s);
        generator.begin();

        if(numberOfRelationships != 0) {
            while (edgesAdded < numberOfRelationships) {
                generator.nextEvents();

                if (edgesAdded % 100000 == 0) {
                    System.out.printf("edgesAdded = %d; nodesAdded = %d\n", edgesAdded, nodesAdded);
                }
            }
        } else if (numberOfNodes != 0) {
            while (nodesAdded < numberOfNodes) {
                generator.nextEvents();

                if (nodesAdded % 100000 == 0) {
                    System.out.printf("edgesAdded = %d; nodesAdded = %d\n", edgesAdded, nodesAdded);
                }
            }
        }

        long endTime = System.currentTimeMillis();      //Value used for random deleteTime generation


        System.out.printf("nodesAdded = %d; edgesAdded = %d\n", nodesAdded, edgesAdded);


        relationshipWriter.close();
        nodeWriter.close();
        addTimeWriter.close();

        HashMap<Integer, String> nodeLabelMapping = designateNodeLabels(nodeFile, nodeLabels);
        designateRelationshipTypesAndTimeStamp(nodeLabelMapping, nodeLabels, relationshipTypes,
                relationshipFile, addTime, endTime);


        nodeFile.delete();
        relationshipFile.delete();
        addTime.delete();

        nodeFile = new File(storeDir + "nodes.csv");
        relationshipFile = new File(storeDir + "relationships.csv");

        System.out.print("neo4j import files created.\nCreating neo4j dataGraph...\n");

        //Creating a README File
        File readMe = new File(storeDir + "README.txt");
        FileWriter readMeWriter = new FileWriter(readMe);
        readMeWriter.write("Synthetic Graph Generator (Using GraphStream Library)\n");
        readMeWriter.write("Number of Nodes = " + nodesAdded + "\n");
        readMeWriter.write("Number of Relationships = " + edgesAdded + "\n");
        readMeWriter.write("Number of Distinct Node Labels = " + numberOfNodeLabels + "\n");
        readMeWriter.write("Number of Distinct Relationship Types = " + numberOfRelationshipTypes + "\n");
        readMeWriter.write("Time Format = " + DATE_FORMAT + "\n");
        readMeWriter.write("Add Timestamp Key = " + ADD_TIMESTAMP_KEY + "\n");
        readMeWriter.write("Delete Timestamp Key = " + DELETE_TIMESTAMP_KEY + "\n");
        readMeWriter.close();

        /**
         * Graph Generation ENDS here
         */


        /**
         * Importing to Neo4j
         */

        String neo4jCommand = neo4jPath + "/bin/neo4j-import --into " + completeDataGraphPath + " --nodes "
                + nodeFile.getAbsolutePath() + " --relationships " + relationshipFile.getAbsolutePath();

        Process p = Runtime.getRuntime().exec(neo4jCommand);
        p.waitFor();
        int exitVal = p.exitValue();

        System.out.println("proc.exitValue(): " + exitVal);
        if (exitVal == 0)
            System.out.println("Neo4j imported properly!");
        else {
            System.out.println("ERROR: Neo4j messed up");
            System.exit(1);
        }










    }

    private static void incNode(){
        nodesAdded++;
    }
    private static void incEdge(){
        edgesAdded++;
    }


    /**
     * This method reads a file containing words with delimeter '\n', and adds
     * all the valid alphabetic words to an ArrayList.
     *
     * @param filePath
     *            the .txt file containing the words
     * @return an ArrayList with all the words in it
     * @throws Exception
     *             in case the file does not exist
     */
    public static ArrayList<String> getAllPossibleWords(String filePath) throws Exception {
        ArrayList<String> wordsList = new ArrayList<String>();
        HashSet<String> wordsSeenSoFar = new HashSet<String>();
        FileReader fileReader = new FileReader(filePath);

        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim().replaceAll("[^A-Za-z0-9 ]", "");
            if (!wordsSeenSoFar.contains(line) && line.length() > 4) {
                wordsList.add(line);
                wordsSeenSoFar.add(line);
            }
        }
        bufferedReader.close();
        return wordsList;
    }

    /**
     * Populated an array of given size with words from our word bank. The words
     * added to the array will be removed from the arraylist to prevent repeated
     * labels.
     *
     * @param fullList
     *            word bank
     * @param num
     *            size of the returned array (number of needed words)
     * @return an array of num words
     */
    public static String[] populateWordList(ArrayList<String> fullList, int num) {
        String[] newList = new String[num];
        Random rnd = new Random();
        HashSet<Integer> seenIndices = new HashSet<Integer>();

        for (int i = 0; i < newList.length; i++) {

            int rndVal = rnd.nextInt(fullList.size());

            while (seenIndices.contains(rndVal)) {
                rndVal = rnd.nextInt(fullList.size());
            }

            seenIndices.add(rndVal);

            newList[i] = fullList.get(rndVal);

            fullList.remove(rndVal);

        }

        return newList;
    }

    public static int getIndexForRandomNumber(double random, double[] probabilityRanges) {

        if (random >= 0 && random < probabilityRanges[0]) {
            return 0;
        } else if (random >= probabilityRanges[probabilityRanges.length - 1] && random < 1) {
            return probabilityRanges.length - 1;
        } else {
            for (int i = 1; i < probabilityRanges.length - 1; i++) {
                if (random >= probabilityRanges[i - 1] && random < probabilityRanges[i]) {
                    return i;
                }
            }
        }

        return 0;

    }

    private static HashMap<Integer, String> designateNodeLabels(File tempNode, String[] allLabels) throws Exception{
        HashMap<Integer, String> nodeLabelMapping = new HashMap<>();

        double[] probs = new double[allLabels.length];
        double[] probabilityRanges = new double[allLabels.length];
        for (int i = 0; i < probabilityRanges.length; i++) {
            double current = Math.pow(2, (-1.0 * (i + 1)));
            probs[i] = current;
            double sum = current;
            for (int j = 0; j < i; j++) {
                sum += probs[j];
            }
            probabilityRanges[i] = sum;
        }

        String tempNodePath = tempNode.getPath();
        String nodePath = tempNodePath.replace(tempNodePath.split("/")[tempNodePath.split("/").length-1], "nodes.csv");
        File node = new File(nodePath);
        FileWriter nodeWriter = new FileWriter(node);

        Scanner read = new Scanner(tempNode);

        nodeWriter.write(read.nextLine() + "\n");

        int currentIndex = allLabels.length - 1;
        String nodeLabel;
        String nextLine;
        while(read.hasNextLine())
        {
            if (currentIndex > 0) {
                nodeLabel = allLabels[currentIndex--];
            } else {
                double random = Math.random();
                int vi = getIndexForRandomNumber(random, probabilityRanges);
                nodeLabel = allLabels[vi];

            }

            nextLine = read.nextLine();
            nodeWriter.write(nextLine + "," + nodeLabel + "\n");

            nodeLabelMapping.put(Integer.parseInt(nextLine.replace(",", "")), nodeLabel);

        }





        nodeWriter.close();

        return nodeLabelMapping;
    }


    private static void designateRelationshipTypesAndTimeStamp(HashMap<Integer, String> nodeLabelMapping,
                                                               String[] nodeLabels, String[] relationshipTypes,
                                                               File tempRelationships, File addTime,
                                                               long endTime) throws Exception{

        String tempRelationshipsPath = tempRelationships.getPath();
        String relationshipsPath = tempRelationshipsPath.replace(tempRelationshipsPath.split("/")[tempRelationshipsPath.split("/").length-1], "relationships.csv");
        File relationships = new File(relationshipsPath);
        FileWriter relationshipsWriter = new FileWriter(relationships);

        Scanner read = new Scanner(tempRelationships);
        Scanner readAddTime = new Scanner(addTime);

        relationshipsWriter.write(read.nextLine() + "\n");

        HashMap<String, String> relTypes = new HashMap<>();

        int currentIndex = relationshipTypes.length - 1;

        for (int i = 0; i < nodeLabels.length; i++) {
            for (int j = 0; j < nodeLabels.length; j++) {
                String relType;
                if (currentIndex > 0) {
                    relType = relationshipTypes[currentIndex];
                    currentIndex--;
                } else {
                    int ei = (int) (Math.random() * relationshipTypes.length);
                    relType = relationshipTypes[ei];
                }

                String n = nodeLabels[i] + "_" + nodeLabels[j];
                relTypes.put(n, relType);
            }
        }


        String nextLine;
        String[] nextLineParts;

        long addedTime;
        long deletedTime;
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        while(read.hasNextLine())
        {
            nextLine = read.nextLine();
            nextLineParts = nextLine.split(",");

            String relType = relTypes.get(nodeLabelMapping.get(Integer.parseInt(nextLineParts[0])) + "_" + nodeLabelMapping.get(Integer.parseInt(nextLineParts[1])));
            addedTime = Long.parseLong(readAddTime.nextLine());
            deletedTime = (long) (Math.random() * (endTime - addedTime)) + addedTime;


            relationshipsWriter.write(nextLine + "," + relType + "," + dateFormat.format(addedTime) + "," +
                    dateFormat.format(deletedTime) + "\n");
        }

        relationshipsWriter.close();


    }


}
