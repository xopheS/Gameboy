package ch.epfl.gameboj;

public interface AddressMap {
    //TODO CHECK FOR REDUNDANCY AND IMPRECISIONS
    
    //From cartridge, fixed bank (EXTERNAL)
    int ROM_BANK_00_START = 0x0000, ROM_BANK_00_END = 0x4000, ROM_BANK_00_SIZE = ROM_BANK_00_END - ROM_BANK_00_START;
    //From cartridge, switchable bank via MBC (if any) (EXTERNAL)
    int ROM_BANK_01_START = 0x4000, ROM_BANK_01_END = 0x8000, ROM_BANK_01_SIZE = ROM_BANK_01_END - ROM_BANK_01_START;
    //Only bank 0 in non-CGB mode, switchable bank 0/1 in CGB mode
    int VRAM_START = 0x8000, VRAM_END = 0xA000, VRAM_SIZE = VRAM_END - VRAM_START;
    //In cartridge, switchable bank if any (EXTERNAL)
    int EXTERNAL_RAM_START = 0xA000, EXTERNAL_RAM_END = 0xC000, EXTERNAL_RAM_SIZE = EXTERNAL_RAM_END - EXTERNAL_RAM_START;
    //WRAM bank 0
    int WRAM_BANK_0_START = 0xC000, WRAM_BANK_0_END = 0xD000, WRAM_BANK_0_SIZE = WRAM_BANK_0_END - WRAM_BANK_0_START;
    //Only bank 1 in non-CGB mode, switchable bank 1-7 in CGB mode
    int WRAM_BANK_1_START = 0xD000, WRAM_BANK_1_END = 0xE000, WRAM_BANK_1_SIZE = WRAM_BANK_1_END - WRAM_BANK_1_START;
    //Mirror of 0xC000-0xDDFF
    int ECHO_RAM_START = 0xE000, ECHO_RAM_END = 0xFD00, ECHO_RAM_SIZE = ECHO_RAM_END - ECHO_RAM_START;
    //Sprite attribute table
    int OAM_START = 0xFE00, OAM_END = 0xFEA0, OAM_RAM_SIZE = OAM_END - OAM_START;
    //-------------------------------- 0xFEA0 - 0xFF00: not usable ----------------------------------
    //I/O registers
    int REGS_START = 0xFF00;
    int REG_P1 = 0xFF00;
    int REG_DIV = 0xFF04;
    int REG_TIMA = 0xFF05;
    int REG_TMA = 0xFF06;
    int REG_TAC = 0xFF07;
    int REG_IF = 0xFF0F;
    
    int REGS_LCDC_START = 0xFF40;
    int REG_LCDC = 0xFF40;
    
    int REGS_LCDC_END = 0xFF4C;
    
    int REG_BOOT_ROM_DISABLE = 0xFF50;
    
    int REGS_END = 0xFF80;
    //High ram
    int HRAM_START = 0xFF80, HRAM_END = 0xFFFF, HIGH_RAM_SIZE = HRAM_END - HRAM_START;
    //Interrupts enable register
    int REG_IE = 0xFFFF;
    
    //Jump vectors
    int[] RESETS = new int[] { 0x00, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38 };
    int[] INTERRUPTS = new int[] { 0x40, 0x48, 0x50, 0x58, 0x60 };
    
    //Cartridge header
    int CARTRIDGE_HEADER_START = 0x0100, CARTRIDGE_HEADER_END = 0x0150, CARTRIDGE_HEADER_SIZE = CARTRIDGE_HEADER_END - CARTRIDGE_HEADER_START;

    
    
    
    int BOOT_ROM_START = 0x0000, BOOT_ROM_END = 0x0100, BOOT_ROM_SIZE = BOOT_ROM_END - BOOT_ROM_START;

    // Video RAM
    int[] TILE_SOURCE = new int[] {0x8800, 0x8000};
    int[] BG_DISPLAY_DATA = new int[] {0x9800, 0x9C00};    
}
