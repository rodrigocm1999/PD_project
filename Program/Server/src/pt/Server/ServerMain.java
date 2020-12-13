package pt.Server;

import pt.Common.*;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
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
		serversManager = createServerNetwork();
		serversManager.discoverServers();
		serversManager.synchronizeDatabase();
		
		Runtime.getRuntime().addShutdownHook(new shutdownHook());
		
		serversManager.start();
		startUpdateClientsServersList();
		
		System.out.println("Server Running ------------------------------------------------");
		
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(new byte[Constants.UDP_MAX_PACKET_SIZE], Constants.UDP_MAX_PACKET_SIZE);
			Command command;
			try {
				command = (Command) UDPHelper.receiveUDPObject(udpSocket, receivedPacket);
			} catch (ClassCastException e) {
				e.printStackTrace();
				continue;
			}
			System.out.println(command);
			
			handleCommand(command, receivedPacket, udpSocket);
		}
	}
	
	public ServerNetwork getServersManager() {
		return serversManager;
	}
	
	private void handleCommand(Command command, DatagramPacket receivedPacket, DatagramSocket udpSocket) {
		switch (command.getProtocol()) {
			case Constants.ESTABLISH_CONNECTION -> {
				System.out.println("Establish Connection --> can accept user: " + !serversManager.checkIfBetterServer());
				try {
					if (!serversManager.checkIfBetterServer()) {
						UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_ACCEPTED, listeningTCPPort),
								udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
						receiveNewUser(receivedPacket, udpSocket);
					} else {
						ArrayList<ServerAddress> list = serversManager.getOrderedServerAddressesThisLast();
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
	
	private ServerNetwork createServerNetwork() throws IOException {
		InetAddress group = InetAddress.getByName(ServerConstants.MULTICAST_GROUP);
		int port = ServerConstants.MULTICAST_PORT;
		return new ServerNetwork(this, group, port, listeningUDPPort);
	}
	
	private void receiveNewUser(DatagramPacket receivedPacket, DatagramSocket udpSocket) throws IOException {
		try {
			serverSocket.setSoTimeout(Constants.CONNECTION_TIMEOUT);
			Socket socket = serverSocket.accept();
			UserThread userThread = new UserThread(socket, serversManager.getOrderedServerAddressesThisLast());
			userThread.start();
			connectedMachines.add(userThread);
			serversManager.updateUserCount(getNConnectedUsers());
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
	
	public int getNConnectedUsers() {
		return connectedMachines.size();
	}
	
	public void removeConnected(UserThread user) throws IOException {
		synchronized (connectedMachines) {
			connectedMachines.remove(user);
			if (serversManager != null) {
				serversManager.updateUserCount(getNConnectedUsers());
			}
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
	
	public void propagateNewMessage(MessageInfo message, UserThread adder) {
		new Thread(() -> {
			try {
				serversManager.propagateNewMessage(message);
				
				for (UserThread user : connectedMachines) {
					if (user != adder) {
						user.receivedPropagatedMessage(message);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void propagateRegisterUserChannel(Ids ids) {
		new Thread(() -> {
			try {
				serversManager.propagateRegisterUserChannel(ids);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void propagateChannelEdition(ChannelInfo channel) {
		new Thread(() -> {
			try {
				serversManager.propagateChannelEdition(channel);
				
				for (UserThread user : connectedMachines) {
					user.receivedPropagatedChannel(channel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void propagateNewUser(UserInfo userInfo) throws NoSuchAlgorithmException {
		userInfo.setPassword(Utils.hashStringBase36(userInfo.getPassword()));
		new Thread(() -> {
			try {
				serversManager.propagateNewUser(userInfo);
				
				for (UserThread user : connectedMachines) {
					user.receivedPropagatedUser(userInfo);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void propagateNewChannel(ChannelInfo channel) {
		new Thread(() -> {
			try {
				serversManager.propagateNewChannel(channel);
				
				for (UserThread user : connectedMachines) {
					user.receivedPropagatedChannel(channel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void protocolReceivedNewMessage(MessageInfo message) {
		new Thread(() -> {
			System.out.println("Received propagation new Message");
			try {
				for (var user : connectedMachines) {
					user.receivedPropagatedMessage(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void protocolReceivedNewUser(UserInfo user) throws IOException {
		System.out.println("Propagated user : " + user);
		for (var userThread : connectedMachines) {
			userThread.receivedPropagatedUser(user);
		}
	}
	
	public void protocolReceivedNewChannel(ChannelInfo channel) throws IOException {
		System.out.println("Propagated channel: " + channel);
		for (var user : connectedMachines) {
			user.receivedPropagatedChannel(channel);
		}
	}
	
	public void protocolReceivedEditedChannel(ChannelInfo channel) throws IOException {
		System.out.println("Propagated channel edition: " + channel);
		for (var user : connectedMachines) {
			user.receivedPropagatedChannel(channel);
		}
	}
	
	private void startUpdateClientsServersList() {
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(ServerConstants.SERVERS_LIST_INTERVAL);
					
					ArrayList<ServerAddress> serversList = serversManager.getOrderedServerAddressesThisLast();
					//System.out.println("Servers List to all clients : " + serversList);
					for (var client : connectedMachines) {
						client.sendCommand(Constants.SERVERS_LIST, serversList);
					}
				} catch (Exception e) {
				}
			}
		}).start();
	}
	
	public void shutdown() {
		serversManager.sendShutdown();
		serversManager = null;
		
		for (var connection : connectedMachines) {
			connection.disconnect();
		}
	}
	
	private static class shutdownHook extends Thread {
		@Override
		public void run() {
			ServerMain.getInstance().shutdown();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		if (args.length < 4) {
			System.out.println("Invalid Arguments : database_address, listening udp port (+1 for server synchronization), listening tcp port, fileTransfer tcp port, OPTIONAL database_name");
			System.exit(-1);
		}
		String databaseAddress = args[0];
		int listeningUDPPort = 0, listeningFilePort = 0, listeningTCPPort = 0;
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
		
		new ServerMain(databaseAddress, databaseName, listeningUDPPort, listeningTCPPort, listeningFilePort)
				.start();
	}
}
