function InstallJava3D

%% Install Java3D.
% Use Miji and Fiji power to automatically download and install Java3D for
% the MATLAB JVM, so that we can play with accelerated 3D afterwards.
% Jean-Yves Tinevez, Johannes Schindelin, July 2011

    %% Process
    
    % First, silently launch Miji, which will make Fiji libs accessible.
    Miji(false)
    
    % Second, test if we have Java3D installed already. We do this by
    % instantiating a Java3D class and checking if we raise an exception.
    fprintf('Testing if Java3D is already installed...\n');
    isJava3DInstalled = IsJava3DInstalled();
    
    if isJava3DInstalled
        
        j3dvsersionstr = char(ij3d.Install_J3D.getJava3DVersion());
        fprintf('Java3D is already installed.\n')
        fprintf('Installed version is %s.\n', j3dvsersionstr)
        
        j3dvsersion = str2double(j3dvsersionstr);
        
        if j3dvsersion >= 1.5
            fprintf('This is good enough, you have nothing to do.\n');
            fprintf('Exiting.\n');
            fprintf('\n');
            return
        end
        
        fprintf('We need at least version 1.5, so let us update it.\b')
        fprintf('\n');
        
    else
        
        fprintf('Java3D is not installed.\n');
        fprintf('Let us try to install it.\n');
        fprintf('\n');
        
    end
    
    
    fprintf('Determining the target JVM ext folder...\n')
    ext_path =  char(ij3d.Install_J3D.getFirstExtDir());
    fprintf('Target path is %s.\n', ext_path);
    fprintf('\n');
    
    fprintf('Determining if we have write access to JVM ext folder...\n')
    [status attrib] = fileattrib(ext_path); %
    
    if ~status
        fprintf('Cannot determine the attributes of %s. Aborting.\n', ext_path);
        return
    end
    
    canwrite = false;
    
    if attrib.UserWrite
        canwrite = true;
        fprintf('MATLAB says that we can write to the path. Let us double check.\n');
        % Indeed, under recent versions of Windows, the permissions reported
        % by MATLAB can be erroneous because of UAC...
        try
            testfile = java.io.File([ext_path java.io.File.separatorChar 'test']);
            if (testfile.exists())
                testfile.delete()
            end
            testfile.createNewFile();
            testfile.delete();
            fprintf('Ok, we do have the right to write to %s.\n', ext_path)
        catch ioe %#ok<NASGU>
            fprintf('Writing test shows that we CANNOT write to the ext path.\n')
            fprintf('We do not have the right to write to %s,\ninstallation CANNOT be done automatically.\n', ext_path)
            canwrite = false;
        end
        
        % On windows versions, we also have to test that the bin folder is
        % writable, for it is where the .dll files are going.
        if ispc
            
            bin_path = [ char(java.lang.System.getProperty('java.home')) java.io.File.separatorChar 'bin' ];
            try
                testfile = java.io.File([bin_path java.io.File.separatorChar 'test']);
                if (testfile.exists())
                    testfile.delete()
                end
                testfile.createNewFile();
                testfile.delete();
                fprintf('Ok, we do have the right to write to %s.\n', bin_path)
            catch ioe %#ok<NASGU>
                fprintf('Writing test shows that we CANNOT write to the bin path.\n')
                fprintf('We do not have the right to write to %s,\ninstallation CANNOT be done automatically.\n', bin_path)
                canwrite = false;
            end
            
        end
        
    end
    
    if canwrite
        
        fprintf('We can write to the path, installation will proceed.\n')
        plugin = ij3d.Install_J3D();
        plugin.run('')
        
        fprintf('\n');
        fprintf('OK, so the files are in place.\n');
        fprintf('You still need to RELAUNCH MATLAB so that they can be sourced.\n');
        fprintf('(It will NOT work if you do not restart MATLAB.)\n');
        fprintf('Then, calling this script again should tell you that everyting is in place.\n');
        fprintf('Exiting.\n');
        fprintf('\n');
        
    else
        
        fprintf('\n');
        
        fprintf('OK, so this is where you have to intervene manually.\n')
        fprintf('You will have to ask or impersonate your computer''s administrator\n')
        fprintf('so that it gives you WRITE PERMISSION to the following folder:\n')
        fprintf('\t -> %s.\n', ext_path)
        if ispc
            fprintf('\t -> %s.\n', bin_path)
        end
        fprintf('\n');
        fprintf('Once (s)he or you did that, relaunch this helper script, saying\n');
        fprintf('''Yes'' to any question you might be asked.\n');
        fprintf('\n');
        fprintf('After that, of course, you will still need to relaunch MATLAB.\n');
        
    end
    
end
