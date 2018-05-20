package ch.epfl.gameboj;

import static ch.epfl.gameboj.component.cpu.Cpu.OPCODE_PREFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import ch.epfl.gameboj.component.cpu.Opcode;
import ch.epfl.gameboj.component.memory.BootRom;

public final class CartridgeDecompiler {
    private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
    private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);
    
    private static Opcode[] buildOpcodeTable(Opcode.Kind opKind) {
        Opcode[] opcodeTable = new Opcode[256];
        for (Opcode o : Opcode.values()) {
            if (o.kind == opKind) {
                opcodeTable[o.encoding] = o;
            }
        }
        return opcodeTable;
    }
    
    public static String decompileBootRom() {
        String opcodeList = "";
        int parserCounter = 0;
        
        System.out.println("direct table " + Arrays.toString(DIRECT_OPCODE_TABLE));
        System.out.println("prefixed table " + Arrays.toString(PREFIXED_OPCODE_TABLE));
        
        while (parserCounter < BootRom.DATA.length) {
            int nextInstruction = Byte.toUnsignedInt(BootRom.DATA[parserCounter]);
            int nnextInstruction = Byte.toUnsignedInt(BootRom.DATA[parserCounter + 1]);
            Opcode nextOpcode = nextInstruction == OPCODE_PREFIX ? PREFIXED_OPCODE_TABLE[nnextInstruction]
                    : DIRECT_OPCODE_TABLE[nextInstruction];
            if(nextOpcode == null) {
                System.out.println(nextInstruction == OPCODE_PREFIX ? ("next prefixed " + Integer.toHexString((nnextInstruction))) :
                    ("next direct " + Integer.toHexString((nextInstruction))));
            }
            String opcodeName = nextOpcode.name();
            opcodeList += opcodeName + "\n";
            parserCounter += nextOpcode.totalBytes;
        }
        
        return opcodeList;
    }
    
    public static String decompileHeader(String fileName) throws IOException {
        String opcodeList = "";
        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(fileName));
            byte[] fileBytes = fis.readAllBytes();
            System.out.println("File length " + fileBytes.length);
            
            int parserCounter = AddressMap.ENTRY_POINT_START;
            
            while (parserCounter < AddressMap.ENTRY_POINT_END) {
                int nextInstruction;
                int nnextInstruction;
                nextInstruction = Byte.toUnsignedInt(fileBytes[parserCounter]);
                nnextInstruction = Byte.toUnsignedInt(fileBytes[parserCounter + 1]);
                Opcode nextOpcode = nextInstruction == OPCODE_PREFIX ? PREFIXED_OPCODE_TABLE[nnextInstruction]
                        : DIRECT_OPCODE_TABLE[nextInstruction];
                if(nextOpcode == null) {
                    System.out.println("counter " + parserCounter + " " + (nextInstruction == OPCODE_PREFIX ? ("next prefixed " + Integer.toHexString((nnextInstruction))) :
                        ("next direct " + Integer.toHexString((nextInstruction)))));
                }
                String opcodeName = nextOpcode.name();
                System.out.println(opcodeName);
                opcodeList += opcodeName + " ";
//                for (int i = 1; i < nextOpcode.totalBytes; ++i) {
//                    opcodeList += Integer.toHexString(Byte.toUnsignedInt(fileBytes[parserCounter + i])) + (i < nextOpcode.totalBytes - 1 ? ", " : " ");
//                }
                opcodeList += Integer.toHexString((Byte.toUnsignedInt(fileBytes[parserCounter + 2]) << 8) | Byte.toUnsignedInt(fileBytes[parserCounter + 1]));
                opcodeList += "\n";
                parserCounter += nextOpcode.totalBytes;
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return opcodeList;
    }
    
    public static String decompileCartridge(String fileName) throws IOException {              
        String opcodeList = "";
        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(fileName));
            byte[] fileBytes = fis.readAllBytes();
            System.out.println("File length " + fileBytes.length);
            
            int parserCounter = AddressMap.USER_AREA_START;
            
            System.out.println("direct table " + Arrays.toString(DIRECT_OPCODE_TABLE));
            System.out.println("prefixed table " + Arrays.toString(PREFIXED_OPCODE_TABLE));
            
            while (parserCounter < AddressMap.USER_AREA_END) {
                int nextInstruction;
                int nnextInstruction;
                nextInstruction = Byte.toUnsignedInt(fileBytes[parserCounter]);
                nnextInstruction = Byte.toUnsignedInt(fileBytes[parserCounter + 1]);
                Opcode nextOpcode = nextInstruction == OPCODE_PREFIX ? PREFIXED_OPCODE_TABLE[nnextInstruction]
                        : DIRECT_OPCODE_TABLE[nextInstruction];
                if(nextOpcode == null) {
                    System.out.println("counter " + Integer.toHexString(parserCounter) + " " + 
                (nextInstruction == OPCODE_PREFIX ? 
                        ("next prefixed " + Integer.toHexString((nnextInstruction))) :
                        ("next direct " + Integer.toHexString((nextInstruction)))));
                }
                String opcodeInfo = nextOpcode.name() + " ";
                for (int i = 1; i < nextOpcode.totalBytes; ++i) {
                    opcodeInfo += Integer.toHexString(Byte.toUnsignedInt(fileBytes[parserCounter + i])) + (i < nextOpcode.totalBytes - 1 ? ", " : " ");
                }
                opcodeList += opcodeInfo + "\n";
                System.out.println("PC: " + Integer.toHexString(parserCounter) + " " + Integer.toHexString(nextInstruction));
                System.out.println(opcodeInfo);
                parserCounter += nextOpcode.totalBytes;
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return opcodeList;
    }
}
