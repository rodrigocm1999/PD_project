package pt;

import java.io.ByteArrayOutputStream;
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
	
	public int run() throws IOException, ClassNotFoundException {
		DatagramSocket datagramSocket = new DatagramSocket();

		Command command = new Command(Constants.ESTABLISH_CONNECTION);
		UDPHelper.sendUDPObject(command,datagramSocket,InetAddress.getByName(ipServer),port);

		datagramSocket.setSoTimeout(4000);
		command = (Command) UDPHelper.receiveUDPObject(datagramSocket);

		if (command.getProtocol().equals(Constants.CONNECTION_ACCEPTED)) {
			int socketTCPort =  (int)command.getExtras();
			socket =  new Socket(ipServer, socketTCPort);
			oOS = new ObjectOutputStream(socket.getOutputStream());
			oIS =  new ObjectInputStream(socket.getInputStream());

			return 1;
		} else {
			// TODO HA DE RECEBER A LISTA DE SERVERS DISPONIVEIS
			return -1;
		}
		
	}

	public Command sendCommandToServer(String protocol, Object object) throws IOException, ClassNotFoundException {
		oOS.writeObject(new Command(protocol,object));
		return (Command)oIS.readObject();
	}

}
