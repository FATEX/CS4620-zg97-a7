package cs4621.Particles;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.LinkedList;

import cs4620.mesh.MeshData;
import cs4620.ray1.camera.Camera;
import egl.GLTexture;
import egl.NativeMem;
import egl.GL.PixelInternalFormat;
import egl.GL.TextureTarget;
import egl.math.*;

/**
 * 
 * ParticleSystem.java
 * 
 * The ParticleSystem class manages a collection of Particle objects with similar appearance 
 * and behavior.
 * 
 * Refactored and added to CS 4621 repository. Originally written by Asher Dunn (ad488).
 * 
 * @author Eric Gao (emg222)
 * @date 2015-11-01
 */
public class ParticleSystem {
    /* Adjustable parameters. */
    public float gravity = 9.8f;
    public float drag    = 1.0f;
    public float wind    = 1.0f;
    
    /* Array of dead/waiting particles which can be spawned during `animate()`. */
    private LinkedList<Particle> mUnspawnedParticles = new LinkedList<Particle>();
    public LinkedList<Particle> mSpawnedParticles = new LinkedList<Particle>();
    
    public Vector3 mPosition;
    public Vector3 mDirection;
    
    private float mTimeSinceLastSpawn;
    
    private int totalNumParticles;
    public double totalTime;
    private int currentSpawn;
    
    /* Texture used for all particles */
    public GLTexture particleTexture;
    
    /* Particle system state */
    public boolean mPaused = false;
    public boolean mFinished = false;
    /* billboardTransform refers to the rotation you must apply to the quads that represent 
     * the particles so that they face the camera. This is updated through the 
     * billboard() method. An identity matrix means the camera is at the position (0, 0, z). */
    private Matrix4 billboardTransform = new Matrix4();
    
    /**
     * Creates a particle system with a certain maximum number of particles.
     * @param maxParticles The maximum number of particles which can exist at a single time. 
     *        Particles are created and destroyed in `animate()` depending on the behavior 
     *        of this particular system.
     */
    public ParticleSystem(int maxParticles) {
        totalNumParticles = maxParticles;
        
        // Load in the texture that will be used by all particles
        particleTexture = new GLTexture(TextureTarget.Texture2D, true);
        particleTexture.internalFormat = PixelInternalFormat.Rgba;
        try {
            particleTexture.setImage2D("data/textures/BlurPreMul.png", false);
        } catch (Exception e) {
            System.out.println("Could not load particle texture.\r\n" + e.getMessage());
        }
        
        // Create a single quad which will be shared by all particles in this system.
        FloatBuffer vertices  = NativeMem.createFloatBuffer(12);
        vertices.put(new float[]{-1.0f, -1.0f, 0.0f,
                                  1.0f, -1.0f, 0.0f,
                                  1.0f,  1.0f, 0.0f,
                                 -1.0f,  1.0f, 0.0f});
        
        FloatBuffer texcoords = NativeMem.createFloatBuffer(8);
        texcoords.put(new float[]{0.0f, 0.0f,  
                                  1.0f, 0.0f, 
                                  1.0f, 1.0f, 
                                  0.0f, 1.0f});
        
        IntBuffer quads = NativeMem.createIntBuffer(6);
        quads.put(new int[]{0, 1, 2, 0, 2, 3});
        System.out.println("num : " + maxParticles);
        // Create a random cloud of particles.
        for (int i = 0; i < maxParticles; ++i) {
        	
            MeshData data = new MeshData();
            data.indexCount = 6;
            data.vertexCount = 4;
            data.positions = vertices;
            data.indices = quads;
            data.uvs = texcoords;
            
            Particle particle = new Particle(data);
            
            // TODO:PPA3 Feel free to play with the color!
            particle.setColor((double)1/256, 
            					(double)255/256,
            					(double)7/256);

            
            particle.setScale(0.01f);
            mUnspawnedParticles.add(particle);
        }
    }

