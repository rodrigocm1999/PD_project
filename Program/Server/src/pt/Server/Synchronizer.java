package pt.Server;

import pt.Common.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;

public class Synchronizer {
	
	private static final String GET_USERS = "SYNCHRONIZER_GET_USERS";
	private static final String GET_CHANNELS = "SYNCHRONIZER_GET_CHANNELS";
	private static final String GET_USER_PHOTO = "SYNCHRONIZER_GET_USER_PHOTO";
	private static final String NO_MORE_USER_PHOTO = "SYNCHRONIZER_NO_MORE_USER_PHOTO";
	private static final String GET_USERS_CHANNELS = "SYNCHRONIZER_GET_USERS_CHANNELS";
	private static final String GET_MESSAGES = "SYNCHRONIZER_GET_MESSAGES";
	private static final int USERS_BLOCK = 40;
	private static final int CHANNELS_BLOCK = 30;
	private static final int USERS_CHANNELS_BLOCK = 100;
	private static final int MESSAGES_BLOCK = 20;
	private static final String NO_MORE_USERS = "SYNCHRONIZER_NO_MORE_USERS";
	private static final String NO_MORE_CHANNELS = "SYNCHRONIZER_NO_MORE_CHANNELS";
	private static final String NO_MORE_USERS_CHANNELS = "SYNCHRONIZER_NO_MORE_USERS_CHANNELS";
	private static final String NO_MORE_MESSAGES = "SYNCHRONIZER_NO_MORE_MESSAGES";
	private static final String GET_MESSAGE_FILE = "SYNCHRONIZER_GET_MESSAGE_FILE";
	private static final String NO_MORE_MESSAGE_FILE = "SYNCHRONIZER_NO_MORE_MESSAGE_FILE";
	
	private static final String FINISHED = "SYNCHRONIZER_FINISHED";
	private static final int FILE_CHUNK_SIZE = 5 * 1024;
	
	private final ServerAddress otherServer;
	private final ServerAddress thisServer;
	private int otherServerPort;
	private final int synchronizerUDPPort;
	private DatagramSocket socket;
	
	public Synchronizer(ServerAddress otherServer, ServerAddress thisServer, int otherServerPort, DatagramSocket datagramSocket, int synchronizerUDPPort) {
		this.otherServer = otherServer;
		this.thisServer = thisServer;
		this.socket = datagramSocket;
		this.otherServerPort = otherServerPort;
		this.synchronizerUDPPort = synchronizerUDPPort;
	}
	
