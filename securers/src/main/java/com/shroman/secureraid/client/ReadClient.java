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
import java.util.zip.CRC32;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.utils.Utils;

public class ReadClient extends Thread implements PushResponseInterface {
	private static final String CHECKSUMS_FILENAME = "checksums.ser";
	private Codec codec;
	private Map<Integer, long[]> checksumMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> sizesMap = new ConcurrentHashMap<>();
	private Map<Integer, byte[][]> readMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> chunksMap = new ConcurrentHashMap<>();
	private Integer mutex = -1;
	private boolean die = false;
	private ExecutorService executer;
	private int shouldPresent;
	private Logger logger;
	private Config config;

	ReadClient(Codec codec, Config config) {
		this.codec = codec;
		this.config = config;
		shouldPresent = codec.getSize() - codec.getParityShardsNum();
		logger = Logger.getLogger("Decode");
	}

	@Override
	public void push(Response response, int serverId) {
		if (response.isSuccess() && response.getData() != null) {
			synchronized (mutex) {
				int stripeId = calcStripeId(response.getObjectId(), response.getChunkId());
				byte[][] responses = readMap.get(stripeId);
				int chunkId = (((serverId - response.getChunkId() - response.getObjectId()) % codec.getSize())
						+ codec.getSize()) % codec.getSize();
				responses[chunkId] = response.getData();
				Integer chunksNum = chunksMap.get(stripeId);
				if (chunksNum == null) {
					chunksMap.put(stripeId, 1);
				} else {
					chunksMap.put(stripeId, chunksNum + 1);
				}
				mutex.notifyAll();
			}
		}
	}

	void readFile(Item item) {
		if (item != null) {
			for (int i = 0; i < item.getStripesNumber(); i++) {
				readMap.put(calcStripeId(item.getId(), i), new byte[codec.getSize()][]);
			}
		}
	}

	void die() {
		synchronized (mutex) {
			die = true;
			mutex.notify();
		}
	}

	void addChecksum(int stripeNum, final int itemId, byte[][] dataShards) {
		int stripeId = calcStripeId(itemId, stripeNum);
		checksumMap.put(stripeId, calcChecksum(dataShards, dataShards[0].length));
		sizesMap.put(stripeId, dataShards[0].length);
	}

	void saveChecksums() throws FileNotFoundException, IOException {
		File outputFile = new File(CHECKSUMS_FILENAME);
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
		out.writeObject(checksumMap);
		out.writeObject(sizesMap);
		out.close();
	}

	@SuppressWarnings("unchecked")
	void loadChecksums() throws FileNotFoundException, IOException, ClassNotFoundException {
		File inputFile = new File(CHECKSUMS_FILENAME);
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
		checksumMap = (Map<Integer, long[]>) in.readObject();
		sizesMap = (Map<Integer, Integer>) in.readObject();
		in.close();
	}

	@Override
	public void run() {
		executer = Utils.buildExecutor(config.getExecuterThreadsNum(), config.getExecuterQueueSize());
		synchronized (mutex) {
			while (!(die && readMap.isEmpty())) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				chunksMap.entrySet().removeIf(e -> isStripeReady(e.getKey(), e.getValue()));
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
				long[] checksum = calcChecksum(decode, sizesMap.get(chunkId));
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
			if (!shardPresent[j]) {
				chunks[j] = new byte[chunks[0].length];
			}
		}
		return shardPresent;
	}

	private int calcStripeId(int itemId, int stripeId) {
		return (itemId << 10) + stripeId;
	}

	private long[] calcChecksum(byte[][] dataShards, int size) {
		CRC32 crc = new CRC32();
		long[] checksums = new long[dataShards.length];
		for (int i = 0; i < dataShards.length; i++) {
			crc.update(dataShards[i], 0, size);
			checksums[i] = crc.getValue();
		}
		return checksums;
	}
}
