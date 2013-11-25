/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.listen;


import edu.utexas.clm.archipelago.data.ClusterMessage;
import edu.utexas.clm.archipelago.network.MessageXC;

public interface TransceiverExceptionListener
{
    public void handleRXThrowable(final Throwable t, final MessageXC mxc,
                                  final ClusterMessage message);

    public void handleTXThrowable(final Throwable t, final MessageXC mxc,
                                  final ClusterMessage message);
}
