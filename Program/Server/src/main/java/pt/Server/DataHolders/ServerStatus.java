package pt.Server.DataHolders;

import pt.Common.ServerAddress;

import java.io.Serializable;
import java.net.InetAddress;

public class ServerStatus implements Serializable,Comparable<ServerStatus> {
	
	private static final long serialVersionUID = 954738192L;
	
	private int connectedUsers;
	private ServerAddress address;
	private boolean heartbeat;
	
	public ServerStatus(int connectedUsers, ServerAddress address) {
		this.connectedUsers = connectedUsers;
		this.address = address;
		this.heartbeat = true;
	}
	
	@Override
	public String toString() {
		return "ServerStatus{" +
				"connectedUsers=" + connectedUsers +
				", address=" + address +
				", heartbeat=" + heartbeat +
				'}';
	}
	
	public int getConnectedUsers() {
		return connectedUsers;
	}
	
	public void setConnectedUsers(int connectedUsers) {
		this.connectedUsers = connectedUsers;
	}
	
	public InetAddress getAddress() {
		return address.getAddress();
	}
	
	public void setAddress(InetAddress address) {
		this.address.setAddress(address);
	}
	
	public int getUdpPort() {
		return address.getUDPPort();
	}
	
	public void setUdpPort(int udpPort) {
		getServerAddress().setUDPPort(udpPort);
	}
	
	public ServerAddress getServerAddress() {
		return address;
	}
	
	public void setServerAddress(ServerAddress serverAddress){
		address = serverAddress;
	}
	
	public boolean getHeartbeat() {
		return heartbeat;
	}
	
	public void setHeartbeat(boolean heartbeat) {
		this.heartbeat = heartbeat;
	}
	
	@Override
	public int compareTo(ServerStatus o) {
		return connectedUsers - o.connectedUsers;
	}
}
