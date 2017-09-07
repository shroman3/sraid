package com.shroman.secureraid.client;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
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

public class EncryptionBenchmark {
	public static interface Encryptor {
		void encrypt(byte[] in, byte[] out);

		void decrypt(byte[] in, byte[] out);
	}

	private static final int BUFFER_SIZE = 1000 * 200;
	private static final int PROCESSOR_CACHE_SIZE = 128 * 1024 * 1024;
	private static final int TWICE_PROCESSOR_CACHE_SIZE = 2 * PROCESSOR_CACHE_SIZE;
	private static final long MEASUREMENT_DURATION = 1000;
	private final int NUMBER_OF_BUFFER_SETS = TWICE_PROCESSOR_CACHE_SIZE / BUFFER_SIZE + 1;
	private int nextBuffer = 0;

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		for(Provider provider : Security.getProviders()) {
			System.out.println(provider.getName());
			for (Service service : provider.getServices()) {
				if (service.getType().equals("Cipher")) {
					System.out.println("\t-" + service.getType() + "\t-" + service.getAlgorithm());
				}
			}
		}
		(new EncryptionBenchmark()).run();
	}

	public void run() {
		System.out.println("preparing...");

		Random random = RandomType.DEV_URANDOM.getRandom();
		final BufferSet[] bufferSets = new BufferSet[NUMBER_OF_BUFFER_SETS];
		for (int iBufferSet = 0; iBufferSet < NUMBER_OF_BUFFER_SETS; iBufferSet++) {
			bufferSets[iBufferSet] = new BufferSet(random);
		}

		List<String> summaryLines = new ArrayList<String>();
		StringBuilder csv = new StringBuilder();
		csv.append("Cipher,EncryptThroughput,DecryptThroughput\n");

		byte[] key = new byte[32];
		byte[] iv = new byte[32];
		random.nextBytes(key);
		random.nextBytes(iv);

		for (Encryptors cipherType : Encryptors.values()) {
			Measurement encryptAverage = new Measurement();
			{
				final String testName = cipherType.name() + " ENCRYPT";
				System.out.println("\nTEST: " + testName);
				Encryptor cipher = cipherType.getEncryptor(key, iv);
				System.out.println("		warm up...");
				doOneEncryptMeasurement(cipher, bufferSets);
				doOneEncryptMeasurement(cipher, bufferSets);
				System.out.println("    testing...");
				for (int iMeasurement = 0; iMeasurement < 10; iMeasurement++) {
					encryptAverage.add(doOneEncryptMeasurement(cipher, bufferSets));
				}
				System.out.println(String.format("\nAVERAGE: %s", encryptAverage));
				summaryLines.add(String.format("    %-45s %s", testName, encryptAverage));
			}

			Measurement decryptAverage = new Measurement();
			{
				final String testName = cipherType.name() + " DECRYPT";
				System.out.println("\nTEST: " + testName);
				Encryptor cipher = cipherType.getEncryptor(key, iv);

				System.out.println("    warm up...");
				doOneDecryptMeasurement(cipher, bufferSets);
				doOneDecryptMeasurement(cipher, bufferSets);
				// System.out.println(" testing...");
				for (int iMeasurement = 0; iMeasurement < 10; iMeasurement++) {
					decryptAverage.add(doOneDecryptMeasurement(cipher, bufferSets));
				}
				// System.out.println(String.format("\nAVERAGE: %s",
				// checkAverage));
				summaryLines.add(String.format("    %-45s %s", testName, decryptAverage));
			}
			// The encoding test should have filled all of the buffers with
			// correct parity, so we can benchmark parity checking.
			csv.append(cipherType.name()).append(",").append(encryptAverage.getRate()).append(",")
					.append(decryptAverage.getRate()).append("\n");
		}

		System.out.println("\n");
		System.out.println(csv.toString());

		System.out.println("\nSummary:\n");
		for (String line : summaryLines) {
			System.out.println(line);
		}
	}

	private Measurement doOneEncryptMeasurement(Encryptor cipher, BufferSet[] bufferSets) {
		long passesCompleted = 0;
		long bytesEncoded = 0;
		long encodingTime = 0;
		while (encodingTime < MEASUREMENT_DURATION) {
			BufferSet bufferSet = bufferSets[nextBuffer];
			nextBuffer = (nextBuffer + 1) % bufferSets.length;

			long startTime = System.currentTimeMillis();
			cipher.encrypt(bufferSet.inputBuffer, bufferSet.outputBuffer);
			long endTime = System.currentTimeMillis();
			encodingTime += (endTime - startTime);
			bytesEncoded += BUFFER_SIZE;
			passesCompleted += 1;
		}

		double seconds = ((double) encodingTime) / 1000.0;
		double megabytes = ((double) bytesEncoded) / 1000000.0;
		Measurement result = new Measurement(megabytes, seconds);
		System.out.println(String.format("        %s passes, %s", passesCompleted, result));
		return result;
	}

	private Measurement doOneDecryptMeasurement(Encryptor cipher, BufferSet[] bufferSets) {
		long passesCompleted = 0;
		long bytesEncoded = 0;
		long encodingTime = 0;
		while (encodingTime < MEASUREMENT_DURATION) {
			BufferSet bufferSet = bufferSets[nextBuffer];
			nextBuffer = (nextBuffer + 1) % bufferSets.length;

			long startTime = System.currentTimeMillis();
			cipher.encrypt(bufferSet.outputBuffer, bufferSet.decodedBuffer);
			long endTime = System.currentTimeMillis();
			encodingTime += (endTime - startTime);
			bytesEncoded += BUFFER_SIZE;
			passesCompleted += 1;
		}

		double seconds = ((double) encodingTime) / 1000.0;
		double megabytes = ((double) bytesEncoded) / 1000000.0;
		Measurement result = new Measurement(megabytes, seconds);
		System.out.println(String.format("        %s passes, %s", passesCompleted, result));
		return result;
	}

	private static class Measurement {
		private double megabytes;
		private double seconds;

		public Measurement() {
			this.megabytes = 0.0;
			this.seconds = 0.0;
		}

		public Measurement(double megabytes, double seconds) {
			this.megabytes = megabytes;
			this.seconds = seconds;
		}

		public void add(Measurement other) {
			megabytes += other.megabytes;
			seconds += other.seconds;
		}

		public double getRate() {
			return megabytes / seconds;
		}

		@Override
		public String toString() {
			return String.format(" %5.1f MB/s", getRate());
		}
	}

	private class BufferSet {
		final byte[] inputBuffer = new byte[BUFFER_SIZE];
		final byte[] outputBuffer = new byte[BUFFER_SIZE + 32];
		final byte[] decodedBuffer = new byte[BUFFER_SIZE + 64];

		public BufferSet(Random random) {
			random.nextBytes(inputBuffer);
		}
	}

	public static class AES implements Encryptor {
		Cipher cipher;
		private IvParameterSpec ivParameterSpec;
		private SecretKeySpec secretKeySpec;

		AES(byte[] key, byte[] iv, String provider) {
			try {
				cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
				e.printStackTrace();
			}
			ivParameterSpec = new IvParameterSpec(iv, 0 ,16);
			secretKeySpec = new SecretKeySpec(key, 0, 32, "AES");
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
				cipher.init(mode, secretKeySpec, ivParameterSpec);
				cipher.doFinal(in, 0, in.length, out, 0);
			} catch (InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException
					| IllegalBlockSizeException | BadPaddingException e) {
				e.printStackTrace();
			}
		}
	}

//	public static class AES2 implements Encryptor {
//		CipherParameters cipherParameters;
//		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(new AESEngine(), new PKCS7Padding());
//
//		AES2(byte[] key, byte[] iv) {
//			cipherParameters = new KeyParameter(key, 0, 32);
//		}
//
//		@Override
//		public void encrypt(byte[] in, byte[] out) {
//			process(in, out, true);
//		}
//
//		@Override
//		public void decrypt(byte[] in, byte[] out) {
//			process(in, out, false);
//		}
//
//		private void process(byte[] in, byte[] out, boolean mode) {
//			try {
//				bufferedBlockCipher.init(mode, cipherParameters);
//				int processBytes = bufferedBlockCipher.processBytes(in, 0, in.length, out, 0);
//				bufferedBlockCipher.doFinal(out, processBytes);
//			} catch (DataLengthException | IllegalStateException | InvalidCipherTextException e) {
//				e.printStackTrace();
//			}
//		}
//	}

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
		AESJ {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, iv, "SunJCE");
			}
		},
		AESBC {
			@Override
			Encryptor getEncryptor(byte[] key, byte[] iv) {
				return new AES(key, iv, "BC");
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
