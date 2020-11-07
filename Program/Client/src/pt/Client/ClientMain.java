package pt.Client;

import pt.Common.*;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

public class ClientMain {
	
	private InetAddress serverIPAddress;
	private int portUDPServer;
	private Socket socket;
	private static ObjectOutputStream oOS;
	private static ObjectInputStream oIS;
	private ArrayList<ServerAddress> serversList;
	private static ClientMain instance;
	private ArrayList<ChannelInfo> channels;
	private UserInfo userInfo;
	private File userPhoto;
	private ChannelInfo currentChannel;
	private UserInfo currentUser;
	
	public static ClientMain getInstance() {
		return instance;
	}
	
	public ClientMain(String ipServer, int port) throws Exception {
		if (instance != null) {
			throw new Exception("Client Main Exists");
		}
		instance = this;
		this.serverIPAddress = InetAddress.getByName(ipServer);
		this.portUDPServer = port;
	}
	
	public void connectToServer() throws Exception {
		DatagramSocket datagramSocket = new DatagramSocket();
		
		while (true) {
			System.out.println(serverIPAddress + ":" + portUDPServer);
			boolean success = tryConnectServer(serverIPAddress, portUDPServer, datagramSocket);
			if (success) {
				return;
			} else {
				ServerAddress serverAddress = serversList.get(0);
				serverIPAddress = serverAddress.getAddress();
				portUDPServer = serverAddress.getUDPPort();
			}
		}
	}
	
	private boolean tryConnectServer(InetAddress ipAddress, int port, DatagramSocket udpSocket) throws Exception {
		Command command = new Command(Constants.ESTABLISH_CONNECTION);
		UDPHelper.sendUDPObject(command, udpSocket, ipAddress, port);
		
		udpSocket.setSoTimeout(2000);
		command = (Command) UDPHelper.receiveUDPObject(udpSocket);
		String protocol = command.getProtocol();
		
		if (protocol.equals(Constants.CONNECTION_ACCEPTED)) {
			int socketTCPort = (int) command.getExtras();
			socket = new Socket(ipAddress, socketTCPort);
			oOS = new ObjectOutputStream(socket.getOutputStream());
			oIS = new ObjectInputStream(socket.getInputStream());
			
			command = (Command) oIS.readObject();
			if (!command.getProtocol().equals(Constants.SERVERS_LIST)) {
				throw new Exception("Should not happen");
			}
			serversList = (ArrayList<ServerAddress>) command.getExtras();
			return true;
		} else if (protocol.equals(Constants.CONNECTION_REFUSED)) {
			// TODO Garantir que recebe
			serversList = (ArrayList<ServerAddress>) command.getExtras();
			return false;
		} else {
			throw new IOException("Illegal Connection Protocol");
		}
	}
	
	public Object sendCommandToServer(String protocol, Object object) throws IOException, ClassNotFoundException {
		oOS.writeObject(new Command(protocol, object));
		Object ob = oIS.readObject();
		System.out.println(ob);
		return ob;
	}
	
	public boolean logout() throws IOException, ClassNotFoundException {
		Command command = (Command) sendCommandToServer(Constants.LOGOUT, null);
		// wait for response
		return true;
	}
	
	public UserInfo getUserInfo() {
		return userInfo;
	}
	
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}
	
	public ChannelInfo getChannelByName(String name) {
		for (var channel : channels) {
			if (name.equals(channel.getName())) {
				return channel;
			}
		}
		return null;
	}
	
	public ArrayList<MessageInfo> getMessagesFromChannel(int id) throws IOException, ClassNotFoundException {
		Command command = (Command) sendCommandToServer(Constants.CHANNEL_GET_MESSAGES, new Ids(-1, id, -1));
		return (ArrayList<MessageInfo>) command.getExtras();
	}
	
	public ArrayList<ChannelInfo> getChannels() {
		return channels;
	}
	
	public void setChannels(ArrayList<ChannelInfo> channels) {
		this.channels = channels;
	}
	
	public File getUserPhoto() {
		return userPhoto;
	}
	
	public void setUserPhoto(File userPhoto) {
		this.userPhoto = userPhoto;
	}
	
	public ChannelInfo getCurrentChannel() {
		return currentChannel;
	}
	
	public void setCurrentChannel(ChannelInfo currentChannel) {
		this.currentChannel = currentChannel;
	}
	
	public UserInfo getCurrentUser() {
		return currentUser;
	}
	
	public void setCurrentUser(UserInfo currentUser) {
		this.currentUser = currentUser;
	}
}
