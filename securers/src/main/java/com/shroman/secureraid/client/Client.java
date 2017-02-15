package com.shroman.secureraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.shroman.secureraid.codec.Codec;
import com.shroman.secureraid.utils.XMLGetter;
import com.shroman.secureraid.utils.XMLGetter.Getter;
import com.shroman.secureraid.utils.XMLParsingException;

public class Client {
	private static final int BYTES_IN_INT = 4;
	private static final String CONFIG_XML = "config.xml";
	static List<ServerConnection> servers = new ArrayList<>();

	public static void main(String[] args) {
		try {
			XMLGetter xmlGetter = new XMLGetter(CONFIG_XML);
			Codec codec = CodecType.getCodecFromArgs(args);
			initServerConnections(xmlGetter, codec.getSize());
			// Scanner inputFileScanner = new Scanner(new
			// FileInputStream(getInputFileName(id)));
			Scanner inputFileScanner = new Scanner(System.in);
			while (inputFileScanner.hasNextLine()) {
				String fileName = inputFileScanner.nextLine();
				if (fileName.toLowerCase().equals("exit")) {
					break;
				}
				readFile(fileName, codec.getDataShardsNum());
			}
		} catch (ParserConfigurationException | SAXException | IOException | XMLParsingException e) {
			throw new RuntimeException("Unable to load config XML file(" + CONFIG_XML + ")\n" + e.getMessage());
		}

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

		// Create a buffer holding the file size, followed by
		// the contents of the file.
		final int bufferSize = shardSize * dataShardsNum;
		final byte[][] dataBytes = new byte[dataShardsNum][shardSize];
		int bytesRead = 0;
		in = new FileInputStream(inputFile);
		for (int i = 0; i < dataShardsNum; i++) {
			bytesRead += in.read(dataBytes[i], shardSize * i, shardSize);
		}
		if (bytesRead != fileSize) {
			throw new IOException("not enough bytes read");
		}
		in.close();
		return dataBytes;
	}
}
