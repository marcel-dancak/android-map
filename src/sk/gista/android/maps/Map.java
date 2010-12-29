package sk.gista.android.maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import static java.lang.String.format;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import sk.gista.android.maps.Layer.Tile;
import sk.gista.android.maps.Layer.TileListener;
import sk.gista.android.utils.MyAnimation;
import sk.gista.android.utils.TmsVisualDebugger;
import sk.gista.android.utils.Utils;

public class Map extends View implements TileListener, MapView {

	private static String TAG = Map.class.getName();

	private PointF center;
	private BBox bbox;
	private int zoom = 1;
	private int heading;
	
	private float tileWidth;
	private float tileHeight;

	// size of screen (map) in pixels
	private int width;
	private int height;

	//private List<Layer> layers = Collections.emptyList();
	private TmsLayer tmsLayer;
	private java.util.Map<String, Tile> tiles = new HashMap<String, Tile>();
	private TilesManager tilesManager;
	private List<Overlay> overlays;
	
	private boolean drawOverlays = true;
	private boolean drawGraphicalScale = true;
	
	// drawing styles
	private Paint imagesStyle;
	private Paint mapStyle;
	private Paint screenBorderStyle;
	private Paint screenBorderStyle2;
	private Paint whiteStyle;
	private Paint scaleStyle;
	
	private float scaleWidth = 141.73236f;
	private String scaleText;
	
	private Timer animTimer = new Timer();
	private TmsVisualDebugger visualDebugger;
	private boolean isMooving;
	
	public Map(Context context) {
		super(context);
		postInit();
	}
	
	public Map(Context context, AttributeSet attrs) {
		super(context, attrs);
		postInit();
	}
	
	private void postInit() {
		imagesStyle = new Paint();
		imagesStyle.setFilterBitmap(true);
		
		mapStyle = new Paint();
		mapStyle.setStrokeWidth(2f);
		mapStyle.setStyle(Paint.Style.STROKE);
		mapStyle.setColor(Color.argb(255, 0, 0, 0));

		screenBorderStyle = new Paint();
		screenBorderStyle.setStyle(Paint.Style.STROKE);
		screenBorderStyle.setStrokeWidth(2f);
		screenBorderStyle.setAntiAlias(true);
		screenBorderStyle.setColor(Color.argb(255, 20, 40, 120));

		screenBorderStyle2 = new Paint(screenBorderStyle);
		screenBorderStyle2.setColor(Color.rgb(150, 30, 50));
		
		whiteStyle = new Paint();
		whiteStyle.setColor(Color.WHITE);
		
		scaleStyle = new Paint();
		scaleStyle.setFakeBoldText(true);
		scaleStyle.setAntiAlias(true);
		
		overlays = new ArrayList<Overlay>(1);
	}
	
	public void setLayer(TmsLayer layer) {
		if (this.tmsLayer != layer) {
			clearTiles();
		}
		if (tilesManager != null) {
			tilesManager.cancelAll();
		}
		tmsLayer = layer;
		if (layer != null) {
			tilesManager = new TilesManager(layer);
			tilesManager.addTileListener(this);
			bbox = layer.getBoundingBox();
			center = new PointF((bbox.minX + bbox.maxX) / 2f, (bbox.minY + bbox.maxY) / 2f);
	
			visualDebugger = new TmsVisualDebugger(this);
			//setZoom(1);
			onZoomChange(zoom, zoom);
		}
	}
	
	public int getZoom() {
		return zoom;
	}
	
