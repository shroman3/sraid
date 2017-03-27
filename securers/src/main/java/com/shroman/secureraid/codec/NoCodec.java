package com.shroman.secureraid.codec;

public class NoCodec extends Codec {
	public static class Builder extends Codec.Builder {
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
	public byte[][] encode(int shardSize, byte[][] data) {
		return data;
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		return shards;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}
