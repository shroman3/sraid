package com.shroman.secureraid.client;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.uncommons.maths.random.AESCounterRNG;

import com.shroman.secureraid.codec.SecureCodec;
import com.shroman.secureraid.utils.Utils;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import net.nullschool.util.DigitalRandom;

public enum RandomType implements SecureCodec.RandomGetter {
	AES("AESJ") {
		private final MD5Digest MD5_DIGEST = new MD5Digest();
		@Override
		public Random getRandom() {
			try {
				byte[] key = new byte[MD5_DIGEST.getDigestSize()];
				trueRandom.nextBytes(key);
				AESCounterRNG aesRandom = new AESCounterRNG(key);
				return aesRandom;
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				System.out.println("Problem creating AESPRNG");
				return new SecureRandom();
			}
		}
	},
	INTEL("INTEL","RDRAND") {
		@Override
		public Random getRandom() {
			return new DigitalRandom(); 
		}
	},
	MY_AES("AES") {
		@Override
		public Random getRandom() {
			byte[] key = new byte[32];
			byte[] iv = new byte[16];
			trueRandom.nextBytes(key);
			AESRandom aesRandom = new AESRandom(key, iv);
			return aesRandom;
		}
	},
//	BC_DEFAULT("BCD") {
//		@Override
//		public Random getRandom() {
//			try {
//				SecureRandom secureRandom = SecureRandom.getInstance("DEFAULT");
//				return secureRandom;
//			} catch (NoSuchAlgorithmException e) {
//				e.printStackTrace();
//				System.out.println("Problem creating NativePRNGBlocking");
//				return new SecureRandom();
//			}
//		}
//		
//	},
//	BC_NONCEANDIV("BCN") {
//		@Override
//		public Random getRandom() {
//			try {
//				SecureRandom secureRandom = SecureRandom.getInstance("NONCEANDIV");
//				return secureRandom;
//			} catch (NoSuchAlgorithmException e) {
//				e.printStackTrace();
//				System.out.println("Problem creating NativePRNGBlocking");
//				return new SecureRandom();
//			}
//		}
//		
//	},
	DEV_URANDOM("DEV_URANDOM", "URANDOM") {
		@Override
		public Random getRandom() {
			try {
				SecureRandom secureRandom = SecureRandom.getInstance("NativePRNGNonBlocking");
				secureRandom.setSeed(trueRandom.nextLong());
				return secureRandom;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.out.println("Problem creating NativePRNGNonBlocking");
				return new SecureRandom();
			}		
		}

	},
	SHA1("SHA", "SHA1") {
		@Override
		public Random getRandom() {
			try {
				SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
				secureRandom.setSeed(trueRandom.nextLong());
				return secureRandom;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.out.println("Problem creating SHA1PRNG");
				return new SecureRandom();
			}
		}
	},
	XOROSHIRO("XOR", "OSHIRO", "XOROSHIRO") {
		@Override
		public Random getRandom() {
			return new XoRoShiRo128PlusRandom(trueRandom.nextLong());
		}
	},
	SIMPLE("SIMPLE") {
		@Override
		public Random getRandom() {
			return new Random(trueRandom.nextLong());
		}
	},
	NONE("NO", "NONE") {
		private NoRandom random = new NoRandom();

		@Override
		public Random getRandom() {
			return random;
		}
	},
//	DEV_RANDOM("DEV_RANDOM", "RANDOM") {
//		@Override
//		public Random getRandom() {
//			try {
//				SecureRandom secureRandom = SecureRandom.getInstance("NativePRNGBlocking");
//				return secureRandom;
//			} catch (NoSuchAlgorithmException e) {
//				e.printStackTrace();
//				System.out.println("Problem creating NativePRNGBlocking");
//				return new SecureRandom();
//			}
//		}
//	}
	;


	private String[] randomNames;

	private static Map<String, RandomType> namesMap;
	private static SecureRandom trueRandom;


	private RandomType(String... codecNames) {
		Utils.validateArrayNotEmpty(codecNames, "Codec names array");
		this.randomNames = codecNames;
	}

	static {
		init();
	}

	public static RandomType getRandomType(String randomName) {
		Utils.validateNotNull(randomName, "codec name");
		RandomType randomType = namesMap.get(randomName.toUpperCase());
		return randomType;
	}

	private static void init() {
		trueRandom = Utils.createTrueRandom();
		Map<String, RandomType> names = new HashMap<>();
		for (RandomType randomType : values()) {
			for (String name : randomType.randomNames) {
				names.put(name.toUpperCase(), randomType);
			}
		}
		namesMap = names;
	}

}
