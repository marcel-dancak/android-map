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

import sk.gista.android.app.About;
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
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

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
	private ImageButton zoomIn;
	private ImageButton zoomOut;
	private ImageButton myLocation;
	
	List<TmsLayer> layers;
	private LocationOverlay locationOverlay;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "** onCreate");
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Log.i(TAG, "LANDSCAPE: "+ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Log.i(TAG, "PORTRAIT: "+ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mapState = getSharedPreferences("MAP_STATE", MODE_PRIVATE);
        
        setContentView(R.layout.main);
        map = (Map) findViewById(R.id.map);
        
        locationOverlay= new LocationOverlay(this, map);// TODO: remove map parameter
        map.addOverlay(locationOverlay);
        
        zoomIn = (ImageButton) findViewById(R.id.zoom_in);
        zoomOut = (ImageButton) findViewById(R.id.zoom_out);
        myLocation = (ImageButton) findViewById(R.id.move_to_position);
        
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
        
        myLocation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Point2D location = locationOverlay.getLastLocation();
				if (location != null) {
					Point2D projected = new Point2D();
					map.getLayer().getProjection().transform(location, projected);
					map.moveToLocation((float) projected.x, (float) projected.y);
				} else {
					// use center of the map
					BBox bbox = map.getLayer().getBoundingBox();
					map.moveToLocation((bbox.minX + bbox.maxX)/2f, (bbox.minY + bbox.maxY)/2f);
				}
			}
		});
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
        if (layers == null || layers.size() == 0) {
    		loadLayersConfig();
    	}
        restoreState();
    }
    
    @Override
    protected void onStart() {
    	Log.i(TAG, "** onStart");
    	super.onStart();
    	int orientation = Integer.parseInt(getSetting("screen_orientation", "1"));
    	if (orientation != getRequestedOrientation()) {
    		setRequestedOrientation(orientation);
    	}
    	Log.i(TAG, "screen orient: "+orientation);
    }
    
    @Override
    protected void onResume() {
    	Log.i(TAG, "** onResume");
    	//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	super.onResume();
    	
    	
    	if (layers == null || layers.size() == 0) {
    		loadLayersConfig();
    		restoreState();
    	}
    	//restoreState();
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
    	//map.recycle();
    }
    
    @Override
    protected void onStop() {
    	Log.i(TAG, "** onStop");
    	super.onStop();
    	map.recycle();
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
    			setMapLayer(layer);
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
    	if (map.getLayer() == null && layers.size() > 0) {
    		setMapLayer(layers.get(0));
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
		case R.id.about:
			startActivity(new Intent(this, About.class));
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
					setMapLayer(layer);
		    		if (newCenter != null) {
		    			map.setCenter((float) newCenter.x, (float) newCenter.y);
		    		}
		    		((View) map).invalidate();
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
			int selectedItem = -1;
			for (int i = 0; i < layers.size(); i++) {
				items[i] = layers.get(i).getTitle();
				if (currentLayer != null && currentLayer.getName().equals(layers.get(i).getName())) {
					selectedItem = i;
				}
			}
			
			ListView list = layersDialog.getListView();
			list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			
			//Log.i(TAG, "enabled "+layersDialog.getListView().getAdapter().isEnabled(0));
			//list.setSelector(R.drawable.selector);
			//layersDialog.getListView().setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, items));
			//layersDialog.getListView().setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, items));
			layersDialog.getListView().setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_single_choice, items));
			if (selectedItem != -1) {
				list.setItemChecked(selectedItem, true);
			}
			//list.setSelection(1);
			//list.setSelected(true);
			//Log.i(TAG, "Selected item: "+list.getSelectedItem());
			break;
		}
    }
    
    private void setMapLayer(TmsLayer layer) {
    	if (map.getLayer() == null && layer != null) {
    		zoomIn.setVisibility(View.VISIBLE);
    		zoomOut.setVisibility(View.VISIBLE);
    		myLocation.setVisibility(View.VISIBLE);
    	}
    	map.setLayer(layer);
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
        try {
        	if (layersSetting == null) {
        		String settingUrl = getSetting("layers_config_url", "");
        		if (settingUrl.length() == 0 || settingUrl.endsWith("://")) {
        			Toast.makeText(this, "URL of layers configuration is not set", Toast.LENGTH_LONG).show();
        			return;
        		}
        		layersSetting = Utils.httpGet(settingUrl);
        	}

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
        //catch (java.net.UnknownHostException e)
        } catch (java.io.FileNotFoundException e) {
        	Log.e(TAG, "Downloading of layers configuration failed: Invalid URL", e);
        	Toast.makeText(this, "Invalid URL: "+e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e(TAG, "Downloading of layers configuration failed", e);
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (JSONException e) {
			Log.e(TAG, "Layers configuration parsing failed", e);
			Toast.makeText(this, "Layers configuration is not valid", Toast.LENGTH_LONG).show();
			layersSetting = null;// don't save invalid configuration
		}
	}
	
	private String getSetting(String name, String defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(name, defaultValue);
	}
}