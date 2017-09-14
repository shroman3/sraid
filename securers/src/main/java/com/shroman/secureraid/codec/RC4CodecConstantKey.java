package com.shroman.secureraid.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4CodecConstantKey extends CryptoCodecConstantKey {
	public static class Builder extends CryptoCodecConstantKey.Builder {
		private RC4CodecConstantKey codec;

		public Builder() {
			setCodec(new RC4CodecConstantKey());
		}

		Builder(RC4CodecConstantKey secureRS) {
			setCodec(new RC4CodecConstantKey(secureRS));
		}

		@Override
		public RC4CodecConstantKey build() {
			validate();
			return new RC4CodecConstantKey(codec);
		}

		protected void setCodec(RC4CodecConstantKey codec) {
			super.setCodec(codec);
			this.codec = codec;
		}

		@Override
		protected MessageDigest getDigest() {
			try {
				return MessageDigest.getInstance("MD5", "SUN");
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				throw new IllegalArgumentException("Unable to create MD5");
			}
		}
	}

//	private RC4Engine encrypt = null;
//	private RC4Engine decrypt = null;
	private CipherParameters cipherParameters;


	RC4CodecConstantKey() {
	}

	RC4CodecConstantKey(RC4CodecConstantKey other) {
		super(other);
		cipherParameters = new KeyParameter(getKey());
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		try {
			byte[][] encrypt = encrypt(data);
			return encodeRS(encrypt);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);

		try {
			return decrypt(shards);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	public byte[][] encrypt(byte[][] data) throws InvalidCipherTextException {
		RC4Engine encrypt = new RC4Engine();
        encrypt.init(true, cipherParameters);
		return process(encrypt, data);
	}

	public byte[][] decrypt(byte[][] shards) throws InvalidCipherTextException {
		RC4Engine decrypt = new RC4Engine();
        decrypt.init(false, cipherParameters);
		return process(decrypt, shards);
	}

	public byte[][] process(RC4Engine engine, byte[][] shards) throws InvalidCipherTextException {
		int inputLength = shards[0].length;

		byte[][] output = new byte[getDataShardsNum()][shards[0].length];
		
		for (int i = 0; i < getDataShardsNum(); i++) {
			engine.processBytes(shards[i], 0, inputLength, output[i], 0);
			engine.reset();
		}
		return output;
	}
}
