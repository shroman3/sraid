/*******************************************************************************
 * SSDPlayer Visualization Platform (Version 1.0)
 * Authors: Or Mauda, Roman Shor, Gala Yadgar, Eitan Yaakobi, Assaf Schuster
 * Copyright (c) 2015, Technion � Israel Institute of Technology
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *******************************************************************************/
package com.shroman.secureraid.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {
	public static void validateNotNull(Object param, String paramName) {
		if (param == null) {
			throw new IllegalArgumentException(paramName + " parameter is null (It shouldn't be)");
		}
	}

	public static void validateNotNegative(int param, String paramName) {
		if (param < 0) {
			throw new IllegalArgumentException(paramName + " parameter is negative (it shouldn't be)");
		}
	}

	public static void validatePositive(int param, String paramName) {
		if (param < 0) {
			throw new IllegalArgumentException(paramName + " parameter isn't positive (it should be)");
		}
	}

	public static void validateNotNegative(double param, String paramName) {
		if (param < 0) {
			throw new IllegalArgumentException(paramName + " parameter is negative (It shouldn't be)");
		}
	}

	public static void validateInteger(double param, String paramName) {
		if (param != (int) param) {
			throw new IllegalArgumentException(paramName + " parameter is not an Integer (It should be)");
		}
	}

	public static void validateArrayNotEmpty(Object[] objects, String paramName) {
		if (objects.length <= 0) {
			throw new IllegalArgumentException(paramName + " should have at least one item");
		}
	}

	public static void validateArraySize(String[] objects, int size, String paramName) {
		if (objects.length != size) {
			throw new IllegalArgumentException(paramName + " should have exactly " + size + " items");
		}
	}

	public static ExecutorService buildExecutor(int threadsNum, int maxTasksNum) {
		RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				int i = 0;
				while (true) {
					try {
						if (!executor.isShutdown()) {
							System.out.println("REJECTED " + i);
							executor.getQueue().put(r);
							return;
						}
					} catch (InterruptedException e) {
					}
				}
			}
		};
		return new ThreadPoolExecutor(threadsNum, threadsNum, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(maxTasksNum), rejectedExecutionHandler);
	}

	public static String buildLogMessage(Long timestamp, int stripeId, String message) {
		StringBuilder sb = new StringBuilder();
		sb.append("start[").append(timestamp).append("] time[").append(System.currentTimeMillis() - timestamp)
				.append("] tag[").append(stripeId).append("] message[").append(message).append(']');
		return sb.toString();
	}
}
