package fiji.plugin.timelapsedisplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.util.Util;
import mpicbg.models.AffineModel3D;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class TimeLapseDisplay
{

	public static void main( String args[] )
	{
		/*
		SPIMConfiguration config = null;

		try
		{
			String temp;

			if ( args == null || args.length == 0 || args[0].trim().length() < 1 )
				temp = "spimconfig/configuration.txt";
			else
				temp = args[0].trim();

			config = ConfigurationParserSPIM.parseFile( temp );

			if ( config.debugLevelInt <= ViewStructure.DEBUG_ALL )
				config.printProperties();
		}
		catch ( ConfigurationParserException e )
		{
			IOFunctions.println( "Cannot open SPIM configuration file: \n" + e );
			System.exit(0);
		}

		if ( config.showImageJWindow )
		{
			// read&parse configuration file
			ProgramConfiguration conf = null;

			try
			{
				conf = ConfigurationParserGeneral.parseFile( "config/configuration.txt" );
			}
			catch (final Exception e)
			{
				IOFunctions.println( "Cannot open configuration file: \n" + e );
			}

			// open imageJ window
			if ( conf != null )
			{
				System.getProperties().setProperty( "plugins.dir", conf.pluginsDir );
				final String params[] = { "-ijpath " + conf.pluginsDir };

				// call the imageJ main class
				ij.ImageJ.main( params );
			}
			else
			{
				final String params[] = { "-ijpath ." };

				// call the imageJ main class
				ij.ImageJ.main( params );
			}
		}
		*/
		// load registrations, dims, ...

		//ArrayList< TimepointData > data = loadData( config );
		ArrayList< RegistrationStatistics > data = defaultData();

		//new ImageJ();
		final ArrayList<FileOpenMenuEntry> items = new ArrayList<FileOpenMenuEntry>();
		
		items.add( new FileOpenMenuEntry( "Display image stack", data ) );
		
		plotData( data, getOptimalTimePoint( data ), true, items );
	}
	
	public static void plotData( final ArrayList< RegistrationStatistics > data )
	{
		plotData( data, -1, false, null );
	}

	public static GraphFrame plotData( final ArrayList< RegistrationStatistics > data, int referenceTimePoint, boolean enableReferenceTimePoint )
	{
		return plotData( data, referenceTimePoint, enableReferenceTimePoint, null );
	}

	public static GraphFrame plotData( final ArrayList< RegistrationStatistics > data, int referenceTimePoint, boolean enableReferenceTimePoint, final List<FileOpenMenuEntry> extraMenuItems )
	{
        Color errorColorMin = new Color(240, 50, 50);
        Color errorColorAvg = new Color(255, 0, 0);
        Color errorColorMax = errorColorMin;

        Color ratioColorMin = new Color( 50, 50, 240 );
        Color ratioColorAvg = new Color( 0, 0, 255 );
        Color ratioColorMax = ratioColorMin;

		XYSeries seriesMinError = new XYSeries("minError");
		XYSeries seriesAvgError = new XYSeries("avgError");
		XYSeries seriesMaxError = new XYSeries("maxError");

		for ( RegistrationStatistics tp : data ) {
			seriesMinError.add( tp.timePoint, tp.minError );
			seriesAvgError.add( tp.timePoint, tp.avgError );
			seriesMaxError.add( tp.timePoint, tp.maxError );
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries( seriesMinError );
		dataset.addSeries( seriesAvgError );
		dataset.addSeries( seriesMaxError );

		XYSeries seriesMinRatio = new XYSeries("minRatio");
		XYSeries seriesAvgRatio = new XYSeries("avgRatio");
		XYSeries seriesMaxRatio = new XYSeries("maxRatio");

		for ( RegistrationStatistics tp : data ) {
			seriesMinRatio.add( tp.timePoint, tp.minRatio*100 );
			seriesAvgRatio.add( tp.timePoint, tp.avgRatio*100 );
			seriesMaxRatio.add( tp.timePoint, tp.maxRatio*100 );
		}

		XYSeriesCollection dataset2 = new XYSeriesCollection();
		dataset2.addSeries( seriesMinRatio );
		dataset2.addSeries( seriesAvgRatio );
		dataset2.addSeries( seriesMaxRatio );

		JFreeChart chart = ChartFactory.createXYLineChart
							( "Registration Quality",  // Title
		                      "Timepoint",             // X-Axis label
		                      "Error [px]",                 // Y-Axis label
		                      dataset,
		                      PlotOrientation.VERTICAL,
		                      true,                    // Show legend
		                      false,				   // show tooltips
		                      false
		                     );
        final XYPlot plot = chart.getXYPlot();
        final NumberAxis axis2 = new NumberAxis( "Correspondence Ratio  [%]" );
        plot.getRangeAxis( 0 ).setLabelPaint( errorColorAvg );
        axis2.setLabelPaint( ratioColorAvg );
        axis2.setLabelFont( plot.getRangeAxis( 0 ).getLabelFont() );
        axis2.setRange( 0.0, 100 );
        plot.setRangeAxis( 1, axis2 );
        plot.setDataset( 1, dataset2);
        plot.mapDatasetToRangeAxis( 1, 1 );
        final XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint( 0, errorColorMin );
        renderer.setSeriesStroke( 0, new BasicStroke ( 0.5f ) );
        renderer.setSeriesPaint( 1, errorColorAvg );
        renderer.setSeriesStroke( 1, new BasicStroke ( 1.5f ) );
        renderer.setSeriesPaint( 2, errorColorMax );
        renderer.setSeriesStroke( 2, new BasicStroke ( 0.5f ) );

        final StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
        renderer2.setSeriesPaint( 0, ratioColorMin );
        renderer2.setSeriesStroke( 0, new BasicStroke ( 0.5f ) );
        renderer2.setSeriesPaint( 1, ratioColorAvg );
        renderer2.setSeriesStroke( 1, new BasicStroke ( 1.5f ) );
        renderer2.setSeriesPaint( 2, ratioColorMax );
        renderer2.setSeriesStroke( 2, new BasicStroke ( 0.5f ) );
        renderer2.setPlotImages( true );
        plot.setRenderer( 1, renderer2 );

        // Is it somehow possible to add a new tab to this Properties menu item?        
		GraphFrame graphFrame = new GraphFrame( chart, referenceTimePoint, enableReferenceTimePoint, extraMenuItems, data );
		
		Dimension d = new Dimension( 800, 400 );	
		graphFrame.setSize( d );

		// resizing fucks up the interaction
		graphFrame.setResizable( false );
		
		graphFrame.setVisible(true);
		
		return graphFrame;//.getReferenceTimePoint();
	}
	
	/**
	 * Select a timepoint with minimal maxError and maximal minRatio
	 * 
	 * @param data - the timepoint data
	 * @return - the index for the {@link ArrayList}
	 */
	public static int getOptimalTimePoint( final ArrayList< RegistrationStatistics > data )
	{
		// first sort the values together with links to the indices
		final double[] maxErrors = new double[ data.size() ];
		final double[] minRatioInv = new double[ data.size() ];
		final int[] indicesMaxError = new int[ data.size() ];
		final int[] indicesMinRatio = new int[ data.size() ];
				
		for ( int i = 0; i < data.size(); ++i )
		{
			maxErrors[ i ] = data.get( i ).maxError;
			indicesMaxError[ i ] = i;
			
			// in this way they are sorted the same
			minRatioInv[ i ] = 10 - data.get( i ).minRatio;
			indicesMinRatio[ i ] = i;
		}
		
		Util.quicksort( maxErrors, indicesMaxError, 0, data.size() - 1 );
		Util.quicksort( minRatioInv, indicesMinRatio, 0, data.size() - 1 );
		
		// now for each of both sum up the rank in the array, the index with the lowest rank will be the suggestion
		final double[] sumRank = new double[ data.size() ];
		final int[] indicesSumRank = new int[ data.size() ];
		
		for ( int i = 0; i < data.size(); ++i )
		{
			sumRank[ indicesMaxError[ i ] ] += i; 
			sumRank[ indicesMinRatio[ i ] ] += i;
			indicesSumRank[ i ] = i;
		}

		// now we sort the ranks and select the best
		Util.quicksort( sumRank, indicesSumRank, 0, data.size() - 1 );

		// the best timepoint is the one with the highest rank
		return data.get( indicesSumRank[ 0 ] ).getTimePoint();
	}
	
	public static ArrayList< RegistrationStatistics > loadData( SPIMConfiguration conf )
	{
		ArrayList< RegistrationStatistics > data = new ArrayList< RegistrationStatistics >();

		for (int timePointIndex = 0; timePointIndex < conf.file.length; timePointIndex++)
		{
			final ViewStructure timepoint = ViewStructure.initViewStructure( conf, timePointIndex, new AffineModel3D(), "ViewStructure Timepoint " + conf.timepoints[timePointIndex], ViewStructure.DEBUG_ERRORONLY );

			timepoint.loadDimensions();
			timepoint.loadSegmentations();
			timepoint.loadRegistrations();

			//System.out.println( timepoint );
			double minError = timepoint.getGlobalErrorStatistics().getMinAlignmentError();
			double avgError = timepoint.getGlobalErrorStatistics().getAverageAlignmentError();
			double maxError = timepoint.getGlobalErrorStatistics().getMaxAlignmentError();

			double minRatio = 1;
			double maxRatio = 0;
			double avgRatio = 0;
			int numViews = 0;
			ViewDataBeads worstView = null;

			for ( final ViewDataBeads view : timepoint.getViews() )
			{
				if ( view.getUseForRegistration() )
				{
					final ArrayList<Bead> beadList = view.getBeadStructure().getBeadList();

					int candidates = 0;
					int correspondences = 0;

					for ( final Bead bead : beadList )
					{
						candidates += bead.getDescriptorCorrespondence().size();
						correspondences += bead.getRANSACCorrespondence().size();
					}

					if ( candidates > 0 )
					{
						final double ratio = (double)correspondences / (double)candidates;

						if ( ratio < minRatio )
						{
							minRatio = ratio;
							worstView = view;
						}

						if ( ratio > maxRatio )
							maxRatio = ratio;

						avgRatio += ratio;
					}
					else
					{
						minRatio = 0;
					}
					numViews++;
				}
			}

			avgRatio /= numViews;

			System.out.println("data.add( new TimepointData( "+timepoint.getTimePoint()+", "+minError+", "+avgError+", "+maxError+", "+minRatio+", "+avgRatio+", "+maxRatio+" ) );");
			data.add( new RegistrationStatistics( timepoint.getTimePoint(), minError, avgError, maxError, minRatio, avgRatio, maxRatio, new File( worstView.getFileName() ) ) );

			//ImageJFunctions.show( worstView.getImage() );
		}

		return data;
	}

	public static ArrayList< RegistrationStatistics > defaultData()
	{
		ArrayList< RegistrationStatistics > data = new ArrayList< RegistrationStatistics >();
		int t = 0;
		
		data.add( new RegistrationStatistics( 0, 1.5524958372116089, 1.806956688563029, 2.268580198287964, 0.9751693002257337, 0.9819754143698312, 0.9853658536585366 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 1, 1.4457428455352783, 1.7010159492492676, 2.2213871479034424, 0.9500891265597148, 0.9682656927994552, 0.9824505424377792 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 2, 1.5695387125015259, 1.7638821999231975, 2.234736442565918, 0.9596100278551533, 0.9717308561793622, 0.9787632729544035 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 3, 1.6117498874664307, 1.840670386950175, 2.2731757164001465, 0.971449758991472, 0.9773524748499655, 0.9824561403508771 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 4, 1.5867455005645752, 1.827444076538086, 2.3411481380462646, 0.9624012638230648, 0.9727959366299341, 0.9783328317785134 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 5, 1.651970386505127, 1.886688768863678, 2.3361032009124756, 0.9735649546827795, 0.9792610772478447, 0.983587786259542 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 6, 1.629520297050476, 1.8914413849512737, 2.432326316833496, 0.9734063355494721, 0.9788148643151829, 0.9813049853372434 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 7, 1.6326627731323242, 1.8764132062594097, 2.3397233486175537, 0.9720566318926974, 0.9773193925214058, 0.9842164599774521 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 8, 1.5867546796798706, 1.808415710926056, 2.1658318042755127, 0.9651647103369936, 0.9729788796693929, 0.9774741506646972 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 9, 1.514437198638916, 1.6355570952097576, 1.8275426626205444, 0.9705304518664047, 0.9748074787958393, 0.9805892547660312 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 10, 1.518883466720581, 1.6631513436635335, 1.8628147840499878, 0.9689866570501262, 0.9740114442225017, 0.9782456140350877 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 11, 1.4911465644836426, 1.6402262250582378, 1.8275282382965088, 0.968413496051687, 0.9763122116272882, 0.9822253821542837 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 12, 1.514184832572937, 1.6533780097961426, 1.851939082145691, 0.9701275045537341, 0.9745128582955255, 0.9829028737722808 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 13, 1.5256481170654297, 1.661082148551941, 1.8332107067108154, 0.9661080074487896, 0.9752132896404819, 0.980840543881335 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 14, 1.5242377519607544, 1.6752323309580486, 1.8725628852844238, 0.9665977961432507, 0.974406238563704, 0.9835495096488454 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 15, 1.534326195716858, 1.6659722924232483, 1.8329306840896606, 0.9677075940383251, 0.9754331277097882, 0.9819587628865979 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 16, 1.5190484523773193, 1.6676682233810425, 1.8493690490722656, 0.9696869851729819, 0.9765270178243082, 0.9830747531734838 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 17, 1.6115732192993164, 1.8294549385706584, 2.107720136642456, 0.9677298311444653, 0.9781560832813886, 0.9848812095032398 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 18, 1.620741844177246, 1.8333700895309448, 2.114537477493286, 0.9697318007662835, 0.9760511054879251, 0.9805685498380713 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 19, 1.6114051342010498, 1.8187030951182048, 2.101839542388916, 0.9679756004574914, 0.9742571046566114, 0.9780673871582962 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 20, 1.5609114170074463, 1.6749335130055745, 1.8528552055358887, 0.9700775766531216, 0.9764834671636775, 0.9811046511627907 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 21, 1.5582594871520996, 1.6768479148546855, 1.872881531715393, 0.9627130681818182, 0.9730970793195156, 0.9787011173184358 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 22, 1.547990083694458, 1.6681870222091675, 1.8411319255828857, 0.9653114509511377, 0.974769479696338, 0.9836897426603842 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 23, 1.555517315864563, 1.708265523115794, 1.9268492460250854, 0.9664222307989193, 0.9699493548569768, 0.9783929777177582 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 24, 1.5546066761016846, 1.6604079206784566, 1.8108513355255127, 0.9639468690702088, 0.9691565667886387, 0.975601714474118 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 25, 1.5520716905593872, 1.6782016158103943, 1.8644237518310547, 0.9660290237467019, 0.9716072525273557, 0.9793947198969736 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 26, 1.542048454284668, 1.6761364539464314, 1.855847716331482, 0.9724702380952381, 0.973910792415993, 0.9761646803900325 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 27, 1.5720570087432861, 1.688110113143921, 1.861188530921936, 0.9702934860415175, 0.9736773546590635, 0.9771039603960396 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 28, 1.5528913736343384, 1.690041760603587, 1.8497505187988281, 0.9638467100506146, 0.9738529265369946, 0.9802747446283903 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 29, 1.596397042274475, 1.720573325951894, 1.9573100805282593, 0.9611727416798732, 0.9689755226338458, 0.975225979243388 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 30, 1.5479422807693481, 1.6795976161956787, 1.8378007411956787, 0.9701720841300191, 0.9729352768488986, 0.9764532744665195 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 31, 1.542989730834961, 1.6805902123451233, 1.8393124341964722, 0.9640207715133531, 0.9704835211615954, 0.976375046142488 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 32, 1.567991852760315, 1.7222594618797302, 1.941042184829712, 0.9629773967264225, 0.9711092184882301, 0.9759392148585901 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 33, 1.5617954730987549, 1.7037303646405537, 1.925266981124878, 0.956408345752608, 0.9670798671120305, 0.9714393085306275 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 34, 1.5661051273345947, 1.6953144868214924, 1.8977755308151245, 0.9581271412257327, 0.9695520990685793, 0.974106491611962 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 35, 1.5512316226959229, 1.7010654211044312, 1.9034820795059204, 0.9653774173424828, 0.9678353281583681, 0.9704893537542024 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 36, 1.5522935390472412, 1.715204397837321, 1.9078375101089478, 0.9615099925980755, 0.9708115442260744, 0.9750452079566003 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 37, 1.5696918964385986, 1.736938198407491, 1.9722877740859985, 0.966198419666374, 0.9702938064883667, 0.9727126805778491 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 38, 1.602862000465393, 1.814005156358083, 2.111128091812134, 0.9650213758258842, 0.9695294928119891, 0.9742075823492853 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 39, 1.6148203611373901, 1.8462434609731038, 2.171909809112549, 0.9576144036009002, 0.9671420245398333, 0.9721390053924506 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 40, 1.6335563659667969, 1.8592471480369568, 2.1524922847747803, 0.9657469077069457, 0.9693711416199066, 0.974247595408005 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 41, 1.6082319021224976, 1.839365820089976, 2.1301896572113037, 0.9621013133208255, 0.9701897983843207, 0.9740121039515842 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 42, 1.6240485906600952, 1.8474337458610535, 2.136050224304199, 0.9600307455803229, 0.9704532148539773, 0.975234521575985 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 43, 1.6398121118545532, 1.849562168121338, 2.1263303756713867, 0.9641768292682927, 0.9709188803378748, 0.9768243031631695 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 44, 1.652018666267395, 1.8670400778452556, 2.172206401824951, 0.9566563467492261, 0.970048599912955, 0.9745466115176583 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 45, 1.6802598237991333, 1.8945400317509968, 2.1701018810272217, 0.9586840091813313, 0.9678541212342423, 0.9712460063897763 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 46, 1.6323862075805664, 1.8492120504379272, 2.151214361190796, 0.9607142857142857, 0.9707672607072446, 0.9768822243049047 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 47, 1.6310921907424927, 1.851080060005188, 2.101172924041748, 0.9601301871440195, 0.970036507031419, 0.9753320683111955 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 48, 1.6221837997436523, 1.8516292969385784, 2.1341469287872314, 0.9599358974358975, 0.970576225977457, 0.9771021021021021 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 49, 1.6522371768951416, 1.8666374683380127, 2.1568238735198975, 0.9679529103989536, 0.9718610587687704, 0.9772151898734177 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 50, 1.6307493448257446, 1.7848721941312153, 1.9938324689865112, 0.9615384615384616, 0.9702151657313824, 0.9735099337748344 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 51, 1.6329890489578247, 1.8624405066172283, 2.1313486099243164, 0.9595761381475667, 0.9696071992416323, 0.9741602067183462 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 52, 1.6418877840042114, 1.7994750340779622, 2.0034234523773193, 0.9545638945233266, 0.9637883801091572, 0.9670809539805173 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 53, 1.6689380407333374, 1.7795648376146953, 1.9736169576644897, 0.9609467455621302, 0.967350363616823, 0.9740728585493929 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 54, 1.5824886560440063, 1.7283894022305806, 1.868834376335144, 0.9654042243262928, 0.9704131703544219, 0.9727934198038596 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 55, 1.5904712677001953, 1.7551137208938599, 1.9055973291397095, 0.9617809298660362, 0.9705623483687734, 0.975783015821763 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 56, 1.5938656330108643, 1.7424479126930237, 1.8878238201141357, 0.966320428076802, 0.9705460093130721, 0.9731102479832686 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 57, 1.595832347869873, 1.7506959040959675, 1.94493567943573, 0.965472312703583, 0.9708662819719479, 0.9755784061696658 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 58, 1.672014832496643, 1.8709512154261272, 2.1232831478118896, 0.953184957789716, 0.9650423480773925, 0.9698275862068966 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 59, 1.5999242067337036, 1.7399375438690186, 1.8467296361923218, 0.9596033786265149, 0.9705922539006681, 0.9777549623545517 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 60, 1.5819746255874634, 1.7170563340187073, 1.8199642896652222, 0.9609527528309254, 0.9693425403864747, 0.9786254521538967 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 61, 1.5872857570648193, 1.7444188594818115, 1.8641180992126465, 0.9643660915228808, 0.9716094152312188, 0.9741238126433017 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 62, 1.6132761240005493, 1.737346927324931, 1.8830829858779907, 0.9655439411536972, 0.9718385772780721, 0.9766167336499817 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 63, 1.580646276473999, 1.7191651264826457, 1.8716703653335571, 0.9632297194844579, 0.9708040720295967, 0.9760024301336574 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 64, 1.6514935493469238, 1.7906444867451985, 1.9639458656311035, 0.96832, 0.9712959500686776, 0.9737881508078995 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 65, 1.5704454183578491, 1.707777778307597, 1.8911854028701782, 0.95695618754804, 0.9685547175786584, 0.9770226537216828 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 66, 1.6405329704284668, 1.7527039249738057, 1.917934536933899, 0.9642289348171701, 0.9685638121689024, 0.9741912031988368 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 67, 1.6430104970932007, 1.759121835231781, 1.9452135562896729, 0.9640138408304498, 0.9707603885923201, 0.9757733879985091 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 68, 1.5918322801589966, 1.7173205018043518, 1.7899507284164429, 0.9582841401023219, 0.9655909187879607, 0.9716704656463693 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 69, 1.6390761137008667, 1.781669060389201, 1.9955137968063354, 0.9648400380107697, 0.9700799835357943, 0.9770671834625323 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 70, 1.6015228033065796, 1.7476178805033367, 1.942074179649353, 0.965, 0.9714126159621942, 0.98 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 71, 1.634105920791626, 1.7552514274915059, 1.917275309562683, 0.9635562675210252, 0.9671475715547858, 0.9729553600521342 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 72, 1.5365360975265503, 1.670231540997823, 1.7544198036193848, 0.95267892061009, 0.9660648650491476, 0.9735532047293093 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 73, 1.6014795303344727, 1.7344619035720825, 1.9356279373168945, 0.9590038314176246, 0.9670374086795753, 0.9733284618195104 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 74, 1.5946496725082397, 1.7317408720652263, 1.8848766088485718, 0.9604250641260536, 0.9679625159229642, 0.9717971797179717 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 75, 1.6457058191299438, 1.8571737011273701, 2.120846748352051, 0.9555555555555556, 0.9678837605823579, 0.9748427672955975 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 76, 1.5898629426956177, 1.7139034271240234, 1.8737297058105469, 0.9626022594468251, 0.9709977883447817, 0.9754043126684636 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 77, 1.555689811706543, 1.668826699256897, 1.7672628164291382, 0.959192439862543, 0.9692190985987629, 0.9741998693664272 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 78, 1.601244330406189, 1.7187205950419109, 1.866756796836853, 0.9628647214854111, 0.9681709197884231, 0.9740714786264891 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 79, 1.5450093746185303, 1.6617095271746318, 1.7271759510040283, 0.9556669236700077, 0.9654251794181365, 0.9694258016405667 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 80, 1.5704091787338257, 1.684806187947591, 1.8463698625564575, 0.958706661472536, 0.9673565214664284, 0.9716911764705882 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 81, 1.5500454902648926, 1.6780269940694172, 1.8349429368972778, 0.9657250470809793, 0.9715062455528062, 0.976491862567812 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 82, 1.5674132108688354, 1.6787593364715576, 1.8557803630828857, 0.9639705882352941, 0.9725761783094992, 0.9786856127886323 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 83, 1.5270506143569946, 1.6316694418589275, 1.6895743608474731, 0.9592906086255543, 0.9638893599831818, 0.9689343295320487 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 84, 1.4844719171524048, 1.6160747011502583, 1.7201961278915405, 0.959983498349835, 0.967585666928157, 0.9783449342614076 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 85, 1.550892949104309, 1.628505011399587, 1.7180737257003784, 0.9567876070118223, 0.9695270192062876, 0.9747278382581649 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 86, 1.50522780418396, 1.6307544310887654, 1.727838397026062, 0.9606361829025845, 0.9662167938662723, 0.9751259445843828 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 87, 1.5282948017120361, 1.64990895986557, 1.809705376625061, 0.9623717217787914, 0.9697370935961578, 0.975624619134674 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 88, 1.504470705986023, 1.6278573671976726, 1.7410410642623901, 0.9636363636363636, 0.9702563760365406, 0.9790209790209791 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 89, 1.5573090314865112, 1.6481094360351562, 1.81259024143219, 0.9636925453843183, 0.9717661391787011, 0.978810408921933 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 90, 1.4968587160110474, 1.6212068597475688, 1.6951786279678345, 0.9613083366573594, 0.9676607658359302, 0.9725291131681099 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 91, 1.523604154586792, 1.6559208830197651, 1.8464540243148804, 0.96280834914611, 0.9712360091058247, 0.9766782225023637 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 92, 1.493512749671936, 1.6153903206189473, 1.6973741054534912, 0.9682220434432823, 0.971428622891993, 0.9764344262295082 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 93, 1.5358227491378784, 1.6551886200904846, 1.8613364696502686, 0.9642721398707715, 0.9713728156411118, 0.9750283768444948 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 94, 1.5022156238555908, 1.637016475200653, 1.7593426704406738, 0.9697933227344993, 0.9745444131593666, 0.9780534351145038 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 95, 1.5107723474502563, 1.6318629781405132, 1.7256920337677002, 0.9618415595188718, 0.9732442230785145, 0.9792 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 96, 1.571242094039917, 1.6781799991925557, 1.8696825504302979, 0.9632117722328855, 0.9689412043868998, 0.9735384615384616 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 97, 1.4929817914962769, 1.618196149667104, 1.694556713104248, 0.960691823899371, 0.9697311961062542, 0.9733420026007802 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 98, 1.5200825929641724, 1.631967802842458, 1.747897982597351, 0.9609467455621302, 0.9678103697483552, 0.972203838517538 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 99, 1.5578786134719849, 1.664229412873586, 1.850327730178833, 0.9659706109822119, 0.9723975352274655, 0.975742100223428 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 100, 1.572194218635559, 1.6942598422368367, 1.9007418155670166, 0.9616766467065868, 0.9699192426555957, 0.9746497665110073 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 101, 1.5185768604278564, 1.648180067539215, 1.735249638557434, 0.9524931291715744, 0.9636206576022764, 0.9673871824042473 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 102, 1.556459903717041, 1.6600275039672852, 1.8457504510879517, 0.9585432003099574, 0.9669542275425623, 0.9743213499633162 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 103, 1.5520274639129639, 1.6759024659792583, 1.8799974918365479, 0.9685746352413019, 0.973519114223666, 0.9759659263766353 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 104, 1.5671838521957397, 1.6753477255503337, 1.830601453781128, 0.9595443833464258, 0.968819679888132, 0.9741863075196409 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 105, 1.5723133087158203, 1.6825612783432007, 1.8763232231140137, 0.9593625498007968, 0.9687249415025486, 0.9750755287009063 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 106, 1.5894625186920166, 1.690873960653941, 1.877936601638794, 0.9606569900687548, 0.9664073259689733, 0.9741818181818181 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 107, 1.5939608812332153, 1.6955344478289287, 1.89657461643219, 0.9643687064291248, 0.9695228071693922, 0.9721733004579077 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 108, 1.5865941047668457, 1.6863028804461162, 1.8687597513198853, 0.9620808523973676, 0.9686795114671255, 0.9769917582417582 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 109, 1.591106653213501, 1.6839884718259175, 1.8651673793792725, 0.9607541362062332, 0.9721237991328145, 0.9793178519593614 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 110, 1.5838327407836914, 1.6851284702618916, 1.8754159212112427, 0.964327258627375, 0.9725387229064769, 0.9769820971867008 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 111, 1.5714569091796875, 1.6557644406954448, 1.6995630264282227, 0.9615232050773502, 0.9674978367857046, 0.9741051028179741 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 112, 1.5489201545715332, 1.684557358423869, 1.901409387588501, 0.9629233511586452, 0.9686016589010723, 0.972937293729373 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 113, 1.5298823118209839, 1.6275341709454854, 1.699489951133728, 0.9618013671089666, 0.9693778077478324, 0.975016655562958 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 114, 1.6135131120681763, 1.718704601128896, 1.9389467239379883, 0.9597511891694109, 0.9644183576914754, 0.9736255572065379 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 115, 1.5904518365859985, 1.7050456603368123, 1.9179463386535645, 0.9594405594405594, 0.964180662989861, 0.9696132596685083 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 116, 1.5087720155715942, 1.6394520203272502, 1.7266491651535034, 0.9616346955796498, 0.9664459596135302, 0.9716828478964401 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 117, 1.5007072687149048, 1.6382944981257122, 1.75978422164917, 0.962322183775471, 0.9691174806113209, 0.9760986892829607 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 118, 1.5880134105682373, 1.694528043270111, 1.9210580587387085, 0.9617241379310345, 0.9650691352150434, 0.968113714944295 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 119, 1.527837872505188, 1.6511491735776265, 1.755268931388855, 0.9573796369376479, 0.9668967247421717, 0.9726735598227474 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 120, 1.5994281768798828, 1.6971482634544373, 1.8568546772003174, 0.960546875, 0.9656871238761346, 0.9733234531307892 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 121, 1.583714246749878, 1.6896448334058125, 1.8865026235580444, 0.9597523219814241, 0.9671679449742965, 0.9765309864319766 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 122, 1.5863481760025024, 1.6932262579600017, 1.8985224962234497, 0.9648105181747874, 0.969362583666582, 0.974821367812181 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 123, 1.5365279912948608, 1.6389374335606892, 1.7239294052124023, 0.9614021268215833, 0.9673294544799352, 0.9730529083141637 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 124, 1.5607047080993652, 1.6774375438690186, 1.871826410293579, 0.9607397793640493, 0.9672289378531382, 0.9720305121685434 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 125, 1.5842971801757812, 1.703223208586375, 1.943206548690796, 0.9506369426751592, 0.9639584625740131, 0.9728301886792453 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 126, 1.5366222858428955, 1.640598436196645, 1.7135374546051025, 0.9627473806752037, 0.9680490042285242, 0.9734426229508196 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 127, 1.606985330581665, 1.7038463751475017, 1.9246344566345215, 0.9632822477650064, 0.9658750982718708, 0.9705561613958561 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 128, 1.6010830402374268, 1.7027505040168762, 1.9207789897918701, 0.9583481666073336, 0.9643911255988685, 0.9725776965265083 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 129, 1.6145564317703247, 1.7083363731702168, 1.9204641580581665, 0.9593241551939925, 0.9636040781817198, 0.9664299548095545 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 130, 1.5355712175369263, 1.643915633360545, 1.7246646881103516, 0.9552819183408944, 0.96270127166475, 0.970050563982886 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 131, 1.5990346670150757, 1.7155587474505107, 1.9496289491653442, 0.9543393782383419, 0.9653145512245848, 0.9746621621621622 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 132, 1.5161749124526978, 1.6323487361272175, 1.7251425981521606, 0.9607056936647955, 0.9676349757778512, 0.9776908023483366 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 133, 1.606669545173645, 1.7055446704228718, 1.9142991304397583, 0.956096020214782, 0.9605589210116113, 0.9658179281478898 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 134, 1.6007723808288574, 1.7008912762006123, 1.9064372777938843, 0.9554848966613673, 0.9648419224289141, 0.9728240910760191 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 135, 1.6058839559555054, 1.715877632300059, 1.9289616346359253, 0.9598741148701809, 0.9627186726727902, 0.9662047989185536 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 136, 1.5900007486343384, 1.703506847222646, 1.9212995767593384, 0.9579646017699115, 0.9618624440390243, 0.9680968096809681 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 137, 1.6029092073440552, 1.7150281071662903, 1.905590295791626, 0.9573344169036977, 0.9622021661600387, 0.9686555891238671 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 138, 1.524883508682251, 1.641092300415039, 1.7588787078857422, 0.9548114434330299, 0.9627347235216145, 0.9680192572214581 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 139, 1.5905219316482544, 1.7050808270772297, 1.9105918407440186, 0.9588543767648245, 0.966348581348729, 0.9720438231960711 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 140, 1.5061417818069458, 1.6427184343338013, 1.7407400608062744, 0.9491803278688524, 0.9599820037481482, 0.9673510602490744 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 141, 1.6277086734771729, 1.7205183704694111, 1.9575234651565552, 0.9567584881486226, 0.965035524092584, 0.9728490832157969 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 142, 1.6036306619644165, 1.7002676129341125, 1.9089672565460205, 0.9533781114184117, 0.9605349075714938, 0.9672846237731734 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 143, 1.5927302837371826, 1.7179765303929646, 1.9655870199203491, 0.9607530312699426, 0.9663394157639589, 0.9724896836313618 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 144, 1.6081408262252808, 1.7205002705256145, 1.939281940460205, 0.9570576540755468, 0.963702347352516, 0.9715639810426541 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 145, 1.6222851276397705, 1.7278209726015727, 1.972853183746338, 0.9559429477020602, 0.9652914811357762, 0.970050563982886 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 146, 1.5928421020507812, 1.7072955965995789, 1.9313100576400757, 0.9531303455354089, 0.9636998936087707, 0.9678966789667897 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 147, 1.5750689506530762, 1.6678698460261028, 1.7601509094238281, 0.9529489728296885, 0.9634219557182301, 0.9703803433187479 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 148, 1.5913840532302856, 1.7016958594322205, 1.9441807270050049, 0.9545454545454546, 0.9622794569600305, 0.9706112852664577 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 149, 1.6142109632492065, 1.7289138237635295, 1.9502233266830444, 0.9516547696301103, 0.9600573331872367, 0.9683275395905755 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 150, 1.5985370874404907, 1.7127336462338765, 1.9542804956436157, 0.9462639109697933, 0.9614328636662419, 0.9690824468085106 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 151, 1.5223973989486694, 1.6339101394017537, 1.704500436782837, 0.9525793015059276, 0.9606029382581758, 0.964524765729585 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 152, 1.5879098176956177, 1.701353410879771, 1.9226244688034058, 0.956989247311828, 0.963656443308523, 0.9698548567175288 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 153, 1.6293959617614746, 1.7874524394671123, 1.9803643226623535, 0.9415370108439416, 0.9574542372658962, 0.9651162790697675 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 154, 1.574871301651001, 1.7035215894381206, 1.9358433485031128, 0.9509868421052632, 0.9614192954336542, 0.9669559412550067 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 155, 1.5666178464889526, 1.7197301586469014, 1.9440189599990845, 0.9502338009352037, 0.9603550281901924, 0.968473663975394 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 156, 1.5824028253555298, 1.7089781562487285, 1.961410403251648, 0.9499181669394435, 0.963696185676061, 0.973314606741573 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 157, 1.5725244283676147, 1.6925131281216939, 1.9349414110183716, 0.9540636042402827, 0.9626965341119216, 0.9679919137466307 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 158, 1.6434383392333984, 1.8627337416013081, 2.177839517593384, 0.9420798595875384, 0.9545749664114481, 0.9653707938582162 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 159, 1.59622061252594, 1.7299720843633015, 1.9947178363800049, 0.9558773997979118, 0.9613385859945786, 0.9695892575039494 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 160, 1.5691684484481812, 1.69951065381368, 1.9320995807647705, 0.952591402169546, 0.9609393691127409, 0.9681481481481482 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 161, 1.5816278457641602, 1.7087932229042053, 1.9403057098388672, 0.9593817397555715, 0.9630406953834648, 0.969732246798603 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 162, 1.615296483039856, 1.7332876920700073, 1.9942047595977783, 0.9510420112471055, 0.9634120816741375, 0.9700476514635806 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 163, 1.613237977027893, 1.7221608956654866, 1.9515715837478638, 0.9517355371900826, 0.9582975914096495, 0.9648307896483079 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 164, 1.6113214492797852, 1.7307289838790894, 1.9311610460281372, 0.9497278258085174, 0.9577246504883236, 0.9635833940276766 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 165, 1.6018229722976685, 1.7261549433072407, 1.974181890487671, 0.948236069897791, 0.9549768852808925, 0.9609296059279219 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 166, 1.5279556512832642, 1.6489343841870625, 1.7364851236343384, 0.9441056910569106, 0.9583149984862173, 0.9713793103448276 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 167, 1.5402247905731201, 1.6484312613805134, 1.7641957998275757, 0.9512594046450769, 0.9614585074352068, 0.9701542588866533 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 168, 1.5903922319412231, 1.6914054155349731, 1.9176204204559326, 0.95, 0.9598538320474171, 0.9716767371601208 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 169, 1.6008358001708984, 1.7190862099329631, 1.960518479347229, 0.9478577202910267, 0.9578051342896113, 0.9668174962292609 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 170, 1.5097540616989136, 1.635406494140625, 1.7607072591781616, 0.9518518518518518, 0.9633393169429906, 0.9718026183282981 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 171, 1.518952488899231, 1.6387594938278198, 1.747834324836731, 0.9465346534653465, 0.9579551040002698, 0.9637873754152824 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 172, 1.6031562089920044, 1.7107612093289692, 1.9422695636749268, 0.957760989010989, 0.962811276084618, 0.9704905938767983 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 173, 1.5119643211364746, 1.6299973328908284, 1.7201423645019531, 0.9510004083299306, 0.9575121153034059, 0.9639074146724206 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 174, 1.5611305236816406, 1.7174640695254009, 1.985178828239441, 0.9448630136986301, 0.9548378765159379, 0.9654637175009702 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 175, 1.5575844049453735, 1.675627330938975, 1.7758584022521973, 0.9511691884456671, 0.9598525463642728, 0.9675558759913482 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 176, 1.5283033847808838, 1.6365413069725037, 1.7527726888656616, 0.9496981891348089, 0.9578689812690063, 0.9639001349527665 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 177, 1.5948337316513062, 1.7124940752983093, 1.9437575340270996, 0.9490662139219015, 0.9578725347577421, 0.9647150058162078 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 178, 1.5489245653152466, 1.6486278971036274, 1.7533276081085205, 0.9548060708263069, 0.9620960811161611, 0.9695607056381875 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 179, 1.5433231592178345, 1.6515618761380513, 1.770294189453125, 0.9558277654046028, 0.9591072049017809, 0.9643636363636363 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 180, 1.5400747060775757, 1.664241075515747, 1.7861788272857666, 0.9489921842863019, 0.9586928796160273, 0.9645986680686996 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 181, 1.5802359580993652, 1.6886998812357585, 1.899155616760254, 0.9382757463938276, 0.9559032363937549, 0.9650837988826816 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 182, 1.527144193649292, 1.6475236018498738, 1.7594108581542969, 0.9518394648829431, 0.9573615611615569, 0.9639001349527665 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 183, 1.5313776731491089, 1.6515411535898845, 1.7687538862228394, 0.9480135249366018, 0.9579087464814604, 0.9652379345258184 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 184, 1.5111745595932007, 1.6382564306259155, 1.74457848072052, 0.9497697781498535, 0.9591683895785975, 0.9668807029401825 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 185, 1.6269102096557617, 1.7359571258227031, 2.0102386474609375, 0.9459005376344086, 0.9582284547389035, 0.9651162790697675 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 186, 1.50460946559906, 1.62788321574529, 1.769491195678711, 0.9512362637362637, 0.9578366980035066, 0.9651556156968877 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 187, 1.5312353372573853, 1.644807795683543, 1.7501721382141113, 0.9466386554621848, 0.9546913498190984, 0.961524759529747 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 188, 1.575893759727478, 1.6870361765225728, 1.9062983989715576, 0.9459745762711864, 0.957902900727997, 0.9679089026915114 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 189, 1.5674200057983398, 1.677693784236908, 1.7918933629989624, 0.9459276782696857, 0.9562006141271087, 0.9674637397099177 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 190, 1.6144760847091675, 1.7182808915774028, 1.96262526512146, 0.9420875420875421, 0.9550462699833315, 0.9631227932522558 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 191, 1.5199713706970215, 1.640555461247762, 1.7314046621322632, 0.9408713692946058, 0.9540184182903197, 0.9633714060653801 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 192, 1.6024643182754517, 1.6943806012471516, 1.9254826307296753, 0.9420783645655877, 0.9547715683244702, 0.9644343548997352 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 193, 1.5974634885787964, 1.727014164129893, 2.0155832767486572, 0.9483722459717199, 0.9581532687102278, 0.9659676864902028 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 194, 1.5992943048477173, 1.7124213774998982, 1.9793177843093872, 0.9468646864686469, 0.9571138968647187, 0.9630818619582665 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 195, 1.5730408430099487, 1.6848955949147542, 1.8929109573364258, 0.9547297297297297, 0.9611220550593037, 0.9707097258730755 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 196, 1.587584137916565, 1.703760286172231, 1.9349769353866577, 0.9480681346073951, 0.9565759005697462, 0.962431693989071 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 197, 1.57707941532135, 1.6819013754526775, 1.9114314317703247, 0.9511948838774823, 0.958435028171837, 0.9652730950775456 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 198, 1.6235078573226929, 1.727578600247701, 1.9641766548156738, 0.9460547504025765, 0.9564293857861556, 0.9643845089903181 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 199, 1.5388517379760742, 1.6794010003407795, 1.9391237497329712, 0.9479056670186554, 0.9595643967493209, 0.9667063020214031 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 200, 1.508813738822937, 1.6303137342135112, 1.75262451171875, 0.946671195652174, 0.9577826077267243, 0.9640635798203179 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 201, 1.577466368675232, 1.6939815481503804, 1.9340029954910278, 0.9487391484084332, 0.9600266266643241, 0.9656344410876133 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 202, 1.558579683303833, 1.6817200183868408, 1.920337200164795, 0.9525560867966164, 0.9596261771781681, 0.9662000682826903 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 203, 1.5718284845352173, 1.6896467804908752, 1.9505845308303833, 0.9442105263157895, 0.9558759050870124, 0.9622350674373795 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 204, 1.5666649341583252, 1.6946412722269695, 1.9336479902267456, 0.9510982179859097, 0.9584158795387158, 0.9678305217732444 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 205, 1.5423249006271362, 1.6861331462860107, 1.948500156402588, 0.9445413324032089, 0.9530973351047388, 0.9590361445783132 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 206, 1.5860365629196167, 1.699912190437317, 1.9374991655349731, 0.944378228049265, 0.9548148878292833, 0.9665643125213238 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 207, 1.5981162786483765, 1.7058812578519185, 1.9743807315826416, 0.9462585034013605, 0.9546864190622438, 0.963258232235702 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 208, 1.5987361669540405, 1.7102017998695374, 1.9630420207977295, 0.9480563002680965, 0.9574507945436355, 0.9688593421973408 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 209, 1.572716236114502, 1.6754315098126729, 1.8749966621398926, 0.9451882845188284, 0.952463439425454, 0.9559132260321903 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 210, 1.5847920179367065, 1.6987008651097615, 1.9326452016830444, 0.951473136915078, 0.9615768297977739, 0.9669104841518634 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 211, 1.5925778150558472, 1.7014639774958293, 1.9516704082489014, 0.945964669206789, 0.9566620265350944, 0.9660076309399931 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 212, 1.573500394821167, 1.7071045239766438, 1.969913125038147, 0.953444360333081, 0.9582585718950026, 0.965323992994746 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 213, 1.5751657485961914, 1.7155040899912517, 2.000051975250244, 0.9441687344913151, 0.9537206854657597, 0.9638132295719845 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 214, 1.5671591758728027, 1.7144807974497478, 1.962027907371521, 0.9462227912932138, 0.9527775694997095, 0.9603455045151158 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 215, 1.5848661661148071, 1.7142133116722107, 1.9456911087036133, 0.9479923518164436, 0.9571977928485889, 0.9669161087042143 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 216, 1.5807538032531738, 1.7269968787829082, 1.970346450805664, 0.9473317056156261, 0.9560961800753766, 0.9646341463414634 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 217, 1.5670260190963745, 1.7086017727851868, 1.9825758934020996, 0.9351851851851852, 0.9529927347803882, 0.9674698795180723 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 218, 1.5694260597229004, 1.6946364641189575, 1.9521710872650146, 0.9511482254697285, 0.9589845410849916, 0.9714867617107943 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 219, 1.5951520204544067, 1.7299905220667522, 1.9969241619110107, 0.9473684210526315, 0.9573594781149871, 0.9663608562691132 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 220, 1.608273983001709, 1.711024562517802, 1.9040303230285645, 0.9533987386124737, 0.959727040603128, 0.966796875 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 221, 1.5559722185134888, 1.7254364887873332, 1.9854588508605957, 0.9453781512605042, 0.9548117376693686, 0.9657002858309514 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 222, 1.6217255592346191, 1.7536453207333882, 1.9830268621444702, 0.9486477233824033, 0.955567628596769, 0.9639967637540453 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 223, 1.6132134199142456, 1.7347165147463481, 1.979617714881897, 0.9490223463687151, 0.9570565077305098, 0.9665610700457585 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 224, 1.5915212631225586, 1.7358543674151103, 1.9606555700302124, 0.9399827288428325, 0.9507173625902589, 0.9648261758691207 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 225, 1.5759891271591187, 1.7065946459770203, 1.9470741748809814, 0.9430979978925185, 0.9578414338219777, 0.9701555275325767 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 226, 1.6324678659439087, 1.75869619846344, 1.973283052444458, 0.9458413926499033, 0.9508405879562106, 0.9569593891010066 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 227, 1.6139353513717651, 1.740302860736847, 1.9749635457992554, 0.944206008583691, 0.954395867304984, 0.9602836879432625 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 228, 1.6752195358276367, 1.8614332675933838, 2.1480422019958496, 0.9434554973821989, 0.9555880708817656, 0.9653537563822028 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 229, 1.6049246788024902, 1.743294397989909, 1.9802348613739014, 0.9365279529213956, 0.9492141373616091, 0.958904109589041 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 230, 1.6241493225097656, 1.748774250348409, 1.9594078063964844, 0.9415337889141989, 0.949939632956435, 0.9596122778675282 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 231, 1.6014798879623413, 1.7359582980473836, 1.9527840614318848, 0.9424314256688113, 0.9531565291847, 0.9617486338797814 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 232, 1.6159158945083618, 1.7451598246892293, 1.9369410276412964, 0.9385843164469119, 0.9489265673879516, 0.9578074287774972 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 233, 1.589997410774231, 1.713127116362254, 1.9150049686431885, 0.9507914659325534, 0.9587052256110025, 0.966824644549763 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 234, 1.582884430885315, 1.7220104535420735, 1.94236421585083, 0.9488795518207283, 0.9583098498172649, 0.9638263665594855 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 235, 1.6580431461334229, 1.7520660956700642, 1.9667030572891235, 0.9508825786646201, 0.9596138043870428, 0.9704811969268096 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 236, 1.5994391441345215, 1.7228879928588867, 1.913842797279358, 0.9488555078683834, 0.9578313673896653, 0.9651759530791789 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 237, 1.6572115421295166, 1.8555166522661846, 2.150590181350708, 0.9413854351687388, 0.9502183868430568, 0.95949263502455 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 238, 1.6476479768753052, 1.8412818511327107, 2.0993268489837646, 0.9411764705882353, 0.9505031896344094, 0.9627201966407211 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 239, 1.645446538925171, 1.8247068325678508, 2.0945773124694824, 0.9474290099769762, 0.9548591848299187, 0.963978714695047 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 240, 1.6500462293624878, 1.8404795130093892, 2.1534881591796875, 0.9425287356321839, 0.9541815929594947, 0.9630252100840336 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 241, 1.658663034439087, 1.8551423748334248, 2.167856216430664, 0.9388400702987698, 0.9488026668392072, 0.957166392092257 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 242, 1.6265368461608887, 1.8334292769432068, 2.1132636070251465, 0.9413593958240782, 0.9512522790769441, 0.9608731466227347 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 243, 1.5931782722473145, 1.7220899264017742, 1.9358497858047485, 0.9482638888888889, 0.955780679495906, 0.9678443826915443 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 244, 1.608971357345581, 1.7387365500132244, 1.9346405267715454, 0.9378145219266715, 0.951040338639794, 0.9627702161729383 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 245, 1.6661949157714844, 1.8567901452382405, 2.169090747833252, 0.9374135546334716, 0.950361738311689, 0.9606270506744441 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 246, 1.6519161462783813, 1.8403209447860718, 2.159132719039917, 0.9431171786120591, 0.9530606614840497, 0.9602756384272395 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 247, 1.65725839138031, 1.8254758715629578, 2.102229595184326, 0.9450897571277719, 0.9528648023346271, 0.9601342845153168 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 248, 1.663632869720459, 1.8466234803199768, 2.1215832233428955, 0.9536878216123499, 0.960907552253477, 0.9684647302904564 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 249, 1.6930992603302002, 1.8810086051623027, 2.1956286430358887, 0.9384835479256081, 0.9522036887254649, 0.959563283461383 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 250, 1.6699830293655396, 1.8419113953908284, 2.119027853012085, 0.9423849933005806, 0.954526184530061, 0.9667477696674777 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 251, 1.640185832977295, 1.829611321290334, 2.1309568881988525, 0.9417000445037829, 0.9534702906850144, 0.9676724137931034 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 252, 1.6085513830184937, 1.7487957278887432, 1.999627947807312, 0.9388686131386861, 0.9476277437292061, 0.9552583025830258 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 253, 1.6314574480056763, 1.8471223711967468, 2.1355926990509033, 0.9459152798789713, 0.9555780115420371, 0.9682735887927483 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 254, 1.621284008026123, 1.814189652601878, 2.123413562774658, 0.9418874941887494, 0.9497353888583091, 0.9628099173553719 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 255, 1.5742865800857544, 1.7004127502441406, 1.9166712760925293, 0.9353340478742408, 0.9508231431961397, 0.9657070279424217 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 256, 1.683037281036377, 1.8595745364824932, 2.1186561584472656, 0.9437679083094556, 0.9518300078785492, 0.9598804950917627 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 257, 1.6290339231491089, 1.8178640604019165, 2.0856521129608154, 0.9444664815549385, 0.9503175713657835, 0.9589678510998308 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 258, 1.6752177476882935, 1.8394526441891987, 2.119141101837158, 0.9368581209585394, 0.9500793348386907, 0.9602695991486343 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 259, 1.6643249988555908, 1.8695754210154216, 2.1573381423950195, 0.9475920679886686, 0.9555785561514686, 0.9658227848101266 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 260, 1.633056640625, 1.7391061385472615, 1.9550063610076904, 0.9480231008440693, 0.957535184433317, 0.9650092081031307 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 261, 1.5838713645935059, 1.7002485990524292, 1.904476284980774, 0.941819772528434, 0.9519870319718251, 0.9645621181262729 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 262, 1.592617154121399, 1.7185367544492085, 1.9372367858886719, 0.9391812865497076, 0.9509408966032801, 0.9600679694137638 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 263, 1.5911269187927246, 1.718589226404826, 1.947585105895996, 0.9469026548672567, 0.9534799317556665, 0.9576697401508801 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 264, 1.616759181022644, 1.715800444285075, 1.918503761291504, 0.9471876341777586, 0.9558380456859806, 0.9621026894865525 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 265, 1.593601942062378, 1.714219868183136, 1.8783143758773804, 0.9378260869565217, 0.9499920722061436, 0.9605067064083458 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 266, 1.6503057479858398, 1.8423586289087932, 2.149200677871704, 0.936359590565198, 0.9539058695004242, 0.963919639196392 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 267, 1.6379448175430298, 1.8312202095985413, 2.119497299194336, 0.9435146443514645, 0.9540121246685151, 0.9621968616262482 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 268, 1.6208685636520386, 1.7107183337211609, 1.8944423198699951, 0.9483471074380165, 0.9560025163798819, 0.9647310295689348 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 269, 1.6241294145584106, 1.7186627785364788, 1.908668875694275, 0.9399690162664601, 0.9494414883239665, 0.9624122263527468 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 270, 1.5607637166976929, 1.7005714972813923, 1.917895793914795, 0.943305713039686, 0.9511710795193752, 0.9644202595228129 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 271, 1.6029753684997559, 1.7352551619211833, 1.9882874488830566, 0.942225392296719, 0.9511690079579349, 0.9608345534407028 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 272, 1.6038624048233032, 1.7574714422225952, 1.998871088027954, 0.9390374331550803, 0.9481540062635849, 0.955276579050608 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 273, 1.5955718755722046, 1.7207245826721191, 1.9664769172668457, 0.9404502541757443, 0.9513677748788107, 0.9599711503786513 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 274, 1.6312458515167236, 1.7541517615318298, 1.975887656211853, 0.9489465153970826, 0.9553802436796519, 0.9607933579335793 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 275, 1.6524336338043213, 1.8466522097587585, 2.1122515201568604, 0.9392014519056261, 0.9498026941662351, 0.958104104951333 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 276, 1.5865733623504639, 1.7016642093658447, 1.9043281078338623, 0.9450856942987058, 0.9528473301104393, 0.9632107023411371 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 277, 1.5974725484848022, 1.718796710173289, 1.931140422821045, 0.9437794704388829, 0.95367019990481, 0.9663066954643629 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 278, 1.6240930557250977, 1.7398961583773296, 1.947887659072876, 0.9403197158081705, 0.9522143106851164, 0.9651312957382695 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 279, 1.5987093448638916, 1.7336943944295247, 1.956038236618042, 0.9377445748843828, 0.9502263918602877, 0.9604841580633677 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 280, 1.6039336919784546, 1.7139435807863872, 1.9290870428085327, 0.9419180549302116, 0.9514860035149152, 0.9585427135678392 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 281, 1.6044899225234985, 1.7245485385258992, 1.8985769748687744, 0.9449781659388646, 0.9568465946311427, 0.9662642045454546 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 282, 1.5892012119293213, 1.7061912616093953, 1.8972320556640625, 0.942846034214619, 0.9548776670237733, 0.9649345162653148 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 283, 1.6073426008224487, 1.7233226696650188, 1.9592310190200806, 0.9410267288926601, 0.9505760019858602, 0.9641517298874531 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 284, 1.5773451328277588, 1.7083760301272075, 1.8993144035339355, 0.9421990438939591, 0.9519408936221448, 0.9643895348837209 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 285, 1.6133264303207397, 1.731576184431712, 1.9282740354537964, 0.9396628216503993, 0.952109205835617, 0.9611436950146628 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 286, 1.6384739875793457, 1.7604334155718486, 1.9659126996994019, 0.9428350714561606, 0.9506008908900868, 0.9635912287960281 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 287, 1.6210280656814575, 1.7555815974871318, 1.983307123184204, 0.9329940627650551, 0.9472365583490783, 0.9663972777541472 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 288, 1.6169581413269043, 1.7374642690022786, 1.9832662343978882, 0.9448099415204678, 0.9518126467365148, 0.9608421052631579 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 289, 1.626725196838379, 1.7537443041801453, 1.9581156969070435, 0.9418153607447634, 0.9508375148151286, 0.9555475243946513 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 290, 1.6215935945510864, 1.7610488136609395, 1.983129620552063, 0.93296853625171, 0.9481638260441002, 0.9626096909576497 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 291, 1.6393249034881592, 1.7556434472401936, 1.9752057790756226, 0.9440635149765427, 0.9546492045122998, 0.9644024700326916 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 292, 1.6369003057479858, 1.7668787638346355, 1.9912434816360474, 0.9460008120178643, 0.9503375442082315, 0.9565217391304348 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 293, 1.5978089570999146, 1.739584465821584, 1.9336844682693481, 0.9369449378330373, 0.949931273969903, 0.9679213002566296 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 294, 1.7032021284103394, 1.896750013033549, 2.184535503387451, 0.9380445304937076, 0.9507979776448835, 0.9617889709075119 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 295, 1.6467877626419067, 1.764617343743642, 1.9770258665084839, 0.9361419068736142, 0.9481367594735034, 0.9632006846384253 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 296, 1.6188212633132935, 1.7473501563072205, 1.9696590900421143, 0.9424151267726687, 0.9524762466653932, 0.9671399594320487 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 297, 1.645015835762024, 1.7686326901117961, 1.998482346534729, 0.9430637144148215, 0.9537366981427491, 0.9666527719883284 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 298, 1.6055434942245483, 1.7684065500895183, 1.9963124990463257, 0.9310718152291592, 0.9492607219854122, 0.9581132075471698 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 299, 1.6538739204406738, 1.7728787859280903, 1.9699175357818604, 0.9405405405405406, 0.9518829010036168, 0.9618608549874267 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 300, 1.658218502998352, 1.7690735856691997, 2.000413417816162, 0.9429726088908846, 0.9501742746189977, 0.9642705231816249 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 301, 1.6308375597000122, 1.741701642672221, 1.9338130950927734, 0.9376114081996435, 0.9478009460781195, 0.9579622372639829 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 302, 1.6078581809997559, 1.7365942597389221, 1.9757739305496216, 0.9387610619469027, 0.9500244265384842, 0.9590231788079471 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 303, 1.631874442100525, 1.769434670607249, 1.985072374343872, 0.938241308793456, 0.9469605665412035, 0.9553726169844021 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 304, 1.6885707378387451, 1.8124090035756428, 2.0735597610473633, 0.9409647228221742, 0.9496355904714351, 0.9583785740137531 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 305, 1.6789461374282837, 1.8026105960210164, 2.031966209411621, 0.9468037550290568, 0.9533715441769618, 0.9622957687473817 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 306, 1.666229486465454, 1.8035929600397747, 2.0514354705810547, 0.9376412110257569, 0.9471188996515827, 0.9562660786475561 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 307, 1.7654123306274414, 1.965090036392212, 2.31255841255188, 0.9424493554327809, 0.9511114339139901, 0.9579655317360235 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 308, 1.7586127519607544, 1.969997763633728, 2.361069679260254, 0.9439592430858806, 0.950565731270809, 0.9600175746924429 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 309, 1.767977237701416, 2.0054562091827393, 2.4261016845703125, 0.9308087891538102, 0.9466682347094751, 0.9585303746817024 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 310, 1.7959277629852295, 2.071215867996216, 2.629335880279541, 0.9412644968200524, 0.9474639643028758, 0.954161103693814 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 311, 1.7473063468933105, 1.960598627726237, 2.304255962371826, 0.9360601786553832, 0.9502871973820097, 0.9569500182415177 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 312, 1.7348483800888062, 1.9591535727183025, 2.3364739418029785, 0.9395584509591024, 0.9486990833647035, 0.9586383601756955 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 313, 1.727165699005127, 1.9390812714894612, 2.3061532974243164, 0.9344587884806356, 0.9498298553051471, 0.9612997444322745 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 314, 1.7426774501800537, 1.9541202982266743, 2.3356728553771973, 0.9330889092575618, 0.946299562871991, 0.9604591836734694 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 315, 1.6614794731140137, 1.9302804072697957, 2.3102447986602783, 0.9421797004991681, 0.9528461480309232, 0.9655172413793104 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 316, 1.6913987398147583, 1.9311785101890564, 2.2800755500793457, 0.9408502772643254, 0.9499542574673691, 0.9632670700086431 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 317, 1.7024129629135132, 1.9330380757649739, 2.280815362930298, 0.941712204007286, 0.9525035741270691, 0.9631748301751877 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 318, 1.685755968093872, 1.8069371779759724, 2.0626132488250732, 0.948013367991088, 0.9528437646364306, 0.9571068124474348 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 319, 1.7164397239685059, 1.9448129137357075, 2.3037939071655273, 0.935854475825754, 0.9473409757151443, 0.9562720848056537 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 320, 1.6853444576263428, 1.913787066936493, 2.268692970275879, 0.9381593306656966, 0.9484061833548995, 0.9549234135667396 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 321, 1.7299524545669556, 1.9565688172976177, 2.3168795108795166, 0.9287054409005628, 0.9445481715576451, 0.9536804308797128 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 322, 1.7500969171524048, 1.9551203846931458, 2.268484354019165, 0.9339449541284404, 0.9467386606912297, 0.9594417793283908 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 323, 1.734403371810913, 1.943222463130951, 2.2876651287078857, 0.932761087267525, 0.946175930074448, 0.9581896551724138 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 324, 1.6954681873321533, 1.8833789428075154, 2.142073154449463, 0.9094269870609981, 0.94254945284993, 0.9600760456273765 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 325, 1.7518815994262695, 1.9672709306081135, 2.3428685665130615, 0.9368635437881874, 0.946144833804432, 0.9550133434998094 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 326, 1.675992488861084, 1.9122587243715923, 2.292466878890991, 0.9368421052631579, 0.9478048382204577, 0.9575021682567216 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 327, 1.6690168380737305, 1.910009503364563, 2.2773168087005615, 0.9322115384615385, 0.9454044973049397, 0.9504470938897168 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 328, 1.7102468013763428, 1.9225364724795024, 2.226331949234009, 0.9449075752084088, 0.9517522408322211, 0.9595229490422841 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 329, 1.7053216695785522, 1.9473621249198914, 2.3248276710510254, 0.9363736667892607, 0.9498779292366635, 0.9588688946015425 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 330, 1.7281097173690796, 1.9553959767023723, 2.3471667766571045, 0.9383070301291249, 0.9474814175871371, 0.9565217391304348 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 331, 1.714241623878479, 1.943812867005666, 2.3009700775146484, 0.9406264609630669, 0.9510874037399115, 0.9604927782497876 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 332, 1.712548017501831, 1.9686156511306763, 2.331970691680908, 0.9414245548266167, 0.9512148140766309, 0.9615552643075579 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 333, 1.658645749092102, 1.9216991662979126, 2.3421413898468018, 0.9336782690498588, 0.9494066314294068, 0.9594947735191638 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 334, 1.6614527702331543, 1.936685582002004, 2.3481013774871826, 0.9437689969604863, 0.9510674532136668, 0.9600363306085377 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 335, 1.7143694162368774, 1.9192385077476501, 2.2442591190338135, 0.9408752327746741, 0.9517006643682974, 0.9585571757482733 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 336, 1.7052998542785645, 1.9234701593716939, 2.2678589820861816, 0.9330802088277171, 0.9495286869788359, 0.9608888888888889 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 337, 1.7077548503875732, 1.9227336446444194, 2.2379531860351562, 0.9412607449856734, 0.9495496220666109, 0.9616874730951356 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 338, 1.6071745157241821, 1.8122721115748088, 2.040987730026245, 0.9379236532286835, 0.9493243825808652, 0.957408081543502 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 339, 1.53253972530365, 1.670154293378194, 1.8138571977615356, 0.9382826475849732, 0.9475537874772516, 0.9592579119679884 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 340, 1.5294861793518066, 1.6790962219238281, 1.827099084854126, 0.9472459270752521, 0.9533833264400716, 0.9598042642432716 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 341, 1.5624217987060547, 1.6752528150876362, 1.8414154052734375, 0.9349722442505948, 0.9434714198957836, 0.9549270879363676 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 342, 1.617659568786621, 1.796622057755788, 1.985163688659668, 0.9440263405456256, 0.948976364155178, 0.9531525184924269 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 343, 1.549444317817688, 1.7025600870450337, 1.8762465715408325, 0.9456214689265536, 0.9536075826600876, 0.9638047138047138 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 344, 1.5360015630722046, 1.6725580493609111, 1.8369122743606567, 0.9377431906614786, 0.9453207509506573, 0.9574544154451198 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 345, 1.631345510482788, 1.7344860434532166, 1.9087907075881958, 0.9372453137734311, 0.9477270035831261, 0.9578641482019615 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 346, 1.616646647453308, 1.8005117972691853, 1.9833747148513794, 0.9349593495934959, 0.9473118110133151, 0.957656116338751 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 347, 1.5426390171051025, 1.6958154837290447, 1.8853716850280762, 0.9407204742362061, 0.9474759007521095, 0.9591208791208791 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 348, 1.5281983613967896, 1.6773338516553242, 1.8270535469055176, 0.939615736505032, 0.9512852294581161, 0.9591251344567946 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 349, 1.5527589321136475, 1.6992446978886921, 1.8764398097991943, 0.9356884057971014, 0.9467896104325834, 0.9600173837461973 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 350, 1.6026054620742798, 1.7066826820373535, 1.8790295124053955, 0.9412026726057906, 0.9510436259713302, 0.9608256107834878 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 351, 1.536532998085022, 1.6796143054962158, 1.8244495391845703, 0.9442484121383204, 0.9509144833828244, 0.9577198942997357 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 352, 1.549379587173462, 1.6873960892359416, 1.825623631477356, 0.944603629417383, 0.9498406341541329, 0.9599117322545053 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 353, 1.584637999534607, 1.7110970616340637, 1.90225350856781, 0.9442724458204335, 0.9559633613036339, 0.9637975618766161 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 354, 1.5678281784057617, 1.7574419379234314, 1.9417266845703125, 0.9318504495977283, 0.9473015184056655, 0.9592282489989079 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 355, 1.5506871938705444, 1.6917302012443542, 1.9397703409194946, 0.9395348837209302, 0.9497075458683178, 0.9607420189818809 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 356, 1.5207785367965698, 1.658125062783559, 1.8386342525482178, 0.9460853258321613, 0.9536909410784254, 0.9617787578096288 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 357, 1.5939857959747314, 1.784203867117564, 1.9494844675064087, 0.9353593825373855, 0.9480595771975536, 0.9584040747028862 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 358, 1.5330097675323486, 1.6645127534866333, 1.782859206199646, 0.931723176962378, 0.946460445521703, 0.9583517944173682 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 359, 1.5228639841079712, 1.673193593819936, 1.8753050565719604, 0.9291784702549575, 0.9470891455388287, 0.9568684305907937 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 360, 1.5700894594192505, 1.7486850221951802, 1.9068375825881958, 0.9342544068604097, 0.9491057962381692, 0.9608837377761681 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 361, 1.5367809534072876, 1.6674458980560303, 1.7749478816986084, 0.9433874709976798, 0.948902926088628, 0.9582100591715976 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 362, 1.5297585725784302, 1.6759331822395325, 1.8530969619750977, 0.9356280733124721, 0.9491435687587183, 0.9570943075615973 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 363, 1.5679588317871094, 1.678926984469096, 1.7696771621704102, 0.9381953028430161, 0.9483595540717386, 0.9576629974597799 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 364, 1.6010222434997559, 1.7194649179776509, 1.8291912078857422, 0.9420097388224878, 0.9507138678978603, 0.9593120805369127 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 365, 1.5606739521026611, 1.7017750938733418, 1.9137543439865112, 0.9402585822559073, 0.951613894776906, 0.9587671803415244 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 366, 1.5857802629470825, 1.6815964380900066, 1.8031643629074097, 0.9429783223374175, 0.95265373532736, 0.9653356735410268 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 367, 1.6300806999206543, 1.7726765275001526, 1.9121763706207275, 0.9375886524822695, 0.9499988245669733, 0.9600343790287924 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 368, 1.5956299304962158, 1.7311200896898906, 1.925315260887146, 0.9359534206695779, 0.9462296748051578, 0.9571585499816917 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 369, 1.5625940561294556, 1.6889758507410686, 1.8280032873153687, 0.93710407239819, 0.9480631887033139, 0.9587513935340022 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 370, 1.565006971359253, 1.6911243796348572, 1.8405381441116333, 0.9308608058608059, 0.9446758985163498, 0.9579646017699115 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 371, 1.6097489595413208, 1.689367651939392, 1.7697744369506836, 0.9377358490566038, 0.9462479191593, 0.9533538936006168 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 372, 1.5990487337112427, 1.6878808538119, 1.7937520742416382, 0.9334582942830365, 0.9472749073551174, 0.9550810014727541 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 373, 1.6487268209457397, 1.8172303040822346, 2.076693296432495, 0.9469781238413052, 0.9562337191094635, 0.965843023255814 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 374, 1.6514852046966553, 1.7413022915522258, 1.90756094455719, 0.9357798165137615, 0.9454678748050656, 0.9575306479859895 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 375, 1.6776964664459229, 1.835413118203481, 2.0920915603637695, 0.9282660332541568, 0.94612872332848, 0.9581413210445469 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 376, 1.6377993822097778, 1.7488171259562175, 1.930222988128662, 0.9409190371991247, 0.9480553990654051, 0.9583154275891707 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 377, 1.614703893661499, 1.7348262866338093, 1.962375283241272, 0.9372050816696915, 0.9465652610483767, 0.9579579579579579 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 378, 1.6682803630828857, 1.8559158245722454, 2.1248362064361572, 0.9376146788990826, 0.9479758654675753, 0.9599659284497445 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 379, 1.64944589138031, 1.8382586240768433, 2.086949586868286, 0.9320614239181014, 0.9485334466292503, 0.9627592044011849 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 380, 1.6581238508224487, 1.7648778955141704, 1.9209747314453125, 0.9377488237423091, 0.9459280946420847, 0.9535243996901627 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 381, 1.6977578401565552, 1.8396133780479431, 2.068399429321289, 0.9335038363171355, 0.9467266231461416, 0.9566371681415929 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 382, 1.6397650241851807, 1.743139664332072, 1.936659812927246, 0.9365303244005642, 0.9454428045858201, 0.955011135857461 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 383, 1.6220237016677856, 1.7602797945340474, 1.9635676145553589, 0.9323761000463178, 0.9443703994437254, 0.9577995478522984 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 384, 1.5949740409851074, 1.7450660268465679, 1.9677470922470093, 0.9404761904761905, 0.9491416130158079, 0.9608102157639806 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 385, 1.5962198972702026, 1.7299718856811523, 1.9007128477096558, 0.9365006852444039, 0.9486441328276206, 0.9578107183580388 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 386, 1.6403342485427856, 1.7545773386955261, 1.9508389234542847, 0.9451382694023194, 0.9516647747114627, 0.9584775086505191 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 387, 1.6575419902801514, 1.8322811722755432, 2.086836099624634, 0.9366262814538676, 0.9505264506612671, 0.9583864118895966 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 388, 1.6098967790603638, 1.7218028505643208, 1.9199048280715942, 0.9444029850746268, 0.9531852793416108, 0.9576235978396344 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 389, 1.336694359779358, 1.6764418284098308, 1.946390986442566, 0.9465899753492194, 0.9576095067706557, 0.9700787401574803 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 390, 1.5825082063674927, 1.7137750188509624, 1.9056832790374756, 0.9413396749900912, 0.9480813859323697, 0.957751791776688 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 391, 1.5844773054122925, 1.7323930859565735, 1.940775990486145, 0.9443226654975888, 0.9517246296999717, 0.9590925994793603 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 392, 1.5827759504318237, 1.7385555108388264, 1.9697041511535645, 0.9369830641985033, 0.948072481321495, 0.9576587795765878 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 393, 1.5632238388061523, 1.7089022000630696, 1.8990449905395508, 0.9428312159709619, 0.9523675783981743, 0.9649669131957961 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 394, 1.600856065750122, 1.7378642757733662, 1.9427015781402588, 0.9389067524115756, 0.945601963682379, 0.9549446353570065 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 395, 1.5686086416244507, 1.7198915084203084, 1.955479383468628, 0.9340262087663804, 0.9487135964394479, 0.9597722960151802 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 396, 1.5825799703598022, 1.7201486031214397, 1.9211626052856445, 0.9485849056603773, 0.9541348405311042, 0.9623008161678974 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 397, 1.6052489280700684, 1.749980052312215, 1.9657421112060547, 0.9417163836622304, 0.9508973494301696, 0.9578107183580388 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 398, 1.6460978984832764, 1.8386198083559673, 2.1261942386627197, 0.938381937911571, 0.9492508543324991, 0.9610332749562172 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 399, 1.5875122547149658, 1.806088129679362, 2.102301597595215, 0.9400444115470022, 0.9490107652206256, 0.9603541185527329 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 400, 1.5668865442276, 1.7022728721300762, 1.901236653327942, 0.9372460496613996, 0.9499665431462246, 0.9614957423176601 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 401, 1.544782280921936, 1.7139469782511394, 1.8928494453430176, 0.9366197183098591, 0.952678237970055, 0.9613508442776736 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 402, 1.535841703414917, 1.7061867316563923, 1.9372344017028809, 0.9459023011707711, 0.9518529034473358, 0.9571645185746778 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 403, 1.606907606124878, 1.8170831600824993, 2.1036319732666016, 0.9430076628352491, 0.9477316243915378, 0.9541684853775644 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 404, 1.5800551176071167, 1.8165140350659688, 2.1016948223114014, 0.9259640102827763, 0.9505599228083518, 0.9605855855855856 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 405, 1.5876470804214478, 1.733607252438863, 1.9344220161437988, 0.9294436906377205, 0.9458447871712731, 0.9575653664809258 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 406, 1.5994771718978882, 1.809923768043518, 2.08144474029541, 0.9310872894333844, 0.9488771350108451, 0.9565557729941292 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 407, 1.5565794706344604, 1.7065402468045552, 1.9153623580932617, 0.9470729751403368, 0.9534094605804126, 0.9617810760667903 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 408, 1.5679454803466797, 1.7323835889498393, 1.975409746170044, 0.9354541263254956, 0.9474069860766493, 0.9605034722222222 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 409, 1.5901886224746704, 1.8035029371579487, 2.098149299621582, 0.9293233082706767, 0.9419257284526495, 0.9637488947833776 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 410, 1.5675843954086304, 1.7848261992136638, 2.0853350162506104, 0.9287128712871288, 0.9451881700129299, 0.9566691785983421 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 411, 1.5977951288223267, 1.793465296427409, 2.049530267715454, 0.9363855421686746, 0.9494243107244892, 0.9541818181818181 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 412, 1.6116446256637573, 1.8112523158391316, 2.0709404945373535, 0.9415121255349501, 0.9503529576622332, 0.958826106000876 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 413, 1.603360891342163, 1.8130294680595398, 2.0702154636383057, 0.9479371316306483, 0.9516047273864342, 0.9552238805970149 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		data.add( new RegistrationStatistics( 414, 1.5735677480697632, 1.7949772278467815, 2.0684738159179688, 0.9433534743202417, 0.9515466098414119, 0.9588584136397331 ,new File( "F:/Temp/test_"+(t++)+".tif" ) ) );
		return data;
	}
}
