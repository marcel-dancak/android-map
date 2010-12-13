package sk.maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import static java.lang.String.format;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jhlabs.geom.Point2D;

import sk.maps.Layer.Tile;
import sk.maps.Layer.TileListener;
import sk.utils.TmsVisualDebugger;
import sk.utils.Utils;

public class Map extends View implements TileListener, MapView {

	private static String TAG = Map.class.getName();

	private PointF center;
	private BBox bbox;
	private int zoom = -1;
	private int heading;
	
	private float tileWidth;
	private float tileHeight;

	// size of screen (map) in pixels
	private int width;
	private int height;

	//private List<Layer> layers = Collections.emptyList();
	private TmsLayer tmsLayer;
	private java.util.Map<String, Tile> tiles = new HashMap<String, Tile>();
	
	// drawing styles
	private Paint imagesStyle;
	private Paint mapStyle;
	private Paint screenBorderStyle;
	private Paint screenBorderStyle2;
	
	private Timer animTimer = new Timer();
	private TmsVisualDebugger visualDebugger;
	private boolean isMooving;
	
	public Map(Context context, TmsLayer layer) {
		super(context);
		imagesStyle = new Paint();
		imagesStyle.setFilterBitmap(true);
		
		mapStyle = new Paint();
		mapStyle.setColor(Color.argb(127, 127, 20, 20));

		screenBorderStyle = new Paint();
		screenBorderStyle.setStyle(Paint.Style.STROKE);
		screenBorderStyle.setStrokeWidth(2f);
		screenBorderStyle.setAntiAlias(true);
		screenBorderStyle.setColor(Color.argb(255, 20, 40, 120));

		screenBorderStyle2 = new Paint(screenBorderStyle);
		screenBorderStyle2.setColor(Color.rgb(150, 30, 50));

		if (layer != null) {
			setLayer(layer);
		}
	}

	public void setLayer(TmsLayer layer) {
		tmsLayer = layer;
		bbox = layer.getBoundingBox();
		center = new PointF((bbox.minX + bbox.maxX) / 2f, (bbox.minY + bbox.maxY) / 2f);

		tmsLayer.addTileListener(this);
		visualDebugger = new TmsVisualDebugger(this);
		setZoom(1);
	}
	
	public int getZoom() {
		return zoom;
	}
	
	public void setZoom(int zoom) {
		if (zoom > 0 && zoom < tmsLayer.getResolutions().length) {
			int oldZoom = this.zoom;
			this.zoom = zoom;
			onZoomChange(oldZoom, zoom);
			invalidate();
		}
	}

