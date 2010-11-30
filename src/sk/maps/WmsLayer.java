package sk.maps;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static java.lang.String.format;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.util.Log;

public class WmsLayer extends Layer {

	private static String TAG = WmsLayer.class.getName();
	
	public Bitmap requestTile(RectF bbox, int width, int height) {
		URL url;
		try {
			String bboxEncoded = URLEncoder.encode(format("%f,%f,%f,%f", bbox.left, bbox.top, bbox.right, bbox.bottom)); 
			Log.i(TAG, bboxEncoded);
			//Log.i(TAG, URLDecoder.decode("http://localhost/cgi-bin/mapserv?map=/var/www/mapfiles/test.map&layers=countries&styles=&service=WMS&width=512&format=image%2Fpng&request=GetMap&height=612&srs=EPSG%3A4326&version=1.1.1&bbox=-215.15625%2C-125.15625%2C395.15625%2C305.15625"));
			url = new URL("http://192.168.2.2/cgi-bin/mapserv?map=/var/www/mapfiles/test.map" +
					"&layers=countries&styles=&service=WMS&format=image%2Fpng&request=GetMap" +
					"&version=1.1.1&srs=EPSG%3A4326" +
					"&width=256&height=256&bbox="+bboxEncoded);
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
