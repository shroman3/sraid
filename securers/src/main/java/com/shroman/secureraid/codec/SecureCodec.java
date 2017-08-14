package com.shroman.secureraid.codec;

import java.util.Random;

import com.shroman.secureraid.utils.Utils;

public abstract class SecureCodec extends Codec {
	public static abstract class Builder extends Codec.Builder {
		private SecureCodec codec;

		public abstract SecureCodec build();
		

		public Builder setSecrecyShardsNum(int secrecyShardsNum) {
			Utils.validateNotNegative(secrecyShardsNum, "secrecy Shards Num");
			codec.secrecyShardsNum = secrecyShardsNum;
			return this;
		}
		
		public Builder setRandom(Random random) {
			Utils.validateNotNull(random, "Random generator");
			codec.random = random;
			return this;
		}
		
		protected void validate() {
			Utils.validateNotNegative(codec.secrecyShardsNum, "secrecy Shards Num");
			Utils.validateNotNull(codec.random, "Random generator");
			super.validate();
		}
		
		protected void setCodec(SecureCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
		
		@Override
		protected int calcSize() {
			// MDS Property
			return codec.getParityShardsNum() + codec.secrecyShardsNum + codec.getDataShardsNum();
		}
	}

	private int secrecyShardsNum = -1;

	private Random random;

	public abstract byte[][] encode(int shardSize, byte[][] data);
	public abstract byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize);

	SecureCodec() {}
	SecureCodec(SecureCodec other) {
		super(other);
		secrecyShardsNum = other.secrecyShardsNum;
		random = other.random;
	}
	
	public int getSecrecyShardsNum() {
		return secrecyShardsNum;
	}

	public Random getRandom() {
		return random;
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
}
