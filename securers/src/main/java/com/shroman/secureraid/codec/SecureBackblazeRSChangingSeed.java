package com.shroman.secureraid.codec;

import java.security.SecureRandom;
import java.util.Random;

import com.shroman.secureraid.utils.Utils;

public class SecureBackblazeRSChangingSeed extends SecureBackblazeRS {

	public static class Builder extends SecureBackblazeRS.Builder {
		private SecureBackblazeRSChangingSeed securers;

		public Builder() {
			setCodec(new SecureBackblazeRSChangingSeed());
		}

		Builder(SecureBackblazeRSChangingSeed secureRS) {
			setCodec(new SecureBackblazeRSChangingSeed(secureRS));
		}

		@Override
		public SecureBackblazeRSChangingSeed build() {
			validate();
			return new SecureBackblazeRSChangingSeed(securers);
		}

		protected void setCodec(SecureBackblazeRSChangingSeed securers) {
			super.setCodec(securers);
			this.securers = securers;
		}
	}

	private SecureRandom trueRandom;

	SecureBackblazeRSChangingSeed() {
	}

	SecureBackblazeRSChangingSeed(SecureBackblazeRSChangingSeed other) {
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
