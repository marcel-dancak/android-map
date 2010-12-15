package sk.maps;

import static java.lang.String.format;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
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

import sk.maps.Layer.Tile;

import com.jhlabs.map.proj.Projection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

public class TmsLayer extends Layer {

	private static final String TAG = TmsLayer.class.getName();
	
	private String title;
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
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
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
	
	public int getTileWidth() {
		return tileWidth;
	}
	
	public int getTileHeight() {
		return tileHeight;
	}
	
	private final void process(Tile tile) {
		URL url;
		try {
			String query = format("/1.0.0/%s/%d/%d/%d.%s", name, tile.getZoomLevel(), tile.getX(), tile.getY(), format);
			url = new URL(serverUrl+"/"+query);
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
		for (Tile t : tiles) {
			requestTile2(t);
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
				String query = format("/1.0.0/%s/%d/%d/%d.%s", layer.name, tile.getZoomLevel(), tile.getX(), tile.getY(), layer.format);
				url = new URL(layer.serverUrl+"/"+query);
				HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
				InputStream is = httpCon.getInputStream();
				byte [] content = inputStreamToByteArray(is);
				image = BitmapFactory.decodeByteArray(content, 0, content.length);
				//image = BitmapFactory.decodeStream(is);
				is.close();

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
	
	static class FlushedInputStream extends FilterInputStream {
	    public FlushedInputStream(InputStream inputStream) {
	        super(inputStream);
	    }

	    @Override
	    public long skip(long n) throws IOException {
	        long totalBytesSkipped = 0L;
	        while (totalBytesSkipped < n) {
	            long bytesSkipped = in.skip(n - totalBytesSkipped);
	            if (bytesSkipped == 0L) {
	                  int bytes = read();
	                  if (bytes < 0) {
	                      break;  // we reached EOF
	                  } else {
	                      bytesSkipped = 1; // we read one byte
	                  }
	           }
	            totalBytesSkipped += bytesSkipped;
	        }
	        return totalBytesSkipped;
	    }
	}
	
	public class PatchInputStream extends FilterInputStream {
		public PatchInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			Log.i(TAG, "read 1");
			return super.read();
		}

		@Override
		public int read(byte[] buffer, int offset, int count) throws IOException {
			Log.i(TAG, "read 3");
			return super.read(buffer, offset, count);
		}
		
		@Override
		public int read(byte[] buffer) throws IOException {
			Log.i(TAG, "read 2");
			return super.read(buffer);
		}
		
		@Override
		public long skip(long n) throws IOException {
			Log.i(TAG, "skip " + n);
			long m = 0L;
			while (m < n) {
				long _m = in.skip(n - m);
				if (_m == 0L)
					break;
				m += _m;
			}
			return m;
		}
	}


	public static final byte[] inputStreamToByteArray(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while(result !=-1) {
			byte b = (byte)result;
			buf.write(b);
			result = bis.read();
		}
		return buf.toByteArray();
	}


}
