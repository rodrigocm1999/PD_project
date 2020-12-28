package pt.Common.RemoteService;

import pt.Common.MessageInfo;
import pt.Common.UserInfo;

import java.rmi.RemoteException;

public interface Observer {

	void userAuthenticated(UserInfo user) throws RemoteException; // TODO maybe just print to the console, IMPLEMENT, needs to be called from some fucked up place
	void newMessage(MessageInfo message) throws RemoteException; // TODO same, IMPLEMENT
}
