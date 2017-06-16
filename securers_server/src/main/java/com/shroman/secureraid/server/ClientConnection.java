package com.shroman.secureraid.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class ClientConnection extends Thread {
	private Socket socket;
	private int id;
	private Path clientPath;
	private Logger logger;
	private int serverId;

	public ClientConnection(Path serverPath, Socket socket) throws IOException {
		this.socket = socket;
		id = socket.getInputStream().read();
		serverId = socket.getInputStream().read();
		logger = Logger.getLogger("ClientConnection");
//		logger.info("Connected with server id: " + serverId);
		this.clientPath = Paths.get(serverPath.toString(), Integer.toString(id));	
		Files.createDirectories(clientPath);
	}

	@Override
	public void run() {
		try {
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Message message = null;
			while (true) {
				Response response;
				int length = 0;
				int responseLength = 0;
				StopWatch stopWatch = new Log4JStopWatch(logger);
				StopWatch fullStopWatch = new Log4JStopWatch(logger);
				try {
					message = (Message) input.readUnshared();
//					String messageTag = message.toString();
					length = message.getDataLength();
					stopWatch.stop(Integer.toString(message.getChunkId()), length + ",RECIEVE");
					stopWatch.start();
					response = Operation.executeOperation(clientPath, message);
					if (response == null) {
						break;
					}
					responseLength = response.getDataLength(); 
					length += responseLength;
					stopWatch.stop(Integer.toString(message.getChunkId()), length + ",EXECUTED");
					stopWatch.start();
				} catch (EOFException e) {
					return;
				} catch (IOException e) {
					if (message != null) {
						response = new Response(ResponseType.ERROR, e.getMessage().getBytes(), message.getObjectId(),
								message.getChunkId());
					} else {
						response = new Response(ResponseType.ERROR, e.getMessage().getBytes(), -1, -1);
					}
					e.printStackTrace();
				}
				output.reset();
				output.writeUnshared(response);
				stopWatch.stop(Integer.toString(message.getChunkId()), responseLength + ",SEND");
				fullStopWatch.stop(Integer.toString(message.getChunkId()), responseLength + ",FULL");
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getClientId() {
		return id;
	}
}
