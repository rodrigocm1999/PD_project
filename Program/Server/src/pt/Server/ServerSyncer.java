package pt.Server;

import pt.Common.Constants;
import pt.Common.ServerAddress;
import pt.Common.Utils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

public class ServerSyncer extends Thread {
	
	private final ServerMain serverMain;
	private MulticastSocket socket;
	private final ServerAddress ownPublicAddress;
	private final ServerAddress ownAddress;
	private final ArrayList<ServerStatus> serversList;
	private Thread heartbeatSend;
	private Thread heartbeatCheck;
	private boolean stop;
	private final MulticastManager multiMan;
	
	ServerSyncer(ServerMain serverMain, InetAddress group, int port, int serverUDPPort) throws IOException {
		this.serverMain = serverMain;
		serversList = new ArrayList<>();
		stop = false;
		InetAddress publicIPAddress = Utils.getPublicIp();
		if (publicIPAddress == null)
			publicIPAddress = InetAddress.getLocalHost();
		
		ownPublicAddress = new ServerAddress(publicIPAddress, serverUDPPort);
		ownAddress = new ServerAddress(InetAddress.getLocalHost(), serverUDPPort);
		System.out.println("Own Local Address : " + ownAddress + "\t Own Public Address" + ownPublicAddress);
		startMulticastSocket();
		multiMan = new MulticastManager(socket, ownPublicAddress, group, port);
	}
	
	@Override
	public void run() {
		try {
			startHeartbeatChecker();
			startHeartbeatSender();
			//TODO fix outside IP instead of local
			//TODO make everything reliable
			
			receiveUpdates();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			stop = true;// TODO maybe when stopping send a packet to receive and sheet
		}
	}
	
	private void receiveUpdates() throws IOException, ClassNotFoundException {
		while (!stop) {
			DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			ServerCommand command = (ServerCommand) multiMan.receiveObject(packet);
			
			ServerAddress otherServerAddress = command.getServerAddress();
			if (isOwnAddress(otherServerAddress))
				continue;
			
			try {
				
				switch (command.getProtocol()) {
					
					case ServerConstants.CAME_ONLINE -> {
						ServerAddress serverAddress = command.getServerAddress();
						ServerStatus server = new ServerStatus(0,serverAddress);
						serverConnected(server);
						multiMan.sendServerCommand(ServerConstants.AM_ONLINE, serverMain.getConnectedUsers());
						System.out.println("Came Online : " + server);
					}
					
					case ServerConstants.HEARTBEAT -> {
						//System.out.println("got heartbeat -> address : " + otherAddress);
						ServerStatus status = getServerStatus(otherServerAddress);
						if (status != null) {
							status.setHeartbeat(true);
							//System.out.println("set heartbeat true : " + status);
						} else {
							System.out.println("Not yet registered. Not supposed to happen\t Registering now"); // should never happen
							serverConnected(new ServerStatus(Integer.MAX_VALUE, otherServerAddress));
							printAvailableServers();
						}
					}
					
					case ServerConstants.UPDATE_USER_COUNT -> {
						int connected = (int) command.getExtras();
						ServerStatus status = getServerStatus(otherServerAddress);
						if (status != null) {
							status.setConnectedUsers(connected);
						}else{
							System.out.println("Status == null\t Should never happen");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void warnEveryone() throws IOException {
		multiMan.sendServerCommand(ServerConstants.CAME_ONLINE);
	}
	
	private void startMulticastSocket() throws IOException {
		InetAddress group = InetAddress.getByName(ServerConstants.MULTICAST_GROUP);
		int port = ServerConstants.MULTICAST_PORT;
		socket = new MulticastSocket(port);
		SocketAddress socketAddress = new InetSocketAddress(group, port);
		NetworkInterface networkInterface = NetworkInterface.getByInetAddress(group);
		socket.joinGroup(socketAddress, networkInterface);
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
		System.out.println("Available Servers ---------------------------------------------");
		synchronized (serversList) {
			for (var server : serversList) System.out.println(server);
		}
		System.out.println("---------------------------------------------------------------");
	}
	
	private ServerStatus getServerStatus(ServerAddress address) {
		synchronized (serversList) {
			for (var server : serversList)
				if (server.getServerAddress().equals(address))
					return server;
			return null;
		}
	}
	
	private boolean isOwnAddress(ServerAddress other) {
		return ownPublicAddress.equals(other);
	}
	
	public ArrayList<ServerAddress> getOrderedServerAddresses() {
		synchronized (serversList) {
			boolean alreadyAddedSelf = false;
			Collections.sort(serversList);
			ArrayList<ServerAddress> list = new ArrayList<>(serversList.size() + 1);
			for (int i = 0; i < serversList.size(); i++) {
				ServerStatus server = serversList.get(i);
				list.add(server.getServerAddress());
				if (!alreadyAddedSelf && i != 0 && serverMain.getConnectedUsers() > server.getConnectedUsers()) {
					list.add(ownPublicAddress);
					alreadyAddedSelf = true;
				}
			}
			if (!alreadyAddedSelf) {
				list.add(ownPublicAddress);
			}
			return list;//TODO fix duplicated servers bug, not in this function though
		}
	}
	
	public boolean checkIfBetterServer() {
		//TODO test this
		synchronized (serversList) {
			if (serversList.size() == 0) return false;
			Collections.sort(serversList);
			ServerStatus smol = serversList.get(0);
			//System.out.println("smol: " + smol.getConnectedUsers() + "\tthis: " + serverMain.getConnectedUsers());
			return ((float) smol.getConnectedUsers() < // If has less then it half of this one, then it is a better server
					(float) serverMain.getConnectedUsers() * ServerConstants.ACCEPT_PERCENTAGE_THRESHOLD);
		}
	}
	
	public void discoverServers() throws ClassNotFoundException, IOException {
		System.out.println("Server Discovery ----------------------------------------------");
		warnEveryone();
		
		socket.setSoTimeout(1500);
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
		socket.setSoTimeout(0);
	}
	
	public void updateUserCount(int count) throws IOException {
		multiMan.sendServerCommand(ServerConstants.UPDATE_USER_COUNT, count);
	}
}
