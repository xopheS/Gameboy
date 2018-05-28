package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class SerialIO implements Component, Clocked {
	private enum SC implements Bit { IO_SEL, CLK, UNUSED2, UNUSED3, UNUSED4, UNUSED5, UNUSED6, START }
	
	private Bus bus;
	
	private int serialBus;
	private int serialControl;
	
	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
	}
	
	@Override
	public void cycle(long cycle) {
		if (Bits.test(serialControl, SC.START)) {
			
		}
	}

	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) == AddressMap.REG_SB) {
			return serialBus;
		} else if (address == AddressMap.REG_SC) {
			return serialControl;
		}
		
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		
		if (Preconditions.checkBits16(address) == AddressMap.REG_SB) {
			serialBus = data;
		} else if (address == AddressMap.REG_SC) {
			serialControl = data;
		}
	}
}
