package ch.epfl.gameboj.component.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.mvc.View;

public final class SoundController implements Component, Clocked {
	private enum Reg implements Register { NR50, NR51, NR52 }
	
	private enum NR50 implements Bit { SO1_LEVEL0, SO1_LEVEL1, SO1_LEVEL2, SO1_POW, SO2_LEVEL0, SO2_LEVEL1, SO2_LEVEL2, SO2_POW }
	
	private enum NR51 implements Bit { SO1_1, SO2_1, SO3_1, SO4_1, SO1_2, SO2_2, SO3_2, SO4_2 }
	
	private enum NR52 implements Bit { SOUND1, SOUND2, SOUND3, SOUND4, UNUSED4, UNUSED5, UNUSED6, POW }
	
	public static final int SAMPLE_RATE = 44100;
	
	private Bus bus;
	
	private final SourceDataLine line;
	
	private final RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private byte[][] soundBuffers; 
	private byte[] soundBuffer;
	
	int soundBufferIndex;
	
	private final Sound1 sound1 = new Sound1(this);
	private final Sound2 sound2 = new Sound2();
	private final Sound3 sound3 = new Sound3();
	private final Sound4 sound4 = new Sound4();
	
	View view = View.getView();
	
	public SoundController(Cpu cpu) throws LineUnavailableException {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 2, 2, SAMPLE_RATE, true);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		soundRegs.setBit(Reg.NR52, NR52.SOUND1, true);
		soundRegs.setBit(Reg.NR52, NR52.SOUND2, true);
		soundRegs.setBit(Reg.NR52, NR52.SOUND3, true);
		soundRegs.setBit(Reg.NR52, NR52.SOUND4, true);
		
		line = (SourceDataLine) AudioSystem.getLine(info);
		
		line.open(format, 60000);
		
