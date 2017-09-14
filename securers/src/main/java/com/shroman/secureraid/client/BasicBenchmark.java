package com.shroman.secureraid.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.shroman.secureraid.client.BasicBenchmark.testable;
import com.shroman.secureraid.utils.Utils;



public abstract class BasicBenchmark <T extends testable> {
	private static final int BUFFER_SIZE = 500 * 1024; // Average size in system 
	private static final int PROCESSOR_CACHE_SIZE = 40 * 1024 * 1024;
	private static final int TWICE_PROCESSOR_CACHE_SIZE = 4 * PROCESSOR_CACHE_SIZE;
	private static final long MEASUREMENT_DURATION = 10000;
	
	final int NUMBER_OF_BUFFER_SETS = TWICE_PROCESSOR_CACHE_SIZE / BUFFER_SIZE + 1;
	private int threadsNum;

//	public static void main(String [] args) {
////		Security.addProvider(new BouncyCastleProvider());
////        (new BasicBenchmark()).run();
//    }
	public BasicBenchmark(int threadsNum) {
		this.threadsNum = threadsNum;
	}

	public abstract Iterable<T> getItems();
	
	public abstract Runnable buildTest(T item, BufferSet buffer); 
	
	public void run() {
    	Random random = RandomType.DEV_URANDOM.getRandom();  	
		final BufferSet[] bufferSets = initbufferSets(random);
//    	System.out.println("preparing...");

        List<String> summaryLines = new ArrayList<String>();
        StringBuilder csv = new StringBuilder();
        for (T item : getItems()) {
        	System.out.println("\nTEST: " + item.name());
//        	System.out.println("\nWarmup");
        	buildTest(item, bufferSets[bufferSets.length-1]).run();
//        	System.out.println("    testing...");
        	
        	long bytesEncoded = 0;
        	long encodingTime = 0;
        	while (encodingTime < MEASUREMENT_DURATION) {
        		ExecutorService executor = Utils.buildExecutor(threadsNum, 0);
        		encodingTime += doOneMeasurement(executor, item, bufferSets);
        		bytesEncoded += BUFFER_SIZE*NUMBER_OF_BUFFER_SETS;
        	}
        	
        	double seconds = ((double)encodingTime) / 1000.0;
        	double megabytes = ((double) bytesEncoded) / 1048576.0;
        	
        	Measurement encodeAverage = new Measurement(megabytes, seconds);
        	System.out.println(String.format("\nAVERAGE: %s", encodeAverage));
//        	summaryLines.add(String.format("    %-45s %s", item.name(), encodeAverage));
            // The encoding test should have filled all of the buffers with
            // correct parity, so we can benchmark parity checking.
            csv.append(item.name()).append(",").append(encodeAverage.getRate()).append("\n");
        }

        System.out.println(csv.toString());
//
//        System.out.println("\nSummary:\n");
//        for (String line : summaryLines) {
//            System.out.println(line);
//        }
    }

	protected BufferSet[] initbufferSets(Random random) {
		final BufferSet[] bufferSets = new BufferSet[NUMBER_OF_BUFFER_SETS];
		for (int iBufferSet = 0; iBufferSet < NUMBER_OF_BUFFER_SETS; iBufferSet++) {
			bufferSets[iBufferSet] = new BufferSet(random);
		}
		return bufferSets;
	}

    private long doOneMeasurement(ExecutorService executor, T item, BufferSet[] bufferSets) {
		long startTime = System.currentTimeMillis();
    	for (int i = 0; i < bufferSets.length; i++) {
    		executor.execute(buildTest(item, bufferSets[i]));
		}
		try {
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long encodingTime = System.currentTimeMillis() - startTime;
		
    	double seconds = ((double)encodingTime) / 1000.0;
    	double megabytes = ((double) BUFFER_SIZE*NUMBER_OF_BUFFER_SETS) / 1000000.0;
    	
    	Measurement encodeAverage = new Measurement(megabytes, seconds);
    	System.out.println(encodeAverage);
		return encodingTime;
    }
    
    private static class Measurement {
        private double megabytes;
        private double seconds;

//        public Measurement() {
//            this.megabytes = 0.0;
//            this.seconds = 0.0;
//        }

        public Measurement(double megabytes, double seconds) {
            this.megabytes = megabytes;
            this.seconds = seconds;
        }

//        public void add(Measurement other) {
//            megabytes += other.megabytes;
//            seconds += other.seconds;
//        }

        public double getRate() {
            return megabytes / seconds;
        }

        @Override
        public String toString() {
            return String.format("%5.1f MB/s", getRate());
        }
    }
    
	public static class BufferSet {
		final byte[] inputBuffer = new byte[BUFFER_SIZE];
		final byte[] outputBuffer = new byte[BUFFER_SIZE + 16];
		final byte[] decodedBuffer = new byte[BUFFER_SIZE + 32];

		public BufferSet(Random random) {
			random.nextBytes(inputBuffer);
		}
	}

	public static interface testable {
		String name();
		
	}
}
