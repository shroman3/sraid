package com.shroman.secureraid.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.shroman.secureraid.client.BasicBenchmark.testable;
import com.shroman.secureraid.client.DigestBenchmark.digest;

public class DigestBenchmark extends BasicBenchmark<digest> {
	private static int BUFFER_SIZE;
	
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
//		for (Provider provider : Security.getProviders()) {
//			System.out.println(provider.getName());
//			Set<Service> services = provider.getServices();
//			for (Service service : services) {
//				if (service.getType().equals("MessageDigest"))
//					System.out.println("\t" + service.getAlgorithm());			
//			}
//		}
    	int threads = Integer.parseInt(args[0]);
    	BUFFER_SIZE = Integer.parseInt(args[1]) * 1024;
		(new DigestBenchmark(threads)).run();
	}
	
	public static class digest implements testable {
		private String algo;
		private String provider;
		digest(String algo, String provider) {
			this.algo = algo;
			this.provider = provider;
		}

		MessageDigest getMD() {
			try {
				return MessageDigest.getInstance(algo, provider);
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
		}
		@Override
		public String name() {
			return algo + " " + provider;
		}
	}
	
	public DigestBenchmark(int threadsNum) {
		super(threadsNum);
	}

	@Override
	public Iterable<digest> getItems() {
		List<digest> list = new ArrayList<digest>();
		list.add(new digest("SHA-256", "SUN"));
		list.add(new digest("SHA-1", "SUN"));
		list.add(new digest("SHA-256", "BC"));
		list.add(new digest("MD5", "SUN"));
		list.add(new digest("MD5", "BC"));
		return list;
	}

	@Override
	public Runnable buildTest(digest dig, BufferSet buffer) {
		MessageDigest md = dig.getMD();
		return new Runnable() {
			@Override
			public void run() {
				
				int done = 0;
				int stripes = (buffer.inputBuffer.length - 1) / BUFFER_SIZE;
				for (int i = 0; i < stripes; ++i) {
					md.update(buffer.inputBuffer, done, BUFFER_SIZE);
					done += BUFFER_SIZE;
				}
				if (buffer.inputBuffer.length - done > 0) {					
					md.update(buffer.inputBuffer, done, buffer.inputBuffer.length - done);
				}
				md.digest();
			}
		};
	}
}
