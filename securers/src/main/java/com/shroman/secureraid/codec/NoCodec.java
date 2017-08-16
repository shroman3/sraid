package com.shroman.secureraid.codec;

public class NoCodec extends CryptoCodec {
	public static class Builder extends CryptoCodec.Builder {
		private NoCodec codec;

		public Builder() {
			setCodec(new NoCodec());
		}

		Builder(NoCodec secureRS) {
			setCodec(new NoCodec(secureRS));
		}

		@Override
		public NoCodec build() {
			validate();
			return new NoCodec(codec);
		}

		protected void setCodec(NoCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	NoCodec() {
	}

	NoCodec(NoCodec other) {
		super(other);
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		return encodeRS(data);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		decodeRS(shardPresent, shards, shardSize);
		byte[][] data = new byte[getDataShardsNum()][];
		System.arraycopy(shards, 0, data, 0, getDataShardsNum());
		return data;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
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
