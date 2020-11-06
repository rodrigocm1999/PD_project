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
	private ServerSocket serverSocket;
	
	private final String databaseAddress;
	private final String databaseName;
	private Connection databaseConnection;
	
	private static ServerMain instance;
	
	private final ArrayList<ServerUserThread> connectedMachines;
	private ServerSyncer serversManager;
	
	public static ServerMain getInstance() {
		assert instance != null;
		return instance;
	}
	
	public ServerMain(String databaseAddress, String databaseName, int listeningUDPPort, int listeningTCPPort) throws Exception {
		if (instance != null) {
			throw new Exception("Server Already Running");
		}
		instance = this;
		this.databaseAddress = databaseAddress;
		this.databaseName = databaseName;
		connectedMachines = new ArrayList<>();
		this.listeningUDPPort = listeningUDPPort;
		this.listeningTCPPort = listeningTCPPort;
	}
	
	public void start() throws Exception {
		DatagramSocket udpSocket = new DatagramSocket(listeningUDPPort);
		serverSocket = new ServerSocket(listeningTCPPort);
		
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
	}
	
	private ServerSyncer createServerSyncer() throws IOException {
		InetAddress group = InetAddress.getByName(ServerConstants.MULTICAST_GROUP);
		int port = ServerConstants.MULTICAST_PORT;
		return new ServerSyncer(this, group, port, listeningUDPPort);
	}
	
	private void synchronizeDatabase() {
		//TODO get all of the new information
		System.out.println("Syncing Database ----------------------------------------------");
		//Connect to the server with the least load at the moment
		
		//Have to use reliable UDP and break files into 5KB chunks
		
		// get all of the info after list ID of the table that I have
	}
	
	private void receiveNewUser(DatagramPacket receivedPacket, DatagramSocket udpSocket) throws IOException {
		try {
			serverSocket.setSoTimeout(Constants.CONNECTION_TIMEOUT);
			Socket socket = serverSocket.accept();
			ServerUserThread serverUserThread = new ServerUserThread(socket, serversManager.getOrderedServerAddresses());
			serverUserThread.start();
			connectedMachines.add(serverUserThread);
			serversManager.updateUserCount(getConnectedUsers());
			System.out.println(Constants.CONNECTION_ACCEPTED + " : " + socket);
		} catch (Exception e) {
			System.out.println("Catch Establish Connection : " + e.getMessage());
		}
	}
	
	private void printConnected() {
		System.out.println("Connected : ");
		for (ServerUserThread conn : connectedMachines) {
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
	
	public void removeConnected(ServerUserThread user) {
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
	
	public static void main(String[] args) throws Exception {
		
		if (args.length < 3) {
			System.out.println("Invalid Arguments : database_address, listening udp port, listening tcp port, OPTIONAL database_name");
			System.exit(-1);
		}
		String databaseAddress = args[0];
		int listeningUDPPort = 0, listeningTCPPort = 0;
		try {
			listeningUDPPort = Integer.parseInt(args[1]);
			listeningTCPPort = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid Port number(s)");
			System.exit(-1);
		}
		
		String databaseName = ServerConstants.DATABASE_NAME;
		if (args.length == 4) {
			databaseName = args[3];
		}
		
		ServerMain serverMain = new ServerMain(databaseAddress, databaseName, listeningUDPPort, listeningTCPPort);
		serverMain.start();
		
		//TODO use this --> Runtime.getRuntime().addShutdownHook();
	}
}
