package src.dataset.dataSnapshot;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by shayan on 6/29/16.
 */
public class SnapshotSimulator {

	File delta;
	File[] deltas;
	Scanner readDelta;
	StreamEdge nextEdge;
	boolean multiFileSetting;
	int currentIndex;
	String timeStampFormat;

	public String getCurrentDeltaFileName() {
		return deltas[currentIndex].getName();
	}

	public String getCurrentEdgeStreamLine() {
		return (nextEdge.isAdded() ? "+" : "-") + "," + nextEdge.getTimeStamp() + "," + nextEdge.getSourceNode() + ","
				+ nextEdge.getDestinationNode() + "," + nextEdge.getRelationshipType();
	}

	public SnapshotSimulator(File deltaFile, String timeStampFormat) throws Exception {
		multiFileSetting = false;
		loadFile(deltaFile);
		this.timeStampFormat = timeStampFormat;
	}

	public SnapshotSimulator(String dir, String timeStampFormat) throws Exception {
		currentIndex = 0;
		multiFileSetting = true;
		deltas = fileInTheDirfinder(dir);
		loadFile(deltas[currentIndex]);
		this.timeStampFormat = timeStampFormat;
	}

	public StreamEdge getNextStreamEdge() throws Exception {
		if (multiFileSetting) {
			if (readDelta.hasNext()) {
				nextEdge = new StreamEdge(readDelta.next(), timeStampFormat);
				return nextEdge;
			} else {
				if (currentIndex < deltas.length - 1) {
					currentIndex++;
					loadFile(deltas[currentIndex]);
					nextEdge = new StreamEdge(readDelta.next(), timeStampFormat);
					return nextEdge;
				} else {
					return null;
				}

			}
		} else {

			if (readDelta.hasNext()) {
				nextEdge = new StreamEdge(readDelta.next(), timeStampFormat);
				return nextEdge;
			} else {
				return null;
			}
		}

	}

	private void loadFile(File f) throws Exception {
		delta = f;
		readDelta = new Scanner(delta);
		readDelta.useDelimiter("\n");
	}

	public static File[] fileInTheDirfinder(String dirName) {
		File dir = new File(dirName);

		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".txt") || filename.startsWith("Delta");
			}
		});

		if (files != null && files.length > 1)
			Arrays.sort(files);

		for (int i = 0; i < files.length; i++) {
			System.out.println("catched file " + i + "; " + files[i].getName());
		}
		return files;

	}

}
