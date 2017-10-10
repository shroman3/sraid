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

	void printShard(int shardSize, byte[][] shards, int size){
		for(int j=0; j<shardSize; j++){
			for(int i=0; i<size; i++){
				System.out.print(shards[i][j]);
				System.out.print("	");
			}
			System.out.print("\n");
		}
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

		byte[][] shards = new byte[getSize()][shardSize];
		initializationKeys(shards, shardSize);
		// here I start computing the generator matrix of evenodd*
		parityPropagate(shards, shardSize);
		int numberOfChunks = 1 + (shardSize / (p - 1)); // each chunk is of p-1 rows 
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			EVENODDSTAR(shardSize, shards, p, chunk);
			// now columns 0 and 1 are the Random Key and column 2 to p-1 are
			// the message encryption key
			hideData(shards, data, shardSize, chunk);
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
		byte[][] data = dataArrayInitializtion(shardSize);
		int failures[] = { -1, -1, -1 };
		failuresLocalization(failures, shardPresent);
		int numberOfChunks = 1 + (shardSize / (p - 1)); 
		if(failures[2]==-1){
			RecverOneFailure(shards, shardSize, numberOfChunks, failures);
		}
		else{
			RecoverTwoFailures(shards, shardSize, numberOfChunks, failures);
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

	private void initializationKeys(byte[][] shards, int shardSize){
		Random random = getRandom();
		shards[0] = new byte[shardSize];
		random.nextBytes(shards[0]);
		shards[shards.length-1] = new byte[shardSize];
		random.nextBytes(shards[shards.length-1]);
		for (int i = getDataShardsNum() + getSecrecyShardsNum(); i < shards.length; i++) {
			shards[i] = new byte[shardSize];
		}
	}
	
	private void hideData(byte[][] shards, byte[][] data, int shardSize, int chunk){
		for (int i = 0; i < getDataShardsNum(); i++) { // columns
			for (int j = 0; j < p - 1 && (chunk * (p - 1) + j) < shardSize; j++) { // rows
				shards[i + getSecrecyShardsNum()][j + chunk * (p - 1)] ^= data[i][j + chunk * (p - 1)];
			}
		}
	}
	
	private void parityPropagate(byte[][] shards, int shardSize){
		for (int i = 1; i < shards.length - 2; i++) {
			for (int j = 0; j < shardSize; j++) {
				shards[i][j] ^= shards[0][j];
			}
		}
	}
	
	private byte[][] dataArrayInitializtion(int shardSize){
		byte[][] data = new byte[getDataShardsNum()][shardSize];
		for (int i = 0; i < getDataShardsNum(); i++) {
			data[i] = new byte[shardSize];
		}
		return data;
	}
	
	private void failuresLocalization(int[] failures, boolean[] shardPresent){
		int indexOfFailure = 1;
		for (int i = 0; i < getSize(); i++) {
			if (!shardPresent[i]) {
				failures[indexOfFailure] = i;
				indexOfFailure++;
			}
		}
	}
	
	private void case1ReEncode(byte[][] shards, int shardSize, int numberOfChunks){
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			EVENODD(shardSize, shards, p, chunk);

		}
	}
	
	private void case2HorizntalandReEncode(byte[][] shards, int shardSize, int numberOfChunks, int[] failures){
		// using horizontal parity and than re-econding the last colum
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			for (int i = 0; i < p - 1 && (i + chunk * (p - 1)) < shardSize; i++) {
				// first we recover the data disk using the horizontal parity
				RecoverByHorizontalParity(shards, shardSize, chunk, failures, i);
				reEnocdeLastColumn(shards, shardSize, chunk, i);
			}
		}
	}
	
	private byte sDiagonalByUnaffectedDiagonal(byte[][] shards, int shardSize,
			int chunk,int[] failures){
		byte mainDiagonal = 0;
		int rowIndex = (failures[1] - 1) % p;
		if (rowIndex < 0) {
			rowIndex += p;
		}
		if (rowIndex + (chunk * (p - 1)) < shardSize && rowIndex != p - 1) {
			mainDiagonal ^= shards[shards.length - 1][rowIndex + (chunk * (p - 1))];
		}
		for (int i = 0; i < p && i < getDataShardsNum() + 2; i++) { 
			rowIndex = (failures[1] - 1 - i) % p;
			if (rowIndex < 0) {
				rowIndex += p;
			}

			if (rowIndex + (chunk * (p - 1)) < shardSize && rowIndex != p - 1) {
				mainDiagonal ^= shards[i][rowIndex + (chunk * (p - 1))];
			}
		}
		return mainDiagonal;
	}
	
	private void reEncodingByUnaffectedDiag
		(byte[][] shards, int shardSize, int chunk, int faileDisk, byte sDiagonal){
		// now re-encoding the 1st failed disk using the unaffected
		// diagonal
		int startingRow=chunk*(p-1);
		for (int i=0; i<p-1 && (i+startingRow)<shardSize; i++){
			int currentRow=i+startingRow;
			shards[faileDisk][currentRow]=0;
			shards[faileDisk][currentRow]^=sDiagonal;
			if ((i+faileDisk)%p!=p-1&&((i+faileDisk)%p+startingRow<shardSize)){
				shards[faileDisk][(currentRow)]^=shards[shards.length-1][(i+faileDisk)%p
						+startingRow];
			}
			for(int j=0; j<faileDisk; j++){
				if ((faileDisk+i-j)%p+startingRow<shardSize
					&&(faileDisk+i-j)%p!=p-1){
					shards[faileDisk][currentRow]^=shards[j][(faileDisk+i-j)%p
										+startingRow];
					}
			}
			for(int j=faileDisk+1; j<p && j<(getDataShardsNum()+2); j++){
				int rowIndex=(faileDisk+i-j)%p;
				if(rowIndex<0){
					rowIndex+=p;
				}
				if(rowIndex+startingRow<shardSize && rowIndex!=p-1){
					shards[faileDisk][currentRow]^=shards[j][rowIndex+startingRow];
				}
			}
			// now we can re-encode the parity disk in the chunk
			shards[shards.length-2][currentRow]=0;
			for (int j=0; j<getDataShardsNum()+2; j++) { 
				shards[shards.length-2][currentRow]^=shards[j][currentRow];
			}
		}		
	}
	
	private void case3UnaffectedDiagonal(byte[][] shards, int shardSize,
			int numberOfChunks,int[] failures){
		// CASE 3: the failed disks are the parity disk and another data disks
		// there is always an unaffected diagonal we can use
		// first lets compute the S diagonal
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			byte sDiagonal=sDiagonalByUnaffectedDiagonal(shards, shardSize, chunk, failures);
			reEncodingByUnaffectedDiag(shards, shardSize, numberOfChunks, failures[1], sDiagonal);
		}

	}
	
	private void reEnocdeLastColumn(byte[][] shards, int shardSize, int chunk, int i){
		byte mainDiagonal = 0;
		for (int j = 0; j < p  ; j++) { // colums
			// main diagonal OF EVENODD
			if ((chunk * (p - 1) + j) < shardSize) {
				if (j != p - 1 && (p - (j + 1)) < getDataShardsNum() + 2) {
					mainDiagonal ^= shards[p - (j + 1)][j + (chunk * (p - 1))];
				}
				// The Second Diagonal of EVENODD
				if ((p - (j - i)) % p < (getDataShardsNum() + 2)) {
					shards[shards.length-1][(chunk * (p - 1) + i)] ^= shards[(p - (j - i)) % p][j
							+ (chunk * (p - 1))];
				}
			}
		}
		shards[shards.length-1][(chunk * (p - 1) + i)] ^= mainDiagonal;
	}
	
	private void RecoverByHorizontalParity(byte[][] shards, int shardSize, int chunk, int[] failures, int i){
		shards[failures[1]][(i + chunk * (p - 1))] = 0;
		for (int j = 0; j < failures[1]; j++) {
			shards[failures[1]][i + chunk * (p - 1)] ^= shards[j][i + chunk * (p - 1)];
		}
		for (int j = failures[1] + 1; j <shards.length - 1 ; j++) {
			shards[failures[1]][i + chunk * (p - 1)] ^= shards[j][i + chunk * (p - 1)];
		}
	}
	
	private byte mainDiagonalByRedundencyDisks(byte[][] shards, int shardSize, int chunk){
		byte mainDiagonal=0;
		for (int r = 0; r < p - 1 && r + chunk * (p - 1) < shardSize; r++) {
			// as discussed in the paper - the main Diagonal is XOR of
			// the two last colls
			mainDiagonal ^= shards[shards.length - 1][r + chunk * (p - 1)];
			mainDiagonal ^= shards[shards.length - 2][r + chunk * (p - 1)];
		}
		return mainDiagonal; 
	}
	
	private void recoverHighColumn(byte[][] shards, int shardSize, int chunk,
							int currentRow, byte mainDiagonal, int[] failures){
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
	}
	
	private void recoverLowColumnByParity(byte[][] shards, int shardSize,
								int chunk, int currentRow, int[] failures){
		// now we compute the other failure using the horizontal
		// parity
		shards[failures[1]][currentRow + chunk * (p - 1)] = 0;
		for (int i = 0; i < failures[1]; i++) {
			shards[failures[1]][currentRow + chunk * (p - 1)] ^= shards[i][currentRow + chunk * (p - 1)];
		}
		for (int i = failures[1] + 1; i < p + 1 && i < (getDataShardsNum() + 3); i++) {
			shards[failures[1]][currentRow + chunk * (p - 1)] ^= shards[i][currentRow + chunk * (p - 1)];
		}

	}
	
	private void case4twoDataFailures(byte[][] shards, int shardSize, int numberOfChunks, int[] failures){
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			byte mainDiagonal = mainDiagonalByRedundencyDisks(shards, shardSize, chunk);
			
			int currentRow = (-(failures[2] - failures[1]) - 1) % p;
			if (currentRow < 0) {
				currentRow += p;
			}
			
			while (currentRow < p - 1 && currentRow + chunk * (p - 1) < shardSize) {
				recoverHighColumn(shards, shardSize, chunk, currentRow, mainDiagonal, failures);
				recoverLowColumnByParity(shards, shardSize, chunk, currentRow, failures);
				currentRow = (currentRow - (failures[2] - failures[1])) % p;
				if (currentRow < 0) {
					currentRow += p;
				}
			}
		}
	}
	
	private void initiateEncryptionTable(byte[][] encryptionTable,
			byte[][] shards, int shardSize, int totalDisksNum){
		for (int i = 0; i < getSecrecyShardsNum(); i++) {
			encryptionTable[i] = new byte[shardSize];
		}
		// first we copy the keys to the ecryption table
		System.arraycopy(shards, 0, encryptionTable, 0, 1);
		System.arraycopy(shards, 1, encryptionTable, 1, 1);
		// initating the ecryptionTable 
		for (int i = 2; i < totalDisksNum - 1; i++) {
			encryptionTable[i] = new byte[shardSize];
			for (int j = 0; j < shardSize; j++) {
				encryptionTable[i][j] ^= encryptionTable[0][j];
			}
		}
	}
	
	private void computeLastCol(byte[][] encryptionTable,int shardSize,
			int totalDisksNum, int chunk){
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
		
	}
	
	private void decryptMsg(byte[][] shards, byte[][] data,
			byte[][] encryptionTable, int chunk, int shardSize){
		for (int i = 0; i < getDataShardsNum() && (chunk * (p - 1) + i) < shardSize; i++) {
			for (int j = 0; j < p - 1 && (chunk * (p - 1) + j) < shardSize; j++) {
				data[i][chunk * (p - 1) + j] ^= shards[i + 2][chunk * (p - 1) + j];
				data[i][chunk * (p - 1) + j] ^= encryptionTable[i + 2][chunk * (p - 1) + j];
			}
		}
	}
	
	private void RecverOneFailure(byte[][] shards, int shardSize,
			int numberOfChunks,int[] failures){
		// One Failure Only
		if(failures[1]==shards.length-1){
			/* in this case we need to re-encode the last column */
			for (int chunk = 0; chunk < numberOfChunks; chunk++) {
				for (int i = 0; i < p - 1 && (i + chunk * (p - 1)) < shardSize; i++) {
					reEnocdeLastColumn(shards, shardSize, chunk, i);
				}
			}
		}
		else{
			/* in this case we need to use the pairty to recover */ 
			for (int chunk = 0; chunk < numberOfChunks; chunk++) {
				for (int i = 0; i < p - 1 && (i + chunk * (p - 1)) < shardSize; i++) {
					RecoverByHorizontalParity(shards, shardSize, chunk, failures, i);
				}
			}
		}
	}
	
	private void RecoverTwoFailures(byte[][] shards, int shardSize,
			int numberOfChunks, int[] failures){
		if (failures[2] == (getSize() - 1)) { 
			if (failures[1] == (getSize() - 2)) {
				// CASE 1: two last columns are failed
				case1ReEncode(shards, shardSize, numberOfChunks);
			} else { 
				// CASE 2: last column and data column are failed
				case2HorizntalandReEncode(shards, shardSize, numberOfChunks, failures);
			}

		} else if (failures[2]==getDataShardsNum()+2) {
			// CASE 3: one data disk and pairty disk are failed
			case3UnaffectedDiagonal(shards, shardSize, numberOfChunks, failures);
			
		} else { 
			/* CASE 4: both failures are of data disks */
			case4twoDataFailures(shards, shardSize, numberOfChunks, failures);
		}
	}
	
	public byte[][] decrypt(byte[][] shards, int shardSize, byte[][] data) {
		// stage 1 - Lets compute the encryption table from the keys
		int totalDisksNum = getSize(); 
		byte[][] encryptionTable = new byte[totalDisksNum][shardSize];
		initiateEncryptionTable(encryptionTable, shards, shardSize, totalDisksNum);
		int numberOfChunks = 1 + (shardSize / (p - 1)); 
		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			computeLastCol(encryptionTable, shardSize, totalDisksNum, chunk);
			EVENODDSTAR(shardSize, encryptionTable, p, chunk);
			decryptMsg(shards, data, encryptionTable, chunk, shardSize);
		}
		/* end of decrypt */
		return data;
	}
}
