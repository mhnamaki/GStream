//package server;
//
//import java.net.*;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.awt.List;
//import java.io.*;
//import src.utilities.Configuration;
//
//public class EchoServerSimple {
//	public static void main(String[] args) throws IOException {
//
//		// if (args.length != 1) {
//		// System.err.println("Usage: java EchoServer <port number>");
//		// System.exit(1);
//		// }
//		// int portNumber = Integer.parseInt(args[0]);
//		int portNumber = 7575;
//		ServerSocket server = new ServerSocket(portNumber);
//		System.out.println("Listening for connection on port 7575 ....");
//		while (true) {
//			try (Socket socket = server.accept()) {
//				InputStreamReader isr = new InputStreamReader(socket.getInputStream());
//				BufferedReader reader = new BufferedReader(isr);
//				String inputLine;
//				String request = "";
//				while (!(inputLine = reader.readLine()).equals("end")) {
//					request += inputLine + "\n";
//					System.out.println(inputLine);
//				}
//				System.out.println("end of read");
//				String response = processRequest(request);
//				Date today = new Date();
//				// String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + today;
//				String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + response;
//				socket.getOutputStream().write(httpResponse.getBytes("UTF-8"));
//			}
//		}
//	}
//
//	private static String processRequest(String request) {
//		// TODO Auto-generated method stub
//		String reqType = "none";
//		boolean bodyStart = false;
//		String body = "";
//		String response = "";
//		String lines[] = request.split("\\r?\\n");
//		for (String line : lines) {
//			if (line.startsWith("type")) {
//				// TODO get reqtype
//			} else if (line.equals("")) {
//				bodyStart = true;
//			} else if (bodyStart) {
//				body += line;
//			}
//		}
//		try {
//			Map<String, String> params = getQueryParams(body);
//			// String focuses, int maxAllowedHops, int maxAllowedEdges, String
//			// dataGraphName, int k,
//			// double threshold, int numberOfTransactionInASnapshot, int
//			// numberOfSnapshots, int edgeStreamCacheCapacity,
//			// boolean windowMode, int windowSizeL
//			System.out.println(params.toString());
//			System.out.println(params.get("focuses"));
//			boolean windowMode = false;
//			if (params.get("systemModeAc").equals("win")) {
//				windowMode = true;
//			}
//			// params.get(focuses);
//			Configuration conf = new Configuration(params.get("focuses"), Integer.parseInt(params.get("diameter")),
//					Integer.parseInt(params.get("eventSize")), params.get("dataGraph").toLowerCase().trim(),
//					Integer.parseInt(params.get("topk")), Double.parseDouble(params.get("threshold")),
//					Integer.parseInt(params.get("cache")), windowMode, Integer.parseInt(params.get("winSize")),
//					Integer.parseInt(params.get("snapshots")), Double.parseDouble(params.get("decay")));
//
//			response = conf.getTheFinalResult().toJSONString();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if (reqType.equals("")) {
//
//		} else if (reqType.equals("")) {
//
//		} else if (reqType.equals("")) {
//
//		} else if (reqType.equals("")) {
//
//		} else if (reqType.equals("")) {
//		}
//		return response;
//	}
//
//	public static Map<String, String> getQueryParams(String query) throws UnsupportedEncodingException {
//
//		Map<String, String> params = new HashMap<String, String>();
//		for (String param : query.split("&")) {
//			String[] pair = param.split("=");
//			String key = URLDecoder.decode(pair[0], "UTF-8");
//			String value = "";
//			if (pair.length > 1) {
//				value = URLDecoder.decode(pair[1], "UTF-8");
//			}
//
//			String values = params.get(key);
//			if (values == null) {
//				values = new String();
//				params.put(key, value);
//			}
//			values = value;
//		}
//
//		return params;
//
//	}
//}
