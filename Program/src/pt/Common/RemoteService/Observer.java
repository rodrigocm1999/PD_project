package pt.Common.RemoteService;

import pt.Common.MessageInfo;
import pt.Common.UserInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Observer extends Remote {

	void userAuthenticated(UserInfo user) throws RemoteException;
	void newMessage(MessageInfo message) throws RemoteException;
}
