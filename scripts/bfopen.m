function [result] = bfopen(id)
% A script for opening microscopy images in MATLAB using Bio-Formats.
%
% The function returns a list of image series; i.e., a cell array of cell
% arrays of (matrix, label) pairs, with each matrix representing a single
% image plane, and each inner list of matrices representing an image
% series. See below for examples of usage.
%
% Portions of this code were adapted from:
% http://www.mathworks.com/support/solutions/en/data/1-2WPAYR/
%
% This method is ~1.5x-2.5x slower than Bio-Formats's command line
% showinf tool (MATLAB 7.0.4.365 R14 SP2 vs. java 1.6.0_20),
% due to overhead from copying arrays.
%
% Thanks to all who offered suggestions and improvements:
%     * Ville Rantanen
%     * Brett Shoelson
%     * Martin Offterdinger
%     * Tony Collins
%     * Cris Luengo
%     * Arnon Lieber
%     * Jimmy Fong
%
% NB: Internet Explorer sometimes erroneously renames the Bio-Formats library
%     to loci_tools.zip. If this happens, rename it back to loci_tools.jar.
%
% Here are some examples of accessing data using the bfopen function:
%
%     % read the data using Bio-Formats
%     data = bfopen('C:/data/experiment.lif');
%
%     % unwrap some specific image planes from the result
%     numSeries = size(data, 1);
%     series1 = data{1, 1};
%     series2 = data{2, 1};
%     series3 = data{3, 1};
%     metadataList = data{1, 2};
%     % ...etc.
%     series1_numPlanes = size(series1, 1);
%     series1_plane1 = series1{1, 1};
%     series1_label1 = series1{1, 2};
%     series1_plane2 = series1{2, 1};
%     series1_label2 = series1{2, 2};
%     series1_plane3 = series1{3, 1};
%     series1_label3 = series1{3, 2};
%     % ...etc.
%
%     % plot the 1st series's 1st image plane in a new figure
%     series1_colorMaps = data{1, 3};
%     figure('Name', series1_label1);
%     if isempty(series1_colorMaps{1})
%         colormap(gray);
%     else
%         colormap(series1_colorMaps{1});
%     end
%     imagesc(series1_plane1);
%
%     % Or if you have the image processing toolbox, you could use:
%     % imshow(series1_plane1, []);
%
%     % Or animate as a movie (assumes 8-bit unsigned data)
%     v = linspace(0, 1, 256)';
%     cmap = [v v v];
%     for p = 1:series1_numPlanes
%         M(p) = im2frame(uint8(series1{p, 1}), cmap);
%     end
%     movie(M);
%
%     % Query some metadata fields (keys are format-dependent)
%     subject = metadataList.get('Subject');
%     title = metadataList.get('Title');

% -- Configuration - customize this section to your liking --

% Toggle the autoloadBioFormats flag to control automatic loading
% of the Bio-Formats library using the javaaddpath command.
%
% For static loading, you can add the library to MATLAB's class path:
%     1. Type "edit classpath.txt" at the MATLAB prompt.
%     2. Go to the end of the file, and add the path to your JAR file
%        (e.g., C:/Program Files/MATLAB/work/loci_tools.jar).
%     3. Save the file and restart MATLAB.
%
% There are advantages to using the static approach over javaaddpath:
%     1. If you use bfopen within a loop, it saves on overhead
%        to avoid calling the javaaddpath command repeatedly.
%     2. Calling 'javaaddpath' may erase certain global parameters.
autoloadBioFormats = 1;

% Toggle the stitchFiles flag to control grouping of similarly
% named files into a single dataset based on file numbering.
stitchFiles = 0;

% To work with compressed Evotec Flex, fill in your LuraWave license code.
%lurawaveLicense = 'xxxxxx-xxxxxxx';

% -- Main function - no need to edit anything past this point --

% load the Bio-Formats library into the MATLAB environment
if autoloadBioFormats
    path = fullfile(fileparts(mfilename('fullpath')), '../plugins/loci_tools.jar');
    javaaddpath(path);
end

% set LuraWave license code, if available
if exist('../jars/lurawaveLicense')
    path = fullfile(fileparts(mfilename('fullpath')), '../jars/lwf_jsdk2.6.jar');
    javaaddpath(path);
    java.lang.System.setProperty('lurawave.license', fullfile(fileparts(mfilename('fullpath')), '../jars/lurawaveLicense'));
end

% check MATLAB version, since typecast function requires MATLAB 7.1+
canTypecast = versionCheck(version, 7, 1);

% check Bio-Formats version, since makeDataArray2D function requires trunk
bioFormatsVersion = char(loci.formats.FormatTools.VERSION);
isBioFormatsTrunk = versionCheck(bioFormatsVersion, 5, 0);

% initialize logging
loci.common.DebugTools.enableLogging('INFO');

r = loci.formats.ChannelFiller();
r = loci.formats.ChannelSeparator(r);
if stitchFiles
    r = loci.formats.FileStitcher(r);
end

