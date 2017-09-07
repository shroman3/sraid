package com.shroman.secureraid.securers;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

import com.shroman.secureraid.client.RandomType;
import com.shroman.secureraid.codec.SecureEvenodd;

public class SecureEvenoddTest {

	private static final int SHARD_SIZE = 256;

	@Test
	public void test() {
		int n = 7;
		int k = 3;
		int z = 2;
		int r = 2;
		
		byte[][] data = initData(k, SHARD_SIZE, RandomType.AES.getRandom());
		byte[][] decoded = testEvenodd(n, k, z, r, RandomType.AES, duplicate(data));
		compare(data, decoded);
	}

	private void compare(byte[][] data, byte[][] decoded) {
		for (int i = 0; i < decoded.length; i++) {
			Assert.assertArrayEquals(data[i], decoded[i]);
		}
	}

	private byte[][] duplicate(byte[][] data) {
		byte[][] duplicated = new byte[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			System.arraycopy(data[i], 0, duplicated[i], 0, data[0].length);			
		}
		return duplicated;
	}

	private byte[][] testEvenodd(int n, int k, int z, int r, RandomType random, byte[][] data) {
		SecureEvenodd.Builder builder = new SecureEvenodd.Builder();
		builder.setRandom(random).setSecrecyShardsNum(z)
					.setDataShardsNum(k).setParityShardsNum(r);
		SecureEvenodd secureEvenodd = builder.build();
		
		byte[][] encode = secureEvenodd.encode(SHARD_SIZE, data);
		
		boolean[] present = getrandomShardsPresent(n, k+z, random.getRandom());
		byte[][] decode = secureEvenodd.decode(present, encode, SHARD_SIZE);
		return decode;
	}

	private boolean[] getrandomShardsPresent(int n, int presentNum, Random random) {
		boolean[] bs = new boolean[n];
		int count = 0;
		while (count < presentNum) {
			int nextInt = random.nextInt(n);
			if (!bs[nextInt]) {
				count++;
				bs[nextInt] = true;
			}
		}
		return bs;
	}

	private byte[][] initData(int k, int size, Random random) {
		byte[][] data = new byte[k][size];
		for (byte[] shard : data) {
			random.nextBytes(shard);
		}
		return data;
	}
}
