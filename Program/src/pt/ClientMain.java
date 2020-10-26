package pt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ClientMain {
	
	private String ipServer;
	private int port;
	private Socket socket;
	
	
	public ClientMain(String ipServer, int port) {
		this.ipServer = ipServer;
		this.port = port;
	}
	
	public int run() throws IOException {
		DatagramSocket datagramSocket = new DatagramSocket();
		
		byte[] buff = ServerMain.ESTABLISH_CONNECTION.getBytes();
		InetAddress ip = InetAddress.getByName(ipServer);
		DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length, ip, port);
		
		datagramSocket.send(datagramPacket);
		
		datagramPacket = new DatagramPacket(new byte[ServerMain.UDP_PACKET_SIZE], ServerMain.UDP_PACKET_SIZE);
		datagramSocket.setSoTimeout(2000);
		datagramSocket.receive(datagramPacket);
		
		String str = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
		System.out.println(str);
		
		
		if (str.equals(ServerMain.CONNECTION_ACCEPTED)) {
			socket =  new Socket(ipServer, ServerMain.SERVER_PORT);
			return 1;
		} else {
			
			return -1;
		}
		
	}
}
