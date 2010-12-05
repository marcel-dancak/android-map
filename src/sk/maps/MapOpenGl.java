package sk.maps;

import static javax.microedition.khronos.opengles.GL10.GL_COLOR_BUFFER_BIT;
import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

public class MapOpenGl extends GLSurfaceView implements MapView, GLSurfaceView.Renderer {

	private static final String TAG = MapOpenGl.class.getName();
	
	private Plane tile;
	
	public MapOpenGl(Context context) {
		super(context);
		setEGLConfigChooser(false);
		//setRenderer(new MapRenderer(context));
		setRenderer(this);
	}

	private int zoom;
	
	@Override
	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	@Override
	public int getZoom() {
		return zoom;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.i(TAG, "opengl init");
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glClearColor(0.0f, 0.5f, 0.2f, 0.5f);
		
		tile = new Plane(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthox(0, width, 0, height, -100, 100);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		Log.i(TAG, format("opengl config: width=%d height=%d", width, height));
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		gl.glLoadIdentity();
		gl.glTranslatef(0, 0, -2.0f);
		//Log.i(TAG, "opengl drawing");
		tile.draw(gl);
	}
	
	public static class Plane {
		private IntBuffer vertexBuffer;
		private FloatBuffer texCoordBuffer;
		private Bitmap image;
		private int textureId;
		
		public Plane(GL10 gl) {
			int width = 256;
			int height = 256;
			int[] vertices = {
					0, height, 0,
					width, height, 0,
					0, 0, 0,
					width, 0, 0,
			};
			ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
			vbb.order(ByteOrder.nativeOrder());
			vertexBuffer = vbb.asIntBuffer();
			vertexBuffer.put(vertices);
			vertexBuffer.position(0);
			
			// float size is 4
			ByteBuffer texVbb = ByteBuffer.allocateDirect(2*4*4);
			texVbb.order(ByteOrder.nativeOrder());
			texCoordBuffer = texVbb.asFloatBuffer();
			//Put the first coordinate (x,y (s,t):0,0)
			texCoordBuffer.put(new float[] {
					0f, 1f,
					1f, 1f,
					0f, 0f,
					1f, 0f
			});
			texCoordBuffer.position(0);

			
			int[] textures = new int[1];
			gl.glGenTextures(1, textures, 0);
			textureId = textures[0];
			
			image = BitmapFactory.decodeFile("/sdcard/tiles/4-3.jpeg");
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);			
			gl.glTexParameterx(GL10.GL_TEXTURE_2D,
			      GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D,
			      GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
			        GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
			        GL10.GL_CLAMP_TO_EDGE);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, image, 0);
			image.recycle();
		}
		
		public void draw(GL10 gl) {
			gl.glLoadIdentity();
			gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
			gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoordBuffer);
			//gl.glColor4f(0, 0, 1, 1);
			//gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 3, GL10.GL_UNSIGNED_SHORT, vertexBuffer);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	
	
	
	public static class MapRenderer implements GLSurfaceView.Renderer {

		private Plane tile;
		private SimpleTriangleRenderer triangle;
		
		public MapRenderer(Context context) {
			triangle = new SimpleTriangleRenderer(context, 1.5f);
		}
		
		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			tile = new Plane(gl);
			//gl.glDisable(GL10.GL_DITHER);
			//gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
			gl.glClearColor(.5f, .5f, .5f, 1);
			//gl.glShadeModel(GL10.GL_SMOOTH);
			//gl.glEnable(GL10.GL_DEPTH_TEST);
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			gl.glViewport(0, 0, width, height);
			float ratio = (float) width / height;
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7);

		}

		@Override
		public void onDrawFrame(GL10 gl) {
			//gl.glDisable(GL10.GL_DITHER);
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();
			GLU.gluLookAt(gl, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
			
			triangle.draw(gl);
			tile.draw(gl);
		}
		
	}
	
	public static class SimpleTriangleRenderer {
	   //Number of points or vertices we want to use
	        private final static int VERTS = 3;
	        //A raw native buffer to hold the point coordinates
	        private FloatBuffer mFVertexBuffer;
	        //A raw native buffer to hold indices
	        //allowing a reuse of points.
	        private ShortBuffer mIndexBuffer;
	        public SimpleTriangleRenderer(Context context, float size)
	        {
	             ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
	             vbb.order(ByteOrder.nativeOrder());
	             mFVertexBuffer = vbb.asFloatBuffer();
	             ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
	             ibb.order(ByteOrder.nativeOrder());
	             mIndexBuffer = ibb.asShortBuffer();
	             float[] coords = {
	                       -0.5f*size, -0.5f*size, 0, // (x1,y1,z1)
	                        0.5f*size, -0.5f*size, 0,
	                        0.0f, 0.5f*size, 0
	             };
	             for (int i = 0; i < VERTS; i++) {
	                  for(int j = 0; j < 3; j++) {
	                       mFVertexBuffer.put(coords[i*3+j]);
	                  }
	             }
	             short[] myIndecesArray = {0,1,2};
	             for (int i=0;i<3;i++)
	             {
	                 mIndexBuffer.put(myIndecesArray[i]);
	             }
	             mFVertexBuffer.position(0);
	             mIndexBuffer.position(0);
	        }
	       //overriden method
	        protected void draw(GL10 gl)
	        {
	            gl.glColor4f(1.0f, 0, 0, 0.5f);
	            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
	            gl.glDrawElements(GL10.GL_TRIANGLES, VERTS,
	                       GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
	        }
	}

}
