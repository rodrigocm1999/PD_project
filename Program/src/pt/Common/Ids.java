package pt.Common;

import java.io.Serializable;

public class Ids implements Serializable {
	
	private static final long serialVersionUID = 4326789L;
	
	private int genericId;
	private int userId;
	private int channelId;
	private int messageId;
	
	public Ids(int userId, int channelId, int messageId) {
		this.userId = userId;
		this.channelId = channelId;
		this.messageId = messageId;
	}
	
	@Override
	public String toString() {
		return "Ids{" +
				"userId=" + userId +
				", channelId=" + channelId +
				", messageId=" + messageId +
				'}';
	}
	
	public int getGenericId() {
		return genericId;
	}
	
	public void setGenericId(int genericId) {
		this.genericId = genericId;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public int getChannelId() {
		return channelId;
	}
	
	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}
	
	public int getMessageId() {
		return messageId;
	}
	
	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}
	
	public void setChannelOnly(int channelId) {
		this.channelId = channelId;
		this.userId = -1;
	}
	
	public void setUserOnly(int userId) {
		this.userId = userId;
		this.channelId = -1;
	}
}
