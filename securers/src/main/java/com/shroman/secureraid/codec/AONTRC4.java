package com.shroman.secureraid.codec;

import java.security.SecureRandom;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

import com.shroman.secureraid.utils.Utils;

public class AONTRC4 extends CryptoCodecConstantKey {
	private static final int KEY_SIZE = 16;
	
	public static class Builder extends CryptoCodecConstantKey.Builder {
		private AONTRC4 codec;

		public Builder() {
			setCodec(new AONTRC4());
		}

		Builder(AONTRC4 secureRS) {
			setCodec(new AONTRC4(secureRS));
		}

		@Override
		public AONTRC4 build() {
			validate();
			return new AONTRC4(codec);
		}

		protected void setCodec(AONTRC4 codec) {
			super.setCodec(codec);
			this.codec = codec;
		}

		@Override
		protected Digest getDigest() {
			return getMD5Digest();
		}
	}
	
	private int keySize;
	private SecureRandom trueRandom;
	
	AONTRC4() {
	}

	AONTRC4(AONTRC4 other) {
		super(other);
		keySize = (int) Math.ceil((double)KEY_SIZE/getDataShardsNum());
		trueRandom = Utils.createTrueRandom();
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[] key = new byte[KEY_SIZE];
		trueRandom.nextBytes(key);	
		CipherParameters cipherParameters = new KeyParameter(key);
		
		byte[][] encrypt = new byte[getDataShardsNum()][shardSize + keySize];
		try {
			encrypt(data, cipherParameters, shardSize, encrypt);
			AONTAES.hashEncryptedAndXORwithKey(key, shardSize, encrypt, getMD5Digest(), getDataShardsNum());
			AONTAES.addKeyToShards(key, shardSize, encrypt, keySize);
			return encodeRS(encrypt);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);
		byte[] key = new byte[KEY_SIZE];

		AONTAES.getKeyFromShards(shards, shardSize, key, keySize);
		AONTAES.hashEncryptedAndXORwithKey(key, shardSize - keySize, shards, getMD5Digest(), getDataShardsNum());

		CipherParameters cipherParameters = new KeyParameter(key);
		byte[][] output = new byte[getDataShardsNum()][shardSize - keySize];
		
		try {
			decrypt(shards, cipherParameters, shardSize - keySize, output);
			return output;
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	public void encrypt(byte[][] data, CipherParameters cipherParameters, int inputLength, byte[][] output) throws InvalidCipherTextException {
		RC4Engine encrypt = new RC4Engine();
        encrypt.init(true, cipherParameters);
		process(encrypt, data, inputLength, output);
	}

	public void decrypt(byte[][] shards, CipherParameters cipherParameters, int inputLength, byte[][] output) throws InvalidCipherTextException {
		RC4Engine decrypt = new RC4Engine();
        decrypt.init(false, cipherParameters);
		process(decrypt, shards, inputLength, output);
	}

	public void process(RC4Engine engine, byte[][] shards, int inputLength, byte[][] output) throws InvalidCipherTextException {
		
		for (int i = 0; i < getDataShardsNum(); i++) {
			engine.processBytes(shards[i], 0, inputLength, output[i], 0);
			engine.reset();
		}
	}

	private static Digest getMD5Digest() {
		return new MD5Digest();
	}
}


