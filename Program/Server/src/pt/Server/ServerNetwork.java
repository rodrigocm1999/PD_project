package pt.Server;

import pt.Common.Constants;
import pt.Common.MessageInfo;
import pt.Common.ServerAddress;
import pt.Common.Utils;

import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

public class ServerNetwork extends Thread {
	
	private final ServerMain serverMain;
	private MulticastSocket multicastSocket;
	private final ServerAddress ownPublicAddress;
	private final ServerAddress ownAddress;
	private final ArrayList<ServerStatus> serversList = new ArrayList<>();
	private Thread heartbeatSend;
	private Thread heartbeatCheck;
	private boolean stop = false;
	private final MulticastManager multiMan;
	private int synchronizationFakeUsers;
	
	ServerNetwork(ServerMain serverMain, InetAddress group, int port, int serverUDPPort) throws IOException {
		this.serverMain = serverMain;
		InetAddress publicIPAddress = Utils.getPublicIp();
		if (publicIPAddress == null)
			publicIPAddress = InetAddress.getLocalHost();
		
		ownPublicAddress = new ServerAddress(publicIPAddress, serverUDPPort);
		ownAddress = new ServerAddress(InetAddress.getLocalHost(), serverUDPPort);
		System.out.println("Own Local Address : " + ownAddress + "\t Own Public Address" + ownPublicAddress);
		startMulticastSocket();
		multiMan = new MulticastManager(multicastSocket, getServerAddress(), group, port);
	}
	
	public MulticastSocket getMulticastSocket() {
		return multicastSocket;
	}
	
	public ServerAddress getServerAddress() {
		return ownAddress; // IMPORTANT return public when on different networks
	}
	
	@Override
	public void run() {
		try {
			startHeartbeatChecker();
			startHeartbeatSender();
			//TODO make everything reliable
			
			receiveUpdates();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			stop = true;
		}
	}
	
	public ArrayList<ServerAddress> getOrderedServerAddressesThisLast() {
		synchronized (serversList) {
			Collections.sort(serversList);
			ArrayList<ServerAddress> list = new ArrayList<>(serversList.size() + 1);
			for (var server : serversList) {
				list.add(server.getServerAddress());
			}
			list.add(ownPublicAddress);
			return list;
		}
	}
	
	public ServerAddress getLeastLoadServer() {
		ArrayList<ServerAddress> servers = getOrderedServerAddressesThisLast();
		if (servers.size() == 1) {
			return null;
		}
		return servers.get(0);
	}
	
	public boolean checkIfBetterServer() {
		synchronized (serversList) {
			if (serversList.size() == 0) return false;
			Collections.sort(serversList);
			ServerStatus smallest = serversList.get(0);
			//System.out.println("smol: " + smol.getConnectedUsers() + "\tthis: " + serverMain.getConnectedUsers());
			return ((float) smallest.getConnectedUsers() < // If has less then it half of this one, then it is a better server
					(float) serverMain.getNConnectedUsers() * ServerConstants.ACCEPT_PERCENTAGE_THRESHOLD);
		}
	}
	
