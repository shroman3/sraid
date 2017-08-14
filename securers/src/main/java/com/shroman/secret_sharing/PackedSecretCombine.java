package com.shroman.secret_sharing;

//import com.shroman.secureraid.codec.let;

public class PackedSecretCombine {
	private int size;
	private int[] shareIndexes;
	private int threshold;
	private byte[][] shares;

	public PackedSecretCombine(int[] shareIndexes, byte[][] sharesData) {
		this.shareIndexes = shareIndexes;
		this.shares = sharesData;
		threshold = sharesData.length;
		this.size = sharesData[0].length;
	}

	/**
	 * Combines the shares and calculates the secret using interpolation.
	 * @param secretIndex - index of the secret to reconstruct
	 * @return secret
	 */
	public byte[] extractSecret(int secretIndex) {
		byte[] secret = new byte[size];
		for (int i = 0; i < threshold; ++i) {
			// Compute Li(0) (Lagrange Interpolation)
			int LiDenominator = 0, LiNumerator = 0;
			
			for (int j = 0; j < threshold; ++j) {
				if (i == j) {
					continue;
				}
				LiNumerator += LogExpTables.logs[(shareIndexes[j]) ^ unsignedToBytes((byte)-secretIndex)];
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
					secret[j] ^= LogExpTables.exps[logLi + LogExpTables.logs[share_byte]];
				}
			}
		}
		return secret;
	}

	
////	public byte[][] compute_newton_coefficients(byte[][] points, byte[][] values/*shards*/) {
//////	    assert_eq!(points.len(), values.len());
////
//////	    let mut store: Vec<(usize, usize, i64)> =
//////	        values.iter().enumerate().map(|(index, &value)| (index, index, value)).collect();
////		int[][] indexes_lower = new int[values.length][values[0].length];
////		int[][] indexes_upper = new int[values.length][values[0].length];
////		
////		for (int i = 0; i < indexes_lower.length; i++) {
////			for (int j = 0; j < indexes_lower[i].length; j++) {
////				indexes_lower[i][j] = i;
////				indexes_upper[i][j] = i;
////			}
////		}
////		for (int k = 0; k < values[0].length; k++) {	
////			for (int j = 0; j < values.length; j++) {
////				for (int i = values.length-1; i >= 0 ; i--) {
////					int index_lower = indexes_lower[i - 1][k];
////					int index_upper  = indexes_upper[i][k];
////	
////		            int point_lower = points[index_lower][k];
////		            int point_upper = points[index_upper][k];
////		            int point_diff = (point_upper - point_lower) & 0xff;
////		            int point_diff_inverse = mod_inverse(point_diff, prime);
////	
////		            int coef_lower = values[i - 1][k];
////		            int coef_upper = values[i][k];
////		            int coef_diff = (coef_upper - coef_lower) % prime;
////	
////		            let fraction = (coef_diff * point_diff_inverse) % prime;
////	
////		            store[i] = (index_lower, index_upper, fraction);
////					
////				}	
////			}
////		}
////	}
////		for j in 1..store.len() {
////	        for i in (j..store.len()).rev() {
////	            let index_lower = store[i - 1].0;
////	            let index_upper = store[i].1;
////
////	            let point_lower = points[index_lower];
////	            let point_upper = points[index_upper];
////	            let point_diff = (point_upper - point_lower) % prime;
////	            let point_diff_inverse = mod_inverse(point_diff, prime);
////
////	            let coef_lower = store[i - 1].2;
////	            let coef_upper = store[i].2;
////	            let coef_diff = (coef_upper - coef_lower) % prime;
////
////	            let fraction = (coef_diff * point_diff_inverse) % prime;
////
////	            store[i] = (index_lower, index_upper, fraction);
////	        }
////	    }
////
////	    store.iter().map(|&(_, _, v)| v).collect()
////	}
//	
//	/// Inverse of `k` in the *Zp* field defined by `prime`.
//	public int mod_inverse(int k) {
//		int inverted = (-LogExpTables.logs[k]) + 0xff;
//		return LogExpTables.exps[inverted];
//	}
//	
	public static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
}
