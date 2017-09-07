package com.shroman.secureraid.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NoRandom extends Random {

	private static final long serialVersionUID = 5979766988573925049L;

	private Map<Integer, byte[]> randomCache = new HashMap<>();

	public NoRandom() {
		super();
	}

	@Override
	public void nextBytes(byte[] bytes) {
		byte[] bs = randomCache.get(bytes.length);
		if (bs == null) {
			bs = fillRandom(bytes.length);
			randomCache.put(bs.length, bs);
		}
		System.arraycopy(bs, 0, bytes, 0, bs.length);
	}

	private byte[] fillRandom(int length) {
		byte[] bytes = new byte[length];
		super.nextBytes(bytes);
		return bytes;
	}
}
