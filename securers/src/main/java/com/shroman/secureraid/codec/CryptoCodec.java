package com.shroman.secureraid.codec;

import com.backblaze.erasure.InputOutputByteTableCodingLoop;
import com.backblaze.erasure.ReedSolomon;

public abstract class CryptoCodec extends Codec {
	public static abstract class Builder extends Codec.Builder {
		public abstract CryptoCodec build();
	}

	private ReedSolomon parityRS = null;

	CryptoCodec() {
	}

	CryptoCodec(CryptoCodec other) {
		super(other);
		if (getParityShardsNum() > 0) {			
			parityRS = new ReedSolomon(getDataShardsNum(), getParityShardsNum(), new InputOutputByteTableCodingLoop());
		}
	}
	
	protected byte[][] encodeRS(byte[][] encrypt) {
		if (parityRS == null) {
			return encrypt;
		}
		byte[][] shards = new byte[getSize()][];
		System.arraycopy(encrypt, 0, shards, 0, encrypt.length);
		for (int i = encrypt.length; i < shards.length; i++) {
			shards[i] = new byte[encrypt[0].length];
		}
		
		parityRS.encodeParity(shards, 0, encrypt[0].length);
		return shards;
	}

	protected void decodeRS(boolean[] shardPresent, byte[][] shards, int shardSize) {
		if (parityRS != null) {
			for (int i = 0; i < getDataShardsNum(); i++) {
				if (!shardPresent[i]) {
					parityRS.decodeMissing(shards, shardPresent, 0, shardSize);
					break;
				}
			}
		}
	}
}
