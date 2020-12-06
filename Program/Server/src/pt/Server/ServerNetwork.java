package pt.Server;

import pt.Common.*;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

public class ServerNetwork extends Thread {
	
	private final ServerMain serverMain;
	private MulticastSocket multicastSocket;
	private final int synchronizerUDPPort;
	private final ServerAddress ownPublicAddress;
	private final ServerAddress ownAddress;
	private final ArrayList<ServerStatus> serversList = new ArrayList<>();
	private Thread heartbeatSend;
	private Thread heartbeatCheck;
	private boolean stop = false;
	private int synchronizationFakeUsers;
	private final MulticastSocketReceiver socketReceiver;
	
	ServerNetwork(ServerMain serverMain, InetAddress group, int port, int serverUDPPort) throws IOException {
		this.serverMain = serverMain;
		InetAddress publicIPAddress = Utils.getPublicIp();
		if (publicIPAddress == null)
			publicIPAddress = InetAddress.getLocalHost();
		
		ownPublicAddress = new ServerAddress(publicIPAddress, serverUDPPort);
		ownAddress = new ServerAddress(InetAddress.getLocalHost(), serverUDPPort);
		startMulticastSocket();
		System.out.println("Own Local Address : " + ownAddress + "\t Own Public Address" + ownPublicAddress);
		//multiMan = new MulticastManager(multicastSocket, getServerAddress(), group, port);
		socketReceiver = new MulticastSocketReceiver(multicastSocket);
		
		this.synchronizerUDPPort = serverUDPPort + 1;
	}
	
	public MulticastSocket getMulticastSocket() {
		return multicastSocket;
	}
	
	public ServerAddress getServerAddress() {
		return ownAddress; //TODO IMPORTANT return publicIpAddress when on different networks
	}
	
	@Override
	public void run() {
		try {
			startHeartbeatChecker();
			startHeartbeatSender();
			
			receiveUpdates();
		} catch (Exception e) {
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
	
	public void discoverServers() throws Exception {
		System.out.println("Server Discovery ----------------------------------------------");
		thisCameOnline();
		
		multicastSocket.setSoTimeout(1000);
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
				multicastSocket.receive(packet);
				ServerCommand command = (ServerCommand) UDPHelper.readObjectFromPacket(packet);
				
				if (command.getProtocol().equals(ServerConstants.AM_ONLINE)) {
					int nConnected = (int) command.getExtras();
					ServerAddress serverAddress = command.getServerAddress();
					if (!isOwnAddress(serverAddress))
						serverConnected(new ServerStatus(nConnected, serverAddress));
				}
			}
		} catch (IOException ignore) {
			// Needed for timeout
		}
		multicastSocket.setSoTimeout(0);
		printAvailableServers();
		
		socketReceiver.start();
	}
	
	public void updateUserCount(int count) throws IOException {
		sendServerCommand(ServerConstants.UPDATE_USER_COUNT, count + synchronizationFakeUsers);
	}
	
	private void sendServerCommand(String protocol, Object extras) throws IOException {
		UDPHelper.sendUDPObject(new ServerCommand(protocol, getServerAddress(), extras), multicastSocket,
				InetAddress.getByName(ServerConstants.MULTICAST_GROUP), ServerConstants.MULTICAST_PORT);
	}
	
