package pt.Server;

import pt.Common.MessageInfo;
import pt.Common.UserInfo;
import pt.Common.Utils;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerUserManager {
	
	private static ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public static boolean insertUser(UserInfo user, String imagePath) throws SQLException, NoSuchAlgorithmException {
		// insert the new user into the database ------------------------------------------
		String insert = "insert into user(name,username,password_hash,photo_path) values(?,?,?,?)";
		PreparedStatement preparedStatement = ServerMain.getInstance().getPreparedStatement(insert);
		preparedStatement.setString(1, user.getName());
		preparedStatement.setString(2, user.getUsername());
		preparedStatement.setString(3, Utils.hashStringBase36(user.getPassword()));
		preparedStatement.setString(4, imagePath);
		return preparedStatement.executeUpdate() == 1;
	}
	
	public static boolean checkUsernameAvailability(String username) throws SQLException {
		// check if username is already taken ---------------------------------------
		return !doesUsernameExist(username);
	}
	
	public static boolean doesUsernameExist(String username) throws SQLException {
		// check if username exists -------------------------------------------
		String select = "select count(id) from user where username = ?";
		PreparedStatement stat = ServerMain.getInstance().getPreparedStatement(select);
		stat.setString(1, username);
		ResultSet result = stat.executeQuery();
		result.next();
		return result.getInt(1) == 1;
	}
	
	public static boolean doesPasswordMatchUsername(String username, String password) throws SQLException, NoSuchAlgorithmException {
		// get the number of users with that password and username. should either return 1 or 0, never anything else
		String select = "select count(id) from user where username = ? and password_hash = ?";
		PreparedStatement preparedStatement = ServerMain.getInstance().getPreparedStatement(select);
		preparedStatement.setString(1, username);
		preparedStatement.setString(2, Utils.hashStringBase36(password));
		ResultSet resultSet = preparedStatement.executeQuery();
		resultSet.next();
		return resultSet.getInt(1) == 1;
	}
	
	public static int getUserId(String username) throws SQLException {
		String select = "select id from user where username = ?";
		PreparedStatement statement = ServerMain.getInstance().getPreparedStatement(select);
		statement.setString(1, username);
		ResultSet result = statement.executeQuery();
		if (!result.next())
			throw new SQLException("WTF HOW DID THIS HAPPEN");
		return result.getInt(1);
	}
	
	public static String getNameUser(int userId) throws SQLException {
		String select = "select name from user where id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, userId);
		ResultSet result = statement.executeQuery();
		if (!result.next())
			throw new SQLException("WTF HOW DID THIS HAPPEN");
		return result.getString(1);
	}
	
	public static boolean insertMessage(int senderId, int receiverId, String type, String content) throws SQLException {
		String insertMessage = "insert into message(id,sender_id,type,content) values(?,?,?,?)";
		String insertUserMessage = "insert into user_message(message_id,receiver_id) values(?,?)";
		int newMessageId = ServerChannelManager.getLastMessageId() + 1;
		PreparedStatement statement = getApp().getPreparedStatement(insertMessage);
		statement.setInt(1, newMessageId);
		statement.setInt(2, senderId);
		statement.setString(3, type);
		statement.setString(4, content);
		boolean added = statement.executeUpdate() == 1;
		if (!added) return false;
		
		statement = getApp().getPreparedStatement(insertUserMessage);
		statement.setInt(1, newMessageId);
		statement.setInt(2, receiverId);
		return statement.executeUpdate() == 1;
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
	
	private static int getLastChannelMessageId(int channelId) throws SQLException {
		String select = "select max(id) from message,channel_message where message_id = id and channel_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
}
