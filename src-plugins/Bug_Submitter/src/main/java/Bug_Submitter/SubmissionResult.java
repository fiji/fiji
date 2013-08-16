/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006 - 2013 Mark Longair */

/*
  This file is part of the ImageJ plugin "Bug_Submitter".

  The ImageJ plugin "Bug_Submitter" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Bug_Submitter" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package Bug_Submitter;

class SubmissionResult {
	public SubmissionResult( int resultCode,
				 int bugNumber,
				 String bugURL,
				 String authenticationResultPage,
				 String submissionResultPage ) {
		this.resultCode = resultCode;
		this.bugNumber = bugNumber;
		this.bugURL = bugURL;
		this.authenticationResultPage = authenticationResultPage;
		this.submissionResultPage = submissionResultPage;
	}
	int resultCode;
	int bugNumber;
	String bugURL;
	String authenticationResultPage;
	String submissionResultPage;
}
