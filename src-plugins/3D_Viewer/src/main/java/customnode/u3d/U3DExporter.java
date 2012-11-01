package customnode.u3d;

import ij3d.Content;
import ij3d.ContentNode;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import isosurface.MeshGroup;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import customnode.CustomMeshNode;
import customnode.CustomTriangleMesh;

/**
 * Some links:
 * http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-363%204th%20Edition.pdf
 * http://www3.math.tu-berlin.de/jreality/download/jr/src-io/de/jreality/writer/u3d/
 * http://u3d.svn.sourceforge.net/viewvc/u3d/trunk/Source/Samples/Data/
 * http://www.ctan.org/tex-archive/macros/latex/contrib/movie15/
 *
 */
public class U3DExporter {

	public static void main(String[] args) throws IOException {
		Image3DUniverse univ = new Image3DUniverse();
		univ.show();

		// List<Point3f> l = customnode.MeshMaker.createSphere(0, 0, 0, 1f);
		List<Point3f> l = customnode.MeshMaker.createIcosahedron(1, 1f);
		CustomTriangleMesh ctm = new CustomTriangleMesh(l);
		ctm.setColor(new Color3f(0f, 1f, 0f));
		univ.addCustomMesh(ctm, "icosahedron");

		l = customnode.MeshMaker.createSphere(1, 0, 0, 0.5f);
		ctm = new CustomTriangleMesh(l);
		ctm.setColor(new Color3f(1f, 0f, 0f));
		univ.addCustomMesh(ctm, "sphere");

		U3DExporter.export(univ, "/tmp/sphere.u3d");
	}

	private static final int uACContextBaseShadingID = 1;
	private static final int uACStaticFull           = 0x00000400;
	private static final int uACMaxRange             = uACStaticFull + 0x00003FFF;

	public static String getTexStub(Image3DUniverse univ, String path) {
		Color3f bg = new Color3f();
		((ImageCanvas3D)univ.getCanvas()).getBG().getColor(bg);
		return
		"\\documentclass[a4paper]{article}\n" +
		"\\usepackage[english]{babel}\n" +
		"\\usepackage{hyperref}\n" +
		"\\usepackage[3D]{movie15}\n" +
		"\n" +
		"\\begin{document}\n" +
		"\\includemovie[poster,label=my_label,3Dlights=Headlamp,3Dbg=" + bg.x + " " + bg.y + " " + bg.z + ",3Dcoo=0 0 0,3Droo=2.4]{.8\\linewidth}{.8\\linewidth}{%\n" +
		path + "%\n}\\ \n" +
		"% \\movieref[3Dcalculate]{my_label}{Click here!}\n" +
		"\\end{document}";
	}

	public static void export(Image3DUniverse univ, String path) throws IOException {

		Point3f min = new Point3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		Point3f max = new Point3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		List<Mesh> meshes = new ArrayList<Mesh>();

		for(Object content : univ.getContents()) {
			ContentNode cn = ((Content)content).getContent();
			CustomTriangleMesh ctm = null;
			if(cn instanceof CustomMeshNode) {
				ctm = (CustomTriangleMesh)((CustomMeshNode)cn).getMesh();
			} else if(cn instanceof MeshGroup) {
				if(((MeshGroup)cn).getMesh() instanceof CustomTriangleMesh)
					ctm = ((MeshGroup)cn).getMesh();
			}

			if(ctm != null) {
				Content c = (Content)content;
				Mesh m = new Mesh(ctm, c.getName(), c.getColor(), c.getTransparency());
				m.getMinMax(min, max);
				meshes.add(m);
			}
		}

		ByteArrayOutputStream bOutDecl = new ByteArrayOutputStream();
		ByteArrayOutputStream bOutCont = new ByteArrayOutputStream();
		WritableByteChannel oDecl = java.nio.channels.Channels.newChannel(bOutDecl);
		WritableByteChannel oCont = java.nio.channels.Channels.newChannel(bOutCont);
		int ds = 0;
		int cs = 0;
		DataBlock b;
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN);

