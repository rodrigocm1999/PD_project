package pt.Server;

import pt.Common.ServerAddress;

import java.sql.SQLException;

public class Synchronizer {
	
	private final ServerAddress server;
	
	public Synchronizer(ServerAddress server) {
		this.server = server;
	}
	
	void start() throws SQLException {
		//Setup connection with server
		
		
		int lastUserId = UserManager.getLastUserId();
		// send to the other server to get all missing users
		
		int lastChannelId = ChannelManager.getLastChannelId();
		// send to the other server to get all missing channels
		
		// synchronize user_channel
		
		int lastMessageId = MessageManager.getLastMessageId();
		// send to the other server to get all missing messages
		
		// synchronize channel_message and user_message
		
	}
}