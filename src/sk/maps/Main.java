package sk.maps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import sk.gista.settings.Settings;
import sk.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.PointF;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Main extends Activity implements SensorEventListener {
	private static final String TAG = Main.class.getName();
	
	static final int DIALOG_LAYERS_ID = 0;

	static final String ZOOM = "map_zoom";
	static final String LAYER_ID = "layer_id";
	static final String CENTER_X = "center_x";
	static final String CENTER_Y = "center_y";
	
	private int layerId;
	
	private SensorManager sensorManager;
	private MapView map;
	private Button zoomIn;
	private Button zoomOut;
	
	List<TmsLayer> layers;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, PreferenceManager.getDefaultSharedPreferences(this).getString("layers_config_url", ""));

        loadLayersConfig();
        
        TmsLayer layer = null;
        // set first layer if it's application startup (savedInstanceState is null) and there are some available layers
        if (savedInstanceState == null && layerId < layers.size()){
        	layer = layers.get(layerId);
        } else {
        	//startActivity(new Intent(this, Settings.class));
        }
        
        map = new Map(this, layer);
        //map = new MapSurface(this, layer.getBoundingBox(), layer.getResolutions(), layer);
        zoomIn = new Button(this);
        zoomIn.setText("+");
        zoomIn.setFocusable(false);
        zoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				map.setZoom(map.getZoom()+1);
			}
		});
        
        zoomOut = new Button(this);
        zoomOut.setText("-");
        zoomOut.setFocusable(false);
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
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
    }
    
    @Override
    protected void onResume() {
    	Log.i(TAG, "onResume");
    	super.onResume();
    	
    	if (layers.size() == 0) {
    		loadLayersConfig();
    	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.i(TAG, "Save State");
    	super.onSaveInstanceState(outState);
    	outState.putInt(LAYER_ID, layerId);
    	outState.putInt(ZOOM, map.getZoom());
    	PointF center = map.getCenter();
    	if (center != null) {
    		outState.putFloat(CENTER_X, center.x);
    		outState.putFloat(CENTER_Y, center.y);
    	}
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.i(TAG, "onRestoreInstanceState");
    	super.onRestoreInstanceState(savedInstanceState);
    	layerId = savedInstanceState.getInt(LAYER_ID);
    	int zoom = savedInstanceState.getInt(ZOOM, 1);
    	float centerX = savedInstanceState.getFloat(CENTER_X, Float.MIN_VALUE);
    	float centerY = savedInstanceState.getFloat(CENTER_Y, Float.MIN_VALUE);
    	
    	if (layerId < layers.size()) {
    		map.setLayer(layers.get(layerId));
    	}
    	map.setZoom(zoom);
    	if (centerX != Float.MIN_VALUE) {
    		map.setCenter(centerX, centerY);
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
    	Dialog dialog;
        switch(id) {
        case DIALOG_LAYERS_ID:
        	DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					System.out.println(which);
					layerId = which;
					map.setLayer(layers.get(layerId));
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Layer");

			String[] items = new String[layers.size()];
			for (int i = 0; i < layers.size(); i++) {
				items[i] = layers.get(i).getTitle();
			}
			Log.i(TAG, "create dialog (layers "+layers.size());
			builder.setItems(items, listener);
			
			AlertDialog layersDialog = builder.create();
			/*
			layersDialog.getListView().setSelector(R.drawable.selector);
			layersDialog.getListView().setSelection(1);
			
			Dialog cdialog = new Dialog(this);
			cdialog.setTitle("title");
			ListView list = new ListView(this);
			list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] {"item1", "item2"}));
			
			list.setSelection(1);
			list.setSelector(R.drawable.selector);
			cdialog.setContentView(list);
			*/
			
			dialog = layersDialog;
            break;
        default:
            dialog = null;
        }
        return dialog;

    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
    	Log.i(TAG, "prepare dialog "+id);
    	switch (id) {
		case DIALOG_LAYERS_ID:
			//if (true) break;
			AlertDialog layersDialog = (AlertDialog) dialog;
			String[] items = new String[layers.size()];
			for (int i = 0; i < layers.size(); i++) {
				items[i] = layers.get(i).getTitle();
			}
			
			if (layerId >= 0 && layerId < items.length) {
				items[layerId] = "> "+items[layerId];
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
        	String settings = Utils.httpGet(settingUrl);
			//String settings = Utils.httpGet(getString(R.string.settings_url));
        	//String settings = Utils.readInputStream(getResources().openRawResource(R.raw.settings));
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
				
				Projection proj = ProjectionFactory.getNamedPROJ4CoordinateSystem(srs);
				TmsLayer tmsLayer = new TmsLayer(bbox, resolutions, url, layerName, extension, proj);
				tmsLayer.setTitle(title);
				layers.add(tmsLayer);
				
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
	}
}