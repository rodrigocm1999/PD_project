package pt;

import java.io.Serializable;

public class UserInfo implements Serializable {
	public String name;
	public String username;
	public String password;
	public String photoPath;


	public UserInfo(String name, String username, String password, String photoPath) {
		this.name = name;
		this.username = username;
		this.password = password;
		this.photoPath = photoPath;
	}
}
