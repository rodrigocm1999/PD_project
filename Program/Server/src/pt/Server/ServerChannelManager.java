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
					rs.getInt(1), rs.getInt(2),
					rs.getString(3), rs.getString(4),
					rs.getInt(5) == 1));
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
	
	public static ArrayList<MessageInfo> getChannelMessagesBefore(int channelId, int messageId) throws SQLException {
		String select = "select count(id) from channel where id = ? and creator_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		statement.setInt(2, messageId);
		
		ArrayList<MessageInfo> messages = new ArrayList<>();
		return messages;
	}
}
