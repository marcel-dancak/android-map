package sk.maps;

import java.util.Collections;
import java.util.List;
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

import sk.utils.Utils;

public class Map extends View {

	private static String TAG = Map.class.getName();

	private PointF center;
	private BBox bbox;
	private int zoom = -1;

	private float tileWidth;
	private float tileHeight;
	private int tileWidthPx = 256;
	private int tileHeightPx = 256;

	// size of map in pixels
	private int width;
	private int height;

	private List<Layer> layers = Collections.emptyList();

	private Paint mapStyle;
	private Paint tileStyle;
	private Paint screenBorderStyle;

	public Map(Context context, List<Layer> layers) {
		super(context);
		this.layers = Collections.unmodifiableList(layers);
		bbox = new BBox(-180, -90, 180, 90);
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
		screenBorderStyle.setColor(Color.argb(255, 20, 40, 120));

		buildTextTextures();
		setZoom(5);
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
			invalidate();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, format("width: %d height: %d", w, h));
		width = w;
		height = h;
	}

	protected void onZoomChange(int oldZoom, int zoom) {
		tileWidth = tileWidthPx * getResolution();
		tileHeight = tileHeightPx * getResolution();
	}

	private PointF centerAtDragStart;
	private PointF dragStart;

	private PointF lastPos;
	private PointF dragStartPx;

	@Override
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
				invalidate();
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

				invalidate();
				break;
			}
		}
		// Utils.dumpEvent(event);
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float scale = 0.5f; 
		canvas.scale(scale, scale, width / 2f, height / 2f);
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
		canvas.drawRect(startP.x, startP.y, endP.x, endP.y, mapStyle);
		/*
		 * float sx = startP.x > 0? startP.x : 0; float sy = startP.y > 0?
		 * startP.y : 0; float ex = endP.x < width? endP.x : width; float ey =
		 * endP.y < height? endP.y : height; canvas.drawRect(sx, sy, ex, ey,
		 * mapStyle);
		 */

		canvas.drawRect(1, 1, width, height, screenBorderStyle);
		// Log.d(TAG, format("first tile [%f, %f]", startP.x, startP.y));

		PointF o = screenToMap(0, 0);
		if (o.x > bbox.maxX || o.y > bbox.maxY) {
			return;
		}
		Point s = getTileAtScreen(0, 0);
		Point e = getTileAtScreen(width, height);
		// Log.i(TAG,
		// format("left-top tile: [%d, %d] right-bottom tile: [%d, %d]", s.x,
		// s.y, e.x, e.y));
		int lastTileX = (int) ((bbox.maxX - bbox.minX) / tileWidth);
		int lastTileY = (int) ((bbox.maxY - bbox.minY) / tileHeight);

		lastTileX = e.x < lastTileX ? e.x : lastTileX;
		lastTileY = e.y < lastTileY ? e.y : lastTileY;
		int firstTileX = s.x > 0 ? s.x : 0;
		int firstTileY = s.y > 0 ? s.y : 0;
		for (int i = firstTileX; i <= lastTileX; i++) {
			for (int j = firstTileY; j <= lastTileY; j++) {
				drawTile(canvas, i, j);
			}
		}
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
		PointF startP = mapToScreen(bbox.minX + tileWidth * x, bbox.minY + tileHeight * y);
		canvas.drawRect(startP.x, startP.y, startP.x + 256, startP.y + 256, tileStyle);
		/*
		canvas.drawText(format("x=%d y=%d", x, y),
			startP.x+50, startP.y+127,
			tileStyle);
		if (true) return;
		*/
		// draw "x="
		int xPos = (int) startP.x+50;
		int yPos = (int) startP.y+127;
		
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
		float offsetY = y - height / 2f;
		return new PointF(centerAtDragStart.x + offsetX * getResolution(), centerAtDragStart.y
				+ offsetY * getResolution());
	}

	private PointF screenToMap(float x, float y) {
		float offsetX = x - width / 2f;
		float offsetY = y - height / 2f;
		return new PointF(center.x + offsetX * getResolution(), center.y + offsetY
				* getResolution());
	}

	private PointF mapToScreen(float x, float y) {
		float offsetX = x - center.x;
		float offsetY = y - center.y;
		return new PointF(width / 2f + offsetX / getResolution(), height / 2f + offsetY
				/ getResolution());
	}

	private float getResolution() {
		return 1.125f / zoom;
	}
}
