package ch.epfl.gameboj.component.serial;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_SECOND;

import java.io.IOException;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class SerialIO implements Component, Clocked {
	public enum SC implements Bit { IO_SEL, CLK, UNUSED2, UNUSED3, UNUSED4, UNUSED5, UNUSED6, START }
	
	public final static int TRANSFER_FREQ = 8000;
	
	public final static int CYCLE_FREQ = Math.floorMod(CYCLES_PER_SECOND, TRANSFER_FREQ);
	
	private Bus bus;
	private Cpu cpu;
	
	private int serialBus;
	private int serialControl = 0b01000000;
	
	public SerialIO(Cpu cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
		bus.attach(this);
	}
	
	@Override
	public void cycle(long cycle) {
		if (cycle % (Byte.SIZE * CYCLE_FREQ) == 0) {
			reallyCycle();
		}
	}
	
	public void reallyCycle() {
		if (Bits.test(serialControl, SC.START)) {
			try {
				SerialProtocol.executeTransfer(serialBus);
				serialBus = SerialProtocol.getReceivedDataBuffer();
				serialControl = Bits.set(serialControl, SC.START.index(), false);
				cpu.requestInterrupt(Interrupt.SERIAL);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
			serialControl = data | Bits.mask(SC.CLK.index());
		}
	}
}
