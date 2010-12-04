package sk.maps;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

public abstract class Layer {

	protected BBox bbox;
	protected double[] resolutions;
	
	private List<TileListener> tileListeners = new ArrayList<TileListener>();
	
	public Layer(BBox bbox, double[] resolutions) {
		this.bbox = bbox;
		this.resolutions = resolutions;
	}
	
	public final BBox getBoundingBox() {
		return bbox;
	}
	
	public final double[] getResolutions() {
		return resolutions;
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
		private Bitmap image;
		
		public Tile(int x, int y, Bitmap image) {
			this.x = x;
			this.y = y;
			this.image = image;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		
		public Bitmap getImage() {
			return image;
		}
	}
	
	public interface TileListener {
		void onTileLoad(Tile tile);
		void onTileLoadingFailed(Tile tile);
	}
}
