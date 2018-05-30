package ch.epfl.gameboj.component.sound;

import static ch.epfl.gameboj.component.sound.SoundController.SAMPLE_RATE;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.memory.Ram;

public final class Sound3 extends SoundCircuit {
	private enum Reg implements Register { NR30, NR31, NR32, NR33, NR34 }
	
	private enum NR30 implements Bit { UNUSED0, UNUSED1, UNUSED2, UNUSED3, UNUSED4, UNUSED5, UNUSED6, POWER }
	
	private enum NR32 implements Bit { UNUSED0, UNUSED1, UNUSED2, UNUSED3, UNUSED4, LEVEL0, LEVEL1, UNUSED7 }
	
	private enum NR34 implements Bit { FREQ0, FREQ1, FREQ2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }

	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private int index;
	private int length;
	
	private int[] wave = new int[32];
	
	private int internalFreq;
	
	private final SoundController soundController;
	
	private Ram waveRam = new Ram(AddressMap.WAVE_RAM_SIZE);
	
	public Sound3(SoundController soundController) {
		this.soundController = soundController;
	}
	
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
		for (int i = 0x30; i < 0x40; ++i) {
			wave[(i - 0x30) * 2] = waveRam.read(i - AddressMap.WAVE_RAM_START);
			wave[(i - 0x30) * 2] = waveRam.read(i - AddressMap.WAVE_RAM_START);
		}
		
		return wave;
	}
	
	public int getDefaultInternalFreq() {
		return soundRegs.get(Reg.NR33) | soundRegs.asInt(Reg.NR34, NR34.FREQ0, NR34.FREQ1, NR34.FREQ2);
	}
	
	public float getFreq() {
		return 4194304 / (8 * (2048 - internalFreq));
	}
	
	public int getOutputLevel() {
		return soundRegs.asInt(Reg.NR32, NR32.LEVEL0, NR32.LEVEL1);
	}
	
	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S3_START && address < AddressMap.REGS_S3_END) {
			return soundRegs.get(address - AddressMap.REGS_S3_START);
		} else if (address >= AddressMap.WAVE_RAM_START && address < AddressMap.WAVE_RAM_END) {
			return waveRam.read(address - AddressMap.WAVE_RAM_START);
		}
		
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S3_START && address < AddressMap.REGS_S3_END) {
			soundRegs.set(address - AddressMap.REGS_S3_START, data);
		} else if (address >= AddressMap.WAVE_RAM_START && address < AddressMap.WAVE_RAM_END) {
			waveRam.write(address - AddressMap.WAVE_RAM_START, data);
		}
	}
	
	public void setLength() {
		length =  ((64 - soundRegs.get(Reg.NR31)) * SAMPLE_RATE) / 256;
	}

	@Override
	public void cycle(long cycle) {
		if (isCounterActive() && length > 0) {
			length--;
			if (length == 0) {
				soundController.setSound1Pow(false);
			}
		}
	}

	@Override
	protected void reset() {
		setLength();
		internalFreq = getDefaultInternalFreq();
	}

	@Override
	public boolean isCounterActive() {
		return soundRegs.testBit(Reg.NR34, NR34.COUNTER);
	}
}
