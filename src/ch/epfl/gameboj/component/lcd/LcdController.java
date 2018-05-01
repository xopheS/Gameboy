package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

public final class LcdController implements Component, Clocked {

    private enum LCDReg implements Register {
        LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
    }

    private enum LCDC implements Bit {
        BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }

    private enum STAT implements Bit {
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED
    }

    private static final int TILE_SIZE = 8;
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_SIZE;
    private static final int WIN_SIZE = BG_SIZE, WIN_TILE_SIZE = BG_TILE_SIZE;
    public static final int LCD_WIDTH = 160, LCD_HEIGHT = 144;
    private static final int LCD_TILE_WIDTH = LCD_WIDTH / TILE_SIZE;
    private static final int LCD_TILE_HEIGHT = LCD_HEIGHT / TILE_SIZE;
    private static final int MODE2_DURATION = 20, MODE3_DURATION = 43, MODE0_DURATION = 51;
    private static final int LINE_CYCLE_DURATION = MODE2_DURATION + MODE3_DURATION + MODE0_DURATION;
    private LcdImage displayedImage;
    private final Ram videoRam, oamRam;
    private final Cpu cpu;
    private Bus bus;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private long lineStartT = 0;
    private int winY = 0;
    private LcdImage.Builder nextImageBuilder;
    private final RegisterFile<Register> lcdRegs = new RegisterFile<>(LCDReg.values());

    /**
     * Construit un contrôleur LCD.
     * 
     * @param cpu
     * le CPU avec lequel le contrôleur interagit
     */
    public LcdController(Cpu cpu) {

        this.cpu = Objects.requireNonNull(cpu);
        videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        oamRam = new Ram(AddressMap.OAM_RAM_SIZE);

        ArrayList<LcdImageLine> imgLines = new ArrayList<>(LCD_HEIGHT);

        for (int i = 0; i < LCD_HEIGHT; ++i) {
            imgLines.add(
                    new LcdImageLine(new BitVector(LCD_WIDTH), new BitVector(LCD_WIDTH), new BitVector(LCD_WIDTH)));
        }

        displayedImage = new LcdImage(LCD_WIDTH, LCD_HEIGHT, imgLines);

        nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    }

    public LcdImage currentImage() {
        return Objects.requireNonNull(displayedImage, "fatal: attempt to display a null image");
    }

    @Override
    public void cycle(long cycle) {
        if (nextNonIdleCycle == Long.MAX_VALUE && lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
            System.out.println("Power on, cycle " + cycle);
            lineStartT = cycle;
            nextNonIdleCycle = cycle + 20;
            setMode(2);
            reallyCycle(cycle);
        } else if (cycle != nextNonIdleCycle || !lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
            return;
        } else {
            reallyCycle(cycle);
        }
        // DMA process
    }

