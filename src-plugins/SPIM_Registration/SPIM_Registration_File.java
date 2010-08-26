import mpicbg.spim.Reconstruction;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.ConfigurationParserSPIM;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewStructure;

import fiji.util.gui.GenericDialogPlus;

public class SPIM_Registration_File extends SPIMRegistrationAbstract
{
	public static String configurationFileStatic = "";
	
	String configurationFile;
	
	@Override
	public Reconstruction execute( )
	{
        SPIMConfiguration config = null;
        
        try 
        {        	
			config = ConfigurationParserSPIM.parseFile( configurationFile );
			
			if ( config.debugLevelInt <= ViewStructure.DEBUG_ALL )
				config.printProperties();
		} 
        catch ( ConfigurationParserException e ) 
        {
        	IOFunctions.println( "Cannot open SPIM configuration file '" + configurationFile + "': \n" + e );
			return null;
		}
	
        return new Reconstruction( config );
		
	}
	
	@Override
	protected void getParameters( final GenericDialogPlus gd )
	{
		configurationFile = gd.getNextString();
		configurationFileStatic = configurationFile;		
	}
	
	@Override
	protected GenericDialogPlus createGenericDialogPlus()
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "SPIM Registration" );
		
		gd.addFileField( "Configuration File", configurationFileStatic, 70 );		
		
		return gd;
	}
}
