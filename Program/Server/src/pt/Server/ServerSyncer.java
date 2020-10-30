package pt.Server;

import pt.Common.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ServerSyncer extends Thread {
	
	private MulticastSocket socket;
	private InetAddress group;
	private int port;
	
	ServerSyncer(MulticastSocket socket, InetAddress group, int port){
		this.socket = socket;
		this.group = group;
		this.port = port;
	}
	
	@Override
	public void run() {
		try {
			socket.receive(new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE],Constants.UDP_PACKET_SIZE));
			
			
			
			
		}catch (IOException e){
		
		
		}
	}
}
