package com.shroman.secureraid.codec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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

public class AESJavaCodec extends CryptoCodecWithKey {
	public static final int IV_SIZE = 16;
	public static final int KEY_SIZE = 32;

	public static class Builder extends CryptoCodecWithKey.Builder {
		private AESJavaCodec codec;

		public Builder() {
			setCodec(new AESJavaCodec());
		}

		Builder(AESJavaCodec secureRS) {
			setCodec(new AESJavaCodec(secureRS));
		}
		
		public Builder setProvider(String provider) {
			Utils.validateNotNull(provider, "provider");
			codec.provider = provider;
			return this;
		}

		@Override
		protected void validate() {
			Utils.validateNotNull(codec.provider, "provider");
			super.validate();
		}
	
		@Override
		public AESJavaCodec build() {
			validate();
			return new AESJavaCodec(codec);
		}

		protected void setCodec(AESJavaCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	private String provider;

	AESJavaCodec() {
	}

	AESJavaCodec(AESJavaCodec other) {
		super(other);
		provider = other.provider;
	}
	
	public String getProvider() {
		return provider;
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data, byte[] key) {
		try {
			byte[][] encrypt = encrypt(shardSize, data, key);
			return encodeRS(encrypt);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}
	
	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		decodeRS(shardPresent, shards, shardSize);
		try {
			return decrypt(shardSize, shards, key);
		} catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}
    public byte[][] encrypt(int shardSize, byte[][] data, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException  {
        // Generating IV.
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = Utils.createTrueRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Encrypt.
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        int maximumOutputLength = cipher.getOutputSize(shardSize);

        byte[][] encrypted = new byte[getDataShardsNum()][];
        for (int i = 0; i < encrypted.length; i++) {
        	encrypted[i] = new byte[maximumOutputLength + IV_SIZE];
        	System.arraycopy(iv, 0, encrypted[i], 0, IV_SIZE);
        	cipher.doFinal(data[i], 0, shardSize, encrypted[i], IV_SIZE);
		}
        
        return encrypted;
    }

	 public byte[][] decrypt(int shardSize, byte[][] shards, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		 SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		 Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
		 byte[] iv = new byte[IV_SIZE];
		 
		 byte[][] output = new byte[getDataShardsNum()][];
		 for (int i = 0; i < getDataShardsNum(); i++) {
			 System.arraycopy(shards[i], 0, iv, 0, IV_SIZE);
			 IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
			 int encryptedSize = shardSize - IV_SIZE;
			 cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
			 int maximumOutputLength = cipherDecrypt.getOutputSize(encryptedSize);
			 output[i] = new byte[maximumOutputLength];
			 cipherDecrypt.doFinal(shards[i], IV_SIZE, encryptedSize, output[i], 0);
		 }
		 
		 return output;
	 }
	 
	public Builder getSelfBuilder() {
		return new Builder(this);
	}
	
	@Override
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE - ((getDataShardsNum()*IV_SIZE)/2);
	}

	@Override
	public int getKeySize() {
		return KEY_SIZE;
	}
}