		b = getViewModifierChain("DefaultView", "SceneViewResource");
		ds += writeDataBlock(b, oDecl, buffer);

		for(Mesh mesh : meshes) {
			String n = mesh.name;
			mesh.normalizeCoords(min, max);

			float[] matrix = new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
			String nodeModifierChainName = n; // "Box01";
			String modelNodeName         = n; // "Box01";
			String modelResourceName     = n; // "LightBoxModel";
			String shadingName           = n; // "Box01";
			String shaderName            = n; // "Box010";

			b = getNodeModifierChain(nodeModifierChainName, modelNodeName, modelResourceName, shadingName, shaderName, matrix);
			ds += writeDataBlock(b, oDecl, buffer);

			String modelModifierChainName = n; // "LightBoxModel";
			String meshname               = n; // "LightBoxModel";

			b = getModelResourceModifierChain(modelModifierChainName, mesh, meshname);
			ds += writeDataBlock(b, oDecl, buffer);

			String materialName = n; // "Box010";
			b = getLitTextureShaderBlock(shaderName, materialName);
			ds += writeDataBlock(b, oDecl, buffer);

			b = getMaterialResourceBlock(materialName, mesh.color.x, mesh.color.y, mesh.color.z, mesh.color.w);
			ds += writeDataBlock(b, oDecl, buffer);

			b = getMeshContinuationBlock(mesh, meshname);
			cs += writeDataBlock(b, oCont, buffer);
		}

		String lightModifierChainName = "Omni01";
		String lightResourceName      = "DefaultPointLight";
		b = getLightModifierChain(lightModifierChainName, lightResourceName);
		ds += writeDataBlock(b, oDecl, buffer);

		b = getViewResourceBlock("SceneViewResource");
		ds += writeDataBlock(b, oDecl, buffer);

		b = getLightResourceBlock("DefaultPointLight");
		ds += writeDataBlock(b, oDecl, buffer);


