package pt.Server;

import pt.Common.Command;
import pt.Common.UDPHelper;
import pt.Common.UserInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MulticastSocket;
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
		
		
		MulticastSocket multicastSocket = new MulticastSocket(5432);
		
		MulticastSocketReceiver receiver = new MulticastSocketReceiver(multicastSocket);
		receiver.start();
		
		Runnable runnable = () -> {
			try {
				System.out.println("thread waiting ");
				Object object = receiver.waitForPacket();
				System.out.println("thread received : " + object);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		};
		
		new Thread(runnable).start();
		new Thread(runnable).start();
		new Thread(runnable).start();
		
		
		UDPHelper.sendUDPObject(new UserInfo("dsadfs","fnewi"),multicastSocket, InetAddress.getLocalHost(),5432);
		
		
		/*int initialSize = 10;
		int endSize = 5;
		int[] arr = new int[initialSize];
		for (int i = 0; i < initialSize; i++) {
			arr[i] = i * 2;
			System.out.println("i : " + i + " : " + arr[i]);
		}
		int[] newArr = Arrays.copyOfRange(arr, 0, endSize);
		for (int i = 0; i < endSize; i++) {
			System.out.println("i : " + i + " : " + newArr[i]);
		}*/
		
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
