#@ ImageJ ij
#@ Dataset dataset

import napari

from superqt import ensure_main_thread

@ensure_main_thread
def show(data, **kwargs):
	napari.imshow(data)

arr = ij.py.from_java(dataset)
show(arr)
print("Done")
