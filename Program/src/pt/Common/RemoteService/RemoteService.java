package pt.Common.RemoteService;

import pt.Common.MessageInfo;
import pt.Common.UserInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteService extends Remote {
	
	void registerNewUser(UserInfo user) throws RemoteException;
	void sendMessageToAllConnected(UserInfo user, MessageInfo message) throws RemoteException;
	
	void addObserver(Observer observer) throws RemoteException;
	void removeObserver(Observer observer) throws RemoteException;
}
