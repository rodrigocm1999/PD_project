package pt.RemoteService;

import pt.Common.MessageInfo;
import pt.Common.RemoteService.Observer;
import pt.Common.UserInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIObserver extends UnicastRemoteObject implements Observer {
	
	private Main.ServerService serverService;
	
	public RMIObserver(Main.ServerService serverService) throws RemoteException {
		this.serverService = serverService;
	}
	
	@Override
	public void userAuthenticated(UserInfo user) throws RemoteException {
		System.out.println("Server: " + serverService.name + ", user authenticated: " + user);
	}
	
	@Override
	public void newMessage(MessageInfo message) throws RemoteException {
		System.out.println("Server: " + serverService.name + ", new message: " + message);
	}
}
