package sk.maps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import sk.utils.Utils;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Main extends Activity {
	private static final String TAG = Main.class.getName();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        List<TmsLayer> layers = new ArrayList<TmsLayer>();
        try {
			//String settings = Utils.httpGet(getString(R.string.settings_url));
        	String settings = Utils.readInputStream(getResources().openRawResource(R.raw.settings));
			JSONArray json = new JSONArray(settings);
			for (int i = 0; i < json.length(); i++) {
				JSONObject layer = json.getJSONObject(i);
				assert 1 == layer.names().length();
				String layerName = layer.names().getString(0);
				Log.i(TAG, "layer name:"+layerName);
				
				JSONObject layerSettings = layer.getJSONObject(layerName);
				String title = layerSettings.getString("title");
				String url = layerSettings.getString("url");
				String extension = layerSettings.getString("extension");
				String srs = layerSettings.getString("srs");
				
				JSONArray resolutionsArray = layerSettings.getJSONArray("resolutions");
				JSONArray bboxArray = layerSettings.getJSONArray("bbox");
				double[] resolutions = new double[resolutionsArray.length()];
				for (int j = 0; j < resolutionsArray.length(); j++) {
					resolutions[j] = resolutionsArray.getDouble(j);
				}
				BBox bbox = new BBox(
						(float) bboxArray.getDouble(0),
						(float) bboxArray.getDouble(1),
						(float) bboxArray.getDouble(2),
						(float) bboxArray.getDouble(3));
				
				layers.add(new TmsLayer(bbox, resolutions, url, layerName, extension));
				
				Log.i(TAG, "Title: "+title);
				Log.i(TAG, "URL: "+url);
				Log.i(TAG, "Extension: "+extension);
				Log.i(TAG, "Projection: "+srs);
			}
			
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
		}
        
        TmsLayer layer = layers.get(0);
        //final MapView map = new Map(this, layer.getBoundingBox(), layer.getResolutions(), layer);
        //final MapView map = new MapSurface(this, layer.getBoundingBox(), layer.getResolutions(), layer);
        final MapView map = new MapOpenGl(this, layer);
        //layers.remove(0);
        
        Button zoomIn = new Button(this);
        zoomIn.setText("+");
        zoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				map.setZoom(map.getZoom()+1);
			}
		});
        
        Button zoomOut = new Button(this);
        zoomOut.setText("-");
        zoomOut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (map.getZoom() > 1) {
					map.setZoom(map.getZoom()-1);
				}
			}
		});
        
        RelativeLayout layout = new RelativeLayout(this);
        LinearLayout zoomControls = new LinearLayout(this);
        zoomControls.addView(zoomOut);
        zoomControls.addView(zoomIn);
        
        layout.addView((View) map);
        //layout.addView(mapSurface);
        layout.addView(zoomControls);
        setContentView(layout);
        //setContentView(map);
    }
}