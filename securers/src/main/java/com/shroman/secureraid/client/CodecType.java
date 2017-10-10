package com.shroman.secureraid.client;

import java.util.HashMap;
import java.util.Map;

import com.shroman.secureraid.codec.AESJavaCodec;
import com.shroman.secureraid.codec.AESJavaCodecRA;
import com.shroman.secureraid.codec.AONTAESJava;
import com.shroman.secureraid.codec.AONTAESJavaRA;
import com.shroman.secureraid.codec.AONTChaCha;
import com.shroman.secureraid.codec.AONTChaChaRA;
import com.shroman.secureraid.codec.ChaChaCodec;
import com.shroman.secureraid.codec.ChaChaRandomAccessCodec;
import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.codec.NoCodec;
import com.shroman.secureraid.codec.NoCodecRandomAccess;
import com.shroman.secureraid.codec.PackedSecretSharingCodec;
import com.shroman.secureraid.codec.PackedSecretSharingCodecRA;
import com.shroman.secureraid.codec.SecretSharingCodec;
import com.shroman.secureraid.codec.SecureBackblazeRS;
import com.shroman.secureraid.codec.SecureBackblazeRSRA;
import com.shroman.secureraid.codec.SecureEvenodd;
import com.shroman.secureraid.codec.SecureEvenoddRA;
import com.shroman.secureraid.utils.Utils;

public enum CodecType {
	NONE("NONE") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			NoCodec.Builder builder = new NoCodec.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	NONE_RA("NONE_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			NoCodecRandomAccess.Builder builder = new NoCodecRandomAccess.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	SECURE_RS_RA("BB_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			SecureBackblazeRSRA.Builder builder = new SecureBackblazeRSRA.Builder();

			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandomType(randomName)).build();
		}
	},
	SECURE_RS("BB") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			SecureBackblazeRS.Builder builder = new SecureBackblazeRS.Builder();

			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandomType(randomName)).build();
		}
	},
	AES_JCE_RA("AES_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AESJavaCodecRA.Builder builder = new AESJavaCodecRA.Builder();
			builder.setProvider("SunJCE").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AES_JCE("AES") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AESJavaCodec.Builder builder = new AESJavaCodec.Builder();
			builder.setProvider("SunJCE").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AONT_AES_RA("AONT_AES_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AONTAESJavaRA.Builder builder = new AONTAESJavaRA.Builder();
			builder.setProvider("SunJCE").setKey("no_need").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AONT_AES("AONT_AES") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AONTAESJava.Builder builder = new AONTAESJava.Builder();
			builder.setProvider("SunJCE").setKey("no_need").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	CHA_RA("CHA_RA", "CHACHA_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			ChaChaRandomAccessCodec.Builder builder = new ChaChaRandomAccessCodec.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	CHACHA("CHA", "CHACHA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			ChaChaCodec.Builder builder = new ChaChaCodec.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AONT_CHA_RA("AONT_CHA_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AONTChaChaRA.Builder builder = new AONTChaChaRA.Builder();
			builder.setKey("no_need").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AONT_CHA("AONT_CHA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AONTChaCha.Builder builder = new AONTChaCha.Builder();
			builder.setKey("no_need").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AES_BC_RA("AES_BC_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AESJavaCodecRA.Builder builder = new AESJavaCodecRA.Builder();
			builder.setProvider("BC").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AES_BC("AES_BC") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AESJavaCodec.Builder builder = new AESJavaCodec.Builder();
			builder.setProvider("BC").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AONT_AES_BC_RA("AONT_AES_BC_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AONTAESJavaRA.Builder builder = new AONTAESJavaRA.Builder();
			builder.setProvider("BC").setKey("no_need").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	AONT_AES_BC("AONT_AES_BC") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			AONTAESJava.Builder builder = new AONTAESJava.Builder();
			builder.setProvider("BC").setKey("no_need").setDataShardsNum(k).setParityShardsNum(r);
			return builder.build();
		}
	},
	SHAMIR_SECRET_SHARING("SSS", "SSS_RA", "SHAMIR", "SHAMIR_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			SecretSharingCodec.Builder builder = new SecretSharingCodec.Builder();

			if (k > 1) {
				throw new UnsupportedOperationException();
			}

			builder.setRandom(RandomType.getRandomType(randomName)).setSecrecyShardsNum(z).setDataShardsNum(k)
					.setParityShardsNum(r);
			return builder.build();
		}
	},
	PACKED_SECRET_SHARING_RA("PSS_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			PackedSecretSharingCodecRA.Builder builder = new PackedSecretSharingCodecRA.Builder();
			builder.setRandom(RandomType.getRandomType(randomName)).setSecrecyShardsNum(z).setDataShardsNum(k)
					.setParityShardsNum(r);
			return builder.build();
		}
	},
	PACKED_SECRET_SHARING("PSS") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			PackedSecretSharingCodec.Builder builder = new PackedSecretSharingCodec.Builder();
			builder.setRandom(RandomType.getRandomType(randomName)).setSecrecyShardsNum(z).setDataShardsNum(k)
					.setParityShardsNum(r);
			return builder.build();
		}
	},
//	SECURE_JRS("JRS") {
//		@Override
//		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
//			SecureJerasureRS.Builder builder = new SecureJerasureRS.Builder();
//			builder.setDataShardsNum(k).setParityShardsNum(r);
//			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandomType(randomName)).build();
//		}
//	},
	SECURE_EVENODD("EVENODD", "EO") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			SecureEvenodd.Builder builder = new SecureEvenodd.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandomType(randomName)).build();
		}
	},
	SECURE_EVENODD_RA("EVENODD_RA", "EO_RA") {
		@Override
		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
			SecureEvenoddRA.Builder builder = new SecureEvenoddRA.Builder();
			builder.setDataShardsNum(k).setParityShardsNum(r);
			return builder.setSecrecyShardsNum(z).setRandom(RandomType.getRandomType(randomName)).build();
		}
	},
//	AES("AES") {
//	@Override
//	Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
//		AESCodec.Builder builder = new AESCodec.Builder();
//		builder.setDataShardsNum(k).setParityShardsNum(r);
//		return builder.build();
//	}
//},
//	SALSA20("SALSA", "SALSA20") {
//		@Override
//		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
//			Salsa20Codec.Builder builder = new Salsa20Codec.Builder();
//			builder.setDataShardsNum(k).setParityShardsNum(r);
//			return builder.build();
//		}
//	},
//	RC4("RC4") {
//		@Override
//		Codec buildCodecFromArgs(int k, int r, int z, String randomName) {
//			RC4Codec.Builder builder = new RC4Codec.Builder();
//			builder.setDataShardsNum(k).setParityShardsNum(r);
//			return builder.build();
//		}
//	},
	;
	
	private String[] codecNames;

	private static Map<String, CodecType> namesMap;

	private CodecType(String... codecNames) {
		Utils.validateArrayNotEmpty(codecNames, "Codec names array");
		this.codecNames = codecNames;
	}

	static {
		init();
	}

	public static Codec getCodecFromArgs(String codecName, int k, int r, int z, String randomName) {
		Utils.validateNotNull(codecName, "codec name");
		CodecType codecType = namesMap.get(codecName.toUpperCase());
		return codecType.buildCodecFromArgs(k, r, z, randomName);
	}

	abstract Codec buildCodecFromArgs(int k, int r, int z, String randomName);

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
