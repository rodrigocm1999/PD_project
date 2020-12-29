package pt.Server.RMI;

import pt.Common.MessageInfo;
import pt.Common.RemoteService.Observer;
import pt.Common.RemoteService.RemoteService;
import pt.Common.UserInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class RemoteServiceRMI extends UnicastRemoteObject implements RemoteService {
	
	private final List<Observer> observerList = new ArrayList<>();
	
	protected RemoteServiceRMI() throws RemoteException {
	}
	
	@Override
	public void registerNewUser(UserInfo user) throws RemoteException {
		//TODO register
	}
	
	@Override
	public void sendMessageToAllConnected(UserInfo user, MessageInfo message) throws RemoteException {
		//TODO send to ALL on this server
		//observerList.forEach(item -> item.newMessage(message));
	}
	
	@Override
	public void addObserver(Observer observer) throws RemoteException {
		observerList.add(observer);
	}
	
	@Override
	public void removeObserver(Observer observer) throws RemoteException {
		observerList.remove(observer);
	}
}
