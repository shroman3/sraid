package com.shroman.secureraid.server;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class ReadObjectOperation extends Operation {
	@Override
	protected MessageType getMessageType() {
		return MessageType.READ_OBJECT;
	}

	@Override
	protected Response execute(int id, Message message) {
		return new Response(ResponseType.OK, null);
	}
}
