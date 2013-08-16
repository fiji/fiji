function imgplus = copytoImgPlus(I, calibration, name, axes)
%%COPYTOIMGPLUS Create a new ImgLib2 ImgPlus from a MATLAB image.
%
% Returns a new ImgLib2 ImgPlus object, which is basically an Img with some
% metadata added. This method allows specifying the spatial calibration,
% the target image name in ImageJ, and the desired axes type. Because of
% metadata limitation in ImageJ, only 3 dimensions are allowed, and they
% must be taken from X, Y, Z, T and C.
%
% img = copytoImgPlus(I) returns a new ImgPlus built from the specified
% MATLAB array, with the right type determined at construction. The target
% image has its X ans Y axis permuted so that both images have the same
% orientation in MATLAB and ImgLib2. The default spatial calibration is
% taken to 1 pixel in all dimensions. The default name is set to be
% 'matlab' and the default axes are tken from X, Y, Z, T, C in this order.
%
% img = copytoImgPlus(I, calibration) allows specifying the spatial
% calibration for each dimension of the source array. It must be a double
% array of ndims elements where ndims is the number of dimensions of the
% array I.
%
% img = copytoImgPlus(I, calibration, name) allows specifying the name of
% the ImgPlus image.
%
% img = copytoImgPlus(I, calibration, name, axes) allows specifying axes
% types for the target multidimensional image. They must be passed as a
% cell array of strings taken from { 'X', 'Y', 'Z', 'T', 'C' }.
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
%   imgplus = copytoImgPlus(X, [0.2 0.7], 'Dürer', { 'Y' , 'X' });
%   net.imglib2.img.display.imagej.ImageJFunctions.show(imgplus);
%
% see also: copytoImg, copytoMatlab
% Jean-Yves Tinevez - 2013


%% CONTANTS

DEFAULT_AXES = { 'X', 'Y', 'Z', 'T', 'C' };

try
    MATCHING_AXES = [
        net.imglib2.meta.Axes.X
        net.imglib2.meta.Axes.Y
        net.imglib2.meta.Axes.Z
        net.imglib2.meta.Axes.TIME
        net.imglib2.meta.Axes.CHANNEL ];
catch merr
    if strcmp(merr.identifier, 'MATLAB:undefinedVarOrClass')
        error('MATLAB:copytoImgPlus:undefinedVarOrClass', ...
            'Could not find ImgLib2 on the path. Did you forget to run ''Miji(false)'' before calling this function?');
    else
        rethrow(merr);
    end
end
DEFAULT_NAME = 'matlab';

%% Check input

if ndims(I) > 5
    error('MATLAB:copytoImgPlus:TooManyDimensions', ...
        'Cannot deal with more that 5 dims, got %d.', ndims(I))
end


if nargin < 2
    calibration = ones( ndims(I), 1);
else
    if ~isfloat(calibration)
        error('MATLAB:copytoImgPlus:IllegalArgument', ...
            'Second argument, calibration, must be a double array. Got a %s.', ...
            class(calibration))
    end
    if numel(calibration) ~= ndims(I)
        error('MATLAB:copytoImgPlus:IllegalArgument', ...
            'Second argument, calibration, must must have one element per dimension of I. Got %d elements.', ...
            numel(calibration))
    end
    
end

if nargin < 3
    name = DEFAULT_NAME;
else
    if ~ischar(name)
        error('MATLAB:copytoImgPlus:IllegalArgument', ...
            'Third argument, name, must be a string. Got a %s.', ...
            class(name))
    end
end

if nargin < 4
    axes = DEFAULT_AXES(1 : ndims(I));
else
    if ~iscell(axes)
        error('MATLAB:copytoImgPlus:IllegalArgument', ...
            'Fourth argument, axes, must be a cell array of strings. Got a %s.', ...
            class(axes))
    end
    if numel(axes) ~= ndims(I)
        error('MATLAB:copytoImgPlus:IllegalArgument', ...
            'Fourth argument, axes, must must have one element per dimension of I. Got %d elements.', ...
            numel(axes))
    end
end

%% Deal with metadata

ax(ndims(I)) = MATCHING_AXES(1);
for i = 1 : ndims(I)
    index = find(strcmp(DEFAULT_AXES, axes{i}), 1);
    if isempty(index) || index < 0
        error('MATLAB:copytoImgPlus:IllegalArgument', ...
            'Unkown axis type: %s. Can only deal with X, Y, Z, T & C.',  axes{i})
    end
    
    ax(i) = MATCHING_AXES(index);
    
end

%% Generate ImgLib2 img

img = copytoImg(I);

imgplus = net.imglib2.img.ImgPlus(img, name, ax, calibration);

end