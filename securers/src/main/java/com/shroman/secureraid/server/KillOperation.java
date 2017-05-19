package com.shroman.secureraid.server;

import java.io.IOException;
import java.nio.file.Path;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;

public class KillOperation extends Operation {

	@Override
	protected MessageType getMessageType() {
		return MessageType.KILL;
	}

	@Override
	protected Response execute(Path executionPath, Message message) throws IOException {
		return null;
	}
}
