package pt.Common;

public class MessageInfo {
	
	public static final String TYPE_TEXT = "TYPE_TEXT";
	public static final String TYPE_FILE = "TYPE_FILE";
	
	int id;
	int senderId;
	long momentSent; // UTC time
	String type;
	String content;
	
	public MessageInfo(String type, String content) {
		this.type = type;
		this.content = content;
	}
	
	public MessageInfo(int id, int senderId, long momentSent, String type, String content) {
		this.id = id;
		this.senderId = senderId;
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
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public long getMomentSent() {
		return momentSent;
	}
	
	public void setMomentSent(long momentSent) {
		this.momentSent = momentSent;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
}
