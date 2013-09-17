package edu.utexas.clm.reconstructreader.trakem2;

public interface SeriesTemplate
{
    public String getValue(final String key);
    
    public void setContourText(final StringBuilder sb);
}
