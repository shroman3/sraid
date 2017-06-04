package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.Digest;

import com.backblaze.erasure.ReedSolomon;
import com.shroman.secureraid.utils.Utils;

public abstract class CryptoCodec extends Codec {
	public static abstract class Builder extends Codec.Builder {
		private CryptoCodec codec;
		
		public Builder setKey(String password) {
			Utils.validateNotNull(password, "password");
			Digest digest = getDigest();
			digest.update(password.getBytes(), 0, password.getBytes().length);
			codec.key = new byte[digest.getDigestSize()];
			digest.doFinal(codec.key, 0);
			return this;
		}

		public abstract CryptoCodec build();
		
		protected abstract Digest getDigest();
		
		protected void validate() {
			Utils.validateNotNull(codec.key, "key");
			super.validate();
		}
		
		protected void setCodec(CryptoCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	private byte[] key;
	private ReedSolomon parityRS = null;

	CryptoCodec() {
	}

	CryptoCodec(CryptoCodec other) {
		super(other);
		key = other.key;
		if (getParityShardsNum() > 0) {			
			parityRS = ReedSolomon.create(getDataShardsNum(), getParityShardsNum());
		}
	}
	
	protected byte[] getKey() {
		return key;
	}
	
	protected byte[][] encodeRS(byte[][] encrypt) {
		if (parityRS == null) {
			return encrypt;
		}
		byte[][] shards = new byte[getSize()][];
		int i = 0;
		for (; i < encrypt.length; i++) {
			shards[i] = encrypt[i];
		}
		for (; i < shards.length; i++) {
			shards[i] = new byte[encrypt[0].length];
		}
		
		parityRS.encodeParity(shards, 0, encrypt[0].length);
		return shards;
	}

	protected void decodeRS(boolean[] shardPresent, byte[][] shards, int shardSize) {
		if (parityRS != null) {
			parityRS.decodeMissing(shards, shardPresent, 0, shardSize);
		}
	}
}
