package com.shroman.secret_sharing;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

public class SecretSplit {

	private final SecureRandom random = new SecureRandom();
	private int n;
	private int size;
	private int[] shares;
	private byte[][] buffer = null;
	private byte[][] secret;
	private int z;
	private int threshold;

	public SecretSplit(int n, int z, byte[][] secret, int[] shares) {
		this.n = n;
		this.z = z;
		threshold = z + secret.length;
		this.size = secret[0].length;
		this.secret = secret;
		this.buffer = new byte[threshold][];
		this.shares = shares;
		if (shares == null) {
			shares = ballotShareIndexes();
		}
		fillBuffer();
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
		byte[] share = new byte[size];
		int pos, coefficient;
		int ilog = LogExpTables.logs[shares[shareIndex]];

		for (pos = 0; pos < size; ++pos) {
			share[pos] = buffer[0][pos];
		}

		for (coefficient = 1; coefficient < threshold; ++coefficient) {
			for (pos = 0; pos < size; ++pos) {
				int share_byte = unsignedToBytes(share[pos]);
				if (share_byte != 0)
					share_byte = LogExpTables.exps[ilog + LogExpTables.logs[share_byte]];
				share[pos] = (byte) (share_byte ^ buffer[coefficient][pos]);
			}
		}
		return share;
	}

	// /* Inform a recombination context of a change in share indexes */
	// public static void gfshare_ctx_dec_newshares(byte[] sharenrs) {
	//
	// }
	//
	// /*
	// * Provide a share context with one of the shares. The 'sharenr' is the
	// * index into the 'sharenrs' array
	// */
	// public static void gfshare_ctx_dec_giveshare(byte sharenr, byte[] share)
	// {
	//
	// }
	//
	// /*
	// * Extract the secret by interpolation of the shares. secretbuf must be
	// * allocated and at least 'size' bytes long
	// */
	// public static void gfshare_ctx_dec_extract(GFShareContext ctx, byte[]
	// secretbuf) {
	//
	// }

	private void fillBuffer() {
		for (int i = 0; i < z; i++) {
			buffer[i] = new byte[size];
			// random.nextBytes(buffer[i]);
			// TODO: Change back
			for (int j = 0; j < size; j++) {
				buffer[i][j] = (byte) (j + 1);
			}
		}
		for (int i = 0; i < secret.length; i++) {
			buffer[i + z] = secret[i];
		}
	}

	private int[] ballotShareIndexes() {
		int[] shareIndexes = new int[n];
		Set<Integer> existingShares = new HashSet<>();
		while (existingShares.size() < n) {
			int proposed = (random.nextInt() & 0xff00) >> 8;
			if (proposed == 0) {
				proposed = 1;
			}
			while (existingShares.contains(proposed)) {
				proposed = (proposed + 1) % 254 + 1;
			}
			if (proposed > 255) {
				throw new RuntimeException("Shares calculated wrong: " + existingShares + " new share: " + proposed);
			}
			shareIndexes[existingShares.size()] = proposed;
			existingShares.add(proposed);
		}
		return shareIndexes;
	}

	public static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
}
