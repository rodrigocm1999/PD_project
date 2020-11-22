package pt.Server;

import com.sun.security.jgss.GSSUtil;
import pt.Common.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class UserThread extends Thread {
	
	private final Socket socket;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;
	
	private boolean isLoggedIn = false;
	private UserInfo userInfo;
	private final Ids currentPlace = new Ids(0, 0, 0);
	
	private boolean keepReceiving = true;
	private ArrayList<ServerAddress> orderedServerAddresses;
	
	private ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public UserThread(Socket socket, ArrayList<ServerAddress> orderedServerAddresses) throws IOException {
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
				System.out.println("Received : " + command);
				handleRequest(command);
			}
		} catch (IOException e) { // Lost connection
			disconnect();
		} catch (NoSuchAlgorithmException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			if (socket.isConnected()) {
				socket.close();
			}
			getApp().removeConnected(this);
		} catch (Exception ignore) {
		}
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
							Ids ids = (Ids) protocol.getExtras();
							protocolChannelGetMessages(ids);
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
							protocolChannelEdit(info);
							//TODO test channel edition
						}
						
						case Constants.ADD_MESSAGE -> {
							MessageInfo message = (MessageInfo) protocol.getExtras();
							if (protocolAddMessage(message))
								getApp().propagateNewMessage(message, this);
						}
						
						case Constants.ADD_FILE -> {
							MessageInfo message = (MessageInfo) protocol.getExtras();
							protocolAddFile(message);
						}
						
						case Constants.CHANNEL_REGISTER -> {
							ChannelInfo channelInfo = (ChannelInfo) protocol.getExtras();
							protocolChannelRegister(channelInfo);
						}
						case Constants.USER_GET_LIKE -> {
							String username = (String) protocol.getExtras();
							protocolUserGetLike(username);
						}
						case Constants.USER_GET_MESSAGES -> {
							Ids ids = (Ids) protocol.getExtras();
							protocolUserGetMessages(ids);
							// TODO Test messages before
						}
						case Constants.GET_FILE -> {
							int messageId = (int) protocol.getExtras();
							protocolGetFile(messageId);
						}
						
						case Constants.USER_GET_PHOTO -> {
							String username = (String) protocol.getExtras();
							protocolGetUserPhoto(username);
						}
						
						case Constants.LOGOUT -> {
							logout();
						}
					}
				}
			}
		}
	}
	
	private void protocolGetUserPhoto(String username) {
		File file = new File(ServerConstants.getPhotoPathFromUsername(username));
		
		if (file.exists() && !file.isDirectory()) {
			
			new Thread(() -> {
				try {
					Socket fileSocket = getApp().acceptFileConnection(this);
					byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
					OutputStream outputStream = fileSocket.getOutputStream();
					
					try (FileInputStream fileStream = new FileInputStream(file)) {
						while (true) {
							int amountRead = fileStream.read(buffer);
							if (amountRead <= 0) {
								socket.close();
								break;
							}
							outputStream.write(buffer, 0, amountRead);
						}
					}
					sendCommand(Constants.FINISHED_PHOTO_DOWNLOAD);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		} else {
			System.out.println("File does not exists or is directory : " + file.getAbsolutePath());
		}
	}
	
	private void protocolUserGetMessages(Ids ids) throws SQLException, IOException {
		ArrayList<MessageInfo> userMessages;
		if (ids.getMessageId() <= 0)
			userMessages = MessageManager.getUserMessages(userInfo.getUserId(), ids.getUserId(), ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		else
			userMessages = MessageManager.getUserMessagesBefore(userInfo.getUserId(), ids.getUserId(), ids.getMessageId(), ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		Utils.printList(userMessages, "UserMessages");
		sendCommand(Constants.SUCCESS, userMessages);
		
		currentPlace.setUserOnly(ids.getUserId());
	}
	
	private void protocolUserGetLike(String username) throws SQLException, IOException {
		if (username == null) username = "";
		ArrayList<UserInfo> usersLike = UserManager.getUsersLike(username,userInfo.getUserId());
		sendCommand(Constants.SUCCESS, usersLike);
	}
	
	public void protocolGetFile(int messageId) throws IOException, SQLException {
		MessageInfo message = MessageManager.getMessageById(messageId);
		
		if (message.getType().equals(MessageInfo.TYPE_FILE)) {
			
			String path = ServerConstants.getTransferredFilesPath() + File.separator + message.getContent();
			File file = new File(path);
			
			new Thread(() -> {
				try {
					Socket fileSocket = getApp().acceptFileConnection(this);
					byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
					OutputStream outputStream = fileSocket.getOutputStream();
					
					try (FileInputStream fileStream = new FileInputStream(file)) {
						while (true) {
							int amountRead = fileStream.read(buffer);
							if (amountRead <= 0) {
								socket.close();
								break;
							}
							outputStream.write(buffer, 0, amountRead);
						}
					}
					sendCommand(Constants.FINISHED_FILE_DOWNLOAD, messageId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		} else {
			sendCommand(Constants.ERROR, "Invalid Message for download");
		}
	}
	
	public void protocolAddFile(MessageInfo message) throws IOException {
		if (message.getContent().isBlank()) {
			sendCommand(Constants.ERROR, "Empty Content");
			return;
		}
		String fileNameWithTime = addTimestampFileName(message.getContent());
		
		String filePath = ServerConstants.getTransferredFilesPath() + File.separator + fileNameWithTime;
		
		File file = new File(filePath);
		Utils.createDirectories(file);
		
		Thread receivingFile = new Thread(() -> {
			try {
				Socket fileSocket = getApp().acceptFileConnection(this);
				InputStream socketInputStream = fileSocket.getInputStream();
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
				try {
					while (true) {
						int readAmount = socketInputStream.read(buffer);
						if (readAmount == -1) { /* Reached the end of file */
							fileOutputStream.close();
							fileSocket.close();
							break;
						}
						fileOutputStream.write(buffer, 0, readAmount);
					}
				} catch (IOException e) {
					fileSocket.close();
					fileOutputStream.close();
					file.delete();
				}
				
				message.setSenderId(userInfo.getUserId());
				message.setContent(fileNameWithTime);
				System.out.println(message);
				if (MessageManager.insertMessage(message))
					sendCommand(Constants.SUCCESS, fileNameWithTime);
				else
					sendCommand(Constants.ERROR, "Should not happen");
				
			} catch (IOException | SQLException e) {
				e.printStackTrace();
			}
		});
		receivingFile.start();
	}
	
	private String addTimestampFileName(String fileName) {
		String utcTimeString = "" + new Date().getTime();
		int timeLength = utcTimeString.length() - 8;
		
		utcTimeString = utcTimeString.substring(Math.max(timeLength, 0));
		
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex == -1)
			return fileName + "_" + utcTimeString;
		else
			return fileName.substring(0, dotIndex) + "_" + utcTimeString + fileName.substring(dotIndex);
	}
	
	public void protocolChannelEdit(ChannelInfo channel) throws IOException, SQLException, NoSuchAlgorithmException {
		if (!Utils.checkPasswordFollowsRules(channel.getPassword())) {
			sendCommand(Constants.ERROR, "Invalid Password");
			return;
		}
		boolean success = ChannelManager.updateChannel(channel);
		if (success) sendCommand(Constants.SUCCESS);
		else sendCommand(Constants.ERROR, "Error Updating, channel name might already be in use");
	}
	
	public void protocolChannelRegister(ChannelInfo channelInfo) throws IOException, SQLException, NoSuchAlgorithmException {
		if (ChannelManager.isUserPartOf(userInfo.getUserId(), channelInfo.getId()))
			sendCommand(Constants.SUCCESS, "User is already part of channel");
		else {
			if (ChannelManager.isChannelPassword(channelInfo.getId(), channelInfo.getPassword())) {
				if (ChannelManager.registerUserToChannel(userInfo.getUserId(), channelInfo.getId()))
					sendCommand(Constants.SUCCESS);
				else
					sendCommand(Constants.ERROR, "Should never happen. Pls fix");
			} else sendCommand(Constants.ERROR, "Wrong password");
		}
	}
	
	public boolean protocolAddMessage(MessageInfo message) throws IOException, SQLException {
		if (message.getContent().isBlank()) {
			sendCommand(Constants.ERROR);
			return false;
		}
		message.setSenderId(userInfo.getUserId());
		
		if (MessageManager.insertMessage(message)) {
			sendCommand(Constants.SUCCESS);
			return true;
		} else {
			sendCommand(Constants.ERROR, "Should not happen");
			return false;
		}
	}
	
	public void protocolChannelRemove(int channelId) throws SQLException, IOException {
		if (ChannelManager.isUserChannelOwner(userInfo.getUserId(), channelId)) {
			boolean success = ChannelManager.deleteChannel(channelId);
			if (success) sendCommand(Constants.SUCCESS);
			else sendCommand(Constants.ERROR, "Error Removing channel"); // Shouldn't happen
		} else sendCommand(Constants.ERROR, "User doesn't have permissions"); // Shouldn't happen
	}
	
	public void protocolChannelAdd(ChannelInfo channel) throws IOException, SQLException, NoSuchAlgorithmException {
		if (channel.getPassword().length() < 3 || channel.getPassword().length() > 50) {
			sendCommand(Constants.ERROR, "Invalid Password (need to be between 3 and 50 characters)");
			return;
		}
		if (!checkNameAvailability(channel.getName())) {
			sendCommand(Constants.ERROR, "Name already in use by another channel or user");
			return;
		}
		channel.setCreatorId(userInfo.getUserId());
		boolean success = ChannelManager.createChannel(channel);
		if (success) {
			sendCommand(Constants.SUCCESS);
			ChannelManager.registerUserToChannel(userInfo.getUserId(), channel.getId());
		} else sendCommand(Constants.ERROR, "Something went wrong (Username might already be in use)");
	}
	
	public void protocolChannelGetMessages(Ids ids) throws IOException, SQLException {
		if (!ChannelManager.isUserPartOf(userInfo.getUserId(), ids.getChannelId())) {
			sendCommand(Constants.NO_PERMISSIONS);
			return;
		}
		ArrayList<MessageInfo> channelMessages;
		if (ids.getMessageId() <= 0)
			channelMessages = MessageManager.getChannelMessages(ids.getChannelId(), ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		else
			channelMessages = MessageManager.getChannelMessagesBefore(ids.getChannelId(), ids.getMessageId(), ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		sendCommand(Constants.CHANNEL_GET_MESSAGES, channelMessages);
		
		currentPlace.setChannelOnly(ids.getChannelId());
	}
	
	public void protocolChannelGetALl() throws SQLException, IOException {
		ArrayList<ChannelInfo> channels = ChannelManager.getChannels(userInfo.getUserId());
		sendCommand(Constants.CHANNEL_GET_ALL, channels);
	}
	
	private void logout() throws IOException {
		isLoggedIn = false;
		sendCommand(Constants.SUCCESS);
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
				sendCommand(Constants.REGISTER_ERROR, "Name is invalid (needs to be between 3 and 50 characters long)");
				
			} else if (!checkNameAvailability(userInfo.getUsername())) {
				sendCommand(Constants.REGISTER_ERROR, "Username already in use by another user or channel");
				
			} else {
				String imageName = "";
				if (userInfo.getImageBytes() != null) {
					imageName = UserManager.saveImage(userInfo);
				}
				if (UserManager.insertUser(userInfo, imageName)) {
					System.out.println("Added new user");
					sendCommand(Constants.REGISTER_SUCCESS);
				} else {
					if (imageName != null)
						new File(imageName).delete();
					System.out.println("No new user added\t Not supposed to happen");
					sendCommand(Constants.REGISTER_ERROR, "No new user added");
				}
			}
		} catch (NoSuchAlgorithmException | SQLException e) {
			System.out.println("Error on User Registration : " + e.getMessage());
			e.printStackTrace();
			sendCommand(Constants.REGISTER_ERROR);
		}
	}
	
	private void login(String username, String password) throws SQLException, IOException, NoSuchAlgorithmException {
		if (isLoggedIn()) {
			sendCommand(Constants.LOGIN_ERROR, "Already Logged In");
			return;
		}
		if (!UserManager.doesUsernameExist(username)) {
			sendCommand(Constants.LOGIN_ERROR, "Username does not exist");
			return;
		}
		if (!UserManager.doesPasswordMatchUsername(username, password)) {
			sendCommand(Constants.LOGIN_ERROR, "Password is incorrect");
			return;
		}
		int userId = UserManager.getUserId(username);
		String nameUser = UserManager.getNameUser(userId);
		userInfo = new UserInfo(userId, username, nameUser);
		sendCommand(Constants.LOGIN_SUCCESS, userInfo);
		System.out.println("Login success : " + userInfo);
		isLoggedIn = true;
	}
	
	public void receivedPropagatedMessage(MessageInfo message) throws IOException {
		System.out.println("Propagated message : " + message);
		if (isLoggedIn) {
			sendCommand(Constants.NEW_MESSAGE, message);
			if (currentPlace.getChannelId() == message.getRecipientId() || currentPlace.getMessageId() == message.getRecipientId()) {
				System.out.println("Send propagated message : " + message);
				sendCommand(Constants.NEW_MESSAGE, message);
			}
		}
	}
	
	// TODO add all propagators
	public synchronized void sendCommand(String command) throws IOException {
		sendCommand(command, null);
	}
	
	public synchronized void sendCommand(String command, Object extra) throws IOException {
		Command obj = new Command(command, extra);
		System.out.println("Sent : " + obj);
		oos.writeUnshared(obj);
	}
	
	public String getSocketInformation() {
		return ("local port: " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
	}
	
	public boolean isLoggedIn() {
		return isLoggedIn;
	}
	
	private boolean checkNameAvailability(String name) throws SQLException {
		return (UserManager.checkUsernameAvailability(name) && ChannelManager.checkNameAvailability(name));
	}
}
