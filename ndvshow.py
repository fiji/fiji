#@ ImageJ ij
#@ Dataset dataset

import ndv

from superqt import ensure_main_thread

@ensure_main_thread
def create_viewer(data, **kwargs):
	v = ndv.ArrayViewer(data, **kwargs)
	return v

@ensure_main_thread
def show_viewer(viewer, **kwargs):
    viewer.show()

arr = ij.py.from_java(dataset)
viewer = create_viewer(arr).result()
print(viewer)
show_viewer(viewer)
print("Done")