	public void discoverServers() throws ClassNotFoundException, IOException {
		System.out.println("Server Discovery ----------------------------------------------");
		warnEveryone();
		
		multicastSocket.setSoTimeout(1000);
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
				ServerCommand command = (ServerCommand) multiMan.receiveObject(packet);
				
				if (command.getProtocol().equals(ServerConstants.AM_ONLINE)) {
					int nConnected = (int) command.getExtras();
					ServerAddress serverAddress = command.getServerAddress();
					if (!isOwnAddress(serverAddress))
						serverConnected(new ServerStatus(nConnected, serverAddress));
				}
			}
		} catch (IOException e) {
			// Needed for timeout
		}
		printAvailableServers();
		multicastSocket.setSoTimeout(0);
	}
	
	public void updateUserCount(int count) throws IOException {
		multiMan.sendServerCommand(ServerConstants.UPDATE_USER_COUNT, count + synchronizationFakeUsers);
	}
	
	public void propagateNewMessage(MessageInfo newMessage) throws IOException {
		sendAllCommand(ServerConstants.NEW_MESSAGE, newMessage);
	}
	//TODO propagate all changes
	
	private void receiveUpdates() throws IOException, ClassNotFoundException {
		while (!stop) {
			DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			ServerCommand command = (ServerCommand) multiMan.receiveObject(packet);
			ServerAddress otherServerAddress = command.getServerAddress();
			if (isOwnAddress(otherServerAddress)) {
				System.out.println("received with own address : " + command);
				continue;
			}
			System.out.println("ServerNetwork received : " + command);
			
			try {
				switch (command.getProtocol()) {
					
					case ServerConstants.HEARTBEAT -> {
						ServerStatus status = getServerStatus(otherServerAddress);
						receivedHeartbeat(status);
					}
					
					case ServerConstants.CAME_ONLINE -> {
						ServerAddress serverAddress = command.getServerAddress();
						serverConnected(serverAddress);
					}
					
					case ServerConstants.CAME_OFFLINE -> {
						ServerAddress serverAddress = command.getServerAddress();
						serverDisconnected(getServerStatus(serverAddress));
					}
					
					case ServerConstants.UPDATE_USER_COUNT -> {
						int connected = (int) command.getExtras();
						ServerStatus status = getServerStatus(otherServerAddress);
						if (status != null) {
							status.setConnectedUsers(connected);
						} else {
							System.err.println("Status == null\t Should never happen");
						}
					}
					
					case ServerConstants.ASK_SYNCHRONIZER -> {
						//System.out.println("ASK_SYNCHRONIZER");
						receivedSynchronizationRequest(command.getServerAddress());
					}
					
					case ServerConstants.NEW_MESSAGE -> {
						MessageInfo message = (MessageInfo) command.getExtras();
						System.out.println("Received propagated : " + message);
						protocolNewMessage(message);
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void synchronizeDatabase(){
		//Connect to the server with the least user load at the moment
		ServerAddress server = getLeastLoadServer();
		if (server == null || server.equals(getServerAddress())) {
			System.out.println("No others servers running. Skipping synchronization");
			return;
		}
		
		System.out.println("Syncing Database ----------------------------------------------");
		System.out.println("Creating synchronizer with address : " + getServerAddress() + " to server " + server);
		Synchronizer synchronizer = new Synchronizer(server, getServerAddress(), getMulticastSocket(), multiMan);
		try {
			synchronizer.receiveData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Get all of the info after list ID of the table that I have
		//Have to use reliable UDP and break files into 5KB chunks
	}
	
	private void receivedSynchronizationRequest(ServerAddress serverAddress) throws IOException {
		synchronizationFakeUsers += ServerConstants.FAKE_USER_SYNC_COUNT;
		updateUserCount(serverMain.getNConnectedUsers());
		Synchronizer synchronizer = new Synchronizer(serverAddress, getServerAddress(), getMulticastSocket(), multiMan);
		try {
			synchronizer.sendData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		synchronizationFakeUsers -= ServerConstants.FAKE_USER_SYNC_COUNT;
	}
	
	private void protocolNewMessage(MessageInfo message) throws IOException, SQLException {
		MessageManager.insertMessage(message);
		serverMain.propagatedNewMessage(message);
	}
	
	private void serverConnected(ServerAddress serverAddress) throws IOException {
		ServerStatus server = new ServerStatus(0, serverAddress);
		serverConnected(server);
		multiMan.sendServerCommand(ServerConstants.AM_ONLINE, serverMain.getNConnectedUsers());
		System.out.println("Came Online : " + server);
	}
	
	private void receivedHeartbeat(ServerStatus status) {
		if (status != null) {
			status.setHeartbeat(true);
			//System.out.println("set heartbeat true : " + status);
		} else {
			System.err.println("Not yet registered. Not supposed to happen\t Registering now"); // should never happen
			serverConnected(new ServerStatus(Integer.MAX_VALUE, status.getServerAddress()));
			printAvailableServers();
		}
	}
	
	private void warnEveryone() throws IOException {
		sendAllCommand(ServerConstants.CAME_ONLINE);
	}
	
	private void sendAllCommand(String protocol) throws IOException {
		sendAllCommand(protocol, null);
	}
	
	private void sendAllCommand(String protocol, Object extras) throws IOException {
		multiMan.sendServerCommand(protocol, extras);
	}
	
	private void startMulticastSocket() throws IOException {
		InetAddress group = InetAddress.getByName(ServerConstants.MULTICAST_GROUP);
		int port = ServerConstants.MULTICAST_PORT;
		multicastSocket = new MulticastSocket(port);
		SocketAddress socketAddress = new InetSocketAddress(group, port);
		NetworkInterface networkInterface = NetworkInterface.getByInetAddress(group);
		multicastSocket.joinGroup(socketAddress, networkInterface);
	}
	
	private void startHeartbeatChecker() {
		Runnable check = () -> {
			while (!stop) {
				try {
					ArrayList<ServerStatus> toRemove = new ArrayList<>();
					synchronized (serversList) {
						for (var server : serversList) {
							if (!server.getHeartbeat())
								toRemove.add(server);
							server.setHeartbeat(false);
						}
					}
					for (var server : toRemove)
						serverDisconnected(server);
					
					Thread.sleep(ServerConstants.HEARTBEAT_WAIT_INTERVAL);
				} catch (InterruptedException e) {
					System.out.println("startHeartbeatChecker : " + e.getLocalizedMessage());
					stop = true;
				}
			}
		};
		heartbeatCheck = new Thread(check);
		heartbeatCheck.start();
	}
	
	private void startHeartbeatSender() {
		Runnable send = () -> {
			while (!stop) {
				try {
					multiMan.sendServerCommand(ServerConstants.HEARTBEAT);
					//System.out.println("Heartbeat Sent");
					Thread.sleep(ServerConstants.HEARTBEAT_SEND_INTERVAL);
				} catch (InterruptedException | IOException e) {
					System.out.println("startHeartbeatSender : " + e.getLocalizedMessage());
					stop = true;
				}
			}
		};
		
		heartbeatSend = new Thread(send);
		heartbeatSend.start();
	}
	
	private void serverConnected(ServerStatus server) {
		synchronized (serversList) {
			serversList.add(server);
		}
	}
	
	private void serverDisconnected(ServerStatus server) {
		System.out.println("Server Disconnected : " + server);
		printAvailableServers();
		synchronized (serversList) {
			serversList.remove(server);
		}
	}
	
	private void printAvailableServers() {
		synchronized (serversList) {
			Utils.printList(serversList, "Available Servers");
		}
	}
	
	private ServerStatus getServerStatus(ServerAddress address) {
		synchronized (serversList) {
			ServerStatus serverStatus = null;
			for (var server : serversList) {
				if (server.getServerAddress().equals(address)) {
					serverStatus = server;
					break;
				}
			}
			if (serverStatus == null) System.err.println("Status == null\t Should never happen");
			return serverStatus;
		}
	}
	
	private boolean isOwnAddress(ServerAddress other) {
		return getServerAddress().equals(other);
	}
	
	public void sendShutdown() {
		try {
			multiMan.sendServerCommand(ServerConstants.CAME_OFFLINE);
		} catch (Exception ignore) {
		}
	}
}