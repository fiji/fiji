package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import mpicbg.models.Point;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A PointBottle to support synchronized point objects across cluster nodes
 */
public class PointBottle implements Bottle<Point>
{
    private static final Map<Integer, Point> idPointMap =
            Collections.synchronizedMap(new HashMap<Integer, Point>());
    /*
    As of this writing, Point has no .equals() nor .hashCode(), meaning this map uses the
    system default hashCode, and instance equality, which is exactly what we want.
    TODO: Consider using IdentityHashMap, to be future-proof.
     */
    private static final Map<Point, Integer> pointIdMap =
            Collections.synchronizedMap(new HashMap<Point, Integer>());
    private static final ReentrantLock idPointLock = new ReentrantLock();
    private static final ReentrantLock idIdLock = new ReentrantLock();
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    private static void mapIdToPoint(final Point point, final int original)
    {
        idIdLock.lock();

        pointIdMap.put(point, original);

        idIdLock.unlock();
    }

    private static int getId(final Point point, final int idDefault)
    {
        final Integer id;
        idIdLock.lock();

        id = pointIdMap.get(point);

        idIdLock.unlock();

        return id == null ? idDefault : id;
    }

    private static void mapPointToID(final int orig, final Point point)
    {
        idPointLock.lock();

        idPointMap.put(orig, point);

        idPointLock.unlock();
    }

    public static Point getPoint(final int orig, final Point localPoint)
    {
        final Point point;
        idPointLock.lock();
        point = idPointMap.get(orig);
        idPointLock.unlock();

        return point == null ? localPoint : point;
    }

    private final float[] w, l;
    private final int id;
    private final boolean fromOrigin;

    /**
     * Creates a Bottle containing a Point
     * @param point the Point to bottle
     * @param isOrigin true if we're bottling from the root-node perspective, false if from the
     *                 client-node perspective.
     */
    public PointBottle(final Point point, boolean isOrigin)
    {
        // This constructor should only be called from PointBottler.bottle, which is sync'ed.

        // Assume identityHashCode does not change for a given Object
        int idHash = System.identityHashCode(point);

        w = point.getW();
        l = point.getL();
        fromOrigin = isOrigin;

        if (fromOrigin)
        {
            /*
            Sending from root node to client node
            */

            //Check to see if we've sent this point before.
            if (pointIdMap.containsKey(point))
            {
                // If we have, just re-use the existing id.
                id = getId(point, idHash);
            }
            else
            {
                // If we haven't, generate a new id...
                id = idGenerator.getAndIncrement();
                // Map that id to the idHash
                mapIdToPoint(point, id);
                // Map the point to the id.
                mapPointToID(id, point);
            }
        }
        else
        {
            /*
            Sending from client node to root node
            */
            id = getId(point, idHash);
        }
    }

    public Point unBottle(final MessageXC xc)
    {
        final Point point = new Point(l, w);

        if (fromOrigin)
        {
            // Operating on a client node
            if (idPointMap.containsKey(id))
            {
                /*
                If we've already seen this point, return the new point that was generated last time.
                 */
                return getPoint(id, point);
            }
            else
            {
                /*
                Otherwise, map the new id to the original id
                */
                mapIdToPoint(point, id);
                /*
                Map the original id to the new point so that we can retrieve it if the original id
                comes through again.
                 */
                mapPointToID(id, point);

                return point;
            }
        }
        else
        {
            /*
            This is a copy of the original point. Retrieve the original and set its values to the
            one we've just recieved.
             */
            final Point origPoint = getPoint(id, point);
            syncPoint(origPoint, point);
            return origPoint;
        }
    }

    private static synchronized void syncPoint(final Point to, final Point from)
    {
        if (from != null && to != from)
        {
            final float[] wTo = to.getW(), wFrom = from.getW(),
                    lTo = to.getL(), lFrom = from.getL();

            for (int j = 0; j < wTo.length; ++j)
            {
                wTo[j] = wFrom[j];
            }
            for (int j = 0; j < lTo.length; ++j)
            {
                lTo[j] = lFrom[j];
            }
        }
    }
}
