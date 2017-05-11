package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.xml.sax.SAXException;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLGetter.Getter;
import com.shroman.secureraid.utils.XMLParsingException;

public class WriteClient {
	private static final String CONFIG_XML = "config.xml";
	private static final int BYTES_IN_MEGABYTE = 1048576;
	private static final int N_THREADS = 2;
	private List<ServerConnection> servers = new ArrayList<>();
	private Map<String, Item> fileSizes = new HashMap<>();
	// private Map<Integer, byte[][][]> readMap = new HashMap<>();
	private int itemIdGenerator = 0;
	private int clientId;
	private int stripeSize;
	private Codec codec;
	private int shardSize;
	private ExecutorService executer;
	private ReadClient reader;
	private Logger logger;

	public static void main(String[] args) {
		// PropertyConfigurator.configure(Client.class.getResource("/log4j.properties"));
		Scanner inputFileScanner = null;
		try {
			WriteClient client = new WriteClient(args);
			inputFileScanner = new Scanner(new FileInputStream("input.txt"));
			// inputFileScanner = new Scanner(System.in);
			try {
				client.write(inputFileScanner);

				client.read(inputFileScanner);
				client.degread1(inputFileScanner);
				client.degread2(inputFileScanner);
				client.sendClean();
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

	private void write(Scanner inputFileScanner) throws IOException, InterruptedException {
		while (inputFileScanner.hasNextLine()) {
			String operationLine = inputFileScanner.nextLine().toLowerCase();
			if ("read".equals(operationLine)) {
				break;
			}
			encodeFile(operationLine);
		}
		executer.shutdown();
		executer.awaitTermination(10, TimeUnit.MINUTES);
	}

	private boolean read(Scanner inputFileScanner) throws IOException {
		reader.start();
		while (inputFileScanner.hasNextLine()) {
			String operationLine = inputFileScanner.nextLine().toLowerCase();
			if ("degread1".equals(operationLine)) {
				break;
			}
			readFile(operationLine);
		}
		return false;
	}

	private boolean degread1(Scanner inputFileScanner) throws IOException {
		while (inputFileScanner.hasNextLine()) {
			String operationLine = inputFileScanner.nextLine().toLowerCase();
			switch (operationLine) {
			case ("degread2"):
				return false;
			default:
				readFile(operationLine);
			}
		}
		return false;
	}

	private boolean degread2(Scanner inputFileScanner) throws IOException {
		while (inputFileScanner.hasNextLine()) {
			String operationLine = inputFileScanner.nextLine().toLowerCase();
			switch (operationLine) {
			case ("exit"):
				return false;
			default:
				readFile(operationLine);
			}
		}
		return false;
	}

	private WriteClient(String[] args)
			throws ParserConfigurationException, SAXException, IOException, XMLParsingException, UnknownHostException {
		XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
		logger = Logger.getLogger("Encode");

		clientId = xmlGetter.getIntField("client", "id");
		stripeSize = xmlGetter.getIntField("client", "stripe_size") * BYTES_IN_MEGABYTE;
		codec = CodecType.getCodecFromArgs(args);
		shardSize = stripeSize / codec.getDataShardsNum();
		executer = Executors.newFixedThreadPool(N_THREADS);
		reader = new ReadClient(codec);
		initServerConnections(xmlGetter, codec.getSize());
	}

	void readFile(String fileName) {
		Item item = fileSizes.get(fileName);
		reader.readFile(item);
		for (int i = 0; i < item.getStripesNumber(); i++) {
			for (int j = 0; j < codec.getSize() - codec.getParityShardsNum(); j++) {
				servers.get((j + i + item.getId()) % codec.getSize())
				.addMessage(new Message(MessageType.READ, null, item.getId(), i));
			}
		}
	}

	void encodeFile(String fileName) throws IOException {
		InputStream in = null;
		try {
			if (fileSizes.containsKey(fileName)) {
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

			fileSizes.put(fileName,
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

	// private boolean parseNextLine(Scanner inputFileScanner) throws
	// IOException {
	// if (!inputFileScanner.hasNextLine()) {
	// return false;
	// }
	// String operationLine = inputFileScanner.nextLine();
	// if (operationLine.toLowerCase().equals("exit")) {
	// return false;
	// }
	// String[] operationArgs = operationLine.split(" ");
	// OperationType operation =
	// OperationType.getOperationByName(operationArgs[0]);
	//
	//// operation.run(operationArgs, this);
	//
	// return true;
	// }

	private void sendEncoded(byte[][] encodedShards, int objectId, int chunkId) {
		for (int i = 0; i < encodedShards.length; i++) {
			servers.get((i + objectId + chunkId) % codec.getSize())
					.addMessage(new Message(MessageType.WRITE, encodedShards[i], objectId, chunkId));
		}
	}

	private void sendClean() throws IOException {
		reader.die();
		for (int i = 0; i < codec.getSize(); i++) {
			servers.get(i).addMessage(new Message(MessageType.CLEAN));
		}
	}

	private void initServerConnections(XMLGetter xmlGetter, int size)
			throws XMLParsingException, UnknownHostException, IOException {
		Iterator<Getter> iterator = xmlGetter.getIterator("connections", "server");
		for (int i = 0; i < size; i++) {
			Getter getter = iterator.next();
			ServerConnection serverConnection = new ServerConnection(i, clientId, getter.getAttribute("host"),
					getter.getIntAttribute("port"), reader);
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

	private static FileInputStream getInputStream(File inputFile) throws FileNotFoundException {
		if (!inputFile.exists()) {
			System.out.println("Cannot read input file: " + inputFile);
			throw new RuntimeException("Cannot read input file: " + inputFile);
		}

		FileInputStream fileInputStream = new FileInputStream(inputFile);
		return fileInputStream;
	}
}
