package sk.gista.android.maps;

import sk.gista.android.utils.Utils;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MapEventsGenerator {
	
	private static final String TAG = MapEventsGenerator.class.getSimpleName();
	
	private MapView map;
	private MapControlListener listener;
	
	private PointF dragStartCenter;
	private PointF dragStartPx;
	
	private boolean wasZoom;
	private float startDistance;
	private long lastTouchTime;
	
	public MapEventsGenerator(MapView map) {
		this.map = map;
	}
	
	public void setMapControlListener(MapControlListener listener) {
		this.listener = listener;
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		Layer layer = map.getLayer();
		
		if (listener != null && layer != null) {
			int height = ((View) map).getHeight();
			int zoom = map.getZoom();
			
			float x = event.getX(0);
			float y = height-event.getY(0);
			
			long curTime = System.currentTimeMillis();
			
			int action = event.getAction() & MotionEvent.ACTION_MASK;
			
			switch (action) {
			case MotionEvent.ACTION_POINTER_DOWN:
				wasZoom = true;
				startDistance = Utils.distance(x, y, event.getX(1), height-event.getY(1));
				//Log.i(TAG, "2 Fingers, start distance: "+startDistance);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				//Log.i(TAG, "1 Finger");
				listener.onZoomEnd();
				break;
			case MotionEvent.ACTION_DOWN:
				//Log.i(TAG, "delta: "+(curTime - lastTouchTime));
				if (curTime - lastTouchTime > 40 && curTime - lastTouchTime < 150) {
					Log.i(TAG, "Double click!");
					listener.onDoubleTap(x, y);
				} else {
					//wasZoom = false;
					dragStartCenter = new PointF(map.getCenter().x, map.getCenter().y);
					dragStartPx = new PointF(x, y);
					
					listener.onTapStart();
				}
				
				break;
			case MotionEvent.ACTION_UP:
				wasZoom = false;
				lastTouchTime = curTime;
				listener.onTapEnd();
				break;
			case MotionEvent.ACTION_MOVE:
				//Log.i(TAG, "ACTION_MOVE "+event.getPointerCount());
				if (!wasZoom && event.getPointerCount() == 1) {
					float newPosX = dragStartCenter.x+(dragStartPx.x-x)*map.getResolution();
					float newPosY = dragStartCenter.y+(dragStartPx.y-y)*map.getResolution();
					listener.onMove(newPosX, newPosY);
				} else {
					float distance = Utils.distance(x, y, event.getX(1), height-event.getY(1));
					float zoomPinch = distance/startDistance;
					
					if ((zoomPinch > 1 && zoom < layer.getResolutions().length -1)
							|| (zoomPinch < 1 && zoom > 0)) {
						listener.onZoom(zoomPinch);
					}
					//Log.i(TAG, "2 Fingers, distance: "+distance + " zoom "+zoomPinch);
				}
				break;
			}
		}
		return true;
	}
	
	
	public interface MapControlListener {
		void onTapStart();
		void onTapEnd();
		void onDoubleTap(float x, float y);
		void onMove(float x, float y);
		void onZoom(float zoom);
		void onZoomEnd();
	}
}
