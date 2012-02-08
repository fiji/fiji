package io;

import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Opens the proprietary FIB-SEM format used at Janelia Farm
 *
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class FIBSEM_Reader implements PlugIn
{
	/**
	 * Opens it either as:
	 * UnsignedShort with the range [0..65535] where 0 == -10 Volts and 65535 == +10 Volts (default)
	 * Float with a range from -10 Volts to +10 Volts
	 *
	 * Note, the voltage might be different in future versions, it is stored in FIBSEMData.detMin and FIBSEMData.detMax
	 */
	public static boolean openAsFloat = false;

	/**
	 * Stores the current header if somebody wants access to it
	 */
	FIBSEMData header;

	/**
	 * @return - the header of the last opened FIB-SEM file
	 */
	public FIBSEMData getHeader() { return header; }

	@Override
	public void run( final String filename )
	{
		File f = new File( filename );

		// try to open, otherwise query
		if ( !f.exists() )
		{
			final OpenDialog od = new OpenDialog( "Open FIB-SEM raw file", null );

			f = new File( od.getDirectory() + od.getFileName() );

			if ( !f.exists() )
			{
				IJ.log( "Cannot find file '" + f.getAbsolutePath() + "'" );
				return;
			}
		}

		try
		{
			final FileInputStream file = new FileInputStream( f );
			final FIBSEMData header = parseHeader( file );

			if ( header == null )
			{
				IJ.log( "The file '" + f.getAbsolutePath() + "' is not a FIB-SEM raw file, the magic number does not match." );
				return;
			}

			//System.out.println( header );

			final ImagePlus imp = readFIBSEM( header, file, openAsFloat );
			file.close();

			if ( imp == null )
				return;

			this.header = header;

			// set the filename
			imp.setTitle( f.getName() );
			Calibration cal = imp.getCalibration();
			cal.setXUnit( "nm" );
			cal.setYUnit( "nm" );
			cal.pixelWidth = header.pixelSize;
			cal.pixelHeight = header.pixelSize;

			imp.show();
		}
		catch ( FileNotFoundException e )
		{
			IJ.log( "Error opening the file '" + f.getAbsolutePath() + "': " + e );
			return;
		}
		catch ( IOException e )
		{
			IJ.log( "Error parsing the file '" + f.getAbsolutePath() + "': " + e );
			return;
		}
	}

	/**
	 * Determines if the given file is a FIB-SEM file based on the magic number (first 4 bytes)
	 *
	 * @param f - the File
	 * @return
	 */
	public static boolean isFIBSEM( final File f )
	{
		try
		{
			final FileInputStream file = new FileInputStream( f );
			final DataInputStream s = new DataInputStream( file );
			final long magicNumber = getUnsignedInt( s.readInt() );

			s.close();

			if ( magicNumber == 3555587570l )
				return true;
			else
				return false;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	public ImagePlus readFIBSEM( final FIBSEMData header, final FileInputStream file, boolean openAsFloat ) throws IOException
	{
		ImagePlus imp;
		double[] minmax = new double[] { Double.MAX_VALUE, Double.MIN_VALUE };

		if ( header.numChannels == 1 )
		{
			imp = new ImagePlus( "", readChannel( header, file, minmax, openAsFloat ) );
		}
		else
		{
			imp = new ImagePlus( "", readChannels( header, file, minmax, openAsFloat ) );
			imp.setDimensions( 1, header.numChannels, 1 );
			imp =  new CompositeImage( imp, CompositeImage.GRAYSCALE );
		}

		imp.setDisplayRange( minmax[ 0 ], minmax[ 1 ] );
		return imp;
	}

	final ImageProcessor readChannel( final FIBSEMData header, final FileInputStream file, double[] minmax, boolean openAsFloat ) throws IOException
	{
		// it is always unsigned short
		final byte[] slice = new byte[ (int)header.xRes * (int)header.yRes * 2 ];
		file.read( slice );

		// for the display range
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		final ImageProcessor ip;

		if ( openAsFloat )
		{
			final float[] floatSlice = new float[ (int)header.xRes * (int)header.yRes ];

			for ( int i = 0; i < floatSlice.length; ++i )
			{
				int j = 2 * i;
				int v = ( slice[ j ] ) << 8;
				v += ( slice[ j + 1 ] );

				final float v2 = (header.offset[ 0 ] + v * header.gain[ 0 ]);

				if ( v2 < min ) min = v2;
				if ( v2 > max ) max = v2;
				floatSlice[ i ] = v2;
			}
			ip = new FloatProcessor( (int)header.xRes, (int)header.yRes, floatSlice, null );
		}
		else
		{
			final short[] shortSlice = new short[ (int)header.xRes * (int)header.yRes ];

			final float minVolts = (float)header.detMin;
			final float rangeVolts = (float)header.detMax - (float)header.detMin;

			for ( int i = 0; i < shortSlice.length; ++i )
			{
				int j = 2 * i;
				int v = ( slice[ j ] ) << 8;
				v += ( slice[ j + 1 ] );

				v = Math.round( ((header.offset[ 0 ] + v * header.gain[ 0 ])-minVolts)/rangeVolts*65535.0f );

				if ( v < min ) min = v;
				if ( v > max ) max = v;
				shortSlice[ i ] = (short)v;
			}
			ip = new ShortProcessor( (int)header.xRes, (int)header.yRes, shortSlice, null );
		}

		minmax[ 0 ] = min;
		minmax[ 1 ] = max;

		return ip;
	}

	final ImageStack readChannels( final FIBSEMData header, final FileInputStream file, double[] minmax, boolean openAsFloat ) throws IOException
	{
		final int numChannels = header.numChannels;

		// it is always unsigned short
		final byte[] slice = new byte[ (int)header.xRes * (int)header.yRes * numChannels * 2 ];
		file.read( slice );

		// for the display range
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		final ImageStack stack;

		if ( openAsFloat )
		{
			final float[][] floatSlice = new float[ numChannels ][ (int)header.xRes * (int)header.yRes ];


			for ( int i = 0; i < floatSlice[ 0 ].length; ++i )
			{
				for ( int c = 0; c < numChannels; ++c )
				{
					int j = 2 * i * numChannels + 2 * c;
					int v = ( slice[ j ] ) << 8;
					v += ( slice[ j + 1 ] );

					final float v2 = header.offset[ 0 ] + v * header.gain[ 0 ];

					if ( v2 < min ) min = v2;
					if ( v2 > max ) max = v2;
					floatSlice[ c ][ i ] = v2;
				}
			}

			stack = new ImageStack( (int)header.xRes, (int)header.yRes );

			for ( int c = 0; c < numChannels; ++c )
				stack.addSlice( "channel " + c, new FloatProcessor( (int)header.xRes, (int)header.yRes, floatSlice[ c ], null ) );
		}
		else
		{
			final short[][] shortSlice = new short[ numChannels ][ (int)header.xRes * (int)header.yRes ];

			final float minVolts = (float)header.detMin;
			final float rangeVolts = (float)header.detMax - (float)header.detMin;

			for ( int i = 0; i < shortSlice[ 0 ].length; ++i )
			{
				for ( int c = 0; c < numChannels; ++c )
				{
					int j = 2 * i * numChannels + 2 * c;
					int v = ( slice[ j ] ) << 8;
					v += ( slice[ j + 1 ] );

					v = Math.round( ((header.offset[ 0 ] + v * header.gain[ 0 ])-minVolts)/rangeVolts*65535.0f );

					if ( v < min ) min = v;
					if ( v > max ) max = v;
					shortSlice[ c ][ i ] = (short)(v);
				}
			}

			stack = new ImageStack( (int)header.xRes, (int)header.yRes );

			for ( int c = 0; c < numChannels; ++c )
				stack.addSlice( "channel " + c, new ShortProcessor( (int)header.xRes, (int)header.yRes, shortSlice[ c ], null ) );

		}

		minmax[ 0 ] = min;
		minmax[ 1 ] = max;

		return stack;
	}

	/**
	 * Parses the header and sets the {@link FileInputStream} to right location where the raw image data starts
	 *
	 * @param file - the input file
	 * @return the {@link FIBSEMData} that contains all meta-data or null if the magic number (file id) does not match
	 * @throws IOException
	 */
	public FIBSEMData parseHeader( final FileInputStream file ) throws IOException
	{
		// read the header
		final DataInputStream s = new DataInputStream( file );
		final FIBSEMData data = new FIBSEMData();

		//
		// parse the data
		//

		// fseek(fid,0,'bof'); FIBSEMData.FileMagicNum = fread(fid,1,'uint32'); % Read in magic number, should be 3555587570
		data.magicNumber = getUnsignedInt( s.readInt() );

		if ( data.magicNumber != 3555587570l )
			return null;

		// fseek(fid,4,'bof'); FIBSEMData.FileVersion = fread(fid,1,'uint16'); % Read in file version number
		data.fileVersion = getUnsignedShort( s.readShort() );
		// fseek(fid,6,'bof'); FIBSEMData.FileType = fread(fid,1,'uint16'); % Read in file type, 1 is Zeiss Neon detectors
		data.fileType = getUnsignedShort( s.readShort() );
		s.skip( 16 );
		// fseek(fid,24,'bof'); FIBSEMData.TimeStep = fread(fid,1,'double'); % Read in AI sampling time (including oversampling) in seconds
		data.timeStep = s.readDouble();
		// fseek(fid,32,'bof'); FIBSEMData.ChanNum = fread(fid,1,'uint8'); % Read in number of channels
		data.numChannels = getUnsignedByte( s.readByte() );
		data.offset = new float[ data.numChannels ];
		data.gain = new float[ data.numChannels ];
		data.secondOrder = new float[ data.numChannels ];
		data.thirdOrder = new float[ data.numChannels ];
		s.skip( 3 );


		if ( data.fileVersion == 1 )
		{
			// fseek(fid,36,'bof'); FIBSEMData.Scaling = single(fread(fid,[4,FIBSEMData.ChanNum],'double')); % Read in AI channel scaling factors, (col#: AI#), (row#: offset, gain, 2nd order, 3rd order)
			for ( int c = 0; c < data.numChannels; ++c )
			{
				data.offset[ c ] = (float)s.readDouble();
				data.gain[ c ] = (float)s.readDouble();
				data.secondOrder[ c ] = (float)s.readDouble();
				data.thirdOrder[ c ] = (float)s.readDouble();
			}

			s.skip( 64 - data.numChannels*8*4 );
		}
		else
		{
			// fseek(fid,36,'bof'); FIBSEMData.Scaling = fread(fid,[4,FIBSEMData.ChanNum],'single');
			for ( int c = 0; c < data.numChannels; ++c )
			{
				data.offset[ c ] = s.readFloat();
				data.gain[ c ] = s.readFloat();
				data.secondOrder[ c ] = s.readFloat();
				data.thirdOrder[ c ] = s.readFloat();
			}

			s.skip( 64 - data.numChannels*4*4 );
		}

		// fseek(fid,100,'bof'); FIBSEMData.XResolution = fread(fid,1,'uint32'); % X resolution
		data.xRes = getUnsignedInt( s.readInt() );
		// fseek(fid,104,'bof'); FIBSEMData.YResolution = fread(fid,1,'uint32'); % Y resolution
		data.yRes = getUnsignedInt( s.readInt() );

		if ( data.fileVersion == 1 || data.fileVersion == 2 || data.fileVersion == 3 )
		{
		    // fseek(fid,108,'bof'); FIBSEMData.Oversampling = fread(fid,1,'uint8'); % AI oversampling
			data.oversampling = getUnsignedByte( s.readByte() );
		    // fseek(fid,109,'bof'); FIBSEMData.AIDelay = fread(fid,1,'int16'); % Read AI delay (# of samples)
			data.AIdelay = s.readShort();
		}
		else
		{
		    // fseek(fid,108,'bof'); FIBSEMData.Oversampling = fread(fid,1,'uint16'); % AI oversampling
			data.oversampling = getUnsignedShort( s.readShort() );
			s.skip( 1 );
		}

		// fseek(fid,111,'bof'); FIBSEMData.ZeissScanSpeed = fread(fid,1,'uint8'); % Scan speed (Zeiss #)
		data.zeissScanSpeed = getUnsignedByte( s.readByte() );

		if ( data.fileVersion == 1 || data.fileVersion == 2 || data.fileVersion == 3 )
		{
		    // fseek(fid,112,'bof'); FIBSEMData.ScanRate = fread(fid,1,'double'); % Actual AO (scanning) rate
			data.scanRate = s.readDouble();
			// fseek(fid,120,'bof'); FIBSEMData.FramelineRampdownRatio = fread(fid,1,'double'); % Frameline rampdown ratio
			data.framelineRampdownRatio = s.readDouble();
			// fseek(fid,128,'bof'); FIBSEMData.Xmin = fread(fid,1,'double'); % X coil minimum voltage
			data.xMin = s.readDouble();
			// fseek(fid,136,'bof'); FIBSEMData.Xmax = fread(fid,1,'double'); % X coil maximum voltage
			data.xMax = s.readDouble();
			// FIBSEMData.Detmin = -10; % Detector minimum voltage
			data.detMin = -10.0;
			// FIBSEMData.Detmax = 10; % Detector maximum voltage
			data.detMax = 10.0;

			s.skip( 151 - 144 );
		}
		else
		{
		    // fseek(fid,112,'bof'); FIBSEMData.ScanRate = fread(fid,1,'single'); % Actual AO (scanning) rate
			data.scanRate = s.readFloat();
			// fseek(fid,116,'bof'); FIBSEMData.FramelineRampdownRatio = fread(fid,1,'single'); % Frameline rampdown ratio
			data.framelineRampdownRatio = s.readFloat();
			// fseek(fid,120,'bof'); FIBSEMData.Xmin = fread(fid,1,'single'); % X coil minimum voltage
			data.xMin = s.readFloat();
			// fseek(fid,124,'bof'); FIBSEMData.Xmax = fread(fid,1,'single'); % X coil maximum voltage
			data.xMax = s.readFloat();
			// fseek(fid,128,'bof'); FIBSEMData.Detmin = fread(fid,1,'single'); % Detector minimum voltage
			data.detMin = s.readFloat();
			// fseek(fid,132,'bof'); FIBSEMData.Detmax = fread(fid,1,'single'); % Detector maximum voltage
			data.detMax = s.readFloat();

			s.skip( 151 - 136 );
		}

		// fseek(fid,151,'bof'); FIBSEMData.AI1 = fread(fid,1,'uint8'); % AI Ch1
		data.AI1 = getUnsignedByte( s.readByte() );
		// fseek(fid,152,'bof'); FIBSEMData.AI2 = fread(fid,1,'uint8'); % AI Ch2
		data.AI2 = getUnsignedByte( s.readByte() );
		// fseek(fid,153,'bof'); FIBSEMData.AI3 = fread(fid,1,'uint8'); % AI Ch3
		data.AI3 = getUnsignedByte( s.readByte() );
		// fseek(fid,154,'bof'); FIBSEMData.AI4 = fread(fid,1,'uint8'); % AI Ch4
		data.AI4 = getUnsignedByte( s.readByte() );

		s.skip( 180 - 155 );

		// fseek(fid,180,'bof'); FIBSEMData.Notes = fread(fid,200,'*char')'; % Read in notes		 */
		byte[] tmp = new byte[ 200 ];
		s.read( tmp );
		data.notes = new String( tmp );

		if ( data.fileVersion == 1 || data.fileVersion == 2 )
		{
			// fseek(fid,380,'bof'); FIBSEMData.DetA = fread(fid,10,'*char')'; % Name of detector A
			tmp = new byte[ 10 ];
			s.read( tmp );
			data.detectorA = new String( tmp );

			// fseek(fid,390,'bof'); FIBSEMData.DetB = fread(fid,18,'*char')'; % Name of detector B
			tmp = new byte[ 18 ];
			s.read( tmp );
			data.detectorB = new String( tmp );

			// fseek(fid,408,'bof'); FIBSEMData.Mag = fread(fid,1,'double'); % Magnification
			data.magnification = s.readDouble();

			// fseek(fid,416,'bof'); FIBSEMData.PixelSize = fread(fid,1,'double'); % Pixel size in nm
			data.pixelSize = s.readDouble();

			// fseek(fid,424,'bof'); FIBSEMData.WD = fread(fid,1,'double'); % Working distance in mm
			data.wd = s.readDouble();

			// fseek(fid,432,'bof'); FIBSEMData.EHT = fread(fid,1,'double'); % EHT in kV
			data.eht = s.readDouble();

			// fseek(fid,440,'bof'); FIBSEMData.SEMApr = fread(fid,1,'uint8'); % SEM aperture number
			data.semApr = getUnsignedByte( s.readByte() );

			// fseek(fid,441,'bof'); FIBSEMData.HighCurrent = fread(fid,1,'uint8'); % high current mode (1=on, 0=off)
			data.highCurrent = getUnsignedByte( s.readByte() );

			s.skip( 448 - 442 );

			// fseek(fid,448,'bof'); FIBSEMData.SEMCurr = fread(fid,1,'double'); % SEM probe current in A
			data.semCurr = s.readDouble();

			// fseek(fid,456,'bof'); FIBSEMData.SEMRot = fread(fid,1,'double'); % SEM scan roation in degree
			data.semRot = s.readDouble();

			// fseek(fid,464,'bof'); FIBSEMData.ChamVac = fread(fid,1,'double'); % Chamber vacuum
			data.chamVac = s.readDouble();

			// fseek(fid,472,'bof'); FIBSEMData.GunVac = fread(fid,1,'double'); % E-gun vacuum
			data.gunVac = s.readDouble();

			// fseek(fid,480,'bof'); FIBSEMData.SEMStiX = fread(fid,1,'double'); % SEM stigmation X
			data.semStiX = s.readDouble();

			// fseek(fid,488,'bof'); FIBSEMData.SEMStiY = fread(fid,1,'double'); % SEM stigmation Y
			data.semStiY = s.readDouble();

			// fseek(fid,496,'bof'); FIBSEMData.SEMAlnX = fread(fid,1,'double'); % SEM aperture alignment X
			data.semAlnX = s.readDouble();

			// fseek(fid,504,'bof'); FIBSEMData.SEMAlnY = fread(fid,1,'double'); % SEM aperture alignment Y
			data.semAlnY = s.readDouble();

			// fseek(fid,512,'bof'); FIBSEMData.StageX = fread(fid,1,'double'); % Stage position X in mm
			data.stageX = s.readDouble();

			// fseek(fid,520,'bof'); FIBSEMData.StageY = fread(fid,1,'double'); % Stage position Y in mm
			data.stageY = s.readDouble();

			// fseek(fid,528,'bof'); FIBSEMData.StageZ = fread(fid,1,'double'); % Stage position Z in mm
			data.stageZ = s.readDouble();

			// fseek(fid,536,'bof'); FIBSEMData.StageT = fread(fid,1,'double'); % Stage position T in degree
			data.stageT = s.readDouble();

			// fseek(fid,544,'bof'); FIBSEMData.StageR = fread(fid,1,'double'); % Stage position R in degree
			data.stageR = s.readDouble();

			// fseek(fid,552,'bof'); FIBSEMData.StageM = fread(fid,1,'double'); % Stage position M in mm
			data.stageM = s.readDouble();

			// fseek(fid,560,'bof'); FIBSEMData.BrightnessA = fread(fid,1,'double'); % Detector A brightness (%)
			data.brightnessA = s.readDouble();

			// fseek(fid,568,'bof'); FIBSEMData.ContrastA = fread(fid,1,'double'); % Detector A contrast (%)
			data.contrastA = s.readDouble();

			// fseek(fid,576,'bof'); FIBSEMData.BrightnessB = fread(fid,1,'double'); % Detector B brightness (%)
			data.brightnessB = s.readDouble();

			// fseek(fid,584,'bof'); FIBSEMData.ContrastB = fread(fid,1,'double'); % Detector B contrast (%)
			data.contrastB = s.readDouble();

			s.skip( 600 - 592 );

			// fseek(fid,600,'bof'); FIBSEMData.Mode = fread(fid,1,'uint8'); % FIB mode: 0=SEM, 1=FIB, 2=Milling, 3=SEM+FIB, 4=Mill+SEM, 5=SEM Drift Correction, 6=FIB Drift Correction, 7=No Beam, 8=External, 9=External+SEM
			data.mode = getUnsignedByte( s.readByte() );

			s.skip( 608 - 601 );

			// fseek(fid,608,'bof'); FIBSEMData.FIBFocus = fread(fid,1,'double'); % FIB focus in kV
			data.fibFocus = s.readDouble();

			// fseek(fid,616,'bof'); FIBSEMData.FIBProb = fread(fid,1,'uint8'); % FIB probe number
			data.fibProb = getUnsignedByte( s.readByte() );

			s.skip( 624 - 617 );

			// fseek(fid,624,'bof'); FIBSEMData.FIBCurr = fread(fid,1,'double'); % FIB emission current
			data.fibCurr = s.readDouble();

			// fseek(fid,632,'bof'); FIBSEMData.FIBRot = fread(fid,1,'double'); % FIB scan rotation
			data.fibRot = s.readDouble();

			// fseek(fid,640,'bof'); FIBSEMData.FIBAlnX = fread(fid,1,'double'); % FIB aperture alignment X
			data.fibAlnX = s.readDouble();

			// fseek(fid,648,'bof'); FIBSEMData.FIBAlnY = fread(fid,1,'double'); % FIB aperture alignment Y
			data.fibAlnY = s.readDouble();

			// fseek(fid,656,'bof'); FIBSEMData.FIBStiX = fread(fid,1,'double'); % FIB stigmation X
			data.fibStiX = s.readDouble();

			// fseek(fid,664,'bof'); FIBSEMData.FIBStiY = fread(fid,1,'double'); % FIB stigmation Y
			data.fibStiY = s.readDouble();

			// fseek(fid,672,'bof'); FIBSEMData.FIBShiftX = fread(fid,1,'double'); % FIB beam shift X in micron
			data.fibShiftX = s.readDouble();

			// fseek(fid,680,'bof'); FIBSEMData.FIBShiftY = fread(fid,1,'double'); % FIB beam shift Y in micron
			data.fibShiftY = s.readDouble();

			s.skip( 700 - 688 );

			// fseek(fid,700,'bof'); FIBSEMData.DetC = fread(fid,20,'*char')'; % Name of detector C
			tmp = new byte[ 20 ];
			s.read( tmp );
			data.detectorC = new String( tmp );

			// fseek(fid,720,'bof'); FIBSEMData.DetD = fread(fid,20,'*char')'; % Name of detector D
			s.read( tmp );
			data.detectorD = new String( tmp );

			s.skip( 800 - 740 );
		}
		else
		{
		    // fseek(fid,380,'bof'); FIBSEMData.DetA = fread(fid,10,'*char')'; % Name of detector A
			tmp = new byte[ 10 ];
			s.read( tmp );
			data.detectorA = new String( tmp );

			// fseek(fid,390,'bof'); FIBSEMData.DetB = fread(fid,18,'*char')'; % Name of detector B
			tmp = new byte[ 18 ];
			s.read( tmp );
			data.detectorB = new String( tmp );

			s.skip( 410 - 408 );

			// fseek(fid,410,'bof'); FIBSEMData.DetC = fread(fid,20,'*char')'; % Name of detector C
			tmp = new byte[ 20 ];
			s.read( tmp );
			data.detectorC = new String( tmp );

			// fseek(fid,430,'bof'); FIBSEMData.DetD = fread(fid,20,'*char')'; % Name of detector D
			s.read( tmp );
			data.detectorD = new String( tmp );

			s.skip( 460 - 450 );

			// fseek(fid,460,'bof'); FIBSEMData.Mag = fread(fid,1,'single'); % Magnification
			data.magnification = s.readFloat();

			// fseek(fid,464,'bof'); FIBSEMData.PixelSize = fread(fid,1,'single'); % Pixel size in nm
			data.pixelSize = s.readFloat();

			// fseek(fid,468,'bof'); FIBSEMData.WD = fread(fid,1,'single'); % Working distance in mm
			data.wd = s.readFloat();

			// fseek(fid,472,'bof'); FIBSEMData.EHT = fread(fid,1,'single'); % EHT in kV
			data.eht = s.readFloat();

			s.skip( 480 - 476 );

			// fseek(fid,480,'bof'); FIBSEMData.SEMApr = fread(fid,1,'uint8'); % SEM aperture number
			data.semApr = getUnsignedByte( s.readByte() );

			// fseek(fid,481,'bof'); FIBSEMData.HighCurrent = fread(fid,1,'uint8'); % high current mode (1=on, 0=off)
			data.highCurrent = getUnsignedByte( s.readByte() );

			s.skip( 490 - 482 );

			// fseek(fid,490,'bof'); FIBSEMData.SEMCurr = fread(fid,1,'single'); % SEM probe current in A
			data.semCurr = s.readFloat();

			// fseek(fid,494,'bof'); FIBSEMData.SEMRot = fread(fid,1,'single'); % SEM scan roation in degree
			data.semRot = s.readFloat();

			// fseek(fid,498,'bof'); FIBSEMData.ChamVac = fread(fid,1,'single'); % Chamber vacuum
			data.chamVac = s.readFloat();

			// fseek(fid,502,'bof'); FIBSEMData.GunVac = fread(fid,1,'single'); % E-gun vacuum
			data.gunVac = s.readFloat();

			s.skip( 510 - 506 );

			// fseek(fid,510,'bof'); FIBSEMData.SEMShiftX = fread(fid,1,'single'); % SEM beam shift X
			data.semShiftX = s.readFloat();

			// fseek(fid,514,'bof'); FIBSEMData.SEMShiftY = fread(fid,1,'single'); % SEM beam shift Y
			data.semShiftY = s.readFloat();

			// fseek(fid,518,'bof'); FIBSEMData.SEMStiX = fread(fid,1,'single'); % SEM stigmation X
			data.semStiX = s.readFloat();

			// fseek(fid,522,'bof'); FIBSEMData.SEMStiY = fread(fid,1,'single'); % SEM stigmation Y
			data.semStiY = s.readFloat();

			// fseek(fid,526,'bof'); FIBSEMData.SEMAlnX = fread(fid,1,'single'); % SEM aperture alignment X
			data.semAlnX = s.readFloat();

			// fseek(fid,530,'bof'); FIBSEMData.SEMAlnY = fread(fid,1,'single'); % SEM aperture alignment Y
			data.semAlnY = s.readFloat();

			// fseek(fid,534,'bof'); FIBSEMData.StageX = fread(fid,1,'single'); % Stage position X in mm
			data.stageX = s.readFloat();

			// fseek(fid,538,'bof'); FIBSEMData.StageY = fread(fid,1,'single'); % Stage position Y in mm
			data.stageY = s.readFloat();

			// fseek(fid,542,'bof'); FIBSEMData.StageZ = fread(fid,1,'single'); % Stage position Z in mm
			data.stageZ = s.readFloat();

			// fseek(fid,546,'bof'); FIBSEMData.StageT = fread(fid,1,'single'); % Stage position T in degree
			data.stageT = s.readFloat();

			// fseek(fid,550,'bof'); FIBSEMData.StageR = fread(fid,1,'single'); % Stage position R in degree
			data.stageR = s.readFloat();

			// fseek(fid,554,'bof'); FIBSEMData.StageM = fread(fid,1,'single'); % Stage position M in mm
			data.stageM = s.readFloat();

			s.skip( 560 - 558 );

			// fseek(fid,560,'bof'); FIBSEMData.BrightnessA = fread(fid,1,'single'); % Detector A brightness (%)
			data.brightnessA = s.readFloat();

			// fseek(fid,564,'bof'); FIBSEMData.ContrastA = fread(fid,1,'single'); % Detector A contrast (%)
			data.contrastA = s.readFloat();

			// fseek(fid,568,'bof'); FIBSEMData.BrightnessB = fread(fid,1,'single'); % Detector B brightness (%)
			data.brightnessB = s.readFloat();

			// fseek(fid,572,'bof'); FIBSEMData.ContrastB = fread(fid,1,'single'); % Detector B contrast (%)
			data.contrastB = s.readFloat();

			s.skip( 600 - 576 );

			// fseek(fid,600,'bof'); FIBSEMData.Mode = fread(fid,1,'uint8'); % FIB mode: 0=SEM, 1=FIB, 2=Milling, 3=SEM+FIB, 4=Mill+SEM, 5=SEM Drift Correction, 6=FIB Drift Correction, 7=No Beam, 8=External, 9=External+SEM
			data.mode = getUnsignedByte( s.readByte() );

			s.skip( 604 - 601 );

			// fseek(fid,604,'bof'); FIBSEMData.FIBFocus = fread(fid,1,'single'); % FIB focus in kV
			data.fibFocus = s.readFloat();

			// fseek(fid,608,'bof'); FIBSEMData.FIBProb = fread(fid,1,'uint8'); % FIB probe number
			data.fibProb = getUnsignedByte( s.readByte() );

			s.skip( 620 - 609 );

			// fseek(fid,620,'bof'); FIBSEMData.FIBCurr = fread(fid,1,'single'); % FIB emission current
			data.fibCurr = s.readFloat();

			// fseek(fid,624,'bof'); FIBSEMData.FIBRot = fread(fid,1,'single'); % FIB scan rotation
			data.fibRot = s.readFloat();

			// fseek(fid,628,'bof'); FIBSEMData.FIBAlnX = fread(fid,1,'single'); % FIB aperture alignment X
			data.fibAlnX = s.readFloat();

			// fseek(fid,632,'bof'); FIBSEMData.FIBAlnY = fread(fid,1,'single'); % FIB aperture alignment Y
			data.fibAlnY = s.readFloat();

			// fseek(fid,636,'bof'); FIBSEMData.FIBStiX = fread(fid,1,'single'); % FIB stigmation X
			data.fibStiX = s.readFloat();

			// fseek(fid,640,'bof'); FIBSEMData.FIBStiY = fread(fid,1,'single'); % FIB stigmation Y
			data.fibStiY = s.readFloat();

			// fseek(fid,644,'bof'); FIBSEMData.FIBShiftX = fread(fid,1,'single'); % FIB beam shift X in micron
			data.fibShiftX = s.readFloat();

			// fseek(fid,648,'bof'); FIBSEMData.FIBShiftY = fread(fid,1,'single'); % FIB beam shift Y in micron
			data.fibShiftY = s.readFloat();

			s.skip( 800 - 652 );
		}

		// fseek(fid,800,'bof'); FIBSEMData.MachineID = fread(fid,160,'*char')'; % Read in Machine ID
		tmp = new byte[ 160 ];
		s.read( tmp );
		data.machineID = new String( tmp );

		s.skip( 1000 - 960 );

		// fseek(fid,1000,'bof'); FIBSEMData.FileLength = fread(fid,1,'int64'); % Read in file length in bytes
		data.fileLength = s.readLong();

		s.skip( 1024 - 968 );

		return data;
	}

	public class FIBSEMData
	{
		/* the magic number identifying the file, should be 3555587570 */
		long magicNumber;
		/* the version of the file, supported until now is 1,2,3 */
		int fileVersion;
		/* file type, 1 is Zeiss Neon detectors */
		int fileType;
		/* AI sampling time (including oversampling) in seconds */
		double timeStep;
		/* the number of channels */
		int numChannels;
		/* the parameters required to transform the 16 bit signed signal back to volts:
		 * volts = offset + intensity*gain
		 * (we ignore second and third order as they are zero anyways)
		 */
		float[] offset, gain, secondOrder, thirdOrder;
		/* number of pixels in x per channel */
		long xRes;
		/* number of pixels in y per channel */
		long yRes;
		/* AI oversampling */
		int oversampling;
		/* Read AI delay (# of samples) - only v3*/
		int AIdelay = 0;
		/* Scan speed (Zeiss #) */
		int zeissScanSpeed;

	    /* Actual AO (scanning) rate */
		double scanRate;
		/* Frameline rampdown ratio */
		double framelineRampdownRatio;
		/* X coil minimum voltage */
		double xMin;
		/* X coil maximum voltage */
		double xMax;
		/* Detector minimum voltage */
		double detMin;
		/* Detector maximum voltage */
		double detMax;

		/* AI Ch1 */
		int AI1;
		/* AI Ch2 */
		int AI2;
		/* AI Ch3 */
		int AI3;
		/* AI Ch4 */
		int AI4;

		/* notes */
		String notes;

		/* Name of detector A */
		String detectorA = "";
		/* Name of detector B */
		String detectorB = "";
		/* Name of detector C */
		String detectorC = "";
		/* Name of detector D */
		String detectorD = "";

		/* Magnification */
		double magnification;
		/* Pixel size in nm */
		double pixelSize;
		/* Working distance in mm */
		double wd;
		/* EHT in kV */
		double eht;
		/* SEM aperture number */
		int semApr;
		/* high current mode (1=on, 0=off) */
		int highCurrent;
		/* FIB mode: 0=SEM, 1=FIB, 2=Milling, 3=SEM+FIB, 4=Mill+SEM, 5=SEM Drift Correction, 6=FIB Drift Correction, 7=No Beam, 8=External, 9=External+SEM */
		int mode;

		/* SEM probe current in A */
		double semCurr;
		/* SEM scan roation in degree */
		double semRot;
		/* Chamber Vacuum */
		double chamVac;
		/* Gun vacuum */
		double gunVac;
		/* SEM beam shift X */
		double semShiftX;
		/* SEM beam shift Y */
		double semShiftY;
		/* SEM stigmation X */
		double semStiX;
		/* SEM stigmation Y */
		double semStiY;
		/* SEM aperture alignment X */
		double semAlnX;
		/* SEM aperture alignment Y */
		double semAlnY;
		/* Stage position X in mm */
		double stageX;
		/* Stage position Y in mm */
		double stageY;
		/* Stage position Z in mm */
		double stageZ;
		/* Stage position T in degree */
		double stageT;
		/* Stage position R in degree */
		double stageR;
		/* Stage position M in mm */
		double stageM;
		/* Detector A brightness (%) */
		double brightnessA;
		/* Detector A contrast (%) */
		double contrastA;
		/* Detector B brightness (%) */
		double brightnessB;
		/* Detector B contrast (%) */
		double contrastB;
		/* FIB focus in kV */
		double fibFocus;
		/* FIB probe number */
		int fibProb;
		/* FIB emission current */
		double fibCurr;
		/* FIB scan rotation */
		double fibRot;
		/* FIB aperture alignment X */
		double fibAlnX;
		/* FIB aperture alignment Y */
		double fibAlnY;
		/* FIB stigmation X */
		double fibStiX;
		/* FIB stigmation Y */
		double fibStiY;
		/* FIB beam shift X in micron */
		double fibShiftX;
		/* FIB beam shift Y in micron */
		double fibShiftY;

		/* name of the machine */
		String machineID;
		/* file length in bytes */
		long fileLength;

		public String toString()
		{
			String offsetString = "";
			String gainString = "";
			String secondOrderString = "";
			String thirdOrderString = "";

			for ( int c = 0; c < numChannels; ++c )
				offsetString += "offset channel " + c + " = " + offset[ c ] + "\n";

			for ( int c = 0; c < numChannels; ++c )
				gainString += "gain channel " + c + " = " + gain[ c ] + "\n";

			for ( int c = 0; c < numChannels; ++c )
				secondOrderString += "2nd order channel " + c + " = " + secondOrder[ c ] + "\n";

			for ( int c = 0; c < numChannels; ++c )
				thirdOrderString += "3rd order channel " + c + " = " + thirdOrder[ c ] + "\n";

			return "magic number, should be 3555587570 = " + magicNumber + "\n" +
				   "file version = " + fileVersion + "\n" +
				   "file type, 1 is Zeiss Neon detectors = " + fileType + "\n" +
				   "AI sampling time (including oversampling) in seconds = " + timeStep + "\n" +
				   "number of channels = " + numChannels + "\n" +
				   offsetString +
				   gainString +
				   secondOrderString +
				   thirdOrderString +
				   "x resolution = " + xRes + "\n" +
				   "y resolution = " + yRes + "\n" +
				   "oversampling = " + oversampling  + "\n" +
				   "AI delay (# of samples) = " + AIdelay + "\n" +
				   "Scan speed (Zeiss #) = " + zeissScanSpeed + "\n" +
				   "Actual AO (scanning) rate = " + scanRate  + "\n" +
				   "Frameline rampdown ratio = " + framelineRampdownRatio  + "\n" +
				   "X coil minimum voltage = " + xMin + "\n" +
				   "X coil maximum voltage = " + xMax + "\n" +
				   "Detector minimum voltage = " + detMin + "\n" +
				   "Detector maximum voltage = " + detMax + "\n" +
				   "AI Ch1 = " + AI1 + "\n" +
				   "AI Ch2 = " + AI2 + "\n" +
				   "AI Ch3 = " + AI3 + "\n" +
				   "AI Ch4 = " + AI4 + "\n" +
				   "notes = " + notes + "\n" +
				   "detector A = " + detectorA + "\n" +
				   "detector B = " + detectorB + "\n" +
				   "detector C = " + detectorC + "\n" +
				   "detector D = " + detectorD + "\n" +
				   "magnification = " + magnification  + "\n" +
				   "Pixel size in nm = " + pixelSize + "\n" +
				   "Working distance in mm = " + wd + "\n" +
				   "EHT in kV = " + eht + "\n" +
				   "SEM aperture number = " + semApr + "\n" +
				   "high current mode (1=on, 0=off) = " + highCurrent + "\n" +
				   "FIB mode: 0=SEM, 1=FIB, 2=Milling, 3=SEM+FIB, 4=Mill+SEM, 5=SEM Drift Correction, 6=FIB Drift Correction, 7=No Beam, 8=External, 9=External+SEM = " + mode + "\n" +
				   "SEM probe current in A = " + semCurr + "\n" +
				   "SEM scan roation in degree = "+ semRot + "\n"+
				   "Chamber vacuum = "+ chamVac + "\n"+
				   "Gun vacuum = "+ gunVac + "\n"+
				   "SEM beam shift X = " + semShiftX + "\n" +
				   "SEM beam shift Y = " + semShiftY + "\n" +
				   "SEM stigmation X = "+ semStiX + "\n"+
				   "SEM stigmation Y = "+ semStiY + "\n"+
				   "SEM aperture alignment X = "+ semAlnX + "\n"+
				   "SEM aperture alignment Y = "+ semAlnY + "\n"+
				   "Stage position X in mm = "+ stageX + "\n"+
				   "Stage position Y in mm = "+ stageY + "\n"+
				   "Stage position Z in mm = "+ stageZ + "\n"+
				   "Stage position T in degree = "+ stageT + "\n"+
				   "Stage position R in degree = "+ stageR + "\n"+
				   "Stage position M in mm = "+ stageM + "\n"+
				   "Detector A brightness (%) = "+ brightnessA + "\n"+
				   "Detector A contrast (%) = "+ contrastA + "\n"+
				   "Detector B brightness (%) = "+ brightnessB + "\n"+
				   "Detector B contrast (%) = "+ contrastB + "\n"+
				   "FIB focus in kV = "+ fibFocus + "\n"+
				   "FIB probe number = "+ fibProb + "\n"+
				   "FIB emission current = "+ fibCurr + "\n"+
				   "FIB scan rotation = "+ fibRot + "\n"+
				   "FIB aperture alignment X = "+ fibAlnX + "\n"+
				   "FIB aperture alignment Y = "+ fibAlnY + "\n"+
				   "FIB stigmation X = "+ fibStiX + "\n"+
				   "FIB stigmation Y = "+ fibStiY + "\n"+
				   "FIB beam shift X in micron = "+ fibShiftX + "\n"+
				   "FIB beam shift Y in micron = "+ fibShiftY + "\n"+
				   "Machine id = " + machineID  + "\n" +
				   "file length = " + fileLength;
		}
	}

	public static long getUnsignedInt( final int signedInt ) { return signedInt & 0xffffffffL; }
	public static int getUnsignedShort( final short signedShort ) { return signedShort & 0xffff; }
	public static int getUnsignedByte( final byte signedByte ) { return signedByte & 0xff; }

	public static void main( String args[] )
	{
		new ImageJ();

		FIBSEM_Reader.openAsFloat = false;

		FIBSEM_Reader r = new FIBSEM_Reader();
		//r.run( "/Users/preibischs/Desktop/Zeiss_12-02-07_094618.dat" );//"/Users/preibischs/Desktop/Zeiss_12-01-14_210123.dat" );
		r.run( "/Users/preibischs/Desktop/Zeiss_12-01-14_210123.dat" );

		System.out.println( r.getHeader() );
	}
}
/*
function FIBSEMData = readfibsem(FullPathFile)
% Read raw data file (*.dat) generated from Neon
% Needs PathName and FileName
%
% Rev history
% 04/17/09
%   1st rev.
% 07/31/2011
%   converted from script to function
%

%% Load raw data file
fid = fopen(FullPathFile,'r', 's'); % Open the file written by LabView (big-endian byte ordering and 64-bit long data type)

% Start header read
fseek(fid,0,'bof'); FIBSEMData.FileMagicNum = fread(fid,1,'uint32'); % Read in magic number, should be 3555587570
fseek(fid,4,'bof'); FIBSEMData.FileVersion = fread(fid,1,'uint16'); % Read in file version number
fseek(fid,6,'bof'); FIBSEMData.FileType = fread(fid,1,'uint16'); % Read in file type, 1 is Zeiss Neon detectors
fseek(fid,24,'bof'); FIBSEMData.TimeStep = fread(fid,1,'double'); % Read in AI sampling time (including oversampling) in seconds
fseek(fid,32,'bof'); FIBSEMData.ChanNum = fread(fid,1,'uint8'); % Read in number of channels
switch FIBSEMData.FileVersion
  case 1
    fseek(fid,36,'bof'); FIBSEMData.Scaling = single(fread(fid,[4,FIBSEMData.ChanNum],'double')); % Read in AI channel scaling factors, (col#: AI#), (row#: offset, gain, 2nd order, 3rd order)
  otherwise
    fseek(fid,36,'bof'); FIBSEMData.Scaling = fread(fid,[4,FIBSEMData.ChanNum],'single');
end
fseek(fid,100,'bof'); FIBSEMData.XResolution = fread(fid,1,'uint32'); % X resolution
fseek(fid,104,'bof'); FIBSEMData.YResolution = fread(fid,1,'uint32'); % Y resolution
switch FIBSEMData.FileVersion
  case {1,2,3}
    fseek(fid,108,'bof'); FIBSEMData.Oversampling = fread(fid,1,'uint8'); % AI oversampling
    fseek(fid,109,'bof'); FIBSEMData.AIDelay = fread(fid,1,'int16'); % Read AI delay (# of samples)
  otherwise
    fseek(fid,108,'bof'); FIBSEMData.Oversampling = fread(fid,1,'uint16'); % AI oversampling
end
fseek(fid,111,'bof'); FIBSEMData.ZeissScanSpeed = fread(fid,1,'uint8'); % Scan speed (Zeiss #)
switch FIBSEMData.FileVersion
  case {1,2,3}
    fseek(fid,112,'bof'); FIBSEMData.ScanRate = fread(fid,1,'double'); % Actual AO (scanning) rate
    fseek(fid,120,'bof'); FIBSEMData.FramelineRampdownRatio = fread(fid,1,'double'); % Frameline rampdown ratio
    fseek(fid,128,'bof'); FIBSEMData.Xmin = fread(fid,1,'double'); % X coil minimum voltage
    fseek(fid,136,'bof'); FIBSEMData.Xmax = fread(fid,1,'double'); % X coil maximum voltage
    FIBSEMData.Detmin = -10; % Detector minimum voltage
    FIBSEMData.Detmax = 10; % Detector maximum voltage
  otherwise
    fseek(fid,112,'bof'); FIBSEMData.ScanRate = fread(fid,1,'single'); % Actual AO (scanning) rate
    fseek(fid,116,'bof'); FIBSEMData.FramelineRampdownRatio = fread(fid,1,'single'); % Frameline rampdown ratio
    fseek(fid,120,'bof'); FIBSEMData.Xmin = fread(fid,1,'single'); % X coil minimum voltage
    fseek(fid,124,'bof'); FIBSEMData.Xmax = fread(fid,1,'single'); % X coil maximum voltage
    fseek(fid,128,'bof'); FIBSEMData.Detmin = fread(fid,1,'single'); % Detector minimum voltage
    fseek(fid,132,'bof'); FIBSEMData.Detmax = fread(fid,1,'single'); % Detector maximum voltage
end
fseek(fid,151,'bof'); FIBSEMData.AI1 = fread(fid,1,'uint8'); % AI Ch1
fseek(fid,152,'bof'); FIBSEMData.AI2 = fread(fid,1,'uint8'); % AI Ch2
fseek(fid,153,'bof'); FIBSEMData.AI3 = fread(fid,1,'uint8'); % AI Ch3
fseek(fid,154,'bof'); FIBSEMData.AI4 = fread(fid,1,'uint8'); % AI Ch4
fseek(fid,180,'bof'); FIBSEMData.Notes = fread(fid,200,'*char')'; % Read in notes

switch FIBSEMData.FileVersion
  case {1,2}
    fseek(fid,380,'bof'); FIBSEMData.DetA = fread(fid,10,'*char')'; % Name of detector A
    fseek(fid,390,'bof'); FIBSEMData.DetB = fread(fid,18,'*char')'; % Name of detector B
    fseek(fid,700,'bof'); FIBSEMData.DetC = fread(fid,20,'*char')'; % Name of detector C
    fseek(fid,720,'bof'); FIBSEMData.DetD = fread(fid,20,'*char')'; % Name of detector D
    fseek(fid,408,'bof'); FIBSEMData.Mag = fread(fid,1,'double'); % Magnification
    fseek(fid,416,'bof'); FIBSEMData.PixelSize = fread(fid,1,'double'); % Pixel size in nm
    fseek(fid,424,'bof'); FIBSEMData.WD = fread(fid,1,'double'); % Working distance in mm
    fseek(fid,432,'bof'); FIBSEMData.EHT = fread(fid,1,'double'); % EHT in kV
    fseek(fid,440,'bof'); FIBSEMData.SEMApr = fread(fid,1,'uint8'); % SEM aperture number
    fseek(fid,441,'bof'); FIBSEMData.HighCurrent = fread(fid,1,'uint8'); % high current mode (1=on, 0=off)
    fseek(fid,448,'bof'); FIBSEMData.SEMCurr = fread(fid,1,'double'); % SEM probe current in A
    fseek(fid,456,'bof'); FIBSEMData.SEMRot = fread(fid,1,'double'); % SEM scan roation in degree
    fseek(fid,464,'bof'); FIBSEMData.ChamVac = fread(fid,1,'double'); % Chamber vacuum
    fseek(fid,472,'bof'); FIBSEMData.GunVac = fread(fid,1,'double'); % E-gun vacuum
    fseek(fid,480,'bof'); FIBSEMData.SEMStiX = fread(fid,1,'double'); % SEM stigmation X
    fseek(fid,488,'bof'); FIBSEMData.SEMStiY = fread(fid,1,'double'); % SEM stigmation Y
    fseek(fid,496,'bof'); FIBSEMData.SEMAlnX = fread(fid,1,'double'); % SEM aperture alignment X
    fseek(fid,504,'bof'); FIBSEMData.SEMAlnY = fread(fid,1,'double'); % SEM aperture alignment Y
    fseek(fid,512,'bof'); FIBSEMData.StageX = fread(fid,1,'double'); % Stage position X in mm
    fseek(fid,520,'bof'); FIBSEMData.StageY = fread(fid,1,'double'); % Stage position Y in mm
    fseek(fid,528,'bof'); FIBSEMData.StageZ = fread(fid,1,'double'); % Stage position Z in mm
    fseek(fid,536,'bof'); FIBSEMData.StageT = fread(fid,1,'double'); % Stage position T in degree
    fseek(fid,544,'bof'); FIBSEMData.StageR = fread(fid,1,'double'); % Stage position R in degree
    fseek(fid,552,'bof'); FIBSEMData.StageM = fread(fid,1,'double'); % Stage position M in mm
    fseek(fid,560,'bof'); FIBSEMData.BrightnessA = fread(fid,1,'double'); % Detector A brightness (%)
    fseek(fid,568,'bof'); FIBSEMData.ContrastA = fread(fid,1,'double'); % Detector A contrast (%)
    fseek(fid,576,'bof'); FIBSEMData.BrightnessB = fread(fid,1,'double'); % Detector B brightness (%)
    fseek(fid,584,'bof'); FIBSEMData.ContrastB = fread(fid,1,'double'); % Detector B contrast (%)

    fseek(fid,600,'bof'); FIBSEMData.Mode = fread(fid,1,'uint8'); % FIB mode: 0=SEM, 1=FIB, 2=Milling, 3=SEM+FIB, 4=Mill+SEM, 5=SEM Drift Correction, 6=FIB Drift Correction, 7=No Beam, 8=External, 9=External+SEM
    fseek(fid,608,'bof'); FIBSEMData.FIBFocus = fread(fid,1,'double'); % FIB focus in kV
    fseek(fid,616,'bof'); FIBSEMData.FIBProb = fread(fid,1,'uint8'); % FIB probe number
    fseek(fid,624,'bof'); FIBSEMData.FIBCurr = fread(fid,1,'double'); % FIB emission current
    fseek(fid,632,'bof'); FIBSEMData.FIBRot = fread(fid,1,'double'); % FIB scan rotation
    fseek(fid,640,'bof'); FIBSEMData.FIBAlnX = fread(fid,1,'double'); % FIB aperture alignment X
    fseek(fid,648,'bof'); FIBSEMData.FIBAlnY = fread(fid,1,'double'); % FIB aperture alignment Y
    fseek(fid,656,'bof'); FIBSEMData.FIBStiX = fread(fid,1,'double'); % FIB stigmation X
    fseek(fid,664,'bof'); FIBSEMData.FIBStiY = fread(fid,1,'double'); % FIB stigmation Y
    fseek(fid,672,'bof'); FIBSEMData.FIBShiftX = fread(fid,1,'double'); % FIB beam shift X in micron
    fseek(fid,680,'bof'); FIBSEMData.FIBShiftY = fread(fid,1,'double'); % FIB beam shift Y in micron
  otherwise
    fseek(fid,380,'bof'); FIBSEMData.DetA = fread(fid,10,'*char')'; % Name of detector A
    fseek(fid,390,'bof'); FIBSEMData.DetB = fread(fid,18,'*char')'; % Name of detector B
    fseek(fid,410,'bof'); FIBSEMData.DetC = fread(fid,20,'*char')'; % Name of detector C
    fseek(fid,430,'bof'); FIBSEMData.DetD = fread(fid,20,'*char')'; % Name of detector D
    fseek(fid,460,'bof'); FIBSEMData.Mag = fread(fid,1,'single'); % Magnification
    fseek(fid,464,'bof'); FIBSEMData.PixelSize = fread(fid,1,'single'); % Pixel size in nm
    fseek(fid,468,'bof'); FIBSEMData.WD = fread(fid,1,'single'); % Working distance in mm
    fseek(fid,472,'bof'); FIBSEMData.EHT = fread(fid,1,'single'); % EHT in kV
    fseek(fid,480,'bof'); FIBSEMData.SEMApr = fread(fid,1,'uint8'); % SEM aperture number
    fseek(fid,481,'bof'); FIBSEMData.HighCurrent = fread(fid,1,'uint8'); % high current mode (1=on, 0=off)
    fseek(fid,490,'bof'); FIBSEMData.SEMCurr = fread(fid,1,'single'); % SEM probe current in A
    fseek(fid,494,'bof'); FIBSEMData.SEMRot = fread(fid,1,'single'); % SEM scan roation in degree
    fseek(fid,498,'bof'); FIBSEMData.ChamVac = fread(fid,1,'single'); % Chamber vacuum
    fseek(fid,502,'bof'); FIBSEMData.GunVac = fread(fid,1,'single'); % E-gun vacuum
    fseek(fid,510,'bof'); FIBSEMData.SEMShiftX = fread(fid,1,'single'); % SEM beam shift X
    fseek(fid,514,'bof'); FIBSEMData.SEMShiftY = fread(fid,1,'single'); % SEM beam shift Y
    fseek(fid,518,'bof'); FIBSEMData.SEMStiX = fread(fid,1,'single'); % SEM stigmation X
    fseek(fid,522,'bof'); FIBSEMData.SEMStiY = fread(fid,1,'single'); % SEM stigmation Y
    fseek(fid,526,'bof'); FIBSEMData.SEMAlnX = fread(fid,1,'single'); % SEM aperture alignment X
    fseek(fid,530,'bof'); FIBSEMData.SEMAlnY = fread(fid,1,'single'); % SEM aperture alignment Y
    fseek(fid,534,'bof'); FIBSEMData.StageX = fread(fid,1,'single'); % Stage position X in mm
    fseek(fid,538,'bof'); FIBSEMData.StageY = fread(fid,1,'single'); % Stage position Y in mm
    fseek(fid,542,'bof'); FIBSEMData.StageZ = fread(fid,1,'single'); % Stage position Z in mm
    fseek(fid,546,'bof'); FIBSEMData.StageT = fread(fid,1,'single'); % Stage position T in degree
    fseek(fid,550,'bof'); FIBSEMData.StageR = fread(fid,1,'single'); % Stage position R in degree
    fseek(fid,554,'bof'); FIBSEMData.StageM = fread(fid,1,'single'); % Stage position M in mm
    fseek(fid,560,'bof'); FIBSEMData.BrightnessA = fread(fid,1,'single'); % Detector A brightness (%)
    fseek(fid,564,'bof'); FIBSEMData.ContrastA = fread(fid,1,'single'); % Detector A contrast (%)
    fseek(fid,568,'bof'); FIBSEMData.BrightnessB = fread(fid,1,'single'); % Detector B brightness (%)
    fseek(fid,572,'bof'); FIBSEMData.ContrastB = fread(fid,1,'single'); % Detector B contrast (%)

    fseek(fid,600,'bof'); FIBSEMData.Mode = fread(fid,1,'uint8'); % FIB mode: 0=SEM, 1=FIB, 2=Milling, 3=SEM+FIB, 4=Mill+SEM, 5=SEM Drift Correction, 6=FIB Drift Correction, 7=No Beam, 8=External, 9=External+SEM
    fseek(fid,604,'bof'); FIBSEMData.FIBFocus = fread(fid,1,'single'); % FIB focus in kV
    fseek(fid,608,'bof'); FIBSEMData.FIBProb = fread(fid,1,'uint8'); % FIB probe number
    fseek(fid,620,'bof'); FIBSEMData.FIBCurr = fread(fid,1,'single'); % FIB emission current
    fseek(fid,624,'bof'); FIBSEMData.FIBRot = fread(fid,1,'single'); % FIB scan rotation
    fseek(fid,628,'bof'); FIBSEMData.FIBAlnX = fread(fid,1,'single'); % FIB aperture alignment X
    fseek(fid,632,'bof'); FIBSEMData.FIBAlnY = fread(fid,1,'single'); % FIB aperture alignment Y
    fseek(fid,636,'bof'); FIBSEMData.FIBStiX = fread(fid,1,'single'); % FIB stigmation X
    fseek(fid,640,'bof'); FIBSEMData.FIBStiY = fread(fid,1,'single'); % FIB stigmation Y
    fseek(fid,644,'bof'); FIBSEMData.FIBShiftX = fread(fid,1,'single'); % FIB beam shift X in micron
    fseek(fid,648,'bof'); FIBSEMData.FIBShiftY = fread(fid,1,'single'); % FIB beam shift Y in micron
end
fseek(fid,800,'bof'); FIBSEMData.MachineID = fread(fid,160,'*char')'; % Read in Machine ID
fseek(fid,1000,'bof'); FIBSEMData.FileLength = fread(fid,1,'int64'); % Read in file length in bytes
% Finish header read

fseek(fid,1024,'bof'); Raw = (fread(fid,[FIBSEMData.ChanNum,inf],'*int16'))'; % Read in raw AI the "*" is needed to read long set of data

fclose(fid); % Close the file

%% Convert raw data to detector voltage
if FIBSEMData.AI1
  DetectorA = FIBSEMData.Scaling(1,1)+single(Raw(:,1))*FIBSEMData.Scaling(2,1); % Converts raw I16 data to voltage based on scaling factors
  if FIBSEMData.AI2
    DetectorB = FIBSEMData.Scaling(1,2)+single(Raw(:,2))*FIBSEMData.Scaling(2,2); % Converts raw I16 data to voltage based on scaling factors
    if FIBSEMData.AI3
      DetectorC = FIBSEMData.Scaling(1,3)+single(Raw(:,3))*FIBSEMData.Scaling(2,3);
      if FIBSEMData.AI4
        DetectorD = FIBSEMData.Scaling(1,4)+single(Raw(:,4))*FIBSEMData.Scaling(2,4);
      end
    elseif FIBSEMData.AI4
      DetectorD = FIBSEMData.Scaling(1,3)+single(Raw(:,3))*FIBSEMData.Scaling(2,3);
    end
  elseif FIBSEMData.AI3
    DetectorC = FIBSEMData.Scaling(1,2)+single(Raw(:,2))*FIBSEMData.Scaling(2,2);
    if FIBSEMData.AI4
      DetectorD = FIBSEMData.Scaling(1,3)+single(Raw(:,3))*FIBSEMData.Scaling(2,3);
    end
  elseif FIBSEMData.AI4
    DetectorD = FIBSEMData.Scaling(1,2)+single(Raw(:,2))*FIBSEMData.Scaling(2,2);
  end
elseif FIBSEMData.AI2
  DetectorB = FIBSEMData.Scaling(1,1)+single(Raw(:,1))*FIBSEMData.Scaling(2,1);
  if FIBSEMData.AI3
    DetectorC = FIBSEMData.Scaling(1,2)+single(Raw(:,2))*FIBSEMData.Scaling(2,2);
    if FIBSEMData.AI4
      DetectorD = FIBSEMData.Scaling(1,3)+single(Raw(:,3))*FIBSEMData.Scaling(2,3);
    end
  elseif FIBSEMData.AI4
    DetectorD = FIBSEMData.Scaling(1,2)+single(Raw(:,2))*FIBSEMData.Scaling(2,2);
  end
elseif FIBSEMData.AI3
  DetectorC = FIBSEMData.Scaling(1,1)+single(Raw(:,1))*FIBSEMData.Scaling(2,1);
  if FIBSEMData.AI4
    DetectorD = FIBSEMData.Scaling(1,2)+single(Raw(:,2))*FIBSEMData.Scaling(2,2);
  end
elseif FIBSEMData.AI4
  DetectorD = FIBSEMData.Scaling(1,1)+single(Raw(:,1))*FIBSEMData.Scaling(2,1);
end
%% Construct image files
if FIBSEMData.AI1
  FIBSEMData.ImageA = (reshape(DetectorA,FIBSEMData.XResolution,FIBSEMData.YResolution))';
end
if FIBSEMData.AI2
  FIBSEMData.ImageB = (reshape(DetectorB,FIBSEMData.XResolution,FIBSEMData.YResolution))';
end
if FIBSEMData.AI3
  FIBSEMData.ImageC = (reshape(DetectorC,FIBSEMData.XResolution,FIBSEMData.YResolution))';
end
if FIBSEMData.AI4
  FIBSEMData.ImageD = (reshape(DetectorD,FIBSEMData.XResolution,FIBSEMData.YResolution))';
end

*/