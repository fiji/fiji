function img = copytoImg(I)
%%COPYTOIMG Create a new ImgLib2 Img from a MATLAB image.
%
% img = copytoImg(I) returns a new ImgLib2 Img object, built from the
% specified MATLAB array, with the right type determined at construction.
% The target image has its X ans Y axis permuted so that both images have
% the same orientation in MATLAB and ImgLib2.
%
% This function supports all MATLAB primitive types but uint64 (unsigned
% long integers).
% 
% We rely on Miji to set up classpath, so you would have to add Miji to
% your MATLAB path and call
%  >> Miji(false); % or true
% prior to using this function.
%
% EXAMPLE
%
%   Miji(false)
%   load durer
%   img = copytoImg(X);
%   net.imglib2.img.display.imagej.ImageJFunctions.show(img);
%
% see also: copytoImgPlus, copytoMatlab
% Jean-Yves Tinevez - 2013

% Permute dim 0 and 1 (X & Y) so that we match MATLAB convention in ImgLib
I = permute(I, [2 1]);

% Copy to ImgLib2
try
    
    switch class(I)
        
        case 'uint8'
            img = net.imglib2.img.array.ArrayImgs.unsignedBytes(I(:), size(I));
            
        case 'int8'
            img = net.imglib2.img.array.ArrayImgs.bytes(I(:), size(I));
            
        case 'uint16'
            img = net.imglib2.img.array.ArrayImgs.unsignedShorts(I(:), size(I));
            
        case 'int16'
            img = net.imglib2.img.array.ArrayImgs.shorts(I(:), size(I));
            
        case 'uint32'
            img = net.imglib2.img.array.ArrayImgs.unsignedInts(I(:), size(I));
            
        case 'int32'
            img = net.imglib2.img.array.ArrayImgs.ints(I(:), size(I));
            
        case 'uint64'
            error('MATLAB:copytoImg:UnsupportedType', ...
                'uint64 is not supported by Imglib2.');
            
        case 'int64'
            img = net.imglib2.img.array.ArrayImgs.shorts(I(:), size(I));
            
        case 'single'
            img = net.imglib2.img.array.ArrayImgs.floats(I(:), size(I));
            
        case 'double'
            img = net.imglib2.img.array.ArrayImgs.doubles(I(:), size(I));
            
        case 'logical'
            img = net.imglib2.img.array.ArrayImgs.bits(I(:), size(I));
            
        otherwise
            error('MATLAB:copytoImg:UnsupportedType', ...
                '%s is not supported by Imglib2.', class(I));
            
    end
    
catch merr
    
    if strcmp(merr.identifier, 'MATLAB:undefinedVarOrClass')
            error('MATLAB:copytoImg:undefinedVarOrClass', ...
                'Could not find ImgLib2 on the path. Did you forget to run ''Miji(false)'' before calling this function?');
    else 
        rethrow(merr);
    end
    
end

end