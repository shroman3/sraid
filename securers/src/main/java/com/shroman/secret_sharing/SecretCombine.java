package com.shroman.secret_sharing;

public class SecretCombine {
	private int size;
	private int[] shareIndexes;
	private int threshold;
	private byte[][] shares;

	public SecretCombine(int[] shareIndexes, byte[][] sharesData) {
		this.shareIndexes = shareIndexes;
		this.shares = sharesData;
		threshold = sharesData.length;
		this.size = sharesData[0].length;
	}

	/**
	 * Combines the shares and calculates the secret using interpolation.
	 * @param k - works only for k=1,2
	 * @return secret
	 */
	public byte[][] extractSecret(int k) {
		byte[][] secret = new byte[k][size];

		if (k == 1) {
			simpleSecretSharing(secret);
		} else {
			rampSecretSharing(secret);
		}
		return secret;
	}

	private void rampSecretSharing(byte[][] secret) {
		for (int i = 0; i < threshold; ++i) {
			// Compute Li(0) (Lagrange Interpolation)
			int LiNumerator = 0, LiDenominator = 0;
			// Compute the sum for first derivative Li'(x) = Li(x)*Sum{1/x-xj} 
			int DLiDenominator = 0;
			
			for (int j = 0; j < threshold; ++j) {
				if (i == j) {
					continue;
				}
				LiNumerator += LogExpTables.logs[shareIndexes[j]];
				if (LiNumerator >= 0xff) {
					LiNumerator -= 0xff;
				}
				LiDenominator += LogExpTables.logs[(shareIndexes[i]) ^ (shareIndexes[j])];
				if (LiDenominator >= 0xff) {
					LiDenominator -= 0xff;
				}
				int invertedXj = (-LogExpTables.logs[shareIndexes[j]]) + 0xff;
				DLiDenominator ^= LogExpTables.exps[invertedXj];
			}
			
			if (LiDenominator > LiNumerator) {
				LiNumerator += 0xff;
			}
			int logLi = LiNumerator - LiDenominator;
			int logDLi = logLi + LogExpTables.logs[DLiDenominator];
			if (logDLi > 0xff) {
				logDLi -= 0xff;
			}
			
			for (int j = 0; j < size; ++j) {
				int share_byte = unsignedToBytes(shares[i][j]);
				if (share_byte != 0) {
					secret[0][j] ^= LogExpTables.exps[logLi + LogExpTables.logs[share_byte]];
					secret[1][j] ^= LogExpTables.exps[logDLi + LogExpTables.logs[share_byte]];
				}
			}
		}
	}

	private void simpleSecretSharing(byte[][] secret) {
		for (int i = 0; i < threshold; ++i) {
			// Compute Li(0) (Lagrange Interpolation)
			int LiNumerator = 0, LiDenominator = 0;
			
			for (int j = 0; j < threshold; ++j) {
				if (i == j) {
					continue;
				}
				LiNumerator += LogExpTables.logs[shareIndexes[j]];
				if (LiNumerator >= 0xff) {
					LiNumerator -= 0xff;
				}
				LiDenominator += LogExpTables.logs[(shareIndexes[i]) ^ (shareIndexes[j])];
				if (LiDenominator >= 0xff) {
					LiDenominator -= 0xff;
				}
			}
			
			if (LiDenominator > LiNumerator) {
				LiNumerator += 0xff;
			}
			int logLi = LiNumerator - LiDenominator;
			
			for (int j = 0; j < size; ++j) {
				int share_byte = unsignedToBytes(shares[i][j]);
				if (share_byte != 0) {
					secret[0][j] ^= LogExpTables.exps[logLi + LogExpTables.logs[share_byte]];
				}
			}
		}
	}

	public static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
}
