package com.shroman.secureraid.codec;

import com.shroman.secret_sharing.SecretCombine;
import com.shroman.secret_sharing.SecretSplit;



public class SecretSharingCodec extends SecureCodec {
	public static class Builder extends SecureCodec.Builder {
		private SecretSharingCodec codec;

		public Builder() {
			setCodec(new SecretSharingCodec());
		}

		Builder(SecretSharingCodec secureRS) {
			setCodec(new SecretSharingCodec(secureRS));
		}

		@Override
		public SecretSharingCodec build() {
			validate();
			return new SecretSharingCodec(codec);
		}

		protected void setCodec(SecretSharingCodec codec) {
			super.setCodec(codec);
			this.codec = codec;
		}
	}

	private int[] shareIndexes = null;

	SecretSharingCodec() {
	}

	SecretSharingCodec(SecretSharingCodec other) {
		super(other);
		shareIndexes  = new int[getSize()];
		for (int i = 0; i < shareIndexes.length; i++) {
			shareIndexes[i] = getRandom().nextInt() & 0xFF;
		}
	}

	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] shares = new byte[getSize()][];
		SecretSplit split = new SecretSplit(getSize(), getSecrecyShardsNum(), data, shareIndexes);
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
		
		SecretCombine combine = new SecretCombine(shareIndexes, shares);
		return combine.extractSecret(getDataShardsNum());
	}
}
