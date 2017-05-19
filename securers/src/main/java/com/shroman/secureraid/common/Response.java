package com.shroman.secureraid.common;

import java.io.Serializable;

public class Response implements Serializable {
	private static final long serialVersionUID = 5088480449520246664L;

	private ResponseType type;
	private byte[] data = null;
	private int objectId = -1;
	private int chunkId = -1;

	public Response(ResponseType type, byte[] data, int objectId, int chunkId) {
		this.type = type;
		this.data = data;
		this.objectId = objectId;
		this.chunkId  = chunkId;
	}
	
	public Response(ResponseType type) {
		this.type = type;
		if (type == ResponseType.ERROR) {
			System.err.println("something went wrong");
		}
	}

	public boolean isSuccess() {
		return type != ResponseType.ERROR;
	}
	
	public String getErrorMessage() {
		return new String(data);
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

	@Override
	public String toString() {
		return new StringBuilder().append(type).append(",").append(objectId).append(",").append(chunkId).toString();
	}
}
