package com.shroman.secureraid.securers;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import com.shroman.secureraid.client.RandomType;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		
//		ReedSolomon secure = new ReedSolomon(2, 4, new InputOutputByteTableCodingLoop());
//		ReedSolomon parity = new ReedSolomon(4, 2, new InputOutputByteTableCodingLoop());
//
//		byte[][] data = new byte[6][1];
//		data[2][0] = 5;
//		data[3][0] = (byte) 255;
//
//		data[1][0] = 127;
//		data[0][0] = 32;
//		secure.encodePartialParity(data, 0, 1, 2);
//
//		parity.encodeParity(data, 0, 1);
//		//
//		// secure.encodePartialParity(data, 0, 1, 2);
//		// assertTrue(data[2][0]==5);
//		// assertTrue((data[3][0]& 0xff) == 255);
//		//
//		data[5][0] = 0;
//		data[1][0] = 0;
//		boolean[] shardsPresent = new boolean[6];
//		shardsPresent[0] = true;
//		shardsPresent[1] = false;
//		shardsPresent[2] = true;
//		shardsPresent[3] = true;
//		shardsPresent[4] = true;
//		shardsPresent[5] = false;
//
//		parity.decodeMissing(data, shardsPresent, 0, 1);
//		assertTrue(data[1][0] == 127);

//		final ArrayBlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(2);
//		new Thread(new Runnable() {
//			public void run() {
//				for (int i = 0; i < 5; i++) {
//					try {
//						blockingQueue.put(i);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}).start();
//		
//		while (true) {
//			System.out.println(blockingQueue.remove());
//		}
	}
	
	public void testRandomGenerators() {
		for (RandomType random : RandomType.values()) {
			random.buildRandom("randomKey");
		}
	}
}
