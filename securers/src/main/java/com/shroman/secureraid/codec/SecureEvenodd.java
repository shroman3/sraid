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

		@Override
		protected void validate() {
			super.validate();
			if (secureEvenodd.getSecrecyShardsNum() > 2) {
				throw new IllegalArgumentException("Secure EVENODD z should be up to 2, given z=" + secureEvenodd.getSecrecyShardsNum());
			}
			if (secureEvenodd.getParityShardsNum() > 2) {
				throw new IllegalArgumentException("Secure EVENODD r should be up to 2, given r=" + secureEvenodd.getSecrecyShardsNum());
			}
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
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			shards[i] = new byte[shardSize];
			getRandom().nextBytes(shards[i]);
		}
		System.arraycopy(data, 0, shards, getSecrecyShardsNum(), getDataShardsNum());
		for (int i = getDataShardsNum() + getSecrecyShardsNum(); i < shards.length; i++) {
			shards[i] = new byte[shardSize];
		}
		
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
