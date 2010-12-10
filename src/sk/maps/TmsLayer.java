package sk.maps;

import static java.lang.String.format;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sk.maps.Layer.Tile;

import com.jhlabs.map.proj.Projection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

public class TmsLayer extends Layer {

	private static final String TAG = TmsLayer.class.getName();
	
	private String serverUrl;
	private String name;
	private String format;
	
	// tile size in pixels
	private int tileWidth = 256;
	private int tileHeight = 256;
	
	ExecutorService pool = Executors.newFixedThreadPool(4);
	private List<Tile> unproccessed = new ArrayList<Layer.Tile>();
	
	public TmsLayer(BBox bbox, double[] resolutions, String url, String name, String format) {
		super(bbox, resolutions);
		this.serverUrl = url;
		this.name = name;
		this.format = format;
		startLoop();
	}
	
	public TmsLayer(BBox bbox, double[] resolutions, String url, String name, String format, Projection projection) {
		super(bbox, resolutions, projection);
		this.serverUrl = url;
		this.name = name;
		this.format = format;
		startLoop();
	}
	
	private void startLoop() {
		Thread t = new Thread() {
			@Override
			public void run() {
				boolean run = true;
				while (run) {
					synchronized (unproccessed) {
						while (unproccessed.size() > 0) {
							pool.submit(new Runnable() {
								@Override
								public void run() {
									process(unproccessed.get(0));
								}
							});
							//synchronized (unproccessed) {
								unproccessed.remove(0);
						}
							try {
								System.out.println("waiting");
								unproccessed.wait(400);
								System.out.println("wake up! "+unproccessed.size());
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
					}
				}
			}
		};
		t.start();
	}
	
	public int getTileWidth() {
		return tileWidth;
	}
	
	public int getTileHeight() {
		return tileHeight;
	}
	
	private final void process(Tile tile) {
		URL url;
		try {
			//System.out.println(format("%d %f : %s", 1, 5.42f, "sadsdf"));
			System.out.println("....");
			String query = format("/1.0.0/%s/%d/%d/%d.%s", name, tile.getZoomLevel()-1, tile.getX(), tile.getY(), format);
			url = new URL(serverUrl+"/"+query);
			//Log.i(TAG, url.toString());
			System.out.println(url.toString());
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			//Log.i(TAG, "Content Type: "+httpCon.getContentType());
			//Log.i(TAG, "Content Length: "+httpCon.getContentLength());
			
			Bitmap image = BitmapFactory.decodeStream(httpCon.getInputStream());
			//imageStream.close();
			if (image != null) {
				//Log.i(TAG, format("Get image %d x %d", image.getWidth(), image.getHeight()));
				//tile.setImage(image);
				//fireTileLoad(tile);
				fireTileLoad(new Tile(tile.getX(), tile.getY(), tile.getZoomLevel(), image));
				return;
			}
		} catch (Exception e) {
			Log.e(TAG, "what!", e);
			e.printStackTrace();
		}
		fireTileLoadingFailed(tile);
		//fireTileLoadingFailed(new Tile(tile.getX(), tile.getY(), tile.getZoomLevel(), null));
	}
	
	public void requestTiles(List<Tile> tiles) {
		synchronized (unproccessed) {
			unproccessed.addAll(tiles);
			unproccessed.notifyAll();
		}
	}
	
	public void requestTile(final Tile tile) {
		//synchronized (unproccessed) {
			unproccessed.add(tile);
			//unproccessed.notifyAll();
		//}
		/*
		pool.submit(new Runnable() {
			
			@Override
			public void run() {
				process(tile);
			}
		});
		*/
		/*
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
		*/
	}
	
	private Queue<Runnable> tasks = new LinkedList<Runnable>();
	private List<Downloader> downloaders = new ArrayList<Downloader>();
	
	public void requestTile2(Tile tile) {
		/*
		for (Downloader d : downloaders) {
			if (d.getStatus() == Status.FINISHED) {
				d.layer = this;
				d.execute(tile);
				Log.i(TAG, "downloaders: "+downloaders.size());
				return;
			}
		}*/
		Downloader asyncDownloader = new Downloader();
		downloaders.add(asyncDownloader);
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
