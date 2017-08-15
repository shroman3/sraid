package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4Codec extends CryptoCodecWithKey {
	public static final int KEY_SIZE = 16;
	public static class Builder extends CryptoCodecWithKey.Builder {
		private RC4Codec codec;

		public Builder() {
			setCodec(new RC4Codec());
		}

		Builder(RC4Codec codec) {
			setCodec(new RC4Codec(codec));
		}

		@Override
		public RC4Codec build() {
			validate();
			return new RC4Codec(codec);
		}

		protected void setCodec(RC4Codec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	RC4Codec() {
	}

	RC4Codec(RC4Codec other) {
		super(other);
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		try {
			CipherParameters cipherParameters = new KeyParameter(key);
			byte[][] encrypt = encrypt(data, cipherParameters);
			return encodeRS(encrypt);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		decodeRS(shardPresent, shards, shardSize);

		try {
			CipherParameters cipherParameters = new KeyParameter(key);
			return decrypt(shards, cipherParameters);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	@Override
	public int getKeySize() {
		return KEY_SIZE;
	}

	private byte[][] encrypt(byte[][] data, CipherParameters cipherParameters) throws InvalidCipherTextException {
		RC4Engine encrypt = new RC4Engine();
        encrypt.init(true, cipherParameters);
		return process(encrypt, data);
	}

	private byte[][] decrypt(byte[][] shards, CipherParameters cipherParameters) throws InvalidCipherTextException {
		RC4Engine decrypt = new RC4Engine();
        decrypt.init(false, cipherParameters);
		return process(decrypt, shards);
	}

	private byte[][] process(RC4Engine engine, byte[][] shards) throws InvalidCipherTextException {
		int inputLength = shards[0].length;

		byte[][] output = new byte[getDataShardsNum()][shards[0].length];
		
		for (int i = 0; i < getDataShardsNum(); i++) {
			engine.processBytes(shards[i], 0, inputLength, output[i], 0);
			engine.reset();
		}
		return output;
	}
}
