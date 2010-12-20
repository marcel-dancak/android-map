package sk.gista.android.utils;

import java.util.Timer;
import java.util.TimerTask;

public class MyAnimation extends TimerTask {

	private int duration;
	private int count;
	private int frame;
	private Timer timer;
	private long startTime;
	
	private MyAnimationListener listener;
	
	public MyAnimation(int duration, int count) {
		this.duration = duration;
		this.count = count;
	}
	
	public void start() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		frame = 0;
		startTime = System.currentTimeMillis();
		timer.schedule(this, 0, duration/count);
	}
	
	public void onAnimationStep(MyAnimationListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void run() {
		frame++;
		if (frame > count) {
			timer.cancel();
			if (listener != null) {
				listener.onEnd();
			}
		} else {
			//float fraction = (System.currentTimeMillis()-startTime)/(float) duration;
			float fraction = frame/(float) count;
			if (listener != null) {
				listener.onFrame(fraction);
			}
		}
	}

	
	public interface MyAnimationListener {
		void onFrame(float fraction);
		void onEnd();
	}
}
