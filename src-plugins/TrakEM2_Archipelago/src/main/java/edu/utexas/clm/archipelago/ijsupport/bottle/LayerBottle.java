package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.ijsupport.TrakEM2Archipelago;
import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import ini.trakem2.Project;
import ini.trakem2.display.Layer;
import ini.trakem2.persistence.DBObject;

import java.io.File;

/**
 *
 */
public class LayerBottle implements Bottle<Layer>
{
    private final long id;
    private final File file;

    public LayerBottle(final Layer l)
    {
        file = TrakEM2Archipelago.getFile(l.getProject());
        id = l.getId();

    }

    public Layer unBottle(final MessageXC xc)
    {
        final Project p = TrakEM2Archipelago.getProject(file);
        DBObject db = p.findById(id);
        if (db instanceof Layer)
        {
            return (Layer)db;
        }
        else
        {
            return null;
        }
    }
}
