package pt.Common;

import java.io.Serializable;

public class UserInfo implements Serializable {
	
	private static final long serialVersionUID = 758943748912L;
	
	private int userId;
	private String name;
	private String username;
	private String password;
	private long creationMoment;
	private byte[] imageBytes;
	private boolean hasImage;

	
	public UserInfo(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public UserInfo(int userId, String name, String username) {
		this.userId = userId;
		this.name = name;
		this.username = username;
	}
	
	public UserInfo(String name, String username, String password, byte[] imageBytes) {
		this.name = name;
		this.username = username;
		this.password = password;
		this.imageBytes = imageBytes;
	}
	
	public UserInfo(int userId, String name, String username, String password, long creationMoment,boolean hasImage) {
		this.userId = userId;
		this.name = name;
		this.username = username;
		this.password = password;
		this.creationMoment = creationMoment;
		this.hasImage = hasImage;
	}
	
	@Override
	public String toString() {
		return "UserInfo{" +
				"userId=" + userId +
				", name='" + name + '\'' +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				'}';
	}
	
	public int getUserId() {
		return userId;
	}
	
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public long getCreationMoment() {
		return creationMoment;
	}
	
	public void setCreationMoment(long creationMoment) {
		this.creationMoment = creationMoment;
	}
	
	public byte[] getImageBytes() {
		return imageBytes;
	}
	
	public void setImageBytes(byte[] imageBytes) {
		this.imageBytes = imageBytes;
	}
	
	public boolean hasImage() {
		return imageBytes != null || hasImage;
	}
	
	public void setHasImage(boolean hasImage) {
		this.hasImage = hasImage;
	}
}
