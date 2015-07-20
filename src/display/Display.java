package display;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWvidmode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import input.InputHandler;
import objects.Light;
import objects.Model;
import utils.loaders.OBJLoader;
import utils.math.Matrix4f;
import utils.math.Vector3f;
 
public class Display {
	
    private long window;
    
    private int vaoID, vboVertID, vboNormID, vboIndID, vsID, fsID, pID, texID,
    	pMatLoc, vMatLoc, mMatLoc, lPosLoc, lColLoc, shineDamperLoc, reflectivityLoc;
    
    private Matrix4f pMat, vMat, mMat;
    
    public static Vector3f mScale, mPos, mAng, camPos, camAng;
    
    private FloatBuffer matBuff;
    
    private GLFWKeyCallback   keyCallback;
    
    private final int WIDTH = 800, HEIGHT = 600;
    private final String TITLE = "Nyuszi";
 
    public void run() {
        try {
            init();
            loop();
 
            glfwDestroyWindow(window);
            keyCallback.release();
        }
        finally {
        	/*GL13.glActiveTexture(GL13.GL_TEXTURE0);
        	GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        	GL11.glDeleteTextures(texID);
        	GL13.glActiveTexture(0);*/
        	
        	GL20.glUseProgram(0);
        	GL20.glDetachShader(pID, vsID);
        	GL20.glDetachShader(pID, fsID);
        	 
        	GL20.glDeleteShader(vsID);
        	GL20.glDeleteShader(fsID);
        	GL20.glDeleteProgram(pID);
        	
        	GL30.glBindVertexArray(vaoID);
        	
        	GL20.glDisableVertexAttribArray(0);
        	GL20.glDisableVertexAttribArray(1);
        	
        	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        	GL15.glDeleteBuffers(vboVertID);
        	GL15.glDeleteBuffers(vboNormID);
        	
        	GL30.glBindVertexArray(0);
        	GL30.glDeleteVertexArrays(vaoID);
        	
        	GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        	GL15.glDeleteBuffers(vboIndID);
        	
            glfwTerminate();
        }
    }
 
    private void init() {
        if ( glfwInit() != GL11.GL_TRUE )
            throw new IllegalStateException("Unable to initialize GLFW");
 
        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");
        
        glfwSetKeyCallback(window, keyCallback = new InputHandler());

        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        
        glfwSetWindowPos(
            window,
            (GLFWvidmode.width(vidmode) - WIDTH) / 2,
            (GLFWvidmode.height(vidmode) - HEIGHT) / 2
        );
        
        glfwMakeContextCurrent(window);
        GLContext.createFromCurrent();
        
        glfwSwapInterval(1);
        
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        glEnable(GL_DEPTH_TEST);
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    }
 
    private void loop() {
    	
    	Model m = new Model();
    	m = OBJLoader.loadModel(new File("res/models/bunny2.obj"));
    	
    	FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(m.vertices.size() * 3);
    	for (int i = 0; i < m.vertices.size(); i ++) {
    		vertexBuffer.put(m.vertices.get(i).x);
    		vertexBuffer.put(m.vertices.get(i).y);
    		vertexBuffer.put(m.vertices.get(i).z);
    	} vertexBuffer.flip();
    	
    	FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(m.normals.size() * 3);
    	for (int i = 0; i < m.normals.size(); i ++) {
    		normalBuffer.put(m.normals.get(i).x);
    		normalBuffer.put(m.normals.get(i).y);
    		normalBuffer.put(m.normals.get(i).z);
    	} normalBuffer.flip();
    	
    	ShortBuffer indexBuffer = BufferUtils.createShortBuffer(m.faces.size() * 3);
    	for (int i = 0; i < m.faces.size(); i ++) {
    		indexBuffer.put((short) (m.faces.get(i).vertexIndices.x - 1));
    		indexBuffer.put((short) (m.faces.get(i).vertexIndices.y - 1));
    		indexBuffer.put((short) (m.faces.get(i).vertexIndices.z - 1));
    	} indexBuffer.flip();
    	
        vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);
        
