package sk.gista.android.maps;

import android.graphics.Canvas;

public interface Overlay extends AndroidComponent {

	void onDraw(MapView map, Canvas canvas);
	
}
