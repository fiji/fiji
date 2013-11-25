package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.ijsupport.TrakEM2Archipelago;
import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import ini.trakem2.Project;
import ini.trakem2.display.Patch;

import java.io.File;

/**
 *
 */
public class PatchBottle implements Bottle<Patch>
{
    private final File projectFile;
    private final long id;

    public PatchBottle(final Patch patch)
    {
        projectFile = TrakEM2Archipelago.getFile(patch.getProject());
        id = patch.getId();
    }

    public Patch unBottle(final MessageXC xc)
    {
        final Project p = TrakEM2Archipelago.getProject(projectFile);
        return (Patch)p.findById(id);
    }
}
