package pt;

import java.io.Serializable;
import java.net.InetAddress;

public class ServerAddress implements Serializable {
	
	private static final long serialVersionUID = 444L;
	
	private InetAddress address;
	private int listeningUDPPort;
	
	public ServerAddress(InetAddress address, int listeningUDPPort) {
		this.address = address;
		this.listeningUDPPort = listeningUDPPort;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public int getUDPPort() {
		return listeningUDPPort;
	}
	
	public void setUDPPort(int listeningUDPPort) {
		this.listeningUDPPort = listeningUDPPort;
	}
}
