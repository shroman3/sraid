package com.shroman.secureraid.common;

import java.io.Serializable;

public class Message implements Serializable {
	private static final long serialVersionUID = -8934948984810922669L;

	private MessageType type;
	int id = -1;
	private byte[] data = null;

	public Message(MessageType type, byte[] data, int id) {
		this.type = type;
		this.data = data;
		this.id = id;
	}

	public MessageType getType() {
		return type;
	}

	public byte[] getData() {
		return data;
	}

	public int getId() {
		return id;
	}
}
