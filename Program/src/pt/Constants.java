package pt;

public class Constants {
	
	public static final int UDP_PACKET_SIZE = 256;
	public static final int SERVER_PORT = 9321;
	
	public static final String ESTABLISH_CONNECTION = "ESTABLISH_CONNECTION";
	public static final String CONNECTION_ACCEPTED = "CONNECTION_ACCEPTED";
	public static final String CONNECTION_REFUSED = "CONNECTION_REFUSED";
	
	public  static final String REGISTER = "REGISTER_ATTEMPT";
	public  static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
	public  static final String LOGIN = "LOGIN_ATTEMPT";
	public  static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";

	public static final String DATABASE_URL = "jdbc:mysql://rodrigohost.ddns.net:3306/main";
	public static final String DATABASE_USER_NAME = "server";
	public static final String DATABASE_USER_PASSWORD = "VeryStrongPassword";
	
}
