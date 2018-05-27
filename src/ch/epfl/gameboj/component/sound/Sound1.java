package ch.epfl.gameboj.component.sound;

import static ch.epfl.gameboj.component.sound.SoundController.SAMPLE_RATE;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;

public final class Sound1 extends SoundCircuit implements Component, Clocked {
	private enum Reg implements Register { NR10, NR11, NR12, NR13, NR14 }
	
	private enum NR10 implements Bit { SWEEP_SHIFT0, SWEEP_SHIFT1, SWEEP_SHIFT2, SWEEP_INC, SWEEP_TIME0, SWEEP_TIME1, SWEEP_TIME2, UNUSED7 }
	
	private enum NR11 implements Bit { S_LENGTH0, S_LENGTH1, S_LENGTH2, S_LENGTH3, S_LENGTH4, S_LENGTH5, DUTY0, DUTY1 }
	
	private enum NR12 implements Bit { ENV_LENGTH0, ENV_LENGTH1, ENV_LENGTH2, ENVELOPE, ENV_DEF0, ENV_DEF1, ENV_DEF2, ENV_DEF3 }
	
	private enum NR14 implements Bit { HIGH_FREQ0, HIGH_FREQ1, HIGH_FREQ2, UNUSED3, UNUSED4, UNUSED5, COUNTER, INIT }
	
	private final RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private final SoundController controller;
	
	private int index;
	private int length;
	
	private int internalFreq;
	
	private Envelope volume = new Envelope();
	private int sweepIndex;
	private int sweepLength;
	private int sweepDirection;
	private int sweepShift;
	
	private int[] wave = new int[32];
	
	public Sound1(SoundController controller) {
		this.controller = controller;
	}
	
	public Envelope getVolume() {
		return volume;
	}
	
	public void setVolume(Envelope volume) {
		this.volume = volume;
	}
	
	public int getSweepIndex() {
		return sweepIndex;
	}
	
	public void setSweepIndex(int sweepIndex) {
		this.sweepIndex = sweepIndex;
	}
	
	public void decSweepIndex() {
		sweepIndex--;
	}
	
	public int getSweepLength() {
		return sweepLength;
	}
	
	public void setSweepLength(int sweepLength) {
		this.sweepLength = sweepLength;
	}
	
	public int getSweepDirection() {
		return sweepDirection;
	}
	
	public void setSweepDirection(int sweepDirection) {
		this.sweepDirection = sweepDirection;
	}
	
	public int getSweepShift() {
		return sweepShift;
	}
	
	public void setSweepShift(int sweepShift) {
		this.sweepShift = sweepShift;
	}
	
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
	
	public boolean isCounterActive() {
		return soundRegs.testBit(Reg.NR14, NR14.COUNTER);
	}
	
	public void setLength() {
		length =  ((64 - soundRegs.asInt(Reg.NR11, NR11.S_LENGTH0, NR11.S_LENGTH1, NR11.S_LENGTH2, NR11.S_LENGTH3, NR11.S_LENGTH4, NR11.S_LENGTH5))
				* SAMPLE_RATE) / 256;
	}
	
	public int getLength() {
		return length;
	}
	
	public void decLength() {
		length--;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void incIndex() {
		index++;
	}
	
	public int getDefaultBase() {
		return soundRegs.asInt(Reg.NR12, NR12.ENV_DEF0, NR12.ENV_DEF1, NR12.ENV_DEF2, NR12.ENV_DEF3);
	}
	
	private int getWaveDuty() {
		return soundRegs.asInt(Reg.NR11, NR11.DUTY0, NR11.DUTY1);
	}
	
	public void setInternalFreq(int internalFreq) {
		this.internalFreq = internalFreq;
	}
	
	public int getInternalFreq() {
		return internalFreq;
	}
	
	public int getDefaultInternalFrequency() {
		int freqData = soundRegs.get(Reg.NR13) | (soundRegs.asInt(Reg.NR14, NR14.HIGH_FREQ0, NR14.HIGH_FREQ1, NR14.HIGH_FREQ2) << Byte.SIZE); 
		return freqData;
	}
	
	public float getFrequency() {
		return (4194304 / (8 * (2048 - internalFreq)));
	}
	
	public boolean isReset() {
		return soundRegs.testBit(Reg.NR14, NR14.INIT);
	}
	
	public void reset() {
		index = 0;
		volume.setBase(getDefaultBase());
		volume.setDirection(soundRegs.testBit(Reg.NR12, NR12.ENVELOPE) ? 1 : 0);
		volume.setStepLength(soundRegs.asInt(Reg.NR12, NR12.ENV_LENGTH0, NR12.ENV_LENGTH1, NR12.ENV_LENGTH2));
		volume.setIndex(volume.getStepLength());
		soundRegs.setBit(Reg.NR14, NR14.INIT, false);
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
			soundRegs.set(address - AddressMap.REGS_S1_START, data);
		}
	}
}