	public void receiveData() throws Exception {
		System.out.println("Receiving Data");
		//Setup connection with server
		socket = new DatagramSocket(synchronizerUDPPort);
		ServerCommand ask = new ServerCommand(ServerConstants.ASK_SYNCHRONIZER, thisServer, socket.getLocalPort());
		UDPHelper.sendUDPObject(ask, socket, otherServer.getAddress(), ServerConstants.MULTICAST_PORT);
		ServerCommand serverCommand = receiveCommand();
		otherServerPort = (int) serverCommand.getExtras();
		
		// send to the other server to get all missing users -----------------------------------------------------------
		int lastUserId = UserManager.getLastUserId();
		sendCommand(GET_USERS, lastUserId);
		
		ArrayList<UserInfo> allUsersWithImages = new ArrayList<>();
		
		while (true) {
			ServerCommand command = receiveCommand();
			if (command.getProtocol().equals(NO_MORE_USERS)) {
				break;
			}
			ArrayList<UserInfo> newUsers = (ArrayList<UserInfo>) command.getExtras();
			
			for (var currentUser : newUsers) {
				String imagePath = "";
				if (currentUser.hasImage()) {
					imagePath = ServerConstants.getPhotoPathFromUsername(currentUser.getUsername());
					allUsersWithImages.add(currentUser);
				}
				if (!UserManager.insertFull(currentUser, imagePath)) {
					System.err.println("User insert");
					socket.close();
					return;
				}
			}
		}
		
		// get all new user photos -------------------------------------------------------------------------------------
		for (var user : allUsersWithImages) {
			sendCommand(GET_USER_PHOTO, user.getUsername());
			File file = new File(ServerConstants.getPhotoPathFromUsername(user.getUsername()));
			Utils.createDirectories(file);
			
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			
			while (true) {
				ServerCommand command = receiveCommand();
				if (command.getProtocol().equals(NO_MORE_USER_PHOTO)) break;
				byte[] receivedFileBytes = (byte[]) command.getExtras();
				fileOutputStream.write(receivedFileBytes);
			}
			fileOutputStream.close();
		}
		
		// send to the other server to get all missing channels --------------------------------------------------------
		int lastChannelId = ChannelManager.getLastChannelId();
		sendCommand(GET_CHANNELS, lastChannelId);
		
		while (true) {
			ServerCommand command = receiveCommand();
			if (command.getProtocol().equals(NO_MORE_CHANNELS)) {
				break;
			}
			ArrayList<ChannelInfo> newChannels = (ArrayList<ChannelInfo>) command.getExtras();
			for (var channel : newChannels) {
				if (!ChannelManager.insertFull(channel)) {
					System.err.println("Channel insert");
					socket.close();
					return;
				}
			}
		}
		
		// synchronize user_channel ------------------------------------------------------------------------------------
		int lastConnectionId = ChannelManager.getLastUserChannelId();
		sendCommand(GET_USERS_CHANNELS, lastConnectionId);
		
		while (true) {
			ServerCommand command = receiveCommand();
			if (command.getProtocol().equals(NO_MORE_USERS_CHANNELS)) {
				break;
			}
			ArrayList<Ids> channelUsers = (ArrayList<Ids>) command.getExtras();
			for (var id : channelUsers) {
				if (!ChannelManager.registerUserToChannel(id.getUserId(), id.getChannelId())) {
					System.err.println("registerUserToChannel insert");
					socket.close();
					return;
				}
			}
		}
		
		// send to the other server to get all missing messages --------------------------------------------------------
		int lastMessageId = MessageManager.getLastMessageId();
		sendCommand(GET_MESSAGES, lastMessageId);
		
		ArrayList<MessageInfo> fileMessages = new ArrayList<>();
		
		while (true) {
			ServerCommand command = receiveCommand();
			if (command.getProtocol().equals(NO_MORE_MESSAGES)) {
				break;
			}
			ArrayList<MessageInfo> newMessages = (ArrayList<MessageInfo>) command.getExtras();
			for (var message : newMessages) {
				if (!MessageManager.insertFull(message)) {
					System.err.println("Message insert");
					socket.close();
					return;
				}
				if (message.getType().equals(MessageInfo.TYPE_FILE)) {
					fileMessages.add(message);
				}
			}
		}
		// channel_message and user_message already get inserted along with the messages due to foreign keys -----------
		for (var fileMessage : fileMessages) {
			sendCommand(GET_MESSAGE_FILE, fileMessage.getContent());
			
			String filePath = ServerConstants.getTransferredFilesPath() + File.separator + fileMessage.getContent();
			
			File file = new File(filePath);
			Utils.createDirectories(file);
			
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			
			while (true) {
				ServerCommand command = receiveCommand();
				if (command.getProtocol().equals(NO_MORE_MESSAGE_FILE)) break;
				byte[] receivedFileBytes = (byte[]) command.getExtras();
				fileOutputStream.write(receivedFileBytes);
			}
			fileOutputStream.close();
		}
		
		sendCommand(FINISHED, null);
		
		socket.close();
	}
	
