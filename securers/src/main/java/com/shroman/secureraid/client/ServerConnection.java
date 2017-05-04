package com.shroman.secureraid.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.Response;

class ServerConnection extends Thread {
	private String host;
	private int port;
	private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<>();
	private Socket socket;
	private int serverId;
	private PushResponseInterface pushResponse;

	ServerConnection(int serverId, int clientId, String host, int port, PushResponseInterface pushResponse) throws UnknownHostException, IOException {
		this.serverId = serverId;
		this.host = host;
		this.port = port;
		this.pushResponse = pushResponse;
		socket = new Socket(host, port);
        socket.getOutputStream().write(clientId);
	}

	@Override
	public void run() {
		ObjectOutputStream output = null;
		ObjectInputStream input = null;
		try {
			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());
			while (true) {
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

	private void write(ObjectOutputStream output, ObjectInputStream input)
			throws IOException, InterruptedException, ClassNotFoundException {
		Message message = null;
		while ((message = messages.poll()) != null) {
			output.writeObject(message);
			Object readObject = input.readObject();
			Response response = (Response) readObject;
			responses.add(response);
			pushResponse.push(response, serverId);
		}
	}

	public void addMessage(Message message) {
		messages.add(message);
	}
	
	@Override
	public String toString() {
		return host + "::" + port;
	}
}
