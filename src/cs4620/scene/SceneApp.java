package cs4620.scene;

import blister.FalseFirstScreen;
import blister.MainGame;
import blister.ScreenList;
import cs4620.common.Scene;
import cs4620.scene.form.ControlWindow;
import cs4621.FinalProject.scene.TestScreen;
import cs4621.Particles.ParticleScreen;

public class SceneApp extends MainGame {
	/**
	 * The Thread That Runs The Other Window
	 */
	Thread tWindow = null;
	
	/**
	 * Window That Modifies The Underlying Data Of The Scene
	 */
	public ControlWindow otherWindow = null;
	
	/**
	 * Scene
	 */
	public Scene scene;
	
	public SceneApp() {
		super("CS 4620 Mesh Workspace", 800, 600);
		
		scene = new Scene();
		otherWindow = new ControlWindow(this);
	}
	
	@Override
	protected void buildScreenList() {
		screenList = new ScreenList(this, 0,
			//new FalseFirstScreen(1),
			new TestScreen()
			
			);
	}
	@Override
	protected void fullInitialize() {
		
	}
	@Override
	protected void fullLoad() {
        tWindow = new Thread(new ControlThread(this));
        tWindow.run();
	}
	
	@Override
	public void exit() {
		if(otherWindow != null) {
			tWindow.interrupt();
		}
		super.exit();
	}

	public static void main(String[] args) {
		SceneApp app = new SceneApp();
		app.run();
		app.dispose();
	}
	
	class ControlThread implements Runnable {
		SceneApp app;
		
		ControlThread(SceneApp a) {
			app = a;
		}
		
		@Override
		public void run() {
			app.otherWindow.run();
		}
		
	}
}
