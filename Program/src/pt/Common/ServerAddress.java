package pt.Common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

public class ServerAddress implements Serializable {
	
	private static final long serialVersionUID = 6374822L;
	
	private InetAddress address;
	private int udpPort;
	
	public ServerAddress(InetAddress address, int udpPort) {
		this.address = address;
		this.udpPort = udpPort;
	}
	
	public ServerAddress(int udpPort) {
		this.udpPort = udpPort;
	}
	
	@Override
	public String toString() {
		return "ServerAddress{" +
				"address=" + address +
				", listeningUDPPort=" + udpPort +
				'}';
	}
	
	public String getServerId() {
		return address.getHostAddress() + ":" + udpPort;
	}
	
	@Override
	public boolean equals(Object obj) { // comparações
		if (obj == null) return false;
		if (!(obj instanceof ServerAddress)) return false;
		ServerAddress other = (ServerAddress) obj;
		return getAddress().equals(other.getAddress())
				&& getUDPPort() == other.getUDPPort();
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public int getUDPPort() {
		return udpPort;
	}
	
	public void setUDPPort(int listeningUDPPort) {
		this.udpPort = listeningUDPPort;
	}
}
