package pt.Server;

import pt.Common.*;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerUserThread extends Thread {
	
	private final Socket socket;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;
	
	private boolean isLoggedIn = false;
	private UserInfo userInfo;
	
	private boolean keepReceiving = true;
	private ArrayList<ServerAddress> orderedServerAddresses;
	
	private ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public ServerUserThread(Socket socket, ArrayList<ServerAddress> orderedServerAddresses) throws IOException {
		this.socket = socket;
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		this.orderedServerAddresses = orderedServerAddresses;
	}
	
	@Override
	public void run() {
		try {
			sendCommand(Constants.SERVERS_LIST, orderedServerAddresses);
			orderedServerAddresses = null;
			receiveRequests();
		} catch (IOException e) {
			System.out.println("Exception sending servers list : " + e.getMessage());
			disconnect();
		}
	}
	
	private void receiveRequests() {
		try {
			while (keepReceiving) {
				Command command;
				try {
					command = (Command) ois.readObject();
				} catch (ClassNotFoundException e) {
					System.out.println("Error reading protocol : " + e.getLocalizedMessage());
					continue;
				} catch (IOException e) {
					throw new IOException("Connection lost");
				}
				System.out.println(command);
				handleRequest(command);
			}
		} catch (IOException e) { // Lost connection
			disconnect();
		} catch (NoSuchAlgorithmException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		getApp().removeConnected(this);
	}
	
	private void handleRequest(Command protocol) throws IOException, SQLException, NoSuchAlgorithmException {
		switch (protocol.getProtocol()) {
			case Constants.REGISTER -> {
				handleRegister((UserInfo) protocol.getExtras());
			}
			
			case Constants.LOGIN -> {
				UserInfo userInfo = (UserInfo) protocol.getExtras();
				login(userInfo.getUsername(), userInfo.getPassword());
			}
			
			case Constants.DISCONNECTING -> {
				disconnect();
			}
			
			default -> {
				if (isLoggedIn()) {
					switch (protocol.getProtocol()) {
						case Constants.CHANNEL_GET_ALL -> {
							protocolChannelGetALl();
						}
						
						case Constants.CHANNEL_GET_MESSAGES -> {
							int[] arr = (int[]) protocol.getExtras();
							protocolChannelGetMessages(arr);
						}
						
						case Constants.CHANNEL_ADD -> {
							ChannelInfo info = (ChannelInfo) protocol.getExtras();
							protocolChannelAdd(info);
						}
						
						case Constants.CHANNEL_REMOVE -> {
							int channelId = (int) protocol.getExtras();
							protocolChannelRemove(channelId);
						}
						
						case Constants.CHANNEL_EDIT -> {
							ChannelInfo info = (ChannelInfo) protocol.getExtras();
							//TODO edit channel
							protocolChannelEdit(info);
						}
						
						case Constants.ADD_MESSAGE -> {
							MessageInfo message = (MessageInfo) protocol.getExtras();
							protocolAddMessage(message);
						}
						
						case Constants.CHANNEL_REGISTER -> {
							ChannelInfo channelInfo = (ChannelInfo) protocol.getExtras();
							protocolChannelRegister(channelInfo);
						}
						case Constants.LOGOUT -> {
							logout();
						}
					}
				}
			}
		}
	}
	
	private void protocolChannelEdit(ChannelInfo info) {
		//ServerChannelManager.updateChannel(info.getId(),info.getName(),info.getPassword(),info.ge);
	}
	
	public void protocolChannelRegister(ChannelInfo channelInfo) throws IOException, SQLException, NoSuchAlgorithmException {
		if (ServerChannelManager.isUserPartOf(userInfo.getUserId(), channelInfo.getId()))
			sendCommand(Constants.SUCCESS, "User is already part of channel");
		else {
			if (ServerChannelManager.isChannelPassword(channelInfo.getId(), channelInfo.getPassword())) {
				if (ServerChannelManager.registerUserToChannel(userInfo.getUserId(), channelInfo.getId()))
					sendCommand(Constants.SUCCESS);
				else
					sendCommand(Constants.ERROR, "Should never happen. Pls fix");
			} else sendCommand(Constants.ERROR, "Wrong password");
		}
	}
	
	public void protocolAddMessage(MessageInfo message) throws IOException, SQLException {
		if (message.getRecipientType().equals(MessageInfo.Recipient.CHANNEL)) {
			if (ServerChannelManager.insertMessage(message.getSenderId(), message.getRecipientId(), message.getContent()))
				sendCommand(Constants.SUCCESS);
			else
				sendCommand(Constants.ERROR, "Should not happen");
		} else if (message.getRecipientType().equals(MessageInfo.Recipient.USER)) {
			if (ServerUserManager.insertMessage(message.getSenderId(), message.getRecipientId(), message.getContent()))
				sendCommand(Constants.SUCCESS);
			else
				sendCommand(Constants.ERROR, "Should not happen");
		}
	}
	
	public void protocolChannelRemove(int channelId) throws SQLException, IOException {
		if (ServerChannelManager.isUserChannelOwner(userInfo.getUserId(), channelId)) {
			boolean success = ServerChannelManager.deleteChannel(channelId);
			if (success) sendCommand(Constants.SUCCESS);
			else sendCommand(Constants.FAILURE, "Error Removing channel"); // Shouldn't happen
		} else sendCommand(Constants.FAILURE, "User doesn't have permissions"); // Shouldn't happen
	}
	
	public void protocolChannelAdd(ChannelInfo info) throws IOException, SQLException, NoSuchAlgorithmException {
		boolean success = ServerChannelManager.createChannel(
				userInfo.getUserId(), info.getName(), info.getPassword(), info.getDescription());
		if (success) sendCommand(Constants.SUCCESS);
		else sendCommand(Constants.FAILURE);
	}
	
	public void protocolChannelGetMessages(int[] arr) throws IOException, SQLException {
		if (arr.length != 2) {
			sendCommand(Constants.ERROR, "Client Protocol Error. Get messages should receive a int[] with channel id and message id");
			return;
		}
		int channelId = arr[0];
		int messageId = arr[1];
		if (ServerChannelManager.isUserPartOf(userInfo.getUserId(), channelId)) {
			sendCommand(Constants.NO_PERMISSIONS);
			return;
		}
		ArrayList<MessageInfo> channelMessages;
		if (messageId == -1)
			channelMessages = ServerChannelManager.getChannelMessagesBefore(channelId, ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		else
			channelMessages = ServerChannelManager.getChannelMessagesBefore(channelId, messageId, ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		Utils.printList(channelMessages, "channelMessages");
		sendCommand(Constants.CHANNEL_GET_MESSAGES, channelMessages);
	}
	
	public void protocolChannelGetALl() throws SQLException, IOException {
		ArrayList<ChannelInfo> channels = ServerChannelManager.getChannels(userInfo.getUserId());
		Utils.printList(channels, "Channels");
		sendCommand(Constants.CHANNEL_GET_ALL, channels);
	}
	
	private void logout() throws IOException {
		isLoggedIn = false;
		sendCommand(Constants.LOGOUT);
	}
	
	private void handleRegister(UserInfo userInfo) throws IOException {
		if (isLoggedIn()) {
			System.out.println("Illegal Request\tNot supposed to happen");
			sendCommand(Constants.INVALID_REQUEST);
			return;
		}
		try {
			System.out.println(userInfo);
			if (!Utils.checkUsername(userInfo.getUsername())) {
				sendCommand(Constants.REGISTER_ERROR, "Username doesn't follow rules (Between 3 and 25 characters and have no special characters and )");
				
			} else if (!Utils.checkPasswordFollowsRules(userInfo.getPassword())) {
				sendCommand(Constants.REGISTER_ERROR, "Password doesn't follow rules (needs 8 to 25 characters, a special character, a number and a upper and lower case letter)");
				
			} else if (!Utils.checkNameUser(userInfo.getName())) {
				sendCommand(Constants.REGISTER_ERROR, "Name is invalid (might be too long, 50 characters is the limit)");
				
			} else if (!ServerUserManager.checkUsernameAvailability(userInfo.getUsername())) {
				sendCommand(Constants.REGISTER_ERROR, "Username already in use");
				
			} else {
				/*if (!userInfo.getPhotoPath().isEmpty()) {
					//TODO receive image
					byte[] imageBytes = (byte[]) ois.readObject();
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
					BufferedImage image = ImageIO.read(byteArrayInputStream);
					image = Utils.getCompressedImage(image, 250, 250);
				}*/
				if (ServerUserManager.insertUser(userInfo)) {
					System.out.println("Added new user");
					sendCommand(Constants.REGISTER_SUCCESS);
				} else {
					System.out.println("No new user added\t Not supposed to happen");
					sendCommand(Constants.REGISTER_ERROR, "No new user added");
				}
			}
		} catch (Exception e) {
			System.out.println("Error on User Registration : " + e.getMessage());
			sendCommand(Constants.REGISTER_ERROR);
		}
	}
	
	private void login(String username, String password) throws SQLException, IOException, NoSuchAlgorithmException {
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
		//TODO send user info
		int userId = ServerUserManager.getUserId(username);
		String nameUser = ServerUserManager.getNameUser(userId);
		userInfo = new UserInfo(userId, username, nameUser);
		sendCommand(Constants.LOGIN_SUCCESS, userInfo);
		System.out.println("Login success : " + userInfo);
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
