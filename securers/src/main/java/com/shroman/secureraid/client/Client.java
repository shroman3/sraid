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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.common.Message;
import com.shroman.secureraid.common.MessageType;
import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLGetter.Getter;
import com.shroman.secureraid.utils.XMLParsingException;

public class Client {
	private static final String CONFIG_XML = "config.xml";
	private static final int BYTES_IN_MEGABYTE = 1048576;
	private static List<ServerConnection> servers = new ArrayList<>();
	private static Map<Integer, Item> fileSizes = new HashMap<>();
	private static int itemId = 0;
	private static int clientId;
	private static int stripeSize;
	private static Codec codec;
	private static int shardSize;

	public static void main(String[] args) {
		Scanner inputFileScanner = null;
		try {
			initClient(args);
			// Scanner inputFileScanner = new Scanner(new
			// FileInputStream(getInputFileName(id)));
			inputFileScanner = new Scanner(System.in);
			while (inputFileScanner.hasNextLine()) {
				try {
					String fileName = inputFileScanner.nextLine();
					if (fileName.toLowerCase().equals("exit")) {
						sendClean();
						break;
					}
					encodeFile(fileName);
				} catch (Exception e) {
					System.out.println("Something went wrong: " + e.getMessage());
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException | XMLParsingException e) {
			throw new RuntimeException("Unable to load config XML file(" + CONFIG_XML + ")\n" + e.getMessage());
		} finally {
			if (inputFileScanner != null) {
				inputFileScanner.close();
			}
		}
	}

	private static void initClient(String[] args)
			throws ParserConfigurationException, SAXException, IOException, XMLParsingException, UnknownHostException {
		XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
		clientId = xmlGetter.getIntField("client", "id");
		stripeSize = xmlGetter.getIntField("client", "stripe_size") * BYTES_IN_MEGABYTE;
		codec = CodecType.getCodecFromArgs(args);
		shardSize = stripeSize / codec.getDataShardsNum();
		initServerConnections(xmlGetter, codec.getSize());
	}

	private static void sendEncoded(byte[][] encodedShards, int objectId, int chunkId) {
		for (int i = 0; i < encodedShards.length; i++) {
			servers.get(i).addMessage(new Message(MessageType.WRITE_OBJECT, encodedShards[i], objectId, chunkId));
		}
	}

	private static void sendClean() {
		for (int i = 0; i < codec.getSize(); i++) {
			servers.get(i).addMessage(new Message(MessageType.CLEAN));
		}
	}

	private static void initServerConnections(XMLGetter xmlGetter, int size)
			throws XMLParsingException, UnknownHostException, IOException {
		Iterator<Getter> iterator = xmlGetter.getIterator("connections", "server");
		for (int i = 0; i < size; i++) {
			Getter getter = iterator.next();
			ServerConnection serverConnection = new ServerConnection(clientId, getter.getAttribute("host"),
					getter.getIntAttribute("port"));
			servers.add(serverConnection);
			serverConnection.start();
		}
	}

	private static void encodeFile(String fileName) throws IOException {
		InputStream in = null;
		try {
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

			fileSizes.put(itemId,
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

	private static int encodeStripe(InputStream in, int shardSize, int stripeId) throws IOException {
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
}
