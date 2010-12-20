package sk.gista.android.maps;

import android.graphics.PointF;

public interface MapView extends AndroidComponent {

	void setZoom(int zoom);
	int getZoom();
	
	void setHeading(int heading);
	void setLayer(TmsLayer layer);
	TmsLayer getLayer();
	
	PointF getCenter();
	void setCenter(float x, float y);
	
	PointF mapToScreenAligned(float x, float y);
	void addOverlay(Overlay overlay);
	
	void moveToLocation(float x, float y);
	void recycle();
}
