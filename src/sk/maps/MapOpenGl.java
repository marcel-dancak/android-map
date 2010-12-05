package sk.maps;

import static javax.microedition.khronos.opengles.GL10.GL_COLOR_BUFFER_BIT;
import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import sk.maps.Layer.Tile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.MotionEvent;

public class MapOpenGl extends GLSurfaceView implements MapView, GLSurfaceView.Renderer {

	private static final String TAG = MapOpenGl.class.getName();
	
	private PointF center;
	private BBox bbox;
	private double resolutions[];
	private Plane openglTile;
	
	private int zoom = -1;

	private float tileWidth;
	private float tileHeight;
	private int tileWidthPx = 256;
	private int tileHeightPx = 256;

	private PointF centerAtDragStart;
	private PointF lastPos;
	
	private java.util.Map<String, Tile> tiles = new HashMap<String, Tile>();
	
	// size of map in pixels
	private int width;
	private int height;
	
	private TmsLayer tmsLayer;
	
	public MapOpenGl(Context context, TmsLayer layer) {
		super(context);
		setEGLConfigChooser(false);
		//setRenderer(new MapRenderer(context));
		setRenderer(this);
		//setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		bbox = layer.getBoundingBox();
		resolutions = layer.getResolutions();
		tmsLayer = layer;
		center = new PointF((bbox.minX + bbox.maxX) / 2f, (bbox.minY + bbox.maxY) / 2f);
		setZoom(1);
	}

	@Override
	public void setZoom(int zoom) {
		if (zoom > 0) {
			int oldZoom = this.zoom;
			this.zoom = zoom;
			onZoomChange(oldZoom, zoom);
		}
	}