    private void reallyCycle(long cycle) {
        int mode0 = lcdRegs.testBit(LCDReg.STAT, STAT.MODE0) ? 1 : 0;
        int mode1 = lcdRegs.testBit(LCDReg.STAT, STAT.MODE1) ? 1 : 0;
        int mode = mode0 | (mode1 << 1);

        if (mode == 0 && lcdRegs.get(LCDReg.LY) == LCD_HEIGHT - 1) {
            displayedImage = nextImageBuilder.build();
            modifyLYorLYC(LCDReg.LY, lcdRegs.get(LCDReg.LY) + 1);
            setMode(1);
            lineStartT = cycle;
            nextNonIdleCycle += 114;
        } else if (mode == 2 && cycle - lineStartT == MODE2_DURATION) {
            nextImageBuilder.setLine(lcdRegs.get(LCDReg.LY), computeLine(lcdRegs.get(LCDReg.LY)));
            winY++;
            setMode(3);
            nextNonIdleCycle += 43;
        } else if (mode == 3 && cycle - lineStartT == MODE2_DURATION + MODE3_DURATION) {
            setMode(0);
            nextNonIdleCycle += 51;
        } else if (mode == 0 && cycle - lineStartT == MODE2_DURATION + MODE3_DURATION + MODE0_DURATION) {
            modifyLYorLYC(LCDReg.LY, lcdRegs.get(LCDReg.LY) + 1);
            lineStartT = cycle;
            setMode(2);
            nextNonIdleCycle += 20;
        } else if (mode == 1 && cycle - lineStartT == LINE_CYCLE_DURATION) {
            lineStartT = cycle;
            if (lcdRegs.get(LCDReg.LY) == 153) {
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_WIDTH);
                modifyLYorLYC(LCDReg.LY, 0);
                setMode(2);
                winY = 0;
                nextNonIdleCycle += 20;
            } else {
                modifyLYorLYC(LCDReg.LY, lcdRegs.get(LCDReg.LY) + 1);
                nextNonIdleCycle += 114;
            }
        }
    }

    private void setMode(int mode) {
        Preconditions.checkArgument(mode >= 0 && mode < 4, "The mode must be between 0 and 3");
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE0, Bits.test(mode, 0));
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE1, Bits.test(mode, 1));

        switch (mode) {
            case 0:
                if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE0)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }
                break;
            case 1:
                if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE1)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }
                cpu.requestInterrupt(Interrupt.VBLANK);
                break;
            case 2:
                if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }
                break;
            default:
                break;
        }
    }

    private LcdImageLine computeLine(int index) {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);
        LcdImageLine.Builder nextWinLineBuilder = new LcdImageLine.Builder(WIN_SIZE);
        LcdImageLine nextBGLine, nextWinLine;

        int adjustedWX = lcdRegs.get(LCDReg.WX) - 7;

        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG)) {
            for (int i = 0; i < BG_TILE_SIZE; ++i) {
                int bgAddress;

                int bgI = Math.floorDiv(Math.floorMod(lcdRegs.get(LCDReg.SCY) + lcdRegs.get(LCDReg.LY), BG_SIZE),
                        TILE_SIZE) * BG_TILE_SIZE + i;

                if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG_AREA)) {
                    bgAddress = AddressMap.BG_DISPLAY_DATA[1] + bgI;
                } else {
                    bgAddress = AddressMap.BG_DISPLAY_DATA[0] + bgI;
                }

                int bgTypeIndex = read(bgAddress);

                int bgTypeAddress;

                if (lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE)) {
                    bgTypeAddress = AddressMap.TILE_SOURCE[1] + bgTypeIndex * 16
                            + Math.floorMod(lcdRegs.get(LCDReg.LY), TILE_SIZE) * 2;
                } else {
                    if (bgTypeIndex >= 0 && bgTypeIndex < 128) {
                        bgTypeAddress = AddressMap.TILE_SOURCE[0] + (bgTypeIndex + 128) * 16
                                + Math.floorMod(lcdRegs.get(LCDReg.LY), TILE_SIZE) * 2;
                    } else if (bgTypeIndex >= 128 && bgTypeIndex < 256) {
                        bgTypeAddress = AddressMap.TILE_SOURCE[0] + (bgTypeIndex - 128) * 16
                                + Math.floorMod(lcdRegs.get(LCDReg.LY), TILE_SIZE) * 2;
                    } else {
                        throw new IllegalArgumentException("bg_type_index wrong!");
                    }
                }

                nextBGLineBuilder.setBytes(i, Bits.reverse8(read(bgTypeAddress + 1)),
                        Bits.reverse8(read(bgTypeAddress)));
            }

            nextBGLine = nextBGLineBuilder.build().extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH)
                    .mapColors(lcdRegs.get(LCDReg.BGP));
        } else {
            for (int i = 0; i < BG_TILE_SIZE; ++i) {
                nextBGLineBuilder.setBytes(i, 0, 0);
            }

            nextBGLine = nextBGLineBuilder.build().extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH);
        }

        if (lcdRegs.get(LCDReg.LY) >= lcdRegs.get(LCDReg.WY) && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN)
                && adjustedWX >= 0 && adjustedWX < 167) {
            int winLineIndex = lcdRegs.get(LCDReg.LY) - lcdRegs.get(LCDReg.WY);

            for (int i = 0; i < WIN_TILE_SIZE; ++i) {
                int winAddress;

                int winI = Math.floorDiv(winLineIndex, TILE_SIZE) * WIN_TILE_SIZE + i;

                if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN_AREA)) {
                    winAddress = AddressMap.BG_DISPLAY_DATA[1] + winI;
                } else {
                    winAddress = AddressMap.BG_DISPLAY_DATA[0] + winI;
                }

                int winTypeIndex = read(winAddress);

                int winTypeAddress;

                if (lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE)) {
                    winTypeAddress = AddressMap.TILE_SOURCE[1] + winTypeIndex * 16
                            + Math.floorMod(winLineIndex, TILE_SIZE) * 2;
                } else {
                    if (winTypeIndex >= 0 && winTypeIndex < 128) {
                        winTypeAddress = AddressMap.TILE_SOURCE[0] + (winTypeIndex + 128) * 16
                                + Math.floorMod(winLineIndex, TILE_SIZE) * 2;
                    } else if (winTypeIndex >= 128 && winTypeIndex < 256) {
                        winTypeAddress = AddressMap.TILE_SOURCE[0] + (winTypeIndex - 128) * 16
                                + Math.floorMod(winLineIndex, TILE_SIZE) * 2;
                    } else {
                        throw new IllegalArgumentException("bg_type_index wrong!");
                    }
                }

                nextWinLineBuilder.setBytes(i, Bits.reverse8(read(winTypeAddress + 1)),
                        Bits.reverse8(read(winTypeAddress)));
            }

            nextWinLine = nextWinLineBuilder.build().mapColors(lcdRegs.get(LCDReg.BGP));

            return nextBGLine.join(nextWinLine, adjustedWX);
        } else {
            return nextBGLine;
        }
    }

    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        bus.attach(this);
    }

    @Override
    public int read(int address) throws IllegalArgumentException {
        if (Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            return oamRam.read(address - AddressMap.OAM_START);
        } else if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            return lcdRegs.get(address - AddressMap.REGS_LCDC_START);
        } else {
            return NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {

        Preconditions.checkBits8(data);

        if (Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            videoRam.write(address - AddressMap.VIDEO_RAM_START, data);
        }

        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            if (address == AddressMap.REGS_LCDC_START + LCDReg.LCDC.index()) {
                lcdRegs.set(LCDReg.LCDC, data);
                if (!lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
                    setMode(0);
                    modifyLYorLYC(LCDReg.LY, 0);
                    nextNonIdleCycle = Long.MAX_VALUE;
                    System.out.println("Power off");
                }
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.STAT.index()) {
                lcdRegs.set(LCDReg.STAT, data & 0b1111_1000 | lcdRegs.get(LCDReg.STAT) & 0b0000_0111);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.LY.index()) {
                modifyLYorLYC(LCDReg.LY, data);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.LYC.index()) {
                modifyLYorLYC(LCDReg.LYC, data);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.DMA.index()) {
                // DMA write process
            } else {
                lcdRegs.set(address - AddressMap.REGS_LCDC_START, data);
            }
        }
    }

    private void modifyLYorLYC(LCDReg reg, int data) {
        Preconditions.checkArgument(reg == LCDReg.LY || reg == LCDReg.LYC);

        lcdRegs.set(reg, data);

        if (lcdRegs.get(LCDReg.LY) == lcdRegs.get(LCDReg.LYC)) {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, true);
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_LYC)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
        } else {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, false);
        }
    }
}