tic
r.setMetadataStore(loci.formats.MetadataTools.createOMEXMLMetadata());
r.setId(id);
numSeries = r.getSeriesCount();
result = cell(numSeries, 2);
for s = 1:numSeries
    fprintf('Reading series #%d', s);
    r.setSeries(s - 1);
    width = r.getSizeX();
    height = r.getSizeY();
    pixelType = r.getPixelType();
    bpp = loci.formats.FormatTools.getBytesPerPixel(pixelType);
    fp = loci.formats.FormatTools.isFloatingPoint(pixelType);
    sgn = loci.formats.FormatTools.isSigned(pixelType);
    bppMax = power(2, bpp * 8);
    little = r.isLittleEndian();
    numImages = r.getImageCount();
    imageList = cell(numImages, 2);
    colorMaps = cell(numImages);
    for i = 1:numImages
        if mod(i, 72) == 1
            fprintf('\n    ');
        end
        fprintf('.');
        plane = r.openBytes(i - 1);

        % retrieve color map data
        if bpp == 1
            colorMaps{s, i} = r.get8BitLookupTable()';
        else
            colorMaps{s, i} = r.get16BitLookupTable()';
        end
        if ~isempty(colorMaps{s, i})
            newMap = colorMaps{s, i};
            m = newMap(row, col) < 0;
            newMap(m) = newMap(m) + bppMax;
            colorMaps{s, i} = newMap / (bppMax - 1);
        end

        % convert byte array to MATLAB image
        if isBioFormatsTrunk && (sgn || ~canTypecast)
            % can get the data directly to a matrix
            arr = loci.common.DataTools.makeDataArray2D(plane, ...
                bpp, fp, little, height);
        else
            % get the data as a vector, either because makeDataArray2D
            % is not available, or we need a vector for typecast
            arr = loci.common.DataTools.makeDataArray(plane, ...
                bpp, fp, little);
        end

        % Java does not have explicitly unsigned data types;
        % hence, we must inform MATLAB when the data is unsigned
        if ~sgn
            if canTypecast
                % TYPECAST requires at least MATLAB 7.1
                % NB: arr will always be a vector here
                switch class(arr)
                    case 'int8'
                        arr = typecast(arr, 'uint8');
                    case 'int16'
                        arr = typecast(arr, 'uint16');
                    case 'int32'
                        arr = typecast(arr, 'uint32');
                    case 'int64'
                        arr = typecast(arr, 'uint64');
                end
            else
                % adjust apparent negative values to actual positive ones
                % NB: arr might be either a vector or a matrix here
                mask = arr < 0;
                adjusted = arr(mask) + bppMax / 2;
                switch class(arr)
                    case 'int8'
                        arr = uint8(arr);
                        adjusted = uint8(adjusted);
                    case 'int16'
                        arr = uint16(arr);
                        adjusted = uint16(adjusted);
                    case 'int32'
                        arr = uint32(arr);
                        adjusted = uint32(adjusted);
                    case 'int64'
                        arr = uint64(arr);
                        adjusted = uint64(adjusted);
                end
                adjusted = adjusted + bppMax / 2;
                arr(mask) = adjusted;
            end
        end

        if isvector(arr)
            % convert results from vector to matrix
            shape = [width height];
            arr = reshape(arr, shape)';
        end

        % build an informative title for our figure
        label = id;
        if numSeries > 1
            qs = int2str(s);
            label = [label, '; series ', qs, '/', int2str(numSeries)];
        end
        if numImages > 1
            qi = int2str(i);
            label = [label, '; plane ', qi, '/', int2str(numImages)];
            if r.isOrderCertain()
                lz = 'Z';
                lc = 'C';
                lt = 'T';
            else
                lz = 'Z?';
                lc = 'C?';
                lt = 'T?';
            end
            zct = r.getZCTCoords(i - 1);
            sizeZ = r.getSizeZ();
            if sizeZ > 1
                qz = int2str(zct(1) + 1);
                label = [label, '; ', lz, '=', qz, '/', int2str(sizeZ)];
            end
            sizeC = r.getSizeC();
            if sizeC > 1
                qc = int2str(zct(2) + 1);
                label = [label, '; ', lc, '=', qc, '/', int2str(sizeC)];
            end
            sizeT = r.getSizeT();
            if sizeT > 1
                qt = int2str(zct(3) + 1);
                label = [label, '; ', lt, '=', qt, '/', int2str(sizeT)];
            end
        end

        % save image plane and label into the list
        imageList{i, 1} = arr;
        imageList{i, 2} = label;
    end

    % extract metadata table for this series
    metadataList = r.getMetadata();

    % save images and metadata into our master series list
    result{s, 1} = imageList;
    result{s, 2} = metadataList;
    result{s, 3} = colorMaps;
    result{s, 4} = r.getMetadataStore();
    fprintf('\n');
end
r.close();
toc

% -- Helper functions --

function [result] = versionCheck(v, maj, min)

tokens = regexp(v, '[^\d]*(\d+)[^\d]+(\d+).*', 'tokens');
majToken = tokens{1}(1);
minToken = tokens{1}(2);
major = str2num(majToken{1});
minor = str2num(minToken{1});
result = major > maj || (major == maj && minor >= min);
