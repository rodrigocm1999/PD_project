package pt.Server;

import pt.Common.Constants;
import pt.Common.ServerAddress;
import pt.Common.UDPHelper;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastManager {
	
	private MulticastSocket socket;
	private ServerAddress ownServerAddress;
	private InetAddress group;
	private int port;
	
	private static MulticastManager instance;
	
	public MulticastManager getInstance() {
		return instance;
	}
	
	public MulticastManager(MulticastSocket socket, ServerAddress ownServerAddress, InetAddress group, int port) {
		if (instance != null) {
			System.out.println("ERROR created second MulticastManager");
		}
		instance = this;
		this.socket = socket;
		this.ownServerAddress = ownServerAddress;
		this.group = group;
		this.port = port;
	}
	
	public void sendServerCommand(String protocol) throws IOException {
		sendServerCommand(protocol, null);
	}
	
	public void sendServerCommand(String protocol, Object extras) throws IOException {
		byte[] bytes = UDPHelper.writeServerCommandToArray(protocol, ownServerAddress, extras);
		
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, port);
		socket.send(packet);
	}
	
	public void sendObject(Object obj) throws IOException {
		byte[] bytes = UDPHelper.writeObjectToArray(obj);
		
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, port);
		socket.send(packet);
	}
	
	public Object receiveObject() throws IOException, ClassNotFoundException {
		DatagramPacket packet = new DatagramPacket(
				new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
		return receiveObject(packet);
	}
	
	public Object receiveObject(DatagramPacket packet) throws IOException, ClassNotFoundException {
		socket.receive(packet);
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
		return ois.readObject();
	}
	
}
