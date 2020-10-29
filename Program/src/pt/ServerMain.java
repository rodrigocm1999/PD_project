package pt;

import java.awt.font.NumericShaper;
import java.io.*;
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
	private ArrayList<ServerAddress> serversList;
	
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
		serversList = new ArrayList<>();
		this.listeningUDPPort = listeningUDPPort;
		this.listeningTCPPort = listeningTCPPort;
	}
	
	public void Run() throws Exception {
		connectDatabase();
		startMulticastSocket();
		synchronizeDatabase();
		
		DatagramSocket udpSocket = new DatagramSocket(listeningUDPPort);
		serverSocket = new ServerSocket(listeningTCPPort);
		System.out.println("Server Running");
		
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			Command command = (Command) UDPHelper.receiveUDPObject(udpSocket, receivedPacket);
			
			switch (command.getProtocol()) {
				case Constants.ESTABLISH_CONNECTION -> {
					System.out.println("Establish Connection");
					Runnable runnable = () -> {
						try {
							if (canAcceptNewUser()) {
								UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_ACCEPTED, listeningTCPPort),
										udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
								
								//TODO garantir entrega maybe
								receiveNewUser(receivedPacket, udpSocket);
							} else {
								//TODO send list with other servers
								//TODO ALL SERVER CONNECTIONS
								ArrayList<ServerAddress> serversList = getServersList();
								
								UDPHelper.sendUDPObject(new Command(Constants.CONNECTION_REFUSED, serversList),
										udpSocket, receivedPacket.getAddress(), receivedPacket.getPort());
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
	
	private boolean canAcceptNewUser() {
		return true;
	}
	
	private void synchronizeDatabase() {
		//TODO get all of the new information
	}
	
	private ArrayList<ServerAddress> getServersList() throws UnknownHostException {
		//TODO get servers list
		var list = new ArrayList<ServerAddress>();
		list.add(new ServerAddress(InetAddress.getByName("localhost"),23124));
		return list;
	}
	
	private void startMulticastSocket() throws IOException {
		InetAddress group = InetAddress.getByName(Constants.MULTICAST_GROUP);
		int port = Constants.MULTICAST_PORT;
		MulticastSocket multicastSocket = new MulticastSocket(port);
		SocketAddress socketAddress = new InetSocketAddress(group,port);
		NetworkInterface networkInterface = NetworkInterface.getByInetAddress(group);
		multicastSocket.joinGroup(socketAddress,networkInterface);
	}
	
	
	private void receiveNewUser(DatagramPacket receivedPacket, DatagramSocket udpSocket) throws IOException {
		try {
			serverSocket.setSoTimeout((int) Constants.CONNECTION_TIMEOUT);
			Socket socket = serverSocket.accept();
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			//TODO check this
			oos.writeObject(getServersList());
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
		databaseConnection = DriverManager.getConnection(Constants.getDatabaseURL(databaseAddress),
				Constants.DATABASE_USER_NAME, Constants.DATABASE_USER_PASSWORD);
	}
	
	public static void main(String[] args) throws Exception {
		
		if (args.length != 3) {
			System.out.println("Invalid Arguments : database_address, listening udp port, listening tcp port");
			System.exit(-1);
		}
		String databaseAddress = args[0];
		int listeningUDPPort = 0, listeningTCPPort = 0;
		try {
			listeningUDPPort = Integer.parseInt(args[1]);
			listeningTCPPort = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid Port number/s");
			System.exit(-1);
		}
		
		
		ServerMain serverMain = new ServerMain(databaseAddress, listeningUDPPort, listeningTCPPort);
		serverMain.Run();
		
		
		/*String IP = "238.254.254.254";
		int Port = 5432;
		InetAddress ia = InetAddress.getByName(IP);
		NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
		
		MulticastSocket MSoc = new MulticastSocket(Port);
		MSoc.setTimeToLive(200);
		SocketAddress sa = new InetSocketAddress(IP, Port);
		MSoc.joinGroup(ia);
		
		new Thread(() -> {
			try {
				var b = "Yert bageette Mundo".getBytes();
				DatagramPacket p = new DatagramPacket(b, b.length, ia, Port);
				Thread.sleep(500);
				MSoc.send(p);
				MSoc.leaveGroup(sa, ni);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
		
		DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
		MSoc.receive(p);
		
		String str = new String(p.getData(), 0, p.getLength());
		System.out.println("Data: " + str);*/
		
		
	}
}
