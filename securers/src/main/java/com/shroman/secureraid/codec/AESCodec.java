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

public class AESCodec extends CryptoCodecWithKey {
	public static final int BYTES_IN_MEGABYTE_WITHOUT_PADDING = 1048448;// After padding it is 1048576
	public static final int KEY_SIZE = 32;

	public static class Builder extends CryptoCodecWithKey.Builder {
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
	
	private BlockCipherPadding blockCipherPadding;
//	private CipherParameters cipherParameters;

	AESCodec() {
	}

	AESCodec(AESCodec other) {
		super(other);
		blockCipherPadding = new ZeroBytePadding();
//		cipherParameters = new KeyParameter(getKey());
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		CipherParameters cipherParameters = new KeyParameter(key);
		BlockCipher blockCipher = new AESEngine();
		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);

		try {
			byte[][] encrypt = encrypt(data, bufferedBlockCipher, cipherParameters);
			return encodeRS(encrypt);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		decodeRS(shardPresent, shards, shardSize);

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
	
	@Override
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE_WITHOUT_PADDING;
	}

	@Override
	public int getKeySize() {
		return 32;
	}

	private byte[][] encrypt(byte[][] data, BufferedBlockCipher bufferedBlockCipher, CipherParameters cipherParameters)
			throws InvalidCipherTextException {
		bufferedBlockCipher.init(true, cipherParameters);
		return process(data, bufferedBlockCipher);
	}

	private byte[][] decrypt(byte[][] shards, BufferedBlockCipher bufferedBlockCipher, CipherParameters cipherParameters)
			throws InvalidCipherTextException {
		bufferedBlockCipher.init(false, cipherParameters);
		return process(shards, bufferedBlockCipher);
	}

	private byte[][] process(byte[][] shards, BufferedBlockCipher bufferedBlockCipher) throws InvalidCipherTextException {
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
