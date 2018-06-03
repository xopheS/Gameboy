package ch.epfl.gameboj.component.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class SerialProtocol {
	private static final LinkCableSocket linkCable = new LinkCableSocket();
	private static int receivedDataBuffer;
	
	public static void startProtocol() {
		linkCable.start();
	}
	
	public static void setDataOutput(DataOutputStream dataOut) {
		linkCable.setDataOutputStream(dataOut);
	}
	
	public static void setDataInput(DataInputStream dataIn) {
		linkCable.setDataInputStream(dataIn);
	}
	
	public static void executeTransfer(int byteToSend) throws IOException {
		linkCable.writeByte(byteToSend);
		receivedDataBuffer = linkCable.readByte();
	}
	
	public static int getReceivedDataBuffer() {
		return receivedDataBuffer;
	}
	
	public static boolean isActive() {
		return linkCable.isOpen();
	}
}
