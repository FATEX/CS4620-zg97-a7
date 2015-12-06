package cs4620.ray2.surface;

import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import cs4620.mesh.MeshData;
import cs4620.ray2.IntersectionRecord;
import cs4620.ray2.Ray;
import egl.math.Vector3d;

/**
 * A class that represents an Axis-Aligned box. When the scene is built, the Box
 * is split up into a Mesh of 12 Triangles.
 * 
 * @author sjm324
 *
 */
public class Box extends Surface {

	/* The mesh that represents this Box. */
	private Mesh mesh;

	/* The corner of the box with the smallest x, y, and z components. */
	protected final Vector3d minPt = new Vector3d();

	public void setMinPt(Vector3d minPt) {
		this.minPt.set(minPt);
	}

	/* The corner of the box with the largest x, y, and z components. */
	protected final Vector3d maxPt = new Vector3d();

	public void setMaxPt(Vector3d maxPt) {
		this.maxPt.set(maxPt);
	}

	/* Generate a Triangle mesh that represents this Box. */
	private void buildMesh() {
		// Create the OBJMesh
		MeshData box = new MeshData();

		box.vertexCount = 8;
		box.indexCount = 36;

		// Add positions
		box.positions = BufferUtils.createFloatBuffer(box.vertexCount * 3);
		box.positions.put(new float[] { 
				(float) minPt.x, (float) minPt.y, (float) minPt.z, 
				(float) minPt.x, (float) maxPt.y, (float) minPt.z,
				(float) maxPt.x, (float) maxPt.y, (float) minPt.z, 
				(float) maxPt.x, (float) minPt.y, (float) minPt.z, 
				(float) minPt.x, (float) minPt.y, (float) maxPt.z, 
				(float) minPt.x, (float) maxPt.y, (float) maxPt.z, 
				(float) maxPt.x, (float) maxPt.y, (float) maxPt.z, 
				(float) maxPt.x, (float) minPt.y, (float) maxPt.z });

		box.indices = BufferUtils.createIntBuffer(box.indexCount);
		box.indices.put(new int[] { 0, 1, 2, 0, 2, 3, 0, 5, 1, 0, 4, 5, 0, 7,
				4, 0, 3, 7, 4, 6, 5, 4, 7, 6, 2, 5, 6, 2, 1, 5, 2, 6, 7, 2, 7,
				3 });
		this.mesh = new Mesh(box);
		
		//set transformations and absorptioins
		this.mesh.setTransformation(this.tMat, this.tMatInv, this.tMatTInv);
		
		this.mesh.shader = this.shader;
	}

	public void computeBoundingBox() {
		// TODO#A7: Compute the bounding box and store the result in
		// averagePosition, minBound, and maxBound.
		// Hint: The bounding box is not the same as just minPt and maxPt,
		// because
		// this object can be transformed by a transformation matrix.
   
        Vector3d p1 = this.tMat.clone().mulPos(new Vector3d(minPt.x, minPt.y, minPt.z));
        Vector3d p2 = this.tMat.clone().mulPos(new Vector3d(minPt.x, maxPt.y, minPt.z));
        Vector3d p3 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, maxPt.y, minPt.z));
        Vector3d p4 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, minPt.y, minPt.z));
        
        Vector3d p5 = this.tMat.clone().mulPos(new Vector3d(minPt.x, minPt.y, maxPt.z));
        Vector3d p6 = this.tMat.clone().mulPos(new Vector3d(minPt.x, maxPt.y, maxPt.z));
        Vector3d p7 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, maxPt.y, maxPt.z));
        Vector3d p8 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, minPt.y, maxPt.z));
        
        ArrayList<Vector3d> points = new ArrayList<Vector3d>();
        points.add(p1); points.add(p2);
        points.add(p3); points.add(p4);
        
        points.add(p5); points.add(p6);
        points.add(p7); points.add(p8);
        
//        double minX = p1.x, minY = p1.y, minZ = p1.y;
//        double maxX, maxY, maxZ;
        System.out.println("### debuging ###" + this.minBound);
        this.minBound = new Vector3d();
        this.maxBound = new Vector3d();
        
        this.minBound.set(p1);
        this.minBound.set(p8);
        for (Vector3d point: points) {
        	if(point.x < this.minBound.x) this.minBound.x = point.x;
        	if(point.x > this.maxBound.x) this.maxBound.x = point.x;
        	
        	if(point.y < this.minBound.y) this.minBound.y = point.y;
        	if(point.y > this.minBound.y) this.maxBound.y = point.y;
        	
        	if(point.z < this.minBound.z) this.minBound.z = point.z;
        	if(point.z > this.minBound.z) this.maxBound.z = point.z;
        	
        }
        
        this.averagePosition = new Vector3d();
        this.averagePosition.set(
        		(this.maxBound.x + this.minBound.x) / 2, 
        		(this.maxBound.y + this.minBound.y) / 2, 
        		(this.maxBound.z + this.minBound.z) / 2);
        
	}

	public boolean intersect(IntersectionRecord outRecord, Ray ray) {
		return false;
	}

	public void appendRenderableSurfaces(ArrayList<Surface> in) {
		buildMesh();
		mesh.appendRenderableSurfaces(in);
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		return "Box ";
	}

}
