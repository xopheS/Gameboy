package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;

public final class Sound4 extends SoundCircuit {
	private enum Reg implements Register { NR41, NR42, NR43, NR44 }
	
	private enum NR42 implements Bit { LENGTH0, LENGTH1, LENGTH2, ENVELOPE, ENV_DEF0, ENV_DEF1, ENV_DEF2, ENV_DEF3 }
	
	private enum NR43 implements Bit { DIV_SEL0, DIV_SEL1, DIV_SEL2, STEPS, SFREQ0, SFREQ1, SFREQ2, SFREQ3, SFREQ4 }
	
	private enum NR44 implements Bit { UNUSED0, UNUSED1, UNUSED2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }
	
	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private int index = 0;
	private int length;
	
	private int[] wave = new int[32];
	
	private final SoundController soundController;
	
	public Sound4(SoundController soundController) {
		this.soundController = soundController;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void incIndex() {
		index++;
	}
	
	public boolean isReset() {
		return soundRegs.testBit(Reg.NR44, NR44.INIT);
	}
	
	public int[] getWave() {
		return wave;
	}
	
	public float getFrequency() {
		return 10;
	}

	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_S4_START && address < AddressMap.REGS_S4_END) {
			return soundRegs.get(address - AddressMap.REGS_S4_START);
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

	@Override
	public void cycle(long cycle) {
		if (isCounterActive() && length > 0) {
			length--;
			if (length == 0) {
				soundController.setSound1Pow(false);
			}
		}
	}
	
	public Envelope getVolume() {
		return new Envelope();
	}

	@Override
	public float getFreq() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int getDefaultInternalFreq() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCounterActive() {
		return soundRegs.testBit(Reg.NR44, NR44.COUNTER);
	}
}
