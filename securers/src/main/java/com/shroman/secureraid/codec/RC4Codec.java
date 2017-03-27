package com.shroman.secureraid.codec;

import java.security.SecureRandom;
import java.util.Random;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

import com.backblaze.erasure.ReedSolomon;

public class RC4Codec extends Codec {
	public static class Builder extends Codec.Builder {
		private RC4Codec codec;

		public Builder() {
			setCodec(new RC4Codec());
		}

		Builder(RC4Codec secureRS) {
			setCodec(new RC4Codec(secureRS));
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

	private byte[] key = new byte[16];
	private ReedSolomon parityRS = null;
	private RC4Engine encrypt = null;
	private RC4Engine decrypt = null;


	RC4Codec() {
	}

	RC4Codec(RC4Codec other) {
		super(other);
		Random random = new SecureRandom();
		random.nextBytes(key);
		if (getParityShardsNum() > 0) {			
			parityRS = ReedSolomon.create(getDataShardsNum(), getParityShardsNum());
		}
		
		CipherParameters cipherParameters = new KeyParameter(key);

		encrypt = new RC4Engine();
        encrypt.init(true, cipherParameters);

        decrypt = new RC4Engine();
        decrypt.init(false, cipherParameters);
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		try {
			byte[][] encrypt = encrypt(data);
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
		return process(encrypt, data);
	}

	public byte[][] decrypt(byte[][] shards) throws InvalidCipherTextException {
		return process(decrypt, shards);
	}

	public byte[][] process(RC4Engine engine, byte[][] shards) throws InvalidCipherTextException {
		int inputLength = shards[0].length;

		byte[][] output = new byte[getDataShardsNum()][shards[0].length];
		
		for (int i = 0; i < getDataShardsNum(); i++) {
			engine.processBytes(shards[i], 0, inputLength, output[i], 0);
		}
		return output;
	}
}
