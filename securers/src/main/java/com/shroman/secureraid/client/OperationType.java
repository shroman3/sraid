package com.shroman.secureraid.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.shroman.secureraid.utils.XMLParsingException;

public enum OperationType {
	ENCODING("E", "ENCODE") {
		@Override
		void initOperation(WriteClient client) throws UnknownHostException, XMLParsingException, IOException {
			client.initEncoder();
		}
		
		@Override
		void run(String fileName, WriteClient client) throws IOException {
			client.encodeFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException, IOException {
			client.finalizeEncoder();
		}
	},
	WRITE("W", "WRITE") {
		@Override
		void initOperation(WriteClient client) throws UnknownHostException, XMLParsingException, IOException {
			client.initWriter();
		}
		
		@Override
		void run(String fileName, WriteClient client) throws IOException {
			client.encodeFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException, IOException {
			client.finalizeWriter();
		}
	},
	READ("R", "READ") {
		@Override
		void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException {
			client.initReader();
		}
		
		@Override
		void run(String fileName, WriteClient client) throws IOException {
			client.readFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException {
			client.finalizeReader();
		}
	},
	SERVER_FAILIURE("SF", "FAIL", "SF1", "FAIL1") {
		@Override
		void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException {
			client.initReader();
		}
		
		@Override
		public void run(String fileName, WriteClient client) throws IOException {
			client.serverFailureReadFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException {
			client.finalizeReader();
		}
	},
	SERVER_FAILURE_2("SF2", "FAIL2") {
		@Override
		void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException {
			client.initReader();			
		}

		@Override
		public void run(String fileName, WriteClient client) throws IOException {
			client.serverFailure2ReadFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException {
			client.finalizeReader();
		}
	},
	DEG_READ("DR", "DEG", "DEGREAD", "DR1", "DEG1", "DEGREAD1") {
		@Override
		void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException {
			client.initReader();			
		}

		@Override
		public void run(String fileName, WriteClient client) throws IOException {
			client.degReadFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException {
			client.finalizeReader();
		}
	},
	DEG_READ2("DR2", "DEG2", "DEGREAD2") {
		@Override
		void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException {
			client.initReader();			
		}

		@Override
		public void run(String fileName, WriteClient client) throws IOException {
			client.deg2ReadFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException {
			client.finalizeReader();
		}
	},
	DEG_RAND_CHUNK_READ("RCR", "RC", "RR") {
		@Override
		void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException {
			client.initRandomChunkReader();
		}

		@Override
		public void run(String fileName, WriteClient client) throws IOException {
			client.serverFailure2ReadFile(fileName);
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException {
			client.finalizeReader();
		}
	},
	CLEAN("C", "CLEAN") {
		@Override
		public void run(Scanner inputFileScanner, WriteClient client) throws IOException, XMLParsingException, InterruptedException {
			initOperation(client);
			client.clean();
			finalizeOperation(client);
		}

		@Override
		void initOperation(WriteClient client) throws UnknownHostException, XMLParsingException, IOException {
			client.initWriter();
		}
		
		@Override
		public void run(String fileName, WriteClient client) throws IOException {
		}
		
		@Override
		void finalizeOperation(WriteClient client) throws InterruptedException, IOException {
			client.finalizeWriter();
		}
	};
	

	private static Map<String, OperationType> operationsMap = buildOperationsNameMap();

	private String[] operationNames;
	
	private OperationType(String ... operationName) {
		this.operationNames = operationName;
	}

	public static OperationType getOperationByName(String operationName) {
		OperationType operationType = operationsMap.get(operationName.toUpperCase());
		if (operationType == null) {
			throw new IllegalArgumentException("Illegal operation given:" + operationName);
		}
		return operationType;
	}
	
	private static Map<String, OperationType> buildOperationsNameMap() {
		Map<String, OperationType> map = new HashMap<>();
		for (OperationType operationType : values()) {
			for (String name : operationType.operationNames) {
				map.put(name, operationType);
			}
		}
		return map;
	}

	void run(Scanner inputFileScanner, WriteClient client) throws IOException, InterruptedException, ClassNotFoundException, XMLParsingException {
		initOperation(client);
		while (inputFileScanner.hasNextLine()) {
			String operationLine = inputFileScanner.nextLine().toLowerCase();
			run(operationLine, client);
		}
		finalizeOperation(client);
	}
	
	abstract void run(String fileName, WriteClient client) throws IOException;
	abstract void initOperation(WriteClient client) throws ClassNotFoundException, IOException, XMLParsingException;
	abstract void finalizeOperation(WriteClient client) throws InterruptedException, IOException;
}
