package cs4621.FinalProject.objects;

import cs4620.common.Scene;
import egl.math.Matrix4;

public class MeteorObject {
	private final Scene scene;
	private Matrix4 tInverse;
	
	public MeteorObject(Scene scene) {
		this.scene = scene;
		this.tInverse = scene.objects.get("start").transformation.clone().invert();
	}
	
	
}
