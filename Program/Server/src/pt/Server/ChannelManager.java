package pt.Server;

import pt.Common.*;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;

public class ChannelManager {
	
	private static Object channelLock = new Object();
	
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
		synchronized (channelLock) {
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
	}
	
	public static boolean insertFull(ChannelInfo channel) throws SQLException {
		synchronized (channelLock) {
			String insert = "insert into channel(id,creator_id,name,password_hash,description,creation_moment) values(?,?,?,?,?,?)";
			PreparedStatement statement = getApp().getPreparedStatement(insert);
			statement.setInt(1, channel.getId());
			statement.setInt(2, channel.getCreatorId());
			statement.setString(3, channel.getName());
			statement.setString(4, channel.getPassword());
			statement.setString(5, channel.getDescription());
			statement.setDate(6, new Date(channel.getCreationMoment()));
			return statement.executeUpdate() == 1; // Changed 1 row, it means it was added
		}
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
	
	public static boolean isChannelPassword(int channelId, String password) throws
			NoSuchAlgorithmException, SQLException {
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
		synchronized (channelLock) {
			String insert = "update channel set name = ?, password_hash = ?, description = ? where id = ?";
			PreparedStatement statement = getApp().getPreparedStatement(insert);
			statement.setString(1, channel.getName());
			statement.setString(2, Utils.hashStringBase36(channel.getPassword()));
			statement.setString(3, channel.getDescription());
			statement.setInt(4, channel.getId());
			return statement.executeUpdate() == 1;
		}
	}
	
	public static int getLastChannelId() throws SQLException {
		String select = "select max(id) from channel";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
	
	public static ArrayList<ChannelInfo> getAfterId(int id) throws SQLException {
		String select = "select id,creator_id,name,password_hash,description,creation_moment from channel where id > ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, id);
		ResultSet result = statement.executeQuery();
		
		ArrayList<ChannelInfo> list = new ArrayList<>();
		
		while (result.next()) {
			list.add(new ChannelInfo(result.getInt("id"),
					result.getString("name"),
					result.getString("username"),
					result.getString(("password_hash"))));
		}
		return list;
	}
	
	
	public static ArrayList<Ids> getChannelUsersAfterIds(int lastConnectionId) throws SQLException {
		String select = "select channel_id,user_id from channel_user where id > ?";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setInt(1, lastConnectionId);
		ResultSet result = statement.executeQuery();
		
		ArrayList<Ids> list = new ArrayList<>();
		
		while (result.next()) {
			list.add(new Ids(
					result.getInt("user_id"),
					result.getInt("channel_id"),
					-1));
		}
		
		return list;
	}
	
	public static int getLastUserChannelId() throws SQLException {
		String select = "select max(id) from channel_user";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt("id");
	}
	
	public static boolean checkNameAvailability(String name) throws SQLException {
		String select = "select count(id) from channel where name = ?";
		PreparedStatement stat = ServerMain.getInstance().getPreparedStatement(select);
		stat.setString(1, name);
		ResultSet result = stat.executeQuery();
		result.next();
		return result.getInt(1) == 1;
	}
}
