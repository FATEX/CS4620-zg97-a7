package cs4620.ray2.shader;

import java.util.List;

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
public class Glass extends Shader {

	/**
	 * The index of refraction of this material. Used when calculating Snell's Law.
	 */
	protected double refractiveIndex;
	public void setRefractiveIndex(double refractiveIndex) { this.refractiveIndex = refractiveIndex; }


	public Glass() { 
		refractiveIndex = 1.0;
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
		// TODO#A7: fill in this function.
        // 1) Determine whether the ray is coming from the inside of the surface or the outside.
        // 2) Determine whether total internal reflection occurs.
        // 3) Compute the reflected ray and refracted ray (if total internal reflection does not occur)
        //    using Snell's law and call RayTracer.shadeRay on them to shade them
		//outIntensity.set(new Color(0, 0, 0));
		
		        
            Vector3d loc = new Vector3d();
			loc.set(record.location);
						
			Vector3d inDir = new Vector3d();
			inDir.set(ray.origin);
			inDir.sub(loc).normalize(); // shade point to light
			
			
			Vector3d normal = new Vector3d();
			normal.set(record.normal);
			normal.normalize();
			
			boolean fromOut = false;
			if(normal.clone().dot(inDir) > 0) fromOut = true;
			boolean internal = false;
			if(fromOut) {
				double angle = inDir.clone().angle(normal);
				if(1 * Math.sin(angle) / this.refractiveIndex >= 1) internal = true;
			}else {
				double angle = inDir.clone().angle(normal.clone().negate());
				if(this.refractiveIndex * Math.sin(angle) / 1 >= 1) internal = true;
			}
			
			double F = record.surface.getShader().fresnel(normal, inDir, refractiveIndex);
			
			Vector3d reflect = new Vector3d();
		    reflect.set(normal.clone().mul(inDir.clone().dot(normal)).mul(2).sub(inDir));
		    reflect.normalize();
		    Colord outIntensityF = new Colord();
		    Colord outIntensity1F = new Colord();
		    Colord outIntensitytotal = new Colord();
			if(fromOut && !internal) {
       			Vector3d refract = new Vector3d();
				double costheta1 = normal.clone().normalize().dot(inDir.clone().normalize());
	            double costheta2 = 1 - (1 - costheta1 * costheta1) / (refractiveIndex * refractiveIndex);
	            costheta2 = Math.sqrt(costheta2);  
				Vector3d d = new Vector3d(); d.set(inDir.clone().negate().normalize());
				refract.set(d);
				refract.sub(normal.clone().mul(d.clone().dot(normal)));
				refract.mul(1 / refractiveIndex);
				refract.sub(normal.clone().mul(costheta2));
				
				Ray reflectRay = new Ray();				
				reflectRay.set(record.location, reflect);
				reflectRay.start = 0;                
				reflectRay.end = Double.MAX_VALUE;
				reflectRay.makeOffsetRay();
				Ray refractRay = new Ray();
				refractRay.set(record.location, refract);
				refractRay.start = 0;                
				refractRay.end = Double.MAX_VALUE;
				refractRay.makeOffsetRay();
				RayTracer.shadeRay(outIntensityF, scene, reflectRay, depth+1);
				RayTracer.shadeRay(outIntensity1F, scene, refractRay, depth+1);	
				outIntensity.add(outIntensityF.clone().mul(F));
				outIntensity.add(outIntensity1F.clone().mul(1-F));
			}
			if(fromOut && !internal) {
				Ray reflectRay = new Ray();
				reflectRay.set(record.location, reflect);
				reflectRay.start = 0;                
				reflectRay.end = Double.MAX_VALUE;
				reflectRay.makeOffsetRay();
				RayTracer.shadeRay(outIntensitytotal, scene, reflectRay, depth+1);
				outIntensity.add(outIntensitytotal);
			}
			
			if(!fromOut && !internal) {
				Vector3d refract = new Vector3d();
	
				double costheta1 = normal.clone().normalize().negate().dot(inDir.clone().normalize());
	            double costheta2 = 1 - (1 - costheta1 * costheta1) * (refractiveIndex * refractiveIndex);
	            costheta2 = Math.sqrt(costheta2);  
				Vector3d d = new Vector3d(); d.set(inDir.clone().negate().normalize());
				refract.set(d);
				refract.sub(normal.clone().mul(costheta1));
				refract.mul(refractiveIndex);
				refract.add(normal.clone().mul(costheta2));
				
				
				Ray reflectRay = new Ray();
				reflectRay.set(record.location, reflect);
				reflectRay.start = 0;                
				reflectRay.end = Double.MAX_VALUE;
				reflectRay.makeOffsetRay();
				Ray refractRay = new Ray();
				refractRay.set(record.location, refract);
				refractRay.start = 0;                
				refractRay.end = Double.MAX_VALUE;
				refractRay.makeOffsetRay();
				RayTracer.shadeRay(outIntensityF, scene, reflectRay, depth+1);
				RayTracer.shadeRay(outIntensity1F, scene, refractRay, depth+1);
				outIntensity.add(outIntensityF.clone().mul(F));
				outIntensity.add(outIntensity1F.clone().mul(1-F));
			}
			if(!fromOut && internal) {
				Ray reflectRay = new Ray();
				reflectRay.set(record.location, reflect);
				reflectRay.start = 0;                
				reflectRay.end = Double.MAX_VALUE;
				reflectRay.makeOffsetRay();
				RayTracer.shadeRay(outIntensitytotal, scene, reflectRay, depth+1);
				outIntensity.add(outIntensitytotal);
			}
						
		
        
	}
	

}