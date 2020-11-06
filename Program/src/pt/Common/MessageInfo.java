package pt.Common;

import java.io.Serializable;

public class MessageInfo implements Serializable {
	
	private static final long serialVersionUID = 8574382340L;
	
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_FILE = "file";
	
	public enum Recipient {
		USER,CHANNEL
	}
	
	private int id;
	private int senderId;
	private Recipient recipientType;
	private int recipientId;
	private long momentSent; // UTC time
	private String type;
	private String content;
	
	public MessageInfo(String type, String content) {
		this.type = type;
		this.content = content;
	}
	
	public MessageInfo(int id, int senderId, Recipient recipientType, int recipientId, long momentSent, String type, String content) {
		this.id = id;
		this.senderId = senderId;
		this.recipientType = recipientType;
		this.recipientId = recipientId;
		this.momentSent = momentSent;
		this.type = type;
		this.content = content;
	}
	
	@Override
	public String toString() {
		return "MessageInfo{" +
				"id=" + id +
				", type='" + type + '\'' +
				", momentSent=" + momentSent +
				", content='" + content + '\'' +
				'}';
	}
	
	public int getSenderId() {
		return senderId;
	}
	
	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
	
	public Recipient getRecipientType() {
		return recipientType;
	}
	
	public void setRecipientType(Recipient recipientType) {
		this.recipientType = recipientType;
	}
	
	public int getRecipientId() {
		return recipientId;
	}
	
	public void setRecipientId(int recipientId) {
		this.recipientId = recipientId;
	}
	
	public long getMomentSent() {
		return momentSent;
	}
	
	public void setMomentSent(long momentSent) {
		this.momentSent = momentSent;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
}
