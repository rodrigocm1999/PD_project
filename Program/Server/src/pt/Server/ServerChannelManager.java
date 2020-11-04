package pt.Server;

import pt.Common.ChannelInfo;
import pt.Common.MessageInfo;
import pt.Common.Utils;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerChannelManager {
	
	private static ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public static ArrayList<ChannelInfo> getChannels(int userId) throws SQLException {
		String select = "select id,creator_id,name,description,(" +
				"select count(*) from channel_user where channel_id = id and user_id = ?" +
				") as is_part_of_channel from channel order by name asc";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, userId);
		ResultSet rs = statement.executeQuery();
		
		ArrayList<ChannelInfo> channels = new ArrayList<>();
		while (rs.next()) {
			channels.add(new ChannelInfo(
					rs.getInt("id"), rs.getInt("creator_id"),
					rs.getString("name"), rs.getString("description"),
					rs.getInt("is_part_of_channel") == 1));
		}
		return channels;
	}
	
	public static boolean createChannel(int creatorId, String name, String password, String description) throws SQLException, NoSuchAlgorithmException {
		String insert = "insert into channel(creator_id,name,description,password_hash) values(?,?,?,?)";
		PreparedStatement statement = getApp().getPreparedStatement(insert);
		statement.setInt(1, creatorId);
		statement.setString(2, name);
		statement.setString(3, Utils.hashStringBase36(password));
		statement.setString(4, description);
		return statement.executeUpdate() == 1; // Changed 1 row, it means it was added
	}
	
	public static boolean deleteChannel(int channelId) throws SQLException {
		String delete = "delete from channel where id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(delete);
		statement.setInt(1, channelId);
		return statement.executeUpdate() == 1;
	}
	
	public static boolean isUserChannelOwner(int userid, int channelId) throws SQLException {
		String select = "select count(id) from channel where id = ? and creator_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		statement.setInt(2, userid);
		ResultSet rs = statement.executeQuery();
		rs.next();
		return rs.getInt(1) == 1;
	}
	
	public static ArrayList<MessageInfo> getChannelMessagesBefore(int channelId, int amount) throws SQLException {
		String select = "select max(id) from message,channel_message where message_id = id and channel_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		ResultSet result = statement.executeQuery();
		result.next();
		int lastMessageId = result.getInt(1);
		
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
	
	public static ArrayList<MessageInfo> getUserMessagesBefore(int userId, int messageId, int amount) throws SQLException {
		String select = "select id,type,content,moment_sent, sender_id " +
				"from message,user_message " +
				"where message.id = user_message.message_id " +
				"and (sender_id = ? or receiver_id = ?) " +
				"and moment_sent < ( " +
				"   select mess.moment_sent " +
				"   from message as mess " +
				"   where id = ? " +
				") limit ?";
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
}
