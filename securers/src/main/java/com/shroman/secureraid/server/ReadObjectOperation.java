package com.shroman.secureraid.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	protected Response execute(Path executionPath, Message message) throws IOException {
        Path path = Paths.get(executionPath.toString(), Integer.toString(message.getObjectId()));
        if(!Files.exists(path)) {
        	return new Response(ResponseType.ERROR, "Input object doesn't exists".getBytes(), message.getObjectId(), message.getChunkId());
        }
        return new Response(ResponseType.OK ,Files.readAllBytes(path), message.getObjectId(), message.getChunkId());
	}
}
