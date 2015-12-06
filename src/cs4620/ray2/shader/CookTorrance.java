package cs4620.ray2.shader;

import java.util.List;

import cs4620.ray2.IntersectionRecord;
import cs4620.ray2.Light;
import cs4620.ray2.Ray;
import cs4620.ray2.Scene;
import egl.math.Color;
import egl.math.Colord;
import egl.math.Vector3d;

public class CookTorrance extends Shader {

	/** The color of the diffuse reflection. */
	protected final Colord diffuseColor = new Colord(Color.White);
	public void setDiffuseColor(Colord diffuseColor) { this.diffuseColor.set(diffuseColor); }

	/** The color of the specular reflection. */
	protected final Colord specularColor = new Colord(Color.White);
	public void setSpecularColor(Colord specularColor) { this.specularColor.set(specularColor); }

	/** The roughness controlling the roughness of the surface. */
	protected double roughness = 1.0;
	public void setRoughness(double roughness) { this.roughness = roughness; }

	/**
	 * The index of refraction of this material. Used when calculating Snell's Law.
	 */
	protected double refractiveIndex;
	public void setRefractiveIndex(double refractiveIndex) { this.refractiveIndex = refractiveIndex; }
	
	public CookTorrance() { }

	/**
	 * @see Object#toString()
	 */
	public String toString() {    
		return "CookTorrance " + diffuseColor + " " + specularColor + " " + roughness + " end";
	}

	/**
	 * Evaluate the intensity for a given intersection using the CookTorrance shading model.
	 *
	 * @param outIntensity The color returned towards the source of the incoming ray.
	 * @param scene The scene in which the surface exists.
	 * @param ray The ray which intersected the surface.
	 * @param record The intersection record of where the ray intersected the surface.
	 * @param depth The recursion depth.
	 */
	@Override
	public void shade(Colord outIntensity, Scene scene, Ray ray, IntersectionRecord record, int depth) {
		// TODO#A7 Fill in this function.
		// 1) Loop through each light in the scene.
		// 2) If the intersection point is shadowed, skip the calculation for the light.
		//	  See Shader.java for a useful shadowing function.
		// 3) Compute the incoming direction by subtracting
		//    the intersection point from the light's position.
		// 4) Compute the color of the point using the CookTorrance shading model. Add this value
		//    to the output.
		outIntensity.set(new Color(0, 0, 0));
		List<Light> lights = scene.getLights();
		for (Light light : lights){
			Ray shadowRay = new Ray();
			Vector3d lDir = new Vector3d();
			lDir.set(light.getDirection(record.location));
			lDir.normalize();
			
			shadowRay.set(record.location, lDir);
			shadowRay.start = 0;                
			shadowRay.end = Double.MAX_VALUE;
			
            if (record.surface.getShader().isShadowed(scene, light, record, shadowRay)) continue;
            
            Vector3d loc = new Vector3d();
			loc.set(record.location);
			
			
			Vector3d inDir = new Vector3d();
			inDir.set(light.getDirection(record.location));
			inDir.normalize(); // shade point to light
			
			double r2 = light.getRSq(record.location);
			
			
			Vector3d v = new Vector3d();
			v.set(ray.origin.clone().sub(loc));
			v.normalize();
			
			Vector3d h = new Vector3d();
			h.set(inDir);
			h.add(v);
			h.div(Math.sqrt(h.dot(h)));
			
			Vector3d normal = new Vector3d();
			normal.set(record.normal);
			normal.normalize();
			
			Colord k_d = new Colord(this.diffuseColor); 
			Colord k_l = new Colord(light.intensity);
			Colord k_s = new Colord(this.specularColor);
			Colord outIn = new Colord(k_l.div(r2));
			outIn.mul(Math.max(normal.clone().dot(inDir), 0));
			
			if (normal.dot(inDir) > 0) {
				double F = record.surface.getShader().fresnel(normal, inDir, refractiveIndex);
				double D = FacetDistribution(normal, h);
				double G = getG(normal, v, h, inDir);
				double fr = F * D * G / (4 * Math.abs(normal.clone().dot(inDir)) * Math.abs(normal.clone().dot(v)));
				outIn.mul(k_s.clone().mul(fr).clone().add(k_d));
			} else {
				outIn.set(new Color(0, 0, 0));
			}
			outIntensity.add(outIn);
		}
        
    }
	
	private double FacetDistribution(Vector3d normal, Vector3d h) {
		double thetaH = normal.clone().angle(h.clone());
		double numerator = Math.exp(- (Math.tan(thetaH) / roughness) * (Math.tan(thetaH) / roughness));
		double denominator = Math.PI * roughness * roughness * Math.pow(Math.cos(thetaH), 4);
		return numerator / denominator;
	}
	
	private double getG(Vector3d normal, Vector3d v, Vector3d h, Vector3d l) {
		double term1 = 2 * normal.clone().dot(h) * normal.clone().dot(v) / (v.clone().dot(h));
		double term2 = 2 * normal.clone().dot(h) * normal.clone().dot(l) / (v.clone().dot(h));
		return Math.min(1, Math.min(term1, term2));
	}
}
