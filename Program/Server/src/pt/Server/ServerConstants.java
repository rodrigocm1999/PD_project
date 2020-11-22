package pt.Server;

import java.io.File;

public class ServerConstants {
	
	public static final int MULTICAST_PORT = 5432;
	public static final String MULTICAST_GROUP = "239.234.102.123";
	
	
	public static final String HEARTBEAT = "HEARTBEAT";
	public static final int HEARTBEAT_SEND_INTERVAL = 1000 * 80;
	public static final int HEARTBEAT_WAIT_INTERVAL = 1000 * 100;// TODO REVERT this
	public static final int FAKE_USER_SYNC_COUNT = 25;
	public static final String ASK_SYNCHRONIZER = "ASK_SYNCHRONIZER";
	public static final String ACKNOWLEDGE = "ACKNOWLEDGE";
	
	public static final String CAME_ONLINE = "CAME_ONLINE";
	public static final String CAME_OFFLINE = "CAME_OFFLINE";
	public static final String AM_ONLINE = "AM_ONLINE";
	public static final String NEW_MESSAGE = "NEW_MESSAGE";
	public static final String PROTOCOL_NEW_USER = "PROTOCOL_NEW_USER";
	
	public static final String UPDATE_USER_COUNT = "UPDATE_USER_COUNT";
	public static final float ACCEPT_PERCENTAGE_THRESHOLD = 0.5f;
	
	private static final String DATABASE_URL = "jdbc:mysql://{1}:3306/{2}?allowPublicKeyRetrieval=true&autoReconnect=true&useSSL=false";
	public static final String DATABASE_NAME = "main";
	public static final String DATABASE_USER_NAME = "server";
	public static final String DATABASE_USER_PASSWORD = "VeryStrongPassword";
	
	public static String getDatabaseURL(String address, String name) {
		return DATABASE_URL.replace("{1}", address).replace("{2}", name);
	}
	
	
	public static final String PUBLIC_IP_ADDRESS_API = "https://api.ipify.org";
	
	public static final int DEFAULT_GET_MESSAGES_AMOUNT = 25;
	
	private static String getFilesPath() {
		return FILES_PATH + "_" + ServerMain.getInstance().getDatabaseName();
	}
	
	public static String getTransferredFilesPath() {
		return getFilesPath() + File.separator + ServerConstants.TRANSFERRED_FILES;
	}
	
	public static String getUserPhotosPath() {
		return getFilesPath() + File.separator + ServerConstants.USER_IMAGES_DIRECTORY;
	}
	
	public static String getPhotoPathFromUsername(String username) {
		return getUserPhotosPath() + File.separator + username + USER_PHOTO_EXTENSION;
	}
	
	
	private static final String FILES_PATH = "files";
	private static final String USER_IMAGES_DIRECTORY = "user_images";
	private static final String USER_PHOTO_EXTENSION = ".jpg";
	private static final String TRANSFERRED_FILES = "transferred_files";
	
}
