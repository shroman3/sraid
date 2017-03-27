package com.shroman.secureraid.codec;

import com.shroman.secureraid.utils.Utils;

public abstract class Codec {
	public static abstract class Builder {
		private Codec codec;

		public abstract Codec build();

		public Builder setParityShardsNum(int parityShardsNum) {
			Utils.validateNotNegative(parityShardsNum, "parity Shards Num");
			codec.parityShardsNum = parityShardsNum;
			return this;
		}

		public Builder setDataShardsNum(int dataShardsNum) {
			Utils.validatePositive(dataShardsNum, "data Shards Num");
			codec.dataShardsNum = dataShardsNum;
			return this;
		}

		protected void validate() {
			Utils.validateNotNegative(codec.parityShardsNum, "parity Shards Num");
			Utils.validatePositive(codec.dataShardsNum, "data Shards Num");
			codec.size = calcSize();
		}

		protected void setCodec(Codec codec) {
			this.codec = codec;
		}

		protected int calcSize() {
			// MDS Property
			return codec.parityShardsNum + codec.dataShardsNum;
		}
	}

	private int size = -1;
	private int dataShardsNum = -1;
	private int parityShardsNum = -1;

	Codec() {
	}

	Codec(Codec other) {
		parityShardsNum = other.parityShardsNum;
		dataShardsNum = other.dataShardsNum;
		size = other.size;
	}

	public abstract byte[][] encode(int shardSize, byte[][] data);

	public abstract byte[][] decode(boolean [] shardPresent, byte[][] data, int shardSize);

	public int getSize() {
		return size;
	}

	public int getDataShardsNum() {
		return dataShardsNum;
	}

	public int getParityShardsNum() {
		return parityShardsNum;
	}
}
