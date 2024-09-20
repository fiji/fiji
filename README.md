[![](https://github.com/fiji/fiji/actions/workflows/build-main.yml/badge.svg)](https://github.com/fiji/fiji/actions/workflows/build-main.yml)
[![developer chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://imagesc.zulipchat.com/#narrow/stream/327238-Fiji)
[![Image.sc Forum](https://img.shields.io/badge/dynamic/json.svg?label=forum&amp;url=https%3A%2F%2Fforum.image.sc%2Ftags%2Ffiji.json&amp;query=%24.topic_list.tags.0.topic_count&amp;colorB=green&amp;&amp;suffix=%20topics&amp;logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAABPklEQVR42m3SyyqFURTA8Y2BER0TDyExZ+aSPIKUlPIITFzKeQWXwhBlQrmFgUzMMFLKZeguBu5y+//17dP3nc5vuPdee6299gohUYYaDGOyyACq4JmQVoFujOMR77hNfOAGM+hBOQqB9TjHD36xhAa04RCuuXeKOvwHVWIKL9jCK2bRiV284QgL8MwEjAneeo9VNOEaBhzALGtoRy02cIcWhE34jj5YxgW+E5Z4iTPkMYpPLCNY3hdOYEfNbKYdmNngZ1jyEzw7h7AIb3fRTQ95OAZ6yQpGYHMMtOTgouktYwxuXsHgWLLl+4x++Kx1FJrjLTagA77bTPvYgw1rRqY56e+w7GNYsqX6JfPwi7aR+Y5SA+BXtKIRfkfJAYgj14tpOF6+I46c4/cAM3UhM3JxyKsxiOIhH0IO6SH/A1Kb1WBeUjbkAAAAAElFTkSuQmCC)](https://forum.image.sc/tag/fiji)

[ Fiji Is Just ImageJ ]
=======================

Fiji is a "batteries-included" distribution of
[ImageJ](http://imagej.net/)—a popular, free scientific image processing application—which includes a lot of plugins organized into a coherent menu structure. Fiji compares to ImageJ as Ubuntu compares to Linux.

The primary goal of Fiji is to assist research in life sciences by making ImageJ and its plugins more accessible and easier to install and manage.

## Key Features

- **Pre-installed Plugins**: Ready-to-use plugins for various image processing tasks.
- **Cross-Platform**: Available on Windows, Linux, and MacOS.
- **Easy Updates**: Automatic updates for ImageJ and its plugins.
- **Open Source**: Fully transparent and open to contributions.

## Supported Platforms

- **Windows**: Intel 32-bit/64-bit
- **Linux**: Intel 32-bit/64-bit
- **macOS**: Intel 32-bit/64-bit (partial support for PowerPC 32-bit)
- **Other**: all platforms supporting Java and a POSIX shell, via `bin/ImageJ.sh`

## Installation

Fiji is intended to be the most painless, easy, quick and convenient way to install ImageJ and plugins and keep everything up-to-date.

1. **Download** the portable archive from the [official website](https://imagej.net/software/fiji/downloads).
2. **Unpack** the archive to a directory of your choice.
3. **Launch** Fiji by double-clicking the [ImageJ launcher](https://github.com/imagej/imagej-launcher).

Usage
-----

Fiji is distributed as a portable application, meaning it can be run without installation directly from wherever you copy it. By default, Fiji is distributed without source code to keep the download size as small as possible.

## Running Fiji

To start Fiji:
1. **Launch** Fiji by double-clicking the ImageJ launcher, which will set up the environment, start Java, and open ImageJ.
2. Alternatively, you can run Fiji from the command line:


### Passing Arguments
You can pass arguments to ImageJ directly through the command line. For example:

	$ $ ./ImageJ-linux32 --some-argument

#### Passing Arguments to the Java Virtual Machine (JVM)
To pass arguments to the Java Virtual Machine, specify them on the command line, separating them from the ImageJ arguments (if any) with a `--`.  In other words, if you want to override the memory setting, call Fiji like this:

	$ $ ./ImageJ-linux32 -Xmx128m --
This will set the maximum memory usage for Fiji to 128 MB.

Getting Help
-----------
If you're new to Fiji or ImageJ, there are multiple resources available to assist you:
- **Documentation**: Extensive guides are available on the [Fiji website](https://imagej.net/software/fiji/).
- **Community Support**: Join discussions on the [Image.sc Forum](https://forum.image.sc/tag/fiji).
- **Developer Chat**: Engage with developers on [Zulip](https://imagesc.zulipchat.com/#narrow/stream/327238-Fiji).

Open Source
-----------

Fiji is dedicated to open source. Not only does open source allow other developers to port the application to new platforms that the original authors did not begin to think of, it allows scientists to study the code to understand the inner workings of the algorithms used, and it permits others to use the program in totally new ways, and enhance it in all imaginable ways.

Therefore, the majority of Fiji is licensed under the GNU Public License version 2. Exceptions are listed in the
[LICENSES](https://github.com/fiji/fiji/blob/master/LICENSES) file.

Fiji's source code is split up into a [main repository](https://github.com/fiji/fiji), containing the top-level project and support scripts, while all components live in their own repositories in the [Fiji organization on GitHub](https://github.com/fiji/). As a rule of thumb: the file name and the project name correspond pretty well, e.g. fiji-compat.jar is maintained in [fiji-compat](https://github.com/fiji/fiji-compat).

Contributing to Fiji
-------------

Contributions are encouraged! Here's how you can get involved:

1. **Fork the repository**: Start by forking the [Fiji main repository](https://github.com/fiji/fiji).
2. **Make changes**: Develop new features or improve existing ones.
3. **Submit a Pull Request**: Once your changes are ready, submit a [Pull Requests](https://help.github.com/articles/using-pull-requests) for review. Detailed guidelines for contributing can be found on the [Contributing](http://imagej.net/Contributing) page. 

Credits
-------

- Fiji was created by [Johannes Schindelin](https://imagej.net/User:Schindelin). 
  - It is currently maintained by [Curtis Rueden](https://imagej.net/User:Rueden) of [LOCI](https://imagej.net/LOCI) at the University of Wisconsin-Madison.
- ImageJ 1.x was created and is maintained by [Wayne Rasband](https://imagej.net/Wayne_Rasband).
- ImageJ2 was created and is maintained and actively developed by Curtis Rueden.
- For a list of most recent contributors, please refer to the [Contributors](http://imagej.net/Contributors) page of the ImageJ wiki.

Thanks
------

We are very grateful to Wayne Rasband, who is not only a very dedicated developer of ImageJ 1.x; he also fosters an active and friendly community around ImageJ.

We are especially grateful to be part of an outstanding [community](http://imagej.net/Community) who is active, friendly and helping to scientists understanding and analysing images every day.

Oh, and Fiji is also an island. We just wanted to let you know.