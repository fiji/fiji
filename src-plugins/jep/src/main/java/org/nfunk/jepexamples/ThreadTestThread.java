/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jepexamples;


/**
 * The ThreadTestThread waits for 5 seconds before calling the evaluate method
 * of the ThreadTest instance.
 * <p>
 * Thanks to Matthew Baird and Daniel Teng for this code.
 */
public class ThreadTestThread extends Thread
{
    ThreadTest test;

    public ThreadTestThread(ThreadTest test_in)
    {
        test = test_in;
    }

    public void run() {

        try {
            Thread.sleep(5000);
            test.evaluate();
            Thread.yield();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}
