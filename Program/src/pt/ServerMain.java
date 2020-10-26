package pt;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerMain {
	
	public static final int UDP_PACKET_SIZE = 256;
	public static final int SERVER_PORT = 9321;
	
	public static final String ESTABLISH_CONNECTION = "ESTABLISH_CONNECTION";
	public static final String CONNECTION_ACCEPTED = "CONNECTION_ACCEPTED";
	public static final String CONNECTION_REFUSED = "CONNECTION_REFUSED";
	
	private ServerSocket serverSocket;
	private String databaseAddress;
	private ArrayList<ServerUser> connectedMachines;
	
	public ServerMain(String databaseAddress) {
		this.databaseAddress = databaseAddress;
		connectedMachines = new ArrayList<>();
	}
	
	private boolean canAcceptNewUser() {
		return true;
	}
	
	public void Run() throws IOException {
		
		DatagramSocket udpSocket = new DatagramSocket(SERVER_PORT);
		serverSocket = new ServerSocket(SERVER_PORT);
		
		while (true) {
			DatagramPacket recievedPacket = new DatagramPacket(new byte[UDP_PACKET_SIZE], UDP_PACKET_SIZE);
			udpSocket.receive(recievedPacket);
			String request = new String(recievedPacket.getData(), 0, recievedPacket.getLength());
			
			switch (request) {
				case ESTABLISH_CONNECTION -> {
					if (canAcceptNewUser()) {
						
						byte[] answer = CONNECTION_ACCEPTED.getBytes();
						DatagramPacket answerPacket = new DatagramPacket(answer, answer.length,recievedPacket.getAddress(),recievedPacket.getPort());
						udpSocket.send(answerPacket);
						
						Socket socket = serverSocket.accept();
						ServerUser serverUser = new ServerUser(socket);
						connectedMachines.add(serverUser);
						serverUser.start();
					} else {
						
						byte[] answer = CONNECTION_REFUSED.getBytes();
						udpSocket.send(new DatagramPacket(answer, answer.length));
						//TODO send list with other servers
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
		String databaseAddress = "localhost";
		if (args.length == 0) {
			System.out.println("No database address on arguments\nUsing localhost");
		} else {
			databaseAddress = args[0];
		}
		
		ServerMain serverMain = new ServerMain(databaseAddress);
		serverMain.Run();
	}
	
}
