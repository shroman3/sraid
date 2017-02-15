package com.shroman.secureraid.client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

class ServerConnection implements Runnable {
	private String host;
	private int port;
	private byte[] chunk;

	ServerConnection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
			PrintStream ps = new PrintStream(socket.getOutputStream());
			while (true) {
				try {
					write(ps);
				} catch (InterruptedException e) {
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

	private synchronized void write(PrintStream ps) throws IOException, InterruptedException {
		if (chunk != null) {				
			ps.write(chunk);
			chunk = null;
		}
		wait();
	}
	
	public synchronized void setChunk(byte[] chunk) {
		this.chunk = chunk;
		notify();
	}

	@Override
	public String toString() {
		return host + "::" + port;
	}
}
