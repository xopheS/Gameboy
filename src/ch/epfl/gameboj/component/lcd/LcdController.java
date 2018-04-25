package ch.epfl.gameboj.component.lcd;

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
import ch.epfl.gameboj.bits.Bits;


public final class LcdController implements Component, Clocked {

    public final static int LCD_WIDTH = 160;
    public final static int LCD_HEIGHT = 144;
    private Ram videoRam;
    private Ram oamRam;
    private Cpu cpu;
    private long nextNonIdleCycle = 0;
    private Map<Integer, Integer> registers; // <Addresse, Valeur>
    
    //J'ai stocker les registres dans une map et j'ai créer une interface avec leurs addresses pour les magic values
    //La map c'est ultra pratique pour acceder aux elements selon les addresses
    //La fonction changeMode elle sert à faire les changement lors du changement de mode
    //La fonction modifyLYorLYC modifie LY ou LYC et fait les checks et les interruptions necessaires
    
    
    public LcdController(Cpu cpu) {
        
        Objects.requireNonNull(cpu);
        this.cpu = cpu;
        videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        oamRam = new Ram(AddressMap.OAM_RAM_SIZE);
        
        registers = new HashMap<>();
        registers.put(RegAddress.LCDC, 0);
        registers.put(RegAddress.STAT, 0);
        registers.put(RegAddress.SCY, 0);
        registers.put(RegAddress.SCX, 0);
        registers.put(RegAddress.LY, 0);
        registers.put(RegAddress.LYC, 0);
        registers.put(RegAddress.DMA, 0);
        registers.put(RegAddress.BGP, 0);
        registers.put(RegAddress.OBP0, 0);
        registers.put(RegAddress.OBP1, 0);
        registers.put(RegAddress.WY, 0);
        registers.put(RegAddress.WX, 0);
        
    }
    
    public LcdImage currentImage() {
        
        return null;
    }

    @Override
    public void cycle(long cycle) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int read(int address) throws IllegalArgumentException {
        Preconditions.checkBits16(address);
        if(address >= AddressMap.VIDEO_RAM_START && address < AddressMap.HIGH_RAM_END) return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        if(address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) return registers.get(address);
        else return NO_DATA;
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {
   
        
        if(Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.HIGH_RAM_END) 
            videoRam.write(address - AddressMap.VIDEO_RAM_START, Preconditions.checkBits8(data));
        
        if(address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            
            if(address == RegAddress.LCDC && !Bits.test(registers.get(RegAddress.LCDC), 7)) {
                registers.put(address, Preconditions.checkBits8(data));
                changeMode(0);
                modifyLYorLYC(RegAddress.LY, 0);
                nextNonIdleCycle = Long.MAX_VALUE;
            }
            
            else if(address == RegAddress.STAT) 
                registers.replace(RegAddress.STAT, (registers.get(RegAddress.STAT) & 0b0000_0111) | Preconditions.checkBits8(data) & 0b1111_1000);
            
            else if(address == RegAddress.LY || address == RegAddress.LYC) modifyLYorLYC(address, Preconditions.checkBits8(data));
            
            else registers.put(address, Preconditions.checkBits8(data));
        }

        
    }
    
    private void modifyLYorLYC(int address, int data) {
        registers.replace(address, data);
        if(registers.get(RegAddress.LY) == registers.get(RegAddress.LYC)) {
            registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 2, true));
            if(Bits.test(registers.get(RegAddress.STAT), 6) && address == RegAddress.LY) cpu.requestInterrupt(Interrupt.LCD_STAT);
        }
        else  registers.replace(RegAddress.STAT, Bits.set(registers.get(RegAddress.STAT), 2, false));               
    }
    
    
    private void changeMode(int nextMode) {
        
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
    }

}

