package edu.utexas.clm.reconstructreader.reconstruct;

import org.w3c.dom.Element;

public interface ContourSet {

    public void addContour(final Element e, final ReconstructSection sec);

    public String getName();

}
