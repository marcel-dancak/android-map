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

import sk.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class Main extends Activity implements SensorEventListener {
	private static final String TAG = Main.class.getName();
	
	static final int DIALOG_LAYERS_ID = 0;

	private SensorManager sensorManager;
	private MapView map;
	List<TmsLayer> layers;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        layers = new ArrayList<TmsLayer>();
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
        
        TmsLayer layer = layers.get(0);
        map = new Map(this, layer);
        //map = new MapSurface(this, layer.getBoundingBox(), layer.getResolutions(), layer);
        
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
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
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
					map.setLayer(layers.get(which));
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Layer");
			/*
			builder.setMultiChoiceItems(new String[] { "Presov", "Kosice", "2", "3"},
					new boolean[] {true, true, false, false}, new DialogInterface.OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						}
					});
			*/
			//builder.setSingleChoiceItems(new String[] { "Presov", "Kosice", "2", "3"}, 0, listener);
			String[] items = new String[layers.size()];
			for (int i = 0; i < layers.size(); i++) {
				items[i] = layers.get(i).getTitle();
			}
			//builder.setItems(new String[] { "Presov", "Kosice", "2", "3"}, listener);
			builder.setItems(items, listener);
			
			AlertDialog layersDialog = builder.create();
			/*
			dialog.setButton("button", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

				}
			});
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
	public void onSensorChanged(SensorEvent event) {
		int heading = (int) event.values[0];
		//Log.i(TAG, "heading:" +heading);
		map.setHeading(-heading);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
}