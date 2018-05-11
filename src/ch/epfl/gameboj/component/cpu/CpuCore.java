package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.Register;

public final class CpuCore {
 // PC = program counter, stores the address of the next instruction
    private int PC = 0;
    // SP = stack pointer, stores the address of the top of the stack
    private int SP = 0;
    // IME = interrupt master enable, tells us if interrupts are enabled or not
    private boolean IME = false;
    // IE = interrupt enable, tells us if corresponding interrupt is enabled
    private int IE = 0;
    // IF = interrupt flags, tells us if corresponding interrupt is happening
    private int IF = 0;

    private enum Reg implements Register { A, F, B, C, D, E, H, L }

    private enum Reg16 implements Register {
        AF(Reg.A, Reg.F), BC(Reg.B, Reg.C), DE(Reg.D, Reg.E), HL(Reg.H, Reg.L);

        private Reg a, b;

        Reg16(Reg a, Reg b) {
            this.a = a;
            this.b = b;
        }
    };

}
