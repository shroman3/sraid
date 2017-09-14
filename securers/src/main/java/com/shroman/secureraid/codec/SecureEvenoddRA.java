package com.shroman.secureraid.codec;

public class SecureEvenoddRA extends SecureEvenodd {

	public static class Builder extends SecureEvenodd.Builder {
		private SecureEvenoddRA secureEvenodd;

		public Builder() {
			setCodec(new SecureEvenoddRA());
		}

		Builder(SecureEvenoddRA secureRS) {
			setCodec(new SecureEvenoddRA(secureRS));
		}

		@Override
		public SecureEvenoddRA build() {
			validate();
			return new SecureEvenoddRA(secureEvenodd);
		}

		@Override
		protected void validate() {
			super.validate();
			if (secureEvenodd.getSecrecyShardsNum() > 2) {
				throw new IllegalArgumentException(
						"Secure EVENODD z should be up to 2, given z=" + secureEvenodd.getSecrecyShardsNum());
			}
			if (secureEvenodd.getParityShardsNum() > 2) {
				throw new IllegalArgumentException(
						"Secure EVENODD r should be up to 2, given r=" + secureEvenodd.getSecrecyShardsNum());
			}
		}

		protected void setCodec(SecureEvenoddRA secureEvenodd) {
			super.setCodec(secureEvenodd);
			this.secureEvenodd = secureEvenodd;
		}
	}

	private int p = nearestPrime(getDataShardsNum() + 2);

	SecureEvenoddRA() {
	}

	SecureEvenoddRA(SecureEvenoddRA other) {
		super(other);

	}

	
	public static int nearestPrime(int x) {
		if (isPrime(x)) {
			return x;
		}

		if (x % 2 == 0) {
			x += 1;
		}
		int p = x;
		for (;; p += 2) {
			if (isPrime(p)) {
				return p;
			}
		}
	}

