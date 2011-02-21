function Miji(open_imagej)
    %% This script sets up the classpath to Fiji and optionally starts MIJ
    %% Author: Jacques Pecreaux & Johannes Schindelin
    if nargin < 1
        open_imagej = true;
    end

    %% Get the Fiji directory
    fiji_directory = fileparts(fileparts(mfilename('fullpath')));

    %% Get the Java classpath
    classpath = javaclasspath('-all');

    %% Add all libraries in jars/ and plugins/ to the classpath
    add_to_classpath(classpath, strcat([fiji_directory filesep 'jars']));
    add_to_classpath(classpath, strcat([fiji_directory filesep 'plugins']));

    %% Maybe open the ImageJ window
    if open_imagej
      cd ..;
      fprintf('\n\nUse MIJ.exit to end the session\n\n');
      MIJ.start(pwd);
    end
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