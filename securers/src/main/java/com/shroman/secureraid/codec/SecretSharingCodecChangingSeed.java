package com.shroman.secureraid.codec;

import java.security.SecureRandom;
import java.util.Random;

import com.shroman.secureraid.utils.Utils;



public class SecretSharingCodecChangingSeed extends SecretSharingCodec {
	public static class Builder extends SecretSharingCodec.Builder {
		private SecretSharingCodecChangingSeed codec;

		public Builder() {
			setCodec(new SecretSharingCodecChangingSeed());
		}

		Builder(SecretSharingCodecChangingSeed secureRS) {
			setCodec(new SecretSharingCodecChangingSeed(secureRS));
		}

		@Override
		public SecretSharingCodecChangingSeed build() {
			validate();
			return new SecretSharingCodecChangingSeed(codec);
		}

		protected void setCodec(SecretSharingCodecChangingSeed codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	private SecureRandom trueRandom;

	SecretSharingCodecChangingSeed() {
	}

	SecretSharingCodecChangingSeed(SecretSharingCodecChangingSeed other) {
		super(other);
		trueRandom = Utils.createTrueRandom();
	}
	
	@Override
	public Random getRandom() {
		Random random = super.getRandom();
		random.setSeed(trueRandom.nextLong());
		return random;
	}

}