		soundBuffers = new byte[4][1500];
		soundBuffer = new byte[line.getBufferSize()];
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				line.start();
			}
		}).start();
	}
	
	@Override
	public void cycle(long cycle) {
		if (cycle % (GameBoy.CYCLES_PER_SECOND / SAMPLE_RATE) == 0 && isOn()) {
			reallyCycle(cycle);
		}
	}
	
	private void reallyCycle(long cycle) {
		initSounds();
		
		if (soundRegs.testBit(Reg.NR52, NR52.SOUND1)) {
			sound1.cycle(cycle);

			soundBuffers[0][soundBufferIndex] = (byte) (sound1.getWave()[(int) (((32 * sound1.getIndex() * sound1.getFrequency()) / SAMPLE_RATE) % 32)]);
			
			sound1.incIndex();
		}
		
		if (soundRegs.testBit(Reg.NR52, NR52.SOUND2)) {
			sound2.cycle(cycle);
			
			soundBuffers[1][soundBufferIndex] = (byte) sound2.getWave()[(int) (((32 * sound2.getIndex() * sound2.getFrequency()) / SAMPLE_RATE) % 32)];
			
			sound2.incIndex();
		}
		
		if (soundRegs.testBit(Reg.NR52, NR52.SOUND3)) {	
			sound3.cycle(cycle);
			
			soundBuffers[2][soundBufferIndex] = (byte) sound3.getWave()[(int) (((32 * sound2.getIndex() * sound2.getFrequency()) / SAMPLE_RATE) % 32)];
			
			switch (sound3.getOutputLevel()) {
			case 0:
				soundBuffers[2][soundBufferIndex] = 0;
				break;
			case 1:
				break;
			case 2:
				soundBuffers[2][soundBufferIndex] >>= 1;
				break;
			case 3:
				soundBuffers[2][soundBufferIndex] >>= 2;
				break;
			}
			
			sound3.incIndex();
		}
		
		if (soundRegs.testBit(Reg.NR52, NR52.SOUND4)) {
			sound4.cycle(cycle);
			
			soundBuffers[3][soundBufferIndex] = (byte) sound4.getWave()[(int) (((32 * sound2.getIndex() * sound2.getFrequency()) / SAMPLE_RATE) % 32)];
			
			sound4.incIndex();
		}
		
		setSoundBufferByte(soundBufferIndex);
		
		soundBufferIndex++;
		
		if (soundBufferIndex == soundBuffers.length / 2) {
			line.write(soundBuffer, 0, 2 * Math.min(line.available(), soundBuffers.length));
			soundBufferIndex = 0;
		}
	}
	
	public void setSound1Pow(boolean pow) {
		soundRegs.setBit(Reg.NR52, NR52.SOUND1, pow);
	}
	
	public void setSound2Pow(boolean pow) {
		soundRegs.setBit(Reg.NR52, NR52.SOUND2, pow);
	}
	
	public void setSound3Pow(boolean pow) {
		soundRegs.setBit(Reg.NR52, NR52.SOUND3, pow);
	}
	
	public void setSound4Pow(boolean pow) {
		soundRegs.setBit(Reg.NR52, NR52.SOUND4, pow);
	}
	
	private void initSounds() {
		initSound1();
		initSound2();
		initSound3();
		initSound4();
	}
	
	private void initSound1() {
		if (sound1.isReset()) {	
			sound1.setInternalFreq(sound1.getDefaultInternalFrequency());
			sound1.setLength();
			soundRegs.setBit(Reg.NR52, NR52.SOUND1, true);
			sound1.reset();
		}
	}
	
	private void initSound2() {
		if (sound2.isReset()) {
			soundRegs.setBit(Reg.NR52, NR52.SOUND2, true);
		}
	}
	
	private void initSound3() {
		if (sound3.isReset()) {
			soundRegs.setBit(Reg.NR52, NR52.SOUND3, true);
		}
	}
	
	private void initSound4() {
		if (sound4.isReset()) {
			soundRegs.setBit(Reg.NR52, NR52.SOUND4, true);
		}
	}
	
	private void setSoundBufferByte(int byteIndex) {
		int mono1 = 0, mono2 = 0;
		
		if (soundRegs.testBit(Reg.NR51, NR51.SO1_1)) {
			mono1 += soundBuffers[0][soundBufferIndex];
		}
		if (soundRegs.testBit(Reg.NR51, NR51.SO2_1)) {
			mono1 += soundBuffers[1][soundBufferIndex];
		}
		if (soundRegs.testBit(Reg.NR51, NR51.SO3_1)) {
			mono1 += soundBuffers[2][soundBufferIndex];
		}
		if (soundRegs.testBit(Reg.NR51, NR51.SO4_1)) {
			mono1 += soundBuffers[3][soundBufferIndex];
		}
		mono1 *= soundRegs.asInt(Reg.NR50, NR50.SO1_LEVEL0, NR50.SO1_LEVEL1, NR50.SO1_LEVEL2);
		mono1 *= view.getCurrentVolume().get() * 0.005;
		if (soundRegs.testBit(Reg.NR51, NR51.SO1_2)) {
			mono2 += soundBuffers[0][soundBufferIndex];
		}
		if (soundRegs.testBit(Reg.NR51, NR51.SO2_2)) {
			mono2 += soundBuffers[1][soundBufferIndex];
		}
		if (soundRegs.testBit(Reg.NR51, NR51.SO3_2)) {
			mono2 += soundBuffers[2][soundBufferIndex];
		}
		if (soundRegs.testBit(Reg.NR51, NR51.SO4_2)) {
			mono2 += soundBuffers[3][soundBufferIndex];
		}
		mono2 *= soundRegs.asInt(Reg.NR50, NR50.SO2_LEVEL0, NR50.SO2_LEVEL1, NR50.SO2_LEVEL2);
		mono2 *= view.getCurrentVolume().get() * 0.005;
		
		soundBuffer[byteIndex * 2] = (byte) mono1;
		soundBuffer[byteIndex * 2 + 1] = (byte) mono2;
	}
	
	private boolean isOn() {
		return soundRegs.testBit(Reg.NR52, NR52.POW);
	}
	
	public SourceDataLine getLine() {
		return line;
	}
	
	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
		bus.attach(this);
		bus.attach(sound1);
	}

	@Override
	public int read(int address) {
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_SC_START && address < AddressMap.REGS_SC_END) {
			return soundRegs.get(address - AddressMap.REGS_SC_START);
		}
		
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		
		if (Preconditions.checkBits16(address) >= AddressMap.REGS_SC_START && address < AddressMap.REGS_SC_END) {
			soundRegs.set(address - AddressMap.REGS_SC_START, data);
		}
	}	
}
