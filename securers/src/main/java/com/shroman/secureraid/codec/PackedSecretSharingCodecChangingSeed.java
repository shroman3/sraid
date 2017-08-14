package com.shroman.secureraid.codec;

import java.security.SecureRandom;
import java.util.Random;

import com.shroman.secureraid.utils.Utils;



public class PackedSecretSharingCodecChangingSeed extends PackedSecretSharingCodec {
	public static class Builder extends PackedSecretSharingCodec.Builder {
		private PackedSecretSharingCodecChangingSeed codec;

		public Builder() {
			setCodec(new PackedSecretSharingCodecChangingSeed());
		}

		Builder(PackedSecretSharingCodecChangingSeed secureRS) {
			setCodec(new PackedSecretSharingCodecChangingSeed(secureRS));
		}

		@Override
		public PackedSecretSharingCodecChangingSeed build() {
			validate();
			return new PackedSecretSharingCodecChangingSeed(codec);
		}

		protected void setCodec(PackedSecretSharingCodecChangingSeed codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	private SecureRandom trueRandom;

	PackedSecretSharingCodecChangingSeed() {
	}

	PackedSecretSharingCodecChangingSeed(PackedSecretSharingCodecChangingSeed other) {
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
