package src.dualSimulation.gtar.mag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Data Generator for Microsoft Academic Graph (MAG)
 * https://academicgraph.blob.core.windows.net/graph/index.html?eulaaccept=on
 * Version: 2016-02-05
 */
public class KeywordsFrequencies {

    public static void count(String basename) throws FileNotFoundException {
        String PaperKeywords  = basename + "/PaperKeywords.txt";
        String FieldsOfStudy  = basename + "/FieldsOfStudy.txt";
        String FieldsHierachy = basename + "/FieldOfStudyHierarchy.txt";

        String ofile = "./FreqKeywords.csv";

        Scanner scanner;
        PrintWriter writer;
        String line;


        Map<String, String> Fields= new HashMap<>();
        scanner = new Scanner(new File(FieldsOfStudy));
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            String tokens[] = line.split("\\s*\t\\s*");
            Fields.put(tokens[0], tokens[1]);
        }
        scanner.close();

        Map<String, String> FieldLevels = new HashMap<>();
        scanner = new Scanner(new File(FieldsHierachy));
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            String tokens[] = line.split("\\s*\t\\s*");
            FieldLevels.putIfAbsent(tokens[0], tokens[1]);
            FieldLevels.putIfAbsent(tokens[2], tokens[3]);
        }
        for (String fieldId : Fields.keySet()) {
            if (FieldLevels.get(fieldId) == null) {
                FieldLevels.put(fieldId, "unclassified");
            }
        }
        scanner.close();

        scanner = new Scanner(new File(PaperKeywords));
        Map<String, Long> keyfreq = new HashMap<>();
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();

            String tokens[] = line.split("\\s*\t\\s*");
            String keyId = tokens[2];

            Long freq = keyfreq.getOrDefault(keyId, 0L) + 1;
            keyfreq.put(keyId, freq);
        }

        Map<Long, Set<String>> freqkey = new TreeMap<>(Collections.reverseOrder());
        for (String k : keyfreq.keySet()) {
            Long freq = keyfreq.get(k);
            freqkey.putIfAbsent(freq, new HashSet<String>());
            freqkey.get(freq).add(k);
        }

        writer = new PrintWriter(new File(ofile));
        for (Long freq : freqkey.keySet()) {
            for (String keyId : freqkey.get(freq)) {
                writer.println(String.format("%d", freq) + "," + FieldLevels.get(keyId) + "," + Fields.get(keyId));
            }
        }

        writer.close();
        scanner.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Prepare MAG data");

        String basename = args[0];
        count(basename);
    }
}
