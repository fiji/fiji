package edu.utexas.archipelago.image;


import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.compute.SerializableCallable;
import edu.utexas.clm.archipelago.data.FileChunk;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.*;


/**
 * A FijiArchipelago-compatible class for splitting an image into smaller blocks, then
 * recombining into the original image shape, potentially after processing.
 *
 */
public class ImageBlockDeblock implements Serializable
{
    public class BlockCallable implements Callable<FileChunk>
    {
        final int[][] pix;
        final int x0, x1, y0, y1;
        final ImageProcessor ip;

        public BlockCallable(final int x0, final int x1, final int y0, final int y1,
                             final int[][] pix, ImageProcessor ip, FileChunk inputFile)
        {
            this.pix = pix;
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
            this.ip = ip;
        }

        private String blockFilePath(final int w, final int h, final int x, final int y)
        {
            File inFile = new File(inputFile.getData()).getAbsoluteFile();
            String afterSlashName = inFile.getName();
            String directoryName = inFile.getParent() + "/";
            String blockDirectoryName = directoryName + subfolder;
            File blockFolder = new File(blockDirectoryName);

            int lastDot = afterSlashName.lastIndexOf('.');
            String blockName = afterSlashName.substring(0, lastDot) +
                    "_" + w + "x" + h + "_" + x + "_" + y + ".tif";
            

            if (!blockFolder.exists() && !blockFolder.mkdirs())
            {
                if (!blockFolder.exists())
                {
                    FijiArchipelago.debug("Could not create directory " + blockDirectoryName);
                    blockDirectoryName = directoryName;
                }
            }

            return blockDirectoryName + blockName;
        }

        public FileChunk call() throws Exception
        {
            final int w = x1 - x0, h = y1 - y0;
            final int[][] pixBlock = new int[w][h];
            final ImageProcessor ipBlock = ip.createProcessor(w, h);
            final String filePath = blockFilePath(w, h, x0, y0);

            for (int x = 0; x < w; ++x)
            {
                for (int y = 0; y < h; ++y)
                {
                    pixBlock[x][y] = pix[x + x0][y + y0];
                }
            }

            ipBlock.setIntArray(pixBlock);

            IJ.save(new ImagePlus(filePath, ipBlock), filePath);
            
            if (!(new File(filePath).exists()))
            {
                throw new IOException("Could not write file to " + filePath);
            }
            else
            {
                FijiArchipelago.log("Successfully wrote file " + filePath);
            }

            return new FileChunk(filePath, inputFile);
        }
    }

    /**
     * Contains input file path
     */
    private final FileChunk inputFile;
    /**
     * Contains output file path
     */
    private final FileChunk outputFile;
    /**
     * List of files containing image blocks.
     */
    private final ArrayList<FileChunk> blockFiles;

    /**
     * Corresponding list of bounding box maps. Each array is like
     * [x0, y0, xb0, yb0, w, h], where the pixel at (x0,  y0) in the combined image corresponds to
     * the pixel at (xb0, yb0) in the block image. w and h represent the width and height of the
     * valid window in the block image. This is at most the block size, but may be smaller, and
     * excludes the overlap region.
     */
    private final ArrayList<int[]> boxMap;
    /**
     * Requested size of the block images.
     */
    private final int[] blockSize;
    /**
     * Stores the overlap size, halved.
     */
    private final int[] halfOverlap;
    /**
     * Since we divide the overlap in two and store as int[]. we also store a correction value
     * which is zero if overlap is even and 1 if odd.
     */
    private final int[] ovlpCorrection;
    /**
     * Maps input block image files to output block image files.
     */
    private final ArrayList<Hashtable<FileChunk, FileChunk>> chunkMaps;
    
    private final int n;

    /**
     * Width and height of the input image.
     */
    private int w = -1, h = -1;

    private final ImageBlockDeblock self;

    private String subfolder;
    
    public ImageBlockDeblock(final File inFile, final File outFile, final int[] bs, final int[] o)
    {
        this(inFile, outFile, bs, o, 1);
    }

