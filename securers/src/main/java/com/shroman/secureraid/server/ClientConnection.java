package com.shroman.secureraid.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class ClientConnection extends Thread {

	private Socket socket;
	private int id;

	public ClientConnection(Socket socket) throws IOException {
		this.socket = socket;
        id = socket.getInputStream().read();
	}

	@Override
	public void run() {
		try {
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			while (true) {
				Message message = (Message) input.readObject();
				Response response;
				try {
					response = Operation.executeOperation(id, message);
				} catch (IOException e) {
					response = new Response(ResponseType.ERROR, e.getMessage().getBytes());
					e.printStackTrace();
				}
				output.writeObject(response);
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
