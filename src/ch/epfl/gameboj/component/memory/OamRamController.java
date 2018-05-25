package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * Cette classe rajoute des fonctionnalités d'OAM à un RamController.
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class OamRamController extends RamController {

	// Enumération des informations présentes dans l'OAM pour chaque sprite
	public enum DISPLAY_DATA {
		Y_COORD, X_COORD, TILE_INDEX, ATTRIBUTES
	}

	// Enumération des attributs présents dans le dernier octet des informations d'un sprite
	public enum ATTRIBUTES implements Bit {
		P_NUM0, P_NUM1, P_NUM2, VRAM_BANK, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
	}

	// Maximum 40 sprites peuvent être sur l'écran en même temps
	private static final int MAX_SPRITES = 40;
	// Maximum 10 sprites peuvent être sur une ligne en même temps
	public static final int SPRITES_PER_LINE = 10;
	private static final int SPRITE_ATTR_BYTES = DISPLAY_DATA.values().length;
	// Le décalage de l'abscisse et de l'ordonnée des sprites, imposé pour garder
	// leurs coordonnées positives
	public static final int SPRITE_XOFFSET = 8;
	public static final int SPRITE_YOFFSET = 16;

	public OamRamController(Ram ram, int startAddress) {
		super(ram, startAddress);
	}

	/**
	 * Cette méthode calcule les sprites qui intersectent la ligne d'index donné.
	 * 
	 * @param lineIndex
	 *            l'index de la ligne
	 * @param height
	 *            la hauteur des sprites
	 * @return un tableau contenant les abscisses et les index des tuiles
	 */
	public Integer[] spritesIntersectingLine(int lineIndex, int height) {
		int scanIndex = 0, foundSprites = 0;

		Integer[] intersect = new Integer[SPRITES_PER_LINE];

		// Tant que aucune limite n'est atteinte (10 par ligne ou 40 en total),
		// continuer à chercher les intersections
		while (foundSprites < SPRITES_PER_LINE && scanIndex < MAX_SPRITES) {
			int spriteY = readAttr(scanIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;

			if (lineIndex >= spriteY && lineIndex < spriteY + height) {
				intersect[foundSprites] = packSpriteInfo(scanIndex);
				foundSprites++;
			}

			scanIndex++;
		}

		Integer[] intersectIndex = trimIntArray(intersect, foundSprites);

		Arrays.sort(intersectIndex);

		return intersectIndex;
	}

	/**
	 * Cette méthode permet de rétrécir un tableau à sa taille "occupée".
	 * 
	 * @param array
	 *            l'array à "couper"
	 * @param trimIndex
	 *            l'index auquel il faut "couper"
	 * @return le tableau rétréci à la capacité voulue
	 */
	private Integer[] trimIntArray(Integer[] array, int trimIndex) {
		Integer[] intersectIndex = new Integer[trimIndex];

		for (int i = 0; i < trimIndex; ++i) {
			intersectIndex[i] = array[i];
		}

		return intersectIndex;
	}

	private int packSpriteInfo(int spriteIndex) {
		return Bits.make16(readAttr(spriteIndex, DISPLAY_DATA.X_COORD), spriteIndex);
	}

	/**
	 * Cette méthode permet de lire une donnée d'un sprite.
	 * 
	 * @param spriteIndex
	 *            l'index du sprite dans l'OAM
	 * @param attr
	 *            l'attribut à lire
	 * @return la valeur de l'attribut
	 */
	public int readAttr(int spriteIndex, DISPLAY_DATA attr) {
		return read(AddressMap.OAM_START + Objects.checkIndex(spriteIndex, MAX_SPRITES) * SPRITE_ATTR_BYTES
				+ attr.ordinal());
	}

	/**
	 * Cette méthode permet de lire un attribut d'un sprite, au sein du registre
	 * d'attributs.
	 * 
	 * @param spriteIndex
	 *            l'index du sprite dans l'OAM
	 * @param attribute
	 *            l'attribut à lire (un bit)
	 * @return la valeur de l'attribut (0 ou 1)
	 */
	public boolean readAttr(int spriteIndex, ATTRIBUTES attribute) {
		return Bits.test(readAttr(spriteIndex, DISPLAY_DATA.ATTRIBUTES), attribute);
	}
}
