package pt.Server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerMessageManager {
	
	private static ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public static ArrayList<String> getLikeFiles(String fileName) throws SQLException {
		String select = "select content from message where type = 'file' and content like ?";
		PreparedStatement preparedStatement = ServerMain.getInstance().getPreparedStatement(select);
		preparedStatement.setString(1, fileName + '%');
		ResultSet resultSet = preparedStatement.executeQuery();
		
		ArrayList<String> list = new ArrayList<>();
		while(resultSet.next()){
			list.add(resultSet.getString(1));
		}
		return list;
	}
	
}
