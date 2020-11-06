package pt.Common;

public class Ids {
	
	private static final long serialVersionUID = 4326789L;
	
	private int userId;
	private int channelId;
	private int messageId;
	
	public Ids(int userId, int channelId, int messageId) {
		this.userId = userId;
		this.channelId = channelId;
		this.messageId = messageId;
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
}
