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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.shroman.secureraid.utils.Utils;
import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLGetter.Getter;
import com.shroman.secureraid.utils.XMLParsingException;

public class WriteClient {
	private static final String ITEMS_FILENAME = "items.ser";
	private static final String CONFIG_XML = "config.xml";
	private static final int BYTES_IN_MEGABYTE = 1048576;
	private List<ServerConnection> servers = new ArrayList<>();
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

	public static void main(String[] args) {
		Scanner inputFileScanner = null;
		try {
//			WriteClient client = new WriteClient(Arrays.copyOfRange(args, 1, args.length));
			WriteClient client = new WriteClient(args);
			inputFileScanner = new Scanner(new FileInputStream("input.txt"));
			// inputFileScanner = new Scanner(System.in);
			try {
				client.run(inputFileScanner);
			} catch (Exception e) {
				System.out.println("Something went wrong: " + e.getMessage());
			}
		} catch (ParserConfigurationException | SAXException | IOException | XMLParsingException e) {
			throw new RuntimeException("Unable to load config XML file(" + CONFIG_XML + ")\n" + e.getMessage());
		} finally {
			if (inputFileScanner != null) {
				inputFileScanner.close();
			}
		}
	}

	private WriteClient(String[] args) throws ParserConfigurationException, SAXException, IOException, XMLParsingException, UnknownHostException {	
		XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
		config = new Config(xmlGetter);
		logger = Logger.getLogger("Encode");
		clientId = xmlGetter.getIntField("client", "id");
		stripeSize = xmlGetter.getIntField("client", "stripe_size") * BYTES_IN_MEGABYTE;
		codec = CodecType.getCodecFromArgs(Arrays.copyOfRange(args, 1, args.length));
		shardSize = stripeSize / codec.getDataShardsNum();
		reader = new ReadClient(codec, config);
		initServerConnections(xmlGetter, codec.getSize());
		operation = OperationType.getOperationByName(args[0]);
	}

	public void initWriter() {
		executer = Utils.buildExecutor(config.getExecuterThreadsNum(), config.getExecuterQueueSize());
	}
	
	void initReader() throws ClassNotFoundException, IOException {
		loadItemsMap();
		reader.loadChecksums();
		reader.start();
	}

	void finalizeWriter() throws InterruptedException, IOException {
		executer.shutdown();
		executer.awaitTermination(10, TimeUnit.MINUTES);
		reader.saveChecksums();
		saveItemsMap();
		finalizeServerConnections();
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

	void degReadFile(String fileName) {
		Item item = itemsMap.get(fileName);
		reader.readFile(item);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			for (int j = 1; j < codec.getSize() - codec.getParityShardsNum()+1; j++) {
				servers.get((j + i + item.getId()) % codec.getSize())
				.addMessage(new Message(MessageType.READ, null, item.getId(), i));
			}
		}
	}

	void deg2ReadFile(String fileName) {
		Item item = itemsMap.get(fileName);
		reader.readFile(item);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			for (int j = 2; j < codec.getSize() - codec.getParityShardsNum(); j++) {
				servers.get((j + i + item.getId()) % codec.getSize())
				.addMessage(new Message(MessageType.READ, null, item.getId(), i));
			}
		}
	}

	void readFile(String fileName) {
		Item item = itemsMap.get(fileName);
		reader.readFile(item);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			for (int j = 0; j < codec.getSize() - codec.getParityShardsNum(); j++) {
				int serverId = (j + i + item.getId()) % codec.getSize();
				servers.get(serverId)
				.addMessage(new Message(MessageType.READ, null, item.getId(), i));
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
			int fileSize = (int) inputFile.length();
			// Figure how many stripes should be created
			int stripes = (fileSize / stripeSize) + (fileSize % stripeSize == 0 ? 0 : 1);

			int bytesRead = 0;
			// Creating stripes (all except the last)
			for (int i = 0; i < stripes - 1; i++) {
				bytesRead += encodeStripe(in, shardSize, i, itemIdGenerator);
			}
			// Creating the last stripe
			int shardsNum = codec.getDataShardsNum();
			int leftoverSize = fileSize - (stripes - 1) * shardsNum * shardSize;
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
		for (int i = 0; i < codec.getSize(); i++) {
			servers.get(i).addMessage(new Message(MessageType.CLEAN));
		}
	}

	private void sendEncoded(byte[][] encodedShards, int objectId, int chunkId) {
		for (int i = 0; i < encodedShards.length; i++) {
			int serverId = (i + objectId + chunkId) % codec.getSize();
			servers.get(serverId)
					.addMessage(new Message(MessageType.WRITE, encodedShards[i], objectId, chunkId));
		}
	}

	private void initServerConnections(XMLGetter xmlGetter, int size)
			throws XMLParsingException, UnknownHostException, IOException {
		Iterator<Getter> iterator = xmlGetter.getIterator("connections", "server");
		for (int i = 0; i < size; i++) {
			Getter getter = iterator.next();
			ServerConnection serverConnection = new ServerConnection(i, clientId, getter.getAttribute("host"),
					getter.getIntAttribute("port"), reader, config);
			servers.add(serverConnection);
			serverConnection.start();
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
				StopWatch stopWatch = new Log4JStopWatch(Integer.toString(itemId), Integer.toString(stripeId), logger);
				reader.addChecksum(stripeId, itemId, dataShards);

				byte[][] encodedShards = codec.encode(dataShards[0].length, dataShards);
				sendEncoded(encodedShards, itemId, stripeId);
				stopWatch.stop();
			}
		});
		return bytesRead;
	}

	private void run(Scanner inputFileScanner) throws IOException, InterruptedException, ClassNotFoundException {
		operation.run(inputFileScanner, this);
	}

	private void finalizeServerConnections() {
		for (int i = 0; i < codec.getSize(); i++) {
			servers.get(i).addMessage(Message.KILL);
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
}
