package pt.Common;

import pt.Server.ServerCommand;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class UDPHelper {
	
	private static class Wrapper implements Serializable {
		private static final long serialVersionUID = 333L;
		
		private final long id;
		private final Object object;
		static private long idCounter = 0;
		
		public Wrapper(Object object) {
			id = ++idCounter;
			this.object = object;
		}
		
		@Override
		public String toString() {
			return "Wrapper{id=" + id + ", object=" + object + '}';
		}
	}
	
	//TODO test reliable ones
	static final int RETRY_LIMIT = 3;
	static final int RECEIVE_TIMEOUT = 5000;
	
	/*public static DatagramSocket sendUDPObjectReliably(Object object, InetAddress address, int port) throws Exception {
		DatagramSocket socket = new DatagramSocket();
		sendUDPObjectReliably(object, socket, address, port);
		return socket;
	}*/
	
	public static void sendUDPObject(Object obj, DatagramSocket socket, InetAddress address, int port) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(obj);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		socket.send(packet);
	}
	
	public static Object receiveUDPObject(DatagramSocket socket, DatagramPacket packet) throws IOException, ClassNotFoundException {
		socket.receive(packet);
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
		return ois.readObject();
	}
	
	public static Object receiveUDPObject(DatagramSocket socket) throws IOException, ClassNotFoundException {
		DatagramPacket packet = new DatagramPacket(
				new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
		return receiveUDPObject(socket, packet);
	}
	
	/*public static void sendUDPObjectReliably(Object object, DatagramSocket socket, InetAddress address, int port) throws IOException {
		Wrapper wrap = new Wrapper(object);
		System.out.println(wrap);
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(wrap);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		
		int timeout = socket.getSoTimeout();
		socket.setSoTimeout(RECEIVE_TIMEOUT);
		int tryNumber = 0;
		
		while (true) {
			if (++tryNumber > RETRY_LIMIT) {
				//Time to give up
				break;
			}
			try {
				// Send the stuff
				socket.send(packet);
				System.out.println("Sent packet");
				DatagramPacket acknowledge = new DatagramPacket(
						new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
				//Receive ACK. If timeout, then try again a couple more times
				System.out.println("waiting for ACK");
				socket.receive(acknowledge);
				
				byte[] idArr = acknowledge.getData();
				int idArrLength = acknowledge.getLength();
				ByteBuffer buffer = ByteBuffer.wrap(idArr, 0, idArrLength);
				long idACK = buffer.getLong();
				
				System.out.println(idACK);
				if (idACK != wrap.id) {
					throw new IOException("Invalid ID ACK\nThis is not supposed to EVER happen");
				}
				break;
			} catch (SocketTimeoutException ignored) {
				// If receive times out send packet again ---------------------------------------------
			}
		}
		socket.setSoTimeout(timeout);
	}*/
	
	public static void sendUDPObjectReliably(String protocol, Object object, ServerAddress address, DatagramSocket socket) throws IOException {
		ServerCommand command = new ServerCommand(protocol, address, new Wrapper(object));
		System.out.println(command);
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(command);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address.getAddress(), address.getUDPPort());
		
		int timeout = socket.getSoTimeout();
		socket.setSoTimeout(RECEIVE_TIMEOUT);
		int tryNumber = 0;
		
		while (true) {
			if (++tryNumber > RETRY_LIMIT) {
				//Time to give up
				break;
			}
			try {
				// Send the stuff
				socket.send(packet);
				System.out.println("Sent packet");
				DatagramPacket acknowledge = new DatagramPacket(
						new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
				//Receive ACK. If timeout, then try again a couple more times
				System.out.println("waiting for ACK");
				socket.receive(acknowledge);
				
				byte[] idArr = acknowledge.getData();
				int idArrLength = acknowledge.getLength();
				ByteBuffer buffer = ByteBuffer.wrap(idArr, 0, idArrLength);
				long idACK = buffer.getLong();
				
				System.out.println(idACK);
				if (idACK != ((Wrapper) command.getExtras()).id) {
					throw new IOException("Invalid ID ACK\nThis is not supposed to EVER happen");
				}
				break;
			} catch (SocketTimeoutException ignored) {
				// If receive times out send packet again ---------------------------------------------
			}
		}
		socket.setSoTimeout(timeout);
	}
	
	public static Object receiveUDPObjectReliably(DatagramSocket socket, ServerAddress server) throws IOException, ClassNotFoundException {
		while (true) {
			DatagramPacket packet = new DatagramPacket(
					new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			socket.receive(packet);
			System.out.println("received packet");
			
			
			//Get Object from packet
			ObjectInputStream ois = new ObjectInputStream(
					new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
			ServerCommand serverCommand = (ServerCommand) ois.readObject();
			System.out.println(serverCommand);
			
			if (!serverCommand.getServerAddress().equals(server)) {
				System.out.println("Not from the right server still waiting");
				continue;
			}
			//Send ACK
			Wrapper wrapper = (Wrapper) serverCommand.getExtras();
			ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES).putLong(wrapper.id);
			byte[] bytes = byteBuffer.array();
			packet.setData(bytes);
			packet.setLength(bytes.length);
			socket.send(packet);
			socket.close();
			
			return wrapper.object;
		}
	}
}
