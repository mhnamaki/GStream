package src.dataset.statistics;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

public class FileDatasetStat {

	public static void main(String[] args) throws Exception {

		FileInputStream fis = new FileInputStream("/Users/mnamaki/Downloads/sipscan.release_dataset");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		HashMap<Integer, HashSet<String>> keyValueMap = new HashMap<Integer, HashSet<String>>();

		int countLines = 0;
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] splittedLine = line.split(",");
			if (splittedLine.length > 8) {
				continue;
			}
			
			for (int s = 0; s < splittedLine.length; s++) {

				if (s == 0 || s == 3 || s == 4 || s == 5)
					continue;

				keyValueMap.putIfAbsent(s, new HashSet<String>());
				keyValueMap.get(s).add(splittedLine[s]);
			}
			countLines++;
		}

		System.out.println("countLines: " + countLines);

		for (Integer id : keyValueMap.keySet()) {
			System.out.println(id + " -> " + keyValueMap.get(id).size());
		}

		br.close();

	}

}
