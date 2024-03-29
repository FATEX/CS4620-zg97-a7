package cs4621.FinalProject.scene;

import java.awt.FileDialog;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import blister.GameScreen;
import blister.GameTime;
import blister.ScreenState;
import blister.input.KeyboardEventDispatcher;
import blister.input.KeyboardKeyEventArgs;
import cs4620.anim.Animator;
import cs4620.anim.TimelineViewer;
import cs4620.common.Scene;
import cs4620.common.SceneObject;
import cs4620.common.event.SceneReloadEvent;
import cs4620.gl.CameraController;
import cs4620.gl.GridRenderer;
import cs4620.gl.RenderCamera;
import cs4620.gl.RenderController;
import cs4620.gl.RenderObject;
import cs4620.gl.Renderer;
import cs4620.gl.manip.ManipController;
import cs4620.mesh.MeshData;
import cs4620.scene.SceneApp;
import cs4620.scene.form.RPMaterialData;
import cs4620.scene.form.RPMeshData;
import cs4620.scene.form.RPTextureData;
import cs4620.scene.form.ScenePanel;
import cs4621.FinalProject.gl.CameraUpdater;
import cs4621.Particles.Particle;
import cs4621.Particles.ParticleSystem;
import egl.ArrayBind;
import egl.GL.BufferTarget;
import egl.GL.BufferUsageHint;
import egl.GL.GLType;
import egl.GL.PrimitiveType;
import egl.GL.TextureUnit;
import egl.GLBuffer;
import egl.GLError;
import egl.GLProgram;
import egl.GLUniform;
import egl.NativeMem;
import egl.RasterizerState;
import egl.Semantic;
import egl.ShaderInterface;
import egl.math.Colord;
import egl.math.Matrix4;
import egl.math.Vector2;
import egl.math.Vector3;
import ext.csharp.ACEventFunc;
import ext.java.Parser;

public class TestScreen extends GameScreen {
	Renderer renderer = new Renderer();
	int cameraIndex = 0;
	boolean pick;
	int prevCamScroll = 0;
	boolean wasPickPressedLast = false;
	boolean showGrid = false;
	boolean useTimelineMouseOver = true;
	
	SceneApp app;
	ScenePanel sceneTree;
	RPMeshData dataMesh;
	RPMaterialData dataMaterial;
	RPTextureData dataTexture;
	
	RenderController rController;
	CameraController camController;
	//CameraUpdater camUpdater;
	ManipController manipController;
	GridRenderer gridRenderer;
	boolean updateAnimation;
	Animator animator;
	TimelineViewer animTimeViewer = new TimelineViewer();
	
	SceneObject fireObj = new SceneObject();
	
	private String[] MeteorL = {"Meteor1", "Meteor2", "Meteor3", "Meteor4", "Meteor5"};
	private Vector3[] vL = new Vector3[5]; 
	
    @Override
    public int getNext() {
	        // Don't modify this method
	        return 0;
	    }
	@Override
	protected void setNext(int next) {
	}

	@Override
	public int getPrevious() {
		return 0;
		//return -1; // testScreen
	}
	@Override
	protected void setPrevious(int previous) {
	}

	@Override
	public void build() {
		app = (SceneApp)game;
		
		renderer = new Renderer();
		buildParticle();
	}
	@Override
	public void destroy(GameTime gameTime) {
		destroyParticle(gameTime);
	}

