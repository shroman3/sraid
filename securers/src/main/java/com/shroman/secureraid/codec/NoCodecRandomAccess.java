package com.shroman.secureraid.codec;

public class NoCodecRandomAccess extends NoCodec {
	public static class Builder extends NoCodec.Builder {
		private NoCodecRandomAccess codec;

		public Builder() {
			setCodec(new NoCodecRandomAccess());
		}

		Builder(NoCodecRandomAccess codec) {
			setCodec(new NoCodecRandomAccess(codec));
		}

		@Override
		public NoCodecRandomAccess build() {
			validate();
			return new NoCodecRandomAccess(codec);
		}

		protected void setCodec(NoCodecRandomAccess codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	NoCodecRandomAccess() {
	}

	NoCodecRandomAccess(NoCodecRandomAccess other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		byte[][] data = new byte[getDataShardsNum()][];
		System.arraycopy(shards, 0, data, 0, getDataShardsNum());
		return data;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
	
	@Override
	public boolean hasRandomRead() {
		return true;
	}
	
	@Override
	protected boolean[] chunksPresent(byte[][] chunks, int chunkSize) {
		return null;
	}
}
