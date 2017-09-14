package com.shroman.secureraid.client;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESRandom extends Random {

	private static final long serialVersionUID = 5979766988573925049L;

	private IvParameterSpec ivParameterSpec;
	private SecretKeySpec secretKeySpec;

	private Cipher cipher;

	public AESRandom() {
		super();
	}

	public AESRandom(byte[] key, byte[] iv) {
		ivParameterSpec = new IvParameterSpec(iv, 0 ,16);
		secretKeySpec = new SecretKeySpec(key, 0, 32, "AES");
		try {
			cipher = Cipher.getInstance("AES/CTR/PKCS5Padding", "SunJCE");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void nextBytes(byte[] bytes) {
		try {
			/*byte[] final1 = */cipher.doFinal(bytes,0, bytes.length-16,bytes, 0);
//			System.arraycopy(bytes, 0, final1, 0, bytes.length);
		} catch (IllegalBlockSizeException | BadPaddingException | ShortBufferException e) {
			e.printStackTrace();
		}
	}
}
