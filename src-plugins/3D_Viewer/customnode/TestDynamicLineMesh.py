from ij3d import *;
from customnode import *;

from java.util import ArrayList;
from javax.vecmath import *;

univ = Image3DUniverse();
univ.show();

lines = [Point3f(-15, -11, 0), Point3f(+15, -11, 0)];
mesh = CustomLineMesh(lines, CustomLineMesh.CONTINUOUS, Color3f(1, 1, 0), 0);
univ.addCustomMesh(mesh, "bottom");


# TEST LINE MESH

lines = [];

mesh = CustomLineMesh(lines);
univ.addCustomMesh(mesh, "lines");

mesh.addLines([Point3f(-10, -10, 0)]);
Thread.sleep(1000);
mesh.addLines([Point3f(-10, +10, 0)]); # Das
Thread.sleep(1000);
mesh.addLines([Point3f(+10, -10, 0)]); # ist
Thread.sleep(1000);
mesh.addLines([Point3f(+10, +10, 0)]); # das
Thread.sleep(1000);
mesh.addLines([Point3f(0, +20, 0)]);   # Haus
Thread.sleep(1000);
mesh.addLines([Point3f(-10, +10, 0)]); # vom
Thread.sleep(1000);
mesh.addLines([Point3f(+10, +10, 0)]); # Ni-
Thread.sleep(1000);
mesh.addLines([Point3f(-10, -10, 0)]); # ko-
Thread.sleep(1000);
mesh.addLines([Point3f(+10, -10, 0)]); # laus


# TEST POINT MESH

points = [];
mesh = CustomPointMesh(points, Color3f(1, 1, 1), 0);
mesh.setPointSize(10);
univ.addCustomMesh(mesh, "points");

Thread.sleep(1000);
mesh.addPoint(Point3f(-10, -10, 0));
Thread.sleep(1000);
mesh.addPoint(Point3f(-10, +10, 0));
Thread.sleep(1000);
mesh.addPoint(Point3f(+10, -10, 0));
Thread.sleep(1000);
mesh.addPoint(Point3f(+10, +10, 0));
Thread.sleep(1000);
mesh.addPoint(Point3f( 0,  +20, 0));
Thread.sleep(1000);
mesh.addPoint(Point3f(-10, -10, 0));


# TEST TRIANGLE MESH

triangles = [];
mesh = CustomTriangleMesh(triangles, Color3f(1, 0.8, 0), 0);
c = univ.addCustomMesh(mesh, "triangles");

Thread.sleep(1000);
mesh.addTriangle(Point3f(-10, -10, 0), Point3f(+10, -10, 0), Point3f(0, 0, 0));
Thread.sleep(1000);
mesh.addTriangle(Point3f(+10, -10, 0), Point3f(+10, +10, 0), Point3f(0, 0, 0));
Thread.sleep(1000);
mesh.removeTriangle(1);
Thread.sleep(1000);
mesh.addTriangle(Point3f(+10, +10, 0), Point3f(-10, +10, 0), Point3f(0, 0, 0));
Thread.sleep(1000);
mesh.addTriangle(Point3f(-10, +10, 0), Point3f(-10, -10, 0), Point3f(0, 0, 0));
Thread.sleep(1000);
mesh.addTriangle(Point3f(-10, +10, 0), Point3f(+10, +10, 0), Point3f(0, 20, 0));
Thread.sleep(1000);

c.setShaded(0);

indices = mesh.indicesOfPoint(Point3f(0, 0, 0));
transf = Point3f();
for i in xrange(0, 10):
	transf.set(i, i, 0);
	mesh.setCoordinates(indices, transf);
	Thread.sleep(500);

for i in xrange(9, -1, -1):
	transf.set(i, i/2.0, 0);
	mesh.setCoordinates(indices, transf);
	Thread.sleep(500);


# TEST QUAD MESH

univ.removeAllContents();

quads = [Point3f(-32, -32, 0), Point3f(32, -32, 0), \
	Point3f(32, 32, 0), Point3f(-32, 32, 0)];
mesh = CustomQuadMesh(quads, Color3f(0, 0.1, 0.05), 0);
univ.addCustomMesh(mesh, "background");


# quads = [Point3f(-32, -32, 0), Point3f(-24, -32, 0), \
# 	Point3f(-24, -24, 0), Point3f(-32, -24, 0)];
quads = [];
mesh = CustomQuadMesh(quads, Color3f(0.5, 0.5, 0.5), 0);
univ.addCustomMesh(mesh, "quads");

for y in xrange(0, 8):
	yi = -32 + y * 8;
	for x in xrange(0, 4):
		xi = 0;
		if y % 2 != 0:
			xi = 8;
		xi = -32 + xi + x * 16;
		Thread.sleep(500);
		mesh.addQuad(Point3f(xi, yi, -0.1), Point3f(xi+8, yi, -0.1), \
			Point3f(xi+8, yi+8, -0.1), Point3f(xi, yi+8, -0.1));


