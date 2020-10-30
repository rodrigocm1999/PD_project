package pt.Client;

import pt.Common.Command;
import pt.Common.Constants;
import pt.Server.ServerAddress;
import pt.Common.UDPHelper;

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
			boolean success = tryConnectServer(serverIPAddress, portUDPServer, datagramSocket);
			if (success) {
				return;
			} else {
				ServerAddress serverAddress = serversList.get(0);
				serverIPAddress = serverAddress.getAddress();
				portUDPServer = serverAddress.getUDPPort();
				//TODO test this
			}
		}
	}
	
	private boolean tryConnectServer(InetAddress ipAddress, int port, DatagramSocket udpSocket) throws Exception {
		Command command = new Command(Constants.ESTABLISH_CONNECTION);
		UDPHelper.sendUDPObject(command, udpSocket, ipAddress, port);
		
		udpSocket.setSoTimeout(1500);
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
			serversList = (ArrayList<ServerAddress>) UDPHelper.receiveUDPObject(udpSocket);
			return false;
		} else {
			throw new IOException("Illegal Connection Protocol");
		}
	}
	
	public Object sendCommandToServer(String protocol, Object object) throws IOException, ClassNotFoundException {
		oOS.writeObject(new Command(protocol, object));
		Object ob = oIS.readObject();
		return ob;
	}
	
}
