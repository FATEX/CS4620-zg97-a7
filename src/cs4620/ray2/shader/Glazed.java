package cs4620.ray2.shader;

import java.util.List;

import cs4620.ray2.shader.Shader;
import cs4620.ray2.Light;
import cs4620.ray2.RayTracer;
import cs4620.ray2.IntersectionRecord;
import cs4620.ray2.Ray;
import cs4620.ray2.Scene;
import egl.math.Color;
import egl.math.Colord;
import egl.math.Vector3d;

/**
 * A Phong material.
 *
 * @author ags, pramook
 */
public class Glazed extends Shader {

	/**
	 * The index of refraction of this material. Used when calculating Snell's Law.
	 */
	protected double refractiveIndex;
	public void setRefractiveIndex(double refractiveIndex) { this.refractiveIndex = refractiveIndex; }

	/**
	 * The underlying material beneath the glaze.
	 */
	protected Shader substrate;
	public void setSubstrate(Shader substrate) {
		this.substrate = substrate; 
	}
	
	public Glazed() { 
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {    
		return "glass " + refractiveIndex + " end";
	}

	/**
	 * Evaluate the intensity for a given intersection using the Glass shading model.
	 *
	 * @param outIntensity The color returned towards the source of the incoming ray.
	 * @param scene The scene in which the surface exists.
	 * @param ray The ray which intersected the surface.
	 * @param record The intersection record of where the ray intersected the surface.
	 * @param depth The recursion depth.
	 */
	@Override
	public void shade(Colord outIntensity, Scene scene, Ray ray, IntersectionRecord record, int depth) {
		// TODO#A7 EXTRA CREDIT: fill in this function.
        //  1) Compute the Fresnel term R
        //  2) Shade the substrate and multiply the result color by 1 - R
        //  3) Compute the reflected ray and call RayTracer.shadeRay on it, multiply result color by R
		Vector3d loc = new Vector3d();
		loc.set(record.location);
					
		Vector3d inDir = new Vector3d();
		inDir.set(ray.origin);
		inDir.sub(loc).normalize(); // shade point to light
				
		Vector3d normal = new Vector3d();
		normal.set(record.normal);
		normal.normalize();
		if(normal.clone().dot(inDir) < 0) normal.negate();
		double F = record.surface.getShader().fresnel(normal, inDir, refractiveIndex);
		
		Colord outIntensitysubstrate = new Colord();
	    Colord outIntensityreflect = new Colord();
	    
	    Vector3d reflect = new Vector3d();
	    reflect.set(normal.clone().mul(inDir.clone().dot(normal)).mul(2).sub(inDir));
	    reflect.normalize();
	    Ray reflectRay = new Ray();				
		reflectRay.set(record.location, reflect);
		reflectRay.start = 0;                
		reflectRay.end = Double.MAX_VALUE;
		reflectRay.makeOffsetRay();
		
		substrate.shade(outIntensitysubstrate, scene, ray, record, depth + 1);
		RayTracer.shadeRay(outIntensityreflect, scene, reflectRay, depth + 1);	
		outIntensity.add(outIntensitysubstrate.clone());
		outIntensity.add(outIntensityreflect.clone().mul(F));
	}
}
