package server;

import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import src.utilities.Configuration;

public class EchoServer {
	public static Configuration conf;
	public static boolean stateOne = false;
	public static boolean stateTwo = false;
	public static boolean stateThree = false;
	public static boolean stateFour = false;
	public static String finalResult = "";

	public static void main(String[] args) throws IOException {

		// if (args.length != 1) {
		// System.err.println("Usage: java EchoServer <port number>");
		// System.exit(1);
		// }
		// int portNumber = Integer.parseInt(args[0]);
		int portNumber = 7575;
		ServerSocket server = new ServerSocket(portNumber);
		System.out.println("Listening for connection on port 7575 ....");
		while (true) {
			try (Socket socket = server.accept()) {
				InputStreamReader isr = new InputStreamReader(socket.getInputStream());
				BufferedReader reader = new BufferedReader(isr);
				String inputLine;
				String request = "";
				while (!(inputLine = reader.readLine()).equals("end")) {
					request += inputLine + "\n";
//					System.out.println(inputLine);
				}
//				System.out.println("end of read");
				String response = processRequest(request);
				Date today = new Date();
				// String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + today;
				String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + response;
//				System.out.println("This Response sent! " + response);
				socket.getOutputStream().write(httpResponse.getBytes("UTF-8"));
				socket.getOutputStream().flush();
				socket.close();
//				System.out.println("socket closed");
			}
		}
	}

	private static String processRequest(String request) {
		// TODO Auto-generated method stub
		String reqType = "none";
		boolean bodyStart = false;
		String body = "";
		String response = "";
		String lines[] = request.split("\\r?\\n");
		for (String line : lines) {
			if (line.startsWith("type")) {
				// TODO get reqtype
			} else if (line.equals("")) {
				bodyStart = true;
			} else if (bodyStart) {
				body += line;
			}
		}
		try {

			Map<String, String> params = getQueryParams(body);
			// String focuses, int maxAllowedHops, int maxAllowedEdges, String
			// dataGraphName, int k,
			// double threshold, int numberOfTransactionInASnapshot, int
			// numberOfSnapshots, int edgeStreamCacheCapacity,
			// boolean windowMode, int windowSizeL
			System.out.println(params.toString());
			System.out.println(params.get("focuses"));
			boolean windowMode = false;
			if (params.containsKey("reqType")) {
				reqType = params.get("reqType");
				if (reqType.equals("0")) {
					if (params.get("systemModeAc").equals("win")) {
						windowMode = true;
					}

					try {
						Integer.parseInt(params.get("topk"));

					} catch (Exception exc) {
						Configuration.errorMessage = "Error: select number of top-k";
						response = Configuration.errorMessage;
						return response;
					}

					conf = new Configuration(params.get("focuses"), Integer.parseInt(params.get("diameter")),
							Integer.parseInt(params.get("eventSize")), params.get("dataGraph").toLowerCase().trim(),
							Integer.parseInt(params.get("topk")), Double.parseDouble(params.get("threshold")),
							Integer.parseInt(params.get("cache")), windowMode, Integer.parseInt(params.get("winSize")),
							Integer.parseInt(params.get("snapshots")), Double.parseDouble(params.get("decay")));

					if (Configuration.hasError) {
						response = Configuration.errorMessage;
						Configuration.hasError = false;
						return response;
					}

					EchoServer.finalResult = EchoServer.conf.getTheFinalResult().toJSONString();

					if (Configuration.hasError) {
						response = Configuration.errorMessage;
						Configuration.hasError = false;
						return response;
					}
					response = EchoServer.finalResult;
					return response;
				}

				// simpleThread= new Thread(new SimpleThread());
				// simpleThread.start();
				// response = "thread started";
				// return response;
				// } else if (reqType.equals("1")) {
				// while (true) {
				// // System.out.println("one"+stateOne);
				// TimeUnit.SECONDS.sleep(1);
				// if (stateOne) {
				// response = "one completed";
				// break;
				// }
				// }
				// return response;
				//
				// } else if (reqType.equals("2")) {
				//
				// while (true) {
				// TimeUnit.SECONDS.sleep(1);
				// // System.out.println("two"+stateTwo);
				// if (stateTwo) {
				// response = "two completed";
				// break;
				// }
				// }
				// return response;
				//
				// } else if (reqType.equals("3")) {
				//
				// while (true) {
				// // System.out.println("Three"+stateThree);
				// TimeUnit.SECONDS.sleep(1);
				// if (stateThree) {
				// response = "three completed";
				// break;
				// }
				// }
				// return response;
				//
				// } else if (reqType.equals("4")) {
				//
				// while (true) {
				// TimeUnit.SECONDS.sleep(1);
				// // System.out.println("Four"+stateFour);
				// // TimeUnit.SECONDS.sleep(1);
				// if (stateFour) {
				// if (Configuration.hasError) {
				// response = Configuration.errorMessage;
				// response = finalResult;
				// simpleThread.stop();
				// return response;
				// }
				//
				// break;
				// }
				// }
				// return response;
				// }
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			response = "Error: There is an error in inputs, please try again";
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response = "Error: There is an error in inputs, please try again";
		}
		System.out.println(Configuration.hasError);
		if (Configuration.hasError) {
			response = Configuration.errorMessage;
		}
		return response;
	}

	public static Map<String, String> getQueryParams(String query) throws UnsupportedEncodingException {

		Map<String, String> params = new HashMap<String, String>();
		for (String param : query.split("&")) {
			String[] pair = param.split("=");
			String key = URLDecoder.decode(pair[0], "UTF-8");
			String value = "";
			if (pair.length > 1) {
				value = URLDecoder.decode(pair[1], "UTF-8");
			}

			String values = params.get(key);
			if (values == null) {
				values = new String();
				params.put(key, value);
			}
			values = value;
		}

		return params;

	}
}

// class SimpleThread extends Thread {
// public SimpleThread() {
// // EchoServer.stateOne= false;
// // EchoServer.stateTwo= false;
// // EchoServer.stateThree= false;
// // EchoServer.stateFour= false;
// }
//
//// public void run() {
//// try {
//// EchoServer.finalResult =
// EchoServer.conf.getTheFinalResult().toJSONString();
//// TimeUnit.SECONDS.sleep(2);
//// System.out.println("one added");
//// EchoServer.stateOne = true;
//// TimeUnit.SECONDS.sleep(2);
//// EchoServer.stateTwo = true;
//// System.out.println("two added");
//// TimeUnit.SECONDS.sleep(2);
//// EchoServer.stateThree = true;
//// System.out.println("three added");
//// TimeUnit.SECONDS.sleep(2);
//// EchoServer.stateFour = true;
//// System.out.println("four added");
//// } catch (InterruptedException e) {
//// // TODO Auto-generated catch block
//// e.printStackTrace();
//// } catch (Exception e) {
//// // TODO Auto-generated catch block
//// e.printStackTrace();
//// }
////
//// }
// }
