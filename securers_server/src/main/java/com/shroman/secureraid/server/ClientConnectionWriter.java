package com.shroman.secureraid.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.common.CommonUtils;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;

public class ClientConnectionWriter extends Thread {
	private BlockingQueue<Response> responses;
	private Logger logger;
	private ObjectOutputStream output;
	private int serverId;
	
	ClientConnectionWriter(int serverId, ObjectOutputStream output, int queueSize) {
		this.serverId = serverId;
		this.logger = Logger.getLogger("ClientWriteStream");
		this.output = output;
		responses = CommonUtils.getBlockingQueue(queueSize);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Response response = responses.poll(Integer.MAX_VALUE,TimeUnit.SECONDS);
				if (response == null || response.getType() == ResponseType.KILL) {
					break;
				}
				StopWatch stopWatch = new Log4JStopWatch(Integer.toString(response.getChunkId()), response.getDataLength()+ "," + serverId, logger);
				try {
					output.writeUnshared(response);
					output.reset();
				} catch (IOException e) {
					e.printStackTrace();
					logger.error("1 Something went wrong: " + e.getMessage(), e);
				}
				stopWatch.stop();
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("2 Something went wrong: " + e.getMessage(), e);
			}
		}
	}

	public void addResponse(Response response) {
		while(true) {			
			try {
				responses.put(response);
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
