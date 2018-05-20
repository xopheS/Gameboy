package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Alu.Flag;
import ch.epfl.gameboj.component.cpu.Alu.RotDir;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.input.InputPort;
import ch.epfl.gameboj.input.OutputPort;

/**
 * Cette classe modélise le processeur de la Gameboy.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class DebugCpu implements Component, Clocked {
    private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
    private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);

    public static final int OPCODE_PREFIX = 0xCB;

    private Bus bus;
    private final Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

    private final InputPort P10 = new InputPort();
    private final InputPort P11 = new InputPort();
    private final InputPort P12 = new InputPort();
    private final InputPort P13 = new InputPort();
    private final OutputPort P14 = new OutputPort();
    private final OutputPort P15 = new OutputPort();

    // PC = program counter, stores the address of the next instruction
    public int PC = 0;
    // SP = stack pointer, stores the address of the top of the stack
    private int SP = 0;
    // IME = interrupt master enable, tells us if interrupts are enabled or not
    private boolean IME = false;
    // IE = interrupt enable, tells us if corresponding interrupt is enabled
    private int IE = 0;
    // IF = interrupt flags, tells us if corresponding interrupt is happening
    private int IF = 0;

    private enum Reg implements Register {
        A, F, B, C, D, E, H, L
    }

    private enum Reg16 implements Register {
        AF(Reg.A, Reg.F), BC(Reg.B, Reg.C), DE(Reg.D, Reg.E), HL(Reg.H, Reg.L);

        private Reg a, b;

        Reg16(Reg a, Reg b) {
            this.a = a;
            this.b = b;
        }
    };

    private enum FlagSrc {
        V0, V1, ALU, CPU
    };

    /**
     * Cette énumération donne les interruptions possibles du processeur, dans
     * l'ordre dans lequel elles apparaissent dans les registres IF et IE.
     *
     */
    public enum Interrupt implements Bit {
        VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
    }

    private final RegisterFile<Register> registers8 = new RegisterFile<>(Reg.values());

    private long nextNonIdleCycle;

    private static Opcode[] buildOpcodeTable(Opcode.Kind opKind) {
        Opcode[] opcodeTable = new Opcode[256];
        for (Opcode o : Opcode.values()) {
            if (o.kind == opKind) {
                opcodeTable[o.encoding] = o;
            }
        }
        return opcodeTable;
    }

    private int pendingInterrupts() {
        return IF & IE;
    }

    private boolean isInterruptPending() {
        return pendingInterrupts() != 0;
    }

    private int prioritaryInterruptIndex() {
        return Integer.numberOfTrailingZeros(pendingInterrupts());
    }

    private void handleInterrupt() { // TODO recheck for precision/fidelity
        int i = prioritaryInterruptIndex();
        IF = Bits.set(IF, i, false);
        IME = false;
        push16(PC);
        PC = AddressMap.INTERRUPTS[i];
        nextNonIdleCycle += 5;
    }

    /**
     * Cette méthode s'occupe du fonctionnement du processeur à un très haut niveau,
     * elle décide si celui-ci doit faire quelquechose ou pas, et appelle la méthode
     * {@link #reallyCycle()} si oui.
     *
     * @param cycle
     *            C'est le cycle actuel, au moment de l'appel de la fonction
     */
    @Override
    public void cycle(long cycle) {
        boolean isInterruptPending = isInterruptPending();

        if (!isOn() && isInterruptPending) {
            nextNonIdleCycle = cycle;
        }

        if (cycle == nextNonIdleCycle) {
            reallyCycle(isInterruptPending);
        }
    }

    /**
     * Cette méthode entreprend un cycle de travail du processeur, qui peut donc
     * traiter une interruption ou initier une instruction si il n'y a pas
     * d'interruptions à traiter, i.e. appeler la méthode {@link #dispatch(Opcode)}
     * avec l'opcode indiqué par le PC (program counter)
     *
     */
    public void reallyCycle(boolean interruptPending) {
        if (IME && interruptPending) {
            handleInterrupt();
        } else {
            int nextInstruction = bus.read(PC);
            Opcode nextOpcode = nextInstruction == OPCODE_PREFIX ? PREFIXED_OPCODE_TABLE[read8AfterOpcode()]
                    : DIRECT_OPCODE_TABLE[nextInstruction];
            dispatch(nextOpcode);
        }
    }

    /**
     * Cette méthode effectue une instruction codée par l'opcode qui lui est fourni.
     *
     * @param opcode
     *            L'opcode qui indique l'instruction à effectuer
     */
    private void dispatch(Opcode opcode) {
        int nextPC = PC + opcode.totalBytes;
        boolean instructionDone = false;
        
        System.out.println("cpu result " + opcode.family.name());

        PC = nextPC;
    }

    private boolean isOn() {
        return !(nextNonIdleCycle == Long.MAX_VALUE);
    }

    /**
     * Cette méthode est une redéfinition de attachTo qui permet de connaître le bus
     * auquel le processeur a été attaché.
     *
     * @param bus
     *            le bus auquel le processeur est attaché
     */
    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        bus.attach(this);
    }

    /**
     * Cette méthode permet de demander au processeur d'initier une interruption.
     *
     * @param i
     *            Le type de l'interruption à initier
     */
    public void requestInterrupt(Interrupt i) {
        IF = Bits.set(IF, i.index(), true);
    }

    private int read8(int address) {
        return bus.read(address);
    }

    private int read8AtHl() {
        return read8(reg16(Reg16.HL));
    }

    private int read8AfterOpcode() {
        assert PC < 0xFFFF : "Valeur invalide pour PC superieure a 16 bits";
        return read8(PC + 1);
    }

    private int read16(int address) {
        assert address < 0xFFFF : "Adresse superieure a 16 bits";
        return Bits.make16(read8(address + 1), read8(address));
    }

    private int read16AfterOpcode() {
        return read16(PC + 1);
    }

    private void write8(int address, int v) {
        bus.write(address, v);
    }

    private void write16(int address, int v) {
        write8(address, Bits.clip(8, v));
        write8(address + 1, Bits.extract(v, 8, 8));
    }

    private void write8AtHl(int v) {
        write8(reg16(Reg16.HL), v);
    }

    private void push16(int v) {
        SP = Bits.clip(16, SP - 2);
        write16(SP, v);
    }

    private int pop16() {
        int poppedInt = read16(SP);
        SP = Bits.clip(16, SP + 2);
        return poppedInt;
    }

    private int reg16(Reg16 r) {
        return Bits.make16(registers8.get(r.a), registers8.get(r.b));
    }

    private void setReg16(Reg16 r, int newV) {
        if (r == Reg16.AF) {
            newV &= 0b11111111_11111111_11111111_11110000;
        }
        registers8.set(r.b, Bits.clip(8, newV));
        registers8.set(r.a, Bits.extract(newV, 8, 8));
    }

    private void setReg16SP(Reg16 r, int newV) {
        if (r == Reg16.AF) {
            SP = newV;
        } else {
            setReg16(r, newV);
        }
    }

    private Reg extractReg(Opcode opcode, int startBit) {
        switch (Bits.extract(opcode.encoding, startBit, 3)) {
        case 0:
            return Reg.B;
        case 1:
            return Reg.C;
        case 2:
            return Reg.D;
        case 3:
            return Reg.E;
        case 4:
            return Reg.H;
        case 5:
            return Reg.L;
        case 6:
            throw new NullPointerException();
        case 7:
            return Reg.A;
        default:
            break;
        }

        return null;
    }

    private Reg16 extractReg16(Opcode opcode) {
        switch (Bits.extract(opcode.encoding, 4, 2)) {
        case 0:
            return Reg16.BC;
        case 1:
            return Reg16.DE;
        case 2:
            return Reg16.HL;
        case 3:
            return Reg16.AF;
        default:
            break;
        }

        return null;
    }

    private int extractHlIncrement(Opcode opcode) {
        return Bits.test(opcode.encoding, 4) ? -1 : 1;
    }

    private void setRegFromAlu(Reg r, int vf) {
        registers8.set(r, Alu.unpackValue(vf));
    }

    private void setFlags(int valueFlags) {
        registers8.set(Reg.F, Alu.unpackFlags(valueFlags));
    }

    private void setRegFlags(Reg r, int vf) {
        setRegFromAlu(r, vf);
        setFlags(vf);
    }

    private void write8AtHlAndSetFlags(int vf) {
        write8AtHl(Alu.unpackValue(vf));
        setFlags(vf);
    }

    private void combineAluFlags(int vf, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
        FlagSrc[] tableFlag = { c, h, n, z };
        for (int i = 0; i < tableFlag.length; i++) {
            switch (tableFlag[i]) {
            case V0:
                registers8.setBit(Reg.F, Flag.values()[4 + i], false);
                break;
            case V1:
                registers8.setBit(Reg.F, Flag.values()[4 + i], true);
                break;
            case ALU:
                registers8.setBit(Reg.F, Flag.values()[4 + i], Bits.test(Alu.unpackFlags(vf), Flag.values()[4 + i]));
                break;
            case CPU:
                registers8.setBit(Reg.F, Flag.values()[4 + i], Bits.test(registers8.get(Reg.F), Flag.values()[4 + i]));
                break;
            default:
                break;
            }
        }

        registers8.set(Reg.F, registers8.get(Reg.F) & 0b1111_0000);
    }

    private RotDir extractRotDir(Opcode opcode) {
        return Bits.test(opcode.encoding, 3) ? RotDir.RIGHT : RotDir.LEFT;
    }

    private int extractBitIndex(Opcode opcode) {
        return Bits.extract(opcode.encoding, 3, 3);
    }

    private boolean extractSetValue(Opcode opcode) {
        return Bits.test(opcode.encoding, 6);
    }

    private boolean getInitialCarry(Opcode opcode) {
        return Bits.test(opcode.encoding, 3) & registers8.testBit(Reg.F, Flag.C);
    }

    private boolean testCondition(Opcode opcode) {
        switch (Bits.extract(opcode.encoding, 3, 2)) {
        case 0:
            return !registers8.testBit(Reg.F, Flag.Z);
        case 1:
            return registers8.testBit(Reg.F, Flag.Z);
        case 2:
            return !registers8.testBit(Reg.F, Flag.C);
        case 3:
            return registers8.testBit(Reg.F, Flag.C);
        default:
            return false;
        }
    }

    /**
     * Cette méthode permet d'accéder aux registres IE et IF sur le bus, ainsi qu'au
     * RAM du processeur (high RAM).
     *
     * @param address
     *            L'adresse à laquelle la lecture doit être effectuée
     *
     * @return la valeur lue à l'adresse en question
     *
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas un entier sur 16 bits
     */
    @Override
    public int read(int address) {
        // TODO refactor with switch and default case
        if (Preconditions.checkBits16(address) == AddressMap.REG_IE) {
            return IE;
        } else if (address == AddressMap.REG_IF) {
            return IF;
        } else if (address >= AddressMap.HRAM_START && address < AddressMap.HRAM_END) {
            return highRam.read(address - AddressMap.HRAM_START);
        }
        
        return NO_DATA;
    }

    /**
     * Cette méthode permet d'écrire sur le bus dans les registres IE et IF, ainsi
     * que dans le RAM du processeur.
     *
     * @param address
     *            L'adresse à laquelle l'écriture doit être effectuée
     *
     * @param data
     *            La valeur à écrire à l'endroit respectif
     *
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas sur 16 bits ou la valeur à y écrire n'est
     *             pas sur 8 bits
     */
    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);

        if (Preconditions.checkBits16(address) == AddressMap.REG_IE) {
            IE = data;
        } else if (address == AddressMap.REG_IF) {
            IF = data;
        } else if (address >= AddressMap.HRAM_START && address < AddressMap.HRAM_END) {
            highRam.write(address - AddressMap.HRAM_START, data);
        }
    }

    /**
     * Méthode de déboguage, permet d'obtenir la valeur des registres.
     *
     * @return un tableau contenant les valeurs des registres
     */
    public int[] _testGetPcSpAFBCDEHL() {
        return new int[] { PC, SP, registers8.get(Reg.A), registers8.get(Reg.F), registers8.get(Reg.B),
                registers8.get(Reg.C), registers8.get(Reg.D), registers8.get(Reg.E), registers8.get(Reg.H),
                registers8.get(Reg.L) };
    }
}

