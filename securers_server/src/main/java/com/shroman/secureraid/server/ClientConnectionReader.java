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
import com.shroman.secureraid.common.MessageType;

public class ClientConnectionReader extends Thread {
	private Socket socket;
	private int id;
	private Path clientPath;
	private Logger logger;
//	private int serverId;
	private int queueSize;

	public ClientConnectionReader(Path serverPath, Socket socket) throws IOException {
		this.socket = socket;
		id = socket.getInputStream().read();
		queueSize = socket.getInputStream().read();
//		serverId = socket.getInputStream().read();
		logger = Logger.getLogger("ClientConnection");
		// logger.info("Connected with server id: " + serverId);
		this.clientPath = Paths.get(serverPath.toString(), Integer.toString(id));
		Files.createDirectories(clientPath);
	}

	@Override
	public void run() {
		try {
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			ClientConnectionWriter writer = new ClientConnectionWriter(logger,
					new ObjectOutputStream(socket.getOutputStream()), queueSize);
			Worker worker = new Worker(logger, clientPath, writer, queueSize);
			writer.start();
			worker.start();

			Message message = null;
			while (true) {
				int length = 0;
				try {
					StopWatch stopWatch = new Log4JStopWatch(logger);
					message = (Message) input.readUnshared();
					stopWatch.stop(Integer.toString(message.getChunkId()), length + ",RECIEVE");
					if (message == null || message.getType() == MessageType.KILL) {
						break;
					}
					length = message.getDataLength();
					worker.addMessage(message);
				} catch (EOFException e) {
					break;
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					break;
				}
			}
			worker.addMessage(Message.KILL);
			worker.join();
		} catch (IOException | InterruptedException e) {
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
