package pt.Server;

import pt.Common.UserInfo;
import pt.Common.Utils;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServerUserManager {
	
	public static boolean insertUser(UserInfo user) throws SQLException, NoSuchAlgorithmException {
		// insert the new user into the database ------------------------------------------
		String insert = "insert into user(name,username,password_hash,photo_path) values(?,?,?,?)";
		PreparedStatement preparedStatement = ServerMain.getInstance().getPreparedStatement(insert);
		preparedStatement.setString(1, user.getName());
		preparedStatement.setString(2, user.getUsername());
		preparedStatement.setString(3, Utils.hashStringBase36(user.getPassword()));
		preparedStatement.setString(4, user.getPhotoPath());
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
		PreparedStatement statement = ServerMain.getInstance().getPreparedStatement(select);
		statement.setInt(1, userId);
		ResultSet result = statement.executeQuery();
		if (!result.next())
			throw new SQLException("WTF HOW DID THIS HAPPEN");
		return result.getString(1);
	}
}
