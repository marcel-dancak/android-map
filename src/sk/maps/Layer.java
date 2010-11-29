package sk.maps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class Layer {

	private BBox bbox;
	public void drawTile(Canvas canvas) {
		Paint style = new Paint();
		style.setColor(Color.RED);
		canvas.drawLine(5, 5, 200, 100, style);
	}
}
