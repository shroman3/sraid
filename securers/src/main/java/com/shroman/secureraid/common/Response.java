package com.shroman.secureraid.common;

import java.io.Serializable;

public class Response implements Serializable {
	private static final long serialVersionUID = 5088480449520246664L;

	private ResponseType type;
	private byte[] data;

	public Response(ResponseType type, byte[] data) {
		this.type = type;
		this.data = data;
	}

	public boolean isSuccess() {
		return type == ResponseType.OK;
	}
	
	public String getErrorMessage() {
		return new String(data);
	}

	public byte[] getData() {
		return data;
	}
}
