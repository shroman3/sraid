package com.shroman.secureraid.client;

import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLParsingException;

public class Config {
	private final int executerThreadsNum;
	private final int executerQueueSize;
	private final int serverConnectionQueueSize;


	public Config(XMLGetter xmlGetter) throws XMLParsingException {
		executerThreadsNum = xmlGetter.getIntField("client", "executer_threads_num");
		executerQueueSize = xmlGetter.getIntField("client", "executer_queue_size");
		serverConnectionQueueSize = xmlGetter.getIntField("client", "server_connection_queue_size");
	}
	
	public int getExecuterThreadsNum() {
		return executerThreadsNum;
	}

	public int getExecuterQueueSize() {
		return executerQueueSize;
	}

	public int getServerConnectionQueueSize() {
		return serverConnectionQueueSize;
	}
	
}
