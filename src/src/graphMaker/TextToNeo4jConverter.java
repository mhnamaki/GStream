package src.graphMaker;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

/**
 * This program will convert graphs represented by text files to Neo4j graphs.
 * The input will be two .txt files: one for vertices and one for edges, with the following format:
 *
 * File Vertex.txt:
 * <Node-ID>,<Node-Type>,<subscript>
 *
 *
 *
 * File Edge.txt:
 * <From>,<To>,<Relationship type>
 *
 * @author Shayan Monadjemi
 * @date June 8th, 2016
 */
public class TextToNeo4jConverter {

    public static void main (String[] args) throws IOException
    {
        String vertexPath = "/home/shayan/Documents/WSU/Data/ExpansionTestData2/Vertex.txt";
        String edgePath = "/home/shayan/Documents/WSU/Data/ExpansionTestData2/Edge.txt";
        String storePath = "/home/shayan/Documents/WSU/Data/ExpansionTestData2/ExpansionTestGraphDatabase";

        File vertex = new File(vertexPath);
        File edge = new File(edgePath);
        File storeDir = new File(storePath);

        Path directory = Paths.get(storePath);

        if (java.nio.file.Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
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


        GraphDatabaseService newGraphData = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
                .setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

        //Add Nodes
        Scanner readVertex = new Scanner(vertex);
        String line;
        String[] lineComp;

        try(Transaction tx1 = newGraphData.beginTx()) {
            while (readVertex.hasNextLine()) {
                line = readVertex.nextLine();
                line.trim();
                lineComp = line.split(",");

                Node newNode = newGraphData.createNode();
                newNode.addLabel(Label.label(lineComp[1]));
                newNode.setProperty("subscript", lineComp[2]);

                System.out.println(newNode.getId() + "is the actual ID; We'd like ID = " + lineComp[0] + "; Label is " + lineComp[1]);

            }
            tx1.success();
        }
        catch (Exception e)
        {

        }


        //Add Relationships
        Scanner readEdge = new Scanner(edge);

        try(Transaction tx2 = newGraphData.beginTx())
        {
            while (readEdge.hasNextLine())
            {
                line = readEdge.nextLine();
                line.trim();
                lineComp = line.split(",");

                newGraphData.getNodeById(Long.parseLong(lineComp[0])).createRelationshipTo
                        (newGraphData.getNodeById(Long.parseLong(lineComp[1])), RelationshipType.withName(lineComp[2]));

            }

            tx2.success();
        }
        catch (Exception e)
        {

        }

        newGraphData.shutdown();


    }

}
