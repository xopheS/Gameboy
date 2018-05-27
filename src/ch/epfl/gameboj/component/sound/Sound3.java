package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.Component;

public final class Sound3 extends SoundCircuit implements Component {
	private enum Reg implements Register { NR30, NR31, NR32, NR33, NR34 }
	
	private enum NR30 implements Bit { UNUSED0, UNUSED1, UNUSED2, UNUSED3, UNUSED4, UNUSED5, UNUSED6, POWER }
	
	private enum NR32 implements Bit { UNUSED0, UNUSED1, UNUSED2, UNUSED3, UNUSED4, LEVEL0, LEVEL1, UNUSED7 }
	
	private enum NR34 implements Bit { FREQ0, FREQ1, FREQ2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }

	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private int index = 0;
	
	private int[] wave = new int[32];
	
	public int getIndex() {
		return index;
	}
	
	public void incIndex() {
		index++;
	}

	public boolean isReset() {
		return soundRegs.testBit(Reg.NR34, NR34.INIT);
	}
	
	public int[] getWave() {
		return wave;
	}
	
	public int getOutputLevel() {
		return soundRegs.asInt(Reg.NR32, NR32.LEVEL0, NR32.LEVEL1);
	}
	
	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S3_START && address < AddressMap.REGS_S3_END) {
			return soundRegs.get(address - AddressMap.REGS_S3_START);
		}
		
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S3_START && address < AddressMap.REGS_S3_END) {
			soundRegs.set(address - AddressMap.REGS_S3_START, data);
		}
	}
}
