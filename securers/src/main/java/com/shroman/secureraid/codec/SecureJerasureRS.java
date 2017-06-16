package com.shroman.secureraid.codec;

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

//	private ReedSolomon secrecyRS;
//	private ReedSolomon parityRS;

	SecureJerasureRS() {
	}

	SecureJerasureRS(SecureJerasureRS other) {
		super(other);

	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] shards = new byte[getSize()][shardSize];
		return shards;
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
