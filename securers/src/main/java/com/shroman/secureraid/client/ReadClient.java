package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.CommonUtils;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;
import com.shroman.secureraid.common.Tuple;
import com.shroman.secureraid.utils.Utils;


public class ReadClient extends Thread implements PushResponseInterface {
	private static final String CHECKSUMS_FILENAME = "checksums.ser";
	private Codec codec;
	private Map<Integer, long[]> checksumMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> sizesMap = new ConcurrentHashMap<>();
	private Map<Integer, byte[][]> readMap = new ConcurrentHashMap<>();
	private Map<Integer, Integer> chunksMap = new ConcurrentHashMap<>();

	private BlockingQueue<Tuple<Integer, Response>> responseQ = CommonUtils.getBlockingQueue(0);

	private Map<Integer, Long> timestampMap = new ConcurrentHashMap<>();
	private Map<Integer, byte[]> keysMap = new ConcurrentHashMap<>();

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
					stripeLogger.info(Utils.buildLogMessage(timestamp, stripeId, "0"));
					if (timestampMap.isEmpty()) {
						logThroughput();
					}
				} else {
					chunksMap.put(stripeId, chunksNum);
				}
			}
		} else if (response.isSuccess() && response.getData() != null) {
			int chunkId = (((serverId - (response.getChunkId() + response.getObjectId()) * stepSize) % serversNum)
					+ serversNum) % serversNum;
			responseQ.add(new Tuple<Integer, Response>(chunkId, response));
		}
	}

	@Override
	public void run() {
		executer = Utils.buildExecutor(config.getExecuterThreadsNum(), config.getExecuterQueueSize());
		while (!readMap.isEmpty() || !die) {
			try {
				Tuple<Integer, Response> tuple = responseQ.take();
				int chunkId = tuple.x;
				Response response = tuple.y; 
				int stripeId = calcStripeId(response.getObjectId(), response.getChunkId());
				Integer chunksNum = chunksMap.get(stripeId);
				if (chunksNum != null) {
					chunksNum +=1;
					if (chunksNum < shouldPresent) {
						chunksMap.put(stripeId, chunksNum);
						byte[][] chunks = readMap.get(stripeId);
						chunks[chunkId] = response.getData();
					} else {
						byte[][] chunks = readMap.remove(stripeId);
						chunks[chunkId] = response.getData();
						chunksMap.remove(stripeId);
						finishStripe(stripeId, chunks);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			executer.shutdown();
			executer.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
		chunksMap.put(stripeId,0);
		timestampMap.put(stripeId, System.currentTimeMillis());
	}

	void die() {
//		synchronized (mutex) {
		die = true;
//			mutex.notify();
//		}
	}

	int addChecksumAndKey(int stripeNum, final int itemId, byte[][] dataShards, byte[] key) {
		int stripeId = calcStripeId(itemId, stripeNum);
		timestampMap.put(stripeId, System.currentTimeMillis());
		checksumMap.put(stripeId, calcChecksum(dataShards, dataShards[0].length));
		sizesMap.put(stripeId, dataShards[0].length);
		if (key != null) {
			keysMap.put(stripeId, key);
		}
		return stripeId;
	}

	void saveChecksums() throws FileNotFoundException, IOException {
		File outputFile = new File(CHECKSUMS_FILENAME);
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
		out.writeObject(checksumMap);
		out.writeObject(sizesMap);
		out.writeObject(keysMap);
		out.close();
	}

	@SuppressWarnings("unchecked")
	void loadChecksums() throws FileNotFoundException, IOException, ClassNotFoundException {
		File inputFile = new File(CHECKSUMS_FILENAME);
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
		checksumMap = (Map<Integer, long[]>) in.readObject();
		sizesMap = (Map<Integer, Integer>) in.readObject();
		keysMap = (Map<Integer, byte[]>) in.readObject();
		in.close();
	}
	
	void logThroughput() {
		throughputLogger.info(Utils.buildLogMessage(startTimestamp, -1, "0"));
	}

	private void finishStripe(int stripeId, byte[][] chunks) {
		executer.execute(new Runnable() {
			@Override
			public void run() {
				decode(stripeId, chunks);
			}
		});
	}

	private int calcStripeId(int itemId, int stripeId) {
		return (itemId << 10) + stripeId;
	}

	private long[] calcChecksum(byte[][] dataShards, int size) {
		long[] checksums = new long[dataShards.length];
		for (int i = 0; i < dataShards.length; i++) {
			if (dataShards[i] == null) {
				checksums[i] = -1;
			} else {
				CRC32 crc = new CRC32();
				// IMPORTANT size is reduced for the AONT, this is only a debug feature.
				crc.update(dataShards[i], 0, size/*-32*/);
				checksums[i] = crc.getValue();
			}
		}
		return checksums;
	}

	private void decode(Integer stripeId, byte[][] chunks) {
		int chunkSize = getChunkSize(chunks);
		StopWatch stopWatch = new Log4JStopWatch(stripeId.toString(), Integer.toString(chunkSize), decodeLogger);
		byte[][] decode = codec.decode(chunks, chunkSize, getKey(stripeId));
		
		long[] checksum = calcChecksum(decode, sizesMap.get(stripeId));
		long[] origChecksum = checksumMap.get(stripeId);
		for (int i = 0; i < checksum.length; i++) {
			if (checksum[i] != -1 && checksum[i] != origChecksum[i]) {
				System.err.println("###\nChecksum isn't compatible stripeId: " + stripeId + "\n###");
				break;
			}
		}
		stopWatch.stop();
		Long timestamp = timestampMap.remove(stripeId);
		stripeLogger.info(Utils.buildLogMessage(timestamp, stripeId, "0"));
	}

	private byte[] getKey(Integer chunkId) {
		if (codec.isKeyNeeded()) {
			return keysMap.get(chunkId);
		}
		return null;
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
