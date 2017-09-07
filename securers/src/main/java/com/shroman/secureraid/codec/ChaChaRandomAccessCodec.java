package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class ChaChaRandomAccessCodec extends ChaChaCodec {
	public static class Builder extends ChaChaCodec.Builder {
		private ChaChaRandomAccessCodec codec;

		public Builder() {
			setCodec(new ChaChaRandomAccessCodec());
		}

		Builder(ChaChaRandomAccessCodec codec) {
			setCodec(new ChaChaRandomAccessCodec(codec));
		}

		@Override
		public ChaChaRandomAccessCodec build() {
			validate();
			return new ChaChaRandomAccessCodec(codec);
		}

		protected void setCodec(ChaChaRandomAccessCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	ChaChaRandomAccessCodec() {
	}

	ChaChaRandomAccessCodec(ChaChaRandomAccessCodec other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		byte[][] output = new byte[getDataShardsNum()][];
		for (int i = 0; i < getDataShardsNum(); i++) {
			if (shards[i] != null) {				
				CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key), shards[i], 0, IV_SIZE);
				StreamCipher decrypt = getEngine();
				decrypt.init(false, cipherParameters);
				output[i] = new byte[shardSize - IV_SIZE];
				decrypt.processBytes(shards[i], IV_SIZE, shardSize - IV_SIZE, output[i], 0);
				decrypt.reset();
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
