package sk.gista.android.maps;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Point;
import android.util.Log;

import sk.gista.android.maps.Layer.Tile;

public class MemoryCache {

	private static final String TAG = "MemoryCache";
	
	private int capacity;
	private MapView map;
	
	public MemoryCache(MapView map, int capacity) {
		this.map = map;
		this.capacity = capacity;
	}
	
	private Map<String, Tile> tiles = new HashMap<String, Tile>();
	
	public boolean containsTile(Tile tile) {
		return tiles.containsKey(tile);
	}
	
	public boolean containsTile(int x, int y) {
		String tileKey = tileKey(x, y);
		return tiles.containsKey(tileKey);
	}
	
	public int size() {
		return tiles.size();
	}
	
	public void clearCache() {
		Log.i(TAG, "Clearing tiles");
		synchronized (tiles) {
			for (Tile tile : tiles.values()) {
				tile.recycle();
			}
			tiles.clear();
		}
	}
	
	public void putTile(Tile tile) {
		synchronized (tiles) {
			if (tiles.size() > capacity) {
				Point centerTile = map.getLayer().getTileAt(map.getCenter(), map.getZoom());
				//Point centerTile = getTileAtScreen(width/2, height/2);
				while (tiles.size() > capacity) {
					Tile mostFarAway = tiles.values().iterator().next();
					for (Tile t : tiles.values()) {
						if (Math.abs(centerTile.x-t.getX())+Math.abs(centerTile.y-t.getY()) >
								Math.abs(centerTile.x-mostFarAway.getX())+Math.abs(centerTile.y-mostFarAway.getY())) {
							mostFarAway = t;
						}
					}
					mostFarAway.recycle();
					tiles.remove(tileKey(mostFarAway.getX(), mostFarAway.getY()));
				}
			}
			String tileKey = tileKey(tile.getX(), tile.getY());
			tiles.put(tileKey, tile);
		}
	}
	
	public void remove(Tile tile) {
		synchronized (tiles) {
			if (containsTile(tile)) {
				tiles.remove(tile);
			}
		}
	}
	
	public Tile getTile(int x, int y) {
		String tileKey = tileKey(x, y);
		return tiles.get(tileKey);
	}
	
	private static final String tileKey(int x, int y) {
		return x+":"+y;
	}
}
