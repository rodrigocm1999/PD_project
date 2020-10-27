package pt;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class ServerUser extends Thread {
	
	private final Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	
	public ServerUser(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			
			while (true) {
				String protocol = Constants.INVALID_PROTOCOL;
				try {
					protocol = (String) ois.readObject();
				} catch (ClassNotFoundException e) {
					System.out.println("Error reading protocol : " + e.getLocalizedMessage());
					continue;
				}catch (IOException e){
					System.out.println("Connection lost : " + e.getLocalizedMessage());
					throw new Exception("Connection lost");
				}
				
				switch (protocol) {
					case Constants.REGISTER -> {
						try {
							UserInfo userInfo = (UserInfo) ois.readObject();
							System.out.println(userInfo);
							
							if (false && !Utils.checkPasswordFollowsRules(userInfo.getPassword())) {
								System.out.println("password doesn't follow rules");
								sendCommand(Constants.REGISTER_ERROR, "Invalid Password");
								
							} else if (!checkNameUser(userInfo.getName())) {
								System.out.println("Name is invalid");
								sendCommand(Constants.REGISTER_ERROR, "Name is invalid (might be too long, 50 characters is the limit)");
								
							} else if (!checkUsernameAvailability(userInfo.getUsername())) {
								System.out.println("Username is already in user");
								sendCommand(Constants.REGISTER_ERROR, "Username already in use");
								
							} else {
								
								/*if (!userInfo.getPhotoPath().isEmpty()) {
									//TODO receive image
									byte[] imageBytes = (byte[]) ois.readObject();
									ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
									BufferedImage image = ImageIO.read(byteArrayInputStream);
									image = Utils.getCompressedImage(image, 250, 250);
								}*/
								
								if (insertUser(userInfo) == 1) {
									System.out.println("Added new user");
									sendCommand(Constants.REGISTER_SUCCESS, null);
								} else {
									System.out.println("No new user added");
									sendCommand(Constants.REGISTER_ERROR, "No new user added");
								}
							}
						} catch (Exception e) {
							System.out.println("Error on User Registration : " + e.getMessage());
							sendCommand(Constants.REGISTER_ERROR, null);
						}
					}
					
					case Constants.LOGIN -> {
						UserInfo userInfo = (UserInfo) ois.readObject();
						System.out.println(userInfo);
						
						if (login(userInfo.getUsername(), userInfo.getPassword())) {
						
						
						}
						
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			ServerMain.getInstance().removeConnected(this);
		}
		
	}
	
	private boolean login(String username, String password) throws SQLException, IOException, NoSuchAlgorithmException {
		if (!doesUsernameExist(username)) {
			sendCommand(Constants.LOGIN_ERROR, "Username does not exist");
			return false;
		}
		if (!doesPasswordMatchUsername(username, password)) {
			sendCommand(Constants.LOGIN_ERROR, "Password is incorrect");
			return false;
		}
		sendCommand(Constants.LOGIN_SUCCESS, null);
		return true;
	}
	
	private boolean doesPasswordMatchUsername(String username, String password) throws SQLException, NoSuchAlgorithmException {
		String select = "select count(id) from user where username = ? and password_hash = ?";
		PreparedStatement preparedStatement = getApp().getPreparedStatement(select);
		preparedStatement.setString(1, username);
		preparedStatement.setString(2, Utils.hashStringBase36(password));
		ResultSet resultSet = preparedStatement.executeQuery();
		resultSet.next();
		return resultSet.getInt(1) == 1;
	}
	
	private static ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public String getSocketInformation() {
		return ("local port: " + socket.getLocalPort() + " " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
	}
	
	private void sendCommand(String command, String extra) throws IOException {
		oos.writeObject(command + "," + (extra == null ? "" : extra));
	}
	
	private int insertUser(UserInfo user) throws SQLException, NoSuchAlgorithmException {
		// insert the new user into the database ------------------------------------------
		String insert = "insert into user(name,username,password_hash,photo_path) values(?,?,?,?)";
		PreparedStatement preparedStatement = getApp().getPreparedStatement(insert);
		preparedStatement.setString(1, user.getName());
		preparedStatement.setString(2, user.getUsername());
		preparedStatement.setString(3, Utils.hashStringBase36(user.getPassword()));
		preparedStatement.setString(4, user.getPhotoPath());
		return preparedStatement.executeUpdate();
	}
	
	private boolean checkUsernameAvailability(String username) throws SQLException {
		// check if username is already taken ---------------------------------------
		return !doesUsernameExist(username);
	}
	
	private boolean doesUsernameExist(String username) throws SQLException {
		// check if username exists -------------------------------------------
		String select = "select count(id) from user where username = ?";
		PreparedStatement stat = getApp().getPreparedStatement(select);
		stat.setString(1, username);
		ResultSet result = stat.executeQuery();
		result.next();
		return result.getInt(1) == 1;
	}
	
	private static boolean checkNameUser(String name) {
		return name.length() <= 50;
	}
	
}
