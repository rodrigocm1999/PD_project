package pt.Server;

import pt.Common.ServerAddress;

import java.io.Serializable;

public class ServerCommand implements Serializable {
	
	private String protocol;
	private ServerAddress serverAddress;
	private Object extras;
	
	
	public ServerCommand(String protocol, ServerAddress serverAddress, Object extras) {
		this.protocol = protocol;
		this.serverAddress = serverAddress;
		this.extras = extras;
	}
	
	public ServerCommand(String protocol, ServerAddress serverAddress) {
		this.protocol = protocol;
		this.serverAddress = serverAddress;
	}
	
	public ServerAddress getServerAddress() {
		return serverAddress;
	}
	
	public void setServerAddress(ServerAddress serverAddress) {
		this.serverAddress = serverAddress;
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
