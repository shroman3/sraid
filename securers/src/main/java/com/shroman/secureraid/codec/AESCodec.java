package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;

public class AESCodec extends CryptoCodec {
	private BlockCipherPadding blockCipherPadding;
	private CipherParameters cipherParameters;

	public static class Builder extends CryptoCodec.Builder {
		private AESCodec codec;

		public Builder() {
			setCodec(new AESCodec());
		}

		Builder(AESCodec secureRS) {
			setCodec(new AESCodec(secureRS));
		}

		@Override
		public AESCodec build() {
			validate();
			return new AESCodec(codec);
		}

		protected void setCodec(AESCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}

		@Override
		protected Digest getDigest() {
			return new SHA256Digest();
		}
	}

	AESCodec() {
	}

	AESCodec(AESCodec other) {
		super(other);
		blockCipherPadding = new ZeroBytePadding();
		cipherParameters = new KeyParameter(getKey());
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();
		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);

		try {
			byte[][] encrypt = encrypt(data, bufferedBlockCipher);
			return encodeRS(encrypt);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);


		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();

		// Paddings can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/paddings/BlockCipherPadding.html
		BlockCipherPadding blockCipherPadding = new ZeroBytePadding();

		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);

		try {
			return decrypt(shards, bufferedBlockCipher);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	public byte[][] encrypt(byte[][] data, BufferedBlockCipher bufferedBlockCipher)
			throws InvalidCipherTextException {
		bufferedBlockCipher.init(true, cipherParameters);
		return process(data, bufferedBlockCipher);
	}

	public byte[][] decrypt(byte[][] shards, BufferedBlockCipher bufferedBlockCipher)
			throws InvalidCipherTextException {
		bufferedBlockCipher.init(false, cipherParameters);
		return process(shards, bufferedBlockCipher);
	}

	public byte[][] process(byte[][] shards, BufferedBlockCipher bufferedBlockCipher) throws InvalidCipherTextException {
		int inputLength = shards[0].length;

		int maximumOutputLength = bufferedBlockCipher.getOutputSize(inputLength);
		byte[][] output = new byte[getDataShardsNum()][maximumOutputLength];
		
		for (int i = 0; i < getDataShardsNum(); i++) {
			int processBytes = bufferedBlockCipher.processBytes(shards[i], 0, inputLength, output[i], 0);
			bufferedBlockCipher.doFinal(output[i], processBytes);
			bufferedBlockCipher.reset();
		}
		return output;
	}
}
