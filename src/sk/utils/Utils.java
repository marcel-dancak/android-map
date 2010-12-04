package sk.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;
import android.view.MotionEvent;

public class Utils {
	
	private static String TAG = Utils.class.getName();
	
	public static void dumpEvent(MotionEvent event) {
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP",
				"7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN
				|| actionCode == MotionEvent.ACTION_POINTER_UP) {
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}
		sb.append("]");
		Log.d(TAG, sb.toString());
	}
	
	public static String httpGet(String urlstring) throws MalformedURLException, IOException {
		URL url = new URL(urlstring);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		return readInputStream(httpCon.getInputStream());
	}
	
	public static String readFile(String filename) throws IOException {
		return readInputStream(new FileInputStream(filename));
	}
	
	public static String readInputStream(InputStream is) throws IOException {
		String line;
		String response = "";
		BufferedReader input = new BufferedReader(new InputStreamReader(is));
		while ((line = input.readLine()) != null) {
			response += line;
		}
		input.close();
		return response;
	}
}
