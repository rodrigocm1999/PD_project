package pt.Server;

import java.io.Serializable;
import java.util.Arrays;

public class FileBlock implements Serializable {
	//TODO send the actual blocks
	private String identifier;
	private int offset;
	private byte[] bytes;
	
	public FileBlock(String identifier,int offset, byte[] bytes) {
		this.identifier = identifier;
		this.offset = offset;
		this.bytes = bytes;
	}
	
	@Override
	public String toString() {
		return "FileBlock{" +
				"identifier='" + identifier + '\'' +
				", offset=" + offset +
				", bytes length=" + bytes.length +
				'}';
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
}
