package sk.gista.android.maps;

import java.util.Timer;
import java.util.TimerTask;

import sk.gista.android.utils.Utils;
import android.graphics.Matrix;
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
				listener.onZoomEnd();
				
				//wasZoom = false;
				break;
			case MotionEvent.ACTION_DOWN:
				//Log.i(TAG, "delta: "+(curTime - lastTouchTime));
				if (curTime - lastTouchTime > 40 && curTime - lastTouchTime < 150) {
					Log.i(TAG, "Double click!");
					
				} else {
					//wasZoom = false;
					dragStartCenter = map.getCenter();
					dragStartPx = new PointF(x, y);
				}
				break;
			case MotionEvent.ACTION_UP:
				// for the case when event with ACTION_POINTER_UP action didn't occurred
				if (wasZoom) {
					// TODO: check that tmsLayer is not null
					//onZoomPinchEnd();
				}
				wasZoom = false;
				lastTouchTime = curTime;
				break;
			case MotionEvent.ACTION_MOVE:
				//Log.i(TAG, "ACTION_MOVE "+event.getPointerCount());
				if (!wasZoom && event.getPointerCount() == 1) {
					float newPosX = dragStartCenter.x+(x-dragStartPx.x)*map.getResolution();
					float newPosY = dragStartCenter.y+(y-dragStartPx.y)*map.getResolution();
					
					listener.onMove(newPosX, newPosY);
					//map.setCenter(newPosX, newPosY);
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
					if (possibleZoomPinch > 1 && zoom < layer.getResolutions().length -1) {
						//zoomPinch = possibleZoomPinch;
					}
					if (possibleZoomPinch < 1 && zoom > 0) {
						//zoomPinch = possibleZoomPinch;
					}
					//Log.i(TAG, "2 Fingers, distance: "+distance + " zoom "+zoomPinch);
				}
				break;
			}
		}
		
		return true;
	}
	
	
	public interface MapControlListener {
		void onMoveStart();
		void onMove(float x, float y);
		void onZoomEnd();
	}
}
