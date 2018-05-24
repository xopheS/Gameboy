package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.component.Component;

public final class Sound2 implements Component {
	private enum Reg implements Register { NR21, NR22, NR23, NR24 }
	
	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());

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
