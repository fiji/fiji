/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2011 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.IJ;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/** There have been problems on Mac OS with people trying to start the
  * Sholl analysis interface, but while the focus isn't on the image
  * window.  This is just a key listener to detect such attempts and
  * suggest to people what might be wrong if they type Shift with
  * Control-A or Alt-A in the wrong window.  (This will be added to
  * all the Wrong Windows.) */

public class ClarifyingKeyListener implements KeyListener, ContainerListener {

	/* Grabbing all key presses in a dialog window isn't trivial,
           but the technique suggested here works fine:
           http://www.javaworld.com/javaworld/javatips/jw-javatip69.html */

	public void addKeyAndContainerListenerRecursively(Component c) {
		c.addKeyListener(this);
		if(c instanceof Container) {
			Container container = (Container)c;
			container.addContainerListener(this);
			Component[] children = container.getComponents();
			for(int i = 0; i < children.length; i++){
				addKeyAndContainerListenerRecursively(children[i]);
			}
		}
	}

	private void removeKeyAndContainerListenerRecursively(Component c) {
		c.removeKeyListener(this);
		if(c instanceof Container){
			Container container = (Container)c;
			container.removeContainerListener(this);
			Component[] children = container.getComponents();
			for(int i = 0; i < children.length; i++){
				removeKeyAndContainerListenerRecursively(children[i]);
			}
		}
	}

	@Override
	public void componentAdded(ContainerEvent e) {
		addKeyAndContainerListenerRecursively(e.getChild());
	}

	@Override
	public void componentRemoved(ContainerEvent e) {
		removeKeyAndContainerListenerRecursively(e.getChild());
	}

	@Override
	public void keyPressed(KeyEvent e) {

		int keyCode = e.getKeyCode();

		if( e.isShiftDown() && (e.isControlDown() || e.isAltDown()) && (keyCode == KeyEvent.VK_A) ) {
			IJ.error("You seem to be trying to start Sholl analysis, but the focus is on the wrong window.\n"+
				 "Bring the (2D) image window to the foreground and try again.");
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

}
