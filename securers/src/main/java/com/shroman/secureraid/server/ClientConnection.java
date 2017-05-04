package com.shroman.secureraid.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class ClientConnection extends Thread {
	private Socket socket;
	private int id;
	private Path clientPath;

	public ClientConnection(Path serverPath, Socket socket) throws IOException {
		this.socket = socket;
        id = socket.getInputStream().read();
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
				try {
					message = (Message) input.readObject();
					response = Operation.executeOperation(clientPath, message);
					if (response == null) {
						break;
					}
				} catch (IOException e) {
					if (message != null) {						
						response = new Response(ResponseType.ERROR, e.getMessage().getBytes(), message.getObjectId(), message.getChunkId());
					} else {
						response = new Response(ResponseType.ERROR, e.getMessage().getBytes(), -1, -1);
					}
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
