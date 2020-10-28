package pt;

import java.io.Serializable;

public class ChannelInfo implements Serializable {
	
	public int id;
	public int creatorId;
	public String name;
	public String description;
	public boolean isPartOf;
	
	public ChannelInfo(int id, int creatorId, String name, String description, boolean isPartOf) {
		this.id = id;
		this.creatorId = creatorId;
		this.name = name;
		this.description = description;
		this.isPartOf = isPartOf;
	}
	
	public ChannelInfo(int id, int creatorId, String name, String description) {
		this.id = id;
		this.creatorId = creatorId;
		this.name = name;
		this.description = description;
	}
	
	public boolean isOwner(int userId) {
		return userId == creatorId;
	}
}
