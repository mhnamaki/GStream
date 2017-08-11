//package src.utilities;
//
//import java.io.FileReader;
//import java.util.HashMap;
//import java.util.Iterator;
//
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import src.finalAlgorithm.topkMonitoring.SnapshotTopkMonitoringOptBiSim;
//import src.utilities.Dummy.DummyProperties;
//import src.utilities.Dummy.DummyProperties.DatasetName;
//
//public class ConfigurationSimple {
//
//	public HashMap<String, DatasetInfo> datasetInfos = new HashMap<String, DatasetInfo>();
//	public String focuses;
//	public int maxAllowedHops; // d
//	public int maxAllowedEdges; // event size
//	public String dataGraphName; // data graph name
//	public int k;
//	public double threshold;
//	public int numberOfTransactionInASnapshot;
//	public int numberOfSnapshots;
//	public int edgeStreamCacheCapacity;
//	public boolean windowMode;
//	public int windowSizeL;
//	public String finalDataGraphPath;
//	public double alpha;
//
//	public String dateFormat; // yyyy or dd-MMM-yyyy or .... depends to the
//								// dataset
//	public String deltaEFileOrFiles;
//	public String dataGraphPath;
//	public int startingIncYear = 0;
//
//	// just fill the constructor parameters
//	public ConfigurationSimple(String focuses, int maxAllowedHops, int maxAllowedEdges, String dataGraphName, int k,
//			double threshold, int edgeStreamCacheCapacity, boolean windowMode, int windowSizeL, int numberOfSnapshots,
//			double alpha) throws Exception {
//
//		if (datasetInfos.size() == 0) {
//			populateFromConfigFile(datasetInfos);
//		}
//
//		if (!datasetInfos.containsKey(dataGraphName)) {
//			ConfigurationSimple.returnErrorMessageToWeb("the dataset is not valid!");
//			return;
//		}
//
//		DatasetInfo datasetInfo = datasetInfos.get(dataGraphName);
//
//		switch (dataGraphName.toLowerCase()) {
//
//		case "n10000":
//			DummyProperties.selectedDataset = DatasetName.N10000;
//			break;
//		case "citation":
//			DummyProperties.selectedDataset = DatasetName.CITATION;
//			break;
//		case "panama":
//			DummyProperties.selectedDataset = DatasetName.PANAMA;
//			break;
//		case "ids":
//			DummyProperties.selectedDataset = DatasetName.IDS;
//			break;
//		default:
//			System.err.println("data is not valid!");
//			ConfigurationSimple.returnErrorMessageToWeb("the dataset is not valid!");
//			return;
//		}
//
//		String error = "";
//		if (datasetInfo.dateFormat == null) {
//			error += " date format ";
//		}
//		if (datasetInfo.g0Path == null) {
//			error += " initial graph ";
//		}
//		if (datasetInfo.deltaEfileOrFiles == null) {
//			error += " edge updates ";
//		}
//		if (datasetInfo.finalDataGraphPath == null) {
//			error += " final data graph ";
//		}
//		if (!error.equals("")) {
//			returnErrorMessageToWeb("please fill the following: " + error);
//		}
//
//		this.dataGraphPath = datasetInfo.g0Path;
//		this.deltaEFileOrFiles = datasetInfo.deltaEfileOrFiles;
//		this.dateFormat = datasetInfo.dateFormat;
//		this.finalDataGraphPath = datasetInfo.finalDataGraphPath;
//		this.startingIncYear = Integer.parseInt(datasetInfo.startingIncYear);
//
//		this.dataGraphName = dataGraphName;
//		this.maxAllowedHops = maxAllowedHops;
//		this.maxAllowedEdges = maxAllowedEdges;
//		this.dataGraphName = dataGraphName;
//		this.k = k;
//		this.threshold = threshold;
//		// this.numberOfTransactionInASnapshot = numberOfTransactionInASnapshot;
//		this.numberOfSnapshots = numberOfSnapshots;
//		this.edgeStreamCacheCapacity = edgeStreamCacheCapacity;
//		this.windowMode = windowMode;
//		this.windowSizeL = windowSizeL;
//		this.focuses = focuses;
//		this.alpha = alpha;
//
//		// DummyProperties.softwareMode;
//		// deltaEFileOrFiles
//		// dateFormat
//	}
//
//	public JSONObject getTheFinalResult() throws Exception {
//		new SnapshotTopkMonitoringOptBiSim(this);
//		JSONParser parser = new JSONParser();
//		JSONObject wholeObj = (JSONObject) parser.parse(new FileReader("theWhole.json"));
//		return wholeObj;
//
//	}
//
//	private void populateFromConfigFile(HashMap<String, DatasetInfo> datasetInfos) throws Exception {
//		JSONParser parser = new JSONParser();
//		Object obj = parser.parse(new FileReader("config.conf"));
//		JSONArray datasets = (JSONArray) obj;
//		Iterator<JSONObject> iterator = datasets.iterator();
//		while (iterator.hasNext()) {
//			JSONObject datasetObj = (JSONObject) iterator.next();
//			String name = (String) datasetObj.get("name");
//			String g0Path = (String) datasetObj.get("g0Path");
//			String dateFormat = (String) datasetObj.get("dateFormat");
//			String deltaEFileOrFiles = (String) datasetObj.get("deltaEFileOrFiles");
//			String startingIncYear = (String) datasetObj.get("startingIncYear");
//			String finalDataGraphPath = (String) datasetObj.get("finalDataGraphPath");
//			DatasetInfo datasetInfo = new DatasetInfo(g0Path, deltaEFileOrFiles, dateFormat, startingIncYear,
//					finalDataGraphPath);
//			datasetInfos.put(name.toLowerCase().trim(), datasetInfo);
//		}
//
//	}
//
//	public static void returnErrorMessageToWeb(Object obj) {
//		System.err.println(obj.toString());
//	}
//
//	public static void returnUsualMessageToWeb(Object obj) {
//		System.out.println(obj.toString());
//	}
//
//	// test case:
//	public static void main(String[] args) throws Exception {
//		// focuses example: Asia;Entity|status:Active =>
//		// e.g.
//		// Label1|propertyKey11:propertyValue11,propertyKey12:propertyValue12;Label2|propertyKey21:propertyValue21,propertyKey22:propertyValue22
//		// Scenario 1
//		// Configuration conf = new Configuration("A", 2, 6, "citation", 3,
//		// 0.05, 3, 5, 3, false, 0);
//		//
//		// // Scenario 2
//		// conf = new Configuration("A", 3, 6, "citation", 3, 0.05, 3, 5, 3,
//		// false, 0);
//		//
//		// // Scenario 3
//		// conf = new Configuration("B", 2, 4, "citation", 5, 0.002, 3, 5, 3,
//		// false, 0);
//
//	}
//
//}
