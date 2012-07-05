/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2010 Mark Longair */

/*
  This file is part of the ImageJ plugin "Frangi_".

  The ImageJ plugin "Frangi_" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  The ImageJ plugin "Frangi_" is distributed in the hope that it
  will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fiji.features;

public interface MultiTaskProgress {

	public void updateProgress( double proportionDone, int taskIndex );

	public void done();

}
