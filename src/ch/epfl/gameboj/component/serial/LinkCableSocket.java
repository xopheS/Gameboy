package ch.epfl.gameboj.component.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class LinkCableSocket {
	private DataOutputStream dataOut;
	private DataInputStream dataIn;
	private int currentInput;
	private int currentOutput;
	private boolean isOpen;
	
	public void start(boolean isMasterEnd) {
		System.out.println("Start " + isMasterEnd + " cable ");
		Runnable transferThreadRunnable;
		
		if (isMasterEnd) {
			transferThreadRunnable = new Runnable() {
				@Override
				public void run() {
	        		try {
	        			while (true) {
							dataOut.writeByte(currentInput);
							currentInput = dataIn.read();
	        			}
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
			};
		} else {
			transferThreadRunnable = new Runnable() {
				@Override
				public void run() {
	        		try {
	        			while (true) {
							currentInput = dataIn.read();
							dataOut.writeByte(currentOutput);
	        			}
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
			};
		}
        
        new Thread(transferThreadRunnable).start();
        
        isOpen = true;
	}
	
	public void setDataOutputStream(DataOutputStream s) {
		dataOut = s;
	}
	
	public void setDataInputStream(DataInputStream s) {
		dataIn = s;
	}
	
	public int readByte() throws IOException {
		return currentInput;
	}
	
	public void writeByte(int byteToWrite) throws IOException {
		currentOutput = byteToWrite;
	}
	
	public boolean isOpen() {
		return isOpen;
	}
}
