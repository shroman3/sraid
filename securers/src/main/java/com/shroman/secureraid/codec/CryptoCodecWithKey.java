package com.shroman.secureraid.codec;

import java.security.SecureRandom;

import com.shroman.secureraid.utils.Utils;

public abstract class CryptoCodecWithKey extends CryptoCodec {
	public static abstract class Builder extends CryptoCodec.Builder {
		public abstract CryptoCodecWithKey build();
	}

	private SecureRandom random = null;

	public abstract int getKeySize(); 

	CryptoCodecWithKey() {
	}

	CryptoCodecWithKey(CryptoCodecWithKey other) {
		super(other);
		random = Utils.createTrueRandom();
	}

	@Override
	public byte[] generateKey() {
		byte[] key = new byte[getKeySize()];
		random.nextBytes(key);
		return key;
	}
	
	@Override
	public boolean isKeyNeeded() {
		return true;
	}
}
