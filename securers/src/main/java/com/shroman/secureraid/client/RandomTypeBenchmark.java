package com.shroman.secureraid.client;

import java.security.Provider;
import java.security.Provider.Service;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomTypeBenchmark {
    private static final int BUFFER_SIZE = 1024 * 4;
    
    private static final long MEASUREMENT_DURATION = 1000;

	public static void main(String [] args) {
        (new RandomTypeBenchmark()).run();
    }

    public void run() {
    	for (Provider provider : Security.getProviders()) {
			System.out.println(provider.getName() + ":");
			for (Service service : provider.getServices()) {				
				if (service.getType().equals("SecureRandom")) {
					System.out.println("\t-" + service.getAlgorithm());
				}
			}
		}
    	SecureRandom secureRandom = new SecureRandom();
		System.out.println("SecureRandom=" + secureRandom.getAlgorithm());

    	
        System.out.println("preparing...");
        final byte [] tempBuffer = new byte [BUFFER_SIZE];

        List<String> summaryLines = new ArrayList<String>();
        StringBuilder csv = new StringBuilder();
        csv.append("Random,Throughput\n");
        for (RandomType randomType : RandomType.values()) {
            Measurement encodeAverage = new Measurement();
            {
                final String testName = randomType.name() + " generating RANDOM";
                System.out.println("\nTEST: " + testName);
                Random random = randomType.buildRandom("random_string$for^generating@numbers");
                System.out.println("		warm up...");
                doOneMeasurement(random, tempBuffer);
                doOneMeasurement(random, tempBuffer);
                System.out.println("    testing...");
                for (int iMeasurement = 0; iMeasurement < 10; iMeasurement++) {
                    encodeAverage.add(doOneMeasurement(random, tempBuffer));
                }
                System.out.println(String.format("\nAVERAGE: %s", encodeAverage));
                summaryLines.add(String.format("    %-45s %s", testName, encodeAverage));
            }
            // The encoding test should have filled all of the buffers with
            // correct parity, so we can benchmark parity checking.
            csv.append(randomType.name());
            csv.append(encodeAverage.getRate());
            csv.append("\n");
        }

        System.out.println("\n");
        System.out.println(csv.toString());

        System.out.println("\nSummary:\n");
        for (String line : summaryLines) {
            System.out.println(line);
        }
    }

    private Measurement doOneMeasurement(Random random, byte [] buffer) {
        long passesCompleted = 0;
        long bytesEncoded = 0;
        long encodingTime = 0;
        while (encodingTime < MEASUREMENT_DURATION) {
        	long startTime = System.currentTimeMillis();
        	random.nextBytes(buffer);
            long endTime = System.currentTimeMillis();
            encodingTime += (endTime - startTime);
            bytesEncoded += BUFFER_SIZE;
            passesCompleted += 1;
        }
        double seconds = ((double)encodingTime) / 1000.0;
        double megabytes = ((double)bytesEncoded) / 1000000.0;
        Measurement result = new Measurement(megabytes, seconds);
        System.out.println(String.format("        %s passes, %s", passesCompleted, result));
        return result;
    }
    
    private static class Measurement {
        private double megabytes;
        private double seconds;

        public Measurement() {
            this.megabytes = 0.0;
            this.seconds = 0.0;
        }

        public Measurement(double megabytes, double seconds) {
            this.megabytes = megabytes;
            this.seconds = seconds;
        }

        public void add(Measurement other) {
            megabytes += other.megabytes;
            seconds += other.seconds;
        }

        public double getRate() {
            return megabytes / seconds;
        }

        @Override
        public String toString() {
            return String.format("%5.1f MB/s", getRate());
        }
    }
}
