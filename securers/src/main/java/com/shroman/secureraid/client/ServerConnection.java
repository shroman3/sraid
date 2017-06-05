package com.shroman.secureraid.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.Response;

class ServerConnection extends Thread {
	private String host;
	private int port;
	private BlockingQueue<Message> messages; 
//	private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
	private Socket socket;
	private int serverId;
	private PushResponseInterface pushResponse;
	private Logger logger;
	private boolean die = false;

	ServerConnection(int serverId, int clientId, String host, int port, PushResponseInterface pushResponse, Config config) throws UnknownHostException, IOException {
		this.serverId = serverId;
		this.host = host;
		this.port = port;
		this.pushResponse = pushResponse;
		messages = new ArrayBlockingQueue<>(config.getServerConnectionQueueSize());
		socket = new Socket(host, port);
        socket.getOutputStream().write(clientId);
        socket.getOutputStream().write(serverId);
		logger = Logger.getLogger("ServerConnection"+serverId);
		logger.info("Hots:" + host + " port:" + port + " serever id: " + serverId);
	}

	@Override
	public void run() {
		ObjectOutputStream output = null;
		ObjectInputStream input = null;
		try {
			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());
			while (!die) {
				try {
					write(output, input);
				} catch (InterruptedException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (EOFException e) {
			// This is okay the connection ended.
		} catch (IOException e) {
			System.out.println("Connection Failed : " + this);
			System.out.println("Something went wrong: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
					output.close();
					input.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
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
	
	@Override
	public String toString() {
		return host + "::" + port;
	}

	private void write(ObjectOutputStream output, ObjectInputStream input)
			throws IOException, InterruptedException, ClassNotFoundException {
		Message message = null;
		while ((message = messages.poll()) != null) {
			if (message == Message.KILL) {
				die = true;
				return;
			}
			String messageTag = message.toString();
			StopWatch stopWatch = new Log4JStopWatch(messageTag, "SEND", logger);
			output.writeUnshared(message);
			stopWatch.stop();
			stopWatch.start(messageTag, "RECIEVE");
			Response response = (Response) input.readUnshared();
			stopWatch.stop();
			if (!response.isSuccess()) {
				System.err.println("something wrong");
			}
			pushResponse.push(response, serverId);
			output.reset();
		}
	}
}
