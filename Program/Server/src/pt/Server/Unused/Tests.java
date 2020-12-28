package pt.Server.Unused;

import pt.Common.Command;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class Tests {
	
	private final Socket socket;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;
	
	public Tests(Socket socket) throws IOException {
		this.socket = socket;
		this.oos = new ObjectOutputStream(socket.getOutputStream());
		this.ois = new ObjectInputStream(socket.getInputStream());
	}
	
	private static void printIt(String name, long adding, long removing) {
		System.out.println(name + " took " + adding + "ms adding and " + removing + "ms removing");
	}
	
	public static void main(String[] args) {
		//System.out.println(Utils.hashStringBase36("rodrigo123"));
		final int NUMBER = 1000000;
		
		Random random = new Random();
		if (true) {
			NavigableSet<Integer> sortedSet = new TreeSet<>();
			long start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER; i++) {
				sortedSet.add(random.nextInt());
			}
			long durationAdd = System.currentTimeMillis() - start;
			start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER && sortedSet.size() > 0; i++) {
				sortedSet.remove(sortedSet.first());
			}
			long durationDel = System.currentTimeMillis() - start;
			printIt("TreeSet", durationAdd, durationDel);
		}
		if (true) {
			NavigableMap<Integer, Integer> sortedSet = new TreeMap<>();
			long start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER; i++) {
				sortedSet.put(random.nextInt(), random.nextInt());
			}
			long durationAdd = System.currentTimeMillis() - start;
			start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER && sortedSet.size() > 0; i++) {
				sortedSet.remove(sortedSet.firstKey());
			}
			long durationDel = System.currentTimeMillis() - start;
			printIt("TreeMap", durationAdd, durationDel);
		}
		if (false) {
			List<Integer> list = new ArrayList<>();
			long start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER; i++) {
				list.add(random.nextInt());
			}
			Collections.sort(list);
			long durationAdd = System.currentTimeMillis() - start;
			start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER - 1; i++) {
				list.remove(0);
			}
			long durationDel = System.currentTimeMillis() - start;
			printIt("ArrayList", durationAdd, durationDel);
		}
		if (false) {
			List<Integer> list = new LinkedList<>();
			long start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER; i++) {
				list.add(random.nextInt());
			}
			Collections.sort(list);
			long durationAdd = System.currentTimeMillis() - start;
			start = System.currentTimeMillis();
			for (int i = 0; i < NUMBER - 1; i++) {
				list.remove(0);
			}
			long durationDel = System.currentTimeMillis() - start;
			printIt("LinkedList", durationAdd, durationDel);
		}
		System.out.println("Finished");
		/*
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
		*/
		
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
