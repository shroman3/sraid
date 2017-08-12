package com.shroman.secureraid.client;

import java.util.HashMap;
import java.util.Map;

import com.shroman.secureraid.codec.AESCodec;
import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.codec.NoCodec;
import com.shroman.secureraid.codec.PackedSecretSharingCodec;
import com.shroman.secureraid.codec.RC4Codec;
import com.shroman.secureraid.codec.SecretSharingCodec;
import com.shroman.secureraid.codec.SecureBackblazeRS;
import com.shroman.secureraid.codec.SecureEvenodd;
import com.shroman.secureraid.codec.SecureJerasureRS;
import com.shroman.secureraid.utils.Utils;

public enum CodecType {
	NONE("NONE", "NOP", "NO") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			NoCodec.Builder builder = new NoCodec.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(0);
			return builder.build();
		}
	},
	SECURE_BACKBLAZE_RS("BB", "BBRS") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			SecureBackblazeRS.Builder builder = new SecureBackblazeRS.Builder();

			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandom(randomName, randomKey)).build();
		}
	},
	SECURE_JRS("JRS", "JERASURE", "SECURE_JERASURE_RS") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			SecureJerasureRS.Builder builder = new SecureJerasureRS.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandom(randomName, randomKey)).build();
		}
	},
	SECURE_EVENODD("EVENODD", "EO", "ED") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			SecureEvenodd.Builder builder = new SecureEvenodd.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandom(randomName, randomKey)).build();
		}
	},
	AES_RS("AES", "AESRS", "AES_RS") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			AESCodec.Builder builder = new AESCodec.Builder();
			builder.setKey(randomKey).setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	RC4_RS("RC4", "RC4RS", "RC4_RS") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			RC4Codec.Builder builder = new RC4Codec.Builder();
			builder.setKey(randomKey).setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	SHAMIR_SECRET_SHARING("SSS", "SHAMIR") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			SecretSharingCodec.Builder builder = new SecretSharingCodec.Builder();

			if (k > 2) {
				throw new UnsupportedOperationException();
			}

			builder.setRandom(RandomType.getRandom(randomName, randomKey)).setSecrecyShardsNum(z).setDataShardsNum(k)
					.setParityShardsNum(r);
			return builder.build();
		}
	},
	PACKED_SECRET_SHARING("PSS", "PACKED") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey) {
			PackedSecretSharingCodec.Builder builder = new PackedSecretSharingCodec.Builder();
			builder.setRandom(RandomType.getRandom(randomName, randomKey)).setSecrecyShardsNum(z).setDataShardsNum(k)
					.setParityShardsNum(r);
			return builder.build();
		}
	};
	
	private String[] codecNames;

	private static Map<String, CodecType> namesMap;

	private CodecType(String... codecNames) {
		Utils.validateArrayNotEmpty(codecNames, "Codec names array");
		this.codecNames = codecNames;
	}

	static {
		init();
	}

	public static Codec getCodecFromArgs(String codecName, int k, int r, int z, String randomName, String randomKey) {
		Utils.validateNotNull(codecName, "codec name");
		Utils.validateNotNull(randomKey, "random key");
		CodecType codecType = namesMap.get(codecName.toUpperCase());
		return codecType.buildCodecFromArgs(k, r, z, randomName, randomKey);
	}

	abstract Codec buildCodecFromArgs(int k, int r, int z, String randomName, String randomKey);

	private static void init() {
		Map<String, CodecType> names = new HashMap<>();
		for (CodecType codecType : values()) {
			for (String name : codecType.codecNames) {
				names.put(name.toUpperCase(), codecType);
			}
		}
		namesMap = names;
	}

}
