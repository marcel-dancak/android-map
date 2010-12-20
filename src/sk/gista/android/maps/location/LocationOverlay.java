package sk.gista.android.maps.location;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.jhlabs.geom.Point2D;

import sk.gista.android.maps.MapView;
import sk.gista.android.maps.Overlay;

public class LocationOverlay implements Overlay, LocationListener, Listener {

	private static final String TAG = LocationOverlay.class.getName();
	
	private Context context;
	private LocationManager locationManager;
	private Location currentLocation;
	private Point2D currentLocationPoint = new Point2D(); //new Point2D(21.23886386, 49.00096926)
	private Point2D projectedLocation = new Point2D();
	
	private Paint pointStyle;
	private Paint accuracyStyle;
	
	public LocationOverlay(Context context) {
		this.context = context;
		pointStyle = new Paint();
		pointStyle.setAntiAlias(true);
		pointStyle.setColor(Color.RED);
		
		accuracyStyle = new Paint();
		accuracyStyle.setColor(Color.argb(50, 204, 0, 0));
	}
	
	@Override
	public void onDraw(MapView map, Canvas canvas) {
		//Log.i(TAG, "draw location overlay");
		if (currentLocation != null) {
			currentLocationPoint.x = currentLocation.getLongitude();
			currentLocationPoint.y = currentLocation.getLatitude();
			map.getLayer().getProjection().transform(currentLocationPoint, projectedLocation);
			//Log.i(TAG, format("projected position: [%f, %f]", projectedLocation.x, projectedLocation.y));
			PointF currentPos = map.mapToScreenAligned((float) projectedLocation.x, (float) projectedLocation.y);
			float accuracy = currentLocation.getAccuracy()/(float) map.getLayer().getResolutions()[map.getZoom()];
			//float accuracy = 75.0f/(float) map.getLayer().getResolutions()[map.getZoom()]; // for tests on emulator
			canvas.drawArc(new RectF(currentPos.x-accuracy, currentPos.y-accuracy, currentPos.x+accuracy, currentPos.y+accuracy), 0, 360, true, accuracyStyle);
			canvas.drawArc(new RectF(currentPos.x-3, currentPos.y-3, currentPos.x+3, currentPos.y+3), 0, 360, true, pointStyle);
		}
	}

	@Override
	public void onPause() {
		locationManager.removeUpdates(this);
	}

	@Override
	public void onResume() {
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(this);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this );
		locationManager.addGpsStatusListener(this);
		
		currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (currentLocation == null) {
			currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.i(TAG, "new location: "+location);
		currentLocation = location;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		
	}

	@Override
	public void onGpsStatusChanged(int event) {
		
	}
}
