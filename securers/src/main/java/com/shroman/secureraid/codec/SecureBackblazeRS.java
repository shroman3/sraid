package com.shroman.secureraid.codec;

import com.backblaze.erasure.InputOutputByteTableCodingLoop;
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

	private ReedSolomon secrecyRS = null;
	private ReedSolomon parityRS = null;
	private DecryptInterface decrypt;
	private DecodeInterface decodeMissing;

	SecureBackblazeRS() {
	}

	SecureBackblazeRS(SecureBackblazeRS other) {
		super(other);

		if (getSecrecyShardsNum() > 0) {
			decrypt = SecureBackblazeRS::decrypt;
			secrecyRS = new ReedSolomon(getSecrecyShardsNum(), getParityShardsNum() + getDataShardsNum(),
					new InputOutputByteTableCodingLoop());
		} else {
			decrypt = SecureBackblazeRS::emptyDecrypt;
		}
		
		if (getParityShardsNum() > 0) {
			decodeMissing = SecureBackblazeRS::decodeMissing;
			parityRS = new ReedSolomon(getSecrecyShardsNum() + getDataShardsNum(), getParityShardsNum(),
					new InputOutputByteTableCodingLoop());
		} else {
			decodeMissing = SecureBackblazeRS::emptyDecodeMissing;
		}
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] shards = new byte[getSize()][];
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			shards[i] = new byte[shardSize];
			getRandom().nextBytes(shards[i]);
		}
		System.arraycopy(data, 0, shards, getSecrecyShardsNum(), getDataShardsNum());
		for (int i = getDataShardsNum() + getSecrecyShardsNum(); i < shards.length; i++) {
			shards[i] = new byte[shardSize];
		}

		if (getSecrecyShardsNum() > 0) {
			secrecyRS.encodePartialParity(shards, 0, shardSize, getDataShardsNum());
		}

		// Use Reed-Solomon to calculate the parity.
		if (getParityShardsNum() > 0) {
			parityRS.encodeParity(shards, 0, shardSize);
		}
		return shards;
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeMissing.decodeMissing(parityRS, shardPresent, shards, shardSize);

		decrypt.decrypt(secrecyRS, shardSize, shards);
		byte[][] data = new byte[getDataShardsNum()][];
		System.arraycopy(shards, getSecrecyShardsNum(), data, 0, getDataShardsNum());
		return data;
	}

	private static void emptyDecodeMissing(ReedSolomon parityRS, boolean[] shardPresent, byte[][] shards, int shardSize) {}

	private static void decodeMissing(ReedSolomon parityRS, boolean[] shardPresent, byte[][] shards, int shardSize) {
		for (int i = 0; i < parityRS.getDataShardCount(); i++) {
			if (!shardPresent[i]) {
				parityRS.decodeMissing(shards, shardPresent, 0, shardSize);
				break;
			}
		}
	}

	private static void emptyDecrypt(ReedSolomon secrecyRS, int shardSize, final byte[][] secrecyShards) {}
	
	private static void decrypt(ReedSolomon secrecyRS, int shardSize, final byte[][] secrecyShards) {
		secrecyRS.encodeParity(secrecyShards, 0, shardSize);
	}

	@FunctionalInterface
	private static interface DecodeInterface {
		void decodeMissing(ReedSolomon parityRS, boolean[] shardPresent, byte[][] shards, int shardSize);
	}

	@FunctionalInterface
	private static interface DecryptInterface {
		void decrypt(ReedSolomon secrecyRS, int shardSize, final byte[][] secrecyShards);
	}
}
