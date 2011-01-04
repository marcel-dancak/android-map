package sk.gista.android.maps;

import java.util.ArrayList;
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
import sk.gista.android.maps.MapEventsGenerator.MapControlListener;
import sk.gista.android.utils.CustomAnimation;
import sk.gista.android.utils.TmsVisualDebugger;
import sk.gista.android.utils.Utils;
import sk.gista.android.utils.CustomAnimation.CompositeAnimation;

public class Map extends View implements TileListener, MapView, MapControlListener {

	private static String TAG = Map.class.getSimpleName();

	private PointF center;
	private BBox bbox;
	private int zoomLevel = 1;
	private int heading;
	
	private float tileWidth;
	private float tileHeight;

	// size of screen (map) in pixels
	private int width;
	private int height;

	// coordinates on map and screen to compute aligned positions from map to the screen
	private PointF fixedPointOnMap = new PointF();
	private PointF fixedPointOnScreen = new PointF();
	
	private Point firstVisibleTile = new Point();
	private Point lastVisibleTile = new Point();
	
	// zoom
	private float zoomPinch = 1f;
	private Bitmap zoomBackground;
	private boolean showZoomBackground;
	
	private PointF zoomBgStart = new PointF();
	//private Matrix overlayMatrix;
	
	private TmsLayer tmsLayer;
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
	private boolean isPeriodicallyRedrawing;
	
	private MapListener mapListener;
	private MapEventsGenerator mapEventsGenerator;
	
