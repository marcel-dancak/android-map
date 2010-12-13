package sk.maps;

public interface MapView {

	void setZoom(int zoom);
	int getZoom();
	
	void setHeading(int heading);
	void setLayer(TmsLayer layer);
	TmsLayer getLayer();
}
