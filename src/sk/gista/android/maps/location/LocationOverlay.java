package sk.gista.android.maps.location;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.jhlabs.geom.Point2D;

import sk.gista.android.maps.MapView;
import sk.gista.android.maps.Overlay;

public class LocationOverlay implements Overlay {

	private Paint pointStyle;
	
	public LocationOverlay() {
		
	}
	
	@Override
	public void onDraw(MapView map, Canvas canvas) {
		Log.i("OVERLAY", "draw location overlay");
		Point2D p2 = new Point2D();
		map.getLayer().getProjection().transform(new Point2D(21.23886386, 49.00096926), p2);
		//Log.i(TAG, format("projected position: [%f, %f]", p2.x, p2.y));
		PointF currentPos = map.mapToScreenAligned((float) p2.x, (float) p2.y);
		canvas.drawArc(new RectF(currentPos.x-2, currentPos.y-2, currentPos.x+2, currentPos.y+2), 0, 360, true, pointStyle);
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResume() {
		pointStyle = new Paint();
		pointStyle.setColor(Color.BLUE);
	}

}