	private boolean dirty;
	private int size;
	
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
		mapEventsGenerator = new MapEventsGenerator(this);
		mapEventsGenerator.setMapControlListener(this);
	}
	
	public void setLayer(TmsLayer layer) {
		if (tmsLayer != layer) {
			
			if (tilesManager != null) {
				tilesManager.cancelAll();
				tilesManager.clearCache();
			}
			tmsLayer = layer;
			if (layer != null) {
				tilesManager = new TilesManager(this);
				tilesManager.addTileListener(this);
				bbox = layer.getBoundingBox();
				center = new PointF((bbox.minX + bbox.maxX) / 2f, (bbox.minY + bbox.maxY) / 2f);
		
				visualDebugger = new TmsVisualDebugger(this);
				//setZoom(1);
				onZoomChange(zoomLevel, zoomLevel);
			}
			mapListener.onLayerChanged(layer);
		}
	}
	
	public int getZoom() {
		return zoomLevel;
	}
	
	public void zoomTo(final int zoom) {
		if (this.zoomLevel == zoom) {
			return;
		}
		if (zoom >= 0 && zoom < tmsLayer.getResolutions().length) {
			ZoomAnimation animation = new ZoomAnimation(zoom);
			animation.setDuration(500);
			animation.setFramesCount(8);
			animation.setView(this);
			animation.start();
		}
	}
	
	public void setZoom(int zoom) {
		if (this.zoomLevel == zoom) {
			return;
		}
		if (zoom >= 0 && zoom < tmsLayer.getResolutions().length) {
			int oldZoom = this.zoomLevel;
			this.zoomLevel = zoom;
			onZoomChange(oldZoom, zoom);
			if (mapListener != null) {
				mapListener.onZoomChanged(zoom);
			}
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
			tilesManager.clearCache();
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, format("width: %d height: %d", w, h));
		//Log.i(TAG, "Cached tiles: "+tilesCache.size());
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
		tilesManager.clearCache();
		
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
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mapEventsGenerator.onTouchEvent(event);
	}

	PointF curAlign = new PointF();
	PointF nextAlign = new PointF();
	float compX;
	float compY;
	int count;
	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i(TAG, "redrawing");
		canvas.drawRGB(255, 255, 255);
		if (showZoomBackground) {
			//Log.i(TAG, "drawing background");
			canvas.drawBitmap(zoomBackground, (zoomBgStart.x-center.x)/getResolution(), -(zoomBgStart.y-center.y)/getResolution(), null);
		}
		if (tmsLayer == null) {
			return; // TODO: and what about overlays ?
		}
		
		PointF o = screenToMap(0, 0);
		if (o.x > bbox.maxX || o.y > bbox.maxY) {
			return; // TODO: and what about overlays ?
		}
		
		//canvas.scale(1, -1, width / 2f, height / 2f);
		canvas.scale(1, -1);
		canvas.translate(0, -height);
		float scale = 0.5f;
		canvas.save();
		
		//if (dirty) {
			validateMap();
		//}
		count++;
		//xCompensation = 0.49f*(float) Math.sin(count);
		//Log.i(TAG, "x compensation = "+xCompensation);
		//canvas.scale(zoomPinch, zoomPinch, (width+xCompensation)/2f, height/2f);
		canvas.scale(zoomPinch, zoomPinch, width/2f, height/2f);
		PointF ca = mapToScreenAligned(center.x, center.y);
		//Log.i(TAG, "aligned center: "+ca.x+", "+ca.y +" compensation x="+xCompensation);
		
		
		//compX *= zoomPinch;
		if (zoomPinch == 1f) {
			compX=0;
			compY=0;
		} else {
			compX = curAlign.x+(nextAlign.x-curAlign.x)*((zoomPinch-1)/2f);
			compY = curAlign.y+(nextAlign.y-curAlign.y)*((zoomPinch-1)/2f);
			Log.i(TAG, "compX="+compX + " compY="+compY);
		}
		//canvas.scale(zoomPinch, zoomPinch, (width+compX)/2f, height/2f);
		//canvas.scale(scale, scale, width / 2f, height / 2f);
		//canvas.rotate(-heading, width/2f, height/2f);
		
		
		
		long t1 = System.currentTimeMillis();
		//Log.i(TAG, format("bbox=%f, %f, %f, %f center=%f, %f", bbox.minX, bbox.minY, bbox.maxX, bbox.maxY, center.x, center.y));
		//Log.i(TAG, format("firstTileX=%d firstTileY=%d", firstTileX, firstTileY));
		// TODO: check that firstTileX/Y and lastTileX/Y aren't too high (when onZoomChange() or something like that
		// wasn't called)
		int notAvailableTiles = 0;
		for (int x = firstVisibleTile.x; x <= lastVisibleTile.x; x++) {
			for (int y = firstVisibleTile.y; y <= lastVisibleTile.y; y++) {
				Tile tile = null;
				if (zoomPinch == 1f) {
					tile = tilesManager.getTile(x, y);
				} else if (tilesManager.hasInCache(x, y)) {
					tile = tilesManager.getTile(x, y);
				}
				if (tile != null && tile.getImage() != null) {
					float left = fixedPointOnScreen.x+(256*(x-firstVisibleTile.x));
					float bottom = fixedPointOnScreen.y+(y-firstVisibleTile.y)*256;
					if (showZoomBackground) {
						canvas.drawRect(left, bottom, left+256, bottom+256, whiteStyle);
					}
					canvas.scale(1, -1, left, bottom+128);
					canvas.drawBitmap(tile.getImage(), left, bottom, imagesStyle);
					//visualDebugger.drawTile(canvas, x, y);
					canvas.scale(1, -1, left, bottom+128);
				} else if (zoomPinch == 1f) {
					//neededTiles.add(tile);
					notAvailableTiles++;
				}
			}
		}
		if (notAvailableTiles == 0) {
			showZoomBackground = false;
		}
		
		//Log.i("DEBUG", "zoom: "+zoom+ " resolution: "+getResolution()+" start.x "+startP.x+" end.x "+endP.x);
		if (drawOverlays) {
			PointF startP = mapToScreen(bbox.minX, bbox.minY);
			PointF endP = mapToScreen(bbox.maxX, bbox.maxY);
			
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
	
	private float xCompensation;
	private float yCompensation;
	
	private void validateMap() {
		zoomLevel +=1;
		tileWidth = tmsLayer.getTileWidth() * getResolution();
		tileHeight = tmsLayer.getTileHeight() * getResolution();
		validateMap2();
		nextAlign.x = xCompensation;
		nextAlign.y = yCompensation;
		zoomLevel -=1;
		tileWidth = tmsLayer.getTileWidth() * getResolution();
		tileHeight = tmsLayer.getTileHeight() * getResolution();
		validateMap2();
		curAlign.x = xCompensation;
		curAlign.y = yCompensation;
		if (zoomPinch != 1f) {
			Log.i(TAG, "Current Alignment x="+curAlign.x+" y="+curAlign.y+" Next Alignment x="+nextAlign.x+" y="+nextAlign.y);
		}
	}
	
	private void validateMap2() {
		PointF o = screenToMap(0, 0);
		if (o.x > bbox.maxX || o.y > bbox.maxY) {
			return; // TODO: and what about overlays ?
		}
		Point s = getTileAtScreen(0, 0);
		Point e = getTileAtScreen(width, height);
		
		firstVisibleTile.x = s.x > 0 ? s.x : 0;
		firstVisibleTile.y = s.y > 0 ? s.y : 0;
		
		//Log.i(TAG, format("left-top tile: [%d, %d] right-bottom tile: [%d, %d]", s.x, s.y, e.x, e.y));
		lastVisibleTile.x = (int) ((bbox.maxX - bbox.minX) / tileWidth);
		lastVisibleTile.y = (int) ((bbox.maxY - bbox.minY) / tileHeight);

		lastVisibleTile.x = e.x < lastVisibleTile.x ? e.x : lastVisibleTile.x;
		lastVisibleTile.y = e.y < lastVisibleTile.y ? e.y : lastVisibleTile.y;
		
		fixedPointOnMap.x = bbox.minX + tileWidth * firstVisibleTile.x;
		fixedPointOnMap.y = bbox.minY + tileHeight * firstVisibleTile.y;
		PointF p = mapToScreen(fixedPointOnMap.x, fixedPointOnMap.y);
		//fixedPointOnScreen.x = Math.round(p.x);
		//fixedPointOnScreen.y = Math.round(p.y);
		fixedPointOnScreen.x = p.x;
		fixedPointOnScreen.y = p.y;
		//xCompensation = Math.round(p.x)-p.x;
		//yCompensation = Math.round(p.y)-p.y;
		xCompensation = ((int)p.x)-p.x;
		yCompensation = ((int)p.y)-p.y;
		//Log.i(TAG, "Center: "+center.x+", "+center.y);
	}
	
	private Point getTileAtScreen(int x, int y) {
		assert x <= width && y <= height : "point outside the screen";
		PointF mapPos = screenToMap(x, y);
		//float tileX = (mapPos.x - bbox.minX) / tileWidth;
		//float tileY = (mapPos.y - bbox.minY) / tileHeight;
		//return new Point((int) Math.floor(tileX), (int) Math.floor(tileY));
		return tmsLayer.getTileAt(mapPos, zoomLevel);
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
		float offsetX = x - center.x;
		float offsetY = y - center.y;
		return new PointF( (width / 2f) + (offsetX*zoomPinch / getResolution()), height / 2f + offsetY*zoomPinch
				/ getResolution());
	}
	
	public final PointF mapToScreenAligned2(float x, float y) {
		float positionOffsetX = x - fixedPointOnMap.x;
		float positionOffsetY = y - fixedPointOnMap.y;
		float tx = fixedPointOnScreen.x+positionOffsetX/getResolution();
		float ty = fixedPointOnScreen.y+positionOffsetY/getResolution();
		Matrix m = new Matrix();
		m.postScale(zoomPinch, zoomPinch, width/2f, height/2f);
		float[] pos = new float[] {tx, ty};
		m.mapPoints(pos);
		return new PointF(pos[0]-compX, pos[1]);
	}
	
	public final float getResolution() {
		return (float) tmsLayer.getResolutions()[zoomLevel];
	}

	@Override
	public void onTileLoad(Tile tile) {
		// throw away tiles with not actual zoom level (delayed)
		//Log.i(TAG, "onTileLoad: "+tile);
		if (! isPeriodicallyRedrawing) {
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
		Log.w(TAG, "onTileLoadingFailed: "+tile);
	}
	
	private int getClosestZoomLevel(double newZoom) {
		double newResolution = tmsLayer.getResolutions()[zoomLevel]/newZoom;
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
		int closestZoomLevel = getClosestZoomLevel(zoomPinch);
		ZoomAnimation animation = new ZoomAnimation(closestZoomLevel);
		animation.setDuration(500);
		animation.setFramesCount(8);
		animation.setView(this);
		animation.start();
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
		int screenDistance = (int) (Utils.distance(x, y, center.x, center.y)/getResolution());
		//Log.i(TAG, "distance: "+screenDistance+" px");
		int maxAnimDistance = 2 * (int) Math.sqrt(width*width+height*height);
		
		if (screenDistance < maxAnimDistance) {
			float fraction = (screenDistance/(float) maxAnimDistance);
			MoveAnimation animation = new MoveAnimation(x, y);
			animation.setDuration(100+(int) (500*fraction));
			animation.setFramesCount(2+(int)(10*fraction));
			animation.setView(this);
			animation.start();
		} else {
			center.x = x;
			center.y = y;
			invalidate();
		}
	}
	
	private void moveAndZoom(final float x, final float y, final int zoom) {
		MoveAnimation moveAnim = new MoveAnimation(x, y);
		ZoomAnimation zoomAnim = new ZoomAnimation(zoom);
		CompositeAnimation animation = new CompositeAnimation(350, 6);
		animation.addAnimation(moveAnim);
		animation.addAnimation(zoomAnim);
		
		animation.setView(this);
		animation.start();
	}

	@Override
	public void redraw() {
		invalidate();
	}

	@Override
	public void setOnZoomChangeListener(MapListener listener) {
		this.mapListener = listener;
	}
	

	@Override
	public void onTapStart() {
		startPeriodicalRedrawing();
	}

	public void onTapEnd() {
		stopPeriodicalRedrawing();
		invalidate();
	}
	
	@Override
	public void onMove(float x, float y) {
		setCenter(x, y);
	}

	@Override
	public void onZoom(float zoom) {
		zoomPinch = zoom;
	}
	
	@Override
	public void onZoomEnd() {
		onZoomPinchEnd();
	}

	@Override
	public void onDoubleTap(float x, float y) {
		PointF pos = screenToMap(x, y);
		int newZoom = zoomLevel + 1 < tmsLayer.getResolutions().length? zoomLevel + 1 : zoomLevel;
		moveAndZoom(pos.x , pos.y, newZoom);
	}
	
	private final void startPeriodicalRedrawing() {
		if (isPeriodicallyRedrawing) {
			throw new IllegalStateException();
		}
		isPeriodicallyRedrawing = true;
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
		}, 40, 80);
	}
	
	private final void stopPeriodicalRedrawing() {
		isPeriodicallyRedrawing = false;
		animTimer.cancel();
		animTimer = new Timer();
	}
	
	
	class MoveAnimation extends CustomAnimation {

		private float startX;
		private float startY;
		private float x;
		private float y;
		
		public MoveAnimation(float x, float y) {
			this.x = x;
			this.y = y;
			startX = center.x;
			startY = center.y;
		}
		
		@Override
		public void onFrame(float fraction) {
			center.x = startX + (x-startX)*fraction;
			center.y = startY + (y-startY)*fraction;
		}

		@Override
		public void onEnd() {}
	}
	
	class ZoomAnimation extends CustomAnimation {

		private int zoom;
		private float endZoomPinch;
		private float startZoomPinch;
		
		public ZoomAnimation(int zoom) {
			this.zoom = zoom;
			endZoomPinch = getResolution() / (float) getLayer().getResolutions()[zoom];
			startZoomPinch = zoomPinch;
		}
		
		@Override
		public void onFrame(final float fraction) {
			zoomPinch = startZoomPinch + (endZoomPinch-startZoomPinch)*fraction;
		}
		
		@Override
		public void onEnd() {
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
			onDraw(canvas);
			drawOverlays = true;
			drawGraphicalScale = true;
			
			zoomPinch = 1f;
			if (Map.this.zoomLevel != zoom) {
				showZoomBackground = true;
			}
			setZoom(zoom);
		}
	}
}