	@Override
	public int getZoom() {
		return zoom;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, format("width: %d height: %d", w, h));
		width = w;
		height = h;
		clearTiles();
	}

	protected void onZoomChange(int oldZoom, int zoom) {
		tileWidth = tileWidthPx * getResolution();
		tileHeight = tileHeightPx * getResolution();
		/*
		RectF tileBbox = new RectF(bbox.minX, bbox.minY, bbox.minX+256*getResolution(), bbox.minY+256*getResolution());
		Bitmap tile = new WmsLayer().requestTile(tileBbox, 256, 256);
		if (tile != null) {
			tiles.add(tile);
		}
		*/
		clearTiles();
	}
	
	private void clearTiles() {
		tiles.clear();
	}
	
	private PointF dragStart;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() == 1) {
			int action = event.getAction() & MotionEvent.ACTION_MASK;
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				centerAtDragStart = new PointF(center.x, center.y);
				//lastPos = screenToMap(event.getX(), event.getY());
				dragStart = screenToMap(event.getX(), event.getY());
				break;
			case MotionEvent.ACTION_MOVE:
				/*
				PointF dragPos2 = screenToMap2(event.getX(), event.getY());
				center.offset(lastPos.x-dragPos2.x, lastPos.y-dragPos2.y);
				lastPos = dragPos2;
				*/
				PointF dragPos = screenToMap2(event.getX(), event.getY());
				center.set(centerAtDragStart);
				center.offset(dragStart.x - dragPos.x, dragStart.y - dragPos.y);
				
				requestRender();
			}
		}
		return true;
	}

	private Point getTileAtScreen(int x, int y) {
		assert x <= width && y <= height : "point outside the screen";
		PointF mapPos = screenToMap(x, y);
		// Log.i(TAG, format("mapPos %f, %f", mapPos.x, mapPos.y));
		float tileX = (mapPos.x - bbox.minX) / tileWidth;
		float tileY = (mapPos.y - bbox.minY) / tileHeight;
		return new Point((int) Math.floor(tileX), (int) Math.floor(tileY));
	}

	private PointF screenToMap2(float x, float y) {
		float offsetX = x - width / 2f;
		float offsetY = (height / 2f) - y;
		return new PointF(centerAtDragStart.x + offsetX * getResolution(), centerAtDragStart.y
				+ offsetY * getResolution());
	}

	private PointF screenToMap(float x, float y) {
		float offsetX = x - width / 2f;
		float offsetY = (height / 2f) - y;
		return new PointF(center.x + offsetX * getResolution(), center.y + offsetY
				* getResolution());
	}

	private PointF mapToScreen(float x, float y) {
		float offsetX = x - center.x;
		float offsetY = center.y - y;
		return new PointF(width / 2f + offsetX / getResolution(), height / 2f + offsetY
				/ getResolution());
	}

	private float getResolution() {
		//return 1.125f / zoom;
		return (float) resolutions[zoom-1];
	}
	
	private void drawTiles(GL10 gl) {
		PointF o = screenToMap(0, height);
		if (o.x > bbox.maxX || o.y > bbox.maxY) {
			return;
		}
		Point s = getTileAtScreen(0, height);
		Point e = getTileAtScreen(width, 0);
		//Log.i(TAG, format("s.x=%d s.y=%d", s.x, s.y));
		//Log.i(TAG, format("e.x=%d e.y=%d", e.x, e.y));
		
		//Log.i(TAG, format("left-top tile: [%d, %d] right-bottom tile: [%d, %d]", s.x, s.y, e.x, e.y));
		int lastTileX = (int) ((bbox.maxX - bbox.minX) / tileWidth);
		int lastTileY = (int) ((bbox.maxY - bbox.minY) / tileHeight);
	
		lastTileX = e.x < lastTileX ? e.x : lastTileX;
		lastTileY = e.y < lastTileY ? e.y : lastTileY;
		int firstTileX = s.x > 0 ? s.x : 0;
		int firstTileY = s.y > 0 ? s.y : 0;
		PointF fixedPoint = mapToScreen(bbox.minX + tileWidth * firstTileX, bbox.minY + tileHeight * firstTileY);
		int sx = (int) fixedPoint.x;
		int sy = (int) fixedPoint.y;
		
		//gl.glTranslatex(sx, height-sy, 0);
		//openglTile.draw(gl);
		//Log.i(TAG, format("sx=%d sy=%d firstTileX=%d firstTileY=%d", sx, sy, firstTileX, firstTileY));
		//Log.i(TAG, format("lastTileX=%d lastTileY=%d", lastTileX, lastTileY));
		//if (true) return;
		
		gl.glVertexPointer(3, GL10.GL_FIXED, 0, openglTile.vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, openglTile.texCoordBuffer);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, openglTile.textureId);
		
		for (int x = firstTileX; x <= lastTileX; x++) {
			for (int y = firstTileY; y <= lastTileY; y++) {
				String tileKey = format("%d:%d", x, y);
				Tile tile = null;
				if (tiles.containsKey(tileKey)) {
					tile = tiles.get(tileKey);
				} else {
					//tmsLayer.requestTile(zoom, x, y, tileWidthPx, tileHeightPx);
					//tile = new Tile(x, y, null);
					//tiles.put(tileKey, tile);
				}
				//if (tile.getImage() != null) {
					//canvas.drawBitmap(tile.getImage(), sx + (256*(x-firstTileX)), sy-(y-firstTileY+1)*256, null);
					gl.glPushMatrix();
					gl.glTranslatex(sx + (256*(x-firstTileX)), height-(sy-(y-firstTileY)*256), 0);
					openglTile.draw(gl);
					gl.glPopMatrix();
				//}
				//drawTile(canvas, x, y);
			}
		}
	}
	
	private float offset;
	private void drawAnimation(GL10 gl) {
		gl.glLoadIdentity();
		gl.glVertexPointer(3, GL10.GL_FIXED, 0, openglTile.vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, openglTile.texCoordBuffer);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, openglTile.textureId);
		
		offset += 0.2;
		if (offset > 1000) {
			offset = 0;
		}
		int x = (int) (Math.sin(offset)*25f);
		gl.glTranslatex(x, 0, 0);
		openglTile.draw(gl);
		gl.glTranslatex(256, 0, 0);
		openglTile.draw(gl);
		gl.glTranslatex(0, 256, 0);
		openglTile.draw(gl);
		gl.glTranslatex(-256, 0, 0);
		openglTile.draw(gl);
	}
	
	/******************************************************/
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.i(TAG, "opengl init");
		//gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glClearColor(0.0f, 0.5f, 0.2f, 0.5f);
		
		openglTile = new Plane(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		this.width = width;
		this.height = height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthox(0, width, 0, height, -1, 1);
		//gl.glOrthof(0, width, 0, height, -1, 1);
		//GLU.gluOrtho2D(gl, 0, width, 0, height);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		Log.i(TAG, format("opengl config: width=%d height=%d", width, height));
	}

	private float z;
	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glLoadIdentity();
		//Log.i(TAG, Thread.currentThread().getName());
		//drawAnimation(gl);
		drawTiles(gl);
		
		/*
		z += .1;
		//gl.glTranslatef((float) Math.sin(z)*0.001f, 0, 0);//(float) Math.sin(z));
		gl.glTranslatex((int) (Math.sin(z)*20f), 0, 0);
		//Log.i(TAG, "opengl drawing");
		openglTile.draw(gl);
		*/
	}
	
	public static class Plane {
		private IntBuffer vertexBuffer;
		private FloatBuffer texCoordBuffer;
		private Bitmap image;
		private int textureId;
		
		private float offset;
		
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
			//gl.glLoadIdentity();
			//offset += 0.04;
			//if (offset > 100) {
			//	offset = 0;
			//}
			//gl.glTranslatef(offset, 0, 0);
			//gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

			//gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
			//gl.glDisable(GL10.GL_TEXTURE_2D);
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
