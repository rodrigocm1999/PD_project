package pt;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ClientMain {
	
	private String ipServer;
	private int port;
	private Socket socket;
	private static ClientMain instance;

	public static ClientMain getInstance() {
		return instance;
	}

	public ClientMain(String ipServer, int port) throws Exception {
		if (instance != null) {
			throw new Exception("Client Main Exists");
		}
		instance = this;
		this.ipServer = ipServer;
		this.port = port;
	}
	
	public int run() throws IOException {
		DatagramSocket datagramSocket = new DatagramSocket();
		
		byte[] buff = Constants.ESTABLISH_CONNECTION.getBytes();
		InetAddress ip = InetAddress.getByName(ipServer);
		DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length, ip, port);
		
		datagramSocket.send(datagramPacket);
		
		datagramPacket = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
		datagramSocket.setSoTimeout(2000);
		datagramSocket.receive(datagramPacket);
		
		String str = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
		System.out.println(str);
		
		
		if (str.equals(Constants.CONNECTION_ACCEPTED)) {
			socket =  new Socket(ipServer, Constants.SERVER_PORT);


			return 1;
		} else {
			
			return -1;
		}
		
	}

	public void userRegistration(UserInfo user){
		try {
			ObjectOutputStream ooS = new ObjectOutputStream(socket.getOutputStream());
			ooS.writeObject(Constants.REGISTER);

			ooS.writeObject(user);


		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
