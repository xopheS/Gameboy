package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;

public final class Sound1 implements Component, Clocked {
	private enum Reg implements Register { NR10, NR11, NR12, NR13, NR14 }
	
	private enum NR10 implements Bit { SWEEP_SHIFT0, SWEEP_SHIFT1, SWEEP_SHIFT2, SWEEP_INC, SWEEP_TIME0, SWEEP_TIME1, SWEEP_TIME2, UNUSED7 }
	
	private enum NR11 implements Bit { S_LENGTH0, S_LENGTH1, S_LENGTH2, S_LENGTH3, S_LENGTH4, S_LENGTH5, DUTY0, DUTY1 }
	
	private enum NR12 implements Bit { ENV_LENGTH0, ENV_LENGTH1, ENV_LENGTH2, ENVELOPE, ENV_DEF0, ENV_DEF1, ENV_DEF2, ENV_DEF3 }
	
	private enum NR14 implements Bit { HIGH_FREQ0, HIGH_FREQ1, HIGH_FREQ2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }
	
	private final RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private int index;
	
	private int[] wave = new int[32];
	
	public int[] getWave() {
		int dutyLength = 0;
		
		switch (getWaveDuty()) {
		case 0:
			dutyLength = 4; //12.5%
			break;
		case 1:
			dutyLength = 8; //25%
			break;
		case 2:
			dutyLength = 16; //50%
			break;
		case 3:
			dutyLength = 24; //75%
			break;
		}
		
		for (int i = 0; i < wave.length; ++i) {
			wave[i] = (i < dutyLength) ? 1 : -1;
		}
		
		return wave;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void incIndex() {
		index++;
	}
	
	public boolean needsInit() {
		return soundRegs.testBit(Reg.NR14, NR14.INIT);
	}
	
	private boolean isContinuous() {
		return soundRegs.testBit(Reg.NR14, NR14.COUNTER);
	}
	
	private int getWaveDuty() {
		return soundRegs.asInt(Reg.NR11, NR11.DUTY0, NR11.DUTY1);
	}
	
	public int getFrequency() {
		return soundRegs.get(Reg.NR13);
	}
	
	@Override
	public void cycle(long cycle) {
		// TODO Auto-generated method stub
	}

	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S1_START && address < AddressMap.REGS_S1_END) {
			return soundRegs.get(address - AddressMap.REGS_S1_START);
		}
		
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S1_START && address < AddressMap.REGS_S1_END) {
			System.out.println("Write in sound 1");
			switch (address) {
			case AddressMap.REG_NR14:
				soundRegs.set(Reg.NR14, data);
				if (soundRegs.testBit(Reg.NR14, NR14.INIT)) {
					//Reset it
				}
				break;
			default:
				soundRegs.set(address - AddressMap.REGS_S1_START, data);
			}
		}
	}
}
