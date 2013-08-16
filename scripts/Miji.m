function Miji(open_imagej)
    %% This script sets up the classpath to Fiji and optionally starts MIJ
    % Author: Jacques Pecreaux, Johannes Schindelin, Jean-Yves Tinevez

    if nargin < 1
        open_imagej = true;
    end
    

    %% Get the Fiji directory
    fiji_directory = fileparts(fileparts(mfilename('fullpath')));

    %% Get the Java classpath
    classpath = javaclasspath('-all');

    %% Add all libraries in jars/ and plugins/ to the classpath
    
    % Switch off warning
    warning_state = warning('off');
    
    add_to_classpath(classpath, strcat([fiji_directory filesep 'jars']));
    add_to_classpath(classpath, strcat([fiji_directory filesep 'plugins']));
    
    % Switch warning back to initial settings
    warning(warning_state)

    % Set the Fiji directory (and plugins.dir which is not Fiji.app/plugins/)
    java.lang.System.setProperty('ij.dir', fiji_directory);
    java.lang.System.setProperty('plugins.dir', fiji_directory);

    %% Maybe open the ImageJ window
    if open_imagej
        cd ..;
        fprintf('\n\nUse MIJ.exit to end the session\n\n');
        MIJ.start();
    else
        % initialize ImageJ with the NO_SHOW flag (== 2)
        ij.ImageJ([], 2);
    end

    % Make sure that the scripts are found.
    % Unfortunately, this causes a nasty bug with MATLAB: calling this
    % static method modifies the static MATLAB java path, which is
    % normally forbidden. The consequences of that are nasty: adding a
    % class to the dynamic class path can be refused, because it would be
    % falsy recorded in the static path. On top of that, the static
    % path is fsck in a weird way, with file separator from Unix, causing a
    % mess on Windows platform.
    % So we give it up as now.
    % %    fiji.User_Plugins.installScripts();
end

function add_to_classpath(classpath, directory)
    % Get all .jar files in the directory
    test = dir(strcat([directory filesep '*.jar']));
    path_= cell(0);
    for i = 1:length(test)
        if not_yet_in_classpath(classpath, test(i).name)
            path_{length(path_) + 1} = strcat([directory filesep test(i).name]);
        end
    end

    %% Add them to the classpath
    if ~isempty(path_)
        javaaddpath(path_, '-end');
    end
end

function test = not_yet_in_classpath(classpath, filename)
%% Test whether the library was already imported
expression = strcat([filesep filename '$']);
test = isempty(cell2mat(regexp(classpath, expression)));
end
