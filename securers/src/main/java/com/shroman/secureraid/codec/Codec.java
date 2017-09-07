package com.shroman.secureraid.codec;

import com.shroman.secureraid.utils.Utils;

public abstract class Codec {
	public static final int BYTES_IN_MEGABYTE = 1048576;//No padding

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

	public abstract byte[][] encode(int shardSize, byte[][] data, byte[] key);

	protected abstract byte[][] decode(boolean [] shardPresent, byte[][] shards, int shardSize, byte[] key);

	public abstract byte[] generateKey();
	
	public abstract boolean isKeyNeeded();
	
	public int getSize() {
		return size;
	}

	public int getDataShardsNum() {
		return dataShardsNum;
	}

	public int getParityShardsNum() {
		return parityShardsNum;
	}
	
	public int getBytesInMegaBeforePadding() {
		return BYTES_IN_MEGABYTE;
	}
	
	public byte[][] decode(byte[][] shards, int shardSize, byte[] key) {
		return decode(chunksPresent(shards, shardSize), shards, shardSize, key);
	}
	
	public boolean hasRandomRead() {
		return false;
	}
	
	protected boolean[] chunksPresent(byte[][] chunks, int chunkSize) {
		boolean[] shardPresent = new boolean[size];
		int shardsCount = 0;
		for (int j = 0; j < (size) && shardsCount < (size-parityShardsNum); ++j) {
			shardPresent[j] = (chunks[j] != null);
			if (shardPresent[j]) {
				shardsCount++;
			} else {
				chunks[j] = new byte[chunkSize];
			}
		}
		return shardPresent;
	}
}