	public void zoomTo(final int zoom) {
		if (this.zoom == zoom) {
			return;
		}
		if (zoom >= 0 && zoom < tmsLayer.getResolutions().length) {
			final float endZoomPinch = getResolution() / (float) getLayer().getResolutions()[zoom];
			MyAnimation animation = new MyAnimation(350, 6);
			animation.onAnimationStep(new MyAnimation.MyAnimationListener() {
				
				@Override
				public void onFrame(final float fraction) {
					//Log.i(TAG, "fraction: "+fraction);
					post(new Runnable() {
						
						@Override
						public void run() {
							zoomPinch = 1f + (endZoomPinch-1f)*fraction;
							post(new Runnable() {
								
								@Override
								public void run() {
									invalidate();
								}
							});
						}
					});
				}
				
				@Override
				public void onEnd() {
					
					post(new Runnable() {
						
						@Override
						public void run() {
							if (zoomBackground == null) {
								Log.i(TAG, "createBackgroundImage "+width+" x "+height);
								zoomBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
							}
							
							Canvas canvas = new Canvas(zoomBackground);
							zoomBgStart.x = center.x;
							zoomBgStart.y = center.y;
							showZoomBackground = false;
							drawOverlays = false;
							drawGraphicalScale = false;
							//Log.i(TAG, "**** Drawing ZOOM BACKGROUND ****");
							onDraw(canvas);
							drawOverlays = true;
							drawGraphicalScale = true;
							
							zoomPinch = 1f;
							if (Map.this.zoom != zoom) {
								showZoomBackground = true;
							}
							setZoom(zoom);
						}
					});
				}
			});
			animation.start();
		}
	}
	
	public void setZoom(int zoom) {
		if (this.zoom == zoom) {
			return;
		}
		if (zoom >= 0 && zoom < tmsLayer.getResolutions().length) {
			int oldZoom = this.zoom;
			this.zoom = zoom;
			onZoomChange(oldZoom, zoom);
			invalidate();
		}
	}

	public PointF getCenter() {
		return center;
	}
	
	public void setCenter(float x, float y) {
		if (center != null) {
			center.x = x;
			center.y = y;
		} else {
			center = new PointF(x, y);
		}
	}
	
	public void recycle() {
		if (tilesManager != null) {
			tilesManager.cancelAll();
		}
		clearTiles();
	}
	