	private void receiveUpdates() throws Exception {
		while (!stop) {
			DatagramPacket packet = socketReceiver.waitForPacket();
			ServerCommand command;
			try {
				command = (ServerCommand) UDPHelper.readObjectFromPacket(packet);
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				continue;
			}
			ServerAddress otherServerAddress = command.getServerAddress();
			if (isOwnAddress(otherServerAddress)) {
				//System.out.println("ServerNetwork received with own address, discarding");
				continue;
			}
			
			try {
				switch (command.getProtocol()) {
					
					case ServerConstants.HEARTBEAT -> {
						ServerStatus status = getServerStatus(otherServerAddress);
						receivedHeartbeat(status, otherServerAddress);
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
						receivedSynchronizationRequest(command);
					}
					
					case ServerConstants.PROTOCOL_NEW_MESSAGE -> {
						MessageInfo message = (MessageInfo) command.getExtras();
						System.out.println("Received propagated : " + message);
						protocolReceivedNewMessage(message);
					}
					
					case ServerConstants.PROTOCOL_NEW_USER -> {
						UserInfo userInfo = (UserInfo) command.getExtras();
						System.out.println("Received propagated new user : " + userInfo);
						protocolReceivedNewUser(userInfo);
					}
					case ServerConstants.PROTOCOL_NEW_CHANNEL -> {
						ChannelInfo channelInfo = (ChannelInfo) command.getExtras();
						System.out.println("Received propagated new channel : " + channelInfo);
						protocolReceivedNewChannel(channelInfo);
					}
					case ServerConstants.PROTOCOL_REGISTER_USER_CHANNEL -> {
						Ids ids = (Ids) command.getExtras();
						System.out.println("Received propagated user registration to channel : " + ids);
						protocolReceivedRegisterUserChannel(ids);
					}
					case ServerConstants.PROTOCOL_EDITED_CHANNEL -> {
						ChannelInfo channelInfo = (ChannelInfo) command.getExtras();
						System.out.println("Received propagated channel edited : " + channelInfo);
						protocolReceivedEditedChannel(channelInfo);
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void propagateNewMessage(MessageInfo newMessage) throws IOException {
		sendAllCommand(ServerConstants.PROTOCOL_NEW_MESSAGE, newMessage);
	}
	
	public void propagateRegisterUserChannel(Ids ids) throws IOException {
		sendAllCommand(ServerConstants.PROTOCOL_REGISTER_USER_CHANNEL, ids);
	}
	
	public void propagateNewChannel(ChannelInfo channelInfo) throws IOException {
		sendAllCommand(ServerConstants.PROTOCOL_NEW_CHANNEL, channelInfo);
	}
	
	public void propagateNewUser(UserInfo userInfo) throws IOException {
		sendAllCommand(ServerConstants.PROTOCOL_NEW_USER, userInfo);
	}
	
	public void propagateChannelEdition(ChannelInfo channel) throws IOException {
		sendAllCommand(ServerConstants.PROTOCOL_EDITED_CHANNEL, channel);
	}
	
	public void protocolReceivedRegisterUserChannel(Ids ids) throws Exception {
		ChannelManager.registerUserToChannel(ids.getUserId(), ids.getChannelId());
	}
	
	public void protocolReceivedNewChannel(ChannelInfo channelInfo) throws Exception {
		ChannelManager.insertFull(channelInfo);
		serverMain.protocolReceivedNewChannel(channelInfo);
	}
	
	public void protocolReceivedEditedChannel(ChannelInfo channelInfo) throws Exception {
		ChannelManager.updateChannel(channelInfo);
		serverMain.protocolReceivedEditedChannel(channelInfo);
	}
	
	public void protocolReceivedNewUser(UserInfo userInfo) throws Exception {
		UserManager.insertFull(userInfo, "");
		//TODO receive user image
		serverMain.protocolReceivedNewUser(userInfo);
	}
	
	public void protocolReceivedNewMessage(MessageInfo message) throws Exception {
		boolean success = MessageManager.insertFull(message);
		if (!success) {
			System.out.println("Error that shouldn't happens : protocolNewMessage(MessageInfo message)");
			return;
		}
		//TODO receive file
		if (message.getType().equals(MessageInfo.TYPE_FILE)) {
		
		}
		serverMain.protocolReceivedNewMessage(message);
	}
	
	public void synchronizeDatabase() {
		//Connect to the server with the least user load at the moment
		ServerAddress otherServer = getLeastLoadServer();
		if (otherServer == null) {
			System.out.println("No others servers running. Skipping synchronization");
			return;
		}
		
		System.out.println("Syncing Database ----------------------------------------------");
		System.out.println("Creating synchronizer with address : " + getServerAddress() + " to server " + otherServer);
		Synchronizer synchronizer = new Synchronizer(otherServer, getServerAddress(), -1, null, synchronizerUDPPort);
		try {
			synchronizer.receiveData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Get all of the info after Ids of the info that this one has
		//Have to use reliable UDP and break files into 5KB chunks
	}
	
	private void receivedSynchronizationRequest(ServerCommand command) throws IOException {
		synchronizationFakeUsers += ServerConstants.FAKE_USER_SYNC_COUNT;
		updateUserCount(serverMain.getNConnectedUsers());
		
		new Thread(() -> {
			try {
				int otherServerPort = (int) command.getExtras();
				DatagramSocket datagramSocket = new DatagramSocket(synchronizerUDPPort);
				Synchronizer synchronizer = new Synchronizer(command.getServerAddress(), getServerAddress(), otherServerPort, datagramSocket, -1);
				ServerCommand feedback = new ServerCommand(ServerConstants.ASK_SYNCHRONIZER_OK, getServerAddress(), datagramSocket.getLocalPort());
				UDPHelper.sendUDPObject(feedback, datagramSocket, command.getServerAddress().getAddress(), otherServerPort);
				synchronizer.sendData();
				datagramSocket.close();
				
				synchronizationFakeUsers -= ServerConstants.FAKE_USER_SYNC_COUNT;
				updateUserCount(serverMain.getNConnectedUsers());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}).start();
	}
	
	private void serverConnected(ServerAddress serverAddress) throws IOException {
		ServerStatus server = new ServerStatus(0, serverAddress);
		serverConnected(server);
		sendServerCommand(ServerConstants.AM_ONLINE, serverMain.getNConnectedUsers());
		System.out.println("Came Online : " + server);
	}
	
	private void receivedHeartbeat(ServerStatus status, ServerAddress otherServerAddress) {
		if (status != null) {
			status.setHeartbeat(true);
			//System.out.println("set heartbeat true : " + status);
		} else {
			System.err.println("Not yet registered. Not supposed to happen\t Registering now"); // should never happen
			serverConnected(new ServerStatus(Integer.MAX_VALUE, otherServerAddress));
			printAvailableServers();
		}
	}
	
	private void thisCameOnline() throws IOException {
		sendAllCommand(ServerConstants.CAME_ONLINE);
	}
	
	private void sendAllCommand(String protocol) throws IOException {
		sendAllCommand(protocol, null);
	}
	
	private void sendAllCommand(String protocol, Object extras) throws IOException {
		sendServerCommand(protocol, extras);
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
					sendServerCommand(ServerConstants.HEARTBEAT, null);
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
			sendServerCommand(ServerConstants.CAME_OFFLINE, null);
		} catch (Exception ignore) {
		}
	}
}