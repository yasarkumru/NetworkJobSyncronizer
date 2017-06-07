import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node implements Runnable {
	private static final String WAIT_RESPONSE = "wait";
	private static final String AFFIRMATIVE_RESPONSE = "printed";
	private static final String NEGATIVE_RESPONSE = "negative";

	private static final String AFFIRMATIVE_MESSAGE = AFFIRMATIVE_RESPONSE + "\n";
	private static final String WAIT_MESSAGE = WAIT_RESPONSE + "\n";
	private static final String NEGATIVE_MESSAGE = NEGATIVE_RESPONSE + "\n";

	private static final long WAIT_TIME = 500;

	private ServerSocket serverSocket;
	private boolean isPrinted = false;
	private final String ipAddressesFile;
	private final int portNumber;
	private long creationTime;

	public Node(String ipAddressesFile, int portNumber) {
		this.ipAddressesFile = ipAddressesFile;
		this.portNumber = portNumber;
		creationTime = portNumber;
	}

	@Override
	public void run() {
		
		
		try {
			Thread.sleep(Math.abs(new Random().nextLong() % 1000));
		} catch (InterruptedException e1) {
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					initServerSocket();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}).start();

		new Thread(new Runnable() {
			List<NodeInfo> readOtherNodesInfo = readOtherNodesInfo(ipAddressesFile);

			public void run() {
				isPrinted = checkIfPrinted(readOtherNodesInfo);
				if (isPrinted) {
					System.out.println("other machine printed " + portNumber);
				} else {
					isPrinted = true;
					System.out.println("we are started " + portNumber);
				}
			}
		}).start();

	}

	public void initServerSocket() throws IOException {
		serverSocket = new ServerSocket(portNumber);
		while (true) {
			Socket connectionSocket = serverSocket.accept();
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader inFromClient = new BufferedReader(
								new InputStreamReader(connectionSocket.getInputStream()));
						DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						while (true) {
							String message = inFromClient.readLine();
							long creation = Long.parseLong(message);
							outToClient = new DataOutputStream(connectionSocket.getOutputStream());
							if (isPrinted)
								outToClient.writeBytes(AFFIRMATIVE_MESSAGE);
							else if (creation < creationTime)
								outToClient.writeBytes(WAIT_MESSAGE);
							else
								outToClient.writeBytes(NEGATIVE_MESSAGE);
						}
					} catch (NumberFormatException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

		}
	}

	private boolean checkIfPrinted(List<NodeInfo> readOtherNodesInfo) {
		for (NodeInfo nodeInfo : readOtherNodesInfo) {
			try {
				Socket clientSocket = new Socket(nodeInfo.ipAddress, nodeInfo.portNumber);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				whileloop: {
					while (true) {
						outToServer.writeBytes(Long.toString(creationTime) + "\n");
						String response = inFromServer.readLine();
						switch (response) {
						case WAIT_RESPONSE:
							Thread.sleep(WAIT_TIME);
							break;
						case AFFIRMATIVE_RESPONSE:
							clientSocket.close();
							return true;
						case NEGATIVE_RESPONSE:
							clientSocket.close();
							break whileloop;

						default:
							assert(false);
						}
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
		return false;

	}

	private List<NodeInfo> readOtherNodesInfo(String filePath) {
		List<NodeInfo> otherNodes = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			while (br.ready()) {
				String readLine = br.readLine();
				String[] split = readLine.split(" ");
				otherNodes.add(new NodeInfo(split[0], split[1]));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return otherNodes;
	}
	
	class NodeInfo {
		String ipAddress;
		int portNumber;

		public NodeInfo(String ipAddress, String portNumber) {
			this.ipAddress = ipAddress;
			this.portNumber = Integer.parseInt(portNumber);
		}
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage of this program is: {path to ip addresses file} {portnumber to run}");
			System.exit(0);
		}
		String ipAddressesFile = args[0];
		int portNumber = Integer.parseInt(args[1]);
		new Node(ipAddressesFile, portNumber).run();
	}

}
