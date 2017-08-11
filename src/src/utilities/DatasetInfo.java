package src.utilities;

public class DatasetInfo {
	public String g0Path;
	public String deltaEfileOrFiles;
	public String dateFormat;
	public String startingIncYear;
	public String finalDataGraphPath;

	public DatasetInfo(String g0Path, String deltaEfileOrFiles, String dateFormat, String startingIncYear, String finalDataGraphPath) {
		this.g0Path = g0Path;
		this.deltaEfileOrFiles = deltaEfileOrFiles;
		this.dateFormat = dateFormat;
		this.startingIncYear = startingIncYear;
		this.finalDataGraphPath = finalDataGraphPath;
	}
}
