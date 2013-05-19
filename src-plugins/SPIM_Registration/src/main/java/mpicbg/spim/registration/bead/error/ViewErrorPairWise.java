package mpicbg.spim.registration.bead.error;

import mpicbg.spim.registration.ViewDataBeads;

public class ViewErrorPairWise
{
	final ViewDataBeads view;
	protected boolean isConnected;
	protected double avgError;

	public ViewErrorPairWise( final ViewDataBeads view )
	{
		this.view = view;
		
		setConnected( false );
		setAvgError( -1 );
	}
	
	public ViewDataBeads getView() { return view; }
	
	public void setConnected( final boolean status ) { this.isConnected = status; }
	public void setAvgError( final double error ) { this.avgError = error; }
	
	public double getAvgError () { return avgError; }
	public boolean isConnected () { return isConnected; }
}
