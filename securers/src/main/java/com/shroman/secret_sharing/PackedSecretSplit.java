package com.shroman.secret_sharing;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PackedSecretSplit {

	private final Random random;// = new SecureRandom();
	private int n;
	private int size;
	private int[] shares;
	private byte[][] buffer = null;
	private byte[][] secret;
//	private int z;
	private int threshold;

	public PackedSecretSplit(int n, int z, byte[][] secret, int[] shares, Random random) {
		this.random = random;
		this.n = n;
//		this.z = z;
		threshold = z + secret.length;
		this.size = secret[0].length;
		this.secret = secret;
		this.shares = shares;
		if (shares == null) {
			shares = ballotShareIndexes(n, threshold, random);
		}
		this.buffer = new byte[threshold][];
		fillBuffer();
	}
	
	public static int[] ballotShareIndexes(int n, int threshold, Random random) {
		int[] shareIndexes = new int[n];
		Set<Integer> existingShares = new HashSet<>();
		while (existingShares.size() < n) {
			int proposed = random.nextInt(256 - threshold) + 1;

			if (existingShares.contains(proposed)) {
				continue;
			}
			shareIndexes[existingShares.size()] = proposed;
			existingShares.add(proposed);
		}
		return shareIndexes;
	}
	
	public int getSharePoint(int shareIndex) {
		if (shareIndex >= n || shareIndex < 0) {
			throw new IllegalArgumentException("Share index should be in [0,n)");
		}
		return shares[shareIndex];
	}

	/**
	 * Build a share from the polynomial.
	 * 
	 * @param shareIndex - index of the share
	 * @return a share with the specified index
	 */
	public byte[] getShare(int shareIndex) {
		int x = shares[shareIndex];
		byte[] share = new byte[size];
		for (int i = 0; i < threshold; ++i) {
			// Compute Li(x) (Lagrange Interpolation)
			int LiNumerator = 0, LiDenominator = 0;
			int xi = unsignedToBytes((byte)-i);
			
			for (int j = 0; j < threshold; ++j) {
				if (i == j) {
					continue;
				}
				int xj = unsignedToBytes((byte)-j);
				LiNumerator += LogExpTables.logs[x ^ xj];
				if (LiNumerator >= 0xff) {
					LiNumerator -= 0xff;
				}
				LiDenominator += LogExpTables.logs[xi ^ xj];
				if (LiDenominator >= 0xff) {
					LiDenominator -= 0xff;
				}
			}
			
			if (LiDenominator > LiNumerator) {
				LiNumerator += 0xff;
			}
			int logLi = LiNumerator - LiDenominator;
			
			// Compute yi*Li(x)
			for (int j = 0; j < size; ++j) {
				int bufferByte = unsignedToBytes(buffer[i][j]);
				if (bufferByte != 0) {
					share[j] ^= LogExpTables.exps[logLi + LogExpTables.logs[bufferByte]];
				}
			}
		}
		return share;
	}

	private void fillBuffer() {
		System.arraycopy(secret, 0, buffer, 0, secret.length);
		for (int i = secret.length; i < threshold; i++) {
			buffer[i] = new byte[size];
			random.nextBytes(buffer[i]);
		}
	}

	public static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
}
