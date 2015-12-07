
package cs4620.ray2.accel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cs4620.ray2.IntersectionRecord;
import cs4620.ray2.Ray;
import cs4620.ray2.surface.Surface;
import egl.math.Vector3d;

/**
 * Class for Axis-Aligned-Bounding-Box to speed up the intersection look up time.
 *
 * @author ss932, pramook
 */
public class Bvh implements AccelStruct {   
	/** A shared surfaces array that will be used across every node in the tree. */
	private Surface[] surfaces;

	/** A comparator class that can sort surfaces by x, y, or z coordinate.
	 *  See the subclass declaration below for details.
	 */
	static MyComparator cmp = new MyComparator();
	
	/** The root of the BVH tree. */
	BvhNode root;

	public Bvh() { }

	/**
	 * Set outRecord to the first intersection of ray with the scene. Return true
	 * if there was an intersection and false otherwise. If no intersection was
	 * found outRecord is unchanged.
	 *
	 * @param outRecord the output IntersectionRecord
	 * @param ray the ray to intersect
	 * @param anyIntersection if true, will immediately return when found an intersection
	 * @return true if and intersection is found.
	 */
	public boolean intersect(IntersectionRecord outRecord, Ray rayIn, boolean anyIntersection) {
		boolean in = intersectHelper(root, outRecord, rayIn, anyIntersection);
		//System.out.println("### debugging intersect###" + in);
		return in;
	}
	
	/**
	 * A helper method to the main intersect method. It finds the intersection with
	 * any of the surfaces under the given BVH node.  
	 *   
	 * @param node a BVH node that we would like to find an intersection with surfaces under it
	 * @param outRecord the output InsersectionMethod
	 * @param rayIn the ray to intersect
	 * @param anyIntersection if true, will immediately return when found an intersection
	 * @return true if an intersection is found with any surface under the given node
	 */
	private boolean intersectHelper(BvhNode node, IntersectionRecord outRecord, Ray rayIn, boolean anyIntersection)
	{
		// TODO#A7: fill in this function.
		// Hint: For a leaf node, use a normal linear search. Otherwise, search in the left and right children.
		// Another hint: save time by checking if the ray intersects the node first before checking the childrens.
		 if(node == null) return false;
		 Ray cRay = new Ray(rayIn);
		 boolean hitted = false;
		 if (!node.intersects(cRay)) return false;
         if (node.child[0] == null && node.child[1] == null) {
        	 for(int i = node.surfaceIndexStart; i < node.surfaceIndexEnd; i++) {
        		 if(surfaces[i].intersect(outRecord, rayIn)) {
        			 hitted = true;
        			 //System.out.println("### debuging outRecord ###" + outRecord.surface);
        			 if(anyIntersection) return true;
        			 rayIn.end = outRecord.t;
        		 } 
        	 }
        	 
        	 if (hitted) return true;
        	 else return false;
         }
         else {
        	 boolean left = intersectHelper(node.child[0], outRecord, rayIn, anyIntersection);
        	 IntersectionRecord temp = new IntersectionRecord();
        	 if (left) temp.set(outRecord);
        	 
        	 boolean right = intersectHelper(node.child[1], outRecord, rayIn, anyIntersection);
        	 if (left && right && temp.t < outRecord.t) outRecord.set(temp);
             return left || right;  
         }
	}


	@Override
	public void build(Surface[] surfaces) {
		this.surfaces = surfaces;
		root = createTree(0, surfaces.length);
	}
	
