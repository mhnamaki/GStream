package src.dataset.modification;

import org.neo4j.cypher.internal.compiler.v2_3.No;
import org.neo4j.cypher.internal.compiler.v2_3.commands.expressions.Property;
import org.neo4j.cypher.internal.compiler.v2_3.pipes.matching.Relationships;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shayan on 6/21/16.
 */
public class DataGraphModification {
    private static BatchInserter db;
    private static BatchInserterIndex index;

    public static void main(String[] args) throws Exception {
        String dataGraphPath = "/home/shayan/Documents/WSU/Data/offshoreneo4j.data/panama.graphdb";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-dataGraph")) {
                dataGraphPath = args[++i];
            }
        }

        File storeDir = new File(dataGraphPath);
        GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
                .setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();

        System.out.println("dataset: " + dataGraphPath);

        String[] usefulPropertyKeys = new String[] { "sourceID", "company_type", "jurisdiction", "status",
                "countries" };

        Transaction tx1 = dataGraph.beginTx();
        int counter = 0;
        HashMap<Long, ArrayList<String>> usefulPropsMap = new HashMap<Long, ArrayList<String>>();
//        HashMap<Long, ArrayList<Label>> prevLabelsMap = new HashMap<Long, ArrayList<Label>>();
        HashMap<String, ArrayList<Node>> countries = new HashMap<>();
        HashMap<String, ArrayList<Node>> status = new HashMap<>();
        HashMap<String, ArrayList<Node>> source = new HashMap<>();
        HashMap<String, ArrayList<Node>> company_Type = new HashMap<>();
        HashMap<String, ArrayList<Node>> jurisdiction = new HashMap<>();

        int cntNodes = 0;
        for (Node node : dataGraph.getAllNodes()) {


            cntNodes++;
            if ((cntNodes % 10000) == 0) {
                System.out.println("cntNodes: " + cntNodes);
            }
//            ArrayList<Label> prevLabels = new ArrayList<Label>();
//            for (Label label : node.getLabels()) {
//                prevLabels.add(label);
//            }

//            prevLabelsMap.put(node.getId(), prevLabels);

//            ArrayList<String> propValues = new ArrayList<String>();

                Map<String, Object> allProperties = node.getAllProperties();

                if(allProperties.containsKey("country_codes"))
                {


                    String countryValue = node.getProperty("country_codes").toString();
                    String [] cntParts = countryValue.split(";");
                    for(int i = 0; i < cntParts.length; i++) {

                        if (countries.containsKey(cntParts[i])) {
                            countries.get(cntParts[i]).add(node);
                        } else {
                            countries.put(cntParts[i], new ArrayList<Node>());
                            countries.get(cntParts[i]).add(node);
                        }
                    }


                }
                if(allProperties.containsKey("sourceID"))
                {
                    String srcId = node.getProperty("sourceID").toString();

                    if(source.containsKey(srcId))
                    {
                        source.get(srcId).add(node);
                    }
                    else
                    {
                        source.put(srcId, new ArrayList<>());
                        source.get(srcId).add(node);
                    }
                }
                if(allProperties.containsKey("status"))
                {
                    String sts = node.getProperty("status").toString();
                    if(status.containsKey(sts))
                    {
                        status.get(sts).add(node);
                    }
                    else
                    {
                        status.put(sts, new ArrayList<>());
                        status.get(sts).add(node);
                    }

                }
                if(allProperties.containsKey("company_type"))
                {
                    String type = node.getProperty("company_type").toString();
                    if(company_Type.containsKey(type))
                    {
                        company_Type.get(type).add(node);
                    }
                    else
                    {
                        company_Type.put(type, new ArrayList<>());
                        company_Type.get(type).add(node);
                    }
                }
                if(allProperties.containsKey("jurisdiction"))
                {
                    String jur = node.getProperty("jurisdiction").toString();
                    if(jurisdiction.containsKey(jur))
                    {
                        jurisdiction.get(jur).add(node);
                    }
                    else
                    {
                        jurisdiction.put(jur, new ArrayList<>());
                        jurisdiction.get(jur).add(node);
                    }
                }




//                if (allProperties.containsKey(usefulPropertyKeys[i])) {
//                    Object objValue = node.getProperty(usefulPropertyKeys[i]);
//                    if (objValue != null) {
//                        propValues.add(usefulPropertyKeys[i] + ":" + objValue.toString());
//                    }
//                }



            //usefulPropsMap.put(node.getId(), propValues);

        }
        System.out.println("We got the nodes to be added: \ncountries=" + countries.size() + "\ncompany_type=" + company_Type.size()
                + "\nstatus=" + status.size() + "\nsource=" + source.size() + "\njurisdiction=" + jurisdiction.size());

