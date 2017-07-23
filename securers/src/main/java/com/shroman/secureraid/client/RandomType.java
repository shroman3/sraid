package com.shroman.secureraid.client;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.uncommons.maths.random.AESCounterRNG;

import com.shroman.secureraid.utils.Utils;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

public enum RandomType {
	AES("AES") {
		@Override
		public Random buildRandom(String randomKey) {
			try {
				Digest digest = new MD5Digest();
				digest.update(randomKey.getBytes(), 0, randomKey.getBytes().length);
				byte[] key = new byte[digest.getDigestSize()];
				digest.doFinal(key, 0);
				AESCounterRNG aesRandom = new AESCounterRNG(key);
				return aesRandom;
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				System.out.println("Problem creating AESPRNG");
				return new SecureRandom(randomKey.getBytes());
			}
		}
	},
	SECURE("SECURE", "DEV_RANDOM", "DEV_URANDOM") {
		@Override
		public Random buildRandom(String randomKey) {
			return new SecureRandom(randomKey.getBytes());
		}
	},
	SHA1("SHA", "SHA1") {
		@Override
		public Random buildRandom(String randomKey) {
			try {
				SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
				secureRandom.setSeed(randomKey.getBytes());
				return secureRandom;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.out.println("Problem creating SHA1PRNG");
				return new SecureRandom(randomKey.getBytes());
			}
		}
	},
	XOROSHIRO("XOR", "OSHIRO", "XOROSHIRO") {
		@Override
		public Random buildRandom(String randomKey) {
			BigInteger seed = new BigInteger(randomKey.getBytes());
			return new XoRoShiRo128PlusRandom(seed.longValue());
		}
	},
	SIMPLE("SIMPLE") {
		@Override
		public Random buildRandom(String randomKey) {
			BigInteger seed = new BigInteger(randomKey.getBytes());
			return new Random(seed.longValue());
		}
	},
	NONE("NO", "NONE") {
		@Override
		public Random buildRandom(String randomKey) {
			BigInteger seed = new BigInteger(randomKey.getBytes());
			return new NoRandom(seed.longValue());
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

	abstract public Random buildRandom(String randomKey);

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
