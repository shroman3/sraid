package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;

public class AESCodecRA extends AESCodec {

	public static class Builder extends AESCodec.Builder {
		private AESCodecRA codec;

		public Builder() {
			setCodec(new AESCodecRA());
		}

		Builder(AESCodecRA secureRS) {
			setCodec(new AESCodecRA(secureRS));
		}

		@Override
		public AESCodecRA build() {
			validate();
			return new AESCodecRA(codec);
		}

		protected void setCodec(AESCodecRA codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	AESCodecRA() {
	}

	AESCodecRA(AESCodecRA other) {
		super(other);
	}


	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		CipherParameters cipherParameters = new KeyParameter(key);

		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();

		// Paddings can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/paddings/BlockCipherPadding.html
		BlockCipherPadding blockCipherPadding = new ZeroBytePadding();

		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);

		try {
			bufferedBlockCipher.init(false, cipherParameters);
			int inputLength = shards[0].length;
			
			int maximumOutputLength = bufferedBlockCipher.getOutputSize(inputLength);
			byte[][] output = new byte[getDataShardsNum()][];
			int i = 0;
			output[i] = new byte[maximumOutputLength];
			
			int processBytes = bufferedBlockCipher.processBytes(shards[i], 0, inputLength, output[i], 0);
			bufferedBlockCipher.doFinal(output[i], processBytes);
			return output;
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}
