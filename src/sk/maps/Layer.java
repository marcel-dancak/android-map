package sk.maps;

import java.util.ArrayList;
import java.util.List;

import com.jhlabs.map.proj.NullProjection;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import android.graphics.Bitmap;

public abstract class Layer {

	protected BBox bbox;
	protected double[] resolutions;
	protected Projection projection;
	
	private List<TileListener> tileListeners = new ArrayList<TileListener>();
	
	public Layer(BBox bbox, double[] resolutions) {
		this(bbox, resolutions, new NullProjection());
		//this(bbox, resolutions, ProjectionFactory.getNamedPROJ4CoordinateSystem("epsg:4326"));
	}
	
	public Layer(BBox bbox, double[] resolutions, Projection projection) {
		this.bbox = bbox;
		this.resolutions = resolutions;
		this.projection = projection;
	}
	
	public final BBox getBoundingBox() {
		return bbox;
	}
	
	public final double[] getResolutions() {
		return resolutions;
	}
	
	public Projection getProjection() {
		return projection;
	}
	
	public void addTileListener(TileListener listener) {
		tileListeners.add(listener);
	}
	
	protected void fireTileLoad(Tile tile) {
		for (TileListener listener : tileListeners) {
			listener.onTileLoad(tile);
		}
	}
	
	protected void fireTileLoadingFailed(Tile tile) {
		for (TileListener listener : tileListeners) {
			listener.onTileLoadingFailed(tile);
		}
	}
	
	public static class Tile {
		private int x;
		private int y;
		private int zoomLevel;
		private Bitmap image;
		
		public Tile(int x, int y, int zoomLevel, Bitmap image) {
			this.x = x;
			this.y = y;
			this.zoomLevel = zoomLevel;
			this.image = image;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		
		public int getZoomLevel() {
			return zoomLevel;
		}
		
		public Bitmap getImage() {
			return image;
		}
		
		public void setImage(Bitmap image) {
			this.image = image;
		}
		
		public void recycle() {
			if (image != null) {
				image.recycle();
				image = null;
			}
		}
	}
	
	public interface TileListener {
		void onTileLoad(Tile tile);
		void onTileLoadingFailed(Tile tile);
	}
}
