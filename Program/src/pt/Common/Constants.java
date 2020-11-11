package pt.Common;

public class Constants {
	
	public static final int UDP_PACKET_SIZE = 1024;
	
	public static final String ESTABLISH_CONNECTION = "ESTABLISH_CONNECTION";
	public static final String CONNECTION_ACCEPTED = "CONNECTION_ACCEPTED";
	public static final String CONNECTION_REFUSED = "CONNECTION_REFUSED";
	public static final int CONNECTION_TIMEOUT = 1000 * 5;
	
	public static final String REGISTER = "REGISTER_ATTEMPT";
	public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
	public static final String REGISTER_ERROR = "REGISTER_ERROR";
	public static final String LOGIN = "LOGIN_ATTEMPT";
	public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public static final String LOGIN_ERROR = "LOGIN_ERROR";
	
	public static final String CHANNEL_GET_ALL = "CHANNEL_GET_ALL";
	public static final String CHANNEL_GET_MESSAGES = "CHANNEL_GET_MESSAGES";
	public static final String CHANNEL_ADD = "CHANNEL_ADD";
	public static final String CHANNEL_REMOVE = "CHANNEL_REMOVE";
	public static final String CHANNEL_EDIT = "CHANNEL_EDIT";
	public static final String CHANNEL_REGISTER = "CHANNEL_REGISTER";
	
	public static final String ADD_MESSAGE = "ADD_MESSAGE";
	public static final String NEW_MESSAGE = "NEW_MESSAGE";
	
	public static final String USER_GET_LIKE = "USER_GET_LIKE";
	public static final String USER_GET_MESSAGES = "USER_GET_MESSAGES";
	
	public static final String NO_PERMISSIONS = "NO_PERMISSIONS";
	
	public static final String ADD_FILE = "ADD_FILE";
	public static final String GET_FILE = "GET_FILE";
	public static final String FILE_ACCEPT_CONNECTION = "FILE_ACCEPT_CONNECTION";
	public static final int CLIENT_FILE_CHUNK_SIZE = 128 * 1024;
	
	public static final String SUCCESS = "SUCCESS";
	public static final String FAILURE = "FAILURE";
	
	public static final String DISCONNECTING = "DISCONNECTING";
	public static final String LOGOUT = "LOGOUT";
	public static final String SERVERS_LIST = "SERVERS_LIST";
	
	public static final String ERROR = "ERROR";
	public static final String INVALID_PROTOCOL = "INVALID_PROTOCOL";
	public static final String INVALID_REQUEST = "INVALID_REQUEST";
	
	public static final int USER_IMAGE_SIZE = 256;
}
