package com.shroman.secureraid.codec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.RuntimeCryptoException;

import com.shroman.secureraid.utils.Utils;

public class AONTAESJava extends CryptoCodecConstantKey {
	public static class Builder extends CryptoCodecConstantKey.Builder {
		private AONTAESJava codec;

		public Builder() {
			setCodec(new AONTAESJava());
		}

		Builder(AONTAESJava secureRS) {
			setCodec(new AONTAESJava(secureRS));
		}

		public Builder setProvider(String provider) {
			Utils.validateNotNull(provider, "provider");
			codec.provider = provider;
			return this;
		}

		@Override
		public AONTAESJava build() {
			validate();
			return new AONTAESJava(codec);
		}

		protected void setCodec(AONTAESJava codec) {
			super.setCodec(codec);
			this.codec = codec;
		}

		@Override
		protected void validate() {
			Utils.validateNotNull(codec.provider, "provider");
			super.validate();
		}

		@Override
		protected MessageDigest getDigest() {
			try {
				return MessageDigest.getInstance("SHA-256", "SUN");
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				throw new IllegalArgumentException("Unable to create SHA-256");
			}
		}
	}

	private int keySize;
	private String provider;

	AONTAESJava() {
	}

	AONTAESJava(AONTAESJava other) {
		super(other);
		keySize = (int) Math.ceil((double) AESJavaCodec.KEY_SIZE / getDataShardsNum());
		provider = other.provider;
	}

	public String getProvider() {
		return provider;
	}

	public int getKeySize() {
		return keySize;
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		SecureRandom random = Utils.createTrueRandom();
		byte[] key = new byte[AESJavaCodec.KEY_SIZE];
		byte[] iv = new byte[AESJavaCodec.IV_SIZE];
		random.nextBytes(iv);
		random.nextBytes(key);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

		try {
			Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding", provider);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
			int maximumOutputLength = cipher.getOutputSize(shardSize);

			byte[][] encrypted = new byte[getDataShardsNum()][];
			for (int i = 0; i < encrypted.length; i++) {
				encrypted[i] = new byte[maximumOutputLength + AESJavaCodec.IV_SIZE + keySize];
				System.arraycopy(iv, 0, encrypted[i], 0, AESJavaCodec.IV_SIZE);
				cipher.doFinal(data[i], 0, shardSize, encrypted[i], AESJavaCodec.IV_SIZE);
			}

			hashEncryptedAndXORwithKey(key, maximumOutputLength + AESJavaCodec.IV_SIZE, encrypted, getSHA256Digest(),
					getDataShardsNum());
			addKeyToShards(key, maximumOutputLength + AESJavaCodec.IV_SIZE, encrypted, keySize);
			return encodeRS(encrypted);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException
				| BadPaddingException | NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);
		byte[] key = new byte[AESJavaCodec.KEY_SIZE];

		getKeyFromShards(shards, shardSize, key, keySize);
		hashEncryptedAndXORwithKey(key, shardSize - keySize, shards, getSHA256Digest(), getDataShardsNum());

		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

		try {
			Cipher cipherDecrypt = Cipher.getInstance("AES/CTR/PKCS5Padding", provider);
			byte[] iv = new byte[AESJavaCodec.IV_SIZE];
			byte[][] output = new byte[getDataShardsNum()][];

			for (int i = 0; i < getDataShardsNum(); i++) {
				System.arraycopy(shards[i], 0, iv, 0, AESJavaCodec.IV_SIZE);
				IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
				int encryptedSize = shardSize - AESJavaCodec.IV_SIZE - keySize;
				cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
				int maximumOutputLength = cipherDecrypt.getOutputSize(encryptedSize);
				output[i] = new byte[maximumOutputLength];
				cipherDecrypt.doFinal(shards[i], AESJavaCodec.IV_SIZE, encryptedSize, output[i], 0);
			}
			return output;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException
				| BadPaddingException | NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	@Override
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE - ((getDataShardsNum() * AESJavaCodec.IV_SIZE) / 2)
				- ((keySize * getDataShardsNum()) / 4);
	}

	static void addKeyToShards(byte[] key, int maximumOutputLength, byte[][] encrypt, int keySize) {
		int j = 0;
		for (int i = 0; i < key.length; i += keySize) {
			System.arraycopy(key, i, encrypt[j++], maximumOutputLength, keySize);
		}
	}

	static void getKeyFromShards(byte[][] shards, int shardSize, byte[] key, int keySize) {
		int j = 0;
		for (int i = 0; i < key.length; i += keySize) {
			System.arraycopy(shards[j++], shardSize - keySize, key, i, keySize);
		}
	}

	static void hashEncryptedAndXORwithKey(byte[] key, int maximumOutputLength, byte[][] encrypt, MessageDigest digest,
			int dataShards) {
		byte[] hashed = new byte[digest.getDigestLength()];
		for (int j = 0; j < dataShards; j++) {
			digest.update(encrypt[j], 0, maximumOutputLength);
			digest.digest(hashed);
			for (int i = 0; i < hashed.length; i++) {
				key[i] = (byte) (key[i] ^ hashed[i]);
			}
		}
	}

	protected static MessageDigest getSHA256Digest() {
		try {
			return MessageDigest.getInstance("SHA-256", "SUN");
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new IllegalArgumentException("Unable to create SHA-256");
		}
	}
}
