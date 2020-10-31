package pt.Server;

import pt.Common.Constants;
import pt.Common.ServerAddress;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastManager {
	
	private MulticastSocket socket;
	private ServerAddress ownServerAddress;
	private InetAddress group;
	private int port;
	
	public MulticastManager(MulticastSocket socket, ServerAddress ownServerAddress, InetAddress group, int port) {
		this.socket = socket;
		this.ownServerAddress = ownServerAddress;
		this.group = group;
		this.port = port;
	}
	
	public void sendServerCommand(String protocol) throws IOException {
		sendServerCommand(protocol, null);
	}
	
	public void sendServerCommand(String protocol, Object extras) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeUnshared(new ServerCommand(protocol, ownServerAddress, extras));
		byte[] bytes = byteArrayOutputStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, port);
		socket.send(packet);
	}
	
	public void sendObject(Object obj) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeUnshared(obj);
		byte[] bytes = byteArrayOutputStream.toByteArray();
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
