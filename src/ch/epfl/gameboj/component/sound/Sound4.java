package ch.epfl.gameboj.component.sound;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.component.Component;

public final class Sound4 implements Component {
	private enum Reg implements Register { NR41, NR42, NR43, NR44 }
	
	private RegisterFile<Register> soundRegs = new RegisterFile<>(Reg.values());

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
}
