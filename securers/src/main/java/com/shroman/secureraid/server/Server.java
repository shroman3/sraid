package com.shroman.secureraid.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Server {
	private static Map<Integer, ClientConnection> clientsMap = new HashMap<Integer, ClientConnection>();
	public static void main(String[] args) {
		int serverId = Integer.parseInt(args[0]);
		Path serverPath = Paths.get(args[0]);
		
		int port = Integer.parseInt(args[1]);
	    
	    ServerSocket connectionSocket = null;
	    try {
	    	Operation.initOperations();
	    	Files.createDirectories(Paths.get(serverId + ""));
	    	connectionSocket = new ServerSocket(port);
	        while(true){
	            Socket socket = connectionSocket.accept();
	            ClientConnection clientConnection = new ClientConnection(serverPath, socket);
	            clientsMap.put(clientConnection.getClientId(), clientConnection);
	            clientConnection.start();
	        }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
			if (connectionSocket != null) {
				try {
					connectionSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
