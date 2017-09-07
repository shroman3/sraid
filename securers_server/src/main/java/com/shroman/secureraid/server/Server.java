package com.shroman.secureraid.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;

public class Server {
	private static Map<Integer, ClientConnectionReader> clientsMap = new HashMap<Integer, ClientConnectionReader>();
	public static void main(String[] args) {
		System.setProperty("javax.net.ssl.keyStore", "sraid.store");
		System.setProperty("javax.net.ssl.keyStorePassword", "secureraid");
		int serverId = Integer.parseInt(args[0]);
		Path serverPath = Paths.get(args[0]);
		
		int port = Integer.parseInt(args[1]);
	    
	    ServerSocket connectionSocket = null;
	    try {
	    	Operation.initOperations();
	    	Files.createDirectories(Paths.get(serverId + ""));
//	    	connectionSocket = new ServerSocket(port);
	    	connectionSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);

	        while(true){
	            Socket socket = connectionSocket.accept();
	            ClientConnectionReader clientConnection = new ClientConnectionReader(serverPath, socket);
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
