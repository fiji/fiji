package mpicbg.spim.registration.bead.error;

import java.util.ArrayList;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadIdentification;

/**
 * This class handles View-specific error analysis.
 * 
 * @author Stephan Preibisch
 *
 */
public class ViewErrorStatistics 
{	
	final ViewDataBeads view;
	final ArrayList<ViewErrorPairWise> connectedViews;
	
	public ViewErrorStatistics( ViewDataBeads view )
	{
		this.view = view;
		
		connectedViews = new ArrayList<ViewErrorPairWise>();
		
		for ( ViewDataBeads otherView : view.getViewStructure().getViews() )
			if ( otherView != view )
				connectedViews.add( new ViewErrorPairWise( otherView ) );
	}
	
	public double getViewSpecificError ( final ViewDataBeads otherView )
	{
		boolean foundView = false;
		
		for( ViewErrorPairWise viewError : connectedViews )
			if ( viewError.getView() == otherView )
				return viewError.getAvgError();

		if ( !foundView && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.getViewSpecificError(): Warning! " + otherView + " is not part of the structure of my view " + view );
		
		return -1;
	}

	public void resetViewSpecificError( final ViewDataBeads otherView )
	{
		boolean foundView = false;
		
		for( ViewErrorPairWise viewError : connectedViews )
		{
			if ( viewError.getView() == otherView )
			{
				viewError.setConnected( false );
				viewError.setAvgError( -1 );
				foundView = true;
			}
		}
		
		if ( !foundView && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.setViewSpecificError(): Warning! " + otherView + " is not part of the structure of my view " + view );
	}
	
	public void setViewSpecificError( final ViewDataBeads otherView, final double error )
	{
		boolean foundView = false;
		
		for( ViewErrorPairWise viewError : connectedViews )
		{
			if ( viewError.getView() == otherView )
			{
				viewError.setConnected( true );
				viewError.setAvgError( error );
				foundView = true;
			}
		}
		
		if ( !foundView && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.setViewSpecificError(): Warning! " + otherView + " is not part of the structure of my view " + view );
	}

	public void setViewConnected( final ViewDataBeads otherView, final boolean newStatus )
	{
		boolean foundView = false;
		
		for( ViewErrorPairWise viewError : connectedViews )
		{
			if ( viewError.getView() == otherView )
			{
				viewError.setConnected( newStatus );
				foundView = true;
			}
		}
		
		if ( !foundView && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.setViewConnected(): Warning! " + otherView + " is not part of the structure of my view " + view );
	}

	public void setViewError( final ViewDataBeads otherView, final double error )
	{
		boolean foundView = false;
		
		for( ViewErrorPairWise viewError : connectedViews )
		{
			if ( viewError.getView() == otherView )
			{
				viewError.setAvgError( error );
				foundView = true;
			}
		}
		
		if ( !foundView && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.setViewError(): Warning! " + otherView + " is not part of the structure of my view " + view );
	}
	
	/**
	 * Returns the number of true correspondence pairs this view shares with another view
	 * @param otherView - ViewDataBeads of the view which we potentially share true correspondences with
	 * @return the number of true corresponding pairs
	 */
	public int getNumTrueCorrespondencePairs( final ViewDataBeads otherView )
	{
		boolean partOfViewStructure = false;
		
		if ( view.getViewStructure().getViews().contains( otherView ) )
			partOfViewStructure = true;
		
		if ( !partOfViewStructure && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.getNumTrueCorrespondencePairs(): Warning! " + otherView + " is not part of the structure of my view " + view );
		
		final int otherViewID = otherView.getID();
		
    	int numCorrespondences = 0;
    	
    	for ( final Bead bead : view.getBeadStructure().getBeadList() )
    		for ( BeadIdentification beadIdentification : bead.getRANSACCorrespondence() )
    			if ( beadIdentification.getViewID() == otherViewID )
    				++numCorrespondences;
    	
    	return numCorrespondences;
	}

	/**
	 * Returns the number of correspondence candidate pairs this view shares with another view
	 * @param otherView - ViewDataBeads of the view which we potentially share correspondence candidates with
	 * @return the number of correspondence candidate pairs
	 */
	public int getNumCandidatePairs( final ViewDataBeads otherView )
	{
		boolean partOfViewStructure = false;
		
		if ( view.getViewStructure().getViews().contains( otherView ) )
			partOfViewStructure = true;
		
		if ( !partOfViewStructure && view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			IOFunctions.println( "ViewErrorStatistics.getNumCandidatePaires(): Warning! " + otherView + " is not part of the structure of my view " + view );
		
		final int otherViewID = otherView.getID();
		
    	int numCorrespondences = 0;
    	
    	for ( final Bead bead : view.getBeadStructure().getBeadList() )
    		for ( BeadIdentification beadIdentification : bead.getDescriptorCorrespondence() )
    			if ( beadIdentification.getViewID() == otherViewID )
    				++numCorrespondences;
    	
    	return numCorrespondences;
	}
	
	
	/**
	 * Returns the number of detections in this view ( result of segmentation ). These are all potential correspondence candidates.
	 * @return the number of detections
	 */
    public int getNumDetections() { return view.getBeadStructure().getBeadList().size(); }

    /**
     * Returns the amount of detections that have at least one ( or more ) corresponding candidates in other views 
     * @return number of detections
     */
    public int getNumDetectionsWithCandidates() 
    {
    	int numCandidates = 0;
    	
    	for ( final Bead bead : view.getBeadStructure().getBeadList() )
    		if ( bead.getDescriptorCorrespondence().size() > 0 )
    			++numCandidates;
    	
    	return numCandidates;
    }
    
    /**
     * Returns the overall number of correspondence candidate pairs of all the detections
     * @return number of correspondence candidate pairs
     */
    public int getNumCandidatePairs() 
    {
    	int numCandidates = 0;
    	
    	for ( final Bead bead : view.getBeadStructure().getBeadList() )
    		numCandidates += bead.getDescriptorCorrespondence().size();
    	
    	return numCandidates;
    }
    
    /**
     * Returns the overall number of true correspondence candidate pairs of all the detections
     * @return number of true correspondence candidate pairs
     */
    public int getNumTrueCorrespondencePairs()
    {
    	int numCorrespondences = 0;
    	
    	for ( final Bead bead : view.getBeadStructure().getBeadList() )
    		numCorrespondences += bead.getRANSACCorrespondence().size();
    	
    	return numCorrespondences;
    }

    /**
     * Returns the amount of detections that have at least one ( or more ) true correspondence in other views 
     * @return number of detections
     */
    public int getNumDetectionsWithTrueCorrespondences()
    {
    	int numCorrespondences = 0;
    	
    	for ( final Bead bead : view.getBeadStructure().getBeadList() )
    		if ( bead.getRANSACCorrespondence().size() > 0 )
    			++numCorrespondences;
    	
    	return numCorrespondences;
    }
    
    /**
     * Returns the average error of this view relative to all other vies
     * @return average registration error of this view
     */
    public double getAverageViewError()
    {
    	double avgError = 0;
    	int numConnectedViews = 0;

    	for ( ViewErrorPairWise error : connectedViews )
    		if ( error.isConnected() )
    		{
    			avgError += error.getAvgError();
    			++numConnectedViews;
    		}
    	
    	if ( numConnectedViews <= 0 )
    		return -1;
    	else
    		return avgError / (double)numConnectedViews;
    }
    
    /**
     * Returns the number of views that are connected, i.e. that have true correspondences and a model
     * @return number of views
     */
    public int getNumConnectedViews()
    {
    	int numConnectedViews = 0;
    	
    	for ( ViewErrorPairWise error : connectedViews )
    		if ( error.isConnected() )
    			++numConnectedViews;
    	
    	return numConnectedViews;
    }
    
    
}
