function imp = copytoImagePlus(I,varargin)
% copytoImagePlus allows you to open an array I with an instance of ImageJ
% within MATLAB with a proper data type and hyperstack dimensions.
%
%
% SYNTAX
% imp = copytoImagePlus(I)
% imp = copytoImagePlus(I,dimorder)
% imp = copytoImagePlus(____,'Param',value)
% 
%
% REQUIREMENTS
% ImageJ-MATLAB as part of Fiji installation
% https://imagej.net/MATLAB_Scripting
%
% ijmshow assumes a net.imagej.matlab.ImageJMATLABCommands Java object
% named 'IJM' is made available in the base Workspace by ImageJ (part of
% ImageJ-MATLAB).
%
%
% INPUT ARGUMENTS
% I           uint16 | uint8 | double | single
%             An array of integers to be opened with ImageJ. This array can
%             have from 2 to 5 dimensions.
%
%
% dimorder    char row vector made of 'XYCZT' | 'YXCZT' (default)
%
%             (Optional) A char row vector composed of 'X', 'Y', 'C' for
%             channels, 'Z' for slices, and 'T' for frames. dimorder is
%             case insensitive. You cannot repeat any of the five letters
%             in dimorder. The first two letters must be either 'X' or 'Y'.
%             The length of dimorder must be 5 or match the number of
%             dimensions of the array specified by I. The third to the
%             fifth letters must be chosen from 'C', 'Z', and 'T'.
%
%             The default is set 'YXCZT' rather than 'XYZCT', because the X
%             and Y axes of an MATLAB array is flipped over in ImageJ by
%             IJM.show().
%
%
% OPTIONAL PARAMETER/VALUE PAIRS
% NewName     char row vector | 'new' (default)
%             The window title of the new image in ImageJ
%
% FrameInterval
%             scalar
%             Time frame sampling interval in seconds
%
% OUTPUT ARGUMENTS
% imp         ij.ImagePlus Java object
%
% EXAMPLES
% see https://github.com/kouichi-c-nakamura/copytoImagePlus
%
%
% Written by Kouichi C. Nakamura Ph.D.
% MRC Brain Network Dynamics Unit
% University of Oxford
% kouichi.c.nakamura@gmail.com
% 03-May-2018 04:57:24
%
% See also
% ImageJ as part of ImageJ-MATLAB (https://github.com/imagej/imagej-matlab/)
% copytoImgPlus
% copytoImg, copytoMatlab


import ij.process.ShortProcessor
import ij.process.ByteProcessor
import ij.process.FloatProcessor

p = inputParser;
p.addRequired('I',@(x) isnumeric(x));
p.addOptional('dimorder','YXCZT',@(x) ischar(x) && isrow(x) ...
    && all(arrayfun(@(y) ismember(y,'XYCZT'),upper(x))) && length(x) >=2 ...
    && all(arrayfun(@(y) ismember(y,'XY'),upper(x(1:2))))...
    );
p.addParameter('NewName','new',@(x) ischar(x) && isrow(x));
p.addParameter('FrameInterval',[],@(x) isreal(x) && x > 0);

p.parse(I,varargin{:});

dimorder = upper(p.Results.dimorder);
newname = p.Results.NewName;
frameinterval = p.Results.FrameInterval;



switch dimorder(1:2)
    case 'XY'
        order1 = [1 2];
    case 'YX'
        order1 = [2 1];
end


switch dimorder(3:ndims(I))
    case 'CZT'
        order2 = 3:5;
    case 'CTZ'
        order2 = [3 5 4];
    case 'ZCT'
        order2 = [4 3 5];
    case 'ZTC'
        order2 = [4 5 3];
    case 'TCZ'
        order2 = [5 3 4];
    case 'TZC'
        order2 = [5 4 3];
    case 'CZ'
        order2 = [3 4];
    case 'CT'
        order2 = [3 5 4];
    case 'ZC'
        order2 = [4 3];
    case 'ZT'
        order2 = [5 3 4];        
    case 'TC'
        order2 = [4 5 3];
    case 'TZ'
        order2 = [5 4 3];
    case 'C'
        order2 = [3 4 5];
    case 'Z'
        order2 = [4 3 5];
    case 'T'
        order2 = [4 5 3];
    otherwise
        order2 = 3:5;
end


I0 = permute(I, [order1, order2]);

nX = int32(size(I0,1));
nY = int32(size(I0,2));
nC = int32(size(I0,3)); 
nZ = int32(size(I0,4));
nT = int32(size(I0,5));

try
    switch class(I0)
        case 'uint8'
            bitdepth = 8;

        case 'int8'
            
            bitdepth = 8;
            
        case 'uint16'
            
            bitdepth = 16;
            
        case 'int16'
            
            bitdepth = 8;
            
        case 'uint32'
            
            bitdepth = 32;
            
        case 'int32'
            
            bitdepth = 32;
            
        case 'uint64'
            error('MATLAB:copytoImg:UnsupportedType', ...
                'uint64 is not supported.');
        case 'int64'
            error('MATLAB:copytoImg:UnsupportedType', ...
                'uint64 is not supported.');
        case 'single'
            
            bitdepth = 32;
            
        case 'double'
            
            bitdepth = 32;
            
        case 'logical'
            
            bitdepth = 8;

        otherwise
            error('MATLAB:copytoImg:UnsupportedType', ...
                '%s is not supported.', class(I0));

    end

catch merr
    if strcmp(merr.identifier, 'MATLAB:undefinedVarOrClass')
            error('MATLAB:copytoImg:undefinedVarOrClass', ...
                'Could not find ImgLib2 on the path. Did you forget to run ''Miji(false)'' before calling this function?');
    else
        rethrow(merr);
    end
end


imp = ij.IJ.createHyperStack(newname,nX,nY,nC,nZ,nT,bitdepth);

for t = 1:nT
    imp.setT(t);
    for z = 1:nZ
        imp.setZ(z);
        for c = 1:nC
            imp.setC(c);
            
            XY = I0(:,:,c,z,t);
            xy = XY(:)';
            
            switch bitdepth
                case 16
                    ip = ShortProcessor(nX,nY);
                    ip.setPixels(xy);
                    imp.setProcessor(ip);
                case 8
                    ip = ByteProcessor(nX,nY);
                    ip.setPixels(xy);
                    imp.setProcessor(ip);
                otherwise
                    ip = FloatProcessor(nX,nY);
                    ip.setPixels(single(xy));                  
                    imp.setProcessor(ip);
            end
        end
    end
end

imp.setT(1);
imp.setZ(1);
imp.setC(1);
%imp.show();
imp.setDisplayMode(ij.IJ.COLOR) %NOTE this is required to enable the next line
imp.setDisplayMode(ij.IJ.COMPOSITE)

try
    imp.resetDisplayRanges();
catch mexc
    if strcmpi(mexc.identifier,'MATLAB:UndefinedFunction')
        warning('resetDisplayRanges did not work')
    else
        throw(mexc)
    end
end

if ~isempty(frameinterval)
    
    fi = imp.getFileInfo();
    fi.frameInterval = frameinterval;
    imp.setFileInfo(fi);
    %TODO Show Info... does not show the frameinterval
end


end
