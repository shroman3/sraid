package com.shroman.secureraid.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class Worker extends Thread {
	private BlockingQueue<Message> messages;
	private Logger logger;
	private Path clientPath;
	private ClientConnectionWriter writer;
	private int serverId;
	
	Worker(int serverId, Path clientPath, ClientConnectionWriter writer, int queueSize) {
		this.serverId = serverId;
		this.logger = Logger.getLogger("ClientWork");
		this.clientPath = clientPath;
		this.writer = writer;
		messages = new ArrayBlockingQueue<>(queueSize);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Message message = messages.poll(Integer.MAX_VALUE,TimeUnit.SECONDS);
				if (message == null || message.getType() == MessageType.KILL) {
					break;
				}
				StopWatch stopWatch = new Log4JStopWatch(logger);
//				stopWatch.start();
				Response response = null;
				try {
					response = Operation.executeOperation(clientPath, message);
					if (response == null) {
						break;
					}
				} catch (IOException e) {
					response = new Response(ResponseType.ERROR, e.getMessage().getBytes(), message.getObjectId(),
							message.getChunkId());
					e.printStackTrace();
				}
				int totalLength = message.getDataLength()+response.getDataLength();
				stopWatch.stop(Integer.toString(message.getChunkId()), totalLength + "," + serverId);
				writer.addResponse(response);
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("1 Something went wrong: " + e.getMessage(), e);
			}
		}
		writer.addResponse(Response.KILL);
		try {
			writer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("2 Something went wrong: " + e.getMessage(), e);
		}
	}

	public void addMessage(Message message) {
		while(true) {			
			try {
				messages.put(message);
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
