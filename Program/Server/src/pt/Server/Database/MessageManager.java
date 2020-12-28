package pt.Server.Database;

import pt.Common.MessageInfo;
import pt.Server.ServerMain;

import java.sql.Date;
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
		String select = "select id,type,content,moment_sent, sender_id, (select username from user where user.id = sender_id) as sender_username " +
				"from message,channel_message " +
				"where message.id = channel_message.message_id " +
				"and channel_id = ? " +
				"and id < ? " +
				"order by moment_sent and id " +
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
			String senderUsername = result.getString("sender_username");
			
			messages.add(new MessageInfo(id, senderId, MessageInfo.Recipient.CHANNEL, channelId, utcTime, type, content,senderUsername));
		}
		return messages;
	}
	
	public static ArrayList<MessageInfo> getUserMessages(int thisUserId, int otherUserId, int amount) throws SQLException {
		int lastMessageId = getLastMessageId() + 1;
		return getUserMessagesBefore(thisUserId, otherUserId, lastMessageId, amount);
	}
	
	public static ArrayList<MessageInfo> getUserMessagesBefore(int thisUserId, int otherUserId, int messageId, int amount) throws SQLException {
		String select = "select id,type,content,moment_sent, sender_id " +
				"from message,user_message " +
				"where message.id = user_message.message_id " +
				"and (sender_id = ? and receiver_id = ? or sender_id = ? and receiver_id = ?) " +
				"and id < ? " +
				"order by moment_sent " +
				"limit ? ;";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, thisUserId);
		statement.setInt(2, otherUserId);
		statement.setInt(3, otherUserId);
		statement.setInt(4, thisUserId);
		statement.setInt(5, messageId);
		statement.setInt(6, amount);
		ResultSet result = statement.executeQuery();
		ArrayList<MessageInfo> messages = new ArrayList<>();
		
		while (result.next()) {
			int id = result.getInt("id");
			int senderId = result.getInt("sender_id");
			long utcTime = result.getDate("moment_sent").getTime();
			String type = result.getString("type");
			String content = result.getString("content");
			
			messages.add(new MessageInfo(id, senderId, MessageInfo.Recipient.USER, thisUserId, utcTime, type, content));
		}
		return messages;
	}
	
	public static boolean insertMessage(MessageInfo message) throws SQLException {
		synchronized (messageLock) {
			String insertMessage = "insert into message(id,sender_id,type,content) values(?,?,?,?)";
			message.setId(getLastMessageId() + 1);
			PreparedStatement statement = getApp().getPreparedStatement(insertMessage);
			statement.setInt(1, message.getId());
			statement.setInt(2, message.getSenderId());
			statement.setString(3, message.getType());
			statement.setString(4, message.getContent());
			boolean added = statement.executeUpdate() == 1;
			if (!added) return false;
			
			String tableInsert = "";
			if (message.getRecipientType().equals(MessageInfo.Recipient.CHANNEL)) {
				tableInsert = "insert into channel_message(message_id,channel_id) values(?,?)";
			} else {
				tableInsert = "insert into user_message(message_id,receiver_id) values(?,?)";
			}
			statement = getApp().getPreparedStatement(tableInsert);
			statement.setInt(1, message.getId());
			statement.setInt(2, message.getRecipientId());
			if (statement.executeUpdate() == 1) {
				return true;
			}
			return false;
		}
	}
	
	public static boolean insertFull(MessageInfo message) throws SQLException {
		synchronized (messageLock) {
			String insert = "insert into message(id,sender_id,type,content,moment_sent) values(?,?,?,?,?)";
			PreparedStatement statement = getApp().getPreparedStatement(insert);
			statement.setInt(1, message.getId());
			statement.setInt(2, message.getSenderId());
			statement.setString(3, message.getType());
			statement.setString(4, message.getContent());
			statement.setDate(5, new Date(message.getMomentSent()));
			
			boolean added = statement.executeUpdate() == 1;
			if (!added) return false;
			
			String tableInsert = "";
			if (message.getRecipientType().equals(MessageInfo.Recipient.CHANNEL)) {
				tableInsert = "insert into channel_message(message_id,channel_id) values(?,?)";
			} else {
				tableInsert = "insert into user_message(message_id,receiver_id) values(?,?)";
			}
			statement = getApp().getPreparedStatement(tableInsert);
			statement.setInt(1, message.getId());
			statement.setInt(2, message.getRecipientId());
			return statement.executeUpdate() == 1;
		}
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
	
	public static ArrayList<MessageInfo> getAfterId(int lastMessageId) throws SQLException {
		String select = "select id,sender_id,moment_sent,type,content, " +
				" (select channel_id from channel_message where message.id = message_id) as channel_id, " +
				" (select receiver_id from user_message where message.id = message_id) as receiver_id " +
				"from message " +
				"where id > ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, lastMessageId);
		ResultSet result = statement.executeQuery();
		
		ArrayList<MessageInfo> list = new ArrayList<>();
		
		while (result.next()) {
			MessageInfo.Recipient recipientType = result.getString("channel_id") != null ? MessageInfo.Recipient.CHANNEL : MessageInfo.Recipient.USER;
			int recipientId = recipientType == MessageInfo.Recipient.CHANNEL ? result.getInt("channel_id") : result.getInt("receiver_id");
			
			list.add(new MessageInfo(result.getInt("id"),
					result.getInt("sender_id"),
					recipientType,
					recipientId,
					result.getDate("moment_sent").getTime(),
					result.getString("type"),
					result.getString("content")));
		}
		return list;
	}
	
	public static MessageInfo getMessageById(int messageId) throws SQLException {
		String select = "select id,sender_id,moment_sent,type,content, " +
				" (select channel_id from channel_message where message.id = message_id) as channel_id, " +
				" (select receiver_id from user_message where message.id = message_id) as receiver_id " +
				"from message " +
				"where id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, messageId);
		ResultSet result = statement.executeQuery();
		result.next();
		
		MessageInfo.Recipient recipientType = result.getString("channel_id") != null ? MessageInfo.Recipient.CHANNEL : MessageInfo.Recipient.USER;
		int recipientId = recipientType == MessageInfo.Recipient.CHANNEL ? result.getInt("channel_id") : result.getInt("receiver_id");
		
		return new MessageInfo(
				result.getInt("id"), result.getInt("sender_id"),
				recipientType, recipientId,
				result.getDate("moment_sent").getTime(),
				result.getString("type"), result.getString("content"));
	}
}
