package pt.Server;

import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.ServerAddress;
import pt.Common.UDPHelper;

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
	
	private final ArrayList<ServerUser> connectedMachines;
	private ArrayList<ServerAddress> serversList;
	
	public static ServerMain getInstance() {
		return instance;
	}
	
	public Connection getDatabaseConnection() {
		return databaseConnection;
	}
	
	public PreparedStatement getPreparedStatement(String sql) throws SQLException {
		return databaseConnection.prepareStatement(sql);
	}
	
	public ServerMain(String databaseAddress, String databaseName, int listeningUDPPort, int listeningTCPPort) throws Exception {
		if (instance != null) {
			throw new Exception("Server Already Running");
		}
		instance = this;
		this.databaseAddress = databaseAddress;
		this.databaseName = databaseName;
		connectedMachines = new ArrayList<>();
		serversList = new ArrayList<>();
		this.listeningUDPPort = listeningUDPPort;
		this.listeningTCPPort = listeningTCPPort;
	}
	
	public void start() throws Exception {
		DatagramSocket udpSocket = new DatagramSocket(listeningUDPPort);
		serverSocket = new ServerSocket(listeningTCPPort);
		
		connectDatabase();
		MulticastSocket multicastSocket = startMulticastSocket();
		discoverServers(multicastSocket);
		synchronizeDatabase();
		startServerSyncer(multicastSocket);
		
		System.out.println("Server Running");
		
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(
					new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			Command command = (Command) UDPHelper.receiveUDPObject(udpSocket, receivedPacket);
			
			switch (command.getProtocol()) {
				case Constants.ESTABLISH_CONNECTION -> {
					System.out.println("Establish Connection");
					try {
						if (canAcceptNewUser()) {
							UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_ACCEPTED, listeningTCPPort),
									udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
							
							//TODO garantir entrega maybe, if so, then make this non blocking
							receiveNewUser(receivedPacket, udpSocket);
						} else {
							//TODO send list with other servers
							//TODO ALL SERVER CONNECTIONS
							ArrayList<ServerAddress> serversList = getServersList();
							
							UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_REFUSED, serversList),
									udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void discoverServers(MulticastSocket multicastSocket) {
	
	
	}
	
	private void startServerSyncer(MulticastSocket multicastSocket) throws UnknownHostException {
		InetAddress group = InetAddress.getByName(Constants.MULTICAST_GROUP);
		int port = Constants.MULTICAST_PORT;
		// TODO Actually organize this crap
		ServerSyncer syncer = new ServerSyncer(multicastSocket, group, port);
	}
	
	private boolean canAcceptNewUser() {
		//TODO make this actually check if can or not
		return true;
	}
	
	private void synchronizeDatabase() {
		//TODO get all of the new information
	}
	
	private ArrayList<ServerAddress> getServersList() throws UnknownHostException {
		//TODO get servers list
		var list = new ArrayList<ServerAddress>();
		list.add(new ServerAddress(InetAddress.getByName("localhost"), 23124));
		return list;
	}
	
	private MulticastSocket startMulticastSocket() throws IOException {
		InetAddress group = InetAddress.getByName(Constants.MULTICAST_GROUP);
		int port = Constants.MULTICAST_PORT;
		MulticastSocket multicastSocket = new MulticastSocket(port);
		SocketAddress socketAddress = new InetSocketAddress(group, port);
		NetworkInterface networkInterface = NetworkInterface.getByInetAddress(group);
		multicastSocket.joinGroup(socketAddress, networkInterface);
		return multicastSocket;
	}
	
	private void receiveNewUser(DatagramPacket receivedPacket, DatagramSocket udpSocket) throws IOException {
		try {
			serverSocket.setSoTimeout((int) Constants.CONNECTION_TIMEOUT);
			Socket socket = serverSocket.accept();
			//TODO check this
			ServerUser serverUser = new ServerUser(socket);
			serverUser.sendCommand(Constants.SERVERS_LIST, getServersList());
			serverUser.start();
			synchronized (connectedMachines) {
				connectedMachines.add(serverUser);
			}
			System.out.println(Constants.CONNECTION_ACCEPTED + " : " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
		} catch (Exception e) {
			System.out.println("Catch Establish Connection : " + e.getMessage());
		}
	}
	
	private void printConnected() {
		System.out.println("Connected : ");
		for (ServerUser conn : connectedMachines) {
			System.out.println(conn.getSocketInformation());
		}
		System.out.println("--------------");
	}
	
	public void removeConnected(ServerUser user) {
		connectedMachines.remove(user);
	}
	
	private void connectDatabase() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		databaseConnection = DriverManager.getConnection(Constants.getDatabaseURL(databaseAddress,databaseName),
				Constants.DATABASE_USER_NAME, Constants.DATABASE_USER_PASSWORD);
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
			System.out.println("Invalid Port number/s");
			System.exit(-1);
		}
		
		String databaseName = Constants.DATABASE_NAME;
		if (args.length == 4) {
			databaseName = args[3];
		}
		
		ServerMain serverMain = new ServerMain(databaseAddress, databaseName, listeningUDPPort, listeningTCPPort);
		serverMain.start();
		
	}
	
}
