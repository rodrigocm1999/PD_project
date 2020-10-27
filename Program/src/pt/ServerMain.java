package pt;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerMain {
	
	private ServerSocket serverSocket;
	private final String databaseAddress;
	private final ArrayList<ServerUser> connectedMachines;
	private Connection databaseConnection;
	private static ServerMain instance;
	
	public static ServerMain getInstance() {
		return instance;
	}
	
	public Connection getDatabaseConnection() {
		return databaseConnection;
	}
	
	public PreparedStatement getPreparedStatement(String sql) throws SQLException {
		return databaseConnection.prepareStatement(sql);
	}
	
	public ServerMain(String databaseAddress) throws Exception {
		if (instance != null) {
			throw new Exception("Server Already Running");
		}
		instance = this;
		this.databaseAddress = databaseAddress;
		connectedMachines = new ArrayList<>();
	}
	
	private boolean canAcceptNewUser() {
		return true;
	}
	
	public void Run() throws Exception {
		connectDatabase();
		
		DatagramSocket udpSocket = new DatagramSocket(Constants.SERVER_PORT);
		serverSocket = new ServerSocket(Constants.SERVER_PORT);
		
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			udpSocket.receive(receivedPacket);
			String request = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
			
			
			switch (request) {
				case Constants.ESTABLISH_CONNECTION -> {
					if (canAcceptNewUser()) {
						byte[] answer = Constants.CONNECTION_ACCEPTED.getBytes();
						DatagramPacket answerPacket = new DatagramPacket(
								answer, answer.length, receivedPacket.getAddress(), receivedPacket.getPort());
						udpSocket.send(answerPacket);
						
						try {
							serverSocket.setSoTimeout(1500);
							Socket socket = serverSocket.accept();
							ServerUser serverUser = new ServerUser(socket);
							serverUser.start();
							connectedMachines.add(serverUser);
							System.out.println(Constants.CONNECTION_ACCEPTED + " : " + socket.getInetAddress().getHostName() + ":" + socket.getLocalPort()
									+ "\tserver port : " + socket.getLocalPort());
						} catch (Exception e) {
							System.out.println("Catch Establish Connection : " + e.getMessage());
						}
					} else {
						
						byte[] answer = Constants.CONNECTION_REFUSED.getBytes();
						udpSocket.send(new DatagramPacket(answer, answer.length));
						//TODO send list with other servers
					}
				}
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		
		String databaseAddress = "localhost";
		if (args.length == 0) {
			System.out.println("No database address on arguments. Using 'localhost'");
		} else {
			databaseAddress = args[0];
		}
		
		ServerMain serverMain = new ServerMain(databaseAddress);
		serverMain.Run();
	}
	
	private void printConnected() {
		System.out.println("Connected : ");
		for (ServerUser conn : connectedMachines) {
			System.out.println(conn.getSocketInformation());
		}
		System.out.println("--------------");
	}
	
	public void removeConnected(ServerUser user) {
		connectedMachines.remove(user);
	}
	
	private void connectDatabase() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		databaseConnection = DriverManager.getConnection(Constants.DATABASE_URL, Constants.DATABASE_USER_NAME, Constants.DATABASE_USER_PASSWORD);
	}
	
}
