package sk.gista.android.maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jhlabs.geom.Point2D;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import sk.gista.android.maps.location.LocationOverlay;
import sk.gista.android.settings.Settings;
import sk.gista.android.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class Main extends Activity implements SensorEventListener {
	private static final String TAG = Main.class.getName();
	
	static final int DIALOG_LAYERS_ID = 0;

	// temporary state values
	static final String LAYERS_LIST = "LAYERS_LIST";
	
	// persistent state values
	static final String ZOOM = "map_zoom";
	static final String LAYER_NAME = "layer_name";
	static final String CENTER_X = "center_x";
	static final String CENTER_Y = "center_y";
	
	private String layersSetting;
	
	private SharedPreferences mapState;
	
	private SensorManager sensorManager;
	private MapView map;
	private Button zoomIn;
	private Button zoomOut;
	
	List<TmsLayer> layers;
	private LocationOverlay locationOverlay;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "** onCreate");
        super.onCreate(savedInstanceState);
        
        mapState = getSharedPreferences("MAP_STATE", MODE_PRIVATE);
        
        setContentView(R.layout.main);
        map = (Map) findViewById(R.id.map);
        
        locationOverlay= new LocationOverlay(this);
        map.addOverlay(locationOverlay);
        
        zoomIn = (Button) findViewById(R.id.zoom_in);
        zoomOut = (Button) findViewById(R.id.zoom_out);
        Button moveToCurrentLocation = (Button) findViewById(R.id.move_to_position);
        
        zoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//map.setZoom(map.getZoom()+1);
				map.zoomTo(map.getZoom()+1);
			}
		});
        
        zoomOut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (map.getZoom() > 0) {
					map.zoomTo(map.getZoom()-1);
					//map.setZoom(map.getZoom()-1);
				}
			}
		});
        
        moveToCurrentLocation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Point2D location = locationOverlay.getLastLocation();
				Point2D projected = new Point2D();
				map.getLayer().getProjection().transform(location, projected);
				map.moveToLocation((float) projected.x, (float) projected.y);
			}
		});
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
    }
    
    @Override
    protected void onStart() {
    	Log.i(TAG, "** onStart");
    	super.onStart();
    }
    
    @Override
    protected void onResume() {
    	Log.i(TAG, "** onResume");
    	super.onResume();
    	
    	if (layers == null || layers.size() == 0) {
    		loadLayersConfig();
    	}
    	restoreState();
    	map.onResume();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.i(TAG, "Save temporary state");
    	super.onSaveInstanceState(outState);
    	if (layersSetting != null) {
    		//Log.i(TAG, "save layers list");
    		outState.putString(LAYERS_LIST, layersSetting);
    	}
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.i(TAG, "** onRestoreInstanceState");
    	super.onRestoreInstanceState(savedInstanceState);
    	layersSetting = savedInstanceState.getString(LAYERS_LIST);
    	loadLayersConfig();
    }
    
    @Override
    protected void onPause() {
    	Log.i(TAG, "** onPause");
    	super.onPause();
    	saveState();
    	map.onPause();
    	map.recycle();
    }
    
    @Override
    protected void onStop() {
    	Log.i(TAG, "** onStop");
    	super.onStop();
    }
    
    private void saveState() {
    	Log.i(TAG, "Save perzistent state variables");
    	Editor state = mapState.edit();
    	
    	if (map.getLayer() != null) {
    		state.putString(LAYER_NAME, map.getLayer().getName());
        	state.putInt(ZOOM, map.getZoom());
        	
    		Point2D center = new Point2D(map.getCenter().x, map.getCenter().y);
    		Point2D wgs84Center = new Point2D();
    		Projection proj = map.getLayer().getProjection();
    		proj.inverseTransform(center, wgs84Center);
    		state.putFloat(CENTER_X, (float) wgs84Center.x);
    		state.putFloat(CENTER_Y, (float) wgs84Center.y);
    	}
    	state.commit();
    }
    
    private void restoreState() {
    	String layerName = mapState.getString(LAYER_NAME, "");
    	int zoom = mapState.getInt(ZOOM, 0);
    	float centerX = mapState.getFloat(CENTER_X, Float.MIN_VALUE);
    	float centerY = mapState.getFloat(CENTER_Y, Float.MIN_VALUE);
    	Log.i(TAG, "Restoring state: zoom="+zoom + " center="+centerX+", "+centerY);
    	
    	for (TmsLayer layer : layers) {
    		if (layer.getName().equals(layerName)) {
    			map.setLayer(layer);
        		if (centerX != Float.MIN_VALUE) {
        			Point2D wgs84Center = new Point2D(centerX, centerY);
        			Point2D center = new Point2D();
        			layer.getProjection().transform(wgs84Center, center);
            		map.setCenter((float) center.x, (float) center.y);
            	}
        		map.setZoom(zoom);
        		break;
    		}
    	}
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.layers:
			showDialog(DIALOG_LAYERS_ID);
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Settings.class));
			return true;
		}
		return false;
	}

    @Override
    protected Dialog onCreateDialog(int id) {
    	Log.i(TAG, "** onCreateDialog");
    	Dialog dialog = null;
        switch(id) {
        case DIALOG_LAYERS_ID:
        	DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					TmsLayer layer = layers.get(which);
					Point2D newCenter = null;
					if (map.getLayer() != null) {
						Point2D center = new Point2D(map.getCenter().x, map.getCenter().y);
						Point2D wgs84Center = new Point2D();
			    		Projection proj = map.getLayer().getProjection();
			    		proj.inverseTransform(center, wgs84Center);
			    		// convert to the new layer's projection
			    		newCenter = new Point2D();
			    		layer.getProjection().transform(wgs84Center, newCenter);
			    		
					}
					map.setLayer(layer);
		    		if (newCenter != null) {
		    			map.setCenter((float) newCenter.x, (float) newCenter.y);
		    		}
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Layer");
			builder.setItems(new String[0], listener);
			AlertDialog layersDialog = builder.create();
			dialog = layersDialog;
            break;
        }
        return dialog;

    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
    	Log.i(TAG, "** onPrepareDialog "+id);
    	switch (id) {
		case DIALOG_LAYERS_ID:
			AlertDialog layersDialog = (AlertDialog) dialog;
			String[] items = new String[layers.size()];
			
			TmsLayer currentLayer = map.getLayer();
			for (int i = 0; i < layers.size(); i++) {
				items[i] = layers.get(i).getTitle();
				if (currentLayer != null && currentLayer.getName().equals(layers.get(i).getName())) {
					items[i] = ">" + items[i];
				}
			}
			
			ListView list = layersDialog.getListView();
			list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			
			//Log.i(TAG, "enabled "+layersDialog.getListView().getAdapter().isEnabled(0));
			//list.setSelector(R.drawable.selector);
			layersDialog.getListView().setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, items));
			//layersDialog.getListView().setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, items));
			//list.setItemChecked(0, true);
			//list.setItemChecked(1, false);
			
			//list.setItemChecked(1, true);
			//list.setSelection(1);
			//list.setSelected(true);
			//Log.i(TAG, "Selected item: "+list.getSelectedItem());
			break;
		}
    }
    
	@Override
	public void onSensorChanged(SensorEvent event) {
		int heading = (int) event.values[0];
		//Log.i(TAG, "heading:" +heading);
		map.setHeading(-heading);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	private void loadLayersConfig() {
        layers = new ArrayList<TmsLayer>();
        String settingUrl = PreferenceManager.getDefaultSharedPreferences(this).getString("layers_config_url", "");
        
        try {
        	if (layersSetting == null) {
        		//Log.i(TAG, "****** GET SETTNINGS FROM NET "+settingUrl);
        		layersSetting = Utils.httpGet(settingUrl);
        	}
			//String settings = Utils.httpGet(getString(R.string.settings_url));
        	//String settings = Utils.readInputStream(getResources().openRawResource(R.raw.settings));
			JSONArray json = new JSONArray(layersSetting);
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
				
				Projection proj = ProjectionFactory.getNamedPROJ4CoordinateSystem(srs);
				TmsLayer tmsLayer = new TmsLayer(bbox, resolutions, url, layerName, extension, proj);
				tmsLayer.setTitle(title);
				layers.add(tmsLayer);
				
				Log.d(TAG, "Title: "+title);
				Log.d(TAG, "URL: "+url);
				Log.d(TAG, "Extension: "+extension);
				Log.d(TAG, "Projection: "+srs);
			}
			
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
}