	public static boolean isPrime(int n) {
		if (n % 2 == 0)
			return false;
		for (int i = 3; i * i <= n; i += 2)
			if (n % i == 0)
				return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.shroman.secureraid.codec.Codec#decode(boolean[], byte[][], int)
	 * return
	 */
	@Override
	public byte[][] decode(boolean[] shardPresent, byte[][] shards, int shardSize) {
		byte[][] data = new byte[getDataShardsNum()][shardSize];
		return decrypt(shards, shardSize, data);
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
	
	@Override
	public int getBytesInMegaBeforePadding() {
		int shard = ((BYTES_IN_MEGABYTE*4)/getDataShardsNum());
		shard -=  shard % (p-1);
		int i = (shard*getDataShardsNum())/4;
		return i;
	}

	private void EVENODD(int shardSize, byte[][] shards, int p, int chunk) {
		for (int i = 0; i < p - 1 && (chunk * (p - 1) + i) < shardSize; i++) {
			shards[shards.length - 1][(chunk * (p - 1) + i)] = 0;
			shards[shards.length - 2][(chunk * (p - 1) + i)] = 0;
		}
		for (int i = 0; i < p - 1 && (chunk * (p - 1) + i) < shardSize; i++) { // rows

			byte mainDiagonal = 0;
			for (int j = 0; j < p; j++) { // colums
				// parity
				if (j < getDataShardsNum() + 2) {
					shards[shards.length - 2][(chunk * (p - 1) + i)] ^= shards[j][chunk * (p - 1) + i];
				}
				// main diagonal OF EvenODD
				if ((chunk * (p - 1) + j) < shardSize) {
					if (j != p - 1) {
						if (p - (j + 1) < getDataShardsNum() + 2) {
							mainDiagonal ^= shards[p - (j + 1)][j + (chunk * (p - 1))];
						}
						// The Second Diagonal of EVENODD
						if (((p - (j - i)) % p) < getDataShardsNum() + 2) {
							shards[shards.length - 1][(chunk * (p - 1) + i)] ^= shards[(p - (j - i)) % p][j
									+ (chunk * (p - 1))];
						}
					}

				}
			}
			shards[shards.length - 1][(chunk * (p - 1) + i)] ^= mainDiagonal;
		}
	}

	void EVENODDSTAR(int shardSize, byte[][] shards, int p, int chunk) {
		// TODO: pay attention that this function is not the complete EVENODD*
		// code, there is one step
		// construction of EVENODD*
		for (int i = 0; i < p - 1; i++) {

			// the main diagonal //
			for (int j = 0; j < p - 1 && (chunk * (p - 1) + j) < shardSize; j++) {
				if ((chunk * (p - 1) + i) < shardSize && (p - (j + 1)) < getDataShardsNum() + 2) {
					shards[p - (j + 1)][j + (chunk * (p - 1))] ^= shards[shards.length - 1][i + (chunk * (p - 1))];
				}

			}
			// TODO: do I need 2 different for loops?
			// the other diagonal of EVENODD* //
			for (int j = 0; j < p - 1 && (chunk * (p - 1) + j) < shardSize; j++) {
				if (p - (j - i) == p) { // * may cause a performance problem!
										// *//
					if ((chunk * (p - 1) + i) < shardSize && (p - (j - i)) < getDataShardsNum() + 2) {
						shards[(p - (j - i))][j + (chunk * (p - 1))] ^= shards[shards.length - 1][i
								+ (chunk * (p - 1))];
					}
				} else {
					if ((chunk * (p - 1) + i) < shardSize && (p - (j - i)) % p < getDataShardsNum() + 2) {
						shards[(p - (j - i)) % p][j + (chunk * (p - 1))] ^= shards[shards.length - 1][i
								+ (chunk * (p - 1))];
					}

				}
			}
		}
	}

	public byte[][] decrypt(byte[][] shards, int shardSize, byte[][] data) {
		// decrypt
		// stage 1 - Lets compute the encryption table from the keys
//		int secrecyNum = getSecrecyShardsNum();
//		int parityNum = getParityShardsNum();
		// int p=getDataShardsNum()+2; // data num is p-2
		int totalDisksNum = getSize(); // p+2

		byte[][] encryptionTable = new byte[totalDisksNum][shardSize];
		for (int i = 0; i < totalDisksNum; i++) {
			encryptionTable[i] = new byte[shardSize];
		}

		// first we copy the keys to the ecryption table
		System.arraycopy(shards, 0, encryptionTable, 0, 1);
		System.arraycopy(shards, 1, encryptionTable, 1, 1);

		// initating the ecryptionTable TODO: ask Roman - performance
		for (int i = 2; i < totalDisksNum - 1; i++) {
			// encryptionTable[i] = new byte[shardSize];
			for (int j = 0; j < shardSize; j++) {
				encryptionTable[i][j] = 0;
				encryptionTable[i][j] ^= encryptionTable[0][j];
			}
		}
		/* not so helpful but should be considered */
		for (int j = 0; j < shardSize; j++) {
			encryptionTable[totalDisksNum - 1][j] = 0;
		}
		/* not so helpful but should be considered */
		// now lets reconstruct the encryptionTable

		int numberOfChunks = 1 + (shardSize / (p - 1)); // each construction is
														// of p-1 rows
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {

			// first we compute the last column
			for (int j = 1; j < p - 1; j++) {
				if ((chunk * (p - 1) + j) < shardSize) {
					// each symbol in row j is a XOR of the symbols in 1st and
					// 2nd row j-1
					encryptionTable[totalDisksNum - 1][j + (chunk * (p - 1))] ^= encryptionTable[0][j - 1
							+ (chunk * (p - 1))];
					encryptionTable[totalDisksNum - 1][j + (chunk * (p - 1))] ^= encryptionTable[1][j - 1
							+ (chunk * (p - 1))];
					encryptionTable[totalDisksNum - 1][0 + (chunk * (p - 1))] ^= encryptionTable[totalDisksNum - 1][j
							+ (chunk * (p - 1))];
				} else if ((chunk * (p - 1) + j) == shardSize && (p - 2 + chunk * (p - 1)) > shardSize
						&& (p - j) < getDataShardsNum() + 2) {
					encryptionTable[totalDisksNum - 1][0 + (chunk * (p - 1))] ^= encryptionTable[0][j - 1
							+ (chunk * (p - 1))];
					encryptionTable[totalDisksNum - 1][0 + (chunk * (p - 1))] ^= encryptionTable[p - j][j - 1
							+ (chunk * (p - 1))];
				}
			}
			if ((p - 2 + chunk * (p - 1)) < shardSize) {
				// computation of the first row of the last colum
				encryptionTable[totalDisksNum - 1][0 + (chunk * (p - 1))] ^= encryptionTable[0][p - 2
						+ (chunk * (p - 1))];
				encryptionTable[totalDisksNum - 1][0 + (chunk * (p - 1))] ^= encryptionTable[1][p - 2
						+ (chunk * (p - 1))];
			}
			EVENODDSTAR(shardSize, encryptionTable, p, chunk);
			// EVENODD* is READY
			// now colums 2 to p-1 are the msg decryption key
			// computing the original msg
			for (int i = 0; i < getDataShardsNum() && (chunk * (p - 1) + i) < shardSize; i++) {
				for (int j = 0; j < p - 1 && (chunk * (p - 1) + j) < shardSize; j++) {
					data[i][chunk * (p - 1) + j] = 0;
					data[i][chunk * (p - 1) + j] ^= shards[i + 2][chunk * (p - 1) + j];
					data[i][chunk * (p - 1) + j] ^= encryptionTable[i + 2][chunk * (p - 1) + j];
				}
			}
		}
		/* end of decrypt */
		return data;
	}
}
