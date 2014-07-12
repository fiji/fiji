This is Fiji
============

[ Fiji Is Just ImageJ ]

Fiji is a distribution of [ImageJ](http://imagej.net/), a popular, free
scientific image processing application. Fiji adds a couple of plugins
that are useful to life scientists analysing images.

At the moment, the following platforms are supported:

- Windows Intel 32-bit/64-bit
- Linux Intel 32-bit/64-bit
- MacOSX Intel 32-bit/64-bit (partial support for PowerPC 32-bit)
- all platforms supporting Java and a POSIX shell, via bin/ImageJ.sh

The setup is as easy as unpacking the portable archive and
double-clicking the [ImageJ
launcher](https://github.com/imagej/imagej-launcher).

Fiji is intended to be the most painless, easy, quick and convenient way
to install ImageJ and plugins and keep everything up-to-date.


Usage
-----

Fiji is meant to be distributed without source, to make the download as
small as possible. In the basic version, Fiji is a portable application,
i.e. it should run wherever you copy it.

The starting point is the ImageJ launcher, which will launch Java, set
up the environment, and call ImageJ.

To pass arguments to ImageJ, just specify them on the command line.

To pass arguments to the Java Virtual Machine, specify them on the
command line, separating them from the ImageJ arguments (if any) with a
`--`.  In other words, if you want to override the memory setting, call
Fiji like this:

	$ ./ImageJ-linux32 -Xmx128m --

Open Source
-----------

We are dedicated to open source. Not only does open source allow other
developers to port the application to new platforms that the original
authors did not begin to think of, it allows scientists to study the
code to understand the inner workings of the algorithms used, and it
permits others to use the program in totally new ways, and enhance it in
all imaginable ways.

Therefore, the majority of Fiji is licensed under the GNU Public License
version 2. Exceptions are listed in the
[LICENSES](https://github.com/fiji/fiji/blob/master/LICENSES) file.

Fiji's source code is split up into a [main
repository](https://github.com/fiji/fiji), containing the top-level project and
support scripts, while all components live in their own repositories in the
[Fiji organization on GitHub](https://github.com/fiji/). As a rule of thumb: the
file name and the project name correspond pretty well, e.g. fiji-compat.jar is
maintained in [fiji-compat](https://github.com/fiji/fiji-compat).

Participating
-------------

[Pull Requests](https://help.github.com/articles/using-pull-requests)
are very welcome!

Authors
-------

Fiji was created and is maintained by Johannes Schindelin, ImageJ 1.x
was created and is maintained by Wayne Rasband, ImageJ2 was created and
is maintained and actively developed by Curtis Rueden. For a list of
most recent contributors, please refer to the
[Contributors](http://imagej.net/Contributors) page of the Fiji/ImageJ
wiki.

Thanks
------

We are very grateful to Wayne Rasband, who is not only a very dedicated
developer of ImageJ 1.x; he also fosters an active and friendly
community around ImageJ.

We are especially grateful to be part of an outstanding
[community](http://imagej.net/Mailing_Lists) who is active, friendly and
helping to scientists understanding and analysing images every day.

Oh, and Fiji is also an island. We just wanted to let you know.
