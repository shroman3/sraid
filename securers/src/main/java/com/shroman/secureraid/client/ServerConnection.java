package com.shroman.secureraid.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.Response;

class ServerConnection implements Runnable {
	private String host;
	private int port;
	// private byte[] chunk;
	private Message message;
	private Response response;

	ServerConnection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			while (true) {
				try {
					write(output, input);
				} catch (InterruptedException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.out.println("Connection Failed : " + this);
			System.out.println("Something went wrong: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private synchronized void write(ObjectOutputStream output, ObjectInputStream input)
			throws IOException, InterruptedException, ClassNotFoundException {
		if (message != null) {
			output.writeObject(message);
			response = (Response) input.readObject();
			message = null;
		}
		wait();
	}

	public synchronized void setMessage(Message message) {
		this.message = message;
		notify();
	}
	
	public synchronized Response getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return host + "::" + port;
	}
}
