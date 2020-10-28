package pt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerMain {
	
	private final int listeningUDPPort;
	private final int listeningTCPPort;
	
	private ServerSocket serverSocket;
	private final String databaseAddress;
	private final ArrayList<ServerUser> connectedMachines;
	private Connection databaseConnection;
	private static ServerMain instance;
	private final ArrayList<Thread> threads;
	
	public static ServerMain getInstance() {
		return instance;
	}
	
	public Connection getDatabaseConnection() {
		return databaseConnection;
	}
	
	public PreparedStatement getPreparedStatement(String sql) throws SQLException {
		return databaseConnection.prepareStatement(sql);
	}
	
	public ServerMain(String databaseAddress, int listeningUDPPort, int listeningTCPPort) throws Exception {
		if (instance != null) {
			throw new Exception("Server Already Running");
		}
		instance = this;
		this.databaseAddress = databaseAddress;
		connectedMachines = new ArrayList<>();
		threads = new ArrayList<>();
		this.listeningUDPPort = listeningUDPPort;
		this.listeningTCPPort = listeningTCPPort;
	}
	
	private boolean canAcceptNewUser() {
		return true;
	}
	
	public void Run() throws Exception {
		connectDatabase();
		
		DatagramSocket udpSocket = new DatagramSocket(listeningUDPPort);
		serverSocket = new ServerSocket(listeningTCPPort);
		System.out.println("Server Running");
		
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			udpSocket.receive(receivedPacket);
			String request = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
			
			
			switch (request) {
				case Constants.ESTABLISH_CONNECTION -> {
					Runnable runnable = () -> {
						try {
							if (canAcceptNewUser()) {
								Command command = new Command(Constants.CONNECTION_ACCEPTED, listeningTCPPort);
								sendUDPObject(command, udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
								//TODO garantir entrega
								receiveNewUser(receivedPacket, udpSocket);
							} else {
								
								byte[] answer = Constants.CONNECTION_REFUSED.getBytes();
								udpSocket.send(new DatagramPacket(
										answer, answer.length, receivedPacket.getAddress(), receivedPacket.getPort()));
								//TODO send list with other servers
								
								sendServersList();
							}
						} catch (Exception e) {
						
						}
					};
					//Thread thread = new Thread(runnable);
					//thread.start();
					//threads.add(thread);
					runnable.run();
				}
			}
		}
	}
	
	private void sendUDPObject(Object obj, DatagramSocket socket, InetAddress address, int port) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(obj);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		socket.send(packet);
	}
	
	private void sendServersList() {
		//TODO ALL SERVER CONNECTIONS
	}
	
	private void receiveNewUser(DatagramPacket receivedPacket, DatagramSocket udpSocket) throws IOException {
		try {
			serverSocket.setSoTimeout((int) Constants.CONNECTION_TIMEOUT);
			Socket socket = serverSocket.accept();
			ServerUser serverUser = new ServerUser(socket);
			serverUser.start();
			synchronized (connectedMachines) {
				connectedMachines.add(serverUser);
			}
			System.out.println(Constants.CONNECTION_ACCEPTED + " : " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
		} catch (Exception e) {
			System.out.println("Catch Establish Connection : " + e.getMessage());
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		/*String databaseAddress = "localhost";
		if (args.length != 3) {
			System.out.println("No database address on arguments. Using 'localhost'");
		} else {
			databaseAddress = args[0];
		}*/
		if (args.length != 3) {
			System.out.println("Invalid Arguments : database_address, listening udp port, listening tcp port");
			return;
		}
		String databaseAddress = args[0];
		int listeningUDPPort = Integer.parseInt(args[1]);
		int listeningTCPPort = Integer.parseInt(args[2]);
		
		ServerMain serverMain = new ServerMain(databaseAddress, listeningUDPPort, listeningTCPPort);
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
		databaseConnection = DriverManager.getConnection(Constants.getDatabaseURL(databaseAddress), Constants.DATABASE_USER_NAME, Constants.DATABASE_USER_PASSWORD);
	}
	
}
