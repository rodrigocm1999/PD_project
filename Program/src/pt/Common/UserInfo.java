package pt.Common;

import java.io.Serializable;

public class UserInfo implements Serializable {
	
	private static final long serialVersionUID = 758943748912L;
	
	public String name;
	public String username;
	public String password;
	public String photoPath;
	
	public UserInfo(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public UserInfo(String name, String username, String password, String photoPath) {
		this.name = name;
		this.username = username;
		this.password = password;
		this.photoPath = photoPath;
	}
	
	@Override
	public String toString() {
		return "UserInfo{" +
				"name='" + getName() + '\'' +
				", username='" + getUsername() + '\'' +
				", password='" + getPassword() + '\'' +
				", photoPath='" + getPhotoPath() + '\'' +
				'}';
	}
	
	public String getName() {
		return name != null ? name : "";
	}
	
	public String getUsername() {
		return username != null ? username : "";
	}
	
	public String getPassword() {
		return password != null ? password : "";
	}
	
	public String getPhotoPath() {
		return photoPath != null ? photoPath : "";
	}
}