	private int size;
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, format("width: %d height: %d", w, h));
		Log.i(TAG, "Cached tiles: "+tiles.size());
		if (zoomBackground != null) {
			zoomBackground.recycle();
			zoomBackground = null;
		}
		size = (int) Math.ceil(Math.sqrt(w*w+h*h));
		width = w;
		height = h;
		//clearTiles();
	}

	protected void onZoomChange(int oldZoom, int zoom) {
		tileWidth = tmsLayer.getTileWidth() * getResolution();
		tileHeight = tmsLayer.getTileHeight() * getResolution();
		tilesManager.cancelAll();
		clearTiles();
		
		//double factor = 39.3701; // meters
		//double scale = getResolution()* 72.0 * factor;
		//scale = scale/20.0;
		double scale = scaleWidth*getResolution();
		String scaleUnitText;
		if (scale >= 1000) {
			scale /= 1000.0;
			scaleUnitText = "km";
		} else {
			scaleUnitText = "m";
		}
		scaleText = format("%.2f %s", scale, scaleUnitText);
	}

	public TmsLayer getLayer() {
		return tmsLayer;
	}
	
	private void clearTiles() {
		Log.i(TAG, "Clearing tiles");
		synchronized (tiles) {
			for (Tile tile : tiles.values()) {
				tile.recycle();
			}
			tiles.clear();
		}
	}
	
	private PointF centerAtDragStart;
	private PointF dragStart;

	private PointF lastPos;
	private PointF dragStartPx;

	// coordinates on map and screen to compute aligned positions from map to the screen
	private PointF fixedPointOnMap = new PointF();
	private Point fixedPointOnScreen = new Point();
	
	// zoom
	private boolean wasZoom;
	private float startDistance;
	private float zoomPinch = 1f;
	private Bitmap zoomBackground;
	private boolean showZoomBackground;
	
	private PointF zoomBgStart = new PointF();
	
	private long lastTouchTime;
	//private Matrix overlayMatrix;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (tmsLayer == null) {
			return false;
		}
		float x = event.getX(0);
		float y = height-event.getY(0);
		
		long curTime = System.currentTimeMillis();
		
		
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		
		switch (action) {
		case MotionEvent.ACTION_POINTER_DOWN:
			wasZoom = true;
			//startPoint1 = new PointF(x, y);
			//startPoint2 = new PointF(event.getX(1), height-event.getY(1));
			//startPoint1.length(event.getX(1), height-event.getY(1));
			float x1 = event.getX(0);
			float y1 = event.getY(0);
			float x2 = event.getX(1);
			float y2 = event.getY(1);
			//startDistance = Utils.distance(x, y, event.getX(1), height-event.getY(1));
			startDistance = Utils.distance(x1, y1, x2, y2);
			//Log.i(TAG, "2 Fingers, start distance: "+startDistance);
			break;
		case MotionEvent.ACTION_POINTER_UP:
			//Log.i(TAG, "1 Finger");
			onZoomPinchEnd();
			
			//wasZoom = false;
			break;
		case MotionEvent.ACTION_DOWN:
			//Log.i(TAG, "delta: "+(curTime - lastTouchTime));
			if (curTime - lastTouchTime > 40 && curTime - lastTouchTime < 150) {
				Log.i(TAG, "Double click!");
				PointF pos = screenToMap(x, y);
				int newZoom = zoom + 1 < tmsLayer.getResolutions().length? zoom + 1 : zoom;
				moveAndZoom(pos.x , pos.y, newZoom);
			} else {
				//wasZoom = false;
				//showZoomBackground = false;
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
			}
			break;
		case MotionEvent.ACTION_UP:
			// for the case when event with ACTION_POINTER_UP action didn't occurred
			if (wasZoom) {
				// TODO: check that tmsLayer is not null
				//onZoomPinchEnd();
			}
			animTimer.cancel();
			animTimer = new Timer();
			isMooving = false;
			wasZoom = false;
			lastTouchTime = curTime;
			break;
		case MotionEvent.ACTION_MOVE:
			//Log.i(TAG, "ACTION_MOVE "+event.getPointerCount());
			if (!wasZoom && event.getPointerCount() == 1) {
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
			} else {
				x1 = event.getX(0);
				y1 = event.getY(0);
				x2 = event.getX(1);
				y2 = event.getY(1);
				//Log.i(TAG, "p1="+x1+","+y1+" p2="+x2+","+y2);
				float distance = Utils.distance(x1, y1, x2, y2);
				float possibleZoomPinch = distance/startDistance;
				if (possibleZoomPinch > 1 && zoom < tmsLayer.getResolutions().length -1) {
					zoomPinch = possibleZoomPinch;
				}
				if (possibleZoomPinch < 1 && zoom > 0) {
					zoomPinch = possibleZoomPinch;
				}
				//Log.i(TAG, "2 Fingers, distance: "+distance + " zoom "+zoomPinch);
			}
			break;
		}
		//Utils.dumpEvent(event);
		return true;
	}

	List<Tile> neededTiles = new ArrayList<Layer.Tile>();
	
	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i(TAG, "zoomPinch="+zoomPinch);
		canvas.drawRGB(255, 255, 255);
		if (showZoomBackground) {
			//Log.i(TAG, "drawing background");
			canvas.drawBitmap(zoomBackground, (zoomBgStart.x-center.x)/getResolution(), -(zoomBgStart.y-center.y)/getResolution(), null);
			//return;
		}
		if (tmsLayer == null) {
			return; // TODO: and what about overlays ?
		}
		
		//canvas.scale(1, -1, width / 2f, height / 2f);
		canvas.scale(1, -1);
		canvas.translate(0, -height);
		float scale = 0.5f;
		canvas.save();
		canvas.scale(zoomPinch, zoomPinch, width / 2f, height / 2f);
		//canvas.scale(scale, scale, width / 2f, height / 2f);
		//canvas.rotate(-heading, width/2f, height/2f);
		
		PointF startP = mapToScreen(bbox.minX, bbox.minY);
		PointF endP = mapToScreen(bbox.maxX, bbox.maxY);
		//canvas.drawArc(new RectF(startP.x-2, startP.y-2, startP.x+2, startP.y+2), 0, 360, true, screenBorderStyle);
		
		PointF o = screenToMap(0, 0);
		if (o.x > bbox.maxX || o.y > bbox.maxY) {
			return; // TODO: and what about overlays ?
		}
		Point s = getTileAtScreen(0, 0);
		Point e = getTileAtScreen(width, height);
		
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
		
		//Log.i(TAG, format("bbox=%f, %f, %f, %f center=%f, %f", bbox.minX, bbox.minY, bbox.maxX, bbox.maxY, center.x, center.y));
		//Log.i(TAG, format("firstTileX=%d firstTileY=%d", firstTileX, firstTileY));
		// TODO: check that firstTileX/Y and lastTileX/Y aren't too high (when onZoomChange() or something like that
		// wasn't called)
		int notAvailableTiles = 0;
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
					if (zoomPinch == 1f) {
						neededTiles.add(tile);
					}
				}
				if (tile.getImage() != null) {
					float left = fixedPointOnScreen.x+(256*(x-firstTileX));
					float bottom = fixedPointOnScreen.y+(y-firstTileY)*256;
					if (showZoomBackground) {
						canvas.drawRect(left, bottom, left+256, bottom+256, whiteStyle);
					}
					canvas.scale(1, -1, left, bottom+128);
					canvas.drawBitmap(tile.getImage(), left, bottom, imagesStyle);
					//visualDebugger.drawTile(canvas, x, y);
					canvas.scale(1, -1, left, bottom+128);
				} else {
					notAvailableTiles++;
				}
			}
		}
		if (notAvailableTiles == 0) {
			showZoomBackground = false;
		}
		//System.out.println("rendering time: "+(System.currentTimeMillis()-t1));
		//t1 = System.currentTimeMillis();
		if (neededTiles.size() > 0) {
			tilesManager.requestTiles(neededTiles);
		}
		//System.out.println("requesting time: "+(System.currentTimeMillis()-t1));
		
		//Log.i("DEBUG", "zoom: "+zoom+ " resolution: "+getResolution()+" start.x "+startP.x+" end.x "+endP.x);
		if (drawOverlays) {
			mapStyle.setStrokeWidth(2f/zoomPinch);
			canvas.drawRect(startP.x, startP.y+1, endP.x, endP.y-1, mapStyle);
		}
		canvas.restore();
		float fadeStrength = zoomPinch;
		if (zoomPinch > 1f) {
			if (zoomPinch > 2f) {
				fadeStrength = 2f;
			}
			canvas.drawARGB((int) (100*(fadeStrength-1f)), 255, 255, 255);
		} else if (zoomPinch < 1f) {
			if (zoomPinch < 0.5f) {
				fadeStrength = 0.5f;
			}
			canvas.drawARGB((int) (100*((1f/fadeStrength) -1f)), 255, 255, 255);
		}
		
		if (drawOverlays) {
			for (Overlay overlay : overlays) {
				overlay.onDraw(this, canvas, zoomPinch);
			}
		}
		/*
		canvas.drawRect(0, 0, width, height, screenBorderStyle2);
		canvas.drawArc(new RectF(-3, -3, 3, 3), 0, 360, true, screenBorderStyle2);
		
		canvas.rotate(heading, width/2f, height/2f);
		canvas.drawRect(0, 0, width, height, screenBorderStyle);
		*/
		if (drawGraphicalScale) {
			drawGraphicalScale(canvas);
		}
	}

	
	private void drawGraphicalScale(Canvas canvas) {
		canvas.save();
		canvas.translate(0, height-24);
		scaleStyle.setColor(Color.GRAY);
		scaleStyle.setAlpha(150);
		canvas.drawRect(5, 4, 10+scaleWidth, 20, scaleStyle);
		scaleStyle.setAlpha(255);
		scaleStyle.setColor(Color.BLACK);
		canvas.drawRect(5, 4, 10+scaleWidth, 7, scaleStyle);

		canvas.scale(1, -1, 0, 9);
		canvas.drawText(scaleText, 10, 9, scaleStyle);
		canvas.restore();
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
		
		Matrix m = new Matrix();
		//m.postRotate(heading, width/2f, height/2f);
		m.postScale(1f/zoomPinch, 1f/zoomPinch, width/2f, height/2f);
		float[] pos = new float[] {x, y};
		m.mapPoints(pos);
		float offsetX = pos[0] - width / 2f;
		float offsetY = pos[1] - height / 2f;
		
		//float offsetX = x - width / 2f;
		//float offsetY = y - height / 2f;
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
		float tx = fixedPointOnScreen.x+positionOffsetX/getResolution();
		float ty = fixedPointOnScreen.y+positionOffsetY/getResolution();
		Matrix m = new Matrix();
		m.postScale(zoomPinch, zoomPinch, width/2f, height/2f);
		float[] pos = new float[] {tx, ty};
		m.mapPoints(pos);
		return new PointF(pos[0], pos[1]);
	}
	
	public final float getResolution() {
		return (float) tmsLayer.getResolutions()[zoom];
	}

	@Override
	public void onTileLoad(Tile tile) {
		// throw away tiles with not actual zoom level (delayed)
		//Log.i(TAG, "onTileLoad: "+tile);
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
		if (! isMooving) {
			post(new Runnable() {
				
				@Override
				public void run() {
					invalidate();
				}
			});
		}
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

	private int getClosestZoomLevel(double newZoom) {
		double newResolution = tmsLayer.getResolutions()[zoom]/newZoom;
		double closestResolutionDistance = tmsLayer.getResolutions()[0];
		int indexOfClosestResolution = 0;
		
		for (int i = 0 ; i < tmsLayer.getResolutions().length; i++) {
			double resolution = tmsLayer.getResolutions()[i];
			double distance = Math.abs(resolution-newResolution);
			if (distance < closestResolutionDistance) {
				closestResolutionDistance = distance;
				indexOfClosestResolution = i;
			}
		}
		return indexOfClosestResolution;
	}
	
	private void onZoomPinchEnd() {
		
		final int closestZoomLevel = getClosestZoomLevel(zoomPinch);
		final float endZoomPinch = (float) (tmsLayer.getResolutions()[zoom]/tmsLayer.getResolutions()[closestZoomLevel]);
		//Log.i(TAG, "actual zoom pinch: "+zoomPinch +" calculated: "+endZoomPinch);
		
		
		final float startZoomPinch = zoomPinch;
		MyAnimation animation = new MyAnimation(300, 5);
		animation.onAnimationStep(new MyAnimation.MyAnimationListener() {
			
			@Override
			public void onFrame(final float fraction) {
				//Log.i(TAG, "my animation "+fraction);
				post(new Runnable() {
					
					@Override
					public void run() {
						zoomPinch = startZoomPinch + (endZoomPinch-startZoomPinch)*fraction;
						invalidate();
					}
				});
			}
			
			@Override
			public void onEnd() {
				//Log.i(TAG, "onEnd");
				post(new Runnable() {
					
					@Override
					public void run() {
						if (zoomBackground == null) {
							//Log.i(TAG, "createBackgroundImage "+width+" x "+height);
							zoomBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
						}
						
						Canvas canvas = new Canvas(zoomBackground);
						zoomBgStart.x = center.x;
						zoomBgStart.y = center.y;
						showZoomBackground = false;
						drawOverlays = false;
						drawGraphicalScale = false;
						//Log.i(TAG, "**** Drawing ZOOM BACKGROUND ****");
						onDraw(canvas);
						drawOverlays = true;
						drawGraphicalScale = true;
						
						zoomPinch = 1f;
						if (zoom != closestZoomLevel) {
							showZoomBackground = true;
						}
						setZoom(closestZoomLevel);
					}
				});
			}
		});
		animation.start();
		/*
		TranslateAnimation anim = new TranslateAnimation(0, 1, 0 ,1);
		anim.setInterpolator(new LinearInterpolator());
		anim.setDuration(1000);
		anim.setRepeatCount(20);
		
		anim.setAnimationListener(new Animation.AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				Log.i(TAG, "onAnimationStart");
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				Log.i(TAG, "onAnimationRepeat");
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				Log.i(TAG, "onAnimationEnd");
			}
		});
		Log.i(TAG, "start animation ... ");
		anim.start();
		*/
	}
	
	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
	}
	
	@Override
	public void setHeading(int heading) {
		this.heading = heading;
		invalidate();
	}

	@Override
	public void onPause() {
		for (Overlay overlay : overlays) {
			overlay.onPause();
		}
	}

	@Override
	public void onResume() {
		for (Overlay overlay : overlays) {
			overlay.onResume();
		}
	}

	@Override
	public void moveToLocation(final float x, final float y) {
		final float startX = center.x;
		final float startY = center.y;
		
		int screenDistance = (int) (Utils.distance(x, y, center.x, center.y)/getResolution());
		//Log.i(TAG, "distance: "+screenDistance+" px");
		int maxAnimDistance = 2 * (int) Math.sqrt(width*width+height*height);
		
		if (screenDistance < maxAnimDistance) {
		
			float fraction = (screenDistance/(float) maxAnimDistance);
			
			MyAnimation animation = new MyAnimation(100+(int) (500*fraction), 2+(int)(10*fraction));
			animation.onAnimationStep(new MyAnimation.MyAnimationListener() {
				
				@Override
				public void onFrame(final float fraction) {
					
					post(new Runnable() {
						
						@Override
						public void run() {
							center.x = startX + (x-startX)*fraction;
							center.y = startY + (y-startY)*fraction;
							invalidate();
						}
					});
				}
				
				@Override
				public void onEnd() {
					
				}
			});
			animation.start();
		} else {
			center.x = x;
			center.y = y;
			invalidate();
		}
	}
	
	private void moveAndZoom(final float x, final float y, final int zoom) {
		final float startX = center.x;
		final float startY = center.y;
		final float endZoomPinch = getResolution() / (float) getLayer().getResolutions()[zoom];
		
		MyAnimation animation = new MyAnimation(350, 6);
		animation.onAnimationStep(new MyAnimation.MyAnimationListener() {
			
			@Override
			public void onFrame(final float fraction) {
				//Log.i(TAG, "fraction: "+fraction);
				post(new Runnable() {
					
					@Override
					public void run() {
						center.x = startX + (x-startX)*fraction;
						center.y = startY + (y-startY)*fraction;
						zoomPinch = 1f + (endZoomPinch-1f)*fraction;
						//Log.i(TAG, "Zoom pinch: "+zoomPinch);
						invalidate();
					}
				});
			}
			
			@Override
			public void onEnd() {
				
				post(new Runnable() {
					
					@Override
					public void run() {
						if (zoomBackground == null) {
							Log.i(TAG, "createBackgroundImage "+width+" x "+height);
							zoomBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
						}
						
						Canvas canvas = new Canvas(zoomBackground);
						zoomBgStart.x = center.x;
						zoomBgStart.y = center.y;
						showZoomBackground = false;
						drawOverlays = false;
						drawGraphicalScale = false;
						//Log.i(TAG, "**** Drawing ZOOM BACKGROUND ****");
						onDraw(canvas);
						drawOverlays = true;
						drawGraphicalScale = true;
						
						zoomPinch = 1f;
						if (Map.this.zoom != zoom) {
							showZoomBackground = true;
						}
						setZoom(zoom);
					}
				});
			}
		});
		animation.start();
	}

	@Override
	public void redraw() {
		invalidate();
	}
}
