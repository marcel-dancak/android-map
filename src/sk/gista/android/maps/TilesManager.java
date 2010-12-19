package sk.gista.android.maps;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import sk.gista.android.maps.Layer.Tile;
import sk.gista.android.maps.Layer.TileListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

public class TilesManager {

	private static final String TAG = TilesManager.class.getName();
	
	private List<TileListener> tileListeners = new ArrayList<TileListener>();
	private ExecutorService pool = Executors.newFixedThreadPool(4);
	private List<Tile> unproccessed = new ArrayList<Layer.Tile>();
	
	private TmsLayer layer;
	
	public TilesManager(TmsLayer layer) {
		this.layer = layer;
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

	
	BlockingQueue<Tile> queue = new LinkedBlockingQueue<Layer.Tile>(10);
	
	private void startLoop() {
		Thread t = new Thread() {
			@Override
			public void run() {
				boolean run = true;
				while (run) {
					final Tile t;
					try {
						t = queue.take();
						//System.out.println("Taken tile");
						//Thread.sleep(200);
						
						pool.submit(new Runnable() {
							
							Tile tile = t;
							@Override
							public void run() {
								//System.out.println("task started");
								process(tile);
								//requestTile2(tile);
							}
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}
			}
				
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	
	private final void process(Tile tile) {
		/*
		URL url;
		try {
			url = new URL(layer.getUrl(tile));
			//Log.i(TAG, url.toString());
			//System.out.println(url.toString());
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			//Log.i(TAG, "Content Type: "+httpCon.getContentType());
			//Log.i(TAG, "Content Length: "+httpCon.getContentLength());
			InputStream is = httpCon.getInputStream();
			//InputStream is = new PatchInputStream(httpCon.getInputStream());
			//InputStream is = new FlushedInputStream(httpCon.getInputStream());
			//InputStream is = new ByteArrayInputStream(convertInputStreamToByteArray(httpCon.getInputStream()));
			//Bitmap image = BitmapFactory.decodeStream(is);
			
			byte [] content = inputStreamToByteArray(is);
			Bitmap image = BitmapFactory.decodeByteArray(content, 0, content.length);
			//BitmapFactory.Options options = new BitmapFactory.Options();
			//options.
			//Bitmap image = BitmapFactory.decodeStream(is);
			//Bitmap image = BitmapFactory.decodeStream(is, null, new Options());
			is.close();
			//Log.i(TAG, url.toString());
			//Log.i(TAG, "image "+image);
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
		 */
	}
	
	public void requestTiles(List<Tile> tiles) {
		for (final Tile t : tiles) {
			pool.submit(new Runnable() {
				
				//Tile tile = t;
				@Override
				public void run() {								
					process(t);
				}
			});
		}
	}
	
	public void requestTilesAsync(List<Tile> tiles) {
		//System.out.println("REQUEST start"+tiles.size());
		queue.addAll(tiles);
		//System.out.println("REQUEST end");
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
					String query = format("/1.0.0/%s/%d/%d/%d.%s", name, zoom, x, y, format);
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
	
	public void requestTiles2(List<Tile> tiles) {
		System.out.println("Listeners: "+tileListeners);
		for (Tile t : tiles) {
			requestTile2(t);
		}
	}
	
	public void cancelAll() {
		Log.i(TAG, "Cancel all Requests!");
		for (Downloader d : downloaders) {
			if (d.getStatus() != Status.FINISHED) {
				d.cancel(true);
			}
		}
	}
	
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
		int pending = 0;
		int running = 0;
		int finished = 0;
		for (Downloader d : downloaders) {
			switch (d.getStatus()) {
			case PENDING:
				pending++;
				break;
			case RUNNING:
				running++;
				break;
			case FINISHED:
				finished++;
				break;
			}
			if (d.getStatus() == Status.PENDING) {
				pending++;
			}
		}
		Log.i(TAG, "active downloaders: "+downloaders.size() + " Pending: "+pending + " Running: "+running+" Finished: "+finished);
		Downloader asyncDownloader = new Downloader();
		downloaders.add(asyncDownloader);
		asyncDownloader.layer = layer;
		asyncDownloader.manager = this;
		asyncDownloader.execute(tile);
	}
	
	private static class Downloader extends AsyncTask<Tile, Integer, Tile> {
		TilesManager manager;
		TmsLayer layer;
		
		@Override
		protected Tile doInBackground(Tile... params) {
			Tile tile = params[0]; 
			URL url;
			Bitmap image = null;
			try {
				
				boolean method = false;
				if (method) {
					HttpClient client = new DefaultHttpClient();
					HttpGet get = new HttpGet(layer.getUrl(tile));
					HttpResponse response = client.execute(get);
					for (Header header : response.getAllHeaders()) {
						Log.i(TAG, header.getName() + ": "+header.getValue());
					}
					
					HttpEntity entity = response.getEntity();
					Log.i(TAG, "Content-length: "+entity.getContentLength());
					InputStream is = entity.getContent();
					image = BitmapFactory.decodeStream(is);
					is.close();
				} else {
					url = new URL(layer.getUrl(tile));
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
						image = BitmapFactory.decodeByteArray(content, 0, content.length);
					}
					//Log.i(TAG, Thread.currentThread().getName()+" content length: "+content.length);
					is.close();
				}
				
				Log.i(TAG, Thread.currentThread().getName()+" image "+image);

			} catch (Exception e) {
				Log.e(TAG, "what!", e);
				e.printStackTrace();
			}
			return new Tile(tile.getX(), tile.getY(), tile.getZoomLevel(), image);
		}

		@Override
		protected void onPostExecute(Tile result) {
			Log.i(TAG, "onPostExecute "+result.getImage());
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
			is = new BufferedInputStream(is);
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] buffer = new byte[512];
			int result = is.read(buffer);
			
			while(result > 0 && !isCancelled()) {
				buf.write(buffer, 0, result);
				//Log.i(TAG, "reading incomplete data "+result);
				result = is.read(buffer);
			}
			
			return isCancelled()? null : buf.toByteArray();
		}
	}
}
