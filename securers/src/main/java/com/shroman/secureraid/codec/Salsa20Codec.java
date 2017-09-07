package com.shroman.secureraid.codec;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.Salsa20Engine;

public class Salsa20Codec extends ChaChaCodec {
	public static class Builder extends ChaChaCodec.Builder {
		private Salsa20Codec codec;

		public Builder() {
			setCodec(new Salsa20Codec());
		}

		Builder(Salsa20Codec codec) {
			setCodec(new Salsa20Codec(codec));
		}

		@Override
		public Salsa20Codec build() {
			validate();
			return new Salsa20Codec(codec);
		}

		protected void setCodec(Salsa20Codec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}
	
	Salsa20Codec() {
	}

	Salsa20Codec(Salsa20Codec other) {
		super(other);
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}

	@Override
	protected StreamCipher getEngine() {
		return new Salsa20Engine();
	}
}
