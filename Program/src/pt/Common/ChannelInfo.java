package pt.Common;

import java.io.Serializable;
import java.util.ArrayList;

public class ChannelInfo implements Serializable {
	
	private static final long serialVersionUID = 536475864376L;
	
	private int id;
	private int creatorId;
	private String name;
	private String password;
	private String description;
	private boolean isPartOf;
	private ArrayList<MessageInfo> messages;
	
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

	public ChannelInfo(String name, String password, String description) {
		this.name = name;
		this.password = password;
		this.description = description;
	}

	public ChannelInfo(int id, String password) {
		this.id = id;
		this.password = password;
	}
	
	public boolean isOwner(int userId) {
		return userId == creatorId;
	}
	
	@Override
	public String toString() {
		return "ChannelInfo{" +
				"id=" + id +
				", creatorId=" + creatorId +
				", name='" + name + '\'' +
				", password='" + password + '\'' +
				", description='" + description + '\'' +
				", isPartOf=" + isPartOf +
				'}';
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getCreatorId() {
		return creatorId;
	}
	
	public void setCreatorId(int creatorId) {
		this.creatorId = creatorId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public boolean isPartOf() {
		return isPartOf;
	}
	
	public void setPartOf(boolean partOf) {
		isPartOf = partOf;
	}
	
	public ArrayList<MessageInfo> getMessages() {
		return messages;
	}
	
	public void setMessages(ArrayList<MessageInfo> messages) {
		this.messages = messages;
	}
}
