package pt;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

public class ServerUser extends Thread {
	
	private final Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private String username;
	private final ServerMain app;
	
	public ServerUser(Socket socket) throws IOException {
		this.socket = socket;
		app = ServerMain.getInstance();
		
		ois = new ObjectInputStream(socket.getInputStream());
		oos = new ObjectOutputStream(socket.getOutputStream());
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				String protocol = (String) ois.readObject();
				
				switch (protocol) {
					case Constants.REGISTER -> {
						
						UserInfo userInfo = (UserInfo) ois.readObject();
						System.out.println(userInfo);
						Connection conn = app.getDatabaseConnection();
						
						// check if username is already used ------------------------------------------------------
						PreparedStatement stat = conn.prepareStatement("select count(username) from user where username = '?' ;");
						stat.setString(1, userInfo.username);
						ResultSet result = stat.executeQuery();
						result.next();
						boolean isUsernameAvailable = result.getInt(1) == 0;
						
						if (isUsernameAvailable) {
							
							//Hash the password ------------------------------------------------------
							MessageDigest digest = MessageDigest.getInstance("SHA-256");
							byte[] hash = digest.digest(userInfo.getPassword().getBytes(StandardCharsets.UTF_8));
							String passwordHash = new String(hash);
							
							// insert the new user into the database ------------------------------------------------------
							String insert = "insert into user(name,username,password_hash,photo_path) values('?','?','?');";
							PreparedStatement preparedStatement = conn.prepareStatement(insert);
							preparedStatement.setString(1, userInfo.name);
							preparedStatement.setString(2, userInfo.username);
							preparedStatement.setString(3, passwordHash);
							preparedStatement.setString(4, userInfo.getPhotoPath());
							boolean success = stat.execute(insert);
							
							if (success) {
								System.out.println("Added new user");
								sendCommand(Constants.REGISTER_SUCCESS,null);
							} else {
								System.out.println("No new user added");
								sendCommand(Constants.ERROR,"No new user added");
							}
							
						} else {
							//Username already in use
							sendCommand("username", "Username already in use");
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Exception : " + e.getLocalizedMessage());
			ServerMain.getInstance().removeConnected(this);
		}
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	private void sendCommand(String command, String extra) throws IOException {
		oos.writeObject(command + (extra == null ? "" : extra));
	}
	
}
