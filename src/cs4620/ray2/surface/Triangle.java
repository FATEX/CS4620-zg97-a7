package cs4620.ray2.surface;

import java.util.ArrayList;

import cs4620.ray2.IntersectionRecord;
import cs4620.ray2.Ray;
import egl.math.Vector3d;
import egl.math.Vector3i;
import cs4620.ray2.shader.Shader;

/**
 * Represents a single triangle, part of a triangle mesh
 *
 * @author ags
 */
public class Triangle extends Surface {
	/** The normal vector of this triangle, if vertex normals are not specified */
	Vector3d norm;

	/** The mesh that contains this triangle */
	Mesh owner;

	/** 3 indices to the vertices of this triangle. */
	Vector3i index;

	double a, b, c, d, e, f;

	public Triangle(Mesh owner, Vector3i index, Shader shader) {
		this.owner = owner;
		this.index = new Vector3i(index);

		Vector3d v0 = owner.getPosition(index.x);
		Vector3d v1 = owner.getPosition(index.y);
		Vector3d v2 = owner.getPosition(index.z);

		if (!owner.hasNormals()) {
			Vector3d e0 = new Vector3d(), e1 = new Vector3d();
			e0.set(v1).sub(v0);
			e1.set(v2).sub(v0);
			norm = new Vector3d();
			norm.set(e0).cross(e1);
			norm.normalize();
		}
		a = v0.x - v1.x;
		b = v0.y - v1.y;
		c = v0.z - v1.z;

		d = v0.x - v2.x;
		e = v0.y - v2.y;
		f = v0.z - v2.z;

		this.setShader(shader);
	}

	/**
	 * Tests this surface for intersection with ray. If an intersection is found
	 * record is filled out with the information about the intersection and the
	 * method returns true. It returns false otherwise and the information in
	 * outRecord is not modified.
	 *
	 * @param outRecord
	 *            the output IntersectionRecord
	 * @param rayIn
	 *            the ray to intersect
	 * @return true if the surface intersects the ray
	 */
	public boolean intersect(IntersectionRecord outRecord, Ray rayIn) {
		//transform ray into object space
		Ray ray = untransformRay(rayIn);		
		
		Vector3d v0 = owner.getPosition(index.x).clone();
		
		double g = ray.direction.x;
		double h = ray.direction.y;
		double i = ray.direction.z;
		double j = v0.x - ray.origin.x;
		double k = v0.y - ray.origin.y;
		double l = v0.z - ray.origin.z;
		double M = a * (e * i - h * f) + b * (g * f - d * i) + c
				* (d * h - e * g);

		double ei_hf = e * i - h * f;
		double gf_di = g * f - d * i;
		double dh_eg = d * h - e * g;
		double ak_jb = a * k - j * b;
		double jc_al = j * c - a * l;
		double bl_kc = b * l - k * c;

		double t = -(f * (ak_jb) + e * (jc_al) + d * (bl_kc)) / M;
		if (t > ray.end || t < ray.start)
			return false;

		double beta = (j * (ei_hf) + k * (gf_di) + l * (dh_eg)) / M;
		if (beta < 0 || beta > 1)
			return false;

		double gamma = (i * (ak_jb) + h * (jc_al) + g * (bl_kc)) / M;
		if (gamma < 0 || gamma + beta > 1)
			return false;

		// There was an intersection, fill out the intersection record
		if (outRecord != null) {
			outRecord.t = t;
			ray.evaluate(outRecord.location, t);
			
			//transform back into world space
			tMat.mulPos(outRecord.location);		
			
			outRecord.surface = this;

			if (norm != null) {
				outRecord.normal.set(norm);
			} else {
				outRecord.normal
						.setZero()
						.addMultiple(1 - beta - gamma, owner.getNormal(index.x))
						.addMultiple(beta, owner.getNormal(index.y))
						.addMultiple(gamma, owner.getNormal(index.z));
			}
			
			tMatTInv.mulDir(outRecord.normal);
			
			outRecord.normal.normalize();
			if (owner.hasUVs()) {
				outRecord.texCoords.setZero()
						.addMultiple(1 - beta - gamma, owner.getUV(index.x))
						.addMultiple(beta, owner.getUV(index.y))
						.addMultiple(gamma, owner.getUV(index.z));
			}
		}

		return true;

	}

