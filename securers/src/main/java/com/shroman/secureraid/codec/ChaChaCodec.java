package com.shroman.secureraid.codec;

import java.security.SecureRandom;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.shroman.secureraid.utils.Utils;

public class ChaChaCodec extends CryptoCodecWithKey {
	public static final int IV_SIZE = 8;
	public static final int KEY_SIZE = 16;

	public static class Builder extends CryptoCodecWithKey.Builder {
		private ChaChaCodec codec;

		public Builder() {
			setCodec(new ChaChaCodec());
		}

		Builder(ChaChaCodec codec) {
			setCodec(new ChaChaCodec(codec));
		}

		@Override
		public ChaChaCodec build() {
			validate();
			return new ChaChaCodec(codec);
		}

		protected void setCodec(ChaChaCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	ChaChaCodec() {
	}

	ChaChaCodec(ChaChaCodec other) {
		super(other);
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		byte[][] encrypt = encrypt(shardSize, data, key);
		return encodeRS(encrypt);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		decodeRS(shardPresent, shards, shardSize);
		return decrypt(shardSize, shards, key);
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	@Override
	public int getKeySize() {
		return KEY_SIZE;
	}
	
	@Override
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE - ((getDataShardsNum()*IV_SIZE)/4);
	}

	protected StreamCipher getEngine() {
		return new ChaChaEngine();
	}
	
	private byte[][] encrypt(int shardSize, byte[][] data, byte[] key) {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = Utils.createTrueRandom();
        random.nextBytes(iv);
        
		CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key), iv, 0, IV_SIZE);
      
		StreamCipher encrypt = getEngine();
        encrypt.init(true, cipherParameters);
		byte[][] encrypted = new byte[getDataShardsNum()][];

		for (int i = 0; i < getDataShardsNum(); i++) {
			encrypted[i] = new byte[shardSize + IV_SIZE];
			System.arraycopy(iv, 0, encrypted[i], 0, IV_SIZE);
			encrypt.processBytes(data[i], 0, shardSize, encrypted[i], IV_SIZE);
			encrypt.reset();
		}
		return encrypted;
	}

	private byte[][] decrypt(int shardSize, byte[][] shards, byte[] key) {
		CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key), shards[0], 0, IV_SIZE);
		StreamCipher decrypt = getEngine();
        decrypt.init(false, cipherParameters);
		byte[][] output = new byte[getDataShardsNum()][];
		for (int i = 0; i < getDataShardsNum(); i++) {
			output[i] = new byte[shardSize - IV_SIZE];
			decrypt.processBytes(shards[i], IV_SIZE, shardSize - IV_SIZE, output[i], 0);
			decrypt.reset();
		}
		return output;
	}
}
