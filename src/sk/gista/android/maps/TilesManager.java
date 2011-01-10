package sk.gista.android.maps;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import sk.gista.android.maps.Layer.Tile;
import sk.gista.android.maps.Layer.TileListener;
import sk.gista.android.utils.NetworkDebugger;
import sk.gista.android.utils.NetworkDebugger.Signal;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

public class TilesManager {

	private static final String TAG = TilesManager.class.getName();
	private List<TileListener> tileListeners = new ArrayList<TileListener>();
	private MemoryCache tilesCache;
	private MapView map;
	private TmsLayer layer;
	
	public TilesManager(MapView map) {
		this.map = map;
		this.layer = map.getLayer();
		tilesCache = new MemoryCache(map, 25);
		
		NetworkDebugger.server = "192.168.1.110";
		NetworkDebugger.debuggingEnabled = true;
	}
	
	public void addTileListener(TileListener listener) {
		tileListeners.add(listener);
	}
	
	protected void fireTileLoad(Tile tile) {
		if (tile.getZoomLevel() != map.getZoom()) {
			tile.recycle();
			return;
		}
		tilesCache.putTile(tile);
		for (TileListener listener : tileListeners) {
			listener.onTileLoad(tile);
		}
	}
	
	protected void fireTileLoadingFailed(Tile tile) {
		tilesCache.remove(tile);
		for (TileListener listener : tileListeners) {
			listener.onTileLoadingFailed(tile);
		}
	}
	
	private List<Downloader> downloaders = new ArrayList<Downloader>();
	
	public void cancelAll() {
		Log.i(TAG, "Cancel all Requests!");
		for (Downloader d : downloaders) {
			if (d.getStatus() != Status.FINISHED) {
				d.cancel(true);
			}
		}
	}
	
	public void clearCache() {
		tilesCache.clearCache();
	}
	
	public boolean hasInCache(int x, int y) {
		return tilesCache.getTile(x, y) != null;
	}
	
	public Tile getTile(int x, int y) {
		Tile tile = tilesCache.getTile(x, y);
		if (tile == null) {
			tile = new Tile(x, y, map.getZoom(), null);
			requestTile(tile);
			tilesCache.putTile(tile);
			return null;
		}
		return tile;
	}
	
	public void requestTiles(List<Tile> tiles) {
		System.out.println("Listeners: "+tileListeners);
		Log.i(TAG, "requesting "+tiles.size()+" tiles");
		for (Tile t : tiles) {
			requestTile(t);
		}
	}
	
	public void requestTile(Tile tile) {
		Downloader asyncDownloader = new Downloader();
		downloaders.add(asyncDownloader);
		asyncDownloader.layer = layer;
		asyncDownloader.manager = this;
		asyncDownloader.execute(tile);
	}
	
	public static class Downloader extends AsyncTask<Tile, Integer, Tile> {
		TilesManager manager;
		TmsLayer layer;
		Tile tile;
		
		@Override
		protected Tile doInBackground(Tile... params) {
			tile = params[0]; 
			Bitmap image = null;
			
			boolean method = true;
			
			if (method) {
				Log.i(TAG, layer.getUrl(tile));
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(layer.getUrl(tile));
				InputStream is = null;
				boolean aborted = false;
				try {
					HttpResponse response = client.execute(get);
					/*
					for (Header header : response.getAllHeaders()) {
						Log.i(TAG, header.getName() + ": "+header.getValue());
					}
					*/
					HttpEntity entity = response.getEntity();
					//Log.i(TAG, "Content-length: "+entity.getContentLength());
					is = entity.getContent();
					byte [] content = inputStreamToByteArray(is);
					if (content != null) {
						NetworkDebugger.sendFinished(tile);
						image = BitmapFactory.decodeByteArray(content, 0, content.length);
					}
					//image = BitmapFactory.decodeStream(is);
					//Log.i(TAG, Thread.currentThread().getName()+" image "+image);
					
					if (isCancelled()) {
						NetworkDebugger.sendSignal(tile, Signal.ABORTED);
						get.abort();
						aborted = true;
					}
				} catch (Exception e) {
					Log.e(TAG, "downloading failed!", e);
					get.abort();
					aborted = true;
					NetworkDebugger.sendSignal(tile, Signal.ERROR);
				} finally {
					if (!aborted && is != null) {
						try {
							is.close();
						} catch (IOException e) {
							Log.e(TAG, "aborted: "+aborted);
							Log.e(TAG, "closing input stream failed!", e);
						}
					}
				}
			} else {
				try {
					URL url = new URL(layer.getUrl(tile));
					HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
					
					InputStream is = httpCon.getInputStream();
					/*
					Log.i(TAG, "stream "+is.getClass().getSimpleName() +
							" Code: "+httpCon.getResponseCode() +
							" Content-length: "+httpCon.getContentLength() +
							" Content-type "+httpCon.getContentType()
					);
					*/
					byte [] content = inputStreamToByteArray(is);
					if (content != null) {
						NetworkDebugger.sendFinished(tile);
						image = BitmapFactory.decodeByteArray(content, 0, content.length);
					}
					if (isCancelled()) {
						NetworkDebugger.sendSignal(tile, Signal.ABORTED);
					}
					//Log.i(TAG, Thread.currentThread().getName()+" content length: "+content.length);
					is.close();
					httpCon.disconnect();
				} catch (Exception e) {
					Log.e(TAG, "what!", e);
					Log.e(TAG, "sending error signal");
					NetworkDebugger.sendSignal(tile, Signal.ERROR);
				}
			}
			//Log.i(TAG, Thread.currentThread().getName()+" image "+image);
			return new Tile(tile.getX(), tile.getY(), tile.getZoomLevel(), image);
		}

		@Override
		protected void onPostExecute(Tile result) {
			//Log.i(TAG, "onPostExecute "+result.getImage());
			manager.downloaders.remove(this);
			if (!isCancelled()) {
				if (result.getImage() != null) {
					manager.fireTileLoad(result);
				} else {
					manager.fireTileLoadingFailed(result);
				}
			}
		}
		
		public final byte[] inputStreamToByteArray(InputStream is) throws IOException {
			if (isCancelled()) {
				return null;
			}
			int progress = 0;
			NetworkDebugger.sendProgress(tile, progress, 0);
			is = new BufferedInputStream(is);
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int result = is.read(buffer);
			
			while(result > 0 && !isCancelled()) {
				buf.write(buffer, 0, result);
				//Log.i(TAG, "reading incomplete data "+result);
				//NetworkDebugger.sendProgress(tile, progress, result);
				progress++;
				result = is.read(buffer);
			}
			
			return isCancelled()? null : buf.toByteArray();
		}
	}
}
