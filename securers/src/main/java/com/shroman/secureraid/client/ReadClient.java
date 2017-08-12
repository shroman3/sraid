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
import com.shroman.secureraid.common.ResponseType;
import com.shroman.secureraid.utils.Utils;

public class ReadClient extends Thread implements PushResponseInterface {
	private static final String CHECKSUMS_FILENAME = "checksums.ser";
	private Codec codec;
	private Map<Integer, long[]> checksumMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> sizesMap = new ConcurrentHashMap<>();
	private Map<Integer, byte[][]> readMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> chunksMap = new ConcurrentHashMap<>();

	// private Map<Integer, Integer> writesMap = new ConcurrentHashMap<>();
	private Map<Integer, Long> timestampMap = new ConcurrentHashMap<>();

	private Integer mutex = -1;
	private boolean die = false;
	private ExecutorService executer;
	private int shouldPresent;
	private Logger decodeLogger;
	private Logger stripeLogger;
	private Logger throughputLogger;
	private Config config;
	private int serversNum;
	private int stepSize;
	private long startTimestamp;

	ReadClient(Codec codec, Config config, int serversNum, int stepSize) {
		this.serversNum = serversNum;
		this.stepSize = stepSize;
		this.codec = codec;
		this.config = config;
		shouldPresent = codec.getSize() - codec.getParityShardsNum();
		decodeLogger = Logger.getLogger("Decode");
		stripeLogger = Logger.getLogger("Stripe");
		throughputLogger = Logger.getLogger("Throughput");
	}

	@Override
	public void push(Response response, int serverId) {
		if (response.getType() == ResponseType.WRITE) {
			synchronized (mutex) {
				int stripeId = calcStripeId(response.getObjectId(), response.getChunkId());
				Integer chunksNum = chunksMap.get(stripeId);
				if (chunksNum == null) {
					chunksNum = 1;
				} else {
					++chunksNum;
				}
				if (chunksNum == codec.getSize()) {
					chunksMap.remove(stripeId);
					Long timestamp = timestampMap.remove(stripeId);
					stripeLogger.info(Utils.buildLogMessage(timestamp, stripeId, ""));
				} else {
					chunksMap.put(stripeId, chunksNum);
				}
			}
		} else if (response.isSuccess() && response.getData() != null) {
			synchronized (mutex) {
				int stripeId = calcStripeId(response.getObjectId(), response.getChunkId());
				byte[][] responses = readMap.get(stripeId);
				int chunkId = (((serverId - (response.getChunkId() + response.getObjectId()) * stepSize) % serversNum)
						+ serversNum) % serversNum;
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
		logThroughput();
	}

	void setShouldPresent(int shouldPresent) {
		this.shouldPresent = shouldPresent;
	}


	void setStartTimestamp() {
		startTimestamp = System.currentTimeMillis();
	}
		
	void readStripe(Item item, int stripeNum) {
		int stripeId = calcStripeId(item.getId(), stripeNum);
		readMap.put(stripeId, new byte[codec.getSize()][]);
		timestampMap.put(stripeId, System.currentTimeMillis());
	}

	void die() {
		synchronized (mutex) {
			die = true;
			mutex.notify();
		}
	}

	int addChecksum(int stripeNum, final int itemId, byte[][] dataShards) {
		int stripeId = calcStripeId(itemId, stripeNum);
		timestampMap.put(stripeId, System.currentTimeMillis());
		checksumMap.put(stripeId, calcChecksum(dataShards, dataShards[0].length));
		sizesMap.put(stripeId, dataShards[0].length);
		return stripeId;
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
	
	void logThroughput() {
		throughputLogger.info(Utils.buildLogMessage(startTimestamp, -1, ""));
	}

	private boolean isStripeReady(Integer chunkId, Integer chunksNum) {
		if (chunksNum < shouldPresent) {
			return false;
		}

		byte[][] chunks = readMap.remove(chunkId);

		executer.execute(new Runnable() {
			@Override
			public void run() {
				decode(chunkId, chunks);
			}
		});
		return true;
	}

	private boolean[] chunksPresent(byte[][] chunks) {
		boolean[] shardPresent = new boolean[codec.getSize()];
		for (int j = 0; j < codec.getSize(); ++j) {
			shardPresent[j] = (chunks[j] != null);
			if (!shardPresent[j]) {
				chunks[j] = new byte[getChunkSize(chunks)];
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
			if (dataShards[i] == null) {
				checksums[i] = -1;
			} else {
				crc.update(dataShards[i], 0, size);
				checksums[i] = crc.getValue();
			}
		}
		return checksums;
	}

	private void decode(Integer chunkId, byte[][] chunks) {
		StopWatch stopWatch = new Log4JStopWatch(chunkId.toString(), Integer.toString(getChunkSize(chunks)), decodeLogger);
		byte[][] decode = codec.decode(chunksPresent(chunks), chunks, chunks[0].length);
		long[] checksum = calcChecksum(decode, sizesMap.get(chunkId));
		long[] origChecksum = checksumMap.get(chunkId);
		for (int i = 0; i < checksum.length; i++) {
			if (checksum[i] != -1 && checksum[i] != origChecksum[i]) {
				System.err.println("###\nChecksum isn't compatible stripeId: " + chunkId + "\n###");
				break;
			}
		}
		stopWatch.stop();
		Long timestamp = timestampMap.remove(chunkId);
		stripeLogger.info(Utils.buildLogMessage(timestamp, chunkId, ""));
	}

	private int getChunkSize(byte[][] chunks) {
		for (byte[] bs : chunks) {
			if (bs != null) {				
				return bs.length;
			}
		}
		return 0;
	}
}