    /**
     * Creates an ImageBlockDeblock that will split a file into blocks of size at most bs, plus
     * an overlap determined by o.
     * @param inFile input file.
     * @param outFileFormat re-combined output file format string.
     * @param bs a 2-element array containing the size of the blocks, not including overlap.
     * @param o the total block overlap in pixels. Each image block will be of size at most bs + o.
     */
    public ImageBlockDeblock(final File inFile, final File outFileFormat,
                             final int[] bs, final int[] o, int nOutput)
    {
        n = nOutput;
        inputFile = new FileChunk(inFile.getAbsolutePath());
        outputFile = new FileChunk(outFileFormat.getAbsolutePath(), inputFile);
        blockSize = bs;        
        blockFiles = new ArrayList<FileChunk>();
        boxMap = new ArrayList<int[]>();
        chunkMaps = new ArrayList<Hashtable<FileChunk, FileChunk>>(nOutput);
        halfOverlap = new int[2];
        ovlpCorrection = new int[2];
        
        halfOverlap[0] = o[0] / 2;
        halfOverlap[1] = o[1] / 2;

        // If o contains odd numbers, we have to remember to add 1 to the block range.
        ovlpCorrection[0] = 2 * halfOverlap[0] == o[0] ? 0 : 1;
        ovlpCorrection[1] = 2 * halfOverlap[1] == o[1] ? 0 : 1;

        self = this;

        subfolder = "blocks/";
        
        for (int i = 0; i < nOutput; ++i)
        {
            chunkMaps.add(new Hashtable<FileChunk, FileChunk>());
        }
    }

    /**
     * Splits the input file into blocks. Computation is done in parallel.
     * @return An ArrayList containing FileChunks representing the file paths for each image block file.
     * @throws ExecutionException if an ExecutionError is encountered
     * @throws InterruptedException if this method is interrupted,
     */
    public ArrayList<FileChunk> blockFile() throws ExecutionException, InterruptedException
    {
        FijiArchipelago.debug("Splitting image " + inputFile + " into blocks");
        ImageProcessor ip = IJ.openImage(inputFile.getData()).getProcessor();
        w = ip.getWidth();
        h = ip.getHeight();
        int cblock = (int)Math.ceil((float)w / (float)blockSize[0]);
        int rblock = (int)Math.ceil((float)h / (float)blockSize[1]);

        if (rblock <= 1 && cblock <= 1)
        {
            blockFiles.add(inputFile);
        }
        else
        {
            final ExecutorService localService =
                    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final int[][] pix = ip.getIntArray();
            final ArrayList<Future<FileChunk>> futures = new ArrayList<Future<FileChunk>>();                    
            
            for (int c = 0; c < cblock; c++)
            {
                for (int r = 0; r < rblock; r++)
                {
                    final int x0 = c * blockSize[0];
                    final int x1 = Math.min(w, x0 + blockSize[0]);
                    final int xo0 = Math.max(0, x0 - halfOverlap[0]);
                    final int xo1 = Math.min(w, x1 + halfOverlap[0] + ovlpCorrection[0]);
                    
                    final int y0 = r * blockSize[1];
                    final int y1 = Math.min(h, y0 + blockSize[1]);
                    final int yo0 = Math.max(0, y0 - halfOverlap[1]);
                    final int yo1 = Math.min(h, y1 + halfOverlap[1] + ovlpCorrection[1]);

                    boxMap.add(new int[]{x0, y0, x0 - xo0, y0 - yo0, x1 - x0, y1 - y0});
                    futures.add(localService.submit(
                            new BlockCallable(xo0, xo1, yo0, yo1, pix, ip, inputFile)));
                }
            }
            
            for (Future<FileChunk> future : futures)
            {
                FileChunk file = future.get();
                blockFiles.add(file);
            }
        }
        FijiArchipelago.debug("Done with image " + inputFile);
        return blockFiles;
    }


    /**
     * Recombine image blocks into the original image shape.
     * @param idx the slice index to deblock
     * @return a FileChunk containing the output file.
     * @throws ExecutionException if something goes wrong, probably file IO related
     */
    public FileChunk deblockFile(final int idx) throws ExecutionException
    {
        if (blockFiles.isEmpty())
        {
            throw new ExecutionException(
                    new Exception("There are no blockfiles to combine." +
                    " Have you run blockFiles() yet?"));
        }
        
        if (idx >= n)
        {
            throw new ExecutionException(badIndexException(idx));
        }

        if (blockFiles.size() == 1)
        {
            final ImagePlus imp = IJ.openImage(getChunkOrException(idx, 0).getData());
            IJ.save(imp, getOutputFileName(idx));
        }
        else
        {
            final ImageProcessor blockIpZero =
                    getFirstSliceIP(getChunkOrException(idx, 0).getData());
            final ImageProcessor ipOut = blockIpZero.createProcessor(w, h);
            final int[][] pix = new int[w][h];

            for (int i = 0; i < blockFiles.size(); ++i)
            {
                final FileChunk outChunk = getChunkOrException(idx, i);
                final int[] bmap = boxMap.get(i);

                final ImageProcessor blockIp = i == 0 ? blockIpZero :
                        getFirstSliceIP(outChunk.getData());
                final int[][] blockPix = blockIp.getIntArray();

                for (int x = 0; x < bmap[4]; ++x)
                {
                    for (int y = 0; y < bmap[5]; ++y)
                    {
                        pix[x + bmap[0]][y + bmap[1]] = blockPix[x + bmap[2]][y + bmap[3]];
                    }
                }
            }

            ipOut.setIntArray(pix);

            IJ.save(new ImagePlus(getOutputFileName(idx), ipOut), getOutputFileName(idx));
        }
        return outputFile;
    }

