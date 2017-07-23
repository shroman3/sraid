package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.xml.sax.SAXException;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.common.ResponseType;
import com.shroman.secureraid.utils.Utils;
import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLGetter.Getter;
import com.shroman.secureraid.utils.XMLParsingException;

public class WriteClient {
	private static final String ITEMS_FILENAME = "items.ser";
	private static final String CONFIG_XML = "config.xml";
	private static final int BYTES_IN_MEGABYTE = 1048576;
	private List<Connection> servers = new ArrayList<>();
	private Map<String, Item> itemsMap = new HashMap<>();
	private int itemIdGenerator = 0;
	private int clientId;
	private int stripeSize;
	private Codec codec;
	private int shardSize;
	private ExecutorService executer;
	private ReadClient reader;
	private Logger logger;
	private OperationType operation;
	private Config config;
	private XMLGetter xmlGetter;
	private int serversNum;
	private int stepSize;

	public static void main(String[] args) {
		Scanner inputFileScanner = null;
		try {
			XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
			WriteClient client = buildClient(args, xmlGetter);
			inputFileScanner = new Scanner(new FileInputStream("input.txt"));
			try {
				client.run(inputFileScanner);
			} catch (Exception e) {
				System.out.println("Something went wrong: " + e.getMessage());
				client.finalizeServerConnections();
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Please call the client with the following arguments:\n"
					+ "operation_type codec_name k r z random_name random_key servers_num step_size");
		} catch (ParserConfigurationException | SAXException | IOException | XMLParsingException e) {
			throw new RuntimeException("Unable to load config XML file(" + CONFIG_XML + ")\n" + e.getMessage());
		} finally {
			if (inputFileScanner != null) {
				inputFileScanner.close();
			}
		}
	}

	private WriteClient(Codec codec, OperationType operation, int serversNum, int stepSize, XMLGetter xmlGetter)
			throws XMLParsingException, UnknownHostException, IOException {
		this.serversNum = serversNum;
		this.stepSize = stepSize;
		this.xmlGetter = xmlGetter;
		this.codec = codec;
		logger = Logger.getLogger("Encode");
		config = new Config(xmlGetter);
		clientId = xmlGetter.getIntField("client", "id");
		stripeSize = xmlGetter.getIntField("client", "stripe_size") * BYTES_IN_MEGABYTE;
		shardSize = stripeSize / codec.getDataShardsNum();
		reader = new ReadClient(codec, config, serversNum, stepSize);
		this.operation = operation;
	}

	public void initEncoder() throws UnknownHostException, XMLParsingException, IOException {
		executer = Utils.buildExecutor(config.getExecuterThreadsNum(), config.getExecuterQueueSize());
		initMockConnections(serversNum);
	}

	public void initWriter() throws UnknownHostException, XMLParsingException, IOException {
		executer = Utils.buildExecutor(config.getExecuterThreadsNum(), config.getExecuterQueueSize());
		initServerConnections(xmlGetter, serversNum);
	}

	void initReader() throws ClassNotFoundException, IOException, XMLParsingException {
		loadItemsMap();
		reader.loadChecksums();
		reader.start();
		initServerConnections(xmlGetter, serversNum);
	}

	void initRandomChunkReader() throws ClassNotFoundException, IOException, XMLParsingException {
		initReader();
		reader.setShouldPresent(codec.getSize() - codec.getParityShardsNum() - codec.getDataShardsNum() + 1);
	}

	void finalizeEncoder() throws InterruptedException, IOException {
		executer.shutdown();
		executer.awaitTermination(10, TimeUnit.MINUTES);
		reader.logThroughput();
	}

	void finalizeWriter() throws InterruptedException, IOException {
		executer.shutdown();
		executer.awaitTermination(10, TimeUnit.MINUTES);
		reader.saveChecksums();
		saveItemsMap();
		finalizeServerConnections();
		reader.logThroughput();
	}

	void finalizeReader() {
		reader.die();
		finalizeServerConnections();
	}

	private void saveItemsMap() throws IOException {
		File outputFile = new File(ITEMS_FILENAME);
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
		out.writeObject(itemsMap);
		out.close();
	}

