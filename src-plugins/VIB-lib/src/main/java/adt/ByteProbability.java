package adt;

import java.util.Random;

/**
 * User: Tom Larkworthy
 *
 * class that encodeds probilities as UNSIGNED bytes and makes things go seriously fast!
 * Date: 14-Jul-2006
 * Time: 00:08:49
 */
public class ByteProbability {

	/**
	 * converts from byte's to probabilities usage: BYTE_TO_DOUBLE[myByte&0xFF]
	 */
	public static final double [] BYTE_TO_DOUBLE = new double[256]; //fast convertion between numbers 0-255 and doubles intervales [0.1]
	public static final byte [] INTEGER_TO_BYTE = new byte[256]; //fast convertion between numbers 0-255 and unsigned bytes (packed in a signed byte)
	public static final byte[][] MULTIPLY = new byte[256][256];
    public static final byte[][] DIVIDE = new byte[256][256];


	static{
		for(int i=0; i<256; i++){
			BYTE_TO_DOUBLE[i] = (double)i/(double)255;
		}

		for(int i=Byte.MIN_VALUE; i<=Byte.MAX_VALUE; i++){
			byte val = (byte) i;
			int intVal = val&0xFF;
			INTEGER_TO_BYTE[intVal] = val;
		}

		for(int i=0; i<256; i++){
			for(int j=0; j<256; j++){
				MULTIPLY[i][j] = toByte(BYTE_TO_DOUBLE[i]*BYTE_TO_DOUBLE[j]);
			}
		}

		for(int i=0; i<256; i++){
			for(int j=0; j<256; j++){
				DIVIDE[i][j] = toByte(BYTE_TO_DOUBLE[i]/BYTE_TO_DOUBLE[j]);
			}
		}
	}

	/**
	 * not so easy, SEQ search SLOW!!!
	 * @param v
	 * @return
	 */
	public static byte toByte(double v) {
		byte nearest = 0;

		double nearestValue = Double.MAX_VALUE;

    	for(int i=0; i<256; i++){
			double val = BYTE_TO_DOUBLE[i];
			double diff = Math.abs(v-val);

			if(diff < nearestValue){
				nearestValue = diff;
				nearest = INTEGER_TO_BYTE[i];
			}
		}
		return nearest;
	}


	public static final byte multiply(byte a, byte b){
		return MULTIPLY[a&0xFF][b&0xFF];
	}

	public static final byte divide(byte a, byte b){
		return DIVIDE[a&0xFF][b&0xFF];
	}



	public static void main(String[] args) {

		for(int i=0;i<256;i++){
			System.out.println("i = " + i);
			byte tstByte = INTEGER_TO_BYTE[i];
			System.out.println("tstByte = " + tstByte);
			System.out.println("tstByte&0xFF = " + (tstByte & 0xFF));
			System.out.println("INTEGER_TO_BYTE[tstByte & 0xFF] = " + INTEGER_TO_BYTE[tstByte & 0xFF]);
            System.out.println("BYTE_TO_DOUBLE[tstByte&0xFF] = " + BYTE_TO_DOUBLE[tstByte & 0xFF]);
			tstByte++;
		}

		Random rnd = new Random();
		for(int i=0; i< 20; i++){
			int index1 = rnd.nextInt(256);
			int index2 = rnd.nextInt(256);

			byte a = INTEGER_TO_BYTE[index1];
			byte b = INTEGER_TO_BYTE[index2];

			double ad = BYTE_TO_DOUBLE[a&0xFF];
			double bd = BYTE_TO_DOUBLE[b&0xFF];

			double multiply = BYTE_TO_DOUBLE[MULTIPLY[a&0xFF][b&0xFF] & 0xFF];
			double check = ad*bd;
			System.out.println("multiply via byte = " + multiply);
			System.out.println("check via double = " + check);
		}

		for(int i=0; i< 20; i++){
			int index1 = rnd.nextInt(256);
			int index2 = rnd.nextInt(256);

			byte a = INTEGER_TO_BYTE[index1];
			byte b = INTEGER_TO_BYTE[index2];

			double ad = BYTE_TO_DOUBLE[a&0xFF];
			double bd = BYTE_TO_DOUBLE[b&0xFF];

			double multiply = BYTE_TO_DOUBLE[DIVIDE[a&0xFF][b&0xFF] & 0xFF];
			double check = ad/bd;
			System.out.println("divide via byte = " + multiply);
			System.out.println("check via double = " + check);
		}
	}
}
