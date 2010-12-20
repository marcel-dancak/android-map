package sk.gista.android.maps;

import android.graphics.Bitmap;

import com.jhlabs.map.proj.NullProjection;
import com.jhlabs.map.proj.Projection;


public abstract class Layer {

	protected BBox bbox;
	protected double[] resolutions;
	protected Projection projection;
	
	
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
	
	public abstract String getName();
	
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
