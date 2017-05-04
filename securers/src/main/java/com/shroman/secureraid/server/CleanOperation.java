package com.shroman.secureraid.server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;

public class CleanOperation extends Operation {

	@Override
	protected MessageType getMessageType() {
		return MessageType.CLEAN;
	}

	@Override
	protected Response execute(Path executionPath, Message message) throws IOException {
        Files.walkFileTree(executionPath, new SimpleFileVisitor<Path>() {
        	@Override
        	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        		Files.delete(file);
        		return FileVisitResult.CONTINUE;
        	}
        	
        	@Override
        	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        		Files.delete(dir);
        		return FileVisitResult.CONTINUE;
        	}
		});
        
		return null;
	}

}
