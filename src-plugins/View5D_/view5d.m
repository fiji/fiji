%VIEW5D Start the java viewer by Rainer Heintzmann
%
% VIEW5D(in,ts,mode) starts a java based image viewer
%
% PARAMETERS:
%   in:   can either be an image or a figure handle
%   ts:   is the input a time series? (0,1)
%   mode: 'direct' or 'extern', for, respectively, starting the Java applet
%         directly in MATLAB or in your web browser.
%
% DEFAULTS:
%   ts:   0
%   mode: 'direct'
%
% See also DIPSHOW.
%
% NOTES:
%   To use the external viewer, configure your MATLAB to find your web browser,
%   see DOCOPT. Make sure your web browser has java enabled.
%
%   If there is no java support in MATLAB, the external mode is always used.
%
%   For larger images you may get java.lang.OutOfMemoryError
%   Increase the memory for the jvm by placing the file java.opts in your
%   MATLAB directory with -Xmx#bytes (java 1.3) or maxHeapSize=#bytes (older)
%   Set #bytes to something large (600000000).
%
% More information about the viewer
%   http://www.nanoimaging.de/View5D/
%
% OPTIONAL INSTALLATION INSTRUCTIONS:
%   This part is optional, but might be necessary for earlier versions of
%   MATLAB. In short, if you get error messages when trying to use this
%   function, follow these instructions:
%
%   You can to modify the MATLAB file 'classpath.txt' to use the Java applet
%   View5D.jar (see Help > MATLAB > External Interfaces > Using Sun Java...)
%   Copy the file 'classpath.txt' to $HOME/matlab. To find this file, type:
%   >> which classpath.txt
%   at the MATLAB command prompt. Edit your copy of this file, and add the full
%   path to the View5D.jar file to the end of the list. The line you have to add
%   you can generate by typing:
%   >> fullfile(fileparts(which('view5d')),'private','View5D.jar')
%   at the MATLAB command promt.

% (C) Copyright 1999-2008               Pattern Recognition Group
%     All rights reserved               Faculty of Applied Physics
%                                       Delft University of Technology
%                                       Lorentzweg 1
%                                       2628 CJ Delft
%                                       The Netherlands
%
% Bernd Rieger, Rainer Heintzmann
% Oct 2004, Fixed startup behavior
% June 2005, Fixed Bug with multi-time data (tag is "times")
% June 2005, Added support of complex and double datatypes
% March 2007, Updated for new View5D (RH)
% August 2007, Changed the parsing of inputs, removed function VIEW5D_WINDOW,
%              using web browser as external viewer, using absolute paths with
%              external viewer, removed output argument. (CL)
% 2 December 2008, Setting dynamic Java path, no need to edit classpath.txt. (CL)
% 17 Feb 2009, added return of figure handle (BR)

function out=view5d(varargin)

% Parse input
whe = 'direct';
ts = 0;
switch nargin
case 1
case 2
   if ischar(varargin{2})
      whe = varargin{2};
   else
      ts = varargin{2};
   end
case 3
   ts = varargin{2};
   whe = varargin{3};
otherwise
   error('Too few/many input arguments');
end
in = varargin{1};
if ~ischar(whe)
   error('MODE string should be ''direct'' or ''extern''.')
end
switch whe
   case 'direct'
      direct = 1;
   case 'extern'
      direct = 0;
   otherwise
      error('MODE string should be ''direct'' or ''extern''.')
end

if direct
   % Check for Java
   if exist('javachk','file')
      tmpj = isempty(javachk('jvm'));
   else
      tmpj = 0;
   end
   if ~tmpj
      disp('Using the external viewer, MATLAB started without java support.');
      direct = 0;
   end
end
if direct
   try
      % Add the path if not known
      jp = javaclasspath('-all');
      jarfile = jarfilename;
      if ~any(strcmp(jp,jarfile))
         javaaddpath(jarfile);
      end
      % Force the loading of the JAR file
      import view5d.*
   catch
      error(['Please update your classpath.txt file as explained in the',10,'help to this function, or use the external viewer'])
   end
end

% Prepare input image
if ishandle(in)
   in = dipgetimage(in);
else
   in = dip_image(in);
end
if ~isscalar(in) & ~isvector(in)
   error('Input image must be a scalar or vector image.')
end
sz = imsize(in);
if length(sz)>5 | length(sz)<2
   error('Only available for 2, 3, 4 and 5D images.');
end
if any(sz(1:2)==1)
   error('First two image dimensions must be larger than 1.');
end
if isvector(in)
   % The array elements need to go along the 4th dimension, and not become the time axis later on.
   if length(sz)>4
      error('Only available for 2, 3 and 4D tensor images.');
   end
   if length(sz)<3
      in = iterate('expanddim',in,3);
   end
   in = array2im(in);
   if ndims(in)==5
      in = permute(in,[1,2,3,5,4]); % the new elements dimension is 5th, should be 4th.
   end
   elements = 1;
else
   elements = 0;
