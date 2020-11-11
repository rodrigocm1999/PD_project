package pt.Server;

import pt.Common.MessageInfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MessageManager {
	
	private static Object messageLock = new Object();
	
	private static ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	
	public static ArrayList<String> getLikeFiles(String fileName) throws SQLException {
		String select = "select content from message where type = 'file' and content like ?";
		PreparedStatement preparedStatement = ServerMain.getInstance().getPreparedStatement(select);
		preparedStatement.setString(1, fileName + '%');
		ResultSet resultSet = preparedStatement.executeQuery();
		
		ArrayList<String> list = new ArrayList<>();
		while (resultSet.next()) {
			list.add(resultSet.getString(1));
		}
		return list;
	}
	
	
	public static ArrayList<MessageInfo> getChannelMessages(int channelId, int amount) throws SQLException {
		int lastMessageId = getLastMessageId() + 1;
		return getChannelMessagesBefore(channelId, lastMessageId, amount);
	}
	
	public static ArrayList<MessageInfo> getChannelMessagesBefore(int channelId, int messageId, int amount) throws SQLException {
		String select = "select id,type,content,moment_sent, sender_id " +
				"from message,channel_message " +
				"where message.id = channel_message.message_id " +
				"and channel_id = ? " +
				"and id < ? " +
				"order by moment_sent " +
				" limit ? ";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		statement.setInt(2, messageId);
		statement.setInt(3, amount);
		ResultSet result = statement.executeQuery();
		
		ArrayList<MessageInfo> messages = new ArrayList<>();
		while (result.next()) {
			int id = result.getInt("id");
			int senderId = result.getInt("sender_id");
			long utcTime = result.getDate("moment_sent").getTime();
			String type = result.getString("type");
			String content = result.getString("content");
			
			messages.add(new MessageInfo(id, senderId, MessageInfo.Recipient.CHANNEL, channelId, utcTime, type, content));
		}
		return messages;
	}
	
	public static ArrayList<MessageInfo> getUserMessages(int userId, int amount) throws SQLException {
		int lastMessageId = getLastMessageId() + 1;
		return getUserMessagesBefore(userId, lastMessageId, amount);
	}
	
	public static ArrayList<MessageInfo> getUserMessagesBefore(int userId, int messageId, int amount) throws SQLException {
		String select = "select id,type,content,moment_sent, sender_id " +
				"from message,user_message " +
				"where message.id = user_message.message_id " +
				"and (sender_id = ? or receiver_id = ?) " +
				"and id < ? " +
				"order by moment_sent " +
				"limit ? ;";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, userId);
		statement.setInt(2, userId);
		statement.setInt(3, messageId);
		statement.setInt(4, amount);
		ResultSet result = statement.executeQuery();
		ArrayList<MessageInfo> messages = new ArrayList<>();
		
		while (result.next()) {
			int id = result.getInt("id");
			int senderId = result.getInt("sender_id");
			long utcTime = result.getDate("moment_sent").getTime();
			String type = result.getString("type");
			String content = result.getString("content");
			
			messages.add(new MessageInfo(id, senderId, MessageInfo.Recipient.USER, userId, utcTime, type, content));
		}
		return messages;
	}
	
	
	public static boolean insertMessage(MessageInfo message) throws SQLException {
		synchronized (messageLock) {
			if (message.getRecipientType().equals(MessageInfo.Recipient.CHANNEL)) {
				return insertChannelMessage(message);
			} else {
				return insertUserMessage(message);
			}
		}
	}
	
	private static boolean insertUserMessage(MessageInfo message) throws SQLException {
		String insertMessage = "insert into message(id,sender_id,type,content) values(?,?,?,?)";
		String insertUserMessage = "insert into user_message(message_id,receiver_id) values(?,?)";
		message.setId(getLastMessageId() + 1);
		
		PreparedStatement statement = getApp().getPreparedStatement(insertMessage);
		statement.setInt(1, message.getId());
		statement.setInt(2, message.getSenderId());
		statement.setString(3, message.getType());
		statement.setString(4, message.getContent());
		boolean added = statement.executeUpdate() == 1;
		if (!added) return false;
		
		statement = getApp().getPreparedStatement(insertUserMessage);
		statement.setInt(1, message.getId());
		statement.setInt(2, message.getRecipientId());
		return statement.executeUpdate() == 1;
	}
	
	private static boolean insertChannelMessage(MessageInfo message) throws SQLException {
		String insertMessage = "insert into message(id,sender_id,type,content) values(?,?,?,?)";
		String insertChannelMessage = "insert into channel_message(message_id,channel_id) values(?,?)";
		message.setId(getLastMessageId() + 1);
		PreparedStatement statement = getApp().getPreparedStatement(insertMessage);
		statement.setInt(1, message.getId());
		statement.setInt(2, message.getSenderId());
		statement.setString(3, message.getType());
		statement.setString(4, message.getContent());
		boolean added = statement.executeUpdate() == 1;
		if (!added) return false;
		
		statement = getApp().getPreparedStatement(insertChannelMessage);
		statement.setInt(1, message.getId());
		statement.setInt(2, message.getRecipientId());
		return statement.executeUpdate() == 1;
	}
	
	
	private static int getLastChannelMessageId(int channelId) throws SQLException {
		String select = "select max(id) from message,channel_message where message_id = id and channel_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
	
	private static int getLastUserMessageId(int userId) throws SQLException {
		String select = "select max(id) from message,user_message where message_id = id and user_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, userId);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
	
	public static int getLastMessageId() throws SQLException {
		String select = "select max(id) from message";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
}
