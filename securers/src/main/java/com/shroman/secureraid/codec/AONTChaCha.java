package com.shroman.secureraid.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.shroman.secureraid.utils.Utils;

public class AONTChaCha extends CryptoCodecConstantKey {
	public static class Builder extends CryptoCodecConstantKey.Builder {
		private AONTChaCha codec;

		public Builder() {
			setCodec(new AONTChaCha());
		}

		Builder(AONTChaCha secureRS) {
			setCodec(new AONTChaCha(secureRS));
		}

		@Override
		public AONTChaCha build() {
			validate();
			return new AONTChaCha(codec);
		}

		protected void setCodec(AONTChaCha codec) {
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
	
	private int keySize;
	
	AONTChaCha() {
	}

	AONTChaCha(AONTChaCha other) {
		super(other);
		keySize = (int) Math.ceil((double)ChaChaCodec.KEY_SIZE/getDataShardsNum());
	}
	
	public int getKeySize() {
		return keySize;
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		SecureRandom random = Utils.createTrueRandom();
		byte[] key = new byte[ChaChaCodec.KEY_SIZE];
		byte[] iv = new byte[ChaChaCodec.IV_SIZE];
		random.nextBytes(iv);
		random.nextBytes(key);	
		CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key), iv, 0, ChaChaCodec.IV_SIZE);
		
		StreamCipher encrypt = getEngine();

		encrypt.init(true, cipherParameters);
		byte[][] encrypted = new byte[getDataShardsNum()][];
		for (int i = 0; i < getDataShardsNum(); i++) {
			encrypted[i] = new byte[shardSize + ChaChaCodec.IV_SIZE + keySize];
			System.arraycopy(iv, 0, encrypted[i], 0, ChaChaCodec.IV_SIZE);
			encrypt.processBytes(data[i], 0, shardSize, encrypted[i], ChaChaCodec.IV_SIZE);
			encrypt.reset();
		}
		
		AONTAESJava.hashEncryptedAndXORwithKey(key, shardSize + ChaChaCodec.IV_SIZE, encrypted, getMD5Digest(), getDataShardsNum());
		AONTAESJava.addKeyToShards(key, shardSize + ChaChaCodec.IV_SIZE, encrypted, keySize);
		return encodeRS(encrypted);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);
		byte[] key = new byte[ChaChaCodec.KEY_SIZE];

		AONTAESJava.getKeyFromShards(shards, shardSize, key, keySize);
		AONTAESJava.hashEncryptedAndXORwithKey(key, shardSize - keySize, shards, getMD5Digest(), getDataShardsNum());

		CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key), shards[0], 0, ChaChaCodec.IV_SIZE);
		
		StreamCipher decrypt = getEngine();
		decrypt.init(false, cipherParameters);
		byte[][] output = new byte[getDataShardsNum()][];
		for (int i = 0; i < getDataShardsNum(); i++) {
			output[i] = new byte[shardSize - ChaChaCodec.IV_SIZE - keySize];
			decrypt.processBytes(shards[i], ChaChaCodec.IV_SIZE, shardSize - ChaChaCodec.IV_SIZE - keySize, output[i], 0);
			decrypt.reset();
		}
		return output;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
	
	@Override
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE - ((getDataShardsNum()*ChaChaCodec.IV_SIZE)/4) - ((keySize*getDataShardsNum())/4);
	}
	
	protected StreamCipher getEngine() {
		return new ChaChaEngine();
	}
	
	protected static MessageDigest getMD5Digest() {
		try {
			return MessageDigest.getInstance("MD5", "SUN");
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new IllegalArgumentException("Unable to create MD5");
		}
	}
}


