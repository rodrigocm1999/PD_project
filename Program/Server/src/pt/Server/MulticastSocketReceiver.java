package pt.Server;

import pt.Common.Constants;
import pt.Common.UDPHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MulticastSocketReceiver extends Thread {
	
	private class Waiter {
		public DatagramPacket packet;
	}
	
	private final MulticastSocket multicastSocket;
	private List<Waiter> waiters;
	
	public MulticastSocketReceiver(MulticastSocket multicastSocket) {
		waiters = Collections.synchronizedList(new ArrayList<>());
		this.multicastSocket = multicastSocket;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
				multicastSocket.receive(packet);
				System.out.println("Command Received : " + UDPHelper.readObjectFromPacket(packet));
				
				setAll(packet);
			} catch (IOException e) {
				setAll(null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			synchronized (waiters) {
				for (var waiter : waiters) {
					synchronized (waiter) {
						waiter.notifyAll();
					}
				}
			}
		}
	}
	
	private void setAll(DatagramPacket obj) {
		synchronized (waiters) {
			for (var waiter : waiters) {
				waiter.packet = obj;
			}
		}
	}
	
	public DatagramPacket waitForPacket() throws InterruptedException {
		Waiter waiter = new Waiter();
		synchronized (waiter) {
			waiters.add(waiter);
			waiter.wait();
			waiters.remove(waiter);
		}
		return waiter.packet;
	}
}
