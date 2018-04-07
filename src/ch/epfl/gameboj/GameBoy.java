package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

/**Cette classe modélise la Gameboy en entier, avec tous ses composants fonctionnant ensemble
 * 
 * @author Cristophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public class GameBoy {

    private final Bus bus = new Bus();
    private final Ram workRam;
    private final RamController workRamController, workRamEchoController;
    private final BootRomController bootRomController;
    private final Timer timer;
    private final Cpu cpu;
    
    private long cycles;
    
    /**Constructeur qui initialise une Gameboy avec une cartouche donnée
     * 
     * @param cartridge
     * La cartouche que la Gameboy va exécuter
     * 
     * @throws NullPointerException 
     * si la cartouche est null
     */
    public GameBoy(Cartridge cartridge) {        
        workRam = new Ram(AddressMap.WORK_RAM_SIZE);
        
        bootRomController = new BootRomController(Objects.requireNonNull(cartridge));
        bootRomController.attachTo(bus);
        
        workRamController = new RamController(workRam, AddressMap.WORK_RAM_START, AddressMap.WORK_RAM_END);
        workRamEchoController = new RamController(workRam, AddressMap.ECHO_RAM_START, AddressMap.ECHO_RAM_END);
        
        workRamController.attachTo(bus);
        workRamEchoController.attachTo(bus);
        
        cpu = new Cpu();
        cpu.attachTo(bus);
        
        timer = new Timer(cpu);
        timer.attachTo(bus);
    }
    
    /**Getter du bus
     * 
     * @return le bus de la Gameboy
     */
    public Bus bus() {
        return bus;
    }
    
    /**Getter du cpu
     * 
     * @return le processeur de la Gameboy
     */
    public Cpu cpu() {
        return cpu;
    }
    
    /**Cette méthode dit à la Gameboy de continuer à fonctionner jusqu'à atteindre un certain cycle
     * 
     * @param cycle
     * Le cycle maximal à atteindre
     * 
     * @throws IllegalArgumentException
     * si le cycle à atteindre est strictement inférieur au cycle actuel
     */
    public void runUntil(long cycle) {
    	Preconditions.checkArgument(cycles <= cycle);
        while(cycles <= cycle) {         
            timer.cycle(cycles);
            cpu.cycle(cycles);
            cycles++;
        } 
    }
    
    /**Getter du cycle actuel
     * 
     * @return le cycle actuel
     */
    public long cycles() {
        return cycles;
    }
    
    /**Getter du timer
     * 
     * @return le timer de la Gameboy
     */
    public Timer timer() {
        return timer;
    }
}
