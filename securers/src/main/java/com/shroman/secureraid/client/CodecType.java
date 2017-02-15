package com.shroman.secureraid.client;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.codec.NoCodec;
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
	SECURE_BACKBLAZE_RS("BB", "BBRS", "BACKBLAZE", "SECURE BACKBLAZE RS") {
		@Override
		Codec buildCodecFromArgs(String[] args) {
			Utils.validateArraySize(args, 3, "Arguments");
			SecureBackblazeRS.Builder builder = new SecureBackblazeRS.Builder();

			builder.setDataShardsNum(Integer.parseInt(args[0])).setParityShardsNum(Integer.parseInt(args[1]));
			return builder.setSecrecyShardsNum(Integer.parseInt(args[2])).setRandom(new SecureRandom()).build();
		}
	},
	SECURE_JRS("JRS", "JERASURE", "SECURE JERASURE RS") {
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
	},;
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
		for (CodecType codecType : values()) {
			for (String name : codecType.codecNames) {
				namesMap.put(name.toUpperCase(), codecType);
			}
		}
	}

}
