/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

public interface FineTuneProgressListener {

	static final int COMPLETED   = 1;
	static final int CANCELLED   = 2;

	public void fineTuneNewBestResult( RegistrationResult result );

	public void fineTuneThreadFinished( int reason, RegistrationResult result, FineTuneThread fineTuneThread );

}
