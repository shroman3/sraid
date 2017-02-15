package com.shroman.secureraid.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
	private static Map<Integer, ClientConnection> clientsMap = new HashMap<Integer, ClientConnection>();
	public static void main(String[] args) {
	    int port = Integer.parseInt(args[0]);
	    
	    ServerSocket connectionSocket = null;
	    try {
	    	connectionSocket = new ServerSocket(port);
	        while(true){
	            Socket socket = connectionSocket.accept();
	            ClientConnection clientConnection = new ClientConnection(socket);
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
