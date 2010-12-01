package sk.maps;

import static java.lang.String.format;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class TmsLayer extends Layer {

	private static final String TAG = TmsLayer.class.getName();
	
	private String serverUrl;
	private String name;
	private String format;
	
	public TmsLayer(String url, String name, String format) {
		this.serverUrl = url;
		this.name = name;
		this.format = format;
	}
	
	public Bitmap requestTile(int zoom, int x, int y, int width, int height) {
		URL url;
		try {
			String query = format("/1.0.0/%s/%d/%d/%d.%s", name, zoom-1, x, y, format);
			url = new URL(serverUrl+"/"+query);
			Log.i(TAG, url.toString());
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			Log.i(TAG, "Content Type: "+httpCon.getContentType());
			Log.i(TAG, "Content Length: "+httpCon.getContentLength());
			
			//BufferedInputStream imageStream = new BufferedInputStream(httpCon.getInputStream());
			for (int i = 0; i < 10; i++) {
				//Log.i(TAG, ""+imageStream.read());
			}
			Bitmap image = BitmapFactory.decodeStream(httpCon.getInputStream());
			Log.i(TAG, "image: "+image);
			//imageStream.close();
			if (image != null) {
				Log.i(TAG, format("Get image %d x %d", image.getWidth(), image.getHeight()));
			}
			
			return image;
		} catch (Exception e) {
			Log.e(TAG, "what!", e);
			e.printStackTrace();
		}
		return null;
	}
}
