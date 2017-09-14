package com.shroman.secureraid.codec;

import java.util.Random;

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
				throw new IllegalArgumentException(
						"Secure EVENODD z should be up to 2, given z=" + secureEvenodd.getSecrecyShardsNum());
			}
			if (secureEvenodd.getParityShardsNum() > 2) {
				throw new IllegalArgumentException(
						"Secure EVENODD r should be up to 2, given r=" + secureEvenodd.getSecrecyShardsNum());
			}
		}

		protected void setCodec(SecureEvenodd secureEvenodd) {
			super.setCodec(secureEvenodd);
			this.secureEvenodd = secureEvenodd;
		}
	}

	private int p = nearestPrime(getDataShardsNum() + 2);

	SecureEvenodd() {
	}

	SecureEvenodd(SecureEvenodd other) {
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
	 * @see com.shroman.secureraid.codec.Codec#encode(int, byte[][]) return
	 * byte[n=super.getSize()][shardSize]
	 */
	@Override
	public byte[][] encode(int shardSize, byte[][] data) {
		if (shardSize % (p-1) != 0) {
			throw new IllegalArgumentException("Shardsize doesn't match allignment EVENODD, geven shardsize " + shardSize + " for p=" + p);
		}
		Random random = getRandom();

		byte[][] shards2 = new byte[getSize()][shardSize];
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			shards2[i] = new byte[shardSize];
			random.nextBytes(shards2[i]);
		}
		System.arraycopy(data, 0, shards2, getSecrecyShardsNum(), getDataShardsNum());
		for (int i = getDataShardsNum() + getSecrecyShardsNum(); i < shards2.length; i++) {
			shards2[i] = new byte[shardSize];
		}

		// Omer's Project //
		byte[][] shards = new byte[getSize()][shardSize];
		// copying the 1st collum of random values
		System.arraycopy(shards2, 0, shards, 0, 1);
		// copying the 2nd collum of random values to the last collum
		System.arraycopy(shards2, 1, shards, shards2.length - 1, 1);
		for (int i = 1; i < shards.length - 1; i++) {
			for (int j = 0; j < shardSize; j++) {
				shards[i][j] = 0;
			}
		}

		// here I start computing the generator matrix of evenodd*
		// first c1 - the "parity" disk
		for (int i = 1; i < shards.length - 2; i++) {
			for (int j = 0; j < shardSize; j++) {
				shards[i][j] ^= shards[0][j];
			}
		}
		int secrecyNum = getSecrecyShardsNum();
//		int parityNum = getParityShardsNum();
		// int p=getDataShardsNum()+2; // data num is p-2
		int numberOfChunks = 1 + (shardSize / (p - 1)); // each construction is
														// of p-1 rows

		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			EVENODDSTAR(shardSize, shards, p, chunk);
			// now collums 0 and 1 are the Random Key and column 2 to p-1 are
			// the message encryption key
			// here we hide the message

			for (int i = 0; i < getDataShardsNum(); i++) { // columns
				for (int j = 0; j < p - 1 && (chunk * (p - 1) + j) < shardSize; j++) { // rows
					shards[i + secrecyNum][j + chunk * (p - 1)] ^= data[i][j + chunk * (p - 1)];
				}
			}
			EVENODD(shardSize, shards, p, chunk);
		}

		return shards;
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
		for (int i = 0; i < getDataShardsNum(); i++) {
			data[i] = new byte[shardSize];
		}
		// int p=getSize()-2;

		// finding the failures
		int failures[] = { -1, -1, -1 };
		int indexOfFailure = 1;
		for (int i = 0; i < getSize(); i++) {
			if (!shardPresent[i]) {
				failures[indexOfFailure] = i;
				indexOfFailure++;
			}
		}
		int numberOfChunks = 1 + (shardSize / (p - 1)); // each construction is
														// of p-1 rows

		if (failures[2] == (getSize() - 1)) { // last collum is failed
			if (failures[1] == (getSize() - 2)) {
				// re-encode!
				for (int chunk = 0; chunk < numberOfChunks; chunk++) {
					EVENODD(shardSize, shards, p, chunk);

				}

			} else { // case failure[1]<p failure[2]=p+1

				// using horizontal parity and than re-econding the last colum
				for (int chunk = 0; chunk < numberOfChunks; chunk++) {

					// first we recover the failure of disk number i<p
					for (int i = 0; i < p - 1 && (i + chunk * (p - 1)) < shardSize; i++) {

						shards[failures[1]][(i + chunk * (p - 1))] = 0;
						shards[failures[2]][(chunk * (p - 1) + i)] = 0;
						for (int j = 0; j < failures[1]; j++) {
							shards[failures[1]][i + chunk * (p - 1)] ^= shards[j][i + chunk * (p - 1)];
						}
						for (int j = failures[1] + 1; j < p + 1; j++) {
							shards[failures[1]][i + chunk * (p - 1)] ^= shards[j][i + chunk * (p - 1)];
						}
						// after finishing the recovery of disk i<p we can
						// re-enocde again the last collum
						byte mainDiagonal = 0;
						for (int j = 0; j < p /* && (chunk*(p-1)+j)<shardSize */ ; j++) { // colums
							// main diagonal OF EvenODD
							if ((chunk * (p - 1) + j) < shardSize) {
								if (j != p - 1 && (p - (j + 1)) < getDataShardsNum() + 2) {
									mainDiagonal ^= shards[p - (j + 1)][j + (chunk * (p - 1))];
								}
								// The Second Diagonal of EVENODD
								if ((p - (j - i)) % p < (getDataShardsNum() + 2)) {
									shards[failures[2]][(chunk * (p - 1) + i)] ^= shards[(p - (j - i)) % p][j
											+ (chunk * (p - 1))];
								}
							}

						}
						shards[failures[2]][(chunk * (p - 1) + i)] ^= mainDiagonal;
					}
				}
			}

		} else if (failures[2] == getDataShardsNum() + 2) {
			// using the diagonals we restoring failures[1] and then 2
			// there is always an unaffected diagonal we can use
			// first lets compute the main diagonal
			for (int chunk = 0; chunk < numberOfChunks; chunk++) {
				byte mainDiagonal = 0;
				int rowIndex = (failures[1] - 1) % p;
				if (rowIndex < 0) {
					rowIndex += p;
				}
				if (rowIndex + (chunk * (p - 1)) < shardSize && rowIndex != p - 1) {
					mainDiagonal ^= shards[shards.length - 1][rowIndex + (chunk * (p - 1))];
				}
				for (int i = 0; i < p && i < getDataShardsNum() + 2; i++) { // rows
																			// -
																			// computing
																			// the
																			// mainDiagonal
																			// //
																			// TODO:
																			// cosider
																			// if
																			// the
																			// role
																			// about
																			// data+2
																			// should
																			// be
																			// in
																			// the
																			// for
																			// loop
					rowIndex = (failures[1] - 1 - i) % p;
					if (rowIndex < 0) {
						rowIndex += p;
					}

					if (rowIndex + (chunk * (p - 1)) < shardSize && rowIndex != p - 1) {
						mainDiagonal ^= shards[i][rowIndex + (chunk * (p - 1))];
					}
				}
				// now re-encoding the 1st failed disk using the unaffected
				// diagonal
				for (int i = 0; i < p - 1 && (i + (chunk * (p - 1))) < shardSize; i++) {
					shards[failures[1]][(i + (chunk * (p - 1)))] = 0;
					shards[failures[1]][(i + (chunk * (p - 1)))] ^= mainDiagonal;

					if ((i + failures[1]) % p != p - 1 && ((i + failures[1]) % p + (chunk * (p - 1))) < shardSize) {
						shards[failures[1]][(i + (chunk * (p - 1)))] ^= shards[shards.length - 1][((i + failures[1]) % p
								+ (chunk * (p - 1)))];
					}

					for (int j = 0; j < failures[1]; j++) {
						if ((failures[1] + (i) - j) % p + (chunk * (p - 1)) < shardSize
								&& (failures[1] + (i) - j) % p != p - 1) {
							shards[failures[1]][(i + (chunk * (p - 1)))] ^= shards[j][(failures[1] + (i) - j) % p
									+ (chunk * (p - 1))];
						}
					}
					for (int j = failures[1] + 1; j < p && j < (getDataShardsNum() + 2); j++) {
						rowIndex = (failures[1] + (i) - j) % p;
						if (rowIndex < 0) {
							rowIndex += p;
						}
						if (rowIndex + (chunk * (p - 1)) < shardSize && rowIndex != p - 1) {
							shards[failures[1]][(i + (chunk * (p - 1)))] ^= shards[j][rowIndex + (chunk * (p - 1))];
						}
					}
					// now we can re-encode the parity disk in the chunk
					shards[shards.length - 2][i + chunk * (p - 1)] = 0;
					for (int j = 0; j < getDataShardsNum() + 2; j++) { // TODO:
																		// is it
																		// ok to
																		// be in
																		// the
																		// for
																		// loop
																		// or as
																		// "if"?
						shards[shards.length - 2][i + chunk * (p - 1)] ^= shards[j][i + ((chunk * (p - 1)))];
					}
				}

			}
		} else { // both failures are less than < p
					// finding the existing diagonal
			System.out.println("hereeee\n\n\n\n\n\n\n\n\n\n\n here");

			for (int chunk = 0; chunk < numberOfChunks; chunk++) {
				byte mainDiagonal = 0;
				for (int r = 0; r < p - 1 && r + chunk * (p - 1) < shardSize; r++) {
					// as discussed in the paper - the main Diagonal is XOR of
					// the two last colls
					mainDiagonal ^= shards[shards.length - 1][r + chunk * (p - 1)];
					mainDiagonal ^= shards[shards.length - 2][r + chunk * (p - 1)];
				}
				// int currentRow=(failures[2]-failures[1]-1)%p;
				int currentRow = (-(failures[2] - failures[1]) - 1) % p;
				if (currentRow < 0) {
					currentRow += p;
				}
				while (currentRow < p - 1 && currentRow + chunk * (p - 1) < shardSize) {
					shards[failures[2]][currentRow + chunk * (p - 1)] = 0;
					// XORing the main Diagonal
					shards[failures[2]][currentRow + chunk * (p - 1)] ^= mainDiagonal;
					// first lets XOR the parity of the last column
					int rowOf2ndParity = (failures[2] + currentRow) % p;
					if (rowOf2ndParity + chunk * (p - 1) < shardSize && rowOf2ndParity < p - 1) {
						shards[failures[2]][currentRow + chunk * (p - 1)] ^= shards[shards.length - 1][rowOf2ndParity
								+ chunk * (p - 1)];
					}

					for (int i = 0; i < failures[1]; i++) {
						int rowOf2P = (rowOf2ndParity - i) % p;
						if (rowOf2P < 0) {
							rowOf2P += p;
						}
						if ((rowOf2P) < p - 1 && rowOf2P + chunk * (p - 1) < shardSize) {
							shards[failures[2]][currentRow + chunk * (p - 1)] ^= shards[i][(rowOf2P) + chunk * (p - 1)];
						}
					}
					for (int i = failures[1] + 1; i < failures[2]; i++) {
						int rowOf2P = (rowOf2ndParity - i) % p;
						if (rowOf2P < 0) {
							rowOf2P += p;
						}
						if (rowOf2P < p - 1 && rowOf2P + chunk * (p - 1) < shardSize) {
							shards[failures[2]][currentRow + chunk * (p - 1)] ^= shards[i][rowOf2P + chunk * (p - 1)];
						}
					}
					for (int i = failures[2] + 1; i < p && i < (getDataShardsNum() + 2); i++) {
						int rowOf2P = (rowOf2ndParity - i) % p;
						if (rowOf2P < 0) {
							rowOf2P += p;
						}
						if (rowOf2P < p - 1 && rowOf2P + chunk * (p - 1) < shardSize) {
							shards[failures[2]][currentRow + chunk * (p - 1)] ^= shards[i][(rowOf2P) + chunk * (p - 1)];
						}
					}
					if ((currentRow + (failures[2] - failures[1])) % p < p - 1
							&& (currentRow + (failures[2] - failures[1])) % p + chunk * (p - 1) < shardSize) {
						shards[failures[2]][currentRow
								+ chunk * (p - 1)] ^= shards[failures[1]][(currentRow + (failures[2] - failures[1])) % p
										+ chunk * (p - 1)];

					}

					// now we compute the other failure using the horizontal
					// parity
					shards[failures[1]][currentRow + chunk * (p - 1)] = 0;
					for (int i = 0; i < failures[1]; i++) {
						shards[failures[1]][currentRow + chunk * (p - 1)] ^= shards[i][currentRow + chunk * (p - 1)];
					}
					for (int i = failures[1] + 1; i < p + 1 && i < (getDataShardsNum() + 3); i++) {
						shards[failures[1]][currentRow + chunk * (p - 1)] ^= shards[i][currentRow + chunk * (p - 1)];
					}
					currentRow = (currentRow - (failures[2] - failures[1])) % p;
					if (currentRow < 0) {
						currentRow += p;
					}
				}

			}

		}
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
