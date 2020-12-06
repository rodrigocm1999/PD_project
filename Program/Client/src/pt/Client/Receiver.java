package pt.Client;

import pt.Common.Command;
import pt.Common.Constants;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Receiver extends Thread {
	
	private ObjectInputStream oIS;
	private List<Waiter> waiters;
	
	
	public Receiver(ObjectInputStream oIS) {
		waiters = Collections.synchronizedList(new ArrayList<>());
		this.oIS = oIS;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				
				Command command = (Command) oIS.readObject();
				System.out.println(command);
				
				setAll(command);
			} catch (IOException e) {
				setAll(new Command(Constants.LOST_CONNECTION));
			} catch (ClassNotFoundException e) {
				setAll(null);
			}
			for (var waiter : waiters) {
				synchronized (waiter) {
					waiter.notifyAll();
				}
			}
		}
	}
	
	private void setAll(Object obj) {
		for (var waiter : waiters) {
			waiter.setResult(obj);
		}
	}
	
	public Object waitForCommand() throws InterruptedException {
		Waiter waiter = new Waiter();
		synchronized (waiter) {
			waiters.add(waiter);
			waiter.wait();
			waiters.remove(waiter);
		}
		return waiter.getResult();
	}
	
}
