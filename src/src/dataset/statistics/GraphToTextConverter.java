package src.dataset.statistics;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by shayan on 6/6/16.
 * This program converts Neo4j databases to text files that represent edges and vertices with comma separated fields.
 *
 * Used to test Neo4j data with Peng's graphfun code
 */
public class GraphToTextConverter {



    public static void main(String[] args) throws IOException
    {


        FileWriter edgeWriter = new FileWriter(new File("/home/shayan/Documents/WSU/Data/TextData/SyntheticEdge.txt"));
        FileWriter vertexWriter = new FileWriter(new File("/home/shayan/Documents/WSU/Data/TextData/SyntheticVertex.txt"));

        String dataGraphPath = "/home/shayan/Documents/WSU/Data/Synthetic/SyntheticG.graphdb";

        File storeDir = new File(dataGraphPath);
        GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
                .setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

        System.out.println("dataset: " + dataGraphPath);

        //Write the nodes in the file

        try (Transaction tx1 = dataGraph.beginTx()) {

            for (Node n : dataGraph.getAllNodes())
            {
                vertexWriter.write(n.getId()+"");
                for(Label str : n.getLabels())
                {
                    vertexWriter.write("," + str);
                }
                vertexWriter.write("\n");
            }

            vertexWriter.close();
            tx1.success();
        } catch (Exception e) {
            // TODO: handle exception
        }

        //Write edges in the file

        try (Transaction tx1 = dataGraph.beginTx()) {

            for (Relationship e : dataGraph.getAllRelationships())
            {
                edgeWriter.write(e.getStartNode().getId() + "," + e.getEndNode().getId() + "," + e.getType().name() + "\n");

            }

            edgeWriter.close();
            tx1.success();
        } catch (Exception e) {
            // TODO: handle exception
        }


    }
}
