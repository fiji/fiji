/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2011 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple_Neurite_Tracer".

  The ImageJ plugin "Simple_Neurite_Tracer" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  The ImageJ plugin "Simple_Neurite_Tracer" is distributed in the hope
  that it will be useful, but WITHOUT ANY WARRANTY; without even the
  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
  PURPOSE.  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.IJ;
import java.util.ArrayList;

/** An implementation of the MultiTaskProgress interface that
    updates the ImageJ progress bar */

public class FittingProgress implements MultiTaskProgress {
	ArrayList<Double> tasksProportionsDone;
	int totalTasks;

	public FittingProgress( int totalTasks ) {
		tasksProportionsDone =
			new ArrayList<Double>();
		this.totalTasks = totalTasks;
		for( int i = 0; i < totalTasks; ++i )
			tasksProportionsDone.add(0.0);
	}

	synchronized public void updateProgress(double proportion, int taskIndex) {
		tasksProportionsDone.set(taskIndex,proportion);
		updateStatus();
	}

	protected void updateStatus() {
		double totalDone = 0;
		for( double p : tasksProportionsDone )
			totalDone += p;
		IJ.showProgress(totalDone/totalTasks);
	}

	public void done() {
		IJ.showProgress(1.0);
	}
}
