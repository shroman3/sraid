package com.shroman.secureraid.codec;

import com.shroman.secret_sharing.PackedSecretCombine;



public class PackedSecretSharingCodecRA extends PackedSecretSharingCodec {
	public static class Builder extends PackedSecretSharingCodec.Builder {
		private PackedSecretSharingCodecRA codec;

		public Builder() {
			setCodec(new PackedSecretSharingCodecRA());
		}

		Builder(PackedSecretSharingCodecRA secureRS) {
			setCodec(new PackedSecretSharingCodecRA(secureRS));
		}

		@Override
		public PackedSecretSharingCodecRA build() {
			validate();
			return new PackedSecretSharingCodecRA(codec);
		}

		protected void setCodec(PackedSecretSharingCodecRA codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}


	PackedSecretSharingCodecRA() {
	}

	PackedSecretSharingCodecRA(PackedSecretSharingCodecRA other) {
		super(other);
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		PackedSecretCombine combine = buildPSCombine(shardPresent, shards);

		byte[][] secret = new byte[getDataShardsNum()][];
		secret[0] = combine.extractSecret(0);
		return secret;
	}
}
