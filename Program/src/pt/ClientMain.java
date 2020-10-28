package pt;

import java.io.IOException;
import java.io.ObjectInputStream;
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
	private static ObjectOutputStream oOS;
	private static ObjectInputStream oIS;

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
			oOS = new ObjectOutputStream(socket.getOutputStream());
			oIS =  new ObjectInputStream(socket.getInputStream());

			return 1;
		} else {
			
			return -1;
		}
		
	}

	public String[] userRegistration(UserInfo user){
		String[] strSplited = null;
		try {
			//ObjectOutputStream ooS = new ObjectOutputStream(socket.getOutputStream());
			//ObjectInputStream oIS =  new ObjectInputStream(socket.getInputStream());
			oOS.writeObject(Constants.REGISTER);

			oOS.writeObject(user);

			String serverAnswer =  (String)oIS.readObject();
			strSplited = serverAnswer.split(";");


			return(strSplited);

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return(null);
	}

	public String[] userLogin(UserInfo user){
		String[] strSplited = null;

		try {
			oOS.writeObject(Constants.LOGIN);

			oOS.writeObject(user);

			String serverAnswer = (String)oIS.readObject();
			strSplited = serverAnswer.split(";");

			return(strSplited);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		return(null);
	}


}
