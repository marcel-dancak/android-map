package sk.maps;

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.jhlabs.geom.Point2D;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import sk.maps.Layer.Tile;
import sk.maps.Layer.TileListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MapSurface extends SurfaceView implements SurfaceHolder.Callback, MapView {

	private static final String TAG = MapSurface.class.getName();
	
	private MapThread mapThread;
	
	public MapSurface(Context context, BBox bbox, double resolutions[], TmsLayer layer) {
		super(context);
		getHolder().addCallback(this);
		mapThread = new MapThread(getHolder(), this, bbox, resolutions, layer);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		mapThread.setRunning(true);
		mapThread.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mapThread.onSizeChanged(width, height, -1, -1);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//Log.i(TAG, "somebody touched me!");
		return mapThread.onTouchEvent(event);
	}
	
	public int getZoom() {
		return mapThread.getZoomLevel();
	}
	
	public void setZoom(int zoom) {
		mapThread.setZoom(zoom);
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mapThread.setRunning(false);
        while (retry) {
            try {
            	mapThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
	}

	class MapThread extends Thread implements TileListener {
	    private SurfaceHolder surfaceHolder;
	    private MapSurface map;
	    private boolean run = false;
	 
	    
		private PointF center;
		private BBox bbox;
		private double resolutions[];
		private Projection proj;
		private int zoom = -1;

		private float tileWidth;
		private float tileHeight;
		private int tileWidthPx = 256;
		private int tileHeightPx = 256;

		// size of map in pixels
		private int width;
		private int height;

		private List<Layer> layers = Collections.emptyList();
		private TmsLayer tmsLayer;
		private java.util.Map<String, Tile> tiles = new HashMap<String, Tile>();
		
		private Paint mapStyle;
		private Paint tileStyle;
		private Paint screenBorderStyle;
		
		
	    public MapThread(SurfaceHolder surfaceHolder, MapSurface map, BBox bbox, double resolutions[], TmsLayer layer) {
	        this.surfaceHolder = surfaceHolder;
	        this.map = map;
	        
			this.tmsLayer = layer;
			this.bbox = bbox;
			this.resolutions = resolutions;
			
			center = new PointF((bbox.minX + bbox.maxX) / 2f, (bbox.minY + bbox.maxY) / 2f);

			mapStyle = new Paint();
			mapStyle.setColor(Color.argb(127, 127, 20, 20));

			tileStyle = new Paint();
			tileStyle.setColor(Color.LTGRAY);
			tileStyle.setStyle(Paint.Style.STROKE);
			tileStyle.setTextSize(30);
			tileStyle.setAntiAlias(true);

			screenBorderStyle = new Paint();
			screenBorderStyle.setStyle(Paint.Style.STROKE);
			screenBorderStyle.setStrokeWidth(2f);
			screenBorderStyle.setAntiAlias(true);
			screenBorderStyle.setColor(Color.argb(255, 20, 40, 120));

			buildTextTextures();
			//tmsLayer = new TmsLayer("http://tc.gisplan.sk/1.0.0/", "tmspresov_ortofoto_2009", "jpeg");
			//tmsLayer = new TmsLayer("http://tc.gisplan.sk/1.0.0/", "tmspresov_gg_ortofoto_2009", "jpeg");
			tmsLayer.addTileListener(this);
			setZoom(1);
			
			String[] params = {
					"+proj=merc",
					"+a=6378137",
					"+b=6378137",
					"+lat_ts=0.0",
					"+lon_0=0.0",
					"+x_0=0.0",
					"+y_0=0",
					"+k=1.0",
					"+units=m",
					"+nadgrids=@null",
					"+no_defs"
				};
			proj = ProjectionFactory.fromPROJ4Specification(params);
		}

		private Bitmap textBuffer;
		private float[] numbersWidths;

		private void buildTextTextures() {
			textBuffer = Bitmap.createBitmap(25, 400, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(textBuffer);
			// FontMetrics fm = new FontMetrics();

			numbersWidths = new float[10];
			tileStyle.getTextWidths("0123456789", numbersWidths);
			for (int i = 0; i < 10; i++) {
				canvas.drawText("" + i, 0, (i+1) * 30, tileStyle);
			}
			canvas.drawText("x=", 0, 340, tileStyle);
			canvas.drawText("y=", 0, 370, tileStyle);
		}

		public int getZoomLevel() {
			return zoom;
		}
		
		public void setZoom(int zoom) {
			if (zoom > 0) {
				int oldZoom = this.zoom;
				this.zoom = zoom;
				onZoomChange(oldZoom, zoom);
				//invalidate();
				redraw();
			}
		}

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
			redraw();
		}

		private void clearTiles() {
			tiles.clear();
		}
		
		private PointF centerAtDragStart;
		private PointF dragStart;

		private PointF lastPos;
		private PointF dragStartPx;

		
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getPointerCount() == 1) {
				int action = event.getAction() & MotionEvent.ACTION_MASK;
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					centerAtDragStart = new PointF(center.x, center.y);
					dragStart = screenToMap(event.getX(), event.getY());
					dragStartPx = new PointF(event.getX(), event.getY());
					lastPos = screenToMap(event.getX(), event.getY());
					break;
				case MotionEvent.ACTION_MOVE:
					PointF dragPos2 = screenToMap2(event.getX(), event.getY());
					center.offset(lastPos.x-dragPos2.x, lastPos.y-dragPos2.y);
					lastPos = dragPos2;
					redraw();
					if (true) break;
					
					PointF dragPos = screenToMap2(event.getX(), event.getY());
					// Log.i(TAG, format("Start [%f, %f] currnet [%f, %f]",
					// dragStart.x, dragStart.y, dragPos.x, dragPos.y));
					// Log.i(TAG,
					// format("cur(%f, %f) dragStart(%f, %f) Move [%f, %f]",
					// dragPos.x, dragPos.y, dragStart.x, dragStart.y,
					// dragStart.x-dragPos.x, dragStart.y-dragPos.y));
					/*
					 * Log.i(TAG, format("set center (%f, %f) vs (%f, %f)",
					 * centerAtDragStart.x+(dragStart.x-dragPos.x),
					 * centerAtDragStart.y+(dragStart.y-dragPos.y),
					 * (dragStartPx.x-event.getX())*getResolution(),
					 * (dragStartPx.y-event.getY())*getResolution()));
					 */
					center.set(centerAtDragStart);
					center.offset(dragStart.x - dragPos.x, dragStart.y - dragPos.y);
					// center.offset((dragStartPx.x-event.getX())*getResolution(),
					// (dragStartPx.y-event.getY())*getResolution());
					redraw();
					break;
				}
			}
			// Utils.dumpEvent(event);
			return true;
		}

		void redraw() {
			//Log.i(TAG, "redraw please");
			synchronized (obj) {
				//Log.i(TAG, "Ok then");
				obj.notifyAll();
			}
		}
		
		protected void onDraw(Canvas canvas) {
			canvas.drawRGB(255, 255, 255);
			//float scale = 1.0f; 
			//canvas.scale(scale, scale, width / 2f, height / 2f);
			// float cx = width/2.0f;
			// float cy = height/2.0f;
			// Log.d(TAG, format("center [%f, %f]", cx, cy));
			// Log.d(TAG, format("[0,0] ==> [%f, %f]", center.x-cx*getResolution(),
			// center.y-cy*getResolution()));
			/*
			 * for (Layer layer : layers) { layer.drawTile(canvas); }
			 */

			PointF startP = mapToScreen(bbox.minX, bbox.minY);
			PointF endP = mapToScreen(bbox.maxX, bbox.maxY);
			//canvas.drawArc(new RectF(startP.x-2, startP.y-2, startP.x+2, startP.y+2), 0, 360, true, screenBorderStyle);
			//canvas.drawRect(startP.x, startP.y, endP.x, endP.y, mapStyle);
			canvas.drawRect(startP.x, endP.y+1, endP.x, startP.y-1, mapStyle);
			/*
			 * float sx = startP.x > 0? startP.x : 0; float sy = startP.y > 0?
			 * startP.y : 0; float ex = endP.x < width? endP.x : width; float ey =
			 * endP.y < height? endP.y : height; canvas.drawRect(sx, sy, ex, ey,
			 * mapStyle);
			 */

			//canvas.drawRect(1, 1, width, height, screenBorderStyle);
			// Log.d(TAG, format("first tile [%f, %f]", startP.x, startP.y));
			
			PointF o = screenToMap(0, height);
			if (o.x > bbox.maxX || o.y > bbox.maxY) {
				return;
			}
			Point s = getTileAtScreen(0, height);
			Point e = getTileAtScreen(width, 0);
			
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
			
			//Log.i(TAG, format("sx=%d sy=%d firstTileX=%d firstTileY=%d", sx, sy, firstTileX, firstTileY));
			for (int x = firstTileX; x <= lastTileX; x++) {
				for (int y = firstTileY; y <= lastTileY; y++) {
					String tileKey = format("%d:%d", x, y);
					Tile tile = null;
					if (tiles.containsKey(tileKey)) {
						tile = tiles.get(tileKey);
					} else {
						tmsLayer.requestTile(tile);
						tile = new Tile(x, y, zoom, null);
						tiles.put(tileKey, tile);
					}
					if (tile.getImage() != null) {
						canvas.drawBitmap(tile.getImage(), sx + (256*(x-firstTileX)), sy-(y-firstTileY+1)*256, null);
					}
					//drawTile(canvas, x, y);
				}
			}
			
			Point2D p = new Point2D();
			proj.transform(new Point2D(21.23886386, 49.00096926), p);
			//Log.i(TAG, format("projected position: [%f, %f]", p.x, p.y));
			float positionOffsetX = (float) p.x-(bbox.minX + tileWidth * firstTileX);
			float positionOffsetY = (float) p.y-(bbox.minY + tileHeight * firstTileY);
			PointF currentPos = new PointF(sx+positionOffsetX/getResolution(), sy-positionOffsetY/getResolution());
			
			//PointF currentPos = mapToScreen((float) p.x, (float) p.y);
			//PointF currentPos = mapToScreen(2367713, 6276560);
			//bbox = 2351125 2376721
			//Log.i(TAG, format("on screen: [%d, %d]", (int) currentPos.x, (int) currentPos.y));
			//PointF currentPos = mapToScreen(21.23886386f, 49.00096926f);
			//canvas.drawArc(new RectF(currentPos.x-2, currentPos.y-2, currentPos.x+2, currentPos.y+2), 0, 360, true, screenBorderStyle);
			canvas.drawArc(new RectF(currentPos.x-2, currentPos.y-2, currentPos.x+2, currentPos.y+2), 0, 360, true, screenBorderStyle);
			
			//canvas.drawBitmap(textBuffer, 0, 0, null);
			//drawTile(canvas, 0, 0);
		}

		private Point getTileAtScreen(int x, int y) {
			assert x <= width && y <= height : "point outside the screen";
			PointF mapPos = screenToMap(x, y);
			// Log.i(TAG, format("mapPos %f, %f", mapPos.x, mapPos.y));
			float tileX = (mapPos.x - bbox.minX) / tileWidth;
			float tileY = (mapPos.y - bbox.minY) / tileHeight;
			return new Point((int) Math.floor(tileX), (int) Math.floor(tileY));
		}

		private void drawTile(Canvas canvas, int x, int y) {
			String tileKey = format("%d:%d", x, y);
			Tile tile = null;
			if (tiles.containsKey(tileKey)) {
				tile = tiles.get(tileKey);
			} else {
				tmsLayer.requestTile(tile);
				tile = new Tile(x, y, zoom, null);
				tiles.put(tileKey, tile);
			}
			
			/* tests if there can be some rounding errors
			PointF startP = mapToScreen(bbox.minX + (tileWidthPx*x)*getResolution(), bbox.minY + (tileHeightPx*y)*getResolution());
			Log.i(TAG, format("x diff %f, y diff %f",
					(((tileWidthPx*x)*getResolution())-tileWidth*x),
					(((tileHeightPx*y)*getResolution())-tileHeight*y)
					));
			*/
			PointF startP = mapToScreen(bbox.minX + tileWidth * x, bbox.minY + tileHeight * y);
			if (tile.getImage() != null) {
				canvas.drawBitmap(tile.getImage(), startP.x, startP.y-256f, null);
			}
			if (true) return;
			
			canvas.drawRect(startP.x, startP.y - 256f, startP.x + 256f, startP.y, tileStyle);
			/*
			canvas.drawText(format("x=%d y=%d", x, y),
				startP.x+50, startP.y+127,
				tileStyle);
			if (true) return;
			*/
			
			int xPos = (int) startP.x+50;
			int yPos = (int) startP.y-127;
			
			// draw "x="
			canvas.drawBitmap(textBuffer,
					new Rect(0, 310, 25, 340),
					new Rect(xPos, yPos, xPos+25, yPos+30),
					null);
			xPos += 30;
			drawNumber(canvas, x, xPos, yPos);
			xPos += 35;
			
			// draw "y="
			canvas.drawBitmap(textBuffer,
					new Rect(0, 340, 25, 380),
					new Rect(xPos, yPos, xPos+25, yPos+40),
					null);
			xPos += 30;
			drawNumber(canvas, y, xPos, yPos);
		}

		private void drawNumber(Canvas canvas, int number, int x, int y) {
			String strNum = "" + number;
			//Log.i(TAG, "draw number: "+number);
			int xPos = x;
			for (int i = 0; i < strNum.length(); i++) {
				int n = strNum.charAt(i) - 48;
				//Log.i(TAG, "digit: "+n);
				int width = (int) numbersWidths[n];
				canvas.drawBitmap(textBuffer,
						new Rect(0, 30*(n), width, 30*(n+1)),
						new RectF(xPos, y, xPos + width, y + 30),
						null);
				xPos += width+1;
			}
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

		@Override
		public void onTileLoad(Tile tile) {
			// throw away tiles with not actual zoom level (delayed)
			if (tile.getZoomLevel() != zoom) {
				tile.recycle();
				return;
			}
			String tileKey = format("%d:%d", tile.getX(), tile.getY());
			synchronized (tiles) {
				if (tiles.size() > 35) {
					Point centerTile = getTileAtScreen(width/2, height/2);
					while (tiles.size() > 35) {
						Tile mostFarAway = tiles.values().iterator().next();
						for (Tile t : tiles.values()) {
							if (Math.abs(centerTile.x-t.getX())+Math.abs(centerTile.y-t.getY()) >
									Math.abs(centerTile.x-mostFarAway.getX())+Math.abs(centerTile.y-mostFarAway.getY())) {
								mostFarAway = t;
							}
						}
						mostFarAway.recycle();
						tiles.remove(format("%d:%d", mostFarAway.getX(), mostFarAway.getY()));
					}
				}
				tiles.put(tileKey, tile);
			}
			redraw();
		}

		@Override
		public void onTileLoadingFailed(Tile tile) {
			String tileKey = format("%d:%d", tile.getX(), tile.getY());
			if (tiles.containsKey(tileKey)) {
				tiles.remove(tileKey);
			}
		}
	 
		public void setRunning(boolean running) {
			this.run = running;
		}
		
		Object obj = new String();
		
	    @Override
	    public void run() {
	    	while (run) {
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        //Log.i(TAG, "drawing");
                        c.drawRGB(255, 255, 255);
                        onDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
                try {
                	synchronized (obj) {
                		obj.wait();
                	}
				} catch (InterruptedException e) {
					Log.i(TAG, e.getMessage(), e);
				}
            }
	    }
	}

	@Override
	public void setHeading(int heading) {
		// TODO Auto-generated method stub
		
	}
}
