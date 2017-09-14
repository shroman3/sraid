package com.shroman.secureraid.client;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.shroman.secureraid.client.BasicBenchmark.testable;
import com.shroman.secureraid.client.CiphersBenchmark.cipher;

public class CiphersBenchmark extends BasicBenchmark<cipher> {
	public static interface Encryptor {
		void encrypt(byte[] in, byte[] out);

		void decrypt(byte[] in, byte[] out);
	}

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
    	int threads = Integer.parseInt(args[0]);
		(new CiphersBenchmark(threads)).run();
	}
	
	private static final String ENCRYPTING = "encrypting";
	private static final String DECRYPTING = "decrypting";
	private byte[] key;
	private byte[] iv;

	public static class cipher implements testable {
		private Encryptors enc;
		private String mode;
		cipher(Encryptors enc, String mode) {
			this.enc = enc;
			this.mode = mode;
		}

		@Override
		public String name() {
			return enc.name() + " " + mode;
		}
		
	}
	
	public CiphersBenchmark(int threadsNum) {
		super(threadsNum);
		Random random = RandomType.DEV_URANDOM.getRandom();
		key = new byte[32];
		iv = new byte[32];
		random.nextBytes(key);
		random.nextBytes(iv);
	}

	@Override
	public Iterable<cipher> getItems() {
		List<cipher> list = new ArrayList<cipher>();
		for (Encryptors cipher : Encryptors.values()) {
			list.add(new cipher(cipher, ENCRYPTING));
			list.add(new cipher(cipher, DECRYPTING));
		}
		return list;
	}

	@Override
	public Runnable buildTest(cipher item, BufferSet buffer) {
		Encryptor encryptor = item.enc.getEncryptor(key, iv);
		if (item.mode.equals(ENCRYPTING)) {
			return new Runnable() {
				
				@Override
				public void run() {
					encryptor.encrypt(buffer.inputBuffer, buffer.outputBuffer);
				}
			};
		} else if (item.mode.equals(DECRYPTING)) {
			return new Runnable() {
				
				@Override
				public void run() {
					encryptor.decrypt(buffer.outputBuffer, buffer.decodedBuffer);
				}
			};
		} else {
			throw new IllegalArgumentException();
		}
	}

	public static class AES implements Encryptor {
		Cipher cipher;
		private IvParameterSpec ivParameterSpec;
		private SecretKeySpec secretKeySpec;

		AES(byte[] key, byte[] iv, String provider, String mode) {
			try {
				cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding", provider);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
				e.printStackTrace();
			}
			if (iv != null) {				
				ivParameterSpec = new IvParameterSpec(iv, 0 ,16);
			}
			secretKeySpec = new SecretKeySpec(key, "AES");
		}

		@Override
		public void encrypt(byte[] in, byte[] out) {
			process(in, out, Cipher.ENCRYPT_MODE);
		}

		@Override
		public void decrypt(byte[] in, byte[] out) {
			process(in, out, Cipher.DECRYPT_MODE);
		}

		private void process(byte[] in, byte[] out, int mode) {
			try {
				if (ivParameterSpec != null) {
					cipher.init(mode, secretKeySpec, ivParameterSpec);
				} else {
					cipher.init(mode, secretKeySpec);
				}
//				int outputSize = cipher.getOutputSize(in.length);
				cipher.doFinal(in, 0, in.length, out, 0);
			} catch (InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException
					| IllegalBlockSizeException | BadPaddingException e) {
				e.printStackTrace();
			}
		}
	}

	public static class StreamEncryptor implements Encryptor {
		CipherParameters cipherParameters;
		private StreamCipher cipher;

		StreamEncryptor(CipherParameters cipherParameters, StreamCipher cipher) {
			this.cipherParameters = cipherParameters;
			this.cipher = cipher;
		}

		@Override
		public void encrypt(byte[] in, byte[] out) {
			process(in, out, true);
		}

		@Override
		public void decrypt(byte[] in, byte[] out) {
			process(in, out, false);
		}

		private void process(byte[] in, byte[] out, boolean mode) {
			try {
				cipher.init(mode, cipherParameters);
				cipher.processBytes(in, 0, in.length, out, 0);
			} catch (DataLengthException e) {
				e.printStackTrace();
			}
		}
	}

	private static enum Encryptors {
		AESJ_ECB_16 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				byte[] key16 = new byte[16];
				System.arraycopy(key, 0, key16, 0, 16);
				return new AES(key16, null, "SunJCE", "ECB");
			}
		},
		AESBC_ECB_16 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				byte[] key16 = new byte[16];
				System.arraycopy(key, 0, key16, 0, 16);
				return new AES(key16, null, "BC", "ECB");
			}
		},
		AESJ_CBC_16 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				byte[] key16 = new byte[16];
				System.arraycopy(key, 0, key16, 0, 16);
				return new AES(key16, iv, "SunJCE", "CBC");
			}
		},
		AESBC_CBC_16 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				byte[] key16 = new byte[16];
				System.arraycopy(key, 0, key16, 0, 16);
				return new AES(key16, iv, "BC", "CBC");
			}
		},
		AESJ_CTR_16 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				byte[] key16 = new byte[16];
				System.arraycopy(key, 0, key16, 0, 16);
				return new AES(key16, iv, "SunJCE", "CTR");
			}
		},
		AESJ_ECB {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, null, "SunJCE", "ECB");
			}
		},
		AESBC_ECB {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, null, "BC", "ECB");
			}
		},
		AESJ_CBC {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, iv, "SunJCE", "CBC");
			}
		},
		AESBC_CBC {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, iv, "BC", "CBC");
			}
		},
		AESJ_CTR {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, iv, "SunJCE", "CTR");
			}
		},
		AESBC_CTR {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, iv, "BC", "CTR");
			}
		},
		CHACHA20 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key, 0, 16), iv, 0, 8);
				return new StreamEncryptor(cipherParameters, new ChaChaEngine());
			}
		},
		SALSA20 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				CipherParameters cipherParameters = new ParametersWithIV(new KeyParameter(key, 0, 16), iv, 0, 8);
				return new StreamEncryptor(cipherParameters, new Salsa20Engine());
			}
		},
		RC4 {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				CipherParameters cipherParameters = new KeyParameter(key, 0, 16);
				return new StreamEncryptor(cipherParameters, new RC4Engine());
			}
		};
		abstract Encryptor getEncryptor(byte[] key, byte[] iv);
	}
}
