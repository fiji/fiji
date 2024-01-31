Miji();
MIJ.run('Embryos (42K)');
I=MIJ.getCurrentImage;
E = imadjust(wiener2(im2double(I(:,:,1))));
imshow(E);
MIJ.createImage('result',E, true);