    /**
     * Create, destroy, and move particles.
     */
    public void animate(float dt, Vector3 camT, boolean isM) {
        // TODO#PPA3 SOLUTION START
        // Animate the particle system:
        // 1.) If the particle system is paused, return immediately.
    	//System.out.println("par pos : " + this.mPosition);
    	if (this.mPaused) return;
        // 2.) Update the time since last spawn, and if a sufficient amount of time has
        //     elapsed since the last particle has spawned, spawn another if you can.
        //     This spawned particle should have some random initial velocity upward in the +y 
        //     direction and its position should be -0.
    	float time = (float)0.00005;
    	float mass = (float) 1;
    	this.mTimeSinceLastSpawn += dt;
    	this.totalTime += dt;
    	boolean transparent = false;
    	if(this.mTimeSinceLastSpawn >= time && this.mUnspawnedParticles.size() > 0 && currentSpawn <= totalNumParticles) {
    		this.mTimeSinceLastSpawn = 0;
    		this.currentSpawn++;
    		Particle particle = this.mUnspawnedParticles.poll();
    		//(float)Math.random()*4 -2, (float)Math.random()*2 + 3, (float)Math.random()*4 - 2)
    		mDirection.normalize();
    		
    		Vector3 u = new Vector3();
    		u.set(mDirection.y + mDirection.z + 0, -mDirection.x + 0 -mDirection.z , 0-mDirection.x +mDirection.y);    		
    		u.normalize();
    		
    		Vector3 w = new Vector3();
    		w.set(u.clone().cross(mDirection));
    		w.normalize();
    		
    		Vector3 ver = new Vector3();
    		float alpha = (float) Math.random();
    		float bata  = 1 - alpha;
    		ver.set(u.clone().mul(alpha).add(w.clone().mul(bata)));
    		ver.normalize();
    				
    		Vector3 velocity = new Vector3();
    		
    		double canVsparticle = mDirection.clone().dot(camT);
    		if(canVsparticle < 0) transparent = true;
    		   		
    		if (totalTime < 5) {
    			ver.mul((float)Math.random()* 0.5f - 0.25f);
    			velocity.set((mDirection.clone().mul((float)Math.random()*0.5f + 1)));
    		} else if (totalTime >=5 && totalTime < 12) {
    			ver.mul((float)Math.random()*2 - 1);
    			velocity.set((mDirection.clone().mul((float)Math.random()*2 + 1)));
    		} else {
    			ver.mul((float)Math.random()*1 - 0.5f);
    			velocity.set((mDirection.clone().mul((float)Math.random()*0.5f + 1)));
    		}
    		//Vector3 velocity = new Vector3((float)Math.random()*2 + 2 + mDirection.x, (float)Math.random()*2 + 2 + mDirection.y, (float)Math.random()*2 + 2 + mDirection.z);
    		
    		velocity.add(ver);
    		particle.spawn(mass, mPosition, velocity);
    		//System.out.println("dir" + mPosition);
    		this.mSpawnedParticles.add(particle);
    	}
    	
        // 3.) Remove the particle from the linked list of unspawned particles and put it
        //     onto the linked list of spawned particles.
        // 4.) For each spawned particle:
        //          - Accumulate forces: gravity should move the particle in -y direction
        //                               wind should move the particle in the +x direction
        //                               particle should be slowed down by the drag force.
        //          - Animate each particle according to these new forces.
        //          - Check if the particle is too old. If it is, remove it from the 
        //            linked list of spawned particles and append it to the linked list of
        //            unspawned particles.
        
        Iterator<Particle> it = this.mSpawnedParticles.iterator();
        while(it.hasNext()){
        	Particle p = it.next();
        	p.resetForces();
        	//Vector3 force = new Vector3(this.wind, -this.gravity*mass, 0);	
        	//p.accumForce(force);
        	Vector3 force = new Vector3(this.mDirection.clone().negate().mul(this.gravity*mass));
        	Vector3 dragForce = p.getVelocity().clone().mul(-this.drag);
        	force.add(dragForce);
        	p.accumForce(force);
        	p.animate(dt);
        	
        	double r2 = p.getParticlePosition().clone().distSq(new Vector3(0, 0, 0));  
        			//p.getParticlePosition().x * p.getParticlePosition().x + p.getParticlePosition().y * p.getParticlePosition().y + p.getParticlePosition().z * p.getParticlePosition().z;
        	//System.out.println(r2);
        	if((r2 <= 1 || r2 > 10 || transparent) && !isM){
        		this.mUnspawnedParticles.add(p);
        		it.remove();
        	}
        }
        //System.out.println(mSpawnedParticles.size());
        if (mSpawnedParticles.size() == 0) {
        	this.totalTime = 0;
        	mFinished = true;
        }
        //ENDSOLUTION
    }
    
    /**
     * Points all particles in this system towards the camera.
     */
    public void billboard(Matrix4 view) {
        // TODO#PPA3 SOLUTION START
        // Set the billboardTransform so that if you multiply the particle's quad by this matrix
        // the particle is always facing the camera.
        // 1.) Obtain the inverse of the rotation of the camera.
        // 2.) Set billboardTransform.
    	
    	Matrix4 rotation = new Matrix4(view.clone().invert());
    	Vector3 trans = rotation.getTrans();
    	///trans.invert();
    	rotation.set(0, 3, rotation.get(0, 3)-trans.x);
    	rotation.set(1, 3, rotation.get(1, 3)-trans.y);
    	rotation.set(2, 3, rotation.get(2, 3)-trans.z);
    	//this.billboardTransform.set(rotation);
    	
        // SOLUTION END
    }
    
    public void reset() {
        int numSpawned = mSpawnedParticles.size();
        for(int i = 0; i < numSpawned; ++i) {
            mUnspawnedParticles.add(mSpawnedParticles.removeFirst());
        }
        this.currentSpawn = 0;
        mTimeSinceLastSpawn = 0;
        this.mFinished = false;
        // reset parameters
        gravity = 9.8f; drag = 1.0f; wind = 1.0f;
    }
    
    public void setPosition(Vector3 position) {
    	this.mPosition = position;
    }
    
    public void setInitDirection(Vector3 direction) {
    	this.mDirection = direction;
    }
    
    public Matrix4 getBillboardTransform() {
        return billboardTransform;
    }
}
