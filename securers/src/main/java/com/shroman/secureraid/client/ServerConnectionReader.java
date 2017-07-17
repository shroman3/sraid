package com.shroman.secureraid.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.common.Response;

class ServerConnectionReader extends Thread {
	private int serverId;
	private PushResponseInterface pushResponse;
	private Logger logger;
	private boolean die = false;
	private ObjectInputStream input = null;

	ServerConnectionReader(int serverId, PushResponseInterface pushResponse, ObjectInputStream input) throws UnknownHostException, IOException {
		this.serverId = serverId;
		this.pushResponse = pushResponse;
		this.input = input;
		logger = Logger.getLogger("ServerReadStream");
	}

	@Override
	public void run() {
		try {
			while (!die) {
				try {
					read(input);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (EOFException e) {
			// This is okay the connection ended.
		} catch (IOException e) {
			System.out.println("Connection Failed : " + this);
			System.out.println("Something went wrong: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void read(ObjectInputStream input) throws IOException, ClassNotFoundException {
		while (true) {
			StopWatch stopWatch = new Log4JStopWatch(logger);
			Object object = input.readUnshared();
			Response response = (Response) object;
			stopWatch.stop(Integer.toString(response.getChunkId()), response.getDataLength() + "," + serverId);
			
			if (!response.isSuccess()) {
				System.err.println("something wrong");
			}
			pushResponse.push(response, serverId);
		}
	}
}
