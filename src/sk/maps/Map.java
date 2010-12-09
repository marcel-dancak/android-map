package sk.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.jhlabs.geom.Point2D;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import static java.lang.String.format;

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
import android.view.View;

import sk.maps.Layer.Tile;
import sk.maps.Layer.TileListener;
import sk.utils.Utils;

public class Map extends View implements TileListener, MapView {

	private static String TAG = Map.class.getName();

	private PointF center;
	private BBox bbox;
	private double resolutions[];
	private Projection proj;
	private int zoom = -1;
	private int heading;
	
	private float tileWidth;
	private float tileHeight;
	private int tileWidthPx = 256;
	private int tileHeightPx = 256;

	// size of map in pixels
	private int width;
	private int height;

	//private List<Layer> layers = Collections.emptyList();
	private TmsLayer tmsLayer;
	private java.util.Map<String, Tile> tiles = new HashMap<String, Tile>();
	
	private Paint imagesStyle;
	private Paint mapStyle;
	private Paint tileStyle;
	private Paint screenBorderStyle;
	private Timer animTimer = new Timer();
	
	public Map(Context context, BBox bbox, double resolutions[], TmsLayer layer) {
		super(context);
		//this.layers = Collections.unmodifiableList(layers);
		this.tmsLayer = layer;
		this.bbox = bbox;
		this.resolutions = resolutions;
		
		center = new PointF((bbox.minX + bbox.maxX) / 2f, (bbox.minY + bbox.maxY) / 2f);

		imagesStyle = new Paint();
		imagesStyle.setFilterBitmap(true);
		
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
		
		proj = layer.getProjection();
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

	public int getZoom() {
		return zoom;
	}
	
	public void setZoom(int zoom) {
		if (zoom > 0 && zoom < resolutions.length) {
			int oldZoom = this.zoom;
			this.zoom = zoom;
			onZoomChange(oldZoom, zoom);
			invalidate();
		}
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
	
	private PointF centerAtDragStart;
	private PointF dragStart;

	private PointF lastPos;
	private PointF dragStartPx;

	private boolean filterEvent;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() == 1) {
			int action = event.getAction() & MotionEvent.ACTION_MASK;
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				/*
				Log.i(TAG, format("Tiles: %d Memory Free: %d kB Heap size:%d kB Max: %d kB", 
						tiles.size(),
						Runtime.getRuntime().freeMemory()/1024,
						Runtime.getRuntime().totalMemory()/1024,
						Runtime.getRuntime().maxMemory()/1024));
				*/
				centerAtDragStart = new PointF(center.x, center.y);
				dragStart = screenToMap(event.getX(), event.getY());
				dragStartPx = new PointF(event.getX(), event.getY());
				lastPos = screenToMap(event.getX(), event.getY());
				animTimer.schedule(new TimerTask() {
					
					@Override
					public void run() {
						post(new Runnable() {
							
							@Override
							public void run() {
								invalidate();
							}
						});
					}
				}, 30, 40);
				
				break;
			case MotionEvent.ACTION_UP:
				animTimer.cancel();
				animTimer = new Timer();
				break;
			case MotionEvent.ACTION_MOVE:
				/*
				filterEvent = !filterEvent;
				if (filterEvent) {
					break;
				}
				*/
//				PointF dragPos2 = screenToMap2(event.getX(), event.getY());
//				center.offset(lastPos.x-dragPos2.x, lastPos.y-dragPos2.y);
//				lastPos = dragPos2;
//				invalidate();
//				if (true) break;
				
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

				//invalidate();
				break;
			}
		}
		// Utils.dumpEvent(event);
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);
		canvas.rotate(heading, width/2f, height/2f);
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
		
		long t1 = System.currentTimeMillis();
		//Log.i(TAG, format("sx=%d sy=%d firstTileX=%d firstTileY=%d", sx, sy, firstTileX, firstTileY));
		for (int x = firstTileX; x <= lastTileX; x++) {
			for (int y = firstTileY; y <= lastTileY; y++) {
				String tileKey = tileKey(x, y);
				Tile tile = null;
				if (tiles.containsKey(tileKey)) {
					tile = tiles.get(tileKey);
				} else {
					//tmsLayer.requestTile(zoom, x, y, tileWidthPx, tileHeightPx);
					tile = new Tile(x, y, zoom, null);
					//tiles.put(tileKey, tile);
				}
				if (tile.getImage() != null) {
					canvas.drawBitmap(tile.getImage(), sx + (256*(x-firstTileX)), sy-(y-firstTileY+1)*256, imagesStyle);
				}
				//drawTile(canvas, x, y);
			}
		}
		//System.out.println("rendering time: "+(System.currentTimeMillis()-t1));
		t1 = System.currentTimeMillis();
		for (int x = firstTileX; x <= lastTileX; x++) {
			for (int y = firstTileY; y <= lastTileY; y++) {
				String tileKey = tileKey(x, y);
				if (!tiles.containsKey(tileKey)) {
					//tmsLayer.requestTile(zoom, x, y, tileWidthPx, tileHeightPx);
					Tile tile = new Tile(x, y, zoom, null);
					//System.out.println("need a tile");
					tmsLayer.requestTile2(tile);
					tiles.put(tileKey, tile);
				}
			}
		}
		//System.out.println("requesting time: "+(System.currentTimeMillis()-t1));
		
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
			tmsLayer.requestTile(zoom, x, y, tileWidthPx, tileHeightPx);
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
		post(new Runnable() {
			
			@Override
			public void run() {
				invalidate();
			}
		});
	}

	@Override
	public void onTileLoadingFailed(Tile tile) {
		String tileKey = format("%d:%d", tile.getX(), tile.getY());
		if (tiles.containsKey(tileKey)) {
			tiles.remove(tileKey);
		}
	}
	
	private static final String tileKey(int x, int y) {
		return x+":"+y;
	}

	@Override
	public void setHeading(int heading) {
		this.heading = heading;
		invalidate();
	}
}
