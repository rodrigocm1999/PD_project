package pt;

import java.io.Serializable;

public class Command implements Serializable {
	private String protocol;
	private Object extras;
	
	public Command(String protocol, Object extras) {
		this.protocol = protocol;
		this.extras = extras;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public Object getExtras() {
		return extras;
	}
	
	public void setExtras(Object extras) {
		this.extras = extras;
	}
}
