package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.common.Response;
import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLGetter.Getter;
import com.shroman.secureraid.utils.XMLParsingException;

public class Client implements PushResponseInterface {
	private static final String CONFIG_XML = "config.xml";
	private static final int BYTES_IN_MEGABYTE = 1048576;
	private List<ServerConnection> servers = new ArrayList<>();
	private Map<String, Item> fileSizes = new HashMap<>();
	private Map<Integer, byte[][][]> readMap = new HashMap<>();
	private Map<Integer, byte[][][]> decodedMap = new HashMap<>();
//	private Map<String, Integer> fileIds = new HashMap<>();
	private int itemId = 0;
	private int clientId;
	private int stripeSize;
	private Codec codec;
	private int shardSize;

	public static void main(String[] args) {
		Scanner inputFileScanner = null;
		try {
			Client client = new Client(args);
			// Scanner inputFileScanner = new Scanner(new
			// FileInputStream(getInputFileName(id)));
			inputFileScanner = new Scanner(System.in);
			try {
				while (client.parseNextLine(inputFileScanner)) {
					client.completeFullRead();
				}			
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
	
	private Client(String[] args) throws ParserConfigurationException, SAXException, IOException, XMLParsingException, UnknownHostException {
		XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
		clientId = xmlGetter.getIntField("client", "id");
		stripeSize = xmlGetter.getIntField("client", "stripe_size") * BYTES_IN_MEGABYTE;
		codec = CodecType.getCodecFromArgs(args);
		shardSize = stripeSize / codec.getDataShardsNum();
		initServerConnections(xmlGetter, codec.getSize());
	}
	
	@Override
	public void push(Response response, int serverId) {
		if (response.isSuccess() && response.getData() != null) {
			byte[][][] responses = readMap.get(response.getObjectId());
			responses[response.getChunkId()][(response.getChunkId() + response.getObjectId() + serverId)%codec.getSize()] = response.getData();
		}
	}

	void readFile(String fileName) {
		Item item = fileSizes.get(fileName);
		if (item != null) {
			readMap.put(item.getId(), new byte[item.getStripesNumber()][codec.getSize()][]);
			decodedMap.put(item.getId(), new byte[item.getStripesNumber()][][]);
		}
		for (int i = 0; i < item.getStripesNumber(); i++) {
			for (int j = 0; j < codec.getSize(); j++) {
				servers.get(j).addMessage(new Message(MessageType.READ, null, item.getId(), i));
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
				bytesRead += encodeStripe(in, shardSize, i);
			}
			// Creating the last stripe
			int shardsNum = codec.getDataShardsNum();
			int leftoverSize = fileSize - (stripes - 1) * shardsNum * shardSize;
			int leftoverShardSize = (leftoverSize + shardsNum - 1) / shardsNum;
			bytesRead += encodeStripe(in, leftoverShardSize, stripes - 1);

			if (bytesRead != fileSize) {
				throw new IOException("not enough bytes read");
			}

			fileSizes.put(fileName,
					new Item.Builder().setFileSize(fileSize).setId(itemId).setStripesNumber(stripes).build());

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			itemId++;
			if (in != null) {
				in.close();
			}
		}
	}

	private boolean parseNextLine(Scanner inputFileScanner) throws IOException {
		if (!inputFileScanner.hasNextLine()) {
			return false;
		}
		String operationLine = inputFileScanner.nextLine();
		if (operationLine.toLowerCase().equals("exit")) {
			return false;
		}
//		String[] operationArgs = operationLine.split(" ");
//		OperationType operation = OperationType.getOperationByName(operationArgs[0]);
		
//		operation.run(operationArgs, this);
		
		return true;
	}
	
	private void sendEncoded(byte[][] encodedShards, int objectId, int chunkId) {
		for (int i = 0; i < encodedShards.length; i++) {
			servers.get((i + objectId + chunkId)%codec.getSize()).addMessage(new Message(MessageType.WRITE, encodedShards[i], objectId, chunkId));
		}
	}

	private void sendClean() throws IOException {
		while (!readMap.isEmpty()) {
			completeFullRead();
		}
		
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
					getter.getIntAttribute("port"), this);
			servers.add(serverConnection);
			serverConnection.start();
		}
	}

	private int encodeStripe(InputStream in, int shardSize, int stripeId) throws IOException {
		int bytesRead = 0;
		byte[][] dataShards = new byte[codec.getDataShardsNum()][shardSize];
		for (int j = 0; j < codec.getDataShardsNum(); j++) {
			bytesRead += in.read(dataShards[j]);
		}

		if (bytesRead != shardSize * codec.getDataShardsNum()) {
			throw new IOException("not enough bytes read");
		}

		// long beforeEncode = System.currentTimeMillis();
		byte[][] encodedShards = codec.encode(dataShards[0].length, dataShards);
		// long afterEncode = System.currentTimeMillis();
		sendEncoded(encodedShards, itemId, stripeId);
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

	private void completeFullRead() throws IOException {
		Map<Integer, byte[][][]> readMap = this.readMap;
		int shouldPresent = codec.getSize();
		completeRead(readMap, shouldPresent);
	}

	private void completeRead(Map<Integer, byte[][][]> readMap, int shouldPresent) throws IOException {
		Set<Integer> finished = new HashSet<Integer>();
		for (Entry<Integer, byte[][][]> entry : readMap.entrySet()) {
			byte[][][] decoded = decodedMap.get(entry.getKey());
			int decodedNum = 0;
			for (int i = 0; i < entry.getValue().length; i++) {
				if (decoded[i] != null) {
					++decodedNum;
				} else {
					byte[][] stripe = entry.getValue()[i];
					int present = 0;
					boolean[] shardPresent = new boolean[codec.getSize()];
					for (int j = 0; j < stripe.length; ++j) {
						if(shardPresent[j] = (stripe[j] != null)) {
							++present;
						}
					}
					
					if (present == shouldPresent) {
						decoded[i] = codec.decode(shardPresent, stripe, stripe[0].length);
						++decodedNum;
					}
				}
			}
			if (decodedNum == decoded.length) {
				writeFile(entry.getKey() + ".out", decoded);
				finished.add(entry.getKey());
			}
		}
		for (Integer id : finished) {
			readMap.remove(id);
		}
	}

	private void writeFile(String fileName, byte[][][] decoded) throws IOException {
		OutputStream out = new FileOutputStream(fileName);
		for (int i = 0; i < decoded.length; i++) {
			for (int j = 0; j < decoded[i].length; j++) {
				out.write(decoded[i][j]);
			}
		}
		out.close();
	}
}