	@SuppressWarnings("unchecked")
	private void loadItemsMap() throws IOException, ClassNotFoundException {
		File inputFile = new File(ITEMS_FILENAME);
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
		itemsMap = (Map<String, Item>) in.readObject();
		in.close();
	}

	void randomChunkReadFile(String fileName) {
		Item item = itemsMap.get(fileName);
		Random random = new Random(12345678);
		for (int i = 0; i < item.getStripesNumber() / 4; i++) {
			int stripe = random.nextInt(item.getStripesNumber());
			int chunk = random.nextInt(codec.getDataShardsNum());
			reader.readStripe(item, stripe);
			int secretChunks = codec.getSize() - codec.getParityShardsNum() - codec.getDataShardsNum();
			int stripeStep = ((stripe + item.getId()) * stepSize) % serversNum;
			for (int j = 0; j < secretChunks; j++) {
				int serverId = (j + stripeStep) % serversNum;
				servers.get(serverId).addMessage(new Message(MessageType.READ, null, item.getId(), stripe));
			}
			int serverId = (chunk + stripeStep) % serversNum;
			servers.get(serverId).addMessage(new Message(MessageType.READ, null, item.getId(), stripe));
		}
	}

	void degReadFile(String fileName) {
		Item item = itemsMap.get(fileName);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			reader.readStripe(item, i);
			int j = 0;
			int stripeStep = ((i + item.getId()) * stepSize) % serversNum;
			while (j < codec.getSize() - codec.getParityShardsNum()) {
				int serverId = (j + stripeStep) % serversNum;
				if (serverId != 0) {
					servers.get(serverId).addMessage(new Message(MessageType.READ, null, item.getId(), i));
					j++;
				}
			}
		}
	}

	void deg2ReadFile(String fileName) {
		Item item = itemsMap.get(fileName);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			reader.readStripe(item, i);
			int j = 0;
			int stripeStep = ((i + item.getId()) * stepSize) % serversNum;
			while (j < codec.getSize() - codec.getParityShardsNum()) {
				int serverId = (j + stripeStep) % serversNum;
				if (serverId > 1) {
					servers.get(serverId).addMessage(new Message(MessageType.READ, null, item.getId(), i));
					j++;
				}
			}
		}
	}

	void readFile(String fileName) {
		Item item = itemsMap.get(fileName);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			int stripeStep = ((i + item.getId()) * stepSize) % serversNum;
			reader.readStripe(item, i);
			for (int j = 0; j < codec.getSize() - codec.getParityShardsNum(); j++) {
				int serverId = (j + stripeStep) % serversNum;
				servers.get(serverId).addMessage(new Message(MessageType.READ, null, item.getId(), i));
			}
		}
	}

	void encodeFile(String fileName) throws IOException {
		InputStream in = null;
		try {
			if (itemsMap.containsKey(fileName)) {
				throw new IllegalArgumentException("File allready writen: " + fileName);
			}
			File inputFile = new File(fileName);
			in = getInputStream(inputFile);

			// Get the size of the input file (files bigger than
			// Integer.MAX_VALUE will fail!)
			long fileSize = inputFile.length();
			// Figure how many stripes should be created
			int stripes = (int) ((fileSize / stripeSize) + (fileSize % stripeSize == 0 ? 0 : 1));

			long bytesRead = 0;
			// Creating stripes (all except the last)
			for (int i = 0; i < stripes - 1; i++) {
				bytesRead += encodeStripe(in, shardSize, i, itemIdGenerator);
			}
			// Creating the last stripe
			int shardsNum = codec.getDataShardsNum();
			int leftoverSize = (int) (fileSize - (stripes - 1) * shardsNum * shardSize);
			int leftoverShardSize = (leftoverSize + shardsNum - 1) / shardsNum;
			bytesRead += encodeStripe(in, leftoverShardSize, stripes - 1, itemIdGenerator);

			if (bytesRead != fileSize) {
				throw new IOException("not enough bytes read");
			}

			itemsMap.put(fileName,
					new Item.Builder().setFileSize(fileSize).setId(itemIdGenerator).setStripesNumber(stripes).build());

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			itemIdGenerator++;
			if (in != null) {
				in.close();
			}
		}
	}

	void clean() {
		for (int i = 0; i < serversNum; i++) {
			servers.get(i).addMessage(new Message(MessageType.CLEAN));
		}
	}

	private void sendEncoded(byte[][] encodedShards, int objectId, int chunkId) {
		int initialStep = ((objectId + chunkId) * stepSize) % serversNum;
		for (int i = 0; i < encodedShards.length; i++) {
			int serverId = (i + initialStep) % serversNum;
			servers.get(serverId).addMessage(new Message(MessageType.WRITE, encodedShards[i], objectId, chunkId));
		}
	}

	private void initServerConnections(XMLGetter xmlGetter, int size)
			throws XMLParsingException, UnknownHostException, IOException {
		Iterator<Getter> iterator = xmlGetter.getIterator("connections", "server");
		for (int i = 0; i < size; i++) {
			Getter getter = iterator.next();
			ServerConnectionWriter serverConnection = new ServerConnectionWriter(i, clientId,
					getter.getAttribute("host"), getter.getIntAttribute("port"), reader, config);
			servers.add(serverConnection);
			serverConnection.start();
		}
	}

	private void initMockConnections(int size) {
		for (int i = 0; i < size; i++) {
			final int serverId = i;
			servers.add(new Connection() {
				@Override
				public void addMessage(Message message) {
					if (message.getType() == MessageType.WRITE) {
						reader.push(new Response(ResponseType.WRITE, null, message.getObjectId(), message.getChunkId()),
								serverId);
					}
				}
			});
		}
	}

	private int encodeStripe(InputStream in, final int shardSize, final int stripeId, final int itemId)
			throws IOException {
		int bytesRead = 0;
		byte[][] dataShards = new byte[codec.getDataShardsNum()][shardSize];
		for (int j = 0; j < codec.getDataShardsNum(); j++) {
			bytesRead += in.read(dataShards[j]);
		}

		if (bytesRead != shardSize * codec.getDataShardsNum()) {
			throw new IOException("not enough bytes read");
		}
		executer.execute(new Runnable() {
			@Override
			public void run() {
				StopWatch stopWatch = new Log4JStopWatch(logger);
				int combinedId = reader.addChecksum(stripeId, itemId, dataShards);

				byte[][] encodedShards = codec.encode(dataShards[0].length, dataShards);
				stopWatch.stop(Integer.toString(combinedId), Integer.toString(dataShards[0].length));
				sendEncoded(encodedShards, itemId, stripeId);
				// stopWatch.stop(Integer.toString(combinedId),
				// Integer.toString(dataShards[0].length) + ",BARRIER");
			}
		});
		return bytesRead;
	}

	private void run(Scanner inputFileScanner)
			throws IOException, InterruptedException, ClassNotFoundException, XMLParsingException {
		reader.setStartTimestamp();
		operation.run(inputFileScanner, this);
	}

	private void finalizeServerConnections() {
		for (Connection server : servers) {
			server.addMessage(Message.KILL);
		}
	}

	private static FileInputStream getInputStream(File inputFile) throws FileNotFoundException {
		if (!inputFile.exists()) {
			System.out.println("Cannot read input file: " + inputFile);
			throw new RuntimeException("Cannot read input file: " + inputFile);
		}

		FileInputStream fileInputStream = new FileInputStream(inputFile);
		return fileInputStream;
	}

	private static WriteClient buildClient(String[] args, XMLGetter xmlGetter)
			throws ParserConfigurationException, SAXException, IOException, XMLParsingException, UnknownHostException {
		Utils.validateArraySize(args, 9, "Arguments");
		try {
			String operationType = args[0];
			String codecName = args[1];
			int k = Integer.parseInt(args[2]);
			int r = Integer.parseInt(args[3]);
			int z = Integer.parseInt(args[4]);
			String randomName = args[5];
			String randomKey = args[6];
			int serversNum = Integer.parseInt(args[7]);
			int stepSize = Integer.parseInt(args[8]);
			Codec codec = CodecType.getCodecFromArgs(codecName, k, r, z, randomName, randomKey);
			if (serversNum < codec.getSize()) {
				throw new IllegalArgumentException("Number of servers should be higher that code length. Given "
						+ serversNum + " while n=" + codec.getSize());
			}
			return new WriteClient(codec, OperationType.getOperationByName(operationType), serversNum, stepSize,
					xmlGetter);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("k,r,z should be numbers: " + e.getMessage());
		}
	}
}