	/**
	 * Add Scene Data Hotkeys
	 */
	private final ACEventFunc<KeyboardKeyEventArgs> onKeyPress = new ACEventFunc<KeyboardKeyEventArgs>() {
		@Override
		public void receive(Object sender, KeyboardKeyEventArgs args) {
			RenderObject selected;
			switch (args.key) {
			case Keyboard.KEY_G:
				showGrid = !showGrid;
				break;
			case Keyboard.KEY_F3:
				FileDialog fd = new FileDialog(app.otherWindow);
				fd.setVisible(true);
				for(File f : fd.getFiles()) {
					String file = f.getAbsolutePath();
					if(file != null) {
						Parser p = new Parser();
						Object o = p.parse(file, Scene.class);
						if(o != null) {
							Scene old = app.scene;
							app.scene = (Scene)o;
							if(old != null) old.sendEvent(new SceneReloadEvent(file));
							return;
						}
					}
				}
				break;
			case Keyboard.KEY_F4:
				try {
					app.scene.saveData("data/scenes/Saved.xml");
				} catch (ParserConfigurationException | TransformerException e) {
					e.printStackTrace();
				}
				break;
			case Keyboard.KEY_LBRACKET:
				rController.animEngine.rewind(1);
				updateAnimation = true;
				break;
			case Keyboard.KEY_RBRACKET:
				rController.animEngine.advance(1);
				updateAnimation = true;
				break;
			case Keyboard.KEY_N:
				selected = manipController.getCurrentObject();
				if(selected != null) {
					rController.animEngine.addKeyframe(selected.sceneObject.getID().name);
					updateAnimation = true;
				}
				break;
			case Keyboard.KEY_M:
				selected = manipController.getCurrentObject();
				if(selected != null) {
					rController.animEngine.removeKeyframe(selected.sceneObject.getID().name);
					updateAnimation = true;
				}
				break;
			case Keyboard.KEY_BACKSLASH:
				animator.togglePlaying();
				break;
			case Keyboard.KEY_APOSTROPHE:
				useTimelineMouseOver = !useTimelineMouseOver;
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onEntry(GameTime gameTime) {
		cameraIndex = 0;
		
		if (app.scene.objects.get("FireSphere")  != null) {
			fireObj = app.scene.objects.get("FireSphere");
			fireObj.addScale(new Vector3(1f));
		}
		
		try {
			for (int i = 0; i < MeteorL.length; i++) {
				app.scene.objects.get(MeteorL[i]).transformation.set(Matrix4.createTranslation(0, 0, 10));
			}
		} catch (Exception e){
			
		}
		
		rController = new RenderController(app.scene, new Vector2(app.getWidth(), app.getHeight()));
		renderer.buildPasses(rController.env.root);
		camController = new CameraController(app.scene, rController.env, null);
		createCamController();
		manipController = new ManipController(rController.env, app.scene, app.otherWindow);
		gridRenderer = new GridRenderer();
		animator = new Animator();
		
		KeyboardEventDispatcher.OnKeyPressed.add(onKeyPress);
		manipController.hook();
		
		Object tab = app.otherWindow.tabs.get("Object");
		if(tab != null) sceneTree = (ScenePanel)tab;
		tab = app.otherWindow.tabs.get("Material");
		if(tab != null) dataMaterial = (RPMaterialData)tab;
		tab = app.otherWindow.tabs.get("Mesh");
		if(tab != null) dataMesh = (RPMeshData)tab;
		tab = app.otherWindow.tabs.get("Texture");
		if(tab != null) dataTexture = (RPTextureData)tab;
		
		wasPickPressedLast = false;
		updateAnimation = false;
		animTimeViewer.init();
		prevCamScroll = 0;
		
		onEntryParticle(gameTime);
	}
	@Override
	public void onExit(GameTime gameTime) {
		KeyboardEventDispatcher.OnKeyPressed.remove(onKeyPress);
		rController.dispose();
		animTimeViewer.dispose();
		manipController.dispose();
	}

	private void createCamController() {
		if(rController.env.cameras.size() > 0) {
			RenderCamera cam = rController.env.cameras.get(cameraIndex);
			camController.camera = cam;
		}
		else {
			camController.camera = null;
		}
	}
	
	@Override
	public void update(GameTime gameTime) {
		pick = false;
		int curCamScroll = 0;
		try {
			//System.out.println(app.scene.objects.get("Meteor").transformation.getTrans());
			for (int i = 0; i < MeteorL.length; i++) {
				if (app.scene.objects.get(MeteorL[i]).transformation.getTrans().equals(new Vector3( 0, 0, 10)) ) {	
					//add rotation
					app.scene.objects.get(MeteorL[i]).transformation.set(Matrix4.createTranslation(8, 8, 8));
					app.scene.objects.get(MeteorL[i]).addScale(new Vector3(0.002f));
					app.scene.objects.get(MeteorL[i]).addTranslation(
							new Vector3(1 + (float)Math.random(), 1 + (float) Math.random(), 1 + (float)Math.random()));
					System.out.println("change" + app.scene.objects.get(MeteorL[i]).transformation);
				}
				
				
				vL[i] = new Vector3(-0.000f, -0.000f, 0.001f);
				app.scene.objects.get(MeteorL[i]).addTranslation(vL[i].clone());
				
				Matrix4 trans = new Matrix4();
				trans.set(app.scene.objects.get(MeteorL[i]).transformation.clone());
				Matrix4 rockTransInverse = trans.clone().invert();
				
				Matrix4 rockRotationOrbit = Matrix4.createRotationY(0.01f);
				Matrix4 rockRotationX = Matrix4.createRotationX(0.01f);
				rockRotationOrbit.mulBefore(rockRotationX);
				Matrix4 rockTransform = new Matrix4();
				rockTransform.set(rockTransInverse);
				Matrix4 rockRotationFy = Matrix4.createRotationY(-0.001f);
				Matrix4 rockRotationFx = Matrix4.createRotationX(0.001f);
				rockRotationFy.mulBefore(rockRotationFx);
				rockTransform.mulAfter(rockRotationOrbit).mulAfter(trans).mulAfter(rockRotationFy);
				app.scene.objects.get(MeteorL[i]).transformation.mulAfter(rockTransform);
				
				Matrix4 marsRotation = Matrix4.createRotationY(0.001f);
				app.scene.objects.get("Mars").transformation.mulAfter(marsRotation);
				
				Matrix4 shipTrans = new Matrix4();
				shipTrans.set(app.scene.objects.get("Satellite").transformation.clone());
				Matrix4 shipTransInverse = shipTrans.clone().invert();								
				Matrix4 shipTransform = new Matrix4();
				shipTransform.set(shipTransInverse);
				Matrix4 shiprotationOrbit = Matrix4.createRotationY(0.001f);
				Matrix4 shipRotationFy = Matrix4.createRotationY(0.001f);
				Matrix4 shipRotationFx = Matrix4.createRotationX(0.001f);
				shipRotationFy.mulBefore(shipRotationFx);
				shipTransform.mulAfter(shiprotationOrbit).mulAfter(shipTrans).mulAfter(shipRotationFy);
				app.scene.objects.get("Satellite").transformation.mulAfter(shipTransform); 
				
			}
		} catch (Exception e){
			
		}
		
			
		if(Keyboard.isKeyDown(Keyboard.KEY_EQUALS)) curCamScroll++;
		if(Keyboard.isKeyDown(Keyboard.KEY_MINUS)) curCamScroll--;
		if(rController.env.cameras.size() != 0 && curCamScroll != 0 && prevCamScroll != curCamScroll) {
			if(curCamScroll < 0) curCamScroll = rController.env.cameras.size() - 1;
			cameraIndex += curCamScroll;
			cameraIndex %= rController.env.cameras.size();
			createCamController();
		}
		prevCamScroll = curCamScroll;
		
		if(camController.camera != null) {
			camController.update(gameTime.elapsed);
			manipController.checkMouse(Mouse.getX(), Mouse.getY(), camController.camera);
		}
		
		if(Mouse.isButtonDown(1) || Mouse.isButtonDown(0) && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
			if(!wasPickPressedLast) pick = true;
			wasPickPressedLast = true;
		}
		else wasPickPressedLast = false;
		
		// View A Different Scene
		if(rController.isNewSceneRequested()) {
			setState(ScreenState.ChangeNext);
		}
		
		// Update Animation
		int frames = animator.update((float)gameTime.elapsed);
		if(frames > 0) {
			rController.animEngine.advance(frames);
			updateAnimation = true;
		}
		if(updateAnimation) {
			rController.animEngine.updateTransformations();
			updateAnimation = false;
		}
		
		if (app.scene.objects.get("FireSphere") != null) updateParticle(gameTime);
        
	}
	
	@Override
	public void draw(GameTime gameTime) {

		
		rController.update(renderer, camController);

		if(pick && camController.camera != null) {
			manipController.checkPicking(renderer, camController.camera, Mouse.getX(), Mouse.getY());
		}
		
		Vector3 bg = app.scene.background;
		GL11.glClearColor(bg.x, bg.y, bg.z, 0);
		GL11.glClearDepth(1.0);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		
		// draw particle
		if (gameTime.elapsed > 0.0f) {
            mFps = mFps*(1-FPS_ALPHA) + (1/(float)gameTime.elapsed)*FPS_ALPHA;
            mFps /= 2;
            //System.out.printf("%.2f Frames per Second\n", mFps);
        }
        
        //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Enable blending
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        //GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.6f);
		
		if(camController.camera != null){
			renderer.draw(camController.camera, rController.env.lights, (float) gameTime.total);
			manipController.draw(camController.camera);
			if (showGrid)
				gridRenderer.draw(camController.camera);
		}
		
		RenderObject co = manipController.getCurrentObject();
		animTimeViewer.draw(
				game.getWidth(), game.getHeight(),
				rController.animEngine,
				co == null ? "" : co.sceneObject.getID().name,
				Mouse.getY() < 40 || !useTimelineMouseOver, (float)gameTime.elapsed);
		
        GLError.get("draw");
        
        if (app.scene.objects.get("FireSphere") != null) drawParticle(gameTime);
	}
	
	
	
	
	private final int MAX_PARTICLES = 1024;
    private final int PARTICLE_SYSTEM_NUM = 16;
    /* For calculating FPS */
    private final float FPS_ALPHA = 0.5f;
    private float mFps = 0;
    private int index = 0;
    
    /* Vertex shader inputs */
    private GLBuffer rasterVerts;
    private GLBuffer ibTris;
    private GLBuffer wireframeVerts;
    private GLBuffer velocityVerts;
    private GLBuffer ibWireframe;
    private GLBuffer ibVelocities;
    
    /* Mesh state */
    private FloatBuffer vBuf;
    private FloatBuffer vBufWireframe;
    private FloatBuffer vBufVelocities;
    
    /* Shader state/uniforms with state */
    private GLProgram program;
    private GLProgram linesProgram;
    private ShaderInterface particlesSI;
    private ShaderInterface linesSI;
    private boolean showWireFrames = false;
    private boolean showVelocities = false;
    
    /* Camera information */
    private Matrix4 mViewProjection  = new Matrix4();
    private Matrix4 mView            = new Matrix4();
    private float   mCameraRadius    = 5.0f;
    private float   mCameraLongitude = 0.0f;
    private float   mCameraLatitude  = 0.0f;
    private Vector3 mCameraPosition  = new Vector3(0, 0, mCameraRadius);
    private Vector3 mCameraUp        = new Vector3(0, 1, 0);
    private Vector2 mCameraViewSize  = new Vector2(800, 800);
    private Vector2 mZPlanes         = new Vector2(0.01f, 100.0f);
    private float   mCameraFOV       = 10.0f;
    
    /* Lighting information */
    private Vector3 mLightPos = new Vector3(0, 5, 0);
    private Vector3 mLightIntensity = new Vector3(5, 5, 5);
    
    /* Particle System information*/
    private ParticleSystem mParticleSystem = new ParticleSystem(MAX_PARTICLES);
    
    private LinkedList<ParticleSystem> mUsingParticleSys = new LinkedList<ParticleSystem>();
    private LinkedList<ParticleSystem> mUnusedParticleSys = new LinkedList<ParticleSystem>();
    
    private LinkedList<ParticleSystem> mMeteorParticleSys = new LinkedList<ParticleSystem>();
    
    /* Mouse information */
    private boolean mousePressed = false;

    private final int RADIUS = 1;
    private final int METEOR_PARTICLE_SYS = 5;
    private final int MAX_METEOR_PARTICLE = 200;
    
    
    ParticleSystem ms1;
    ParticleSystem ms2;
    ParticleSystem ms3;
    ParticleSystem ms4;
    ParticleSystem ms5;
 
    
    public void buildParticle() {
        app = (SceneApp)game;
		
		renderer = new Renderer();
    	for (int i = 0; i < PARTICLE_SYSTEM_NUM; i++) this.mUnusedParticleSys.add(new ParticleSystem(MAX_PARTICLES));
    	
    	for (int j = 0; j < METEOR_PARTICLE_SYS; j++) this.mMeteorParticleSys.add(new ParticleSystem(MAX_METEOR_PARTICLE));
    	/*ms1 = new ParticleSystem(200);
    	ms2 = new ParticleSystem(200);
    	ms3 = new ParticleSystem(200);
    	ms4 = new ParticleSystem(200);
    	ms5 = new ParticleSystem(200);*/
    	// First create the programs: one for the particles, one for the wireframes.
        program = new GLProgram(false);
        program.quickCreateResource("particles", "cs4621/Particles/shaders/particles.vert", "cs4621/Particles/shaders/particles.frag", null);
        
        linesProgram = new GLProgram(false);
        linesProgram.quickCreateResource("lines", "cs4621/Particles/shaders/lines.vert", "cs4621/Particles/shaders/lines.frag", null);
        
        // Initialize camera matrix.
        updateCamera();
        
        // Initialize shader interfaces.
        particlesSI = new ShaderInterface(new ArrayBind[]{
                                          new ArrayBind(Semantic.Position, GLType.Float, 3, 0),
                                          new ArrayBind(Semantic.Color, GLType.Float, 3, 3 * 4    /* 3 floats x 4 bytes offset */),
                                          new ArrayBind(Semantic.TexCoord, GLType.Float, 2, 6 * 4 /* 6 floats x 4 bytes offset */)
        });
        particlesSI.build(program.semanticLinks);
        
        linesSI = new ShaderInterface(new ArrayBind[]{new ArrayBind(Semantic.Position, GLType.Float, 3, 0)});
        linesSI.build(linesProgram.semanticLinks);
        
        // Initialize the vertex buffers
        rasterVerts = new GLBuffer(BufferTarget.ArrayBuffer, BufferUsageHint.StaticDraw, true);
        rasterVerts.setAsVertex(8 * 4); // 1 vertex element = (3 floats for position + 2 for uv + 3 for color) * 4 bytes
        vBuf = NativeMem.createFloatBuffer(8 * 4); // 4 vertices * (3 floats for position + 2 for uv + 3 for color) elements 
        
        wireframeVerts = new GLBuffer(BufferTarget.ArrayBuffer, BufferUsageHint.StaticDraw, true);
        wireframeVerts.setAsVertex(4 * 3); // 1 vertex element = 3 floats for position * 4 bytes
        vBufWireframe = NativeMem.createFloatBuffer(3 * 4); // 4 vertices * 3 floats for position
        
        velocityVerts = new GLBuffer(BufferTarget.ArrayBuffer, BufferUsageHint.StaticDraw, true);
        velocityVerts.setAsVertex(4 * 3); // 1 vertex element = 3 floats for position * 4 bytes
        vBufVelocities = NativeMem.createFloatBuffer(3 * 2); // 2 vertices * 3 floats for position
        
        // Initialize index buffers.
        ibTris       = new GLBuffer(BufferTarget.ElementArrayBuffer, BufferUsageHint.StaticDraw, true);
        ibWireframe  = new GLBuffer(BufferTarget.ElementArrayBuffer, BufferUsageHint.StaticDraw, true);
        ibVelocities = new GLBuffer(BufferTarget.ElementArrayBuffer, BufferUsageHint.StaticDraw, true);
        
        ibTris.setAsIndexInt();
        IntBuffer iBuf = NativeMem.createIntBuffer(6);
        iBuf.put(new int[]{0, 1, 2, 0, 2, 3});
        iBuf.rewind();
        ibTris.setDataInitial(iBuf);
        
        ibWireframe.setAsIndexInt();
        iBuf = NativeMem.createIntBuffer(8);
        iBuf.put(new int[]{0, 1, 1, 2, 2, 3, 3, 0});
        iBuf.rewind();
        ibWireframe.setDataInitial(iBuf);
        
        ibVelocities.setAsIndexInt();
        iBuf = NativeMem.createIntBuffer(2);
        iBuf.put(new int[]{0, 1});
        iBuf.rewind();
        ibVelocities.setDataInitial(iBuf);
    }
    
    public void destroyParticle(GameTime gameTime) {
        program.dispose();
        linesProgram.dispose();
        rasterVerts.dispose();
        ibTris.dispose();
        ibWireframe.dispose();
    }
    
   
    public void onEntryParticle(GameTime gameTime) {
        RasterizerState.CULL_NONE.set();
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClearDepth(1.0);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Updates the camera position, camera up vector and the view projection matrix 
     * based on the camera's latitude, longitude, and radius from the lookAt point.
     */
    public void updateCamera() {
    	/*if(rController != null && rController.env.cameras.size() > 0) {
            RenderCamera cam = rController.env.cameras.get(cameraIndex);
            GLUniform.setST(program.getUniform("mModelViewProjection"), cam.mViewProjection, false);
            mParticleSystem.billboard(cam.mView);
    	}*/
        
    }
    
    public void updateParticle(GameTime gameTime) {
        //if(mParticleSystem.mPaused) return;
        
        createParticleSystems(gameTime);
        //mParticleSystem.animate((float) gameTime.elapsed);
    }
    
    public void createParticleSystems(GameTime gameTime) {
    	if (index >= 16) index = 0;
    	
    	while (mUnusedParticleSys.size() > 0) {
    		ParticleSystem sys = this.mUnusedParticleSys.poll();
        	if (fireObj != null) {
        		Vector3 pos = createParticlePos(index);
        		sys.setPosition(pos.clone());
        		sys.setInitDirection(pos.clone());
        		sys.setRefPos(new Vector3(0));
        		index++;
        	} else {
        		sys.setPosition(new Vector3((float) (-1), (float) -0.5, 0));
        		sys.setInitDirection(new Vector3(0f, 1f, 0f));
        	}	
        	this.mUsingParticleSys.add(sys);
    	}
    	
    	try{
    		for (int i = 0; i < METEOR_PARTICLE_SYS; i++) {
    			//if (mMeteorParticleSys.get(i).inited) continue;
    			mMeteorParticleSys.get(i).setPosition(app.scene.objects.get(MeteorL[i]).transformation.getTrans().clone());
    			mMeteorParticleSys.get(i).setInitDirection(vL[i].clone().normalize().negate());
            	
            	if(rController.env.cameras.size() > 0) {
                    RenderCamera cam = rController.env.cameras.get(cameraIndex);
                    mMeteorParticleSys.get(i).animate((float) gameTime.elapsed, cam.mWorldTransform.getTrans(), true);
                    mMeteorParticleSys.get(i).setRefPos(app.scene.objects.get(MeteorL[i]).transformation.getTrans().clone());
            	}
    		}   	
    	} catch (Exception e) {
    		
    	}
    	
    	
    	//index++;
    	
        for (int i = 0; i < mUsingParticleSys.size(); i++) {
        	if(rController.env.cameras.size() > 0) {
                RenderCamera cam = rController.env.cameras.get(cameraIndex);
                mUsingParticleSys.get(i).animate((float) gameTime.elapsed, cam.mWorldTransform.getTrans(), false);
        	}
        	
        }
        
        Iterator<ParticleSystem> it = this.mUsingParticleSys.iterator();
        
        while(it.hasNext()){
        	ParticleSystem p = it.next();
        	if(p.mFinished){
        		p.reset();
        		p.mFinished = false;
        		this.mUnusedParticleSys.add(p);
        		it.remove();
        	}
        }
        
        
    }
    
    private Vector3 createParticlePos(int index) {
    	Vector3 pos = new Vector3();
    	pos.set(fireObj.transformation.getTrans());
    	double theta = 0.0;
    	double phi = 0.0;
    	if (index < 2) {
    		theta = Math.random() * Math.PI / 2;
    		phi = Math.random() * Math.PI / 2;
    		//theta = Math.random() * Math.PI / 2 + Math.PI * 3 / 2;
    		//phi = Math.random() * Math.PI / 2 + Math.PI / 2;
    	} else if (index < 4) {
    		// 0 - pi / 2
    		// -pi / 2 - 0
    		theta = Math.random() * Math.PI / 2;
    		phi = Math.random() * Math.PI / 2 + Math.PI / 2;
    	} else if (index < 6) {
    		theta = Math.random() * Math.PI / 2 + Math.PI / 2;
    		phi = Math.random() * Math.PI / 2 + Math.PI;
    	} else if (index < 8) {
    		theta = Math.random() * Math.PI / 2 + Math.PI / 2;
    		phi = Math.random() * Math.PI / 2 + Math.PI * 3 / 2;
    	} else if (index < 10) {
    		theta = Math.random() * Math.PI / 2 + Math.PI;
    		phi = Math.random() * Math.PI / 2 + Math.PI;
    	} else if (index < 12) {
    		theta = Math.random() * Math.PI / 2 + Math.PI;
    		phi = Math.random() * Math.PI / 2 + Math.PI * 3 / 2;
    	} else if (index < 14) {
    		theta = Math.random() * Math.PI / 2 + Math.PI * 3 / 2;
    		phi = Math.random() * Math.PI / 2;
    	} else if (index < 16) {
    		theta = Math.random() * Math.PI / 2 + Math.PI * 3 / 2;
    		phi = Math.random() * Math.PI / 2 + Math.PI / 2;
    	}
    	
    	double unitD = (double) 360 / 16;
    	double unitT = (double) 60 / 8;
    	phi = unitD * index * Math.PI / 180;
    	theta = (double) unitT * index * Math.PI / 180 + Math.PI/3; 
    	if(theta > 2 * Math.PI / 3) theta = 4 * Math.PI / 3 - theta;
    	
    	Vector3 offset = new Vector3(
				(float) (2 * RADIUS * Math.abs(Math.sin(theta)) * Math.cos(phi)), 
				(float) Math.cos(theta) * 2  * RADIUS, 
				(float) ( 2 * RADIUS * Math.abs(Math.sin(theta)) * Math.sin(phi))
				);
		pos.add(offset);
		//System.out.println("pos" + pos);
		/*.out.println("%%%%%");
		System.out.println(index);
		System.out.println(theta);
		System.out.println(phi);
		System.out.println(pos);*/
    	return pos;
    }
    
    public void drawParticle(GameTime gameTime) {
    	for (int i = 0; i < mUsingParticleSys.size(); i++) {
        	drawPatricleSystem(mUsingParticleSys.get(i));
        }
    	
    	for (int i = 0; i < METEOR_PARTICLE_SYS; i++) {
    		drawPatricleSystem(this.mMeteorParticleSys.get(i));
    	}   
    }
    
   
    private void drawPatricleSystem(ParticleSystem mParticleSystem) {
    	for(Particle p : mParticleSystem.mSpawnedParticles) {
            vBuf.rewind(); vBufWireframe.rewind();
            // Get the positions of the quad for this particle.
            Vector3 particlePosition = p.getParticlePosition();
            MeshData md = p.getMeshData();
            Colord color = p.getColor();
            float scale = p.getScale();
            
            for(int i = 0; i < md.vertexCount; ++i) {
                Vector3 quadPosition = new Vector3((float) p.getPosition(i).x, 
                                                   (float) p.getPosition(i).y, 
                                                   (float) p.getPosition(i).z);
                mParticleSystem.getBillboardTransform().mulPos(quadPosition);
                quadPosition.mul(scale).add(particlePosition);
                
                vBuf.put((float) quadPosition.x);
                vBuf.put((float) quadPosition.y);
                vBuf.put((float) quadPosition.z);
                vBuf.put((float) color.x);
                vBuf.put((float) color.y);
                vBuf.put((float) color.z);
                vBuf.put((float) p.getUV(i).x);
                vBuf.put((float) p.getUV(i).y);
                
                vBufWireframe.put((float) quadPosition.x);
                vBufWireframe.put((float) quadPosition.y);
                vBufWireframe.put((float) quadPosition.z);
                
                
            }
            vBuf.rewind();
            vBufWireframe.rewind();
            vBufVelocities.rewind();
            
            rasterVerts.setDataInitial(vBuf);
            wireframeVerts.setDataInitial(vBufWireframe);
            velocityVerts.setDataInitial(vBufVelocities);
            
            program.use();
            {
            	if(rController.env.cameras.size() > 0) {
                    RenderCamera cam = rController.env.cameras.get(cameraIndex);
                    GLUniform.setST(program.getUniform("mModelViewProjection"), cam.mViewProjection, false);
            	}
                // Pass over view projection matrix.
            	else GLUniform.setST(program.getUniform("mModelViewProjection"), mViewProjection, false);
                
                // Bind the texture.
                mParticleSystem.particleTexture.use(TextureUnit.Texture0, program.getUniform("particleTexture"));
                
                // Bind the attributes.
                rasterVerts.useAsAttrib(particlesSI);
                ibTris.bind();
                GL11.glDrawElements(PrimitiveType.Triangles, 6, GLType.UnsignedInt, 0);
                ibTris.unbind();
                
            }
            GLProgram.unuse();
            
            if(showWireFrames) {
                linesProgram.use();
                {
                    // Pass over view projection matrix.
                    GLUniform.setST(linesProgram.getUniform("mModelViewProjection"), mViewProjection, false);

                    wireframeVerts.useAsAttrib(linesSI);
                    
                    ibWireframe.bind();
                    GL11.glDrawElements(PrimitiveType.Lines, 8, GLType.UnsignedInt, 0);
                    ibWireframe.unbind();
                }
                GLProgram.unuse();
            }
            
            if(showVelocities) {
                // TODO#PPA3 SOLUTION START:
                // Populate the vertex buffer to display velocities for each particle.
                // 1.) Disable depth testing so that you can see the lines no matter what. 
                //     Don't forget to re-enable this at the end!
                // 2.) Put the particle position and the particle position + (velocity scaled
                //     by some constant amount) into the float buffer for velocities (vBufVelocities).
                //     Don't forget to rewind at the appropriate locations!
                // 3.) Set the appropriate GLBuffer (velocityVerts) using vBufVelocities
                // 4.) Use the program (linesProgram and bind the attributes, uniforms and the  
                //     appropriate index buffer (ibVelocities).
                // 5.) Bind the GLBuffer to the appropriate shader interface (linesSI) and draw
                //     the velocity lines.
            	GL11.glDisable(GL11.GL_DEPTH_TEST);
            	vBufVelocities.put(p.getParticlePosition().x);
            	vBufVelocities.put(p.getParticlePosition().y);
            	vBufVelocities.put(p.getParticlePosition().z);
            	Vector3 v = p.getParticlePosition().clone().add(p.getVelocity().clone().mul((float)0.05));
            	vBufVelocities.put(v.x);
            	vBufVelocities.put(v.y);
            	vBufVelocities.put(v.z);
            	vBufVelocities.rewind();
                velocityVerts.setDataInitial(vBufVelocities);
                linesProgram.use();
                {
                    // Pass over view projection matrix.
                    GLUniform.setST(linesProgram.getUniform("mModelViewProjection"), mViewProjection, false);

                    velocityVerts.useAsAttrib(linesSI);
                    
                    ibVelocities.bind();
                    GL11.glDrawElements(PrimitiveType.Lines, 8, GLType.UnsignedInt, 0);
                    ibVelocities.unbind();
                }
                GLProgram.unuse();
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                // SOLUTION END
            }
        }
    }
   
}

	
	
	
