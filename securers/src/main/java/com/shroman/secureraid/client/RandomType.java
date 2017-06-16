package com.shroman.secureraid.client;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.shroman.secureraid.utils.Utils;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

public enum RandomType {
	SECURE("SECURE") {
		@Override
		Random buildRandom(String randomKey) {
			return new SecureRandom(randomKey.getBytes());
		}
	},
	XOROSHIRO("XOR", "OSHIRO", "XOROSHIRO") {
		@Override
		Random buildRandom(String randomKey) {
			BigInteger seed = new BigInteger(randomKey.getBytes());
			return new XoRoShiRo128PlusRandom(seed.longValue());
		}
	},
	SIMPLE("SIMPLE") {
		@Override
		Random buildRandom(String randomKey) {
			BigInteger seed = new BigInteger(randomKey.getBytes());
			return new Random(seed.longValue());
		}
	};

	private String[] randomNames;

	private static Map<String, RandomType> namesMap;

	private RandomType(String... codecNames) {
		Utils.validateArrayNotEmpty(codecNames, "Codec names array");
		this.randomNames = codecNames;
	}

	static {
		init();
	}

	public static Random getRandom(String randomName, String randomKey) {
		Utils.validateNotNull(randomName, "codec name");
		Utils.validateNotNull(randomKey, "random key");
		RandomType randomType = namesMap.get(randomName.toUpperCase());
		return randomType.buildRandom(randomKey);
	}

	abstract Random buildRandom(String randomKey);

	private static void init() {
		Map<String, RandomType> names = new HashMap<>();
		for (RandomType randomType : values()) {
			for (String name : randomType.randomNames) {
				names.put(name.toUpperCase(), randomType);
			}
		}
		namesMap = names;
	}

}
