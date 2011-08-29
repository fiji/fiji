%% How to install Fiji, and make MATLAB aware of it.
%
% ImageJ is an image processing software, very popular in the Life-Science
% field. If you are a master student, a PhD or a postdoc in Biology, there
% is a very good chance that ImageJ is installed on your computer.
%
% *ImageJ* is developed mainly by Wayne Rasband, and is written in Java
% (see http://rsb.info.nih.gov/ij/index.html). It has a clever plugin
% system that allows users to extend it with new functionalities.
%
% *Fiji* (Fiji is Just ImageJ, http://fiji.sc/) is a _distribution_ of
% ImageJ, that includes (amongst other things) a lot of plugins, some
% useful libraries and an updater to keep everything up to date.
%
% Amongst other things, it is made to facilitate inter-operability with
% other softwares. For instance, it offers scripting facilities using
% popular languages such as Python, Ruby, Clojure, Beanshell, Javascript,
% etc. It also ships with *Miji*, a set of utilities made to ease its
% interaction with MATLAB. Since ImageJ is written in Java, it can be
% called directly from within MATLAB, and be used as a supporting image
% visualization, processing or analysis tool.
%
% In this demo bundle, we demonstrate how to use Fiji to get a very nice,
% hardware accelerated, 3D viewer in MATLAB.
%
% This is not a totally trivial example, as it requires to have the Java
% libraries for accelerated 3D rendering installed for the MATLAB Java
% Virtual Machine. Fortunately, Fiji comes with a script that automates
% the process.
%
% But you still have to install Fiji.

%% Installing Fiji
%
% Simply go to http://fiji.sc/Downloads and download the binaries for your
% OS. You will not find an installer; you just have to unzip the Fiji
% folder to somewhere convenient for you.
%
% For demonstration purposes, let us assume you have a PC machine that
% runs a 64-bit version of Windows. You then have downloaded the matching
% Fiji version (fiji-win64-something.zip), and unzipped it in |C:\Program
% Files\| and now have a folder called |C:\Program Files\Fiji.app|.
%
% Before launching MATLAB, launch Fiji itself. You will find an executable
% in the Fiji folder that you just need to run. At startup, Fiji should
% offer you to go to the *updater*. Do that and run the update. If you are
% not proposed this, you can find the updater in the _Help > Update Fiji_
% menu item.
%
% Close Fiji when the update is done.
%
% A side note: ImageJ and its plugin are very useful if you are interested
% in scientific Image Processing. There are a zillion things you can do
% with it. If you are interested in using Fiji as a standalone
% application, check some of the tutorials there:
% http://fiji.sc/Documentation.
%
%% Making MATLAB aware of Fiji
%
% The utilities for interaction with MATLAB can all be accessed by adding a
% single folder to the MATLAB path. They are all located in the |scripts|
% folder of the Fiji folder.
%
% So, if you have installed Fiji in |C:\Program Files\Fiji.app|, you just
% have to add |C:\Program Files\Fiji.app\scripts| to the MATLAB path, and
% that's all.
% 
% By the way, it is in this folder that you will find all the m-files of
% this demo bundle. So you can launch the demos listed below simply by
% typing their name in MATLAB command window; they will be found since they
% are on the path.

%% Installing the Java3D libraries
%
% You are done with the main installation part. You just have to do it
% once, and if some updates are made to Fiji, you just have to launch its
% updater to get them.
%
% You still have to install the *Java3D* libraries. Fortunately, this is
% done automatically with a Fiji script:
%
% * Launch MATLAB;
% * type |InstallJava3D| in the command window.
%
% If you get a message stating that the function cannot be performed, this
% is because you did not add the Fiji |scripts| folder to the MATLAB path.
% in that case, go back to the previous paragraph.
%
% The |InstallJava3D| will tell you what it does verbosely. It will check
% whether your Java Virtual Machine already has Java3D installed, and if
% the version requirement is met. If not, it will automatically download
% Java 3D and install it to the right folder.
%
% There might be some issues if you do not have write permission to the
% target folder. In that case, the script will tell you what folder to make
% writable. Then you can simply re-launch the script.
%
% Once you are done, you have to restart MATLAB, so that the new libraries
% are sourced.
%
%% What now?
%
% You are all set.
%
% You can now play with the 2 demo files |Matlab3DViewerDemo_1.m| and
% |Matlab3DViewerDemo_2.m|, that are heavily annotated and published in
% the same folder as this file.
%
% * <Matlab3DViewerDemo_1.html Matlab3DViewerDemo_1> shows how to load a 3D
% data set in MATLAB and render it in 3D (volumetric rendering);
% * <Matlab3DViewerDemo_2.html Matlab3DViewerDemo_2> shows how to create
% simple object volumes in MATLAB and make a surface rendering of them.
% * <Matlab3DViewerDemo_3.html Matlab3DViewerDemo_3> demonstrates the
% surface plot mode. A 2D image is used to generate a 3D surface, where
% pixel intensity is interpreted as elevation.
%
% *MIJ* is the low level Java class that contains static methods to
% exchange data between MATLAB and ImageJ. You can find its documentation
% there: http://bigwww.epfl.ch/sage/soft/mij/
%
% *Miji* is the glue that facilitates the use of all these components. It
% is described in the aforementioned demo files.
%
%% Authors
%
% * ImageJ is written by Wayne Rasband and friends for the core and the
% plugins;
% * Fiji is written by a bunch of enthusiasts, under the holy auspices of
% Johannes Schindelin and friends;
% * The ImageJ 3D viewer is written by Benjamin Schmid, Mark Longair,
% Albert Cardona and Johannes Schindelin;
% * MIJ is written by Daniel Sage and Dimiter Prodanov;
% * Miji is written by Johannes Schindelin, Jacques Pécréaux and Jean-Yves
% Tinevez.
%
% It's all open source, feel free to modify, distribute, comment or
% whatever you feel like.
%
%% Version history
%
% * 26 Jul 2011: Initial release
% * 4 Aug 2011: Fix a bug in write permission detection on Windows
% platform, thanks to report by Peter Beemiller
% * 11 Aug 2011: Added a 3rd demo, demonstrating the surface plot of 2D
% image, thanks to the fix on the 3D viewer applied today by Johannes
% Schindelin.
% 
%
%%
%
% _Jean-Yves Tinevez \<jeanyves.tinevez at gmail.com\> - July 2011_
