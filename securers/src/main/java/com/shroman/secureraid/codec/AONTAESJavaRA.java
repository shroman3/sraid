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

public class AONTAESJavaRA extends AONTAESJava {

	public static class Builder extends AONTAESJava.Builder {
		private AONTAESJavaRA codec;

		public Builder() {
			setCodec(new AONTAESJavaRA());
		}

		Builder(AONTAESJavaRA secureRS) {
			setCodec(new AONTAESJavaRA(secureRS));
		}

		@Override
		public AONTAESJavaRA build() {
			validate();
			return new AONTAESJavaRA(codec);
		}

		protected void setCodec(AONTAESJavaRA codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	AONTAESJavaRA() {
	}

	AONTAESJavaRA(AONTAESJavaRA other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		byte[] key = new byte[AESJavaCodec.KEY_SIZE];

		getKeyFromShards(shards, shardSize, key, getKeySize());
		hashEncryptedAndXORwithKey(key, shardSize - getKeySize(), shards, getSHA256Digest(), getDataShardsNum());

		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

		try {
			Cipher cipherDecrypt = Cipher.getInstance("AES/CTR/PKCS5Padding", getProvider());
			byte[] iv = new byte[AESJavaCodec.IV_SIZE];
			byte[][] output = new byte[getDataShardsNum()][];

//			int i = 0;
			for (int i = 0; i < output.length/2; i++) {
				System.arraycopy(shards[i], 0, iv, 0, AESJavaCodec.IV_SIZE);
				IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
				int encryptedSize = shardSize - AESJavaCodec.IV_SIZE - getKeySize();
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
}