	/**
	 * Create a BVH [sub]tree.  This tree node will be responsible for storing
	 * and processing surfaces[start] to surfaces[end-1]. If the range is small enough,
	 * this will create a leaf BvhNode. Otherwise, the surfaces will be sorted according
	 * to the axis of the axis-aligned bounding box that is widest, and split into 2
	 * children.
	 * 
	 * @param start The start index of surfaces
	 * @param end The end index of surfaces
	 */
	private BvhNode createTree(int start, int end) {
		// TODO#A7: fill in this function.

		// ==== Step 1 ====
		// Find out the BIG bounding box enclosing all the surfaces in the range [start, end)
		// and store them in minB and maxB.
		// Hint: To find the bounding box for each surface, use getMinBound() and getMaxBound() */
		//Surface[] surfL = new Surface[end - start];
		BvhNode node = new BvhNode();
		double minX = this.surfaces[start].getMinBound().x;
		double minY = this.surfaces[start].getMinBound().y;
		double minZ = this.surfaces[start].getMinBound().z;
		
		double maxX = this.surfaces[start].getMaxBound().x;
		double maxY = this.surfaces[start].getMaxBound().y;
		double maxZ = this.surfaces[start].getMaxBound().z;
		
        ArrayList<Surface> surfL = new ArrayList<Surface>();
		for (int i = start; i < end; i++) {
			//System.out.println("### debuging surface minB ###" + surfaces[i].getMinBound());
			//System.out.println("### debuging surface maxB ###" + surfaces[i].getMaxBound());
			if (minX > surfaces[i].getMinBound().x) minX = surfaces[i].getMinBound().x;
			if (maxX < surfaces[i].getMaxBound().x) maxX = surfaces[i].getMaxBound().x;
			
			if (minY > surfaces[i].getMinBound().y) minY = surfaces[i].getMinBound().y;
			if (maxY < surfaces[i].getMaxBound().y) maxY = surfaces[i].getMaxBound().y;
			
			if (minZ > surfaces[i].getMinBound().z) minZ = surfaces[i].getMinBound().z;
			if (maxZ < surfaces[i].getMaxBound().z) maxZ = surfaces[i].getMaxBound().z;
			
        	surfL.add(surfaces[i]);
        }
		node.surfaceIndexStart = start;
		node.surfaceIndexEnd = end;
		node.minBound.set(minX, minY, minZ);
		node.maxBound.set(maxX, maxY, maxZ);
		
		//System.out.println("### debuging node minB ###" + node.minBound);
		//System.out.println("### debuging node maxB ###" + node.maxBound);
		
		// ==== Step 2 ====
		// Check for the base case. 
		// If the range [start, end) is small enough (e.g. less than or equal to 10), just return a new leaf node.
		if (end - start <= 10) return node;

		// ==== Step 3 ====
		// Figure out the widest dimension (x or y or z).
		// If x is the widest, set widestDim = 0. If y, set widestDim = 1. If z, set widestDim = 2.
		double diffX = maxX - minX;
		double diffY = maxY - minY;
		double diffZ = maxZ - minZ;
		int widestDim = -1;
		
		if (diffX >= Math.max(diffY, diffZ)) widestDim = 0;
		else if (diffY >= Math.max(diffX, diffZ)) widestDim = 1;
		else if (diffZ >= Math.max(diffX, diffY)) widestDim = 2;

		// ==== Step 4 ====
		// Sort surfaces according to the widest dimension.
		cmp.setIndex(widestDim);
		
		Collections.sort(surfL, cmp);
		

		// ==== Step 5 ====
		// Recursively create left and right children.
		//System.out.println("### debuging beforesort" + this.surfaces.toString());
		int count = 0;
		for (int i = start; i < end; i++) {
			this.surfaces[i] = surfL.get(count);
			count++;
		}
		//System.out.println("### debuging aftersort" + this.surfaces.toString());
		Vector3d avrP = new Vector3d(
				(node.maxBound.x + node.minBound.x) / 2, 
				(node.maxBound.y + node.minBound.y) / 2, 
				(node.maxBound.z + node.minBound.z) / 2);
		
//		int index = end - 1;
//		for (; index >= start; index--) {
//			if(this.surfaces[index].getAveragePosition().get(widestDim) >= avrP.get(widestDim)) break;
//		}
//		System.out.println("### debugging end - 1 ###" + this.surfaces[end-1].getAveragePosition());
//		System.out.println("### debugging end - 2 ###" + this.surfaces[end-2].getAveragePosition());
//		System.out.println("### debugging ###" + index);
        node.child[0] = this.createTree(start, (end+start) / 2);
        node.child[1] = this.createTree((end+start)/2, end);
        return node;
	}

}

/**
 * A subclass that compares the average position two surfaces by a given
 * axis. Use the setIndex(i) method to select which axis should be considered.
 * i=0 -> x-axis, i=1 -> y-axis, and i=2 -> z-axis.  
 *
 */
class MyComparator implements Comparator<Surface> {
	int index;
	public MyComparator() {  }

	public void setIndex(int index) {
		this.index = index;
	}

	public int compare(Surface o1, Surface o2) {
		double v1 = o1.getAveragePosition().get(index);
		double v2 = o2.getAveragePosition().get(index);
		if(v1 < v2) return 1;
		if(v1 > v2) return -1;
		return 0;
	}

}
