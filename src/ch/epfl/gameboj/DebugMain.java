package ch.epfl.gameboj;
import java.io.File;
import java.io.IOException;

import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.DebugPrintComponent;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;

public final class DebugMain {
    private DebugMain() {
        
    }
    //First argument: name of ROM file, second argument: number of cycles to execute (30 000 000)
    public static void main(String[] args) throws IOException {
        File romFile = new File(args[0]);
        long cycles = Long.parseLong(args[1]);

        GameBoy gb = new GameBoy(Cartridge.ofFile(romFile));
        Component printer = new DebugPrintComponent();
        printer.attachTo(gb.bus());
        while (gb.cycles() < cycles) {
            long nextCycles = Math.min(gb.cycles() + 17556, cycles);
            gb.runUntil(nextCycles);
            gb.cpu().requestInterrupt(Cpu.Interrupt.VBLANK);          
        }
    }
}
