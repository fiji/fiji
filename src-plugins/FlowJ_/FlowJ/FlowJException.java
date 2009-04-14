package FlowJ;
import java.awt.*;
import ij.*;

/*
	 This is a exception class for optical flow routines.

	 Copyright (c) 1999, Michael Abramoff. All rights reserved.

	 Author: Michael Abramoff,
			  c/o Image Sciences Institute
			  University Medical Center Utrecht
			  Netherlands

	 Small print:
	 Permission to use, copy, modify and distribute this version of this software or any parts
	 of it and its documentation or any parts of it ("the software"), for any purpose is
	 hereby granted, provided that the above copyright notice and this permission notice
	 appear intact in all copies of the software and that you do not sell the softwaret,
	 or include the software in a commercial package.
	 The release of this software into the public domain does not imply any obligation
	 on the part of the author to release future versions into the public domain.
	 The author is free to make upgraded or improved versions of the software available
	 for a fee or commercially only.
	 Commercial licensing of the software is available by contacting the author.
	 THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
	 EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
	 WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
*/

public class FlowJException extends Exception {

   public int info;
   private String string;

   public FlowJException(int info) {

	  this.info = info;

   }

   public FlowJException(String problem) {
	  super(problem);
	  string = problem;

   }

   public FlowJException() {

   }
   public String toString() { return string; }
}


