package pt.Server;

import pt.Common.UserInfo;
import pt.Common.Utils;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class UserManager {
	
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
	
	public static ArrayList<UserInfo> getUsersLike(String username, int thisUserId) throws SQLException {
		String select = "select id,name,username from user where username like ? order by " +
				"(select count(id) from message,user_message where message.id = message_id && receiver_id = user.id), username " +
				" limit 30";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		statement.setString(1, "%" + username + "%");
		ResultSet result = statement.executeQuery();
		
		ArrayList<UserInfo> usersLike = new ArrayList<>();
		
		while (result.next()) {
			usersLike.add(new UserInfo(
					result.getInt("id"),
					result.getString("name"),
					result.getString("username")
			));
		}
		return usersLike;
	}
	
	public static int getLastUserId() throws SQLException {
		String select = "select max(id) from user";
		PreparedStatement statement = getApp().getPreparedStatement(select);
		ResultSet result = statement.executeQuery();
		result.next();
		return result.getInt(1);
	}
}
