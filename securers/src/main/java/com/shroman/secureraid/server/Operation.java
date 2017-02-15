package com.shroman.secureraid.server;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;

public abstract class Operation {
	private static Map<MessageType, Operation> operationsMap;

	protected abstract MessageType getMessageType();
	protected abstract Response execute(int id, Message message) throws IOException;
	
	public static Response executeOperation(int id, Message message) throws IOException {
		Operation operation = operationsMap.get(message.getType());
		return operation.execute(id, message);
	}
	
	public static void initOperations() {
		if (operationsMap == null) {			
			Reflections reflections = new Reflections(Operation.class.getPackage().getName());
			Set<Class<? extends Operation>> subTypes = reflections.getSubTypesOf(Operation.class);
			
			Map<MessageType, Operation> operations = new HashMap<MessageType, Operation>();
			for (Class<? extends Operation> clazz : subTypes) {
				if (!Modifier.isAbstract(clazz.getModifiers())) {
					try {
						Operation operation = clazz.newInstance();
						MessageType type = operation.getMessageType();
						operations.put(type, operation);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
			validate(operations);
			operationsMap = operations;
		}
	}
	
	private static void validate(Map<MessageType, Operation> operations) {
		for (MessageType messageType : MessageType.values()) {
			if(!operations.containsKey(messageType)) {
				throw new RuntimeException("No operation message type: " + messageType);
			}
		}
	}
}
