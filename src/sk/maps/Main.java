package sk.maps;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Main extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        List<Layer> layers = new ArrayList<Layer>();
        layers.add(new Layer());
        BBox bbox = new BBox(21.1466999999999992f, 48.9388000000000005f, 21.3455000000000013f, 49.0544000000000011f);
        double resolutions[] = {0.00158739084402105, 0.00031747816880421, 0.00015873908440210, 0.00007936954220105, 0.00003174781688042, 0.00001587390844021, 0.00000793695422011, 0.00000317478168804, 0.00000158739084402};
        final Map map = new Map(this, bbox, resolutions, layers);
        //final MapSurface mapSurface = new MapSurface(this, bbox, resolutions);
        
        //layers.remove(0);
        
        Button zoomIn = new Button(this);
        zoomIn.setText("+");
        zoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				map.setZoom(map.getZoomLevel()+1);
				//mapSurface.setZoom(map.getZoomLevel()+1);
			}
		});
        
        Button zoomOut = new Button(this);
        zoomOut.setText("-");
        zoomOut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (map.getZoomLevel() > 1) {
					map.setZoom(map.getZoomLevel()-1);
					//mapSurface.setZoom(mapSurface.getZoomLevel()-1);
				}
			}
		});
        
        RelativeLayout layout = new RelativeLayout(this);
        LinearLayout zoomControls = new LinearLayout(this);
        zoomControls.addView(zoomOut);
        zoomControls.addView(zoomIn);
        
        layout.addView(map);
        //layout.addView(mapSurface);
        layout.addView(zoomControls);
        setContentView(layout);
        //setContentView(map);
    }
}