    /**
     * Map a new block file of the same size to the original one given. The new block file will
     * be used in the output. This method is intended for use in the case that a block image
     * file has been opened, processed, then written to a new file.
     * @param origChunk the FileChunk representing the original image block file, created by
     *                  blockImage().
     * @param newChunk the FileChunk representing a new image block file representing the output of
     *                 processing on the original block image.
     */
    public void mapChunks(final FileChunk origChunk, final FileChunk newChunk)
    {
        mapChunks(origChunk, newChunk, 0);
    }

    
    public void mapChunks(final FileChunk origChunk, final FileChunk newChunk, final int idx)
    {
        checkIdx(idx);

        if (blockFiles.contains(origChunk))
        {
            Hashtable<FileChunk, FileChunk> chunkTable = getTable(idx);

            chunkTable.put(origChunk, newChunk);
            
            FijiArchipelago.debug("Map " + idx + " " + origChunk + " <-> " + newChunk);
        }
    }

    public void setSubfolder(final String folder)
    {
        subfolder = folder;
        if (!subfolder.isEmpty() && !subfolder.endsWith("/"))
        {
            subfolder += "/";
        }
    }

    public FileChunk getInputFile()
    {
        return inputFile;
    }

    public FileChunk getOutputFile(int idx)
    {
        return new FileChunk(getOutputFileName(idx), outputFile);
    }

    public String getOutputFileName(int idx)
    {
        return (String.format(outputFile.getData(), idx));
    }

    public Callable<ImageBlockDeblock> imageBlockCallable()
    {
        return new SerializableCallable<ImageBlockDeblock>()
        {
            public ImageBlockDeblock call() throws Exception
            {
                blockFile();
                return self;
            }
        };
    }

    public Callable<ImageBlockDeblock> imageDeblockCallable()
    {
        return new SerializableCallable<ImageBlockDeblock>()
        {
            public ImageBlockDeblock call() throws Exception
            {
                for (int i = 0; i < chunkMaps.size(); ++i)
                {
                    deblockFile(i);
                }
                return self;
            }
        };
    }
    
    public ArrayList<FileChunk> imageBlockFiles()
    {
        return new ArrayList<FileChunk>(blockFiles);
    }
    
    public int numUnmappedBlocks()
    {
        return numUnmappedBlocks(0);
    }
    
    public int numUnmappedBlocks(final int idx)
    {
        return blockFiles.size() - chunkMaps.get(idx).size();
    }
    
    public int getWidth()
    {
        return w;
    }
    
    public int getHeight()
    {
        return h;
    }
    
    public int getNumOutput()
    {
        return chunkMaps.size();
    }
    
    private Hashtable<FileChunk, FileChunk> getTable(final int idx)
    {
        return chunkMaps.get(idx);
    }
    
    private FileChunk getChunk(final int idx, final int chunkIdx)
    {
        return getTable(idx).get(blockFiles.get(chunkIdx));
    }

    private FileChunk getChunkOrException(final int idx, final int chunkIdx)
            throws ExecutionException
    {
        final FileChunk fc = getChunk(idx, chunkIdx);
        if (fc == null)
        {
            throw new ExecutionException(new Exception("No file " + chunkIdx +" for index " + idx));
        }
        return fc;
    }

    private ImageProcessor getFirstSliceIP(final String filename)
    {
        final ImagePlus imp = IJ.openImage(filename);
        final ImageStack is = imp.getImageStack();
        final ImageProcessor ip = is.getProcessor(1);
        return ip == null ? imp.getProcessor() : ip;
    }

    private void checkIdx(int idx)
    {
        if (idx >= n)
        {
            throw badIndexException(idx);
        }
    }
    
    private RuntimeException badIndexException(int idx)
    {
        return new RuntimeException("This ImageBlockDeblock handles " + n +
                " 0-indexed output images. Image " + idx + " was requested.");
    }
}
