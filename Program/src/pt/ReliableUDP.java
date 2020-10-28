package pt;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class ReliableUDP {
	
	private static class Wrapper implements Serializable {
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
	
	public static DatagramSocket sendUDPObject(Object object, InetAddress address, int port) throws Exception {
		DatagramSocket socket = new DatagramSocket();
		sendUDPObject(object, socket, address, port);
		return socket;
	}
	
	public static void sendUDPObject(Object object, DatagramSocket socket, InetAddress address, int port) throws Exception {
		
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
	
	public static Object receiveUDPObject(int port) throws IOException, ClassNotFoundException {
		return receiveUDPObject(new DatagramSocket(port));
	}
	
	public static Object receiveUDPObject(DatagramSocket socket) throws IOException, ClassNotFoundException {
		
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
