package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

public class LcdImageLine {
    private final BitVector LSB;
    private final BitVector MSB;
    private final BitVector opacity;

    /**
     * Construit une ligne d'image.
     * 
     * @param msb
     * les most significant bits
     * @param lsb
     * les least significant bits
     * @param opacity
     * l'opacité (alpha)
     */
    public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {

        Preconditions.checkArgument(lsb.size() == msb.size() && msb.size() == opacity.size(),
                "The three BitVectors must have the same length");
        this.LSB = lsb;
        this.MSB = msb;
        this.opacity = opacity;
    }

    public int size() {
        return LSB.size();
    }

    public BitVector getLsb() {
        return this.LSB;
    }

    public BitVector getMsb() {
        return this.MSB;
    }

    public BitVector getOpacity() {
        return this.opacity;
    }

    public LcdImageLine shift(int distance) {
        return new LcdImageLine(MSB.shift(distance), LSB.shift(distance), opacity.shift(distance));
    }

    public LcdImageLine extractWrapped(int pixel, int size) {
        return new LcdImageLine(MSB.extractWrapped(pixel, size), LSB.extractWrapped(pixel, size),
                opacity.extractWrapped(pixel, size));
    }

    /**
     * Change les couleurs d'une ligne en lui appliquant une palette.
     * 
     * @param palette
     * la palette à appliquer
     * @return la ligne coloriée
     */
    public LcdImageLine mapColors(int palette) {
        Preconditions.checkBits8(palette);

        if (palette == 0b11100100) {
            return this;
        }

        BitVector msbCopy = MSB.extractZeroExtended(0, size()), lsbCopy = LSB.extractZeroExtended(0, size());
        BitVector mask = null;

        for (int i = 0; i < 4; i++) {

            int color = Bits.extract(palette, 2 * i, 2);

            switch (i) {
                case 0:
                    mask = MSB.or(LSB).not();
                    break;
                case 1:
                    mask = MSB.not().and(LSB);
                    break;
                case 2:
                    mask = LSB.not().and(MSB);
                    break;
                case 3:
                    mask = LSB.and(MSB);
                    break;
                default:
                    break;
            }

            switch (color) {
                case 0: {
                    msbCopy = msbCopy.and(mask.not());
                    lsbCopy = lsbCopy.and(mask.not());
                }
                    break;
                case 1: {
                    msbCopy = msbCopy.and(mask.not());
                    lsbCopy = lsbCopy.or(mask);
                }
                    break;

                case 2: {
                    msbCopy = msbCopy.or(mask);
                    lsbCopy = lsbCopy.and(mask.not());
                }
                    break;

                case 3: {
                    msbCopy = msbCopy.or(mask);
                    lsbCopy = lsbCopy.or(mask);
                }
                    break;
                default:
                    break;

            }
        }

        return new LcdImageLine(msbCopy, lsbCopy, opacity);
    }

    // other & opacity | this & notOpacity
    /**
     * Superpose une ligne avec une autre, en la mettant en dessous.
     * 
     * @param other
     * l'autre ligne qui est au-dessus
     * @return la nouvelle ligne
     */
    public LcdImageLine below(LcdImageLine other) {
        Preconditions.checkArgument(other.size() == size(), "The two lines must have the same length");

        BitVector newOpacity = opacity.or(other.opacity);
        BitVector newLSB = (LSB.and(other.opacity.not())).or(other.LSB.and(other.opacity));
        BitVector newMSB = (MSB.and(other.opacity.not())).or(other.MSB.and(other.opacity));
        return new LcdImageLine(newMSB, newLSB, newOpacity);
    }

    /**
     * Superpose une ligne avec une autre, en la mettant en dessous.
     * 
     * @param other
     * l'autre ligne
     * @param opacity
     * l'opacité à utiliser
     * @return la nouvelle ligne
     */
    public LcdImageLine below(LcdImageLine other, BitVector opacity) {
        Preconditions.checkArgument(other.size() == size() && opacity.size() == size(), "The line and the opacity vector must have the same length");

        return new LcdImageLine(MSB.and(opacity.not()).or(other.MSB.and(opacity)), LSB.and(opacity.not()).or(other.LSB.and(opacity)), this.opacity);
    }

    /**
     * Joint deux lignes, à partir d'un index.
     * 
     * @param other
     * l'autre ligne
     * @param n
     * l'index de démarcation
     * @return la nouvelle ligne
     */
    public LcdImageLine join(LcdImageLine other, int n) {
        int size = size();
        Preconditions.checkArgument(other.size() == size, "The two image lines must have the same length");
        Objects.checkIndex(n, size);
        
        BitVector lsbModified = LSB.shift(size - n).or(other.LSB.shift(-n));
        BitVector msbModified = MSB.shift(size - n).or(other.MSB.shift(-n));
        BitVector opacityModified = opacity.shift(size - n).or(other.opacity.shift(-n));

        return new LcdImageLine(msbModified, lsbModified, opacityModified);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof LcdImageLine) && LSB.equals(((LcdImageLine) o).getLsb())
                && MSB.equals(((LcdImageLine) o).getMsb()) && opacity.equals(((LcdImageLine) o).getOpacity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(MSB, LSB, opacity);
    }

    public static final class Builder {

        BitVector.Builder msbBuilder;
        BitVector.Builder lsbBuilder;

        public Builder(int size) {
            msbBuilder = new BitVector.Builder(size);
            lsbBuilder = new BitVector.Builder(size);
        }

        /**
         * Modifie les octets d'une ligne.
         * 
         * @param index
         * l'index des octets à modifier (en bits)
         * @param byteMSB
         * l'octet des MSB
         * @param byteLSB
         * l'octet des LSB
         * @return le constructeur
         */
        public Builder setBytes(int index, int byteMSB, int byteLSB) {
            msbBuilder.setByte(index, byteMSB);
            lsbBuilder.setByte(index, byteLSB);
            return this;
        }

        /**
         * Construit la ligne.
         * 
         * @return la ligne
         */
        public LcdImageLine build() {
            BitVector lsb = lsbBuilder.build();
            BitVector msb = msbBuilder.build();
            return new LcdImageLine(msb, lsb, msb.and(lsb));
        }
    }
}
