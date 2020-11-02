package pt.Server;

import pt.Common.ServerAddress;

import javax.xml.crypto.Data;
import java.io.Serializable;
import java.net.DatagramPacket;

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
	
	public ServerAddress getServerAddress(DatagramPacket packet){
		serverAddress.setAddress(packet.getAddress());
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
