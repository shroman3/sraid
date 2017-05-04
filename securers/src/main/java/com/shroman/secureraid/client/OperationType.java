package com.shroman.secureraid.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public enum OperationType {
	WRITE("W") {

		@Override
		public void run(String[] operationArgs, Client client) throws IOException {
			client.encodeFile(operationArgs[1]);
		}
	},
	READ("R") {

		@Override
		public void run(String[] operationArgs, Client client) {
			client.readFile(operationArgs[1]);
		}
	};

	private static Map<String, OperationType> operationsMap = initializeMap();

	private String operationName;
	
	private OperationType(String operationName) {
		this.operationName = operationName.toUpperCase();
	}

	public static OperationType getOperationByName(String operationName) {
		OperationType operationType = operationsMap.get(operationName.toUpperCase());
		if (operationType == null) {
			throw new IllegalArgumentException("Illegal operation given:" + operationName);
		}
		return operationType;
	}
	
	private static Map<String, OperationType> initializeMap() {
		Map<String, OperationType> map = new HashMap<>();
		for (OperationType operationType : values()) {
			map.put(operationType.operationName, operationType);
		}
		return map;
	}

	public abstract void run(String[] operationArgs, Client client) throws IOException;
}
