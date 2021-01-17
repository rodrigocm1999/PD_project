package pt.Common.RemoteService;

import pt.Common.UserInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteService extends Remote {

    String registerNewUser(UserInfo user) throws RemoteException;

    String sendMessageToAllConnected(String username, String password, String content) throws RemoteException;

    void addObserver(Observer observer) throws RemoteException;

    void removeObserver(Observer observer) throws RemoteException;
}
