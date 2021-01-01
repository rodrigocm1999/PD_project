package pt.RemoteService;

import pt.Common.MessageInfo;
import pt.Common.RemoteService.Observer;
import pt.Common.UserInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIObserver extends UnicastRemoteObject implements Observer {
	public RMIObserver() throws RemoteException {
	}
	
	@Override
	public void userAuthenticated(UserInfo user) throws RemoteException {
		System.out.println("user authenticated");
	}
	
	@Override
	public void newMessage(MessageInfo message) throws RemoteException {
		System.out.println("new message");
	}
}
