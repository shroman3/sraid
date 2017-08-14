package com.shroman.secureraid.codec;

import com.backblaze.erasure.OutputInputByteTableCodingLoop;
import com.backblaze.erasure.ReedSolomon;

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

	private ReedSolomon parityRS = null;

	NoCodec() {
	}

	NoCodec(NoCodec other) {
		super(other);
		if (getParityShardsNum() > 0) {
			parityRS = new ReedSolomon(getDataShardsNum(), getParityShardsNum(), new OutputInputByteTableCodingLoop());
		}
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		return encodeRS(data);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		if (getParityShardsNum() > 0) {			
			for (int i = 0; i < parityRS.getDataShardCount(); i++) {
				if (!shardPresent[i]) {
					parityRS.decodeMissing(shards, shardPresent, 0, shardSize);
					break;
				}
			}
		}
		byte[][] data = new byte[getDataShardsNum()][];
		System.arraycopy(shards, 0, data, 0, getDataShardsNum());
		decodeRS(shardPresent, shards, shardSize);

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
