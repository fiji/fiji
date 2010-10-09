cwd = pwd;
%%
cd (fileparts(mfilename('fullpath')));
cd ..;
cd jars;
%%
if ~exist('mij.jar', 'file')
  disp('you seem not to have mij.jar in the jars/ folder, downloading it!');
  urlwrite('http://bigwww.epfl.ch/sage/soft/mij/mij.jar', 'mij.jar'); 
end
%%
%path_ = fullfile(pwd,'ij.jar');
%%
test = dir('*.jar');
path_= cell(1);
for i = 1:length(test)
  path_{i} = (fullfile(pwd, test(i).name));
end
shift = length(test);
%%
cd ..;
cd plugins;
test = dir('*.jar');
for i = 1:length(test)
  path_{i+shift} = (fullfile(pwd, test(i).name));
end
%%
javaaddpath(path_, '-end');
%%
cd ..;
disp([sprintf('\n') sprintf('\n') 'Use MIJ.exit to end the session' sprintf('\n') sprintf('\n')]);
MIJ.start(pwd);
%%
cd(cwd);
