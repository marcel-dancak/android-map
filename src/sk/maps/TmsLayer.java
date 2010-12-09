package sk.maps;

import static java.lang.String.format;
import java.net.HttpURLConnection;
import java.net.URL;

import com.jhlabs.map.proj.Projection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class TmsLayer extends Layer {

	private static final String TAG = TmsLayer.class.getName();
	
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
	
	public int getTileWidth() {
		return tileWidth;
	}
	
	public int getTileHeight() {
		return tileHeight;
	}
	
	public void requestTile(final int zoom, final int x, final int y) {
		Thread t = new Thread() {
			public void run() {
				URL url;
				try {
					String query = format("/1.0.0/%s/%d/%d/%d.%s", name, zoom-1, x, y, format);
					url = new URL(serverUrl+"/"+query);
					//Log.i(TAG, url.toString());
					HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
					//Log.i(TAG, "Content Type: "+httpCon.getContentType());
					//Log.i(TAG, "Content Length: "+httpCon.getContentLength());
					
					Bitmap image = BitmapFactory.decodeStream(httpCon.getInputStream());
					//imageStream.close();
					if (image != null) {
						//Log.i(TAG, format("Get image %d x %d", image.getWidth(), image.getHeight()));
						fireTileLoad(new Tile(x, y, zoom, image));
						return;
					}
				} catch (Exception e) {
					Log.e(TAG, "what!", e);
					e.printStackTrace();
				}
				fireTileLoadingFailed(new Tile(x, y, zoom, null));
			}
		};
		t.start();
	}
	
	public void requestTile2(Tile tile) {
		Downloader asyncDownloader = new Downloader();
		asyncDownloader.layer = this;
		asyncDownloader.execute(tile);
	}
	
	private static class Downloader extends AsyncTask<Tile, Integer, Tile> {
		
		TmsLayer layer;
		
		@Override
		protected Tile doInBackground(Tile... params) {
			Tile tile = params[0]; 
			URL url;
			Bitmap image = null;
			try {
				String query = format("/1.0.0/%s/%d/%d/%d.%s", layer.name, tile.getZoomLevel()-1, tile.getX(), tile.getY(), layer.format);
				url = new URL(layer.serverUrl+"/"+query);
				//Log.i(TAG, url.toString());
				HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
				//Log.i(TAG, "Content Type: "+httpCon.getContentType());
				//Log.i(TAG, "Content Length: "+httpCon.getContentLength());
				
				image = BitmapFactory.decodeStream(httpCon.getInputStream());
				//imageStream.close();

			} catch (Exception e) {
				Log.e(TAG, "what!", e);
				e.printStackTrace();
			}
			return new Tile(tile.getX(), tile.getY(), tile.getZoomLevel(), image);
		}

		@Override
		protected void onPostExecute(Tile result) {
			if (result.getImage() != null) {
				layer.fireTileLoad(result);
			} else {
				layer.fireTileLoadingFailed(result);
			}
		}
	}
}
