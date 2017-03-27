package com.shroman.secureraid.client;

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

	ServerConnection(int clientId, String host, int port) throws UnknownHostException, IOException {
		this.host = host;
		this.port = port;
		socket = new Socket(host, port);
        socket.getOutputStream().write(clientId);
	}

	@Override
	public void run() {
		try {
			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			while (true) {
				try {
					write(output, input);
//					synchronized(messages) {						
//						messages.wait();
//					}
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

	private void write(ObjectOutputStream output, ObjectInputStream input)
			throws IOException, InterruptedException, ClassNotFoundException {
		Message message = null;
		while ((message = messages.poll()) != null) {
			output.writeObject(message);
			responses.add((Response) input.readObject());
		}
	}

	public void addMessage(Message message) {
		messages.add(message);
//		synchronized (messages) {			
//			notify();
//		}
	}
	
	public Response getResponse() {
		return responses.poll();
	}

	@Override
	public String toString() {
		return host + "::" + port;
	}
}
