package sk.maps;

import android.graphics.PointF;

public interface MapView {

	void setZoom(int zoom);
	int getZoom();
	
	void setHeading(int heading);
	void setLayer(TmsLayer layer);
	TmsLayer getLayer();
	
	PointF getCenter();
	void setCenter(float x, float y);
}
