package com.shroman.secureraid.codec;

import java.security.SecureRandom;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import com.shroman.secureraid.utils.Utils;

public class AONTAESBC extends CryptoCodecConstantKey {
//	public static final int BYTES_IN_MEGABYTE_WITHOUT_PADDING = 1048440;// After padding it is 1048576

	public static class Builder extends CryptoCodecConstantKey.Builder {
		private AONTAESBC codec;

		public Builder() {
			setCodec(new AONTAESBC());
		}

		Builder(AONTAESBC secureRS) {
			setCodec(new AONTAESBC(secureRS));
		}

		@Override
		public AONTAESBC build() {
			validate();
			return new AONTAESBC(codec);
		}

		protected void setCodec(AONTAESBC codec) {
			super.setCodec(codec);
			this.codec = codec;
		}

		@Override
		protected Digest getDigest() {
			return AONTAESJava.getSHA256Digest();
		}
	}
	
	private int keySize;
//	private BlockCipherPadding blockCipherPadding;
	
	AONTAESBC() {
	}

	AONTAESBC(AONTAESBC other) {
		super(other);
		keySize = (int) Math.ceil((double)AESJavaCodec.KEY_SIZE/getDataShardsNum());
//		blockCipherPadding = new PKCS7Padding();
	}
	
	public int getKeySize() {
		return keySize;
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[] key = new byte[AESJavaCodec.KEY_SIZE];
		SecureRandom random = Utils.createTrueRandom();
		random.nextBytes(key);
		BlockCipher blockCipher = new AESEngine();
		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, new PKCS7Padding());
		CipherParameters cipherParameters = new KeyParameter(key);

		try {
			bufferedBlockCipher.init(true, cipherParameters);
			int maximumOutputLength = bufferedBlockCipher.getOutputSize(shardSize);
			byte[][] encrypt = new byte[getDataShardsNum()][maximumOutputLength + keySize];
			
			process(data, bufferedBlockCipher, shardSize, encrypt);
			
			AONTAESJava.hashEncryptedAndXORwithKey(key, maximumOutputLength, encrypt, AONTAESJava.getSHA256Digest(), getDataShardsNum());
			AONTAESJava.addKeyToShards(key, maximumOutputLength, encrypt, keySize);
			return encodeRS(encrypt);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		decodeRS(shardPresent, shards, shardSize);
		byte[] key = new byte[AESJavaCodec.KEY_SIZE];

		AONTAESJava.getKeyFromShards(shards, shardSize, key, keySize);
		AONTAESJava.hashEncryptedAndXORwithKey(key, shardSize - keySize, shards, AONTAESJava.getSHA256Digest(), getDataShardsNum());
		
		// List of BlockCiphers can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/BlockCipher.html
		BlockCipher blockCipher = new AESEngine();

		// Paddings can be found at http://www.bouncycastle.org/docs/docs1.6/org/bouncycastle/crypto/paddings/BlockCipherPadding.html
//		BlockCipherPadding blockCipherPadding = new ZeroBytePadding();

		BufferedBlockCipher bufferedBlockCipher = new PaddedBufferedBlockCipher(blockCipher, new PKCS7Padding());
		CipherParameters cipherParameters = new KeyParameter(key);

		try {
			bufferedBlockCipher.init(false, cipherParameters);
			int maximumOutputLength = bufferedBlockCipher.getOutputSize(shardSize - keySize);
			byte[][] output = new byte[getDataShardsNum()][maximumOutputLength];
			
			process(shards, bufferedBlockCipher, shardSize - keySize, output);
			return output;
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
	
	@Override
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE - ((getDataShardsNum()*AESJavaCodec.IV_SIZE)/2) - ((keySize*getDataShardsNum())/4);
	}

	void process(byte[][] shards, BufferedBlockCipher bufferedBlockCipher, int inputLength, byte[][] output) throws InvalidCipherTextException {		
		for (int i = 0; i < getDataShardsNum(); i++) {
			int processBytes = bufferedBlockCipher.processBytes(shards[i], 0, inputLength, output[i], 0);
			bufferedBlockCipher.doFinal(output[i], processBytes);
			bufferedBlockCipher.reset();
		}
	}
	
//	static void addKeyToShards(byte[] key, int maximumOutputLength, byte[][] encrypt, int keySize) {
//		int j = 0;
//		for (int i = 0; i < key.length; i+=keySize) {
//			System.arraycopy(key, i, encrypt[j++], maximumOutputLength, keySize);
//		}
//	}
//
//	static void getKeyFromShards(byte[][] shards, int shardSize, byte[] key, int keySize) {
//		int j = 0;
//		for (int i = 0; i < key.length; i+=keySize) {
//			System.arraycopy(shards[j++], shardSize - keySize, key, i, keySize);
//		}
//	}
//	
//	static void hashEncryptedAndXORwithKey(byte[] key, int maximumOutputLength, byte[][] encrypt, Digest digest, int dataShards) {
//		byte[] hashed = new byte[digest.getDigestSize()];
//		for (int j = 0; j < dataShards; j++) {
//			digest.update(encrypt[j], 0, maximumOutputLength);		
//			digest.doFinal(hashed, 0);
//			for (int i = 0; i < hashed.length; i++) {
//				key[i] = (byte) (key[i] ^ hashed[i]);
//			}
//		}
//	}
}
