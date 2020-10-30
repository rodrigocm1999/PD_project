package pt.Server;

import pt.Common.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ServerUser extends Thread {
	
	private final Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	
	private boolean isLoggedIn = false;
	private String username;
	private int userId;
	
	private boolean toRemove = false;
	private boolean keepReceiving = true;
	
	private static ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public ServerUser(Socket socket) throws IOException {
		this.socket = socket;
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
	}
	
	@Override
	public void run() {
		try {
			receiveRequests(socket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void receiveRequests(Socket socket) throws Exception {
		try {
			while (keepReceiving) {
				Command command;
				try {
					command = (Command) ois.readObject();
				} catch (ClassNotFoundException e) {
					System.out.println("Error reading protocol : " + e.getLocalizedMessage());
					continue;
				} catch (IOException e) {
					throw new Exception("Connection lost");
				}
				
				handleRequest(command);
			}
		} catch (Exception e) {
			e.printStackTrace();
			toRemove = true;
			ServerUser temp = this;
			//TODO test this shit, finish wait Connection. toRemove never becomes false again
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(Constants.CONNECTION_TIMEOUT);
				} catch (InterruptedException interruptedException) {
					System.out.println("Timeout thread couldn't sleep");
					interruptedException.printStackTrace();
				}
				if (toRemove)
					temp.disconnectNRemove();
			});
			thread.start();
			waitConnection();
			throw new Exception("Lost Connection \tAttempting to reconnect");
		}
	}
	
	public void disconnectNRemove() {
		keepReceiving = false;
		ServerMain.getInstance().removeConnected(this);
	}
	
	private void waitConnection() {
		//TODO do this stuffs
		int localPort = socket.getLocalPort();
		//socket = new Socket(localPort);
	}
	
	private void handleRequest(Command protocol) throws Exception {
		switch (protocol.getProtocol()) {
			case Constants.REGISTER -> {
				handleRegister((UserInfo) protocol.getExtras());
			}
			
			case Constants.LOGIN -> {
				UserInfo userInfo = (UserInfo) protocol.getExtras();
				login(userInfo.getUsername(), userInfo.getPassword());
			}
			
			case Constants.DISCONNECTING -> {
				//TODO clear something I don't know yet
				disconnectNRemove();
			}
		}
		if (isLoggedIn()) {
			switch (protocol.getProtocol()) {
				case Constants.CHANNEL_GET_ALL -> {
					ArrayList<ChannelInfo> channels = ServerChannelManager.getChannels(userId);
					sendCommand(Constants.CHANNEL_GET_ALL, channels);
				}
				
				case Constants.CHANNEL_GET_MESSAGES -> {
					//TODO Get Messages
					
				}
				
				case Constants.CHANNEL_ADD -> {
					ChannelInfo info = (ChannelInfo) protocol.getExtras();
					boolean success = ServerChannelManager.createChannel(
							userId, info.name, info.password, info.description);
					if (success) sendCommand(Constants.SUCCESS);
					else sendCommand(Constants.FAILURE);
				}
				
				case Constants.CHANNEL_REMOVE -> {
					int channelId = (int) protocol.getExtras();
					if (ServerChannelManager.isUserChannelOwner(userId, channelId)) {
						boolean success = ServerChannelManager.deleteChannel(channelId);
						if (success) sendCommand(Constants.SUCCESS);
						else sendCommand(Constants.FAILURE, "Error Removing channel"); // Shouldn't happen
					} else sendCommand(Constants.FAILURE, "User doesn't have permissions"); // Shouldn't happen
				}
				
				case Constants.CHANNEL_EDIT -> {
					ChannelInfo info = (ChannelInfo) protocol.getExtras();
					//TODO edit channel
				}
				
				case Constants.CHANNEL_ADD_MESSAGE -> {
				
				}
				//TODO edit channel, remove channel,  add message, retrieve messages
				
				case Constants.LOGOUT -> {
					logout();
				}
			}
		}
	}
	
	private void logout() {
		isLoggedIn = false;
		// TODO maybe clear userId and username
	}
	
	private void handleRegister(UserInfo userInfo) throws IOException {
		if (isLoggedIn()) {
			System.out.println("Illegal Request\tNot supposed to happen");
			sendCommand(Constants.INVALID_REQUEST);
			return;
		}
		try {
			System.out.println(userInfo);
			
			if (!Utils.checkPasswordFollowsRules(userInfo.getPassword())) {
				System.out.println("Password doesn't follow rules");
				sendCommand(Constants.REGISTER_ERROR, "Invalid Password");
				
			} else if (!Utils.checkNameUser(userInfo.getName())) {
				System.out.println("Name is invalid");
				sendCommand(Constants.REGISTER_ERROR, "Name is invalid (might be too long, 50 characters is the limit)");
				
			} else if (!ServerUserManager.checkUsernameAvailability(userInfo.getUsername())) {
				System.out.println("Username is already in use");
				sendCommand(Constants.REGISTER_ERROR, "Username already in use");
				
			} else {
				/*if (!userInfo.getPhotoPath().isEmpty()) {
					//TODO receive image
					byte[] imageBytes = (byte[]) ois.readObject();
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
					BufferedImage image = ImageIO.read(byteArrayInputStream);
					image = Utils.getCompressedImage(image, 250, 250);
				}*/
				
				if (ServerUserManager.insertUser(userInfo) == 1) {
					System.out.println("Added new user");
					sendCommand(Constants.REGISTER_SUCCESS);
				} else {
					System.out.println("No new user added");
					sendCommand(Constants.REGISTER_ERROR, "No new user added");
				}
			}
		} catch (Exception e) {
			System.out.println("Error on User Registration : " + e.getMessage());
			sendCommand(Constants.REGISTER_ERROR);
		}
	}
	
	private void login(String username, String password) throws Exception {
		if (isLoggedIn()) {
			sendCommand(Constants.LOGIN_ERROR, "Already Logged In");
			return;
		}
		if (!ServerUserManager.doesUsernameExist(username)) {
			sendCommand(Constants.LOGIN_ERROR, "Username does not exist");
			return;
		}
		if (!ServerUserManager.doesPasswordMatchUsername(username, password)) {
			sendCommand(Constants.LOGIN_ERROR, "Password is incorrect");
			return;
		}
		sendCommand(Constants.LOGIN_SUCCESS);
		System.out.println("Login success : " + username);
		userId = ServerUserManager.getUserId(username);
		this.username = username;
		isLoggedIn = true;
	}
	
	public String getSocketInformation() {
		return ("local port: " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
	}
	
	public void sendCommand(String command) throws IOException {
		sendCommand(command, null);
	}
	
	public void sendCommand(String command, Object extra) throws IOException {
		Command obj = new Command(command, extra);
		System.out.println(obj);
		oos.writeUnshared(obj);
	}
	
	public boolean isLoggedIn() {
		return isLoggedIn;
	}
}
