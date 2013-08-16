function [installed] = IsJava3DInstalled(auto_install)

    %% Test whether Java3D was installed.
    % Use Miji and Fiji power to automatically download and install Java3D for
    % the MATLAB JVM, so that we can play with accelerated 3D afterwards.
    % Jean-Yves Tinevez, Johannes Schindelin, July 2011

    if nargin < 1
        auto_install = false;
    end

    try
        javax.media.j3d.Transform3D();
        installed = true;
        return;
    catch me %#ok<NASGU>
    end

    if auto_install
        InstallJava3D();
    end

    installed = false;

end