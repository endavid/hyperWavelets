// Copyright 2001, FreeHEP.
package org.freehep.util.io;

import java.io.EOFException;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Class to write bits to a Stream, allowing for byte synchronization.
 * Signed, Unsigned, Booleans and Floats can be written.
 *
 * <p>Corrections: David Gavilan 
 * @author Mark Donszelmann
 * @author Charles Loomis
 * @version $Id: BitOutputStream.java,v 1.4.1 duns Exp $
 */
public class BitOutputStream
    extends FilterOutputStream
    implements FinishableOutputStream {

    protected int bits;
    protected int bitPos;

    public BitOutputStream(OutputStream out) {
        super(out);

        bits = 0;
        bitPos = 0;
    }

    public void finish()
        throws IOException {

        flushByte();
    }

    public void close()
        throws IOException {

        finish();
        super.close();
    }

    /**
     * A utility method to flush the next byte */
    protected void flushByte()
        throws IOException {

        if (bitPos == 0) return;

        out.write(bits);
        bits = 0;
        bitPos = 0;
    }

    /**
     * A utility to force the next write to be byte-aligned. */
    public void byteAlign() throws IOException {
        flushByte();
    }

    /**
     * Write a bit to the output stream.
     * A 1-bit is true; a 0-bit is false. */
    public void writeBitFlag(boolean bit)
        throws IOException {

        writeUBits((bit) ? 1 : 0, 1);
    }

    /**
     * Write a signed value of n-bits to the output stream. */
    public void writeSBits(long value, int n)
        throws IOException {

        long tmp = value & 0x7FFFFFFF;

        if (value < 0) {
            tmp |= (1L << (n-1));
        }

        writeUBits(tmp, n);
    }

    /**
     * Write a float value of n-bits to the stream. */
    public void writeFBits(float value, int n)
        throws IOException {

        if (n == 0) return;
        long tmp = (long)(value * (float)0x10000);
        writeUBits(tmp, n);
    }

    /**
     * Write an unsigned value of n-bits to the output stream. */
    public void writeUBits(long value, int n)
        throws IOException {

        if (n == 0) return;
        if (bitPos == 0) bitPos = 8;

        int bitNum = n;

        while (bitNum > 0) {
            while ((bitPos > 0) && (bitNum > 0)) {
                long or = (value & (1L << (bitNum - 1)));
                int shift = bitPos-bitNum;
                if (shift < 0) {
                    or >>= -shift;
                } else {
                    or <<= shift;
                }
                bits |= or;

                bitNum--;
                bitPos--;
            }

            if( bitPos == 0 ) {
                write(bits);
                bits = 0;
                if (bitNum > 0) bitPos = 8;
            }
        }
    }

    /**
     * calculates the minumum number of bits necessary to
     * write number.
     *
     * @param number number
     * @return minimum number of bits to store number
     */
    public static int minBits(int number) {
        return minBits((long)number);
    }

    public static int minBits(float number) {
        return minBits((int)(number / 0x10000)) + 16;
    }

    public static int minBits(long number) {
        boolean signed = number < 0;
        number = Math.abs(number);

        long x = 1;
        int i;

        for(i = 1; i <= 64; i++) {
            x <<= 1;
            if (x > number) break;
        }

        return i + ((signed) ? 1 : 0);
    }
}
