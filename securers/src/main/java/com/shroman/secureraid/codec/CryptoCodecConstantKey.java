package com.shroman.secureraid.codec;

import java.security.MessageDigest;

import com.shroman.secureraid.utils.Utils;

public abstract class CryptoCodecConstantKey extends CryptoCodec {
	public static abstract class Builder extends CryptoCodec.Builder {
		private CryptoCodecConstantKey codec;
		
		public Builder setKey(String password) {
			Utils.validateNotNull(password, "password");
			MessageDigest digest = getDigest();
			digest.update(password.getBytes(), 0, password.getBytes().length);
			codec.key = new byte[digest.getDigestLength()];
			digest.digest(codec.key);
			return this;
		}

		public abstract CryptoCodecConstantKey build();
		
		protected abstract MessageDigest getDigest();
		
		protected void validate() {
			Utils.validateNotNull(codec.key, "key");
			super.validate();
		}
		
		protected void setCodec(CryptoCodecConstantKey codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	private byte[] key;

	public abstract byte[][] encode(int shardSize, byte[][] data);
	public abstract byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize);

	CryptoCodecConstantKey() {
	}

	CryptoCodecConstantKey(CryptoCodecConstantKey other) {
		super(other);
		key = other.key;
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		return encode(shardSize, data);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		return decode(shardPresent, shards, shardSize);
	}
	
	@Override
	public byte[] generateKey() {
		return null;
	}
	
	@Override
	public boolean isKeyNeeded() {
		return false;
	}

	protected byte[] getKey() {
		return key;
	}
}
