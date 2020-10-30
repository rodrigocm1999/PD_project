package pt;

import java.io.Serializable;

public class ChannelInfo implements Serializable {
	
	private static final long serialVersionUID = 222L;
	
	public int id;
	public int creatorId;
	public String name;
	public String password;
	public String description;
	public boolean isPartOf;
	
	public ChannelInfo(int id, int creatorId, String name, String description, boolean isPartOf) {
		this.id = id;
		this.creatorId = creatorId;
		this.name = name;
		this.description = description;
		this.isPartOf = isPartOf;
	}
	
	public ChannelInfo(int creatorId, String name, String password, String description) {
		this.creatorId = creatorId;
		this.name = name;
		this.password = password;
		this.description = description;
	}
	
	public boolean isOwner(int userId) {
		return userId == creatorId;
	}
}
