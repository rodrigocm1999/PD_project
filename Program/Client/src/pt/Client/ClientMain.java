package pt.Client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import pt.Common.*;
import pt.Common.MessageInfo.Recipient;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClientMain {
	
	private InetAddress serverIPAddress;
	private int portUDPServer;
	private Socket socket;
	private static ObjectOutputStream oOS;
	private static ObjectInputStream oIS;
	private ArrayList<ServerAddress> serversList;
	private static ClientMain instance;
	private ArrayList<ChannelInfo> channels;
	private ArrayList<UserInfo> users;
	private Receiver receiver;
	private UserInfo userInfo;
	private File userPhoto;
	private ApplicationController applicationController;
	
	private ArrayList<MessageInfo> messages = null;
	private MessageInfo messageTemplate = null;
	
	//TODO FAZER O FAILOVER OVER NETWORK
	
	//TODO TESTAR LABELS EM TODO O LADO
	
	public static ClientMain getInstance() {
		return instance;
	}
	
	public ClientMain(String ipServer, int port) throws Exception {
		if (instance != null) {
			throw new Exception("Client Main Exists");
		}
		instance = this;
		this.serverIPAddress = InetAddress.getByName(ipServer);
		this.portUDPServer = port;
	}
	
	public void connectToServer() throws Exception {
		if (socket != null) {
			if (socket.isConnected()) socket.close();
		}
		DatagramSocket datagramSocket = new DatagramSocket();
		
		while (true) {
			System.out.println(serverIPAddress + ":" + portUDPServer);
			try {
				boolean success = tryConnectServer(serverIPAddress, portUDPServer, datagramSocket);
				
				if (success) {
					receiver = new Receiver(oIS);
					receiver.start();
					return;
				} else {
					ServerAddress serverAddress = serversList.remove(0);
					serverIPAddress = serverAddress.getAddress();
					portUDPServer = serverAddress.getUDPPort();
				}
			} catch (Exception e) {
				try {
					if (serversList == null){
						System.out.println("The servers are down, try again later");
						System.exit(0);
					}
					ServerAddress serverAddress = serversList.remove(0);
					serverIPAddress = serverAddress.getAddress();
					portUDPServer = serverAddress.getUDPPort();
				} catch (IndexOutOfBoundsException ignored) {
				} catch (NullPointerException ignored) {
					System.out.println("Server not accessible");
					System.exit(-1);
				}
			}
		}
	}
	
	private boolean tryConnectServer(InetAddress ipAddress, int port, DatagramSocket udpSocket) throws Exception {
		Command command = new Command(Constants.ESTABLISH_CONNECTION);
		UDPHelper.sendUDPObject(command, udpSocket, ipAddress, port);
		
		udpSocket.setSoTimeout(2000);
		command = (Command) UDPHelper.receiveUDPObject(udpSocket);
		String protocol = command.getProtocol();
		
		if (protocol.equals(Constants.CONNECTION_ACCEPTED)) {
			int socketTCPort = (int) command.getExtras();
			socket = new Socket(ipAddress, socketTCPort);
			oOS = new ObjectOutputStream(socket.getOutputStream());
			oIS = new ObjectInputStream(socket.getInputStream());
			
			
			command = (Command) oIS.readObject();
			if (!command.getProtocol().equals(Constants.SERVERS_LIST)) {
				throw new Exception("Should not happen");
			}
			serversList = (ArrayList<ServerAddress>) command.getExtras();
			
			System.out.println("Connection accepted");
			
			return true;
		} else if (protocol.equals(Constants.CONNECTION_REFUSED)) {
			System.out.println("Connection refused");
			serversList = (ArrayList<ServerAddress>) command.getExtras();
			return false;
		} else {
			throw new IOException("Illegal Connection Protocol");
		}
	}
	
	public Object sendCommandToServer(String protocol, Object object) throws IOException, InterruptedException,SocketException{
		Command command = new Command(protocol, object);
		while (true){
			//WTF IS THIS. IT WORKS THOUGH
			try {
				oOS.writeObject(command);
				break;
			}catch (SocketException e){
				try {
					connectToServer();
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}
		}

		
		Object ob = receiveCommand();
		System.out.print("Sent : " + command + "\n\t");
		return ob;
	}
	
	public Object receiveCommand() throws InterruptedException {
		Command ob = (Command) receiver.waitForCommand();
		if (ob.getProtocol().equals(Constants.LOST_CONNECTION)) {
			throw new InterruptedException("Lost connection to the server");
		}
		System.out.println("receive command :  : " + ob);
		return ob;
	}
	
	public boolean logout() throws IOException, InterruptedException {
		Command command = (Command) sendCommandToServer(Constants.LOGOUT, null);
		return command.getProtocol().equals(Constants.SUCCESS);
	}
	
	public UserInfo getUserInfo() {
		return userInfo;
	}
	
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}
	
	public ChannelInfo getChannelByName(String name) {
		for (var channel : channels) {
			if (name.equals(channel.getName())) {
				return channel;
			}
		}
		return null;
	}
	
	public UserInfo getUserByUsername(String name) {
		System.out.println("getUserByUsername : " + users);
		for (var user : users) {
			if (name.equals(user.getUsername())) {
				return user;
			}
		}
		return null;
	}
	
	public ArrayList<MessageInfo> getMessagesFromChannel(int id) throws IOException, ClassNotFoundException, InterruptedException {
		Command command = (Command) sendCommandToServer(Constants.CHANNEL_GET_MESSAGES, new Ids(-1, id, -1));
		return (ArrayList<MessageInfo>) command.getExtras();
	}
	
	public ArrayList<ChannelInfo> getChannels() {
		return channels;
	}
	
	public void setChannels(ArrayList<ChannelInfo> channels) {
		this.channels = channels;
	}
	
	public File getUserPhoto() {
		return userPhoto;
	}
	
	public void setUserPhoto(File userPhoto) {
		this.userPhoto = userPhoto;
	}
	
	public void sendFile(File file) throws IOException, InterruptedException {
		
		Recipient recipientType = getMessagesRecipientType();
		if (recipientType == null) return;
		int recipientId = getMessagesRecipientId();
		
		MessageInfo message = new MessageInfo(recipientType, recipientId, MessageInfo.TYPE_FILE, file.getName());
		
		
		Command command = (Command) sendCommandToServer(Constants.ADD_FILE, message);
		if (!command.getProtocol().equals(Constants.FILE_ACCEPT_CONNECTION)) {
			System.err.println("Error File not  Success before send");
			return;
		}
		
		Thread thread = new Thread(() -> {
			try {
				int fileTransferPort = (int) command.getExtras();
				Socket socket = new Socket(serverIPAddress, fileTransferPort);
				
				OutputStream outputStream = socket.getOutputStream();
				try (FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath())) {
					
					byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
					while (true) {
						int readAmount = fileInputStream.read(buffer);
						if (readAmount == -1) { /* Reached the end of file */
							outputStream.close();
							socket.close();
							break;
						}
						outputStream.write(buffer, 0, readAmount);
					}
				}
				Command newNameCommand = (Command) receiveCommand();
				String newFileName = (String) newNameCommand.getExtras();
				message.setContent(newFileName);
				Platform.runLater(() -> ApplicationController.get().addMessageToScreen(message));
			} catch (IOException | InterruptedException e) {
			}
		});
		thread.start();
	}
	
	public ArrayList<UserInfo> getUsers() {
		return users;
	}
	
	public void setUsers(ArrayList<UserInfo> users) {
		this.users = users;
	}
	
	public ApplicationController getApplicationController() {
		return applicationController;
	}
	
	public ArrayList<ChannelInfo> getChannelsFromServer() throws IOException, ClassNotFoundException, InterruptedException {
		Command command = (Command) sendCommandToServer(Constants.CHANNEL_GET_ALL, null);
		ArrayList<ChannelInfo> list = (ArrayList<ChannelInfo>) command.getExtras();
		channels = list;
		return channels;
	}
	
	public int getMessagesRecipientId() {
		return messageTemplate == null ? null : messageTemplate.getRecipientId();
	}
	
	public Recipient getMessagesRecipientType() {
		return messageTemplate == null ? null : messageTemplate.getRecipientType();
	}
	
	public ArrayList<MessageInfo> getMessages() {
		return messages;
	}
	
	public void setMessages(ArrayList<MessageInfo> messages) {
		this.messages = messages;
	}
	
	public void defineMessageTemplate(Recipient recipientType, int recipientId) {
		messageTemplate = new MessageInfo(recipientType, recipientId);
	}
	
	public void downloadFile(MessageInfo message, String directory) throws IOException, InterruptedException {
		
		Command command = (Command) sendCommandToServer(Constants.GET_FILE, message.getId());
		if (!command.getProtocol().equals(Constants.FILE_ACCEPT_CONNECTION)) {
			System.err.println("Error File not  Success before send");
			return;
		}
		
		Thread thread = new Thread(() -> {
			int fileDownloadPort = (int) command.getExtras();
			try {
				Socket socket = new Socket(serverIPAddress, fileDownloadPort);
				System.out.println("cheguei aqui");
				
				InputStream fIS = socket.getInputStream();
				FileOutputStream fileOutputStream = new FileOutputStream(directory + File.separator + message.getContent());
				
				byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
				
				while (true) {
					int readAmount = fIS.read(buffer);
					if (readAmount <= 0) {
						fIS.close();
						socket.close();
						break;
					}
					fileOutputStream.write(buffer, 0, readAmount);
				}
				fileOutputStream.close();
				Command newComand = (Command) receiveCommand();
				if (newComand.getProtocol().equals(Constants.FINISHED_FILE_DOWNLOAD)) {
					Platform.runLater(() -> {
						Alert alert = new Alert(Alert.AlertType.INFORMATION);
						alert.setTitle(" INFO ");
						alert.setHeaderText(null);
						alert.setContentText("Download completed");
						alert.showAndWait();
						
					});
					
				}
				
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		thread.start();
	}
	
	public InetAddress getServerIPAddress() {
		return serverIPAddress;
	}
	
	public int getPortUDPServer() {
		return portUDPServer;
	}

	public void setServersList(ArrayList<ServerAddress> serversList) {
		this.serversList = serversList;
	}
}