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

package edu.utexas.clm.archipelago.example;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.compute.SerializableCallable;

import ij.IJ;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An example that demonstrates one potential peril of the Cluster ExecutorService.
 * The assumption of deep equality across submission of a Callable is broken when
 * using Cluster.submit() because it relies on Serialization, unlike any other typical
 * ExecutorService. In other words, a submitted Callable is Serialized, then sent over
 * a network stream to the remote machine on which is it call()'ed. The result of that
 * call is Serialized remotely, then returned, also via a network stream. This means
 * that even though new is not explicitly called, the get() method on the corresponding
 * Future will return a freshly minted object, whose member variables will also be freshly
 * minted, and so forth.
 * 
 * Call run() either with or without a Cluster running to see the difference. This class
 * also illustrates a potential workaround for this problem, using a HashMap.
 */
public class Equality_Example implements PlugIn
{

    public static class EqCall implements SerializableCallable<ArrayList<Float>>
    {
        private final ArrayList<Float> floats;
        
        public EqCall(final ArrayList<Float> floats)
        {
            this.floats = floats;
        }

        public ArrayList<Float> call() throws Exception {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException ie)
            {/**/}

            return floats;
        }
    }

        
    private final HashMap<Integer, Float> ifmap = new HashMap<Integer, Float>();
    private final HashMap<Float, Integer> fimap = new HashMap<Float, Integer>();
    private final HashMap<Float, Float> ffmap = new HashMap<Float, Float>();
    private final AtomicInteger uid = new AtomicInteger(0);

    public void run(String arg)
    {
        final ExecutorService executorService =
                Cluster.activeCluster() ?
                        Cluster.getCluster().getService(1) :
                        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final ArrayList<Future<ArrayList<Float>>> futures = new ArrayList<Future<ArrayList<Float>>>();

        for (int i = 0; i < 128; ++i)
        {
            final ArrayList<Float> floats = new ArrayList<Float>();
            final Float f = new Float(i);
            final Integer id = uid.getAndIncrement();
            
            floats.add(f);
            ifmap.put(id, f);
            fimap.put(f, id);
            ffmap.put(f, f);
            futures.add(executorService.submit(new EqCall(floats)));
        }
        
        try
        {
            for (Future<ArrayList<Float>> fu : futures)
            {
                Float fc = fu.get().get(0);
                //Integer i = fimap.get(fc);
                //Float fo = i == null ? null : ifmap.get(i);
                Float fo = ffmap.get(fc);
                String eqstr = fo == fc ? "they are equal" : "they are unequal";
                IJ.log("Got back float " + fc + " keyed to " + fo + " and " + eqstr);
            }
        }
        catch (InterruptedException ie)
        {
            IJ.log("Woops: " + ie);
        }
        catch (ExecutionException ee)
        {
            IJ.log("Woops: " + ee);
        }
    }
}
