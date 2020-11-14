package pt.Server;

import pt.Common.Command;
import pt.Common.ServerAddress;
import pt.Common.UDPHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Tests {
	
	private final Socket socket;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;
	
	public Tests(Socket socket) throws IOException {
		this.socket = socket;
		this.oos = new ObjectOutputStream(socket.getOutputStream());
		this.ois = new ObjectInputStream(socket.getInputStream());
	}
	
	public static void main(String[] args) throws Exception {
		
		int port = 12345;
		InetAddress address = InetAddress.getLocalHost();
		ServerAddress serverAddress = new ServerAddress(address, port);
		DatagramSocket socket = new DatagramSocket(port);
		
		new Thread(() -> {
			try {
				DatagramSocket socketSend = new DatagramSocket();
				UDPHelper.sendUDPObjectReliably("test object reliably", null, serverAddress, socketSend);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
		
		
		Object o = UDPHelper.receiveUDPObjectReliably(socket, serverAddress);
		System.out.println("received : " + o);
		
		
		
		/*ServerMain main = new ServerMain("localhost", "main", 3213, 54312,53212);
		Socket socketUser = new Socket("localhost",3123);
		
		Socket socketTester = new Socket("localhost",3123);
		
		UserThread user = new UserThread(socketUser, null);
		user.start();
		
		Tests tester = new Tests(socketTester);
		
		tester.test(Constants.REGISTER,Constants.REGISTER_ERROR ,new UserInfo("Test", "tessst", "invalid", null));
		tester.test(Constants.REGISTER,Constants.REGISTER_SUCCESS ,new UserInfo("Test", "tessst", "GoodPassword123", null));
		tester.test(Constants.REGISTER,Constants.REGISTER_ERROR ,new UserInfo("Test", "tessst", "invalid", null));*/
		
	}
	
	private void test(String protocol, String correctAnswer, Object object) throws IOException, ClassNotFoundException {
		oos.writeObject(new Command(protocol, object));
		Command command = (Command) ois.readObject();
		if (command.getProtocol().equals(correctAnswer)) {
			System.out.println("Worked");
		} else {
			System.out.println("Invalid");
		}
	}
	
}
