package pt;

import java.io.*;
import java.net.Socket;

public class ServerUser extends Thread {

	private Socket socket;
	private String username;
	
	public ServerUser(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			
			
			
			
		}catch (Exception e){
			System.out.println(e.getLocalizedMessage());
			ServerMain.getInstance().removeConnected(this);
		}
	}
	
	public Socket getSocket() {
		return socket;
	}
}
