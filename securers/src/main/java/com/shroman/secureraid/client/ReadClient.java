package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.Response;

public class ReadClient extends Thread implements PushResponseInterface {
	private static final String CHECKSUMS_FILENAME = "checksums.ser";
	private static final int N_THREADS = 2;
	private Codec codec;
	private Map<Integer, long[]> checksumMap = new ConcurrentHashMap<>();
	private Map<Integer, byte[][]> readMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> chunksSet = new ConcurrentHashMap<>();
	private Integer mutex = -1;
	private boolean die = false;
	private ExecutorService executer;
	private int shouldPresent;
	private Logger logger;

	ReadClient(Codec codec) {
		this.codec = codec;
		shouldPresent = codec.getSize() - codec.getParityShardsNum();
		logger = Logger.getLogger("Decode");
	}

	@Override
	public void push(Response response, int serverId) {
		if (response.isSuccess() && response.getData() != null) {
			synchronized (mutex) {
				int chunkId = calcChunkId(response.getObjectId(), response.getChunkId());
				byte[][] responses = readMap.get(chunkId);
				responses[(response.getChunkId() + response.getObjectId() + serverId)%codec.getSize()] = response.getData();
				Integer chunksNum = chunksSet.get(chunkId);
				if (chunksNum == null) {
					chunksSet.put(chunkId, 1);
				} else {
					chunksSet.put(chunkId, chunksNum + 1);
				}
				mutex.notifyAll();
			}
		}
	}
	
	void readFile(Item item) {
		if (item != null) {
			for (int i = 0; i < item.getStripesNumber(); i++) {
				readMap.put(calcChunkId(item.getId(), i), new byte[codec.getSize()][]);
			}
		}
	}
	
	void die() {
		synchronized (mutex) {
			die = true;
			mutex.notify();
		}
	}
	
	void addChecksum(int stripeId, final int itemId, byte[][] dataShards) {
		checksumMap.put(calcChunkId(itemId, stripeId), calcChecksum(dataShards));
	}
	
	void saveChecksums() throws FileNotFoundException, IOException {
		File outputFile = new File(CHECKSUMS_FILENAME);
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
		out.writeObject(checksumMap);
		out.close();
	}
	
	@SuppressWarnings("unchecked")
	void loadChecksums() throws FileNotFoundException, IOException, ClassNotFoundException {
		File inputFile = new File(CHECKSUMS_FILENAME);
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
		checksumMap = (Map<Integer, long[]>) in.readObject();
		in.close();
	}
	
	@Override
	public void run() {
		executer = Executors.newFixedThreadPool(N_THREADS);
		synchronized (mutex) {
			while (!(die && readMap.isEmpty())) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				chunksSet.entrySet().removeIf(e -> isStripeReady(e.getKey(), e.getValue()));
			}
			executer.shutdown();
		}
	}
	
	private boolean isStripeReady(Integer chunkId, Integer chunksNum) {
		if (chunksNum < shouldPresent) {
			return false;
		}
		
		byte[][] chunks = readMap.get(chunkId);
		readMap.remove(chunkId);

		executer.execute(new Runnable() {
			@Override
			public void run() {
				StopWatch stopWatch = new Log4JStopWatch(chunkId.toString(), logger);
				byte[][] decode = codec.decode(chunksPresent(chunks), chunks, chunks[0].length);
				long[] checksum = calcChecksum(decode);
				long[] origChecksum = checksumMap.get(chunkId);
				for (int i = 0; i < checksum.length; i++) {
					if (checksum[i] != origChecksum[i]) {
						System.err.println("###\nChecksum isn't compatible stripeId: " + chunkId + "\n###");
						break;
					}
				}
				stopWatch.stop();
			}
		});
		return true;
	}

	private boolean[] chunksPresent(byte[][] chunks) {
		boolean[] shardPresent = new boolean[codec.getSize()];
		for (int j = 0; j < codec.getSize(); ++j) {
			shardPresent[j] = (chunks[j] != null);
		}
		return shardPresent;
	}
	
	private int calcChunkId(int itemId, int stripeId) {
		return (itemId << 10) + stripeId;
	}

	private long[] calcChecksum(byte[][] dataShards) {
		CRC32 crc = new CRC32();
		long[] checksums = new long[dataShards.length];
		for (int i = 0; i < dataShards.length; i++) {
			crc.update(dataShards[i]);
			checksums[i] = crc.getValue();
		}
		return checksums;
	}
}
