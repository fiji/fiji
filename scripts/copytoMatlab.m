function I = copytoMatlab(img)
%%COPYTOMATLAB Copy the content of an ImgLib2 image to MATLAB.
%
% I = copytoMatlab(img) returns a MATLAB copy of the array stored in the
% specified ImgLib2 Img object. This function only works for ImgLib2 images
% that are ArrayImgs, and whose types are native real or integer ones. 
% 
% We rely on Miji to set up classpath, so you would have to add Miji to
% your MATLAB path and call
%  >> Miji(false); % or true
% prior to using this function.
%
% see also: copytoImgPlus, copytoImg
% Jean-Yves Tinevez - 2013

    %% CONSTANTS

    ACCEPTED_TYPES = {
        'net.imglib2.type.numeric.integer.UnsignedByteType'
        'net.imglib2.type.numeric.integer.UnsignedShortType'
        'net.imglib2.type.numeric.integer.UnsignedIntType'
        'net.imglib2.type.numeric.integer.ByteType'
        'net.imglib2.type.numeric.integer.ShortType'
        'net.imglib2.type.numeric.integer.IntType'
        'net.imglib2.type.numeric.integer.LongType'
        'net.imglib2.type.numeric.integer.LongType'
        'net.imglib2.type.numeric.real.FloatType'
        'net.imglib2.type.numeric.real.DoubleType'
        };
    
    %% Check input

    if ~isa(img, 'net.imglib2.img.array.ArrayImg')
        error('MATLAB:copytoMatlab:IllegalArgument', ...
            'Expected argument to be an ImgLib2 ArrayImg, got a %s.', ...
            class(img) )
    end
    
    fel = img.firstElement;
    knowType = false;
    for i = 1 : numel(ACCEPTED_TYPES)
       if isa(fel, ACCEPTED_TYPES{i})
           knowType = true;
           break
       end
    end
    
    if ~knowType
        error('MATLAB:copytoMatlab:IllegalArgument', ...
            'Can only deal with native real or integer types, got a %s.', ...
            class(fel) )
    end
    
    
    
    %% Operate on source image
    
    % Retrieve dimensions
    numDims = img.numDimensions();
    sizes = NaN(1, numDims);
    for i = 1 : numDims
        sizes(i) = img.dimension(i-1);
    end
    
    % Retrieve array container
    J = img.update([]).getCurrentStorageArray;
    
    % Deal with unsigned types
    if isa(fel, 'net.imglib2.type.numeric.integer.UnsignedByteType')
        J = typecast(J, 'uint8');
    elseif isa(fel, 'net.imglib2.type.numeric.integer.UnsignedShortType')
        J = typecast(J, 'uint16');
    elseif isa(fel, 'net.imglib2.type.numeric.integer.UnsignedIntType')
        J = typecast(J, 'uint32');
    end

    % Build MATLAB array
    I = reshape(J, sizes);
    I = permute(I, [2 1]);

end