package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class AONTChaChaRA extends AONTChaCha {
	public static class Builder extends AONTChaCha.Builder {
		private AONTChaChaRA codec;

		public Builder() {
			setCodec(new AONTChaChaRA());
		}

		Builder(AONTChaChaRA secureRS) {
			setCodec(new AONTChaChaRA(secureRS));
		}

		@Override
		public AONTChaChaRA build() {
			validate();
			return new AONTChaChaRA(codec);
		}

		protected void setCodec(AONTChaChaRA codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	AONTChaChaRA() {
	}

	AONTChaChaRA(AONTChaChaRA other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);
		byte[] key = new byte[ChaChaCodec.KEY_SIZE];

		AONTAESJava.getKeyFromShards(shards, shardSize, key, getKeySize());
		AONTAESJava.hashEncryptedAndXORwithKey(key, shardSize - getKeySize(), shards, getMD5Digest(), getDataShardsNum());

		CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key), shards[0], 0, ChaChaCodec.IV_SIZE);
		
		StreamCipher decrypt = getEngine();
		decrypt.init(false, cipherParameters);
		byte[][] output = new byte[getDataShardsNum()][];
		int i = 0;
		output[i] = new byte[shardSize - ChaChaCodec.IV_SIZE - getKeySize()];
		decrypt.processBytes(shards[i], ChaChaCodec.IV_SIZE, shardSize - ChaChaCodec.IV_SIZE - getKeySize(), output[i], 0);
		decrypt.reset();
		
		return output;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}


