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

package edu.utexas.clm.archipelago.compute;

import java.util.concurrent.Callable;

public class QuickRunnable implements Runnable
{
    
    private final Callable callable;
    private Object result;
    private Exception exception;
    
    public QuickRunnable(Callable c)
    {
        callable = c;
        result = null;
        exception = null;
    }
    
    public void run()
    {
        try
        {
            result = callable.call();
        }
        catch (Exception e)
        {
            exception = e;
        }
    }
    
    public Object getResult()
    {
        return result;
    }
    
    public Exception getException()
    {
        return exception;
    }
}
