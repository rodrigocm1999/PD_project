package pt.Common;

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
		static private long idCounter;
		
		public Wrapper(Object object) {
			id = ++idCounter;
			this.object = object;
		}
	}
	//TODO test this stuff
	
	static int RETRY_LIMIT = 3;
	
	public static DatagramSocket sendUDPObjectReliably(Object object, InetAddress address, int port) throws Exception {
		DatagramSocket socket = new DatagramSocket();
		sendUDPObjectReliably(object, socket, address, port);
		return socket;
	}
	
	public static void sendUDPObject(Object obj, DatagramSocket socket, InetAddress address, int port) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(obj);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		socket.send(packet);
	}
	
	public static void sendUDPObjectReliably(Object object, DatagramSocket socket, InetAddress address, int port) throws Exception {
		
		Wrapper wrap = new Wrapper(object);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(wrap);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		
		socket.setSoTimeout(2500);
		int retries = 0;
		
		while (true) {
			if (++retries > RETRY_LIMIT) {
				//Time to give up
				break;
			}
			try {
				// Send the stuff
				socket.send(packet);
				DatagramPacket acknowledge = new DatagramPacket(
						new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
				//Receive ACK. If timeout, then try again a couple more times
				socket.receive(acknowledge);
				
				byte[] idArr = acknowledge.getData();
				int idArrLength = acknowledge.getLength();
				if (idArrLength != Long.BYTES) {
					throw new Exception("Invalid ACK");
				}
				ByteBuffer buffer = ByteBuffer.wrap(idArr, 0, idArrLength);
				long idACK = buffer.getLong();
				if (idACK != wrap.id) {
					throw new Exception("Invalid ID ACK\nThis is not supposed to EVER happen");
				}
				break;
			} catch (SocketTimeoutException ignored) {
				// If receive times out send packet again ---------------------------------------------
			}
		}
		socket.setSoTimeout(0);
	}
	
	public static Object receiveUDPObjectReliably(int port) throws IOException, ClassNotFoundException {
		return receiveUDPObjectReliably(new DatagramSocket(port));
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
	
	public static Object receiveUDPObjectReliably(DatagramSocket socket) throws IOException, ClassNotFoundException {
		
		DatagramPacket packet = new DatagramPacket(
				new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
		socket.receive(packet);
		
		//Get Object from
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
		Wrapper wrap = (Wrapper) ois.readObject();
		
		//Send ACK
		ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES).putLong(wrap.id);
		byte[] bytes = byteBuffer.array();
		packet.setData(bytes);
		packet.setLength(bytes.length);
		socket.send(packet);
		socket.close();
		
		return wrap.object;
	}
}
