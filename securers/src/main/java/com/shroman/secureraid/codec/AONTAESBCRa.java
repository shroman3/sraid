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

public class AONTAESBCRa extends AONTAESBC {
	// public static final int BYTES_IN_MEGABYTE_WITHOUT_PADDING = 1048440;//
	// After padding it is 1048576

	public static class Builder extends AONTAESBC.Builder {
		private AONTAESBCRa codec;

		public Builder() {
			setCodec(new AONTAESBCRa());
		}

		Builder(AONTAESBCRa secureRS) {
			setCodec(new AONTAESBCRa(secureRS));
		}

		@Override
		public AONTAESBCRa build() {
			validate();
			return new AONTAESBCRa(codec);
		}

		protected void setCodec(AONTAESBCRa codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	AONTAESBCRa() {
	}

	AONTAESBCRa(AONTAESBCRa other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		byte[] key = new byte[AESJavaCodec.KEY_SIZE];

		AONTAESJava.getKeyFromShards(shards, shardSize, key, getKeySize());
		AONTAESJava.hashEncryptedAndXORwithKey(key, shardSize - getKeySize(), shards, AONTAESJava.getSHA256Digest(),
				getDataShardsNum());

		// List of BlockCiphers can be found at
		// http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();

		// Paddings can be found at
		// http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/paddings/BlockCipherPadding.html
		BlockCipherPadding blockCipherPadding = new ZeroBytePadding();

		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);
		CipherParameters cipherParameters = new KeyParameter(key);

		try {
			bufferedBlockCipher.init(false, cipherParameters);
			int maximumOutputLength = bufferedBlockCipher.getOutputSize(shardSize - getKeySize());
			byte[][] output = new byte[getDataShardsNum()][];

			int i = 0;
			output[i] = new byte[maximumOutputLength];
			int processBytes = bufferedBlockCipher.processBytes(shards[i], 0, shardSize - getKeySize(), output[i], 0);
			bufferedBlockCipher.doFinal(output[i], processBytes);
			bufferedBlockCipher.reset();
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
