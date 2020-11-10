package pt.Server;

import pt.Common.ChannelInfo;
import pt.Common.MessageInfo;
import pt.Common.Utils;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ChannelManager {
	
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
	
	public static boolean createChannel(ChannelInfo channel) throws SQLException, NoSuchAlgorithmException {
		String insert = "insert into channel(id,creator_id,name,password_hash,description) values(?,?,?,?,?)";
		PreparedStatement statement = getApp().getPreparedStatement(insert);
		// set new channel id
		channel.setId(getLastChannelId() + 1);
		statement.setInt(1, channel.getId());
		statement.setInt(2, channel.getCreatorId());
		statement.setString(3, channel.getName());
		statement.setString(4, Utils.hashStringBase36(channel.getPassword()));
		statement.setString(5, channel.getDescription());
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
	
	public static boolean isUserPartOf(int userId, int channelId) throws SQLException {
		String select = "select count(user_id) from channel_user where user_id = ? and channel_id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, userId);
		statement.setInt(2, channelId);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1) == 1;
	}
	
	public static boolean isChannelPassword(int channelId, String password) throws NoSuchAlgorithmException, SQLException {
		String select = "select count(id) from channel where id = ? and password_hash = ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, channelId);
		statement.setString(2, Utils.hashStringBase36(password));
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1) == 1;
	}
	
	public static boolean registerUserToChannel(int userId, int channelId) throws SQLException {
		String insert = "insert into channel_user(channel_id,user_id) values(?,?)";
		PreparedStatement statement = getApp().getPreparedStatement(insert);
		statement.setInt(1, channelId);
		statement.setInt(2, userId);
		return statement.executeUpdate() == 1;
	}
	
	public static boolean updateChannel(ChannelInfo channel) throws SQLException, NoSuchAlgorithmException {
		String insert = "update channel set name = ?, password_hash = ?, description = ? where id = ?";
		PreparedStatement statement = getApp().getPreparedStatement(insert);
		statement.setString(1, channel.getName());
		statement.setString(2, Utils.hashStringBase36(channel.getPassword()));
		statement.setString(3, channel.getDescription());
		statement.setInt(4, channel.getId());
		return statement.executeUpdate() == 1;
	}
	
	public static int getLastChannelId() throws SQLException {
		String select = "select max(id) from channel";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
}
