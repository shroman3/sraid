package com.shroman.secureraid.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class WriteObjectOperation extends Operation {
	@Override
	protected MessageType getMessageType() {
		return MessageType.WRITE_OBJECT;
	}

	@Override
	protected Response execute(int id, Message message) throws IOException {
		Path file = Paths.get(id + "/" + message.getId());
		Files.write(file, message.getData());
		return new Response(ResponseType.OK, null);
	}
}
