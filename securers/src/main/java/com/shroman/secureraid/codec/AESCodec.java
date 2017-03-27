package com.shroman.secureraid.codec;

import java.security.SecureRandom;
import java.util.Random;

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

import com.backblaze.erasure.ReedSolomon;

public class AESCodec extends Codec {
	public static class Builder extends Codec.Builder {
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
	}

	private byte[] key = new byte[32];
	private ReedSolomon parityRS = null;


	AESCodec() {
	}

	AESCodec(AESCodec other) {
		super(other);
		Random random = new SecureRandom();
		random.nextBytes(key);
		if (getParityShardsNum() > 0) {			
			parityRS = ReedSolomon.create(getDataShardsNum(), getParityShardsNum());
		}
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		CipherParameters cipherParameters = new KeyParameter(key);

		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();

		// Paddings can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/paddings/BlockCipherPadding.html
		BlockCipherPadding blockCipherPadding = new ZeroBytePadding();

		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);

		try {
			byte[][] encrypt = encrypt(data, bufferedBlockCipher, cipherParameters);
			if (parityRS == null) {
				return encrypt;
			}
			byte[][] shards = new byte[getSize()][shardSize];
			
			parityRS.encodeParity(shards, 0, shardSize);
			return shards;
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		if (parityRS != null) {
			parityRS.decodeMissing(shards, shardPresent, 0, shardSize);
		}

		CipherParameters cipherParameters = new KeyParameter(key);

		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();

		// Paddings can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/paddings/BlockCipherPadding.html
		BlockCipherPadding blockCipherPadding = new ZeroBytePadding();

		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);

		try {
			return decrypt(shards, bufferedBlockCipher, cipherParameters);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	public byte[][] encrypt(byte[][] data, BufferedBlockCipher bufferedBlockCipher, CipherParameters cipherParameters)
			throws InvalidCipherTextException {
		boolean forEncryption = true;
		return process(data, bufferedBlockCipher, cipherParameters, forEncryption);
	}

	public byte[][] decrypt(byte[][] shards, BufferedBlockCipher bufferedBlockCipher, CipherParameters cipherParameters)
			throws InvalidCipherTextException {
		boolean forEncryption = false;
		return process(shards, bufferedBlockCipher, cipherParameters, forEncryption);
	}

	public byte[][] process(byte[][] shards, BufferedBlockCipher bufferedBlockCipher, CipherParameters cipherParameters, boolean forEncryption) throws InvalidCipherTextException {
		bufferedBlockCipher.init(forEncryption, cipherParameters);

		int inputLength = shards[0].length;

		int maximumOutputLength = bufferedBlockCipher.getOutputSize(inputLength);
		byte[][] output = new byte[getDataShardsNum()][maximumOutputLength];
		
		for (int i = 0; i < getDataShardsNum(); i++) {
			int processBytes = bufferedBlockCipher.processBytes(shards[i], 0, inputLength, output[i], 0);
			bufferedBlockCipher.doFinal(output[i], processBytes);
		}
		return output;
	}
}
