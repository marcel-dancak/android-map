package sk.gista.android.maps;

import android.graphics.Point;
import android.graphics.PointF;

import com.jhlabs.map.proj.Projection;

public class TmsLayer extends Layer {

	private static final String TAG = TmsLayer.class.getName();
	
	private String title;
	private String serverUrl;
	private String name;
	private String format;
	
	// tile size in pixels
	private int tileWidth = 256;
	private int tileHeight = 256;
	
	public TmsLayer(BBox bbox, double[] resolutions, String url, String name, String format) {
		super(bbox, resolutions);
		this.serverUrl = url;
		this.name = name;
		this.format = format;
	}
	
	public TmsLayer(BBox bbox, double[] resolutions, String url, String name, String format, Projection projection) {
		super(bbox, resolutions, projection);
		this.serverUrl = url;
		this.name = name;
		this.format = format;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public String getName() {
		return name;
	}
	
	public String getUrl(Tile tile) {
		return serverUrl+"/1.0.0/"+name+"/"+tile.getZoomLevel()+"/"+tile.getX()+"/"+tile.getY()+"."+ format;
	}
	
	public int getTileWidth() {
		return tileWidth;
	}
	
	public int getTileHeight() {
		return tileHeight;
	}
	
	public Point getTileAt(PointF position, int zoom) {
		float tileX = (position.x - bbox.minX) / (tileWidth*(float) resolutions[zoom]);
		float tileY = (position.y - bbox.minY) / (tileHeight*(float) resolutions[zoom]);
		return new Point((int) Math.floor(tileX), (int) Math.floor(tileY));
	}
}
