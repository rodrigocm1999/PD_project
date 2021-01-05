package pt.Server.RMI;

import pt.Common.MessageInfo;
import pt.Common.RemoteService.Observer;
import pt.Common.RemoteService.RemoteService;
import pt.Common.UserInfo;
import pt.Common.Utils;
import pt.Server.Database.UserManager;
import pt.Server.ServerMain;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RemoteServiceRMI extends UnicastRemoteObject implements RemoteService {
	
	private final List<Observer> observerList = new ArrayList<>();
	private final ServerMain serverMain;
	
	public RemoteServiceRMI(int objectPort, ServerMain serverMain) throws RemoteException {
		super(objectPort);
		this.serverMain = serverMain;
	}
	
	@Override
	public String registerNewUser(UserInfo user) throws RemoteException {
		try {
			if (!Utils.checkUsername(user.getUsername())) {
				return "Username does not follow rules (length: 3-25 )";
			}
			if (!Utils.checkUserPasswordFollowsRules(user.getPassword())) {
				return "Password doesn't follow rules (needs 8 to 25 characters, a special character, a number and a upper and lower case letter)";
			}
			if (UserManager.insertUser(user, null)) {
				serverMain.propagateNewUser(user);
				return "User registered with success";
			} else {
				return "This username is already in use";
			}
		} catch (SQLException | NoSuchAlgorithmException exception) {
			exception.printStackTrace();
		}
		return "something went wrong";
	}
	
	@Override
	public void sendMessageToAllConnected(UserInfo user, MessageInfo message) throws RemoteException {
		//TODO send to ALL on this server
		//Enviar  uma  mensagem  para  todos  os  utilizadores  que  estão  ligados  ao mesmo servidor.
		//for each connectedMachine -> change the destination
		//MessageManager.insertMessage();
		//serverMain.propagateNewMessage();
	}
	
	@Override
	public void addObserver(Observer observer) throws RemoteException {
		System.out.println("added observer");
		observerList.add(observer);
	}
	
	@Override
	public void removeObserver(Observer observer) throws RemoteException {
		System.out.println("removed observer");
		observerList.remove(observer);
	}
	
	public List<Observer> getObserverList() {
		return observerList;
	}
}
