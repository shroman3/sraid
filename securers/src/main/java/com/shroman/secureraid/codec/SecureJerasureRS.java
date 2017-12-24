package com.shroman.secureraid.codec;

import java.util.Random;

import com.xiaomi.infra.ec.ErasureCodec;
import com.xiaomi.infra.ec.ErasureCodec.Algorithm;

public class SecureJerasureRS extends SecureCodec {
	public static class Builder extends SecureCodec.Builder {
		private SecureJerasureRS securers;

		public Builder() {
			setCodec(new SecureJerasureRS());
		}

		Builder(SecureJerasureRS secureRS) {
			setCodec(new SecureJerasureRS(secureRS));
		}

		@Override
		public SecureJerasureRS build() {
			validate();
			return new SecureJerasureRS(securers);
		}

		protected void setCodec(SecureJerasureRS securers) {
			super.setCodec(securers);
			this.securers = securers;
		}
	}


	private ErasureCodec secrecyRS;
	private ErasureCodec parityRS;

	SecureJerasureRS() {
	}

	SecureJerasureRS(SecureJerasureRS other) {
		super(other);
		secrecyRS = new ErasureCodec.Builder(Algorithm.Cauchy_Reed_Solomon)
				.dataBlockNum(getSecrecyShardsNum())
				.codingBlockNum(getDataShardsNum() + getParityShardsNum())
				.wordSize(8)
				.packetSize(2048)
				.good(false).build();
		parityRS = new ErasureCodec.Builder(Algorithm.Cauchy_Reed_Solomon)
				.dataBlockNum(getSecrecyShardsNum() + getDataShardsNum())
				.codingBlockNum(getParityShardsNum())
				.wordSize(8)
				.packetSize(2048)
				.good(false).build();
//		secrecyRS = new ErasureCodec.Builder(Algorithm.Reed_Solomon)
//				.dataBlockNum(getSecrecyShardsNum())
//				.codingBlockNum(getDataShardsNum() + getParityShardsNum())
//				.wordSize(8).build();
//		parityRS = new ErasureCodec.Builder(Algorithm.Reed_Solomon)
//				.dataBlockNum(getSecrecyShardsNum() + getDataShardsNum())
//				.codingBlockNum(getParityShardsNum())
//				.wordSize(8).build();
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] secrets = new byte[getSecrecyShardsNum()][];
		Random random = getRandom();
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			secrets[i] = new byte[shardSize];
			random.nextBytes(secrets[i]);
		}

		byte[][] secret = secrecyRS.encode(secrets);		
		byte[][] encrypted = new byte[getSecrecyShardsNum() + getDataShardsNum()][];
		System.arraycopy(secret, 0, encrypted, 0, getSecrecyShardsNum());
		// Fill in the data shards
		for (int i = 0; i < getDataShardsNum(); i++) {
			for (int j = 0; j < shardSize; j++) {
				data[i][j] = (byte) (secret[i][j] ^ data[i][j]);
			}
			encrypted[i+getSecrecyShardsNum()] = data[i];
		}
		byte[][] parity = parityRS.encode(encrypted);

		byte[][] shards = new byte[getSize()][];
		System.arraycopy(encrypted, 0, shards, 0, getSecrecyShardsNum() + getDataShardsNum());
		System.arraycopy(parity, 0, shards, getSecrecyShardsNum() + getDataShardsNum(), getParityShardsNum());
		return shards;
//		return null;
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		byte[][] data = new byte[getDataShardsNum()][shardSize];
		return data;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}