	public void sendData() throws Exception {
		System.out.println("Sending Data");
		while (true) {
			ServerCommand command = receiveCommand();
			if (!command.getServerAddress().equals(thisServer)) {
				System.out.println("SendData discarded command : " + command);
				continue; // this packet is not from synchronization
			}
			System.out.println("Synchronizer received request : " + command);
			
			switch (command.getProtocol()) {
				
				case GET_USERS -> {
					int lastUserId = (int) command.getExtras();
					
					ArrayList<UserInfo> usersAfterId = UserManager.getAfterId(lastUserId);
					sendByBlocks(usersAfterId, USERS_BLOCK);
					
					sendCommand(NO_MORE_USERS, null);
				}
				
				case GET_CHANNELS -> {
					int lastChannelId = (int) command.getExtras();
					
					ArrayList<ChannelInfo> channelsAfterID = ChannelManager.getAfterId(lastChannelId);
					sendByBlocks(channelsAfterID, CHANNELS_BLOCK);
					
					sendCommand(NO_MORE_CHANNELS, null);
				}
				
				case GET_USERS_CHANNELS -> {
					int lastConnectionId = (int) command.getExtras();
					
					ArrayList<Ids> channelUsers = ChannelManager.getChannelUsersAfterIds(lastConnectionId);
					sendByBlocks(channelUsers, USERS_CHANNELS_BLOCK);
					
					sendCommand(NO_MORE_USERS_CHANNELS, null);
				}
				
				case GET_MESSAGES -> {
					int lastMessageId = (int) command.getExtras();
					
					ArrayList<MessageInfo> messagesAfterId = MessageManager.getAfterId(lastMessageId);
					sendByBlocks(messagesAfterId, MESSAGES_BLOCK);
					
					sendCommand(NO_MORE_MESSAGES, null);
				}
				
				case GET_MESSAGE_FILE -> {
					String fileName = (String) command.getExtras();
					
					FileInputStream fileInputStream = new FileInputStream(
							ServerConstants.getTransferredFilesPath() + File.separator + fileName);
					sendFileBlocks(fileInputStream);
					
					sendCommand(NO_MORE_MESSAGE_FILE, null);
					fileInputStream.close();
				}
				case GET_USER_PHOTO -> {
					String username = (String) command.getExtras();
					
					try (FileInputStream fileInputStream = new FileInputStream(ServerConstants.getPhotoPathFromUsername(username))) {
						sendFileBlocks(fileInputStream);
						
						sendCommand(NO_MORE_USER_PHOTO, null);
						fileInputStream.close();
					}
				}
				case FINISHED -> {
					socket.close();
					return;
				}
			}
		}
	}
	
	private void sendCommand(String protocol, Object object) throws IOException {
		sendServerCommand(protocol, object);
	}
	
	private ServerCommand receiveCommand() throws Exception {
		return receiveServerCommandObject();
	}
	
	private void sendFileBlocks(FileInputStream fileInputStream) throws IOException {
		byte[] buffer = new byte[FILE_CHUNK_SIZE];
		while (true) {
			int readAmount = fileInputStream.read(buffer);
			if (readAmount <= 0) {
				break;
			}
			byte[] bufferTrimmed = Arrays.copyOfRange(buffer, 0, readAmount);
			sendCommand("", bufferTrimmed);
		}
	}
	
	private void sendByBlocks(ArrayList list, int blockSize) throws Exception {
		if (list.size() == 0) {
			return;
		}
		for (int usersCount = 0; usersCount < list.size(); usersCount++) {
			ArrayList block = new ArrayList();
			
			for (int blockCounter = 0; (blockCounter < blockSize) && (usersCount < list.size()); blockCounter++, usersCount++) {
				block.add(list.get(usersCount));
			}
			sendCommand("", block);
		}
	}
	
	public void sendServerCommand(String protocol, Object object) throws IOException {
		ServerCommand command = new ServerCommand(protocol, otherServer, object);
		
		byte[] bytes = UDPHelper.writeObjectToArray(command);
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, otherServer.getAddress(), otherServerPort);
		socket.send(packet);
	}
	
	public ServerCommand receiveServerCommandObject() throws IOException, ClassNotFoundException {
		while (true) {
			DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			socket.receive(packet);
			
			//Get Object from packet
			ServerCommand serverCommand = (ServerCommand) UDPHelper.readObjectFromPacket(packet);
			
			return serverCommand;
		}
	}
}