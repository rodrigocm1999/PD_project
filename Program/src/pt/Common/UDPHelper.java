package pt.Common;


import java.io.*;
import java.net.*;

public class UDPHelper {
	
	public static void sendUDPObject(Object obj, DatagramSocket socket, InetAddress address, int port)
			throws IOException {
		byte[] bytes = writeObjectToArray(obj);
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		//System.out.println("Sent Packet to address : " + address + ":" + port + "\t obj : " + obj);
		socket.send(packet);
	}
	
	public static Object receiveUDPObject(DatagramSocket socket, DatagramPacket packet)
			throws IOException, ClassNotFoundException {
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
	
	/*public static void sendServerCommandReliably(String protocol, Object object, ServerAddress address, int port, DatagramSocket socket) throws IOException {
		Wrapper wrapperToSend = new Wrapper(object);
		long sentAckId = wrapperToSend.id;
		ServerCommand command = new ServerCommand(protocol, address, wrapperToSend);
		
		byte[] bytes = UDPHelper.writeObjectToArray(command);
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address.getAddress(), port);
		
		int timeout = socket.getSoTimeout();
		socket.setSoTimeout(RECEIVE_TIMEOUT);
		
		int tryNumber = 0;
		
		while (true) {
			if (++tryNumber > RETRY_LIMIT) {
				//Time to give up
				break;
			}
			// Send the stuff
			socket.send(packet);
			System.out.println("Sent Packet to address : " + packet.getAddress() + ":" + packet.getPort() + "\t obj : " + command);
			System.out.println("waiting for ACK");
			
			//Receive ACK. If timeout, then try again a couple more times
			if (getAck(socket, sentAckId)) {
				break;
			}
		}
		socket.setSoTimeout(timeout);
	}*/
	
	/*public static ServerCommand receiveServerCommandObjectReliably(MulticastSocketReceiver socketReceiver, DatagramSocket socket, ServerAddress server) throws
			IOException, ClassNotFoundException, InterruptedException {
		while (true) {
			DatagramPacket packet = socketReceiver.waitForPacket();
			
			//Get Object from packet
			ServerCommand serverCommand = (ServerCommand) readObjectFromPacket(packet);
			
			ServerAddress ownServerAddress = ServerMain.getInstance().getServersManager().getServerAddress();
			if (!serverCommand.getServerAddress().equals(ownServerAddress)
					|| serverCommand.getProtocol().equals(ServerConstants.ACKNOWLEDGE)) {
				 Server sends this own server address so it knows its for him
				System.out.println("Not for this server, still waiting : " + serverCommand);
				continue;
			}
			
			System.out.println("Received object : " + serverCommand);
			
			if (!(serverCommand.getExtras() instanceof Wrapper)) {
				System.out.println("Not for me");
				continue;
			}
			Wrapper wrapper = (Wrapper) serverCommand.getExtras();
			
			//Send ACK
			byte[] bytes = writeServerCommandToArray(ServerConstants.ACKNOWLEDGE, server, wrapper.id);
			// send to server its own ServerAddress so the ServerNetwork loop ignores this UDP packet
			
			//packet.setData(bytes);
			//packet.setLength(bytes.length);
			
			DatagramPacket ackPacket = new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort());
			
			for (int i = 0; i < ACK_SENT_AMOUNT; i++) {
				socket.send(ackPacket); // Send ACK
			}
			System.out.println("sent ACK : " + wrapper.id + " to ip: " + packet.getAddress() + ":" + packet.getPort() + " \n\t to server :" + server);
			
			serverCommand.setExtras(wrapper.object);
			return serverCommand;
		}
	}*/
	
	public static byte[] writeServerCommandToArray(String protocol, ServerAddress serverAddress, Object extras)
			throws IOException {
		return null;//writeObjectToArray(new ServerCommand(protocol, serverAddress, extras));
	}
	
	public static byte[] writeObjectToArray(Object object) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeUnshared(object);
		return byteArrayOutputStream.toByteArray();
	}
	
	public static Object readObjectFromPacket(DatagramPacket packet) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
		return ois.readObject();
	}
}
