package pt;

public class Constants {
	
	public static final int UDP_PACKET_SIZE = 1024;
	public static final int MULTICAST_PORT = 5432;
	public static final String MULTICAST_GROUP = "228.5.6.7";
	
	public static final String ESTABLISH_CONNECTION = "ESTABLISH_CONNECTION";
	public static final String CONNECTION_ACCEPTED = "CONNECTION_ACCEPTED";
	public static final String CONNECTION_REFUSED = "CONNECTION_REFUSED";
	public static final long CONNECTION_TIMEOUT = 1000 * 5;
	
	public static final String REGISTER = "REGISTER_ATTEMPT";
	public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
	public static final String REGISTER_ERROR = "REGISTER_ERROR";
	public static final String LOGIN = "LOGIN_ATTEMPT";
	public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public static final String LOGIN_ERROR = "LOGIN_ERROR";
	public static final String DISCONNECTING = "DISCONNECTING";
	public static final String SERVERS_LIST = "SERVERS_LIST";
	
	public static final String ERROR = "ERROR";
	public static final String INVALID_PROTOCOL = "INVALID_PROTOCOL";
	public static final String INVALID_REQUEST = "INVALID_REQUEST";
	
	private static final String DATABASE_URL = "jdbc:mysql://{1}:3306/{2}?autoReconnect=true&useSSL=false";
	private static final String DATABASE_NAME = "main";
	public static final String DATABASE_USER_NAME = "server";
	public static final String DATABASE_USER_PASSWORD = "VeryStrongPassword";
	
	public static String getDatabaseURL(String address) {
		return DATABASE_URL.replace("{1}", address).replace("{2}",DATABASE_NAME);
	}
}
