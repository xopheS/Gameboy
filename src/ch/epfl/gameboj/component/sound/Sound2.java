package ch.epfl.gameboj.component.sound;

import static ch.epfl.gameboj.component.sound.SoundController.SAMPLE_RATE;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;

public final class Sound2 extends SquareWave {
	private enum Reg implements Register { NR21, NR22, NR23, NR24 }
	
	private enum NR21 implements Bit { S_LENGTH0, S_LENGTH1, S_LENGTH2, S_LENGTH3, S_LENGTH4, S_LENGTH5, DUTY0, DUTY1 }
	
	private enum NR22 implements Bit { ENV_LENGTH0, ENV_LENGTH1, ENV_LENGTH2, ENVELOPE, ENV_DEF0, ENV_DEF1, ENV_DEF2, ENV_DEF3 }
	
	private enum NR24 implements Bit { HIGH_FREQ0, HIGH_FREQ1, HIGH_FREQ2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }
	
	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private final SoundController soundController;
	
	private int index;
	private int length;
	
	private int internalFreq;
	
	private Envelope volume = new Envelope();
	
	public Sound2(SoundController soundController) {
		this.soundController = soundController;
	}
	
	public Envelope getVolume() {
		return volume;
	}
	
	public int[] getWave() {
		return getSquareWave(getWaveDuty());
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
	
	@Override
	public float getFreq() {
		return toFreq.apply(getDefaultInternalFreq());
	}
	
	@Override
	public void cycle(long cycle) {	
		if (isCounterActive() && length > 0) {
			length--;
			if (length == 0) {
				soundController.setSound1Pow(false);
			}
		}
		
		getVolume().handleSweep();
		
		index++;
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
			switch (address) {
			case AddressMap.REG_NR24:
				soundRegs.set(Reg.NR24, data);
				if (soundRegs.testBit(Reg.NR24, NR24.INIT)) {
					soundController.setSound2Pow(true);
					reset();
				}
				break;
			default:
				soundRegs.set(address - AddressMap.REGS_S2_START, data);
				break;
			}
		}
	}
	
	public void setLength() {
		length =  ((64 - soundRegs.asInt(Reg.NR21, NR21.S_LENGTH0, NR21.S_LENGTH1, NR21.S_LENGTH2, NR21.S_LENGTH3, NR21.S_LENGTH4, NR21.S_LENGTH5))
				* SAMPLE_RATE) / 256;
	}

	@Override
	protected int getDefaultInternalFreq() {
		return soundRegs.get(Reg.NR23) | (soundRegs.asInt(Reg.NR24, NR24.HIGH_FREQ0, NR24.HIGH_FREQ1, NR24.HIGH_FREQ2) << Byte.SIZE);
	}
	
	private int getDefaultEnvelope() {
		return soundRegs.asInt(Reg.NR22, NR22.ENV_DEF0, NR22.ENV_DEF1, NR22.ENV_DEF2, NR22.ENV_DEF3);
	}

	@Override
	protected void reset() {
		index = 0;
		setLength();
		internalFreq = getDefaultInternalFreq();
		volume.setBase(getDefaultEnvelope());
		volume.setDirection(soundRegs.testBit(Reg.NR22, NR22.ENVELOPE) ? 1 : 0);
		volume.setStepLength(soundRegs.asInt(Reg.NR22, NR22.ENV_LENGTH0, NR22.ENV_LENGTH1, NR22.ENV_LENGTH2));
		volume.setIndex(volume.getStepLength());
		soundRegs.setBit(Reg.NR24, NR24.INIT, false);
	}

	@Override
	public boolean isCounterActive() {
		return soundRegs.testBit(Reg.NR24, NR24.COUNTER);
	}
}
