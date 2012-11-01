package mpicbg.spim.registration.detection;

public class Detection extends AbstractDetection<Detection> 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Detection( final int id, final float[] location ) 
	{
		super(id, location);
	}

	public Detection( final int id, final float[] location, final float weight ) 
	{
		super(id, location, weight);
	}

	@Override
	public Detection[] createArray( final int arg0 ) { return new Detection[ arg0 ]; }

}
