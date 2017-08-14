package com.shroman.secureraid.codec;

import java.util.Random;

import com.shroman.secret_sharing.PackedSecretCombine;
import com.shroman.secret_sharing.PackedSecretSplit;



public class PackedSecretSharingCodec extends SecureCodec {
	public static class Builder extends SecureCodec.Builder {
		private PackedSecretSharingCodec codec;

		public Builder() {
			setCodec(new PackedSecretSharingCodec());
		}

		Builder(PackedSecretSharingCodec secureRS) {
			setCodec(new PackedSecretSharingCodec(secureRS));
		}

		@Override
		public PackedSecretSharingCodec build() {
			validate();
			return new PackedSecretSharingCodec(codec);
		}

		protected void setCodec(PackedSecretSharingCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	private int[] shareIndexes = null;

	PackedSecretSharingCodec() {
	}

	PackedSecretSharingCodec(PackedSecretSharingCodec other) {
		super(other);
		shareIndexes  = PackedSecretSplit.ballotShareIndexes(getSize(), getDataShardsNum() + getSecrecyShardsNum(), new Random(19981908));
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] shares = new byte[getSize()][];
		PackedSecretSplit split = new PackedSecretSplit(getSize(), getSecrecyShardsNum(), data, shareIndexes, getRandom());
		for (int i = 0; i < getSize(); i++) {
			shares[i] = split.getShare(i);
		}
		return shares;
	}

	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		int[] shareIndexes = new int[getDataShardsNum() + getSecrecyShardsNum()];
		byte[][] shares = new byte[shareIndexes.length][];
		int sharesPresent = 0;
		for (int i = 0; i < shardPresent.length && (sharesPresent < shareIndexes.length); i++) {
			if (shardPresent[i]) {
				shares[sharesPresent] = shards[i]; 
				shareIndexes[sharesPresent] = this.shareIndexes[i];
				++sharesPresent;
			}
		}
		
		PackedSecretCombine combine = new PackedSecretCombine(shareIndexes, shares);
		byte[][] secret = new byte[getDataShardsNum()][];
		for (int i = 0; i < secret.length; i++) {
			secret[i] = combine.extractSecret(i); 
		}
		return secret;
	}
}