	public void computeBoundingBox() {
		// TODO#A7: Compute the bounding box and store the result in
		// averagePosition, minBound, and maxBound.
		Vector3d v0 = this.tMat.clone().mulPos(owner.getPosition(index.x));
		Vector3d v1 = this.tMat.clone().mulPos(owner.getPosition(index.y));
		Vector3d v2 = this.tMat.clone().mulPos(owner.getPosition(index.z));
		
		
		Vector3d minPt = new Vector3d(Math.min(v0.x, Math.min(v1.x, v2.x)),Math.min(v0.y, Math.min(v1.y, v2.y)), Math.min(v0.z, Math.min(v1.z, v2.z)));
		Vector3d maxPt = new Vector3d(Math.max(v0.x, Math.max(v1.x, v2.x)),Math.max(v0.y, Math.max(v1.y, v2.y)), Math.max(v0.z, Math.max(v1.z, v2.z)));
		
//		Vector3d p1 = this.tMat.clone().mulPos(minPt);
//        Vector3d p2 = this.tMat.clone().mulPos(new Vector3d(minPt.x,  maxPt.y, minPt.z));
//        Vector3d p3 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, maxPt.y, minPt.z));
//        Vector3d p4 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, minPt.y, minPt.z));
//        
//        Vector3d p5 = this.tMat.clone().mulPos(new Vector3d(minPt.x, minPt.y, maxPt.z));
//        Vector3d p6 = this.tMat.clone().mulPos(new Vector3d(minPt.x, maxPt.y, maxPt.z));
//        Vector3d p7 = this.tMat.clone().mulPos(new Vector3d(maxPt.x, maxPt.y, maxPt.z));
//        Vector3d p8 = this.tMat.clone().mulPos(maxPt);
		Vector3d p1 = minPt;
        Vector3d p2 = new Vector3d(minPt.x,  maxPt.y, minPt.z);
        Vector3d p3 = new Vector3d(maxPt.x, maxPt.y, minPt.z);
        Vector3d p4 = new Vector3d(maxPt.x, minPt.y, minPt.z);
        
        Vector3d p5 = new Vector3d(minPt.x, minPt.y, maxPt.z);
        Vector3d p6 = new Vector3d(minPt.x, maxPt.y, maxPt.z);
        Vector3d p7 = new Vector3d(maxPt.x, maxPt.y, maxPt.z);
        Vector3d p8 = maxPt;
        
        ArrayList<Vector3d> points = new ArrayList<Vector3d>();
        points.add(p1); points.add(p2);
        points.add(p3); points.add(p4);
        
        points.add(p5); points.add(p6);
        points.add(p7); points.add(p8);
     
        this.minBound = new Vector3d();
        this.maxBound = new Vector3d();
        
        this.minBound.set(p1);
        this.minBound.set(p8);
        for (Vector3d point: points) {
        	if(point.x < this.minBound.x) this.minBound.x = point.x;
        	if(point.x > this.maxBound.x) this.maxBound.x = point.x;
        	
        	if(point.y < this.minBound.y) this.minBound.y = point.y;
        	if(point.y > this.maxBound.y) this.maxBound.y = point.y;
        	
        	if(point.z < this.minBound.z) this.minBound.z = point.z;
        	if(point.z > this.maxBound.z) this.maxBound.z = point.z;
        	
        }
        
        
        
		this.averagePosition = new Vector3d();
		this.averagePosition.set(
        		(v0.x + v1.x + v2.x)/3, 
        		(v0.y + v1.y + v2.y)/3, 
        		(v0.z + v1.z + v2.z)/3);
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		return "Triangle ";
	}
}