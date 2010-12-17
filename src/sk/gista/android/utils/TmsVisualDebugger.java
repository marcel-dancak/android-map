package sk.gista.android.utils;

import static java.lang.String.format;
import sk.gista.android.maps.BBox;
import sk.gista.android.maps.Map;
import sk.gista.android.maps.TmsLayer;
import sk.gista.android.maps.Layer.Tile;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class TmsVisualDebugger {

	private Map map;
	private TmsLayer layer;
	private BBox bbox;
	
	private Bitmap textBuffer;
	private float[] numbersWidths;
	
	private Paint tileStyle;
	
	public TmsVisualDebugger(Map map) {
		this.map = map;
		layer = map.getLayer();
		bbox = layer.getBoundingBox();
		
		tileStyle = new Paint();
		tileStyle.setColor(Color.LTGRAY);
		tileStyle.setStyle(Paint.Style.STROKE);
		tileStyle.setTextSize(30);
		tileStyle.setAntiAlias(true);
		buildTextTextures();
	}
	
	private final void buildTextTextures() {
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
	
	public void drawTile(Canvas canvas, int x, int y) {
		float tileWidth = layer.getTileWidth() * map.getResolution();
		float tileHeight = layer.getTileHeight() * map.getResolution();
		
		PointF startP = map.mapToScreenAligned(bbox.minX + tileWidth * x, bbox.minY + tileHeight * y);
		canvas.drawRect(startP.x, startP.y, startP.x+256f, startP.y+256, tileStyle);
		/*
		canvas.drawText(format("x=%d y=%d", x, y),
			startP.x+50, startP.y+127,
			tileStyle);
		if (true) return;
		*/
		
		int xPos = (int) startP.x+50;
		int yPos = (int) startP.y+127;
		
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
}
