package src.simpleTests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import src.utilities.Dummy.DummyFunctions;

public class Test34 {

	public static void main(String[] args) throws Exception {

		FileInputStream fis = new FileInputStream("forTest.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}

		br.close();

	}

}
