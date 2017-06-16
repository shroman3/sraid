package com.shroman.secureraid.codec;


public class SecureEvenodd extends SecureCodec {

	public static class Builder extends SecureCodec.Builder {
		private SecureEvenodd secureEvenodd;

		public Builder() {
			setCodec(new SecureEvenodd());
		}

		Builder(SecureEvenodd secureRS) {
			setCodec(new SecureEvenodd(secureRS));
		}

		@Override
		public SecureEvenodd build() {
			validate();
			return new SecureEvenodd(secureEvenodd);
		}

		protected void setCodec(SecureEvenodd secureEvenodd) {
			super.setCodec(secureEvenodd);
			this.secureEvenodd = secureEvenodd;
		}
	}

	SecureEvenodd() {
	}

	SecureEvenodd(SecureEvenodd other) {
		super(other);

	}

	/* (non-Javadoc)
	 * @see com.shroman.secureraid.codec.Codec#encode(int, byte[][])
	 * return byte[n=super.getSize()][shardSize] 
	 */
	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		byte[][] shards = new byte[getSize()][shardSize];
		//TODO: Encode
		return shards;
	}

	/* (non-Javadoc)
	 * @see com.shroman.secureraid.codec.Codec#decode(boolean[], byte[][], int)
	 * return 
	 */
	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		byte[][] data = new byte[getDataShardsNum()][shardSize];
		//TODO: Decode
		return data;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}
