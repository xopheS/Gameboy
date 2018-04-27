package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
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
	
	private enum LCDReg implements Register { LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX }
    private enum LCDC implements Bit { BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS }
    private enum STAT implements Bit { MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED }

    public final static int LCD_WIDTH = 160;
    public final static int LCD_HEIGHT = 144;
    private LcdImage displayedImage;
    private final Ram videoRam;
    private final Ram oamRam;
    private final Cpu cpu;
    private long nextNonIdleCycle = 0, lcdOnCycle = 0;
    private LcdImage.Builder nextImageBuilder;
    private final RegisterFile<Register> lcdRegs = new RegisterFile<>(LCDReg.values());
    
    //La fonction changeMode elle sert à faire les changement lors du changement de mode
    //La fonction modifyLYorLYC modifie LY ou LYC et fait les checks et les interruptions necessaires    
    
    public LcdController(Cpu cpu) {
        
        this.cpu = Objects.requireNonNull(cpu);
        videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        oamRam = new Ram(AddressMap.OAM_RAM_SIZE);
        
        ArrayList<LcdImageLine> imgLines = new ArrayList<>(LCD_HEIGHT);
        
        for(int i = 0; i < LCD_HEIGHT; ++i) {
        	imgLines.add(new LcdImageLine(new BitVector(LCD_WIDTH), new BitVector(LCD_WIDTH), new BitVector(LCD_WIDTH)));
        }
        
        displayedImage = new LcdImage(LCD_WIDTH, LCD_HEIGHT, new ArrayList<LcdImageLine>(0));
        
        nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    }
    
    public LcdImage currentImage() {  
        return Objects.requireNonNull(displayedImage, "fatal: attempt to display a null image");
    }
    
    @Override
    public void cycle(long cycle) {
    	if(nextNonIdleCycle == Long.MAX_VALUE && lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
    		lcdRegs.setBit(LCDReg.LCDC, LCDC.LCD_STATUS, true);
    		lcdOnCycle = cycle;
    		reallyCycle(cycle);
    	}
        if(cycle == nextNonIdleCycle) {
        	reallyCycle(cycle);
        } else {
        	return;
        }
    }

    private void reallyCycle(long cycle) {
                
    }  

    @Override
    public int read(int address) throws IllegalArgumentException {
        if (Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.HIGH_RAM_END) return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) { 
        	return lcdRegs.get(address - AddressMap.REGS_LCDC_START);
        }
        else return NO_DATA;
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {   
        
    	Preconditions.checkBits8(data);
    	
        if (Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) 
            videoRam.write(address - AddressMap.VIDEO_RAM_START, Preconditions.checkBits8(data));
        
        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            
            if (address == AddressMap.REGS_LCDC_START) {
            	lcdRegs.set(LCDReg.LCDC, data);
            	if (!Bits.test(lcdRegs.get(LCDReg.LCDC), 7)) {
            		lcdRegs.setBit(LCDReg.STAT, STAT.MODE0, false);
            		lcdRegs.setBit(LCDReg.STAT, STAT.MODE1, false);
            		lcdRegs.set(LCDReg.LY, 0);
            		nextNonIdleCycle = Long.MAX_VALUE;
            	}
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.STAT.index()) {
            	lcdRegs.set(LCDReg.STAT, data | ((lcdRegs.get(LCDReg.STAT) & 0b0000_0111)));
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.LY.index()) {
            	modifyLYorLYC(LCDReg.LY, Preconditions.checkBits8(data));
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.LYC.index()) {
            	modifyLYorLYC(LCDReg.LYC, Preconditions.checkBits8(data));
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
        } else {
        	lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, false);
        }
        
        if (!lcdRegs.testBit(LCDReg.STAT, STAT.INT_LYC)) {
        	cpu.requestInterrupt(Interrupt.LCD_STAT);
        }             
    }
    
    
    /*private void changeMode(int nextMode) {
        
        switch(nextMode) {
        case 0 : {
            if(Bits.test(registers.get(RegAddress.STAT), 3)) cpu.requestInterrupt(Interrupt.LCD_STAT);
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 0, false));
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 1, false));
          } break;
        
        case 1 : {
            if(Bits.test(registers.get(RegAddress.STAT), 4)) cpu.requestInterrupt(Interrupt.LCD_STAT);
            cpu.requestInterrupt(Interrupt.VBLANK);
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 0, true));
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 1, false));
          } break;
        
        case 2 : {
            if(Bits.test(registers.get(RegAddress.STAT), 3)) cpu.requestInterrupt(Interrupt.LCD_STAT);
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 0, false));
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 1, true));
          } break;
        
        case 3 : {
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 0, true));
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 1, true));
          } break;
        
        }
    }*/

}

