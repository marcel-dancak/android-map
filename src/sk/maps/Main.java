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
        final Map map = new Map(this, layers);
        
        //layers.remove(0);
        
        Button zoomIn = new Button(this);
        zoomIn.setText("+");
        zoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				map.setZoom(map.getZoomLevel()+1);
			}
		});
        
        Button zoomOut = new Button(this);
        zoomOut.setText("-");
        zoomOut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (map.getZoomLevel() > 1) {
					map.setZoom(map.getZoomLevel()-1);
				}
			}
		});
        
        RelativeLayout layout = new RelativeLayout(this);
        LinearLayout zoomControls = new LinearLayout(this);
        zoomControls.addView(zoomOut);
        zoomControls.addView(zoomIn);
        
        layout.addView(map);
        layout.addView(zoomControls);
        setContentView(layout);
        //setContentView(map);
    }
}