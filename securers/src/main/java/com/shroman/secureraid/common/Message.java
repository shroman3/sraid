package com.shroman.secureraid.common;

import java.io.Serializable;

public class Message implements Serializable {
	private static final long serialVersionUID = -8934948984810922669L;

	private MessageType type;
	int objectId = -1;
	private int chunkId = -1;
	private byte[] data = null;


	public Message(MessageType type) {
		this.type = type;		
	}
	
	public Message(MessageType type, byte[] data, int objectId, int chunkId) {
		this.type = type;
		this.data = data;
		this.objectId = objectId;
		this.chunkId = chunkId;
	}

	public MessageType getType() {
		return type;
	}

	public byte[] getData() {
		return data;
	}

	public int getObjectId() {
		return objectId;
	}
	
	public int getChunkId() {
		return chunkId;
	}
}
