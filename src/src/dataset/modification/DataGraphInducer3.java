package src.dataset.modification;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import org.neo4j.cypher.internal.compiler.v2_3.commands.expressions.Property;
import org.neo4j.cypher.internal.frontend.v2_3.ast.functions.Str;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class DataGraphInducer3 {

    private static String dataGraphPath;
    private static int maxNumberOfNodes;
    private static String neo4jImportPath;
    private static String nodesRelFileDir;
    private static String futureDbPath;
    private static int maxDegree;
    private static String[] importantPropertyKeys = {"incorporation_date", "inactivation_date", "name"};

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-dataGraphPath")) {
                dataGraphPath = args[++i];
            } else if (args[i].equals("-maxNumberOfNodes")) {
                maxNumberOfNodes = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-neo4jImportPath")) {
                neo4jImportPath = args[++i];
                if (!neo4jImportPath.endsWith("/")) {
                    neo4jImportPath += "/";
                }
            } else if (args[i].equals("-nodesRelFileDir")) {
                nodesRelFileDir = args[++i];
                if (!nodesRelFileDir.endsWith("/")) {
                    nodesRelFileDir += "/";
                }
            } else if (args[i].equals("-futureDbPath")) {
                futureDbPath = args[++i];
            } else if (args[i].equals("-maxDegree")) {
                maxDegree = Integer.parseInt(args[++i]);
            }


        }

        if (dataGraphPath == null || maxNumberOfNodes == 0 || futureDbPath == null || nodesRelFileDir == null
                || neo4jImportPath == null) {
            throw new Exception("Input parameters needed!");
        }

        GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataGraphPath))
                .setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

        try (Transaction tx1 = dataGraph.beginTx()) {
            HashSet<String> distinctLabels = new HashSet<String>();
            ArrayList<Long> allNodes = new ArrayList<Long>();
            HashSet<Long> visitedNodes = new HashSet<Long>();
            HashMap<String, HashSet<String>> properties = new HashMap<>();

            HashMap<String, File> files = new HashMap<>();
            HashMap<String, FileWriter> writers = new HashMap<>();

            for(Node node : dataGraph.getAllNodes())
            {
                String label = getAlphabeticOnly(node.getLabels().iterator().next().name());

                if(properties.keySet().contains(label))
                {
                    for(String propertyKey : node.getAllProperties().keySet())
                    {
                        if(!properties.get(label).contains(propertyKey))
                        {
                            properties.get(label).add(propertyKey);
                        }
                    }
                }
                else
                {
                    properties.put(label, new HashSet<>());
                    for(String propertyKey : node.getAllProperties().keySet())
                    {
                        if(!properties.get(label).contains(propertyKey))
                        {
                            properties.get(label).add(propertyKey);
                        }
                    }
                }
            }

            for (Node node : dataGraph.getAllNodes()) {
                allNodes.add(node.getId());
                for (Label lbl : node.getLabels()) {
                    distinctLabels.add(lbl.name());
                }
            }

//            File foutNodeType = new File("nodes.txt");
//            FileOutputStream fosNodeType = new FileOutputStream(foutNodeType);
//            BufferedWriter bwNodeType = new BufferedWriter(new OutputStreamWriter(fosNodeType));
//            // TODO: handling properties, we have to have different files then
//            // we can have the different properties
//            bwNodeType.write("unique:ID,:Label");
//            bwNodeType.newLine();

            HashSet<String> distinctRelTypes = new HashSet<String>();
            for (Relationship relationship : dataGraph.getAllRelationships()) {
                distinctRelTypes.add(relationship.getType().name());
            }

            File foutRel = new File(nodesRelFileDir +"Relationships.txt");
            FileOutputStream fosRel = new FileOutputStream(foutRel);
            BufferedWriter bwRel = new BufferedWriter(new OutputStreamWriter(fosRel));
            bwRel.write(":START_ID,:END_ID,:TYPE");
            bwRel.newLine();

            Queue<Long> nodesWaitingForExpansion = new LinkedList<Long>();
            Random random = new Random();
            int cnt = 0;
            while (cnt < maxNumberOfNodes) {
                int nodeIndex = random.nextInt(allNodes.size());
                nodesWaitingForExpansion.add(allNodes.get(nodeIndex));
                allNodes.remove(allNodes.get(nodeIndex));

                while (!nodesWaitingForExpansion.isEmpty()) {
                    Node currNode = dataGraph.getNodeById(nodesWaitingForExpansion.poll());
                    if (!visitedNodes.contains(currNode.getId())) {
//                        TreeMap<String, String> labels = new TreeMap<>();
                        String label = getAlphabeticOnly(currNode.getLabels().iterator().next().name());

                        if(files.containsKey(label))
                        {
                            writers.get(getAlphabeticOnly(label)).write(currNode.getId() + "");
                            for(String pk : properties.get(label))
                            {
                                if(currNode.hasProperty(pk)) {
                                    writers.get(getAlphabeticOnly(label)).write("," + getAlphabeticOnly(currNode.getProperty(pk).toString()));
                                }
                                else{
                                    writers.get(getAlphabeticOnly(label)).write("," + "NA");
                                }
                            }
                            writers.get(getAlphabeticOnly(label)).write("\n");

                        } else {

                            System.out.println(getAlphabeticOnly(label));

                            files.put(getAlphabeticOnly(label), new File(nodesRelFileDir+getAlphabeticOnly(label)+".txt"));
                            writers.put(label, new FileWriter(files.get(label)));


                            /**
                             * The header of the text file
                             */
                            writers.get(label).write("unique:ID");
                            for(String pk : properties.get(label))
                            {
                                writers.get(label).write("," + pk);
                            }
                            writers.get(label).write("\n");

                            writers.get(label).write(currNode.getId() + "");
                            for(String pk : properties.get(label))
                            {
                                if(currNode.hasProperty(pk)) {
                                    writers.get(getAlphabeticOnly(label)).write("," + getAlphabeticOnly(currNode.getProperty(pk).toString()));
                                }
                                else{
                                    writers.get(getAlphabeticOnly(label)).write("," + "NA");
                                }
                            }
                            writers.get(label).write("\n");



                        }


                        visitedNodes.add(currNode.getId());
                        cnt++;
                    }

                    if(currNode.getDegree(Direction.OUTGOING) < maxDegree) {

                        for (Relationship rel : currNode.getRelationships(Direction.OUTGOING)) {
                            bwRel.write(rel.getStartNode().getId() + "," + rel.getEndNode().getId() + ","
                                    + rel.getType().name());
                            bwRel.newLine();

                            if (!visitedNodes.contains(rel.getEndNode().getId())) {
                                nodesWaitingForExpansion.add(rel.getEndNode().getId());
                                allNodes.remove(rel.getEndNode().getId());
                            }
                        }
                    }
                }
            }

//            bwNodeType.close();

            bwRel.close();

            for(String s : writers.keySet())
            {
                writers.get(s).close();
            }


            deleteDirIfExist(futureDbPath);

            String neo4jCommand = neo4jImportPath + "neo4j-import --into " + futureDbPath;
            for(String s : files.keySet())
            {
                neo4jCommand = neo4jCommand + " --nodes:" + s + " " + nodesRelFileDir + s + ".txt";
            }

            neo4jCommand = neo4jCommand + " --relationships " + nodesRelFileDir + "Relationships.txt";

            System.out.println(neo4jCommand);
//            Process p = Runtime.getRuntime().exec(neo4jImportPath + "neo4j-import --into " + futureDbPath + " --nodes "
//                    + nodesRelFileDir + "nodes.txt --relationships " + nodesRelFileDir + "Relationships.txt");

            Process p = Runtime.getRuntime().exec(neo4jCommand);
            p.waitFor();
            int exitVal = p.exitValue();

            System.out.println("proc.exitValue(): " + exitVal);

            System.out.println("program is finished properly!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("some err!");
        }

    }

    private static String getAlphabeticOnly(String input){
        return input.replaceAll("[^\\w\\s]","").replaceAll("\\n", " ");
    }

    private static void deleteDirIfExist(String directoryPath) throws Exception {
        Path directory = Paths.get(directoryPath);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });

    }

}
