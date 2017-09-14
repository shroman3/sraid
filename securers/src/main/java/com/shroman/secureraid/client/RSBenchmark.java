package com.shroman.secureraid.client;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.backblaze.erasure.CodingLoop;
import com.backblaze.erasure.InputOutputByteTableCodingLoop;
import com.backblaze.erasure.OutputInputByteTableCodingLoop;
import com.backblaze.erasure.ReedSolomon;
import com.shroman.secureraid.client.BasicBenchmark.testable;
import com.shroman.secureraid.client.RSBenchmark.RS;

public class RSBenchmark extends BasicBenchmark<RS> {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
    	int threads = Integer.parseInt(args[0]);
		(new RSBenchmark(threads)).run();
	}
	
	public static class RS implements testable {
		private int k;
		private int r;
		private CodingLoop codingLoop;

		RS(int k, int r, CodingLoop codingLoop) {
			this.k = k;
			this.r = r;
			this.codingLoop = codingLoop;
		}

		@Override
		public String name() {
			return "RS," + k + "," + r + "," + codingLoop.getClass().getSimpleName();
		}
	}
	
	public RSBenchmark(int threadsNum) {
		super(threadsNum);
	}

	@Override
	public Iterable<RS> getItems() {
		List<RS> list = new ArrayList<RS>();
		list.add(new RS(2,4, new OutputInputByteTableCodingLoop()));
		list.add(new RS(2,4, new InputOutputByteTableCodingLoop()));
		list.add(new RS(2,34, new OutputInputByteTableCodingLoop()));
		list.add(new RS(2,34, new InputOutputByteTableCodingLoop()));
		list.add(new RS(4,2, new OutputInputByteTableCodingLoop()));
		list.add(new RS(4,2, new InputOutputByteTableCodingLoop()));
		list.add(new RS(34,2, new OutputInputByteTableCodingLoop()));
		list.add(new RS(34,2, new InputOutputByteTableCodingLoop()));
		return list;
	}

	@Override
	public Runnable buildTest(RS rs, BufferSet buffer) {
		int shardSize;
		if (rs.k<rs.r) {			
			shardSize = buffer.inputBuffer.length/(rs.r);
		} else {
			shardSize = buffer.inputBuffer.length/(rs.k);
		}
		
		byte[][] buff = new byte[rs.k+rs.r][shardSize];
		for (int i = 0; i < rs.k; i++) {
			for (int j = 0; j < buff[0].length; j++) {
				buff[i][j] = buffer.inputBuffer[i*buff[i].length + j];
			}
		}
		ReedSolomon reedSolomon = new ReedSolomon(rs.k, rs.r, rs.codingLoop);
		for (int i = rs.k; i < buff.length; i++) {
			buff[i] = new byte[buff[0].length];
		}
		return new Runnable() {
			@Override
			public void run() {
				reedSolomon.encodeParity(buff, 0, buff[0].length);
			}
		};
	}
	

}
