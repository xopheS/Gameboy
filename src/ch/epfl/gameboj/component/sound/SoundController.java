package ch.epfl.gameboj.component.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;

public final class SoundController implements Component, Clocked {
	private enum Reg implements Register { NR50, NR51, NR52 }
	
	private enum NR50 implements Bit { SO1_LEVEL0, SO1_LEVEL1, SO1_LEVEL2, SO1_POW, SO2_LEVEL0, SO2_LEVEL1, SO2_LEVEL2, SO2_POW }
	
	private enum NR51 implements Bit { SO1_1, SO1_2, SO1_3, SO1_4, SO2_1, SO2_2, SO2_3, SO2_4 }
	
	private enum NR52 implements Bit { SOUND1, SOUND2, SOUND3, SOUND4, UNUSED4, UNUSED5, UNUSED6, POW }
	
	private static final int SAMPLE_RATE = 44100;
	
	private Bus bus;
	
	private final SourceDataLine line;
	
	private final RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());
	
	private byte[][] soundBuffers; //TODO
	private byte[] soundBuffer;
	
	int soundBufferIndex;
	
	private final Sound1 sound1 = new Sound1();
	private final Sound2 sound2 = new Sound2();
	private final Sound3 sound3 = new Sound3();
	private final Sound4 sound4 = new Sound4();
	
	public SoundController(Cpu cpu) throws LineUnavailableException {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 2, 2, SAMPLE_RATE, true);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		line = (SourceDataLine) AudioSystem.getLine(info);
		
		line.open(format);
		
		soundBuffers = new byte[4][1500];
		soundBuffer = new byte[line.getBufferSize()];
		
		line.start();
	}
	
	@Override
	public void cycle(long cycle) {
		if (cycle % 100 == 0) {
			reallyCycle();
		}
	}
	
	private void reallyCycle() {
		if (isOn()) {
			if (soundRegs.testBit(Reg.NR52, NR52.SOUND1)) {
				soundBuffers[0][soundBufferIndex] = (byte) sound1.getWave()[((sound1.getIndex() * sound1.getFrequency()) / SAMPLE_RATE) % 32];
				
				sound1.incIndex();
			}
			
			if (soundRegs.testBit(Reg.NR52, NR52.SOUND2)) {
				soundBuffers[1][soundBufferIndex] = (byte) sound2.getWave()[((sound2.getIndex() * sound2.getFrequency()) / SAMPLE_RATE) % 32];
				
				sound2.incIndex();
			}
			
			soundBuffers[0][soundBufferIndex] = (byte) sound1.getWave()[((sound1.getIndex() * sound1.getFrequency()) / SAMPLE_RATE) % 32];
			
			sound1.incIndex();
			
			soundBuffers[1][soundBufferIndex] = (byte) sound2.getWave()[((sound2.getIndex() * sound2.getFrequency()) / SAMPLE_RATE) % 32];
			
			sound1.incIndex();
			
			if (soundRegs.testBit(Reg.NR52, NR52.SOUND3)) {
				soundBuffers[2][soundBufferIndex] = 0;
			}

			if (soundRegs.testBit(Reg.NR52, NR52.SOUND4)) {
				soundBuffers[3][soundBufferIndex] = 0;
			}
			
			setSoundBufferByte(soundBufferIndex);
			
			soundBufferIndex++;
			
			if (soundBufferIndex == soundBuffers.length / 2) {
				line.write(soundBuffer, 0, 2 * Math.min(line.available(), soundBuffers.length));
				soundBufferIndex = 0;
			}
		}
	}
	
	private void setSoundBufferByte(int byteIndex) {
		int mono1 = 0, mono2 = 0;
		
		soundBuffer[byteIndex * 2] = (byte) mono1;
		soundBuffer[byteIndex * 2 + 1] = (byte) mono2;
	}
	
	private boolean isOn() {
		return soundRegs.testBit(Reg.NR52, NR52.POW);
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
			if (address == AddressMap.REG_NR52) {
				System.out.println(Integer.toBinaryString(data));
				if (Bits.test(data, NR52.POW)) {
					System.out.println("Sound on");
				}
				if (Bits.test(data, NR52.SOUND1)) {
					System.out.println("Sound 1 on");
				}
				if (Bits.test(data, NR52.SOUND2)) {
					System.out.println("Sound 2 on");
				}
				if (Bits.test(data, NR52.SOUND3)) {
					System.out.println("Sound 3 on");
				}
				if (Bits.test(data, NR52.SOUND4)) {
					System.out.println("Sound 4 on");
				}
			}
		}
	}	
}