        vboVertID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboVertID);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        //GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 20, 12);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        
        vboNormID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboNormID);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        
        GL30.glBindVertexArray(0);
        
        vboIndID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndID);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        
        vsID = loadShader("res/shaders/vertexShader.glsl", GL20.GL_VERTEX_SHADER);
        fsID = loadShader("res/shaders/fragmentShader.glsl", GL20.GL_FRAGMENT_SHADER);
        
        pID = GL20.glCreateProgram();
        GL20.glAttachShader(pID, vsID);
        GL20.glAttachShader(pID, fsID);
        
        GL20.glBindAttribLocation(pID, 0, "vertexPosition");
        GL20.glBindAttribLocation(pID, 1, "vertexNormal");
        //GL20.glBindAttribLocation(pID, 1, "texture");
        
        GL20.glLinkProgram(pID);
        GL20.glValidateProgram(pID);
        
        //texID = loadTexture("res/textures/Texture1.png", GL13.GL_TEXTURE0);
        
        mPos = new Vector3f(0f, 0f, 0f);
        mAng = new Vector3f(0f, 0f, 0f);
        mScale = new Vector3f(1.0f, 1.0f, 1.0f);
        
        camPos = new Vector3f(0f, -0.1f, -0.5f);
        camAng = new Vector3f(0f, 0f, 0f);
        
        mMatLoc = GL20.glGetUniformLocation(pID, "model");
        vMatLoc = GL20.glGetUniformLocation(pID, "view");
        pMatLoc = GL20.glGetUniformLocation(pID, "projection");
        
        mMat = new Matrix4f();
        vMat = new Matrix4f();
        pMat = new Matrix4f();
        
        pMat.perspective(3.1415926535f / 180f * 60f, (float) WIDTH / (float) HEIGHT, 0.015f, 100f);
        
        matBuff = BufferUtils.createFloatBuffer(16);
        
        lPosLoc = GL20.glGetUniformLocation(pID, "lightPosition");
        lColLoc = GL20.glGetUniformLocation(pID, "lightColour");
        
        Light l = new Light(new Vector3f(0f, 1f, 1f), new Vector3f(0.8f, 0.8f, 0.8f));
        
        shineDamperLoc = GL20.glGetUniformLocation(pID, "shineDamper");
        reflectivityLoc = GL20.glGetUniformLocation(pID, "reflectivity");
        
        while ( glfwWindowShouldClose(window) == GL_FALSE ) {
            vMat = new Matrix4f();
            
            vMat.rotateZ((float) (3.1415926535f / 180f * camAng.z));
            vMat.rotateY((float) (3.1415926535f / 180f * camAng.y));
            vMat.rotateX((float) (3.1415926535f / 180f * camAng.x));
            vMat.translate(camPos.x, camPos.y, camPos.z);
            
            GL20.glUseProgram(pID);
             
            pMat.get(0, matBuff);
            GL20.glUniformMatrix4fv(pMatLoc, false, matBuff);
            vMat.get(0, matBuff);
            GL20.glUniformMatrix4fv(vMatLoc, false, matBuff);
            mMat.get(0, matBuff);
            GL20.glUniformMatrix4fv(mMatLoc, false, matBuff);
            
            GL20.glUniform3f(lPosLoc, l.getPosition().x, l.getPosition().y, l.getPosition().z);
            GL20.glUniform3f(lColLoc, l.getColour().x, l.getColour().y, l.getColour().z);
            
            GL20.glUniform1f(shineDamperLoc, m.getShineDamper());
            GL20.glUniform1f(reflectivityLoc, m.getReflectivity());
            
            GL20.glUseProgram(0);
        	
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            GL20.glUseProgram(pID);
            
            /*GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);*/
            
            GL30.glBindVertexArray(vaoID);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndID);
            
            GL11.glDrawElements(GL_TRIANGLES, m.faces.size() * 3, GL_UNSIGNED_SHORT, 0);
            
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL30.glBindVertexArray(0);
            
            /*GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL13.glActiveTexture(0);*/
            
            GL20.glUseProgram(0);
            
            glfwSwapBuffers(window);
 
            glfwPollEvents();
        }
    }
    
    private int loadShader(String filename, int type) {
        StringBuilder shaderSource = new StringBuilder();
        int shaderID = 0;
         
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Could not read file.");
            e.printStackTrace();
            System.exit(-1);
        }
         
        shaderID = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderID, shaderSource);
        GL20.glCompileShader(shaderID);
         
        return shaderID;
    }
    
    private int loadTexture(String filename, int textureUnit) {
        ByteBuffer buf = null;
        int tWidth = 0;
        int tHeight = 0;
         
        try {
            InputStream in = new FileInputStream(filename);
            
            PNGDecoder decoder = new PNGDecoder(in);
             
            tWidth = decoder.getWidth();
            tHeight = decoder.getHeight();
             
            buf = ByteBuffer.allocateDirect(
                    4 * decoder.getWidth() * decoder.getHeight());
            decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
            buf.flip();
             
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
         
        int texId = GL11.glGenTextures();
        GL13.glActiveTexture(textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
         
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
         
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, tWidth, tHeight, 0, 
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
         
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
         
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 
                GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, 
                GL11.GL_LINEAR_MIPMAP_LINEAR);
         
        return texId;
    }
 
    public static void main(String[] args) {
        new Display().run();
    }
    
}
