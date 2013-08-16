package imageware;

/**
 * Class ImageWare.
 *
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public interface ImageWare extends Process {

	public static final int UNDEFINED_TYPE = 0;
	public static final int BYTE	= 1;
	public static final int SHORT	= 2;
	public static final int FLOAT	= 3;
	public static final int DOUBLE	= 4;

	public static final byte UNDEFINED_BOUNDARY	= 0;
	public static final byte NONE		= 1;
	public static final byte MIRROR		= 2;
	public static final byte PERIODIC	= 3;

	public static final int UNDEFINED	= 0;
	public static final int CREATE		= 1;
	public static final int WRAP		= 2;
	
	public static final byte RED	= 0;
	public static final byte GREEN	= 1;
	public static final byte BLUE	= 2;
		
	public ImageWare	duplicate();
	public ImageWare	replicate();
	public ImageWare	replicate(int type);
	public void			copy(ImageWare source);
	public ImageWare	convert(int type);
	public void			printInfo();

	public void 	show();
	public void		show(String title);

	public double 	getMinimum();
	public double 	getMaximum();
	public double 	getMean();
	public double	getNorm1();
	public double	getNorm2();
	public double 	getTotal();
	public double[]	getMinMax();
	
}
