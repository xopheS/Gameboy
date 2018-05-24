package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.Component;

public final class Sound2 implements Component {
	private enum Reg implements Register { NR21, NR22, NR23, NR24 }
	
	private enum NR21 implements Bit { S_LENGTH0, S_LENGTH1, S_LENGTH2, S_LENGTH3, S_LENGTH4, LENGTH5, DUTY0, DUTY1 }
	
	private enum NR22 implements Bit { ENV_LENGTH0, ENV_LENGTH1, ENV_LENGTH3, ENVELOPE, ENV_DEF0, ENV_DEF1, ENV_DEF2, ENV_DEF3 }
	
	private enum NR24 implements Bit { HIGH_FREQ0, HIGH_FREQ1, HIGH_FREQ2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }
	
	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
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

	private int getWaveDuty() {
		return soundRegs.asInt(Reg.NR21, NR21.DUTY0, NR21.DUTY1);
	}
	
	public int getFrequency() {
		return soundRegs.get(Reg.NR23);
	}
	
	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S2_START && address < AddressMap.REGS_S2_END) {
			return soundRegs.get(address - AddressMap.REGS_S2_START);
		}
		
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S2_START && address < AddressMap.REGS_S2_END) {
			soundRegs.set(address - AddressMap.REGS_S2_START, data);
		}
	}
}
