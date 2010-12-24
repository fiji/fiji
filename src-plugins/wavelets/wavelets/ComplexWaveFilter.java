package wavelets;

/**
 * This class generates the Complex wavelets filters.
 * 
 * @author
 * <p style="background-color:#EEEEEE; border-top:1px solid #CCCCCC; border-bottom:1px solid #CCCCCC"">
 * Daniel Sage<br>
 * <a href="http://bigwww.epfl.ch">Biomedical Imaging Group</a> (BIG), 
 * Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland<br>
 * More information: http://bigwww.epfl.ch/</p>
 * <p>You'll be free to use this software for research purposes, but you should
 *  not redistribute it without our consent. In addition, we expect you to 
 *  include a citation or acknowledgement whenever you present or publish 
 *  results that are based on it.</p>
 *  
 *  @version
 *  11 July 2009
 */

public class ComplexWaveFilter {

	/**
	 * real lowpass filter.
	 */
	public double h[];
	
	/**
	 * real highpass filter.
	 */
	public double g[];
	
	/**
	 * imaginary lowpass filter.
	 */
	public double hi[]; 
	/**
	 * imaginary highpass filter.
	 */
	public double gi[];
	
	/**
	 * The constructor generates the 4 filters for a giving length.
	 * @param length length of the filter, 6, 14 or 22
	 */
	ComplexWaveFilter(int length) {
		switch (length) {
		
		case 6:
			// Complex Daubechies 	
			// Real lowpass filter 
			h = new double [6];
			h[0] = -0.0662912607;
			h[1] = 0.1104854346;
			h[2] = 0.6629126074;
			h[3] = 0.6629126074;
			h[4] = 0.1104854346;
			h[5] = -0.0662912607;
			
			// Real highpass filter
			g = new double [6];
			g[5] = 0.0662912607;
			g[4] = 0.1104854346;
			g[3] = -0.6629126074;
			g[2] = 0.6629126074;
			g[1] = -0.1104854346;
			g[0] = -0.0662912607;
			
			// imaginary lowpass filter
			hi = new double [6];
			hi[0] = -0.0855816496;
			hi[1] = -0.0855816496;
			hi[2] = 0.1711632992;
			hi[3] = 0.1711632992;
			hi[4] = -0.0855816496;
			hi[5] = -0.0855816496;
			
			// Imaginary highpass filter
			gi = new double [6];
			gi[5] = -0.0855816496;
			gi[4] = 0.0855816496;
			gi[3] = 0.1711632992;
			gi[2] = -0.1711632992;
			gi[1] = -0.0855816496;
			gi[0] = 0.0855816496;
			break;
			
		case 14:	
			// Complex Daubechies 	
			// Real lowpass filter 
			h = new double [14];
			h[0 ] = 0.0049120149;
			h[1 ] = -0.0054111299;
			h[2 ] = -0.0701089996;
			h[3 ] = -0.0564377788;
			h[4 ] = 0.1872348173;
			h[5 ] = 0.3676385056;
			h[6 ] = 0.2792793518;
			h[7 ] = 0.2792793518;
			h[8 ] = 0.3676385056;
			h[9 ] = 0.1872348173;
			h[10] = -0.0564377788;
			h[11] = -0.0701089996;
			h[12] = -0.0054111299;
			h[13] = 0.0049120149;
			
			// Real highpass filter
			g = new double [14];
			g[13] = -0.0049120149;
			g[12] = -0.0054111299;
			g[11] = 0.0701089996;
			g[10] = -0.0564377788;
			g[9] = -0.1872348173;
			g[8] = 0.3676385056;
			g[7] = -0.2792793518;
			g[6] = 0.2792793518;
			g[5] = -0.3676385056;
			g[4] = 0.1872348173;
			g[3] = 0.0564377788;
			g[2] = -0.0701089996;
			g[1] = 0.0054111299;
			g[0] = 0.0049120149;
			
			// imaginary lowpass filter
			hi = new double [14];
			hi[0 ] = 0.0018464710;
			hi[1 ] = 0.0143947836;
			hi[2 ] = 0.0079040001;
			hi[3 ] = -0.1169376946;
			hi[4 ] = -0.2596312614;
			hi[5 ] = -0.0475928095;
			hi[6 ] = 0.4000165107;
			hi[7 ] = 0.4000165107;
			hi[8 ] = -0.0475928095;
			hi[9 ] = -0.2596312614;
			hi[10] = -0.1169376946;
			hi[11] = 0.0079040001;
			hi[12] = 0.0143947836;
			hi[13] = 0.0018464710;
			
			// Imaginary highpass filter
			gi = new double [14];
			gi[13] = 0.0018464710;
			gi[12] = -0.0143947836;
			gi[11] = 0.0079040001;
			gi[10] = 0.1169376946;
			gi[9 ] = -0.2596312614;
			gi[8 ] = 0.0475928095;
			gi[7 ] = 0.4000165107;
			gi[6 ] = -0.4000165107;
			gi[5 ] = -0.0475928095;
			gi[4 ] = 0.2596312614;
			gi[3 ] = -0.1169376946;
			gi[2 ] = -0.0079040001;
			gi[1 ] = 0.0143947836;
			gi[0 ] = -0.0018464710;
			break;
			
		case 22:	
			// Complex Daubechies 	
			// Real lowpass filter 
			h = new double [22];
			h[0 ] = -0.0002890832;
			h[1 ] = -0.0000935982;
			h[2 ] = 0.0059961342;
			h[3 ] = 0.0122232015;
			h[4 ] = -0.0243700791;
			h[5 ] = -0.1092940542;
			h[6 ] = -0.0918847036;
			h[7 ] = 0.1540094645;
			h[8 ] = 0.4014277015;
			h[9 ] = 0.3153022916;
			h[10] = 0.0440795062;
			h[11] = 0.0440795062;
			h[12] = 0.3153022916;
			h[13] = 0.4014277015;
			h[14] = 0.1540094645;
			h[15] = -0.0918847036;
			h[16] = -0.1092940542;
			h[17] = -0.0243700791;
			h[18] = 0.0122232015;
			h[19] = 0.0059961342;
			h[20] = -0.0000935982;
			h[21] = -0.0002890832;
			
			// Real highpass filter
			g = new double [22];
			g[21] = 0.0002890832;
			g[20] = -0.0000935982;
			g[19] = -0.0059961342;
			g[18] = 0.0122232015;
			g[17] = 0.0243700791;
			g[16] = -0.1092940542;
			g[15] = 0.0918847036;
			g[14] = 0.1540094645;
			g[13] = -0.4014277015;
			g[12] = 0.3153022916;
			g[11] = -0.0440795062;
			g[10] = 0.0440795062;
			g[9] = -0.3153022916;
			g[8] = 0.4014277015;
			g[7] = -0.1540094645;
			g[6] = -0.0918847036;
			g[5] = 0.1092940542;
			g[4] = -0.0243700791;
			g[3] = -0.0122232015;
			g[2] = 0.0059961342;
			g[1] = 0.0000935982;
			g[0] = -0.0002890832;
			
			// imaginary lowpass filter
			hi = new double [22];
			hi[0 ] = 0.0000211708;
			hi[1 ] = -0.0012780664;
			hi[2 ] = -0.0029648612;
			hi[3 ] = 0.0144283733;
			hi[4 ] = 0.0503067404;
			hi[5 ] = -0.0044659104;
			hi[6 ] = -0.1999654035;
			hi[7 ] = -0.2603015239;
			hi[8 ] = 0.0013800055;
			hi[9 ] = 0.2232934469;
			hi[10] = 0.1795460286;
			hi[11] = 0.1795460286;
			hi[12] = 0.2232934469;
			hi[13] = 0.0013800055;
			hi[14] = -0.2603015239;
			hi[15] = -0.1999654035;
			hi[16] = -0.0044659104;
			hi[17] = 0.0503067404;
			hi[18] = 0.0144283733;
			hi[19] = -0.0029648612;
			hi[20] = -0.0012780664;
			hi[21] = 0.0000211708;
			
			// Imaginary highpass filter
			gi = new double [22];
			gi[21] = 0.0000211708;
			gi[20] = 0.0012780664;
			gi[19] = -0.0029648612;
			gi[18] = -0.0144283733;
			gi[17] = 0.0503067404;
			gi[16] = 0.0044659104;
			gi[15] = -0.1999654035;
			gi[14] = 0.2603015239;
			gi[13] = 0.0013800055;
			gi[12] = -0.2232934469;
			gi[11] = 0.1795460286;
			gi[10] = -0.1795460286;
			gi[9] = 0.2232934469;
			gi[8] = -0.0013800055;
			gi[7] = -0.2603015239;
			gi[6] = 0.1999654035;
			gi[5] = -0.0044659104;
			gi[4] = -0.0503067404;
			gi[3] = 0.0144283733;
			gi[2] = 0.0029648612;
			gi[1] = -0.0012780664;
			gi[0] = -0.0000211708;
			break;
			
		default:
			throw(new RuntimeException("Invalid length"));
		}
		
	}
	
}