		// write header and data
		OutputStream out = new FileOutputStream(path);
		WritableByteChannel o = java.nio.channels.Channels.newChannel(out);
		writeDataBlock(getHeaderBlock(ds, cs), o, buffer);
		bOutDecl.writeTo(out);
		bOutCont.writeTo(out);
		out.close();
	}

	private static int writeDataBlock(DataBlock b, WritableByteChannel o, ByteBuffer buffer) throws IOException {

	        int dataSize = (int)Math.ceil(b.getDataSize() / 4.0); // include padding
	        int metaDataSize = (int)Math.ceil(b.getMetaDataSize() / 4.0); // include padding

	        int blockLength = (12 + 4 * (dataSize + metaDataSize));
	        if (buffer.capacity() < blockLength) {
	                buffer = ByteBuffer.allocate(blockLength);
	                buffer.order(ByteOrder.LITTLE_ENDIAN);
	        }
	        buffer.position(0);
	        buffer.limit(blockLength);

	        buffer.putInt((int)b.getBlockType());
	        buffer.putInt((int)b.getDataSize());
	        buffer.putInt((int)b.getMetaDataSize());

	        for (int i = 0; i < dataSize; i++)
	                buffer.putInt((int)b.getData()[i]);
	        for (int i = 0; i < metaDataSize; i++)
	                buffer.putInt((int)b.getMetaData()[i]);
	        buffer.rewind();
	        o.write(buffer);
	        return blockLength;
	}

	private static DataBlock getHeaderBlock(int declSize, long contSize) {
		BitStreamWrite w = new BitStreamWrite();
		w.WriteI16((short)256);               // major version
	        w.WriteI16((short)0);                 // minor version
	        w.WriteU32(0);                        // profile identifier TODO
	        w.WriteU32(36 + declSize);            // declaration size
	        w.WriteU64(36 + declSize + contSize); // file size
	        w.WriteU32(106);                      // character encoding: 106 = UTF-8
	        DataBlock b = w.GetDataBlock();
	        b.setBlockType(0x00443355);           // file header block
	        return b;
	}

	private static void WriteMatrix(BitStreamWrite w, float[] mat) {
		for(int i = 0; i < mat.length; i++)
			w.WriteF32(mat[i]);
	}

	private static DataBlock getModelNodeBlock(String modelNodeName, String modelResourceName, float[] matrix) {
		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff22;   // model node block
		w.WriteString(modelNodeName);          // model node name
		w.WriteU32(1);                         // parent node count
		w.WriteString("");                     // parent node name
		WriteMatrix(w, matrix);                // transformation
		w.WriteString(modelResourceName);      // model resource name
		w.WriteU32(3);                         // visibility 3 = front and back
		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getShadingModifierBlock(String shadingModName, String shaderName ) {
		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff45;   // shading modifier block
		w.WriteString(shadingModName);         // shading modifier name
		w.WriteU32(1);                         // chain index
		w.WriteU32(15);                        // shading attributes
		w.WriteU32(1);                         // shading list count
		w.WriteU32(1);                         // shader count
		w.WriteString(shaderName);             // shader name
		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	public static DataBlock getLitTextureShaderBlock(String shaderName, String materialName) {
		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff53;   // Lit texture shader block
		w.WriteString(shaderName);
		w.WriteU32(5);                         // Lit texture shader attributes: 1 = Lights enabled, 2 = Alpha test enabled, 4 = Use vertex color
		w.WriteF32(0f);                        // Alpha Test Reference
		w.WriteU32(0x00000617);                // Alpha Test Function: ALWAYS
		w.WriteU32(0x00000606);                // Color Blend Function: ALPHA_BLEND
		w.WriteU32(1);                         // Render pass enabled flags
		w.WriteU32(0);                         // Shader channels
		w.WriteU32(0);                         // Alpha texture channels
		w.WriteString(materialName);           // Material name
		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	public static DataBlock getMaterialResourceBlock(String materialName, float r, float g, float b, float a) {
		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff54;   // Material resource block
		w.WriteString(materialName);           // Material resource name
		w.WriteU32(0x0000003f);                // Material attributes: use all colors, opacity and reflectivity 0x0000003f
		w.WriteF32(0.1f);                      // Ambient red
		w.WriteF32(0.1f);                      // Ambient green
		w.WriteF32(0.1f);                      // Ambient blue
		w.WriteF32(r);                         // Diffuse red
		w.WriteF32(g);                         // Diffuse green
		w.WriteF32(b);                         // Diffuse blue
		w.WriteF32(0.1f);                      // Specular red
		w.WriteF32(0.1f);                      // Specular green
		w.WriteF32(0.1f);                      // Specular blue
		w.WriteF32(0.1f);                      // Emissive red
		w.WriteF32(0.1f);                      // Emissive green
		w.WriteF32(0.1f);                      // Emissive blue
		w.WriteF32(0.0f);                      // Reflectivity
		w.WriteF32(a);                         // Opacity
		DataBlock bl = w.GetDataBlock();
		bl.setBlockType(type);
		return bl;
	}

	private static DataBlock getViewModifierChain(String viewname, String viewResourceName) {
		long type = (0xffff << 16) | 0xff14;  // modifier chain block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(viewname);              // modifier chain name
		w.WriteU32(0);                        // modifier chain type: 0 = node modifier chain
		w.WriteU32(0);                        // modifier chain attributes: 0 = neither bounding sphere nor bounding box info present
		w.AlignTo4Byte();
		w.WriteU32(1);                        // modifier count in this chain

		w.WriteDataBlock(getViewNodeBlock(viewname, viewResourceName));

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getViewNodeBlock(String viewname, String viewResourceName) {
		long type = (0xffff << 16) | 0xff24;  // view node block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(viewname);              // view node name
		w.WriteU32(1);                        // parent node count
		w.WriteString("");                    // parent name
		WriteMatrix(w, new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});  // parent transform
		w.WriteString(viewResourceName);      // view resource name
		w.WriteU32(0);                        // view node attributes - 0: default attributes
		w.WriteF32(1f);                       // view near clipping
		w.WriteF32(Float.MAX_VALUE);          // view far clipping
		w.WriteF32(34.5f);                    // view projection
		w.WriteF32(500f);                     // view port width
		w.WriteF32(500f);                     // view port height
		w.WriteF32(0);                        // view port x
		w.WriteF32(0);                        // view port y
		w.WriteU32(0);                        // view backdrop count
		w.WriteU32(0);                        // view overlay count

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getViewResourceBlock(String viewResourceName) {
		long type = (0xffff << 16) | 0xff52;  // view resource block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(viewResourceName);      // view resource name
		w.WriteU32(1);                        // view pass count
		w.WriteString("");                    // root node name
		w.WriteU32(0);                        // render attributes: fog disabled
		w.WriteU32(1);                        // fog mode
		w.WriteF32(0);                        // fog red
		w.WriteF32(0);                        // fog green
		w.WriteF32(0);                        // fog blue
		w.WriteF32(0);                        // fog alpha
		w.WriteF32(0);                        // fog near value
		w.WriteF32(1000);                     // fog far value

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getNodeModifierChain(String modifierChainName, String modelNodeName, String modelResourceName, String shadingName, String shaderName, float[] matrix) {
		long type = (0xffff << 16) | 0xff14;  // modifier chain block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(modifierChainName);     // modifier chain name
		w.WriteU32(0);                        // modifier chain type: 0 = node modifier chain
		w.WriteU32(0);                        // modifier chain attributes: 0 = neither bounding sphere nor bounding box info present
		w.AlignTo4Byte();
		w.WriteU32(2);                        // modifier count in this chain

		w.WriteDataBlock(getModelNodeBlock(modelNodeName, modelResourceName, matrix));
		w.WriteDataBlock(getShadingModifierBlock(shadingName, shaderName));

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getLightModifierChain(String modifierChainName, String lightResourceName) {
		long type = (0xffff << 16) | 0xff14;  // modifier chain block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(modifierChainName);     // modifier chain name
		w.WriteU32(0);                        // modifier chain type: 0 = node modifier chain
		w.WriteU32(0);                        // modifier chain attributes: 0 = neither bounding sphere nor bounding box info present
		w.AlignTo4Byte();
		w.WriteU32(1);                        // modifier count in this chain

		w.WriteDataBlock(getLightNodeBlock(modifierChainName, lightResourceName));

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	public static DataBlock getLightNodeBlock(String lightNodeName, String lightResourceName) {
		float[] matrix = new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 6f, -20f, 4f, 1.0f };
		long type = (0xffff << 16) | 0xff23;  // Light node block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(lightNodeName);         // light node name
		w.WriteU32(1);                        // parent node count
		w.WriteString("");                    // parent node name
		WriteMatrix(w, matrix);               // transformation
		w.WriteString(lightResourceName);     // Light resource name

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	public static DataBlock getLightResourceBlock(String lightResourceName) {
		long type = (0xffff << 16) | 0xff51;  // Light node block
		BitStreamWrite w = new BitStreamWrite();
		w.WriteString(lightResourceName);
		w.WriteU32(1);                        // Light attributes: enable
		w.WriteU8 ((short)2);                 // Light type: Point light
		w.WriteF32(.5f);                      // Light color red
		w.WriteF32(.5f);                      // Light color green
		w.WriteF32(.5f);                      // Light color blue
		w.WriteF32(1f);                       // Reserved, shall be 1
		w.WriteF32(0.1f);                     // Light attenuation constant factor
		w.WriteF32(0f);                       // Light attenuation linear factor
		w.WriteF32(0f);                       // Light attenuation quadratic factor
		w.WriteF32(180f);                     // Light spot angle
		w.WriteF32(.5f);                      // Light intensity

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getMeshDeclarationBlock(Mesh mesh, String meshname) {
		Point3f[] coords   = mesh.coords;
		Vector3f[] normals = mesh.normals;
		Color3f[] colors   = mesh.colors;
		int[] coordIndices = mesh.coordIndices;
		int[] normalIndices= mesh.normalIndices;
		int[] colorIndices = mesh.colorIndices;

		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff31;  // mesh declaration block
		w.WriteString(meshname);              // mesh name
		w.WriteU32(0);                        // chain index
		// max mesh description
		w.WriteU32(0);                        // mesh attributes: 1 = no normals
		w.WriteU32(coordIndices.length / 3);  // face count
		w.WriteU32(coords.length);            // positions count
		w.WriteU32(normals.length);           // normal count
//		w.WriteU32(colors.length);            // diffuse color count
w.WriteU32(0);
		w.WriteU32(0);                        // specular color count
		w.WriteU32(0);                        // texture coord count
		w.WriteU32(1);                        // shading count
		// shading description
// 		w.WriteU32(1);                        // shading attributes: 0 = the shader list uses neither diffuse nor specular colors
w.WriteU32(0);
		w.WriteU32(0);                        // texture layer count
		// w.WriteU32(2);
		w.WriteU32(0);                        // original shader id
		// clod desc
		w.WriteU32(coords.length);            // minimum resolution
		w.WriteU32(coords.length);            // maximum resolution
		w.WriteU32(300);                      // position quality factor
		w.WriteU32(300);                      // normal quality factor
		w.WriteU32(300);                      // texture coord quality factor
		w.WriteF32(0.01f);                    // position inverse quant
		w.WriteF32(0.01f);                    // normal inverse quant
		w.WriteF32(0.01f);                    // texture coord inverse quant
		w.WriteF32(0.01f);                    // diffuse color inverse quant
		w.WriteF32(0.01f);                    // specular color inverse quant
		w.WriteF32(0.9f);                     // normal crease parameter
		w.WriteF32(0.5f);                     // normal update parameter
		w.WriteF32(0.985f);                   // normal tolerance parameter
		w.WriteU32(0);                        // bone count

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getModelResourceModifierChain(String modifierChainName, Mesh mesh, String meshname) {
		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff14;  // modifier chain block
		w.WriteString(modifierChainName);     // modifier chain name
		w.WriteU32(1);                        // modifier chain type: 1 = model resource modifier chain
		w.WriteU32(0);                        // modifier chain attributes: 0 = neither bounding sphere nor bounding box info present
		// padding
		w.AlignTo4Byte();
		w.WriteU32(1);                        // modifier count in this chain

		w.WriteDataBlock(getMeshDeclarationBlock(mesh, meshname));
		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	private static DataBlock getMeshContinuationBlock(Mesh mesh, String meshname) {
		Point3f[] coords   = mesh.coords;
		Vector3f[] normals = mesh.normals;
		Color3f[] colors   = mesh.colors;
		int[] coordIndices = mesh.coordIndices;
		int[] normalIndices= mesh.normalIndices;
		int[] colorIndices = mesh.colorIndices;


		BitStreamWrite w = new BitStreamWrite();
		long type = (0xffff << 16) | 0xff3b;  // mesh continuation block
		w.WriteString(meshname);              // mesh name
		w.WriteU32(0);                        // chain index
		w.WriteU32(coordIndices.length / 3);  // base face count
		w.WriteU32(coords.length);            // base position count
		w.WriteU32(normals.length);           // base normal count
//		w.WriteU32(colors.length);            // base diffuse color count
 w.WriteU32(0);
		w.WriteU32(0);                        // base specular color count
		w.WriteU32(0);                        // base texture coord count


		for(int i = 0; i < coords.length; i++) {
			w.WriteF32(coords[i].x);      // base position x
			w.WriteF32(coords[i].y);      // base position y
			w.WriteF32(coords[i].z);      // base position z
		}
		for(int i = 0; i < normals.length; i++) {
			w.WriteF32(normals[i].x);     // base normal x
			w.WriteF32(normals[i].y);     // base normal y
			w.WriteF32(normals[i].z);     // base normal z
		}
/*
		for(int i = 0; i < colors.length; i++) {
			w.WriteF32(colors[i].x);     // base colors red
			w.WriteF32(colors[i].y);     // base colors green
			w.WriteF32(colors[i].z);     // base colors blue
			w.WriteF32(1);               // base colors alpha
		}
*/

		for(int i = 0; i < coordIndices.length; i += 3) {
			w.WriteCompressedU32(uACContextBaseShadingID, 0);  // shading id
			for (int j = 0; j < 3; j++) {
		 		w.WriteCompressedU32(uACStaticFull + coords.length,  coordIndices [i + j]);
		 		w.WriteCompressedU32(uACStaticFull + normals.length, normalIndices[i + j]);
// w.WriteCompressedU32(uACStaticFull + colors.length,  colorIndices [i + j]);
			}
		}

		DataBlock b = w.GetDataBlock();
		b.setBlockType(type);
		return b;
	}

	public static class Mesh {
		Point3f[] coords;
		Vector3f[] normals;
		Color3f[] colors;
		int[] coordIndices, normalIndices, colorIndices;
		CustomTriangleMesh ctm;
		String name;
		Color4f color;

		public void getMinMax(Point3f min, Point3f max) {
			for(Point3f p : coords) {
				if(p.x > max.x) max.x = p.x;
				if(p.y > max.y) max.y = p.y;
				if(p.z > max.z) max.z = p.z;
				if(p.x < min.x) min.x = p.x;
				if(p.y < min.y) min.y = p.y;
				if(p.z < min.z) min.z = p.z;
			}
		}

		public void normalizeCoords(Point3f min, Point3f max) {
			float dx = max.x - min.x;
			float dy = max.y - min.y;
			float dz = max.z - min.z;
			float maxd = Math.max(dx, Math.max(dy, dz));

			Point3f center = new Point3f();
			center.add(min, max);
			center.scale(0.5f);

			for(Point3f p : coords) {
				p.sub(center);
				p.scale(1 / maxd);
			}
		}

		public Mesh(CustomTriangleMesh mesh, String name, Color3f color, float transparency) {
			this.name = name;
			this.ctm = mesh;
			this.color = new Color4f(color.x, color.y, color.z, 1f - transparency);
			TriangleArray g = (TriangleArray)mesh.getGeometry();
			int N = g.getValidVertexCount();
			coords = new Point3f[N];
			colors = new Color3f[N];
			normals = new Vector3f[N];
			for(int i = 0; i < N; i++) {
				coords[i] = new Point3f();
				colors[i] = new Color3f();
				normals[i] = new Vector3f();
			}
			g.getCoordinates(0, coords);
			g.getColors(0, colors);
			g.getNormals(0, normals);

			// create indices
			Map<Point3f, Integer>  vertexToIndex = new HashMap<Point3f, Integer>();
			Map<Color3f, Integer>  colorToIndex  = new HashMap<Color3f, Integer>();
			Map<Vector3f, Integer> normalToIndex = new HashMap<Vector3f, Integer>();

			int nFaces = N;
			coordIndices  = new int[nFaces];
			colorIndices  = new int[nFaces];
			normalIndices = new int[nFaces];

			List<Point3f>  vList = new ArrayList<Point3f>();
			List<Color3f>  cList = new ArrayList<Color3f>();
			List<Vector3f> nList = new ArrayList<Vector3f>();

			for(int i = 0; i < N; i++) {
				Point3f v  = coords[i];
				Color3f c  = colors[i];
				Vector3f n = normals[i];

				if(!vertexToIndex.containsKey(v)) {
					Point3f newp = new Point3f(v);
					vertexToIndex.put(newp, vList.size());
					vList.add(newp);
				}
				coordIndices[i] = vertexToIndex.get(v);
				if(!colorToIndex.containsKey(c)) {
					Color3f newc = new Color3f(c);
					colorToIndex.put(newc, cList.size());
					cList.add(newc);
				}
				colorIndices[i] = colorToIndex.get(c);
				if(!normalToIndex.containsKey(n)) {
					Vector3f newn = new Vector3f(n);
					normalToIndex.put(newn, nList.size());
					nList.add(newn);
				}
				normalIndices[i] = normalToIndex.get(n);
			}

			coords = new Point3f[vList.size()];
			vList.toArray(coords);

			normals = new Vector3f[nList.size()];
			nList.toArray(normals);

			colors = new Color3f[cList.size()];
			cList.toArray(colors);
		}
	}
}