//
//        for(String str : countries.keySet())
//        {
//            System.out.println(str);
//        }

        tx1.success();
        tx1.close();
        dataGraph.shutdown();



        Map<String, String> config = new HashMap<String, String>();

        config.put("dbms.pagecache.pagesize", "4g");
        config.put("node_auto_indexing", "true");
        db = BatchInserters.inserter(storeDir, config);
        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(db);
        index = indexProvider.nodeIndex("dataSetIndex", MapUtil.stringMap("type", "exact"));
        System.out.println("We're ready to start adding nodes and relationships..... ");



        Map<String, Object> myMappedValues;

        //countries
        counter = 0;
        System.out.println("Adding country nodes and relationships");
        for(String cntCode : countries.keySet())
        {
            myMappedValues = new HashMap<>();
            myMappedValues.put("country_code", cntCode);

            Long destId = db.createNode(myMappedValues, Label.label("Country"));

            for(Node src : countries.get(cntCode))
            {
                db.createRelationship(src.getId(), destId, RelationshipType.withName("NOTYPE"), null);
//                db.createRelationship(destId, src.getId(), RelationshipType.withName("NOTYPE"), null);
            }
            counter++;
            if(counter % 10 == 0)
            {
                System.out.println(counter+"/"+countries.keySet().size());
            }
        }

        //type
        counter = 0;
        System.out.println("Adding type nodes and relationships");
        for(String ctype : company_Type.keySet())
        {
            myMappedValues = new HashMap<>();
            myMappedValues.put("company_type", ctype);

            Long destId = db.createNode(myMappedValues, Label.label("Company_Type"));

            for(Node src : company_Type.get(ctype))
            {
                db.createRelationship(src.getId(), destId, RelationshipType.withName("NOTYPE"), null);
//                db.createRelationship(destId, src.getId(), RelationshipType.withName("NOTYPE"), null);
            }
            counter++;
            if(counter % 10 == 0)
            {
                System.out.println(counter+"/"+company_Type.keySet().size());
            }
        }

        //Status
        counter = 0;
        System.out.println("Adding status nodes and relationships");
        for(String sts : status.keySet())
        {
            myMappedValues = new HashMap<>();
            myMappedValues.put("status", sts);

            Long destId = db.createNode(myMappedValues, Label.label("Status"));

            for(Node src : status.get(sts))
            {
                db.createRelationship(src.getId(), destId, RelationshipType.withName("NOTYPE"), null);
//                db.createRelationship(destId, src.getId(), RelationshipType.withName("NOTYPE"), null);
            }
            counter++;
            if(counter % 10 == 0)
            {
                System.out.println(counter+"/"+status.keySet().size());
            }
        }

        //source
        counter = 0;
        System.out.println("Adding source nodes and relationships");
        for(String srs : source.keySet())
        {
            myMappedValues = new HashMap<>();
            myMappedValues.put("Source", srs);

            Long destId = db.createNode(myMappedValues, Label.label("Source"));

            for(Node src : source.get(srs))
            {
                db.createRelationship(src.getId(), destId, RelationshipType.withName("NOTYPE"), null);
//                db.createRelationship(destId, src.getId(), RelationshipType.withName("NOTYPE"), null);
            }
            counter++;
            if(counter % 10 == 0)
            {
                System.out.println(counter+"/"+source.keySet().size());
            }
        }

        //jurisdiction
        counter = 0;
        System.out.println("Adding jurisdiction nodes and relationships");
        for(String jdc : jurisdiction.keySet())
        {
            myMappedValues = new HashMap<>();
            myMappedValues.put("jurisdiction", jdc);

            Long destId = db.createNode(myMappedValues, Label.label("Jurisdiction"));

            for(Node src : jurisdiction.get(jdc))
            {
                db.createRelationship(src.getId(), destId, RelationshipType.withName("NOTYPE"), null);
//                db.createRelationship(destId, src.getId(), RelationshipType.withName("NOTYPE"), null);
            }
            counter++;
            if(counter % 10 == 0)
            {
                System.out.println(counter+"/"+jurisdiction.keySet().size());
            }
        }





//
//        for (Long nodeId : usefulPropsMap.keySet()) {
//            for (String propValue : usefulPropsMap.get(nodeId)) {
//                prevLabelsMap.get(nodeId).add(Label.label(propValue));
//            }
//
////            db.setNodeLabels(nodeId, prevLabelsMap.get(nodeId).toArray(new Label[prevLabelsMap.get(nodeId).size()]));
//
//        }

//        counter++;
//        if ((counter % 200000) == 0) {
//            index.flush();
//            System.out.println(counter + " progress.");
//            // tx1.success();
//            // tx1.close();
//            // tx1 = knowledgeGraph.beginTx();
//        }

        index.flush();

        System.gc();
        System.runFinalization();

        /// System.out.println("uriProps size: " + uriProps.size());
//        System.out.println("counter: " + counter);
        System.out.println("indexProvider shutting down");
        indexProvider.shutdown();

        System.out.println("db shutting down");
        db.shutdown();

        System.out.println("knowledgeGraph shutting down");
        dataGraph.shutdown();

        System.out.println("completed");

    }

}
