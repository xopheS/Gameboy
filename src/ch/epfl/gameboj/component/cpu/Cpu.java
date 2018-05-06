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

/**
 * Cette classe modélise le processeur de la Gameboy.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class Cpu implements Component, Clocked {
    private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
    private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);

    private static final int opcodePrefix = 0xCB;

    private Bus bus;
    private final Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

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

    private enum Reg implements Register {
        A, F, B, C, D, E, H, L
    };

    private enum Reg16 implements Register {
        AF(Reg.A, Reg.F), BC(Reg.B, Reg.C), DE(Reg.D, Reg.E), HL(Reg.H, Reg.L),;

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
    public enum Interrupt implements Bit { VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD }

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

    private boolean pendingInterrupt() {
        return (IF & IE) != 0;
    }

    private void handleInterrupt() {
        int commonOnes = IF & IE;
        IME = false;
        int i = 31 - Integer.numberOfLeadingZeros(Integer.lowestOneBit(commonOnes));
        IF = Bits.set(IF, i, false);
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
        if (nextNonIdleCycle == Long.MAX_VALUE && pendingInterrupt()) {
            nextNonIdleCycle = cycle;
            reallyCycle();
        } else if (cycle == nextNonIdleCycle) {
            reallyCycle();
        }
    }

    /**
     * Cette méthode entreprend un cycle de travail du processeur, qui peut donc
     * traiter une interruption ou initier une instruction si il n'y a pas
     * d'interruptions à traiter, i.e. appeler la méthode {@link #dispatch(Opcode)}
     * avec l'opcode indiqué par le PC (program counter)
     *
     */
    private void reallyCycle() {
        if (IME && pendingInterrupt()) {
            handleInterrupt();
        } else {
            int nextInstruction = bus.read(PC);
            Opcode nextOpcode = nextInstruction == opcodePrefix ? PREFIXED_OPCODE_TABLE[read8AfterOpcode()] : DIRECT_OPCODE_TABLE[nextInstruction];
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

        switch (opcode.family) {
            case NOP:
                break;
            case LD_R8_HLR: {
                registers8.set(extractReg(opcode, 3), read8AtHl());
            }
                break;
            case LD_A_HLRU: {
                registers8.set(Reg.A, read8AtHl());
                setReg16(Reg16.HL, reg16(Reg16.HL) + extractHlIncrement(opcode));
            }
                break;
            case LD_A_N8R: {
                registers8.set(Reg.A, read8(AddressMap.REGS_START + read8AfterOpcode()));
            }
                break;
            case LD_A_CR: {
                registers8.set(Reg.A, read8(AddressMap.REGS_START + registers8.get(Reg.C)));
            }
                break;
            case LD_A_N16R: {
                registers8.set(Reg.A, read8(read16AfterOpcode()));
            }
                break;
            case LD_A_BCR: {
                registers8.set(Reg.A, read8(reg16(Reg16.BC)));
            }
                break;
            case LD_A_DER: {
                registers8.set(Reg.A, read8(reg16(Reg16.DE)));
            }
                break;
            case LD_R8_N8: {
                registers8.set(extractReg(opcode, 3), read8AfterOpcode());
            }
                break;
            case LD_R16SP_N16: {
                setReg16SP(extractReg16(opcode), read16AfterOpcode());
            }
                break;
            case POP_R16: {
                setReg16(extractReg16(opcode), pop16());
            }
                break;
            case LD_HLR_R8: {
                write8AtHl(registers8.get(extractReg(opcode, 0)));
            }
                break;
            case LD_HLRU_A: {
                write8AtHl(registers8.get(Reg.A));
                setReg16(Reg16.HL, reg16(Reg16.HL) + extractHlIncrement(opcode));
            }
                break;
            case LD_N8R_A: {
                write8(AddressMap.REGS_START + read8AfterOpcode(), registers8.get(Reg.A));
            }
                break;
            case LD_CR_A: {
                write8(AddressMap.REGS_START + registers8.get(Reg.C), registers8.get(Reg.A));
            }
                break;
            case LD_N16R_A: {
                write8(read16AfterOpcode(), registers8.get(Reg.A));
            }
                break;
            case LD_BCR_A: {
                write8(reg16(Reg16.BC), registers8.get(Reg.A));
            }
                break;
            case LD_DER_A: {
                write8(reg16(Reg16.DE), registers8.get(Reg.A));
            }
                break;
            case LD_HLR_N8: {
                write8AtHl(read8AfterOpcode());
            }
                break;
            case LD_N16R_SP: {
                write16(read16AfterOpcode(), SP);
            }
                break;
            case LD_R8_R8: {
                registers8.set(extractReg(opcode, 3), registers8.get(extractReg(opcode, 0)));
            }
                break;
            case LD_SP_HL: {
                SP = reg16(Reg16.HL);
            }
                break;
            case PUSH_R16: {
                push16(reg16(extractReg16(opcode)));
            }
                break;

            // Add
            case ADD_A_R8: {
                int sum = Alu.add(registers8.get(Reg.A), registers8.get(extractReg(opcode, 0)), getInitialCarry(opcode));
                setRegFlags(Reg.A, sum);
            }
                break;
            case ADD_A_N8: {
                int sum = Alu.add(registers8.get(Reg.A), read8AfterOpcode(), getInitialCarry(opcode));
                setRegFlags(Reg.A, sum);
            }
                break;
            case ADD_A_HLR: {
                int sum = Alu.add(registers8.get(Reg.A), read8AtHl(), getInitialCarry(opcode));
                setRegFlags(Reg.A, sum);
            }
                break;
            case INC_R8: {
                Reg r = extractReg(opcode, 3);
                int sum = Alu.add(registers8.get(r), 1);
                setRegFromAlu(r, sum);
                combineAluFlags(sum, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
            }
                break;
            case INC_HLR: {
                int sum = Alu.add(read8AtHl(), 1);
                write8AtHl(Alu.unpackValue(sum));
                combineAluFlags(sum, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
            }
                break;
            case INC_R16SP: {
                Reg16 r = extractReg16(opcode);
                int regValue = r == Reg16.AF ? SP : reg16(r);
                setReg16SP(r, Bits.clip(16, regValue + 1));
            }
                break;
            case ADD_HL_R16SP: {
                Reg16 r = extractReg16(opcode);
                int regValue = r == Reg16.AF ? SP : reg16(r);
                int sum = Alu.add16H(reg16(Reg16.HL), regValue);
                setReg16SP(Reg16.HL, Alu.unpackValue(sum));
                combineAluFlags(sum, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
            }
                break;
            case LD_HLSP_S8: {
                int sum = Alu.add16L(SP, Bits.clip(16, Bits.signExtend8(read8AfterOpcode())));
                if (Bits.test(opcode.encoding, 4)) {
                    setReg16(Reg16.HL, Alu.unpackValue(sum));
                } else {
                    SP = Alu.unpackValue(sum);
                }
                setFlags(Alu.unpackFlags(sum));
            }
                break;

            // Subtract
            case SUB_A_R8: {
                int sub = Alu.sub(registers8.get(Reg.A), registers8.get(extractReg(opcode, 0)),
                        getInitialCarry(opcode));
                setRegFlags(Reg.A, sub);
            }
                break;
            case SUB_A_N8: {
                int sub = Alu.sub(registers8.get(Reg.A), read8AfterOpcode(), getInitialCarry(opcode));
                setRegFlags(Reg.A, sub);
            }
                break;
            case SUB_A_HLR: {
                int sub = Alu.sub(registers8.get(Reg.A), read8AtHl(), getInitialCarry(opcode));
                setRegFlags(Reg.A, sub);
            }
                break;
            case DEC_R8: {
                Reg r = extractReg(opcode, 3);
                int sub = Alu.sub(registers8.get(r), 1);
                setRegFromAlu(r, sub);
                combineAluFlags(sub, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
            }
                break;
            case DEC_HLR: {
                int sub = Alu.sub(read8AtHl(), 1);
                write8AtHl(Alu.unpackValue(sub));
                combineAluFlags(sub, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
            }
                break;
            case CP_A_R8: {
                int sub = Alu.sub(registers8.get(Reg.A), registers8.get(extractReg(opcode, 0)));
                setFlags(sub);
            }
                break;
            case CP_A_N8: {
                int sub = Alu.sub(registers8.get(Reg.A), read8AfterOpcode());
                setFlags(sub);
            }
                break;
            case CP_A_HLR: {
                int sub = Alu.sub(registers8.get(Reg.A), read8AtHl());
                setFlags(sub);
            }
                break;
            case DEC_R16SP: {
                Reg16 r = extractReg16(opcode);
                int regValue = r == Reg16.AF ? SP : reg16(r);
                setReg16SP(r, Bits.clip(16, regValue - 1));
            }
                break;

            // And, or, xor, complement
            case AND_A_N8: {
                setRegFlags(Reg.A, Alu.and(registers8.get(Reg.A), read8AfterOpcode()));
            }
                break;
            case AND_A_R8: {
                setRegFlags(Reg.A, Alu.and(registers8.get(Reg.A), registers8.get(extractReg(opcode, 0))));
            }
                break;
            case AND_A_HLR: {
                setRegFlags(Reg.A, Alu.and(registers8.get(Reg.A), read8AtHl()));
            }
                break;
            case OR_A_R8: {
                setRegFlags(Reg.A, Alu.or(registers8.get(Reg.A), registers8.get(extractReg(opcode, 0))));
            }
                break;
            case OR_A_N8: {
                setRegFlags(Reg.A, Alu.or(registers8.get(Reg.A), read8AfterOpcode()));
            }
                break;
            case OR_A_HLR: {
                setRegFlags(Reg.A, Alu.or(registers8.get(Reg.A), read8AtHl()));
            }
                break;
            case XOR_A_R8: {
                setRegFlags(Reg.A, Alu.xor(registers8.get(Reg.A), registers8.get(extractReg(opcode, 0))));
            }
                break;
            case XOR_A_N8: {
                setRegFlags(Reg.A, Alu.xor(registers8.get(Reg.A), read8AfterOpcode()));
            }
                break;
            case XOR_A_HLR: {
                setRegFlags(Reg.A, Alu.xor(registers8.get(Reg.A), read8AtHl()));
            }
                break;
            case CPL: {
                int cpl = Bits.complement8(registers8.get(Reg.A));
                registers8.set(Reg.A, cpl);
                combineAluFlags(cpl, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);
            }
                break;

            // Rotate, shift
            case ROTCA: {
                int rot = Alu.rotate(extractRotDir(opcode), registers8.get(Reg.A));
                setRegFromAlu(Reg.A, rot);
                combineAluFlags(rot, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
            }
                break;
            case ROTA: {
                int rot = Alu.rotate(extractRotDir(opcode), registers8.get(Reg.A), registers8.testBit(Reg.F, Flag.C));
                setRegFromAlu(Reg.A, rot);
                combineAluFlags(rot, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
            }
                break;
            case ROTC_R8: {
                Reg r = extractReg(opcode, 0);
                int rot = Alu.rotate(extractRotDir(opcode), registers8.get(r));
                setRegFlags(r, rot);
            }
                break;
            case ROT_R8: {
                Reg r = extractReg(opcode, 0);
                int rot = Alu.rotate(extractRotDir(opcode), registers8.get(r), registers8.testBit(Reg.F, Flag.C));
                setRegFlags(r, rot);
            }
                break;
            case ROTC_HLR: {
                int rot = Alu.rotate(extractRotDir(opcode), read8AtHl());
                write8AtHlAndSetFlags(rot);
            }
                break;
            case ROT_HLR: {
                int rot = Alu.rotate(extractRotDir(opcode), read8AtHl(), registers8.testBit(Reg.F, Flag.C));
                write8AtHlAndSetFlags(rot);
            }
                break;
            case SWAP_R8: {
                Reg r = extractReg(opcode, 0);
                int swap = Alu.swap(registers8.get(r));
                setRegFlags(r, swap);
            }
                break;
            case SWAP_HLR: {
                int swap = Alu.swap(read8AtHl());
                write8AtHlAndSetFlags(swap);
            }
                break;
            case SLA_R8: {
                Reg r = extractReg(opcode, 0);
                int shiftL = Alu.shiftLeft(registers8.get(r));
                setRegFlags(r, shiftL);
            }
                break;
            case SRA_R8: {
                Reg r = extractReg(opcode, 0);
                int shiftR = Alu.shiftRightA(registers8.get(r));
                setRegFlags(r, shiftR);
            }
                break;
            case SRL_R8: {
                Reg r = extractReg(opcode, 0);
                int shiftR = Alu.shiftRightL(registers8.get(r));
                setRegFlags(r, shiftR);
            }
                break;
            case SLA_HLR: {
                int shiftL = Alu.shiftLeft(read8AtHl());
                write8AtHlAndSetFlags(shiftL);
            }
                break;
            case SRA_HLR: {
                int shiftR = Alu.shiftRightA(read8AtHl());
                write8AtHlAndSetFlags(shiftR);
            }
                break;
            case SRL_HLR: {
                int shiftR = Alu.shiftRightL(read8AtHl());
                write8AtHlAndSetFlags(shiftR);
            }
                break;

            // Bit test and set
            case BIT_U3_R8: {
                int test = Alu.testBit(registers8.get(extractReg(opcode, 0)), extractBitIndex(opcode));
                combineAluFlags(test, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
            }
                break;
            case BIT_U3_HLR:
                int test = Alu.testBit(read8AtHl(), extractBitIndex(opcode));
                combineAluFlags(test, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
                break;
            case CHG_U3_R8: {
                Reg r = extractReg(opcode, 0);
                if (!extractSetValue(opcode)) {
                    registers8.set(r, registers8.get(r) & Bits.complement8(Bits.mask(extractBitIndex(opcode))));
                } else {
                    registers8.set(r, registers8.get(r) | Bits.mask(extractBitIndex(opcode)));
                }
            }
                break;
            case CHG_U3_HLR: {
                if (!extractSetValue(opcode)) {
                    write8AtHl(read8AtHl() & Bits.complement8(Bits.mask(extractBitIndex(opcode))));
                } else {
                    write8AtHl(read8AtHl() | Bits.mask(extractBitIndex(opcode)));
                }
            }
                break;

            // Misc. ALU
            case DAA:
                int adjust = Alu.bcdAdjust(registers8.get(Reg.A), registers8.testBit(Reg.F, Flag.N),
                        registers8.testBit(Reg.F, Flag.H), registers8.testBit(Reg.F, Flag.C));
                setRegFromAlu(Reg.A, adjust);
                combineAluFlags(adjust, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU);
                break;
            case SCCF:
                int sccf = Alu.maskZNHC(false, false, false, !getInitialCarry(opcode));
                combineAluFlags(sccf, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
                break;

            // Jumps
            case JP_HL:
                nextPC = reg16(Reg16.HL);
                break;
            case JP_N16: {
                nextPC = read16AfterOpcode();
            }
                break;
            case JP_CC_N16: {
                if (testCondition(opcode)) {
                    nextPC = read16AfterOpcode();
                    instructionDone = true;
                }
            }
                break;
            case JR_E8: {
                nextPC += Bits.signExtend8(read8AfterOpcode());
            }
                break;
            case JR_CC_E8: {
                if (testCondition(opcode)) {
                    nextPC += Bits.signExtend8(read8AfterOpcode());
                    instructionDone = true;
                }
            }
                break;

            // Calls and returns
            case CALL_N16: {
                push16(nextPC);
                nextPC = read16AfterOpcode();
            }
                break;
            case CALL_CC_N16: {
                if (testCondition(opcode)) {
                    push16(nextPC);
                    nextPC = read16AfterOpcode();
                    instructionDone = true;
                }
            }
                break;
            case RST_U3: {
                push16(nextPC);
                nextPC = AddressMap.RESETS[Bits.extract(opcode.encoding, 3, 3)];
            }
                break;
            case RET: {
                nextPC = pop16();
            }
                break;
            case RET_CC: {
                if (testCondition(opcode)) {
                    nextPC = pop16();
                    instructionDone = true;
                }
            }
                break;

            // Interrupts
            case EDI: {
                IME = Bits.test(opcode.encoding, 3);
            }
                break;
            case RETI: {
                IME = true;
                nextPC = pop16();
            }
                break;

            // Misc control
            case HALT: {
                nextNonIdleCycle = Long.MAX_VALUE;
            }
                break;
            case STOP:
                throw new Error("STOP is not implemented");
            default:
                break;
        }

        PC = nextPC;
        nextNonIdleCycle += opcode.cycles + (instructionDone ? opcode.additionalCycles : 0);
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
