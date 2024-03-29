package cs4620.splines;
import java.util.ArrayList;

import egl.math.Matrix4;
import egl.math.Vector2;

public class BSpline extends SplineCurve{

	public BSpline(ArrayList<Vector2> controlPoints, boolean isClosed,
			float epsilon) throws IllegalArgumentException {
		super(controlPoints, isClosed, epsilon);
	}

	@Override
	public CubicBezier toBezier(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3,
			float eps) {
		//TODO A5 (Extra Credit)
		//SOLUTION

		
		return new CubicBezier(new Vector2(), new Vector2(), new Vector2(), new Vector2(), eps);
		//END SOLUTION
	}
	

		
	
}
