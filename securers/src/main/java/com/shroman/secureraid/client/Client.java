package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	private static List<ServerConnection> servers = new ArrayList<>();
	private static Map<Integer, Integer> fileSizes = new HashMap<>();
	private static int id = 0;

	public static void main(String[] args) {
		Scanner inputFileScanner = null;
		try {
			XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
			Codec codec = CodecType.getCodecFromArgs(args);
			initServerConnections(xmlGetter, codec.getSize());
			// Scanner inputFileScanner = new Scanner(new FileInputStream(getInputFileName(id)));
			inputFileScanner = new Scanner(System.in);
			while (inputFileScanner.hasNextLine()) {
				String fileName = inputFileScanner.nextLine();
				if (fileName.toLowerCase().equals("exit")) {
					break;
				}
				byte[][] dataShards = readFile(fileName, codec.getDataShardsNum());
//				long beforeEncode = System.currentTimeMillis();
				byte[][] encodedShards = codec.encode(dataShards[0].length, dataShards);
//				long afterEncode  = System.currentTimeMillis();
				sendEncoded(encodedShards);
			}
		} catch (ParserConfigurationException | SAXException | IOException | XMLParsingException e) {
			throw new RuntimeException("Unable to load config XML file(" + CONFIG_XML + ")\n" + e.getMessage());
		} finally {
			if (inputFileScanner != null) {
				inputFileScanner.close();
			}
		}
	}

	private static void sendEncoded(byte[][] encodedShards) {
		for (int i = 0; i < encodedShards.length; i++) {
			servers.get(i).setMessage(new Message(MessageType.WRITE_OBJECT, encodedShards[i], id));
		}
		id++;
	}

	private static void initServerConnections(XMLGetter xmlGetter, int size) throws XMLParsingException {
		Iterator<Getter> iterator = xmlGetter.getIterator("connections", "server");
		for (int i = 0; i < size; i++) {
			Getter getter = iterator.next();
			servers.add(new ServerConnection(getter.getAttribute("host"), getter.getIntAttribute("port")));
		}
	}

	private static byte[][] readFile(String fileName, int dataShardsNum) throws IOException {
		InputStream in = null;
		try {			
			final File inputFile = new File(fileName);
			if (!inputFile.exists()) {
				System.out.println("Cannot read input file: " + inputFile);
				throw new RuntimeException("Cannot read input file: " + inputFile);
			}
			
			// Get the size of the input file. (Files bigger that
			// Integer.MAX_VALUE will fail here!)
			final int fileSize = (int) inputFile.length();
			
			// Figure out how big each shard will be. The total size stored
			final int shardSize = (fileSize + dataShardsNum - 1) / dataShardsNum;
			
			final byte[][] dataBytes = new byte[dataShardsNum][shardSize];
			
			int bytesRead = 0;
			in = new FileInputStream(inputFile);
			for (int i = 0; i < dataShardsNum; i++) {
				bytesRead += in.read(dataBytes[i], shardSize * i, shardSize);
			}
			if (bytesRead != fileSize) {
				throw new IOException("not enough bytes read");
			}
			fileSizes.put(id, fileSize);
			
			return dataBytes;
		} finally {
			if (in != null) {				
				in.close();
			}
		}
	}
}
