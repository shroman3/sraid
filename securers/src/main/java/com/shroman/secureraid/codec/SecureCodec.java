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
			Utils.validatePositive(codec.secrecyShardsNum, "secrecy Shards Num");
			Utils.validateNotNull(codec.random, "Random generator");
			super.validate();
		}
		
		protected void setCodec(SecureCodec codec) {
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
}
