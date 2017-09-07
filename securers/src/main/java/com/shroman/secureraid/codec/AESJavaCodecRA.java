package com.shroman.secureraid.codec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.RuntimeCryptoException;

public class AESJavaCodecRA extends AESJavaCodec {
	public static class Builder extends AESJavaCodec.Builder {
		private AESJavaCodecRA codec;

		public Builder() {
			setCodec(new AESJavaCodecRA());
		}

		Builder(AESJavaCodecRA secureRS) {
			setCodec(new AESJavaCodecRA(secureRS));
		}

		@Override
		public AESJavaCodecRA build() {
			validate();
			return new AESJavaCodecRA(codec);
		}

		protected void setCodec(AESJavaCodecRA codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	AESJavaCodecRA() {
	}

	AESJavaCodecRA(AESJavaCodecRA other) {
		super(other);

	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize, byte[] key) {
		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding", getProvider());
	        byte[] iv = new byte[IV_SIZE];

			byte[][] output = new byte[getDataShardsNum()][];
			
			for (int i = 0; i < getDataShardsNum(); i++) {
				if (shards[i] != null) {
					System.arraycopy(shards[i], 0, iv, 0, IV_SIZE);
		        	IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
					int encryptedSize = shardSize - IV_SIZE;
		        	cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
		        	int maximumOutputLength = cipherDecrypt.getOutputSize(encryptedSize);
					output[i] = new byte[maximumOutputLength];
		        	cipherDecrypt.doFinal(shards[i], IV_SIZE, encryptedSize, output[i], 0);
					return output;
				}
			}
			throw new IllegalArgumentException("Something went wrong, given empty stripe");
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
	
	@Override
	public boolean hasRandomRead() {
		return true;
	}

	@Override
	protected boolean[] chunksPresent(byte[][] chunks, int chunkSize) {
		return null;
	}
}
