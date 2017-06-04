package com.shroman.secureraid.client;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.shroman.secureraid.codec.AESCodec;
import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.codec.NoCodec;
import com.shroman.secureraid.codec.RC4Codec;
import com.shroman.secureraid.codec.SecretSharingCodec;
import com.shroman.secureraid.codec.SecureBackblazeRS;
import com.shroman.secureraid.utils.Utils;

public enum CodecType {
	NONE("NONE", "NOP", "NO") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 1, "Arguments");
			NoCodec.Builder builder = new NoCodec.Builder();
			builder.setDataShardsNum(Integer.parseInt(args[0])).setParityShardsNum(0);
			return builder.build();
		}
	},
	SECURE_BACKBLAZE_RS("BB", "BBRS", "BACKBLAZE", "SECURE_BACKBLAZE_RS") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 4, "Arguments");
			SecureBackblazeRS.Builder builder = new SecureBackblazeRS.Builder();

			builder.setDataShardsNum(Integer.parseInt(args[0])).setParityShardsNum(Integer.parseInt(args[1]));
			return builder.setSecrecyShardsNum(Integer.parseInt(args[2]))
					.setRandom(new SecureRandom(args[3].getBytes())).build();
		}
	},
	SECURE_JRS("JRS", "JERASURE", "SECURE_JERASURE_RS") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			throw new UnsupportedOperationException();
		}
	},
	SECURE_EVENODD("EVENODD", "EO", "ED") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			throw new UnsupportedOperationException();
		}
	},
	AES("AES256", "AES") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 2, "Arguments");
			AESCodec.Builder builder = new AESCodec.Builder();
			builder.setKey(args[1]).setDataShardsNum(Integer.parseInt(args[0])).setParityShardsNum(0);
			return builder.build();
		}
	},
	AES_RS("AES256RS", "AESRS", "AES_RS") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 3, "Arguments");
			AESCodec.Builder builder = new AESCodec.Builder();
			builder.setKey(args[2]).setDataShardsNum(Integer.parseInt(args[0]))
					.setParityShardsNum(Integer.parseInt(args[1]));
			return builder.build();
		}
	},
	RC4("RC4") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 2, "Arguments");
			RC4Codec.Builder builder = new RC4Codec.Builder();
			builder.setKey(args[1]).setDataShardsNum(Integer.parseInt(args[0])).setParityShardsNum(0);
			return builder.build();
		}
	},
	RC4_RS("RC4RS", "RC4_RS") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 3, "Arguments");
			RC4Codec.Builder builder = new RC4Codec.Builder();
			builder.setKey(args[2]).setDataShardsNum(Integer.parseInt(args[0]))
					.setParityShardsNum(Integer.parseInt(args[1]));
			return builder.build();
		}
	},
	SECRET_SHARING("SSS", "SS", "SECRETS", "SSHARING") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 4, "Arguments");
			SecretSharingCodec.Builder builder = new SecretSharingCodec.Builder();

			int dataShardsNum = Integer.parseInt(args[0]);
			if (dataShardsNum > 2) {
				throw new UnsupportedOperationException();				
			}
			
			builder.setRandom(new SecureRandom(args[3].getBytes())).setSecrecyShardsNum(Integer.parseInt(args[2]))
					.setDataShardsNum(dataShardsNum).setParityShardsNum(Integer.parseInt(args[1]));
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

	public static Codec getCodecFromArgs(String[] args) {
		Utils.validateArrayNotEmpty(args, "Arguments");
		CodecType codecType = namesMap.get(args[0].toUpperCase());
		return codecType.buildCodecFromArgs(Arrays.copyOfRange(args, 1, args.length));
	}

	abstract Codec buildCodecFromArgs(String[] args);

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
