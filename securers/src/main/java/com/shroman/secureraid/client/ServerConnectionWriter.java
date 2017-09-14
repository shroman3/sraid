package com.shroman.secureraid.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.common.CommonUtils;
import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;

class ServerConnectionWriter extends Thread implements Connection {
	private String host;
	private int port;
	private BlockingQueue<Message> messages;
	private Socket socket;
	private int serverId;
	private PushResponseInterface pushResponse;
	private Logger logger;
	private boolean die = false;
	private CountDownLatch barrier;

	ServerConnectionWriter(CountDownLatch barrier, int serverId, int clientId, String host, int port, PushResponseInterface pushResponse, Config config) throws UnknownHostException, IOException, NoSuchAlgorithmException {
		this.barrier = barrier;
		this.serverId = serverId;
		this.host = host;
		this.port = port;
		this.pushResponse = pushResponse;
		int queueSize = config.getServerConnectionQueueSize();
		messages = CommonUtils.getBlockingQueue(queueSize);
//		socket = new Socket(host, port);
		socket = SSLSocketFactory.getDefault().createSocket(host, port);
		socket.getOutputStream().write(clientId);
		socket.getOutputStream().write(queueSize);
        socket.getOutputStream().write(serverId);
		logger = Logger.getLogger("ServerWriteStream");
	}

	@Override
	public void run() {
		ObjectOutputStream output = null;
		ObjectInputStream input = null;
		try {
			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());
			ServerConnectionReader reader = new ServerConnectionReader(barrier, serverId, pushResponse, input);
			reader.start();
			barrier.countDown();
			while (!die) {
				try {
					write(output);
				} catch (InterruptedException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			reader.join();
		} catch (EOFException e) {
			// This is okay the connection ended.
		} catch (InterruptedException | IOException e) {
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

	private void write(ObjectOutputStream output) throws IOException, InterruptedException, ClassNotFoundException {
		while (true) {
			try {
				Message message = messages.take();
				if (message == null || message.getType() == MessageType.KILL) {
					die = true;
					output.writeUnshared(message);
					return;
				}
				StopWatch stopWatch = new Log4JStopWatch(Integer.toString(message.getChunkId()), message.getDataLength() + "," + serverId, logger);
				output.writeUnshared(message);
				stopWatch.stop();
				output.reset();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
