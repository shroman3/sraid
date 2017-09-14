package com.shroman.secureraid.client;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.shroman.secureraid.client.BasicBenchmark.testable;
import com.shroman.secureraid.client.RandomBenchmark.random;

public class RandomBenchmark extends BasicBenchmark<random> {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
    	int threads = Integer.parseInt(args[0]);
		(new RandomBenchmark(threads)).run();
	}
	
	public static class random implements testable {
		private RandomType rand;
		random(RandomType rand) {
			this.rand = rand;
		}

		@Override
		public String name() {
			return rand.name();
		}
	}
	
	public RandomBenchmark(int threadsNum) {
		super(threadsNum);
	}

	@Override
	public Iterable<random> getItems() {
		List<random> list = new ArrayList<random>();
		for (RandomType rand : RandomType.values()) {
			list.add(new random(rand));
		}
		return list;
	}

	@Override
	public Runnable buildTest(random rand, BufferSet buffer) {
		Random prng = rand.rand.getRandom();
		return new Runnable() {
			@Override
			public void run() {
				prng.nextBytes(buffer.outputBuffer);
			}
		};
	}
}
