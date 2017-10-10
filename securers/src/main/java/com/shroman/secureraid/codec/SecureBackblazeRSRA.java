package com.shroman.secureraid.codec;

public class SecureBackblazeRSRA extends SecureBackblazeRS {

	public static class Builder extends SecureBackblazeRS.Builder {
		private SecureBackblazeRSRA securers;

		public Builder() {
			setCodec(new SecureBackblazeRSRA());
		}

		Builder(SecureBackblazeRSRA secureRS) {
			setCodec(new SecureBackblazeRSRA(secureRS));
		}

		@Override
		public SecureBackblazeRSRA build() {
			validate();
			return new SecureBackblazeRSRA(securers);
		}

		protected void setCodec(SecureBackblazeRSRA securers) {
			super.setCodec(securers);
			this.securers = securers;
		}
	}

	SecureBackblazeRSRA() {
	}

	SecureBackblazeRSRA(SecureBackblazeRSRA other) {
		super(other);
	}
	
	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		int count = 0;
		for (int i = getSecrecyShardsNum(); i < shards.length - getParityShardsNum(); i++) {
			if (shards[i] != null) {
				count++;
			}
		}
		getSecrecyRS().encodeParityColumn(shards, 0, shardSize, count);
		byte[][] data = new byte[getDataShardsNum()][];
		System.arraycopy(shards, getSecrecyShardsNum(), data, 0, getDataShardsNum());
		return data;
	}
	
	@Override
	public boolean hasRandomRead() {
		return true;
	}
	
	@Override
	protected boolean[] chunksPresent(byte[][] chunks, int chunkSize) {
//		boolean[] shardPresent = new boolean[getSize()];
//		for (int j = 0; j < getSize(); ++j) {
//			shardPresent[j] = (chunks[j] != null);
//		}
//		return shardPresent;
		return null;
	}
}
