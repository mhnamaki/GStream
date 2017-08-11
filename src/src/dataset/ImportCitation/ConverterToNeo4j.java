package src.dataset.ImportCitation;

import org.omg.SendingContext.RunTime;
import scala.reflect.internal.pickling.UnPickler;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

/**
 * This program will convert the citation data set to .CSV files that are importable by Neo4j's built in tools.
 *
 * Created by shayan on 6/23/16.
 */
public class ConverterToNeo4j {

    public static  void main(String[] args) throws Exception {
        final ArrayList<String> suffix = new ArrayList<>(Arrays.asList("jr", "jr.", "sr", "sr.", "i", "ii", "iii", " ", "\n", "", "."));
        final String defaultAuthor = "NO_AUTHOR";
        final String defaultYear = "-1";
        final String defaultTitle = "NO_TITLE";
        final String defaultPublicationVenue = "NO_PUBLICATION_VENUE";
        final String defaultIndex = "NO_INDEX";
        String storePath = "/home/shayan/Documents/WSU/Data/Citation/citation_v2.0";
        String neo4jPath = "/home/shayan/Programs/neo4j-community-3.0.1";
        String futureDbPath = storePath + "/citation_v2.0.graphdb";


        HashSet<String> rejectedIndeces = new HashSet<>();
        HashSet<String> stopWordList = new HashSet<>();


        String stopWordsPath = "/home/shayan/Documents/WSU/TopKGStream/src/src/dataset/ImportCitation/stopwords.txt";
        String inputPath = "/home/shayan/Documents/WSU/Data/Citation/citation_v1.0/citation-acm-v8.txt";
        String authorPath = storePath + "/Author.csv";
        String paperPath = storePath + "/Paper.csv";
        String publicationVenuePath = storePath + "/PV.csv";
        String wrotePath = storePath + "/Wrote.csv";
        String publishedInPath = storePath + "/PI.csv";
        String citedPath = storePath + "/Cited.csv";
        String finalAuthorPath = storePath + "/finalAuthor.csv";
        String finalPVPath = storePath + "/finalPV.csv";
        String containsKeywordPath = storePath + "/Contains_Keyword.csv";
        String keywordPath = storePath + "/Keyword.csv";
        String finalKeywordPath = storePath + "/finalKeyword.csv";

        File stopWords = new File(stopWordsPath);
        File inputCitation = new File(inputPath);
        File author = new File(authorPath);
        File paper = new File(paperPath);
        File publicationVenue = new File(publicationVenuePath);
        File wrote = new File(wrotePath);
        File publishedIn = new File(publishedInPath);
        File cited = new File(citedPath);
        File finalAuthor = new File(finalAuthorPath);
        File finalPV = new File(finalPVPath);
        File containsKeyword = new File(containsKeywordPath);
        File keyword = new File(keywordPath);
        File finalKeyword = new File(finalKeywordPath);

        Scanner readStopWords = new Scanner(stopWords);
        readStopWords.useDelimiter("\n");
        while (readStopWords.hasNext()) {
            stopWordList.add(readStopWords.next());
        }

        Scanner readInput = new Scanner(inputCitation);
        readInput.useDelimiter("\n\n");
        String inputChunk;

        FileWriter authorWriter = new FileWriter(author);
        FileWriter paperWriter = new FileWriter(paper);
        FileWriter publicationVenueWriter = new FileWriter(publicationVenue);
        FileWriter wroteWriter = new FileWriter(wrote);
        FileWriter publishedInWriter = new FileWriter(publishedIn);
        FileWriter citedWriter = new FileWriter(cited);
        FileWriter finalAuthorWriter = new FileWriter(finalAuthor);
        FileWriter finalPVWriter = new FileWriter(finalPV);
        FileWriter containsKeywordWriter = new FileWriter(containsKeyword);
        FileWriter keywordWriter = new FileWriter(keyword);
        FileWriter finalKeywordWriter = new FileWriter(finalKeyword);

        paperWriter.write("Index:ID(Paper),Title,Year\n");
        wroteWriter.write(":START_ID(Author), :END_ID(Paper)\n");
        publishedInWriter.write(":START_ID(Paper),:END_ID(Publication_Venue)\n");
        citedWriter.write(":START_ID(Paper),:END_ID(Paper)\n");
        finalAuthorWriter.write("Name:ID(Author)\n");
        finalPVWriter.write("Venue_Name:ID(Publication_Venue)\n");
        containsKeywordWriter.write(":START_ID(Paper),:END_ID(Keyword)\n");
        finalKeywordWriter.write("Keyword:ID(Keyword)\n");

        Long counter = 0l;
        int rejected = 0;

        while (readInput.hasNext()) {
            inputChunk = readInput.next();
            String[] chunkParts = inputChunk.split("\n");

            String[] authors = {defaultAuthor};
            String title = defaultTitle;
            ArrayList<String> keywords = new ArrayList<>();
            String year = defaultYear;
            String pubVen = defaultPublicationVenue;
            ArrayList<String> citations = new ArrayList<>();
            String index = defaultIndex;


            //Extracting information to variables

            for (String str : chunkParts) {
                if (str.substring(0, 2).equals("#*")) {
                    title = str.substring(2);
                    title = title.replaceAll("\"", " ").replaceAll(",", " ").replaceAll(";", " ");

                    String[] titleParts = title.split(" ");
                    for (String string : titleParts) {
                        if (!stopWordList.contains(string)) {
                            keywords.add(string);
                        }
                    }

                } else if (str.substring(0, 2).equals("#@")) {
                    authors = str.substring(2).split(", ");
//                    System.out.println("Author(s): " + authors);
                } else if (str.substring(0, 2).equals("#t")) {
                    year = str.substring(2);
//                    System.out.println("Year: " + str.substring(2));
                } else if (str.substring(0, 2).equals("#c")) {
                    pubVen = str.substring(2);
                    pubVen = pubVen.replaceAll(",", ";").replaceAll("\"", "'");
//                    System.out.println("Publication Venue: " + str.substring(2));
                } else if (str.substring(0, 2).equals("#%")) {
                    citations.add(str.substring(2));
//                     System.out.println("Cited: " + str.substring(2));
                } else if (str.contains("#index")) {
                    index = str.substring(6);
//                    System.out.println("Index: " + str.substring(6));
                }
            }

//            if(authors[0].equals(defaultAuthor) || year.equals(defaultYear) || title.equals(defaultTitle) || index.equals(defaultIndex)
//                    || title.length() < 20)
//            {
//                rejectedIndeces.add(index);
//                rejected++;
//                continue;
//            }

            //Putting information in the csv files.

            paperWriter.write(index + "," + title + "," + year + "\n");


            for (String a : authors) {
                if (!suffix.contains(a.toLowerCase())) {
                    a = a.replaceAll(",", ";").replaceAll("[0-9]+/*\\.*[0-9]*", "").replaceAll("\"", "'");
                    authorWriter.write(a + "\n");
                    wroteWriter.write(a + "," + index + "\n");
                }
            }

            for (String s : keywords) {
                s = s.replaceAll("[0-9]+/*\\.*[0-9]*", "").toLowerCase();
                if(!s.equals("")) {
                    keywordWriter.write(s + "\n");
                    containsKeywordWriter.write(index + "," + s + "\n");
                }
            }

            publicationVenueWriter.write(pubVen + "\n");
            publishedInWriter.write(index + "," + pubVen + "\n");

            for (String c : citations) {
//                if(!rejectedIndeces.contains(c))
                citedWriter.write(index + "," + c + "\n");
            }

            counter++;

            if (counter % 100 == 0)
                System.out.println(counter + " entries processed.");


//            System.out.println();
        }

        System.out.println("Completed\nAdded " + counter + " entries\nRejected " + rejected + " entries");

        authorWriter.close();
        paperWriter.close();
        publicationVenueWriter.close();
        wroteWriter.close();
        publicationVenueWriter.close();
        citedWriter.close();
        publishedInWriter.close();
        keywordWriter.close();
        containsKeywordWriter.close();


        System.out.println("Start sortKeyword");
        Process sortKeyword = Runtime.getRuntime().exec("sort -o " + keywordPath + " " + keywordPath);
        System.out.println("waitFor sortKeyword");
        sortKeyword.waitFor();
        System.out.println("sortKeyword is complete. Exit value: " + sortKeyword.exitValue());

        System.out.println("Start uniqKeyword");
        Process uniqKeyword = Runtime.getRuntime().exec("uniq " + keywordPath + " " + storePath + "/Keyword2.csv");
        System.out.println("waitFor uniqKeyword");
        uniqKeyword.waitFor();
        System.out.println("uniqKeyword is complete. Exit value: " + uniqKeyword.exitValue());

        System.out.println("Start sortAuthor");
        Process sortAuthor = Runtime.getRuntime().exec("sort -o " + authorPath + " " + authorPath);
        System.out.println("waitFor sortAuthor");
        sortAuthor.waitFor();
        System.out.println("sortAuthor is complete. Exit value: " + sortAuthor.exitValue());

        System.out.println("Start uniqAuthor");
        Process uniqAuthor = Runtime.getRuntime().exec("uniq " + authorPath + " " + storePath + "/Author2.csv");
        System.out.println("waitFor uniqAuthor");
        uniqAuthor.waitFor();
        System.out.println("uniqAuthor is complete. Exit value: " + uniqAuthor.exitValue());

        System.out.println("Start sortPV");
        Process sortPV = Runtime.getRuntime().exec("sort -o " + publicationVenuePath + " " + publicationVenuePath);
        System.out.println("waitFor sortPV");
        sortPV.waitFor();
        System.out.println("sortPV is complete. Exit value: " + sortAuthor.exitValue());

        System.out.println("Start uniqPV");
        Process uniqPV = Runtime.getRuntime().exec("uniq " + publicationVenuePath + "  " + storePath + "/PV2.csv");
        System.out.println("waitFor uniqPV");
        uniqPV.waitFor();
        System.out.println("uniqPV is complete. Exit value: " + uniqPV.exitValue());

        Scanner read = new Scanner(new File(storePath + "/Author2.csv"));
        while (read.hasNextLine()) {
            finalAuthorWriter.write(read.nextLine() + "\n");
        }
        read.close();
        finalAuthorWriter.close();

        read = new Scanner(new File(storePath + "/PV2.csv"));
        while (read.hasNextLine()) {
            finalPVWriter.write(read.nextLine() + "\n");
        }
        read.close();
        finalPVWriter.close();

        read = new Scanner((new File(storePath + "/Keyword2.csv")));
        while ((read.hasNextLine()))
        {
            finalKeywordWriter.write(read.nextLine() + "\n");
        }
        read.close();
        finalKeywordWriter.close();


//        System.out.println("CSV files read; Building the Neo4j database");
//        Process p = Runtime.getRuntime().exec(neo4jPath + "/bin/neo4j-import --into " + futureDbPath + " --nodes:Author "
//                + finalAuthorPath + " --nodes:Paper " + paperPath + " --nodes:Keyword " + finalKeywordPath + " --nodes:Publication_Venue " + finalPVPath +
//                " --relationships:WROTE " + wrotePath + " --relationships:PUBLISHED_IN " + publishedInPath + " --relationships:CITED " + citedPath
//                + " --relationships:CONTAINS_KEYWORD " + containsKeywordPath);
//        p.waitFor();
//        int exitVal = p.exitValue();
//
//        System.out.println("proc.exitValue(): " + exitVal);
//
//        if(exitVal == 0)
//            System.out.println("program is finished properly!");
//        else
//            System.out.println("ERROR: Neo4j messed up");



    }

}
