package pt.Server;

import pt.Common.*;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerMain {
	
	private final int listeningUDPPort;
	private final int listeningTCPPort;
	private final int listeningFilePort;
	private ServerSocket serverSocket;
	private ServerSocket serverFileSocket;
	
	private final String databaseAddress;
	private final String databaseName;
	private Connection databaseConnection;
	
	private static ServerMain instance;
	
	private final ArrayList<UserThread> connectedMachines;
	private ServerNetwork serversManager;
	
	public static ServerMain getInstance() {
		assert instance != null;
		return instance;
	}
	
	public ServerMain(String databaseAddress, String databaseName, int listeningUDPPort, int listeningTCPPort, int listeningFilePort) throws Exception {
		if (instance != null) {
			throw new Exception("Server Already Running");
		}
		instance = this;
		this.databaseAddress = databaseAddress;
		this.databaseName = databaseName;
		connectedMachines = new ArrayList<>();
		this.listeningUDPPort = listeningUDPPort;
		this.listeningTCPPort = listeningTCPPort;
		this.listeningFilePort = listeningFilePort;
	}
	
	public void start() throws Exception {
		DatagramSocket udpSocket = new DatagramSocket(listeningUDPPort);
		serverSocket = new ServerSocket(listeningTCPPort);
		serverFileSocket = new ServerSocket(listeningFilePort);
		
		connectDatabase();
		serversManager = createServerSyncer();
		serversManager.discoverServers();
		synchronizeDatabase();
		serversManager.start();
		
		System.out.println("Server Running ------------------------------------------------");
		
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			Command command = (Command) UDPHelper.receiveUDPObject(udpSocket, receivedPacket);
			System.out.println(command);
			
			handleCommand(command, receivedPacket, udpSocket);
		}
		
	}
	
	private void handleCommand(Command command, DatagramPacket receivedPacket, DatagramSocket udpSocket) {
		switch (command.getProtocol()) {
			case Constants.ESTABLISH_CONNECTION -> {
				System.out.println("Establish Connection --> can accept user: " + !serversManager.checkIfBetterServer());
				try {
					if (!serversManager.checkIfBetterServer()) {
						UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_ACCEPTED, listeningTCPPort),
								udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
						//TODO garantir entrega MAYBE, if so, then make this non blocking, and add syncronized on connectedMachines
						receiveNewUser(receivedPacket, udpSocket);
					} else {
						ArrayList<ServerAddress> list = serversManager.getOrderedServerAddresses();
						Utils.printList(list, "Servers Sent");
						UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_REFUSED, list),
								udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public synchronized Socket acceptFileConnection(UserThread user) throws IOException {
		user.sendCommand(Constants.FILE_ACCEPT_CONNECTION, listeningFilePort);
		return serverFileSocket.accept();
	}
	
	private ServerNetwork createServerSyncer() throws IOException {
		InetAddress group = InetAddress.getByName(ServerConstants.MULTICAST_GROUP);
		int port = ServerConstants.MULTICAST_PORT;
		return new ServerNetwork(this, group, port, listeningUDPPort);
	}
	
	private void synchronizeDatabase() throws SQLException {
		//TODO get all of the new information
		System.out.println("Syncing Database ----------------------------------------------");
		//Connect to the server with the least user load at the moment
		ServerAddress server = serversManager.getOrderedServerAddresses().get(0);
		Synchronizer synchronizer = new Synchronizer(server);
		synchronizer.start();
		//Get all of the info after list ID of the table that I have
		//Have to use reliable UDP and break files into 5KB chunks
	}
	
	private void receiveNewUser(DatagramPacket receivedPacket, DatagramSocket udpSocket) throws IOException {
		try {
			serverSocket.setSoTimeout(Constants.CONNECTION_TIMEOUT);
			Socket socket = serverSocket.accept();
			UserThread userThread = new UserThread(socket, serversManager.getOrderedServerAddresses());
			userThread.start();
			connectedMachines.add(userThread);
			serversManager.updateUserCount(getConnectedUsers());
			System.out.println(Constants.CONNECTION_ACCEPTED + " : " + socket);
		} catch (Exception e) {
			System.out.println("Catch Establish Connection : " + e.getMessage());
		}
	}
	
	private void printConnected() {
		System.out.println("Connected : ");
		for (UserThread conn : connectedMachines) {
			System.out.println(conn.getSocketInformation());
		}
		System.out.println("--------------");
	}
	
	public int getUDPPort() {
		return listeningUDPPort;
	}
	
	public int getConnectedUsers() {
		return connectedMachines.size();
	}
	
	public void removeConnected(UserThread user) {
		synchronized (connectedMachines) {
			connectedMachines.remove(user);
		}
	}
	
	private void connectDatabase() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		databaseConnection = DriverManager.getConnection(ServerConstants.getDatabaseURL(databaseAddress, databaseName),
				ServerConstants.DATABASE_USER_NAME, ServerConstants.DATABASE_USER_PASSWORD);
	}
	
	public Connection getDatabaseConnection() {
		return databaseConnection;
	}
	
	public PreparedStatement getPreparedStatement(String sql) throws SQLException {
		return databaseConnection.prepareStatement(sql);
	}
	
	public String getDatabaseName() {
		return databaseName;
	}
	
	public void propagateNewMessage(MessageInfo message, UserThread adder) throws IOException {
		for (UserThread user : connectedMachines) {
			if (user != adder) {
				user.receivedPropagatedMessage(message);
			}
		}
		serversManager.propagateNewMessage(message);
	}
	
	public void propagateNewMessage(MessageInfo message) throws IOException {
		System.out.println("Received propagation");
		for (UserThread user : connectedMachines) {
			user.receivedPropagatedMessage(message);
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		/*String IP = "239.4.5.6";
		int Port = 5432;
		
		try (MulticastSocket mS = new MulticastSocket(Port)) {
			InetAddress address = InetAddress.getByName(IP);
			NetworkInterface nI = NetworkInterface.getByInetAddress(InetAddress.getByName("25.63.62.45"));
			mS.joinGroup(new InetSocketAddress(address, 5432), nI);
			//mS.joinGroup(InetAddress.getByName(IP));
			Scanner sc = new Scanner(System.in);
			System.out.println("Username: ");
			String username = sc.nextLine();
			
			new Thread(() -> {
				DatagramPacket dP = new DatagramPacket(new byte[1024], 1024);
				try {
					while (true) {
						dP.setLength(1024);
						mS.receive(dP);
						String raw = new String(dP.getData(), 0, dP.getLength());
						System.out.println(dP.getAddress().getHostName() + ":" + dP.getPort() + ": " + raw);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}).start();
			
			while (true) {
				String msg = sc.nextLine();
				if (msg.equals("exit"))
					break;
				msg = "[" + username + "]: " + msg;
				DatagramPacket dP = new DatagramPacket(msg.getBytes(), msg.getBytes().length, address, Port);
				mS.send(dP);
			}
			mS.leaveGroup(address);
		}*/
		
		
		/*Object obj = null;
		obj.toString();*/
		
		if (args.length < 4) {
			System.out.println("Invalid Arguments : database_address, listening udp port, listening tcp port, fileTransfer tcp port, OPTIONAL database_name");
			System.exit(-1);
		}
		String databaseAddress = args[0];
		int listeningUDPPort = 0;
		int listeningTCPPort = 0;
		int listeningFilePort = 0;
		try {
			listeningUDPPort = Integer.parseInt(args[1]);
			listeningTCPPort = Integer.parseInt(args[2]);
			listeningFilePort = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid Port number(s)");
			System.exit(-1);
		}
		
		String databaseName = ServerConstants.DATABASE_NAME;
		if (args.length == 5) {
			databaseName = args[4];
		}
		
		ServerMain serverMain = new ServerMain(databaseAddress, databaseName, listeningUDPPort, listeningTCPPort, listeningFilePort);
		serverMain.start();
		
		//TODO use this --> Runtime.getRuntime().addShutdownHook();
	}
}