	private int size;
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, format("width: %d height: %d", w, h));
		size = (int) Math.ceil(Math.sqrt(w*w+h*h));
		width = w;
		height = h;
		clearTiles();
	}

	protected void onZoomChange(int oldZoom, int zoom) {
		tileWidth = tmsLayer.getTileWidth() * getResolution();
		tileHeight = tmsLayer.getTileHeight() * getResolution();
		clearTiles();
	}

	public TmsLayer getLayer() {
		return tmsLayer;
	}
	
	private void clearTiles() {
		tiles.clear();
	}
	
	private PointF centerAtDragStart;
	private PointF dragStart;

	private PointF lastPos;
	private PointF dragStartPx;

	// coordinates on map and screen to compute aligned positions from map to the screen
	private PointF fixedPointOnMap = new PointF();
	private Point fixedPointOnScreen = new Point();
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (tmsLayer == null) {
			return false;
		}
		float x = event.getX();
		float y = height-event.getY();
		
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
				isMooving = true;
				centerAtDragStart = new PointF(center.x, center.y);
				dragStart = screenToMap(x, y);
				dragStartPx = new PointF(x, y);
				lastPos = screenToMap(x, y);
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
				}, 30, 60);
				
				break;
			case MotionEvent.ACTION_UP:
				animTimer.cancel();
				animTimer = new Timer();
				isMooving = false;
				break;
			case MotionEvent.ACTION_MOVE:
				PointF dragPos2 = screenToMap2(x, y);
				Matrix m = new Matrix();
				m.postRotate(heading, lastPos.x, lastPos.y);
				float[] pos = new float[] {dragPos2.x, dragPos2.y};
				m.mapPoints(pos);
				//center.set(pos[0], pos[1]);
				center.offset(lastPos.x-pos[0], lastPos.y-pos[1]);
				//center.offset(lastPos.x-dragPos2.x, lastPos.y-dragPos2.y);
				lastPos = dragPos2;
				if (true) break;
				
				PointF dragPos = screenToMap2(x, y);
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
				 * (dragStartPx.x-x)*getResolution(),
				 * (dragStartPx.y-y)*getResolution()));
				 */
				center.set(centerAtDragStart);
				m.reset();
				m.postRotate(heading, dragStart.x, dragStart.y);
				pos = new float[] {dragPos.x, dragPos.y};
				m.mapPoints(pos);
				center.offset(dragStart.x - pos[0], dragStart.y - pos[1]);
				//center.offset(dragStart.x - dragPos.x, dragStart.y - dragPos.y);
				//invalidate();
				break;
			}
		}
		// Utils.dumpEvent(event);
		return true;
	}

	List<Tile> neededTiles = new ArrayList<Layer.Tile>();
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);
		if (tmsLayer == null) {
			return;
		}
		//canvas.scale(1, -1, width / 2f, height / 2f);
		canvas.scale(1, -1);
		canvas.translate(0, -height);
		float scale = 0.5f; 
		//canvas.scale(scale, scale, width / 2f, height / 2f);
		//canvas.rotate(-heading, width/2f, height/2f);
		
		//canvas.drawLine(0, 0, 20, 10, screenBorderStyle);

		PointF startP = mapToScreen(bbox.minX, bbox.minY);
		PointF endP = mapToScreen(bbox.maxX, bbox.maxY);
		//canvas.drawArc(new RectF(startP.x-2, startP.y-2, startP.x+2, startP.y+2), 0, 360, true, screenBorderStyle);
		canvas.drawRect(startP.x, startP.y+1, endP.x, endP.y-1, mapStyle);

		//canvas.drawRect(startP.x, startP.y, startP.x+256, startP.y+256, screenBorderStyle);
		// Log.d(TAG, format("first tile [%f, %f]", startP.x, startP.y));
		
		PointF o = screenToMap(0, 0);
		if (o.x > bbox.maxX || o.y > bbox.maxY) {
			return;
		}
		Point s = getTileAtScreen(0, 0);
		Point e = getTileAtScreen(width, height);
		//Point s = getTileAtScreen(-(size-width)/2, -(size-height)/2);
		//Point e = getTileAtScreen(width+(size-width)/2, height+(size-height)/2);
		
		//Log.i(TAG, format("left-top tile: [%d, %d] right-bottom tile: [%d, %d]", s.x, s.y, e.x, e.y));
		int lastTileX = (int) ((bbox.maxX - bbox.minX) / tileWidth);
		int lastTileY = (int) ((bbox.maxY - bbox.minY) / tileHeight);

		lastTileX = e.x < lastTileX ? e.x : lastTileX;
		lastTileY = e.y < lastTileY ? e.y : lastTileY;
		int firstTileX = s.x > 0 ? s.x : 0;
		int firstTileY = s.y > 0 ? s.y : 0;
		
		fixedPointOnMap.x = bbox.minX + tileWidth * firstTileX;
		fixedPointOnMap.y = bbox.minY + tileHeight * firstTileY;
		PointF p = mapToScreen(fixedPointOnMap.x, fixedPointOnMap.y);
		fixedPointOnScreen.x = (int) Math.round(p.x);
		fixedPointOnScreen.y = (int) Math.round(p.y);
		
		neededTiles.clear();
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
					tiles.put(tileKey, tile);
					neededTiles.add(tile);
				}
				if (tile.getImage() != null) {
					float left = fixedPointOnScreen.x+(256*(x-firstTileX));
					float bottom = fixedPointOnScreen.y+(y-firstTileY)*256;
					canvas.scale(1, -1, left, bottom+128);
					canvas.drawBitmap(tile.getImage(), left, bottom, imagesStyle);
					//visualDebugger.drawTile(canvas, x, y);
					canvas.scale(1, -1, left, bottom+128);
				}
			}
		}
		//System.out.println("rendering time: "+(System.currentTimeMillis()-t1));
		t1 = System.currentTimeMillis();
		//for (Tile tile : neededTiles) {
			//tmsLayer.requestTile(tile);
			//tiles.put(tileKey(tile.getX(), tile.getY()), tile);
		//}
		if (neededTiles.size() > 0) {
			tmsLayer.requestTiles(neededTiles);
		}
		/*
		for (int x = firstTileX; x <= lastTileX; x++) {
			for (int y = firstTileY; y <= lastTileY; y++) {
				String tileKey = tileKey(x, y);
				if (!tiles.containsKey(tileKey)) {
					//tmsLayer.requestTile(zoom, x, y, tileWidthPx, tileHeightPx);
					Tile tile = new Tile(x, y, zoom, null);
					//System.out.println("need a tile");
					tmsLayer.requestTile(tile);
					tiles.put(tileKey, tile);
				}
			}
		}
		*/
		System.out.println("requesting time: "+(System.currentTimeMillis()-t1));
		
		Point2D p2 = new Point2D();
		tmsLayer.getProjection().transform(new Point2D(21.23886386, 49.00096926), p2);
		//Log.i(TAG, format("projected position: [%f, %f]", p2.x, p2.y));
		PointF currentPos = mapToScreenAligned((float) p2.x, (float) p2.y);
		canvas.drawArc(new RectF(currentPos.x-2, currentPos.y-2, currentPos.x+2, currentPos.y+2), 0, 360, true, screenBorderStyle);
		/*
		canvas.drawRect(0, 0, width, height, screenBorderStyle2);
		canvas.drawArc(new RectF(-3, -3, 3, 3), 0, 360, true, screenBorderStyle2);
		
		canvas.rotate(heading, width/2f, height/2f);
		canvas.drawRect(0, 0, width, height, screenBorderStyle);
		*/
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
		float offsetY = y - height / 2f;
		return new PointF(centerAtDragStart.x + offsetX * getResolution(), centerAtDragStart.y
				+ offsetY * getResolution());
	}

	public final PointF screenToMap(float x, float y) {
		/*
		Matrix m = new Matrix();
		m.postRotate(heading, width/2f, height/2f);
		float[] pos = new float[] {x, y};
		m.mapPoints(pos);
		float offsetX = pos[0] - width / 2f;
		float offsetY = pos[1] - height / 2f;
		*/
		float offsetX = x - width / 2f;
		float offsetY = y - height / 2f;
		return new PointF(center.x + offsetX * getResolution(), center.y + offsetY
				* getResolution());
	}

	public final PointF mapToScreen(float x, float y) {
		float offsetX = x - center.x;
		float offsetY = y - center.y;
		return new PointF(width / 2f + offsetX / getResolution(), height / 2f + offsetY
				/ getResolution());
	}

	public final PointF mapToScreenAligned(float x, float y) {
		float positionOffsetX = x - fixedPointOnMap.x;
		float positionOffsetY = y - fixedPointOnMap.y;
		return new PointF(fixedPointOnScreen.x+positionOffsetX/getResolution(), fixedPointOnScreen.y+positionOffsetY/getResolution());
	}
	
	public final float getResolution() {
		return (float) tmsLayer.getResolutions()[zoom-1];
	}

	@Override
	public void onTileLoad(Tile tile) {
		// throw away tiles with not actual zoom level (delayed)
		Log.i(TAG, "onTileLoad: "+tile);
		if (tile.getZoomLevel() != zoom) {
			tile.recycle();
			return;
		}
		String tileKey = tileKey(tile.getX(), tile.getY());
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
					tiles.remove(tileKey(mostFarAway.getX(), mostFarAway.getY()));
				}
			}
			tiles.put(tileKey, tile);
		}
		//if (! isMooving) {
			post(new Runnable() {
				
				@Override
				public void run() {
					invalidate();
				}
			});
		//}
	}

	@Override
	public void onTileLoadingFailed(Tile tile) {
		String tileKey = tileKey(tile.getX(), tile.getY());
		Log.w(TAG, "onTileLoadingFailed: "+tile);
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