end
in = expanddim(in,5); % make sure the input has 5 dimensions
if strcmp(datatype(in),'dcomplex')
   in = dip_image(in,'scomplex'); % dcomplex not supported
end
sz = imsize(in);
if ts
   % The user asks for a time series -- move the last data dimension to the 5th dimension
   t = find(sz>1); t = t(end);
   if t<3 | t==5
      t = [];
   elseif t==4 & elements
      sz(t) = 1;
      t = find(sz>1); t = t(end);
      if t<3
         t = [];
      end
   end
   if ~isempty(t)
      order = 1:5;
      order(t) = 5;
      order(5) = t;
      in = permute(in,order);
   end
end

% Start the applet
if direct
   if ~isreal(in)
      % Make a one dimensional flat input array
      inr = reshape(real(in),1,prod(sz));
      ini = reshape(imag(in),1,prod(sz));
      in5df = dip_array(reshape([inr ini],1,2*prod(sz)));
      out = View5D.Start5DViewerC(in5df,sz(1),sz(2),sz(3),sz(4),sz(5));
   else
      % Make a one dimensional flat input array
      in5d = dip_array(reshape(in,1,prod(sz)));
      out = View5D.Start5DViewer(in5d,sz(1),sz(2),sz(3),sz(4),sz(5));
   end
else
   view5d_image_extern(in);
   out =[];
end

%-----------------------------------------------------------------------
function jarfile = jarfilename
function jarfile = jarfilename
%if matlabversion>=6.5
  jarfile = fullfile(fileparts(mfilename('fullpath')),'private','View5D.jar');
%else
%   jarfile = fullfile(fileparts(which(mfilename)),'private','View5D.jar');
%end

%-----------------------------------------------------------------------
function view5d_image_extern(in)

jarfile = jarfilename;
base = tempdir; % return OS set tempdir
if ~tempdir
   error('No temp directory given by OS.');
end
fn = [base,'dipimage_view5d'];

mdt = {'uint8',    1, 8,  'Byte'
       'uint16',   2, 16, 'Short'
       'uint32',   4, 32, 'Long'
       'sint8',    1, 8,  'Byte'
       'sint16',   2, 16, 'Short'
       'sint32',   4, 32, 'Long'
       'sfloat',  -1, 32, 'Float'
       'dfloat',  -1, 64, 'Double'
       'scomplex',-1, 32, 'Complex'
       'bin',      1, 8,  'Byte'
   };
ind = find(strcmp(datatype(in),mdt(:,1)));
bytes = mdt{ind,2};
bits  = mdt{ind,3};
dtype = mdt{ind,4};

sz = imsize(in);

% write raw data
%in = permute(in,[1 2 3 5 4]); % (Why swap two last dimensions??? I don't get this. -- CL)
writeim(in,fn,'icsv1',0);
delete([fn,'.ics']);

% write html file
content = ['<html><head><title>5D Viewer</title></head><body>' 10 ...
      '<!--Automatically generated by DIPimage.-->' 10 ...
      '<h1>5D Viewer</h1><p>Image Data displayed by View5D, a Java-Applet by Rainer Heintzmann</p>' 10 ...
      '<hr><applet archive=',jarfile,' code=View5D.class width=600 height=700 alt="Please enable Java.">' 10 ...
      '<param name=file value=',fn,'.ids>' 10 ...
      '<param name=sizex value=' num2str(sz(1)) '>' 10 ...
      '<param name=sizey value=' num2str(sz(2)) '>' 10 ...
      '<param name=sizez value=' num2str(sz(3)) '>' 10 ...
      '<param name=times value=' num2str(sz(5)) '>' 10 ...
      '<param name=elements value=' num2str(sz(4)) '>' 10 ...
      '<param name=bytes value=' num2str(bytes) '>' 10 ...
      '<param name=bits value=' num2str(bits) '>' 10 ...
      '<param name=dtype value=''' dtype '''>' 10 ...
      '<param name=unitsx value=''pix''>' 10 ...
      '<param name=scalex value=''1''>' 10 ...
      '<param name=unitsy value=''pix''>' 10 ...
      '<param name=scaley value=''1''>' 10 ...
      '<param name=unitsz value=''pix''>' 10 ...
      '<param name=scalez value=''1''>' 10 ...
      '<param name=unitse value=''color''>' 10 ...
      '<param name=scalee value=''1''>' 10 ...
      '<param name=unitst value=''time''>' 10 ...
      '<param name=scalet value=''1''>' 10 ...
      '<param name=unitsv1 value=''int.''>' 10 ...
      '<param name=unitsv2 value=''int.''>' 10 ...
      '<param name=unitsv3 value=''int.''>' 10 ...
      '<param name=unitsv4 value=''int.''>' 10 ...
      '<p>Your browser does not support Java applets.</p>' 10 ...
      '</applet><hr></body></html>' 10];
fid = fopen([fn '.html'],'w');
if fid <0
   error(['Could not create file' fn '.html']);
end
fprintf(fid,'%s',content);
fclose(fid);

% Start the applet
web(['file://' fn '.html']);
