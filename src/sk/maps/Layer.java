package sk.maps;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class Layer {

	private BBox bbox;
	private List<TileListener> tileListeners = new ArrayList<TileListener>();
	
	public void drawTile(Canvas canvas) {
		Paint style = new Paint();
		style.setColor(Color.RED);
		canvas.drawLine(5, 5, 200, 100, style);
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
