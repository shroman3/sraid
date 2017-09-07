package com.shroman.secureraid.common;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CommonUtils {
	public static <T> BlockingQueue<T> getBlockingQueue(int queueSize) {
		if (queueSize == 0) {
			return new LinkedBlockingQueue<T>();
		}
		return new ArrayBlockingQueue<T>(queueSize);
	}
}
