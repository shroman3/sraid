package com.shroman.secureraid.codec;

import com.backblaze.erasure.ReedSolomon;

public class SecureBackblazeRS extends SecureCodec {

	public static class Builder extends SecureCodec.Builder {
		private SecureBackblazeRS securers;

		public Builder() {
			setCodec(new SecureBackblazeRS());
		}

		Builder(SecureBackblazeRS secureRS) {
			setCodec(new SecureBackblazeRS(secureRS));
		}

		@Override
		public SecureBackblazeRS build() {
			validate();
			return new SecureBackblazeRS(securers);
		}

		protected void setCodec(SecureBackblazeRS securers) {
			super.setCodec(securers);
			this.securers = securers;
		}
	}

	private ReedSolomon secrecyRS;
	private ReedSolomon parityRS;

	SecureBackblazeRS() {
	}

	SecureBackblazeRS(SecureBackblazeRS other) {
		super(other);
		secrecyRS = ReedSolomon.create(getSecrecyShardsNum(), getParityShardsNum() + getDataShardsNum());
		parityRS = ReedSolomon.create(getSecrecyShardsNum() + getDataShardsNum(), getParityShardsNum());

	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] shards = new byte[getSize()][shardSize];
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			getRandom().nextBytes(shards[i]);
		}
		secrecyRS.encodeParity(shards, 0, shardSize);
		// Fill in the data shards
		for (int i = 0; i < getDataShardsNum(); i++) {
			for (int j = 0; j < shardSize; j++) {
				shards[i + getSecrecyShardsNum()][j] = (byte) (shards[i + getSecrecyShardsNum()][j] ^ data[i][j]);
			}
		}

		// Use Reed-Solomon to calculate the parity.
		parityRS.encodeParity(shards, 0, shardSize);
		return shards;
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		byte[][] data = new byte[getDataShardsNum()][shardSize];
		parityRS.decodeMissing(shards, shardPresent, 0, shardSize);
		final byte[][] secrecyShards = new byte[getSize()][shardSize];
		// Fill in the data shards
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			System.arraycopy(shards[i], 0, secrecyShards[i], 0, shardSize);
		}
		
		secrecyRS.encodeParity(secrecyShards, 0, shardSize);
		// Fill in the data shards
		for (int i = 0; i < getDataShardsNum(); i++) {
			int afterSecretIndex = i + getSecrecyShardsNum();
			for (int j = 0; j < shardSize; j++) {
				data[i][j] = (byte) (shards[afterSecretIndex][j] ^ secrecyShards[afterSecretIndex][j]);
			}
		}
		return data;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}
