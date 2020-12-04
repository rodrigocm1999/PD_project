package pt.Server;

import pt.Common.*;

import java.io.*;
import java.net.*;
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
	
	//public static final int RETRY_LIMIT = 3;
	//public static final int RECEIVE_TIMEOUT = 1500;
	
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
		/*UDPHelper.Wrapper wrapperToSend = new UDPHelper.Wrapper(object);
		long sentAckId = wrapperToSend.id;
		ServerCommand command = new ServerCommand(protocol, otherServer, wrapperToSend);*/
		ServerCommand command = new ServerCommand(protocol, otherServer, object);
		byte[] bytes = UDPHelper.writeObjectToArray(command);
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, otherServer.getAddress(), otherServerPort);
		System.out.println(command);
		
		socket.send(packet);
		/*int timeout = socket.getSoTimeout();
		socket.setSoTimeout(RECEIVE_TIMEOUT);
		int tryNumber = 0;
		
		while (true) {
			try {
				if (++tryNumber > RETRY_LIMIT) {
					//Time to give up
					System.err.println("ACK didn't arrive, shouldn't happen");
					break;
				}
				
				socket.send(packet);
				
				//Receive ACK. If timeout, then try again a couple more times
				if (getAck(socket, sentAckId)) {
					break;
				}
			} catch (IOException e) {
				System.out.println("Packet ACK timeout, waiting again : try -> " + tryNumber);
			}
		}
		
		socket.setSoTimeout(timeout);*/
	}
	
	private ServerCommand receiveCommand() throws Exception {
		DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
		socket.receive(packet);
		//Get Object from packet
		return (ServerCommand) UDPHelper.readObjectFromPacket(packet);
		/*
		while (true) {
			DatagramPacket packet = new DatagramPacket(new byte[Constants.UDP_PACKET_SIZE], Constants.UDP_PACKET_SIZE);
			socket.receive(packet);
			
			//Get Object from packet
			ServerCommand serverCommand = (ServerCommand) UDPHelper.readObjectFromPacket(packet);
			System.out.println(serverCommand);
			if (!(serverCommand.getExtras() instanceof UDPHelper.Wrapper)) {
				System.err.println("is not Wrapper, shouldn't happen");
				continue;
			}
			UDPHelper.Wrapper wrapper = (UDPHelper.Wrapper) serverCommand.getExtras();
			
			//Send ACK
			byte[] bytes = UDPHelper.writeServerCommandToArray(ServerConstants.ACKNOWLEDGE, thisServer, wrapper.id);
			
			DatagramPacket ackPacket = new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort());
			
			socket.send(ackPacket); // Send ACK
			
			serverCommand.setExtras(wrapper.object);
			return serverCommand;
		}*/
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
	
	private static boolean getAck(DatagramSocket socket, long sentId) {
		byte[] buffer = new byte[Constants.UDP_PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		while (true) {
			try {
				packet.setLength(buffer.length);
				
				socket.receive(packet);
				System.out.print("got something and is");
				ServerCommand ackCommand = (ServerCommand) UDPHelper.readObjectFromPacket(packet);
				System.out.println("\t, received : " + ackCommand);
				
				if (ackCommand.getProtocol().equals(ServerConstants.ACKNOWLEDGE)) {
					long ackId = (long) ackCommand.getExtras();
					if (ackId == sentId) {
						System.out.println("received right ACK : " + ackCommand);
						return true;
					}
				}
			} catch (SocketTimeoutException e) {
				return false;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				//continue;
			}
		}
	}
	
	/*public static class Wrapper implements Serializable {
		private static final long serialVersionUID = 333L;
		
		public final long id;
		public final Object object;
		static private long idCounter = 0;
		
		public Wrapper(Object object) {
			id = ++idCounter;
			this.object = object;
		}
		
		@Override
		public String toString() {
			return "Wrapper{id=" + id + ", object=" + object + '}';
		}
	}*/
}