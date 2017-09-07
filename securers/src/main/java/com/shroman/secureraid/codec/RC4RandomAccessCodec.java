package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4RandomAccessCodec extends RC4Codec {
	public static class Builder extends RC4Codec.Builder {
		private RC4RandomAccessCodec codec;

		public Builder() {
			setCodec(new RC4RandomAccessCodec());
		}

		Builder(RC4RandomAccessCodec codec) {
			setCodec(new RC4RandomAccessCodec(codec));
		}

		@Override
		public RC4RandomAccessCodec build() {
			validate();
			return new RC4RandomAccessCodec(codec);
		}

		protected void setCodec(RC4RandomAccessCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	RC4RandomAccessCodec() {
	}

	RC4RandomAccessCodec(RC4RandomAccessCodec other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		RC4Engine decrypt = new RC4Engine();
		decrypt.init(false, new KeyParameter(key));
		byte[][] output = new byte[getDataShardsNum()][];			
		for (int i = 0; i < getDataShardsNum(); i++) {
			if (shards[i] != null) {
				output[i] = new byte[shards[i].length]; 
				decrypt.processBytes(shards[i], 0, shards[i].length, output[i], 0);
				return output;
			}
		}
		throw new IllegalArgumentException("Something went wrong, given empty stripe");
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
