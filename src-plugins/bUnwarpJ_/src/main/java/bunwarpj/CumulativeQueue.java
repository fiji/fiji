package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ/Fiji.
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

import java.util.Vector;

/*====================================================================
|   CumulativeQueue
\===================================================================*/
/**
 * Class to create a cumulative queue in bUnwarpJ.
 */
public class CumulativeQueue extends Vector < Double >
{
    /**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -1862591645420274411L;
	/** front index of the queue */
    private int ridx;
    /** rear index of the queue */
    private int widx;
    /** current length of the queue */
    private int currentLength;
    /** queue sum */
    private double sum;

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of CumulativeQueue.
     *
     * @param length length of the queue to be created
     */
    public CumulativeQueue(int length)
    {
        currentLength=ridx=widx=0; setSize(length);
    }

    /*------------------------------------------------------------------*/
    /**
     * Get the current size of the queue.
     *
     * @return current size
     */
    public int currentSize(){return currentLength;}

    /*------------------------------------------------------------------*/
    /**
     * Get the sum of the queue.
     *
     * @return sum
     */
    public double getSum(){return sum;}

    /*------------------------------------------------------------------*/
    /**
     * Pop the value from the front of the queue.
     *
     * @return front value
     */
    public double pop_front()
    {
       if (currentLength==0)
           return 0.0;
       double x=((Double)elementAt(ridx)).doubleValue();
       currentLength--;
       sum-=x;
       ridx++;
       if (ridx==size())
           ridx=0;
       return x;
    }

    /*------------------------------------------------------------------*/
    /**
     * Push a value at the end of the queue.
     */
    public void push_back(double x)
    {
       if (currentLength==size())
           pop_front();
       setElementAt(new Double(x),widx);
       currentLength++;
       sum+=x;
       widx++;
       if (widx==size())
           widx=0;
    }

} /* end class CumulativeQueue */
