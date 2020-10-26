package pt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class ServerMain {
	
	public static final int UDP_PACKET_SIZE = 256;
	public static final int SERVER_PORT = 9321;
	
	public static final String ESTABLISH_CONNECTION = "ESTABLISH_CONNECTION";
	public static final String CONNECTION_ACCEPTED = "CONNECTION_ACCEPTED";
	public static final String CONNECTION_REFUSED = "CONNECTION_REFUSED";
	
	private ServerSocket serverSocket;
	private String databaseAddress;
	
	public ServerMain(String databaseAddress) {
		this.databaseAddress = databaseAddress;
	}
	
	private boolean canAcceptNewUser(){
		return true;
	}
	
	public void Run() throws IOException {
		
		DatagramSocket udpSocket = new DatagramSocket(SERVER_PORT);
		
		
		while (true) {
			DatagramPacket packet = new DatagramPacket(new byte[UDP_PACKET_SIZE], UDP_PACKET_SIZE);
			
			udpSocket.receive(packet);
			
			String request = new String(packet.getData(), 0, packet.getLength());
			
			switch (request) {
				case ESTABLISH_CONNECTION -> {
					if(canAcceptNewUser()){
						
						byte[] answer = CONNECTION_ACCEPTED.getBytes();
						udpSocket.send(new DatagramPacket(answer,answer.length));
						
					}
				}
			}
			
		}
		
		
	}
	
	public static void main(String[] args) throws IOException {
		
		/*Class.forName("com.mysql.jdbc.Driver").newInstance();
		Connection con = DriverManager.getConnection(
				"jdbc:mysql://rodrigohost.ddns.net:3306/main","server","VeryStrongPassword");
		Statement stmt = con.createStatement();*/
		
		String databaseAddress = args[0];
		
		ServerMain serverMain = new ServerMain(databaseAddress);
		serverMain.Run();
	}
	
}
