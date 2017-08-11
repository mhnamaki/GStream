package src.utilities;
 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Logger {

	public static BufferedWriter logger;

	public static void log(String log) {
		File fout = new File("log.txt");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fout, true);
			logger = new BufferedWriter(new OutputStreamWriter(fos));

			logger.write(log);
			logger.newLine();

			logger.close();
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void logAllMatchesOfAPattern(String str) {
		File fout = new File("allMatches.txt");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fout, true);
			logger = new BufferedWriter(new OutputStreamWriter(fos));

			logger.write(str);
			logger.newLine();

			logger.close();
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

	}

}
